package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;

import java.util.Optional;

/**
 * <b>CONCRETE COMMAND:</b> ubica en el rack un lote recibido (RECIBIDO -> ALMACENADO).
 * Irreversible: volver al muelle no es una transición válida.
 */
public final class AlmacenarLote extends ComandoBase {

    private final String bodega;
    private final String lote;

    public AlmacenarLote(String clave, Actor actor, String sku, String bodega, String lote) {
        super(clave, SolicitudMovimiento.para(TipoMovimiento.ALMACENAR, actor, sku, bodega)
                .descripcion(String.format("ALMACENAR lote %s (%s) en %s", lote, sku, bodega))
                .autorizados(Rol.OPERADOR, Rol.SUPERVISOR)
                .loteExistente(lote, 0, false)
                .construir());
        this.bodega = bodega;
        this.lote = lote;
    }

    @Override
    public void ejecutar(OperacionesInventario operaciones) {
        operaciones.almacenar(lote, bodega);
    }

    @Override
    public Optional<ComandoInventario> inverso(Actor solicitante) {
        return Optional.empty();
    }
}
