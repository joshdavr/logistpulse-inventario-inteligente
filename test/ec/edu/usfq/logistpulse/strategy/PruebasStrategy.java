package ec.edu.usfq.logistpulse.strategy;

import ec.edu.usfq.logistpulse.EscenarioDemo;
import ec.edu.usfq.logistpulse.SistemaLogistPulse;
import ec.edu.usfq.logistpulse.command.AjustarStock;
import ec.edu.usfq.logistpulse.command.RecibirLote;
import ec.edu.usfq.logistpulse.command.ReservarPedido;
import ec.edu.usfq.logistpulse.dominio.PerfilDemanda;
import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.state.Lote;
import ec.edu.usfq.logistpulse.state.VistaLote;
import ec.edu.usfq.logistpulse.strategy.picking.AsignacionLote;
import ec.edu.usfq.logistpulse.strategy.picking.PickingFefo;
import ec.edu.usfq.logistpulse.strategy.picking.PickingFifo;
import ec.edu.usfq.logistpulse.strategy.picking.PickingLifo;
import ec.edu.usfq.logistpulse.strategy.picking.PlanPicking;
import ec.edu.usfq.logistpulse.strategy.picking.PoliticaPicking;
import ec.edu.usfq.logistpulse.strategy.reposicion.LoteEconomico;
import ec.edu.usfq.logistpulse.strategy.reposicion.MinMax;
import ec.edu.usfq.logistpulse.strategy.reposicion.OrdenCompra;
import ec.edu.usfq.logistpulse.strategy.reposicion.PuntoDeReorden;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static ec.edu.usfq.logistpulse.EscenarioDemo.*;
import static ec.edu.usfq.logistpulse.MiniTest.*;

/** Pruebas del patrón Strategy (picking y reposición). */
public final class PruebasStrategy {

    private PruebasStrategy() {
    }

    private static PlanPicking simular(SistemaLogistPulse s, PoliticaPicking p, String sku, int n) {
        return s.picking().planificarCon(p, s.inventario().lotesDe(sku, NORTE), n, s.inventario().hoy());
    }

    public static void correr() {
        seccion("STRATEGY");

        prueba("FEFO toma primero lo que vence antes; el contexto descarta lo vencido", () -> {
            PlanPicking p = simular(EscenarioDemo.sistema(), new PickingFefo(), LECHE, 100);
            iguales(List.of(new AsignacionLote(L_LEC_1108, 70), new AsignacionLote(L_LEC_1103, 30)),
                    p.asignaciones(), "plan");
        });

        prueba("FIFO toma primero lo que entro antes", () -> {
            PlanPicking p = simular(EscenarioDemo.sistema(), new PickingFifo(), ARROZ, 70);
            iguales(List.of(new AsignacionLote(L_ARZ_0901, 60), new AsignacionLote(L_ARZ_1015, 10)),
                    p.asignaciones(), "plan");
        });

        prueba("LIFO toma primero lo ultimo que entro", () -> {
            PlanPicking p = simular(EscenarioDemo.sistema(), new PickingLifo(), CEMENTO, 50);
            iguales(List.of(new AsignacionLote(L_CEM_1105, 40), new AsignacionLote(L_CEM_0920, 10)),
                    p.asignaciones(), "plan");
        });

        prueba("La politica solo ordena: no filtra ni reparte", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            List<VistaLote> lotes = new ArrayList<>(s.inventario().lotesDe(LECHE, NORTE));
            lotes.sort(new PickingFefo().prioridad());
            iguales(L_LEC_1001, lotes.get(0).codigo(), "el comparador ordena tambien el vencido");
            verificar(simular(s, new PickingFefo(), LECHE, 10).asignaciones().stream()
                    .noneMatch(a -> a.lote().equals(L_LEC_1001)), "pero el contexto nunca lo asigna");
        });

        prueba("Filtro de seguridad: cuarentena y recibidos no se asignan aunque los pasen", () -> {
            Lote cuarentena = Lote.almacenado("L-Q", ACEITE, NORTE, 50, HOY.minusDays(9), HOY.plusDays(90));
            cuarentena.ponerEnCuarentena();
            Lote recibido = Lote.recibido("L-R", ACEITE, NORTE, 50, HOY, HOY.plusDays(90));
            Lote bueno = Lote.almacenado("L-B", ACEITE, NORTE, 50, HOY.minusDays(1), HOY.plusDays(90));
            PlanPicking p = EscenarioDemo.sistema().picking()
                    .planificarCon(new PickingFifo(), List.of(cuarentena, recibido, bueno), 30, HOY);
            iguales(List.of(new AsignacionLote("L-B", 30)), p.asignaciones(), "solo el lote almacenado");
        });

