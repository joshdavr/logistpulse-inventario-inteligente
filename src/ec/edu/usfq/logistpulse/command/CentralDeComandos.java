package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.EslabonValidacion;
import ec.edu.usfq.logistpulse.chain.Veredicto;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;
import ec.edu.usfq.logistpulse.observer.PublicadorEventos;
import ec.edu.usfq.logistpulse.state.TransicionInvalidaException;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>Rol en el patrón Command: INVOKER.</b>
 *
 * <p>Único punto de entrada de operaciones, vengan de la API o de un agente de IA.
 * Solo orquesta; no conoce ningún comando ni eslabón concreto:</p>
 * <pre>
 *  comando -> [idempotencia por huella] -> CHAIN -> execute() -> registrar
 *                                                     |
 *          Inventario: STRATEGY elige lotes, STATE valida, OBSERVER avisa
 * </pre>
 *
 * <p>Garantías:</p>
 * <ul>
 *   <li><b>Todo intento queda en el ledger</b> (ejecutado, rechazado, duplicado o fallido).</li>
 *   <li><b>Idempotencia por huella:</b> la misma clave con los mismos datos es un reintento
 *       (DUPLICADO, no se aplica de nuevo); con otros datos se rechaza y se audita.
 *       Si un comando falla, su clave queda libre para reintentar.</li>
 *   <li><b>Deshacer nunca pierde comandos:</b> el comando sale del historial solo si la
 *       compensación se ejecutó. Si falla, sigue ahí y el intento queda registrado.</li>
 * </ul>
 */
public class CentralDeComandos {

    private final ConsultasInventario consultas;
    private final OperacionesInventario operaciones;
    private final EslabonValidacion cadena;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    private final Map<String, ComandoInventario> aplicadosPorClave = new HashMap<>();
    private final Map<String, ComandoInventario> aplicadosPorId = new HashMap<>();
    /** id del comando original -> id del comando que lo compensó. */
    private final Map<String, String> compensadoPor = new HashMap<>();
    /** Operaciones normales vigentes, para deshacer en orden inverso (LIFO). */
    private final Deque<ComandoInventario> historial = new ArrayDeque<>();

