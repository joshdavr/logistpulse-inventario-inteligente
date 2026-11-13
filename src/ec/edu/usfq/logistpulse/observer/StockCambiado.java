package ec.edu.usfq.logistpulse.observer;

import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;

import java.time.Instant;
import java.util.Objects;

/**
 * El evento <b>StockChanged</b> de la diapositiva: el inventario lo publica por cada
 * lote que cambió, sin saber quién lo va a escuchar.
 *
 * @param referencia pedido o ruta del movimiento (puede ser null)
 */
public record StockCambiado(Instant momento, TipoMovimiento movimiento, String sku, String bodega,
                            String lote, int deltaFisico, int deltaReservado,
                            String estadoAntes, String estadoDespues, String referencia) implements Evento {

    public StockCambiado {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(movimiento, "movimiento");
    }

    public boolean cambioDeEstado() {
        return !estadoAntes.equals(estadoDespues);
    }

    public String describir() {
        StringBuilder sb = new StringBuilder(String.format("%-22s", lote + "@" + bodega));
        sb.append(cambioDeEstado() ? estadoAntes + " -> " + estadoDespues : estadoDespues);
        if (deltaFisico != 0) {
            sb.append(String.format("  fisico %+d", deltaFisico));
        }
        if (deltaReservado != 0) {
            sb.append(String.format("  reservado %+d", deltaReservado));
        }
        return sb.toString();
    }
}
