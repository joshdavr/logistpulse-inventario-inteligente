package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;

import java.util.Optional;

/**
 * <b>Eslabón 1 (CONCRETE HANDLER) — SKU:</b> el SKU existe en el catálogo y, si la
 * operación lo exige (recibir o reservar), sigue activo. El stock de un SKU
 * descontinuado sí se puede despachar, ajustar o mover.
 */
public class FiltroSku extends EslabonValidacion {

    @Override
    public String nombre() {
        return "SKU";
    }

    @Override
    protected Optional<String> revisar(SolicitudMovimiento s, ConsultasInventario inventario) {
        Optional<ProductoSku> producto = inventario.sku(s.sku());
        if (producto.isEmpty()) {
            return Optional.of("El SKU " + s.sku() + " no existe en el catalogo");
        }
        if (s.requiereSkuActivo() && !producto.get().activo()) {
            return Optional.of("El SKU " + s.sku() + " esta descontinuado: no admite recepciones ni reservas nuevas");
        }
        return Optional.empty();
    }
}