    public CentralDeComandos(ConsultasInventario consultas, OperacionesInventario operaciones,
                             EslabonValidacion cadena, PublicadorEventos eventos, Clock reloj) {
        this.consultas = Objects.requireNonNull(consultas, "consultas");
        this.operaciones = Objects.requireNonNull(operaciones, "operaciones");
        this.cadena = Objects.requireNonNull(cadena, "cadena");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    public Resultado despachar(ComandoInventario comando) {
        return procesar(Objects.requireNonNull(comando, "comando"), null);
    }

    /**
     * Compensa un comando ejecutado con su inverso, que pasa por los MISMOS controles.
     *
     * @throws IllegalArgumentException si no hay un comando ejecutado con ese id
     */
    public Resultado compensar(String comandoId, Actor solicitante) {
        ComandoInventario original = aplicadosPorId.get(comandoId);
        if (original == null) {
            throw new IllegalArgumentException("No hay un comando ejecutado con id " + comandoId);
        }
        String previa = compensadoPor.get(comandoId);
        if (previa != null) {
            String msg = original.id() + " ya fue compensado por " + previa + "; no se repite";
            registrar(original.id(), original.claveIdempotencia(), solicitante,
                    "COMPENSAR " + original.describir(), EstadoDecision.DUPLICADO, msg, original.id());
            return new Resultado(EstadoDecision.DUPLICADO, previa, msg, null);
        }
        Optional<ComandoInventario> inverso;
        try {
            inverso = original.inverso(solicitante);
        } catch (RuntimeException e) {
            String msg = "No se pudo construir la compensacion: " + e.getMessage();
            registrar(original.id(), original.claveIdempotencia(), solicitante,
                    "COMPENSAR " + original.describir(), EstadoDecision.FALLIDO, msg, original.id());
            return new Resultado(EstadoDecision.FALLIDO, original.id(), msg, null);
        }
        if (inverso.isEmpty()) {
            String msg = "[Command] " + original.id() + " (" + original.solicitud().tipo()
                    + ") es irreversible; se corrige con una operacion nueva";
            registrar(original.id(), original.claveIdempotencia(), solicitante,
                    "COMPENSAR " + original.describir(), EstadoDecision.RECHAZADO, msg, original.id());
            return new Resultado(EstadoDecision.RECHAZADO, original.id(), msg, null);
        }
        Resultado r = procesar(inverso.get(), original.id());
        if (r.exitoso()) {
            historial.remove(original);
            compensadoPor.put(original.id(), r.comandoId());
        }
        return r;
    }

    /**
     * Deshace la última operación normal vigente. Si la compensación falla, el comando
     * NO sale del historial: el siguiente intento vuelve a apuntar a él.
     *
     * @throws IllegalStateException si no hay nada que deshacer
     */
    public Resultado deshacerUltimo(Actor solicitante) {
        ComandoInventario ultimo = historial.peek(); // peek, no pop: no se pierde si falla
        if (ultimo == null) {
            throw new IllegalStateException("No hay operaciones para deshacer");
        }
        return compensar(ultimo.id(), solicitante);
    }

    private Resultado procesar(ComandoInventario comando, String compensaA) {
        // 1. Idempotencia por huella
        ComandoInventario previo = aplicadosPorClave.get(comando.claveIdempotencia());
        if (previo != null) {
            if (previo.huella().equals(comando.huella())) {
                String msg = "La clave " + comando.claveIdempotencia() + " ya se aplico en " + previo.id()
                        + " con los mismos datos; no se repite";
                registrar(comando, EstadoDecision.DUPLICADO, msg, compensaA);
                return new Resultado(EstadoDecision.DUPLICADO, previo.id(), msg, null);
            }
            String msg = "[Command] La clave " + comando.claveIdempotencia() + " ya se uso en " + previo.id()
                    + " para otra operacion; use una clave nueva";
            registrar(comando, EstadoDecision.RECHAZADO, msg, compensaA);
            return new Resultado(EstadoDecision.RECHAZADO, comando.id(), msg, null);
        }

        // 2. Chain of Responsibility
        Veredicto veredicto;
        try {
            veredicto = cadena.evaluar(comando.solicitud(), consultas);
        } catch (RuntimeException e) {
            String msg = "[Chain] error inesperado al validar: " + e.getMessage();
            registrar(comando, EstadoDecision.FALLIDO, msg, compensaA);
            return new Resultado(EstadoDecision.FALLIDO, comando.id(), msg, null);
        }
        if (!veredicto.aprobado()) {
            String msg = "[" + veredicto.eslabon() + "] " + veredicto.motivo();
            registrar(comando, EstadoDecision.RECHAZADO, msg, compensaA);
            return new Resultado(EstadoDecision.RECHAZADO, comando.id(), msg, veredicto);
        }

        // 3. Ejecutar (adentro actuan Strategy, State y Observer)
        try {
            comando.ejecutar(operaciones);
        } catch (TransicionInvalidaException e) {
            String msg = "[State] " + e.getMessage();
            registrar(comando, EstadoDecision.RECHAZADO, msg, compensaA);
            return new Resultado(EstadoDecision.RECHAZADO, comando.id(), msg, veredicto);
        } catch (RuntimeException e) {
            String msg = "[Inventario] " + e.getMessage();
            registrar(comando, EstadoDecision.FALLIDO, msg, compensaA);
            return new Resultado(EstadoDecision.FALLIDO, comando.id(), msg, veredicto);
        }

        // 4. Recordar y registrar
        aplicadosPorClave.put(comando.claveIdempotencia(), comando);
        aplicadosPorId.put(comando.id(), comando);
        if (compensaA == null) {
            historial.push(comando);
        }
        registrar(comando, EstadoDecision.EJECUTADO, veredicto.trazaLegible(), compensaA);
        return new Resultado(EstadoDecision.EJECUTADO, comando.id(), "OK", veredicto);
    }

    private void registrar(ComandoInventario c, EstadoDecision estado, String detalle, String compensaA) {
        registrar(c.id(), c.claveIdempotencia(), c.actor(), c.describir(), estado, detalle, compensaA);
    }

    private void registrar(String id, String clave, Actor actor, String operacion,
                           EstadoDecision estado, String detalle, String compensaA) {
        eventos.publicar(new DecisionRegistrada(reloj.instant(), id, clave, actor, operacion, estado, detalle, compensaA));
    }

    public int operacionesDeshacibles() {
        return historial.size();
    }

    /** Id del próximo comando que deshacerUltimo intentaría compensar. */
    public Optional<String> proximoADeshacer() {
        return Optional.ofNullable(historial.peek()).map(ComandoInventario::id);
    }
}
