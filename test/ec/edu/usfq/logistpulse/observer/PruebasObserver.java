package ec.edu.usfq.logistpulse.observer;

import ec.edu.usfq.logistpulse.EscenarioDemo;
import ec.edu.usfq.logistpulse.SistemaLogistPulse;
import ec.edu.usfq.logistpulse.command.AlmacenarLote;
import ec.edu.usfq.logistpulse.command.DecisionRegistrada;
import ec.edu.usfq.logistpulse.command.LiberarCuarentena;
import ec.edu.usfq.logistpulse.command.RecibirLote;
import ec.edu.usfq.logistpulse.command.ReservarPedido;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static ec.edu.usfq.logistpulse.EscenarioDemo.*;
import static ec.edu.usfq.logistpulse.MiniTest.*;

/** Pruebas del patrón Observer. */
public final class PruebasObserver {

    private PruebasObserver() {
    }

    private static RecibirLote recepcion(String clave) {
        return new RecibirLote(clave, ANDREA, LECHE, NORTE, "L-LEC-X", 20, LocalDate.of(2026, 12, 31));
    }

    public static void correr() {
        seccion("OBSERVER");

        prueba("Cada operacion publica un StockCambiado por lote, con la transicion de estado", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            List<StockCambiado> recibidos = new ArrayList<>();
            s.eventos().suscribir(StockCambiado.class, Suscriptor.con("prueba", recibidos::add));
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", LECHE, NORTE, 100));
            iguales(2, recibidos.size(), "un evento por lote tocado");
            StockCambiado e = recibidos.get(0);
            iguales(L_LEC_1108, e.lote(), "lote");
            iguales(TipoMovimiento.RESERVAR, e.movimiento(), "movimiento");
            iguales("ALMACENADO", e.estadoAntes(), "estado antes");
            iguales("RESERVADO", e.estadoDespues(), "estado despues");
            iguales(70, e.deltaReservado(), "delta reservado");
            iguales(0, e.deltaFisico(), "delta fisico");
            verificar(e.momento() instanceof Instant, "momento es un Instant");
        });

        prueba("El Ledger recibe las decisiones y los movimientos", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", LECHE, NORTE, 100));
            iguales(1, s.ledger().decisiones().size(), "decisiones");
            iguales(2, s.ledger().movimientos().size(), "movimientos");
        });

        prueba("Cada suscriptor recibe solo el tipo de evento que pidio", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            List<StockCambiado> movimientos = new ArrayList<>();
            List<DecisionRegistrada> decisiones = new ArrayList<>();
            s.eventos().suscribir(StockCambiado.class, Suscriptor.con("m", movimientos::add));
            s.eventos().suscribir(DecisionRegistrada.class, Suscriptor.con("d", decisiones::add));
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ACEITE, NORTE, 10)); // rechazado
            iguales(0, movimientos.size(), "un rechazo no mueve stock");
            iguales(1, decisiones.size(), "pero la decision si se publica");
        });

        prueba("PULSEFLEET solo crea tareas para movimientos fisicos (tabla de reglas)", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            iguales(0, s.pulseFleet().tareas().size(), "reservar no mueve nada");
            s.central().despachar(recepcion("K2"));
            iguales(0, s.pulseFleet().tareas().size(), "recibir deja el lote en el muelle");
            iguales("MUELLE", s.pulseFleet().zonaEnGemelo("L-LEC-X", NORTE).orElseThrow(), "gemelo tras recibir");
            s.central().despachar(new AlmacenarLote("K3", ANDREA, LECHE, NORTE, "L-LEC-X"));
            iguales(1, s.pulseFleet().tareas().size(), "almacenar si mueve el lote");
            iguales("RACK", s.pulseFleet().zonaEnGemelo("L-LEC-X", NORTE).orElseThrow(), "gemelo tras almacenar");
        });

        prueba("Un movimiento nuevo se atiende agregando una regla, sin tocar PULSEFLEET", () -> {
            PulseFleet flota = new PulseFleet(List.of("AMR-X"), List.of(new PulseFleet.Regla(TipoMovimiento.AJUSTAR,
                    PulseFleet.Sentido.CUALQUIERA, "RACK", "MESA-CONTEO", "recontar", true)));
            flota.alRecibir(new StockCambiado(Instant.EPOCH, TipoMovimiento.AJUSTAR, ARROZ, NORTE, L_ARZ_0901,
                    -3, 0, "ALMACENADO", "ALMACENADO", null));
            iguales(1, flota.tareas().size(), "la regla nueva genero la tarea");
            iguales("MESA-CONTEO", flota.tareas().get(0).hacia(), "destino");
        });

        prueba("Un suscriptor se da de baja y vuelve en ejecucion", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(recepcion("K1"));
            verificar(s.eventos().desuscribir(s.pulseFleet()), "deberia darse de baja");
            s.central().despachar(new AlmacenarLote("K2", ANDREA, LECHE, NORTE, "L-LEC-X"));
            iguales(0, s.pulseFleet().tareas().size(), "dado de baja no recibe");
            s.eventos().suscribir(StockCambiado.class, s.pulseFleet());
            s.central().despachar(new LiberarCuarentena("K3", LUCIA, ACEITE, NORTE, L_ACE_0820));
            iguales(1, s.pulseFleet().tareas().size(), "de vuelta si recibe");
        });

        prueba("Un suscriptor que falla no impide que los demas reciban el evento", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            List<StockCambiado> despues = new ArrayList<>();
            s.eventos().suscribir(StockCambiado.class, Suscriptor.con("Roto", e -> {
                throw new IllegalStateException("servicio caido");
            }));
            s.eventos().suscribir(StockCambiado.class, Suscriptor.con("Siguiente", despues::add));
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            iguales(1, despues.size(), "el suscriptor siguiente recibio el evento");
            iguales(1, s.ledger().movimientos().size(), "el ledger tambien");
            iguales(List.of("Roto: servicio caido"), s.eventos().fallosDeEntrega(), "fallo registrado");
        });

        prueba("El ledger no se puede modificar desde afuera", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            lanza(UnsupportedOperationException.class, () -> s.ledger().decisiones().clear(), "borrar decisiones");
            lanza(UnsupportedOperationException.class, () -> s.ledger().movimientos().clear(), "borrar movimientos");
        });
    }
}
