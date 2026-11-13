package ec.edu.usfq.logistpulse.strategy.picking;

import ec.edu.usfq.logistpulse.state.VistaLote;

import java.util.Comparator;

/**
 * <b>CONCRETE STRATEGY — FEFO</b> (First Expired, First Out):
 * sale primero el lote que vence antes. Ideal para perecibles.
 */
public final class PickingFefo implements PoliticaPicking {

    @Override
    public String nombre() {
        return "FEFO";
    }

    @Override
    public Comparator<VistaLote> prioridad() {
        return Comparator.comparing(VistaLote::vencimiento)
                .thenComparing(VistaLote::fechaIngreso)
                .thenComparing(VistaLote::codigo);
    }
}
