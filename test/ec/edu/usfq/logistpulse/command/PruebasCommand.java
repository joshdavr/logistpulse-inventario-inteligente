package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.EscenarioDemo;
import ec.edu.usfq.logistpulse.SistemaLogistPulse;
import ec.edu.usfq.logistpulse.dominio.TipoActor;
import ec.edu.usfq.logistpulse.inventario.Reserva;
import ec.edu.usfq.logistpulse.state.VistaLote;

import java.time.LocalDate;

import static ec.edu.usfq.logistpulse.EscenarioDemo.*;
import static ec.edu.usfq.logistpulse.MiniTest.*;

/** Pruebas del patrón Command. */
public final class PruebasCommand {

    private PruebasCommand() {
    }

    static VistaLote lote(SistemaLogistPulse s, String codigo, String bodega) {
        return s.inventario().lote(codigo, bodega).orElseThrow();
    }

    public static void correr() {
        seccion("COMMAND");

        prueba("Ejecutar un comando cambia el inventario y queda en el ledger", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado r = s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 40));
            verificar(r.exitoso(), r.toString());
            iguales(40, lote(s, L_ARZ_0901, NORTE).reservado(), "reservado");
            DecisionRegistrada d = s.ledger().decisiones().get(0);
            iguales(EstadoDecision.EJECUTADO, d.estado(), "estado en el ledger");
            iguales("K1", d.clave(), "clave en el ledger");
            iguales(s.reloj().instant(), d.momento(), "momento como Instant");
        });

        prueba("Reintento con la misma clave y los mismos datos: DUPLICADO, se aplica una vez", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado primero = s.central().despachar(new ReservarPedido("K2", ANDREA, "P2", ARROZ, NORTE, 40));
            Resultado reintento = s.central().despachar(new ReservarPedido("K2", ANDREA, "P2", ARROZ, NORTE, 40));
            iguales(EstadoDecision.DUPLICADO, reintento.estado(), "estado del reintento");
            iguales(primero.comandoId(), reintento.comandoId(), "apunta al comando original");
            iguales(40, lote(s, L_ARZ_0901, NORTE).reservado(), "no se reservo dos veces");
        });

        prueba("Misma clave con otros datos: se rechaza y se audita (no hay falso OK)", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K3", ANDREA, "P3", ARROZ, NORTE, 40));
            Resultado otraCantidad = s.central().despachar(new ReservarPedido("K3", ANDREA, "P3", ARROZ, NORTE, 41));
            iguales(EstadoDecision.RECHAZADO, otraCantidad.estado(), "otra cantidad");
            verificar(otraCantidad.mensaje().contains("otra operacion"), otraCantidad.mensaje());
            Resultado otroActor = s.central().despachar(new ReservarPedido("K3", MARIO, "P3", ARROZ, NORTE, 40));
            iguales(EstadoDecision.RECHAZADO, otroActor.estado(), "otro actor con la misma clave");
            iguales(40, lote(s, L_ARZ_0901, NORTE).reservado(), "nada cambio");
            iguales(2L, s.ledger().contar(EstadoDecision.RECHAZADO), "los dos intentos quedaron auditados");
        });

        prueba("Si un comando falla, su clave queda libre para reintentar", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado falla = s.central().despachar(new ReservarPedido("K4", ANDREA, "P4", ARROZ, NORTE, 500));
            iguales(EstadoDecision.RECHAZADO, falla.estado(), "primer intento");
            Resultado ok = s.central().despachar(new ReservarPedido("K4", ANDREA, "P4", ARROZ, NORTE, 50));
            iguales(EstadoDecision.EJECUTADO, ok.estado(), "reintento corregido con la misma clave");
        });

        prueba("Un comando rechazado no toca el inventario y el ledger guarda el motivo", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado r = s.central().despachar(new ReservarPedido("K5", AGENTE, "P5", ACEITE, NORTE, 10));
            iguales(EstadoDecision.RECHAZADO, r.estado(), "estado");
            iguales(80, s.inventario().stockLibre(ACEITE, NORTE), "stock sin cambios");
            verificar(s.ledger().decisiones().get(0).detalle().startsWith("[Lote]"), "el motivo nombra al eslabon");
            iguales(0, s.ledger().movimientos().size(), "no hubo movimientos de stock");
        });

        prueba("Cada comando conoce su inverso, o sabe que es irreversible", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            ReservarPedido reservar = new ReservarPedido("A", ANDREA, "P6", ARROZ, NORTE, 5);
            verificar(reservar.inverso(ANDREA).isEmpty(), "sin ejecutar no hay nada que liberar");
            s.central().despachar(reservar);
            verificar(reservar.inverso(ANDREA).orElseThrow() instanceof LiberarReserva, "Reservar -> Liberar");
            iguales("A#comp", reservar.inverso(ANDREA).orElseThrow().claveIdempotencia(), "clave de compensacion");
            Reserva reserva = s.inventario().reserva("P6").orElseThrow();
            verificar(new LiberarReserva("B", ANDREA, reserva).inverso(ANDREA).orElseThrow() instanceof ReservarPedido,
                    "Liberar -> Reservar");
            iguales(-7, new AjustarStock("C", CARLOS, ARROZ, NORTE, L_ARZ_0901, 7, "x").inverso(CARLOS)
                    .orElseThrow().solicitud().cantidad(), "Ajustar(+7) -> Ajustar(-7)");
            ComandoInventario vuelta = new TransferirStock("D", ANDREA, ACEITE, L_ACE_1010, SUR, NORTE, 9)
                    .inverso(ANDREA).orElseThrow();
            iguales(NORTE, vuelta.solicitud().bodega(), "la vuelta sale del destino");
            iguales(SUR, vuelta.solicitud().bodegaDestino(), "la vuelta llega al origen");
            verificar(new PonerEnCuarentena("E", LUCIA, ARROZ, NORTE, L_ARZ_0901, "x").inverso(LUCIA)
                    .orElseThrow() instanceof LiberarCuarentena, "Cuarentena -> Liberar cuarentena");
            verificar(new LiberarCuarentena("F", LUCIA, ACEITE, NORTE, L_ACE_0820).inverso(LUCIA)
                    .orElseThrow() instanceof PonerEnCuarentena, "Liberar cuarentena -> Cuarentena");
            verificar(new RecibirLote("G", ANDREA, ARROZ, NORTE, "L-X", 5, LocalDate.of(2027, 1, 1))
                    .inverso(ANDREA).isEmpty(), "Recibir es irreversible");
            verificar(new AlmacenarLote("H", ANDREA, ARROZ, NORTE, "L-X").inverso(ANDREA).isEmpty(), "Almacenar es irreversible");
            verificar(new DespacharPedido("I", ANDREA, reserva).inverso(ANDREA).isEmpty(), "Despachar es irreversible");
        });

        prueba("Compensar una reserva libera justo los lotes que eligio la estrategia", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado r = s.central().despachar(new ReservarPedido("K7", ANDREA, "P7", LECHE, NORTE, 100));
            iguales("RESERVADO", lote(s, L_LEC_1108, NORTE).nombreEstado(), "1108 quedo reservado");
            Resultado c = s.central().compensar(r.comandoId(), ANDREA);
            verificar(c.exitoso(), c.toString());
            iguales(0, lote(s, L_LEC_1108, NORTE).reservado(), "1108 liberado");
            iguales(0, lote(s, L_LEC_1103, NORTE).reservado(), "1103 liberado");
            iguales("ALMACENADO", lote(s, L_LEC_1108, NORTE).nombreEstado(), "1108 vuelve a ALMACENADO");
            iguales(Reserva.Estado.LIBERADA, s.inventario().reserva("P7").orElseThrow().estado(), "estado de la reserva");
        });

        prueba("Compensar dos veces lo mismo (aunque lo pida otra persona) es DUPLICADO", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado r = s.central().despachar(new AjustarStock("K8", CARLOS, CEMENTO, NORTE, L_CEM_0920, 30, "ingreso"));
            s.central().compensar(r.comandoId(), CARLOS);
            Resultado otraVez = s.central().compensar(r.comandoId(), LUCIA);
            iguales(EstadoDecision.DUPLICADO, otraVez.estado(), "segunda compensacion");
            iguales(60, lote(s, L_CEM_0920, NORTE).fisico(), "se revirtio una sola vez");
        });

        prueba("Un comando irreversible no se compensa y el intento queda registrado", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K9", ANDREA, "P9", ARROZ, NORTE, 10));
            Resultado despacho = s.central().despachar(
                    new DespacharPedido("K10", ANDREA, s.inventario().reserva("P9").orElseThrow()));
            verificar(despacho.exitoso(), despacho.toString());
            Resultado c = s.central().compensar(despacho.comandoId(), CARLOS);
            iguales(EstadoDecision.RECHAZADO, c.estado(), "estado");
            verificar(c.mensaje().contains("irreversible"), c.mensaje());
            iguales(50, lote(s, L_ARZ_0901, NORTE).fisico(), "el despacho sigue hecho");
            iguales(3, s.ledger().decisiones().size(), "el intento esta en el ledger");
        });

        prueba("deshacerUltimo deshace en orden inverso (LIFO)", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K11", ANDREA, "P11", ARROZ, NORTE, 10));
            s.central().despachar(new AjustarStock("K12", CARLOS, CEMENTO, NORTE, L_CEM_1105, 5, "conteo"));
            iguales(2, s.central().operacionesDeshacibles(), "operaciones en el historial");
            s.central().deshacerUltimo(CARLOS);
            iguales(40, lote(s, L_CEM_1105, NORTE).fisico(), "primero se deshace el ajuste");
            iguales(10, lote(s, L_ARZ_0901, NORTE).reservado(), "la reserva sigue");
            s.central().deshacerUltimo(CARLOS);
            iguales(0, lote(s, L_ARZ_0901, NORTE).reservado(), "despues se deshace la reserva");
            lanza(IllegalStateException.class, () -> s.central().deshacerUltimo(CARLOS), "historial vacio");
        });

        prueba("Humanos y agentes pasan por la misma cadena y quedan identificados", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado h = s.central().despachar(new ReservarPedido("H1", ANDREA, "PH", ARROZ, NORTE, 5));
            Resultado ia = s.central().despachar(new ReservarPedido("IA1", AGENTE, "PIA", ARROZ, NORTE, 5));
            iguales(h.veredicto().recorrido(), ia.veredicto().recorrido(), "mismo recorrido");
            iguales(1L, s.ledger().contarPorActor(TipoActor.HUMANO), "decisiones humanas");
            iguales(1L, s.ledger().contarPorActor(TipoActor.AGENTE_IA), "decisiones de IA");
        });

        prueba("Una transicion invalida del State llega como RECHAZADO con [State]", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K13", ANDREA, "P13", LECHE, NORTE, 70)); // 1108 queda RESERVADO
            Resultado r = s.central().despachar(new PonerEnCuarentena("K14", LUCIA, LECHE, NORTE, L_LEC_1108, "x"));
            iguales(EstadoDecision.RECHAZADO, r.estado(), "estado");
            verificar(r.mensaje().startsWith("[State]"), r.mensaje());
            iguales("RESERVADO", lote(s, L_LEC_1108, NORTE).nombreEstado(), "el lote no cambio");
        });
    }
}
