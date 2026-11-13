package ec.edu.usfq.logistpulse.strategy.picking;

import ec.edu.usfq.logistpulse.state.VistaLote;

import java.util.Comparator;

/**
 * <b>CONCRETE STRATEGY — FIFO</b> (First In, First Out):
 * sale primero el lote que entró antes a la bodega.
 */
public final class PickingFifo implements PoliticaPicking {

    @Override
    public String nombre() {
        return "FIFO";
    }

    @Override
    public Comparator<VistaLote> prioridad() {
        return Comparator.comparing(VistaLote::fechaIngreso).thenComparing(VistaLote::codigo);
    }
}
