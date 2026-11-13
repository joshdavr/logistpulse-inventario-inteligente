package ec.edu.usfq.logistpulse;

import ec.edu.usfq.logistpulse.chain.CadenaValidacion;
import ec.edu.usfq.logistpulse.chain.EslabonValidacion;
import ec.edu.usfq.logistpulse.command.CentralDeComandos;
import ec.edu.usfq.logistpulse.command.DecisionRegistrada;
import ec.edu.usfq.logistpulse.dominio.Categoria;
import ec.edu.usfq.logistpulse.inventario.Inventario;
import ec.edu.usfq.logistpulse.observer.BusEventos;
import ec.edu.usfq.logistpulse.observer.Ledger;
import ec.edu.usfq.logistpulse.observer.PulseFleet;
import ec.edu.usfq.logistpulse.observer.StockCambiado;
import ec.edu.usfq.logistpulse.strategy.picking.PickingFefo;
import ec.edu.usfq.logistpulse.strategy.picking.PickingFifo;
import ec.edu.usfq.logistpulse.strategy.picking.PickingLifo;
import ec.edu.usfq.logistpulse.strategy.picking.ServicioPicking;
import ec.edu.usfq.logistpulse.strategy.reposicion.LoteEconomico;
import ec.edu.usfq.logistpulse.strategy.reposicion.MinMax;
import ec.edu.usfq.logistpulse.strategy.reposicion.PuntoDeReorden;
import ec.edu.usfq.logistpulse.strategy.reposicion.ServicioReposicion;

import java.time.Clock;
import java.util.List;

/**
 * Raíz de composición: el único lugar donde se crean las piezas concretas y se
 * conectan entre sí (D de SOLID). Ninguna otra clase hace {@code new} de una política,
 * de un eslabón o del inventario.
 *
 * <pre>
 *  Picking por categoria:     PERECIBLE=FEFO   ABARROTES=FIFO        MATERIALES=LIFO
 *  Reposicion por categoria:  PERECIBLE=MinMax ABARROTES=EOQ         MATERIALES=ROP
 *  Cadena:                    Permisos -> SKU -> Stock -> Lote -> Capacidad
 *  Suscriptores:              StockCambiado -> Reposicion, PULSEFLEET, Ledger
 *                             DecisionRegistrada -> Ledger
 * </pre>
 */
public record SistemaLogistPulse(Clock reloj, BusEventos eventos, ServicioPicking picking,
                                 Inventario inventario, ServicioReposicion reposicion, PulseFleet pulseFleet,
                                 Ledger ledger, EslabonValidacion cadena, CentralDeComandos central) {

    public static SistemaLogistPulse crear(Clock reloj) {
        BusEventos eventos = new BusEventos();

        ServicioPicking picking = new ServicioPicking(new PickingFifo());
        picking.usarParaCategoria(Categoria.PERECIBLE, new PickingFefo());
        picking.usarParaCategoria(Categoria.ABARROTES, new PickingFifo());
        picking.usarParaCategoria(Categoria.MATERIALES, new PickingLifo());

        Inventario inventario = new Inventario(reloj, eventos, picking);

        ServicioReposicion reposicion = new ServicioReposicion(inventario, new MinMax(20, 100));
        reposicion.usarParaCategoria(Categoria.PERECIBLE, new MinMax(80, 250));
        reposicion.usarParaCategoria(Categoria.ABARROTES, new LoteEconomico(20.0, 1.5));
        reposicion.usarParaCategoria(Categoria.MATERIALES, new PuntoDeReorden(100));

        PulseFleet pulseFleet = new PulseFleet(List.of("AMR-01", "AMR-02", "AMR-03"), PulseFleet.reglasEstandar());
        Ledger ledger = new Ledger();
        eventos.suscribir(StockCambiado.class, reposicion);
        eventos.suscribir(StockCambiado.class, pulseFleet);
        eventos.suscribir(StockCambiado.class, ledger.suscriptorMovimientos());
        eventos.suscribir(DecisionRegistrada.class, ledger.suscriptorDecisiones());

        EslabonValidacion cadena = CadenaValidacion.estandar();
        CentralDeComandos central = new CentralDeComandos(inventario, inventario, cadena, eventos, reloj);

        return new SistemaLogistPulse(reloj, eventos, picking, inventario, reposicion, pulseFleet, ledger, cadena, central);
    }
}
