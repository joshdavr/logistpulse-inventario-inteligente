package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;

import java.util.Optional;

/**
 * <b>Eslabón 2 (CONCRETE HANDLER) — Stock:</b> ¿hay suficientes unidades libres del SKU
 * en la bodega? Cuenta todo lo que no está comprometido, sirva o no; si sirve
 * (vencido, cuarentena) lo decide el eslabón Lote.
 *
 * <p>Atiende solo las solicitudes que declaran consumir unidades libres.</p>
 */
public class FiltroStock extends EslabonValidacion {

    @Override
    public String nombre() {
        return "Stock";
    }

    @Override
    protected boolean atiende(SolicitudMovimiento s) {
        return s.consumeLibres() > 0;
    }

    @Override
    protected Optional<String> revisar(SolicitudMovimiento s, ConsultasInventario inventario) {
        if (inventario.bodega(s.bodega()).isEmpty()) {
            return Optional.of("La bodega " + s.bodega() + " no existe");
        }
        int libres = inventario.stockLibre(s.sku(), s.bodega());
        if (libres < s.consumeLibres()) {
            return Optional.of("Stock insuficiente de " + s.sku() + " en " + s.bodega()
                    + ": se piden " + s.consumeLibres() + " y hay " + libres + " libres");
        }
        return Optional.empty();
    }
}