        prueba("Contrato: si no alcanza lo indica; con cantidad invalida lanza IllegalArgumentException", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            PlanPicking p = simular(s, new PickingFifo(), ARROZ, 200);
            iguales(140, p.total(), "total cubierto");
            verificar(!p.cubreTodo(), "no deberia cubrir todo");
            for (PoliticaPicking politica : List.of(new PickingFefo(), new PickingFifo(), new PickingLifo())) {
                lanza(IllegalArgumentException.class, () -> simular(s, politica, ARROZ, 0), politica.nombre() + " con 0");
            }
        });

        prueba("Un pedido se reparte entre varios lotes (70 con dos lotes de 50)", () -> {
            Lote a = Lote.almacenado("L-A", ARROZ, NORTE, 50, HOY.minusDays(5), HOY.plusDays(60));
            Lote b = Lote.almacenado("L-B", ARROZ, NORTE, 50, HOY.minusDays(2), HOY.plusDays(60));
            PlanPicking p = EscenarioDemo.sistema().picking().planificarCon(new PickingFifo(), List.of(a, b), 70, HOY);
            iguales(List.of(new AsignacionLote("L-A", 50), new AsignacionLote("L-B", 20)), p.asignaciones(), "plan");
        });

        prueba("La politica se elige por categoria y un SKU puede tener la suya", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            ProductoSku arroz = s.inventario().sku(ARROZ).orElseThrow();
            iguales("FEFO", s.picking().politicaPara(s.inventario().sku(LECHE).orElseThrow()).nombre(), "perecible");
            iguales("FIFO", s.picking().politicaPara(arroz).nombre(), "abarrotes");
            iguales("LIFO", s.picking().politicaPara(s.inventario().sku(CEMENTO).orElseThrow()).nombre(), "materiales");
            s.picking().usarParaSku(ARROZ, new PickingLifo());
            iguales("LIFO", s.picking().politicaPara(arroz).nombre(), "la politica del SKU gana");
            iguales("FIFO", s.picking().politicaPara(s.inventario().sku(ACEITE).orElseThrow()).nombre(),
                    "el resto de la categoria no cambia");
        });

        prueba("Cambiar la estrategia en ejecucion cambia los lotes de la reserva", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            iguales("FIFO: L-ARZ-0901 x10", s.inventario().reserva("P1").orElseThrow().describir(), "antes del cambio");
            s.picking().usarParaSku(ARROZ, new PickingFefo());
            s.central().despachar(new ReservarPedido("K2", ANDREA, "P2", ARROZ, NORTE, 10));
            iguales("FEFO: L-ARZ-1015 x10", s.inventario().reserva("P2").orElseThrow().describir(), "despues del cambio");
        });

        prueba("ROP, EOQ y Min-Max calculan lo esperado", () -> {
            PerfilDemanda arroz = new PerfilDemanda(12, 4, 30);
            iguales(78, arroz.puntoDeReorden(), "ROP = 12 x 4 + 30");
            LoteEconomico eoq = new LoteEconomico(20.0, 1.5);
            iguales(342, eoq.eoq(arroz), "EOQ = raiz(2 x 4380 x 20 / 1.5)");
            verificar(eoq.evaluar(arroz, 79).isEmpty(), "EOQ: por encima del ROP no pide");
            iguales(342, eoq.evaluar(arroz, 78).orElseThrow().cantidad(), "EOQ: en el ROP pide");
            MinMax minMax = new MinMax(80, 250);
            iguales(190, minMax.evaluar(arroz, 60).orElseThrow().cantidad(), "Min-Max completa hasta el maximo");
            verificar(minMax.evaluar(arroz, 81).isEmpty(), "Min-Max: por encima del minimo no pide");
            PerfilDemanda cemento = new PerfilDemanda(8, 7, 30);
            iguales(100, new PuntoDeReorden(100).evaluar(cemento, 86).orElseThrow().cantidad(), "ROP pide la cantidad fija");
            verificar(new PuntoDeReorden(100).evaluar(cemento, 87).isEmpty(), "ROP: por encima no pide");
        });

        prueba("Parametros invalidos se rechazan al construir (no en medio de una operacion)", () -> {
            lanza(IllegalArgumentException.class, () -> new MinMax(100, 50), "min > max");
            lanza(IllegalArgumentException.class, () -> new LoteEconomico(0, 1), "costo 0");
            lanza(IllegalArgumentException.class, () -> new PuntoDeReorden(-1), "cantidad negativa");
            lanza(IllegalArgumentException.class, () -> new PerfilDemanda(Double.NaN, 1, 1), "demanda NaN");
            lanza(IllegalArgumentException.class, () -> new PerfilDemanda(1, Integer.MAX_VALUE, 1), "lead time absurdo");
        });

        prueba("Reposicion genera la orden cuando la posicion cae y no la duplica", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", LECHE, NORTE, 100));
            iguales(1, s.reposicion().ordenes().size(), "una orden");
            OrdenCompra oc = s.reposicion().ordenes().get(0);
            iguales(190, oc.cantidad(), "Min-Max: 250 - 60");
            s.central().despachar(new ReservarPedido("K2", ANDREA, "P2", LECHE, NORTE, 10));
            iguales(1, s.reposicion().ordenes().size(), "lo pendiente cuenta: no se vuelve a pedir");
        });

        prueba("Recibir mercaderia cierra la orden pendiente", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", LECHE, NORTE, 100));
            s.central().despachar(new RecibirLote("K2", ANDREA, LECHE, NORTE, "L-LEC-X", 190, LocalDate.of(2026, 12, 31)));
            iguales(OrdenCompra.Estado.RECIBIDA, s.reposicion().ordenes().get(0).estado(), "estado de la orden");
            iguales(1, s.reposicion().ordenes().size(), "no se crea otra orden");
        });

        prueba("Un SKU descontinuado no genera ordenes de compra", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new AjustarStock("K1", LUCIA, GALLETAS, SUR, L_GAL_0601, -39, "merma"));
            iguales(1, s.inventario().stockUtilizable(GALLETAS, SUR), "queda 1 unidad (por debajo del ROP)");
            iguales(0, s.reposicion().ordenes().size(), "ordenes");
        });
    }
}
