package ec.edu.usfq.logistpulse.strategy.picking;

import ec.edu.usfq.logistpulse.state.VistaLote;

import java.util.Comparator;

/**
 * <b>CONCRETE STRATEGY — LIFO</b> (Last In, First Out):
 * sale primero lo último que llegó. Útil para material apilado y no perecible.
 */
public final class PickingLifo implements PoliticaPicking {

    @Override
    public String nombre() {
        return "LIFO";
    }

    @Override
    public Comparator<VistaLote> prioridad() {
        return Comparator.comparing(VistaLote::fechaIngreso, Comparator.reverseOrder())
                .thenComparing(VistaLote::codigo);
    }
}
