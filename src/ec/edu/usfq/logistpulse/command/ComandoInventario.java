package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;

import java.util.Optional;

/**
 * <b>Rol en el patrón Command: COMMAND (interfaz).</b>
 *
 * <p>Una operación de inventario convertida en objeto: lleva todos sus datos, declara
 * qué hay que revisar antes de ejecutarla, queda en el ledger, se reintenta sin
 * duplicarse y, si es reversible, se compensa con su inverso.</p>
 *
 * <p>Depende de {@link OperacionesInventario} (una interfaz), no del inventario concreto.</p>
 */
public interface ComandoInventario {

    /** Identificador interno único (CMD-001, CMD-002...). */
    String id();

    /** Clave que manda el cliente para poder reintentar sin duplicar. */
    String claveIdempotencia();

    /**
     * Todos los datos de la operación. Misma clave + misma huella = reintento legítimo;
     * misma clave + otra huella = clave reutilizada para otra operación (se rechaza).
     */
    String huella();

    /** Qué hay que revisar antes de ejecutar; es lo que recorre la cadena. */
    SolicitudMovimiento solicitud();

    /**
     * Delega el trabajo en el receiver.
     *
     * @throws RuntimeException las del contrato de {@link OperacionesInventario}; la central las captura
     */
    void ejecutar(OperacionesInventario operaciones);

    /**
     * Comando que deshace a este, o vacío si es irreversible
     * (un despacho no se "des-envía": se corrige con otra operación).
     */
    Optional<ComandoInventario> inverso(Actor solicitante);

    default Actor actor() {
        return solicitud().actor();
    }

    default String describir() {
        return solicitud().descripcion();
    }
}
