package ec.edu.usfq.logistpulse.strategy.picking;

import java.util.Objects;

/**
 * Cuántas unidades se toman de un lote concreto.
 */
public record AsignacionLote(String lote, int cantidad) {

    public AsignacionLote {
        Objects.requireNonNull(lote, "lote");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("Una asignacion necesita cantidad positiva");
        }
    }

    @Override
    public String toString() {
        return lote + " x" + cantidad;
    }
}
