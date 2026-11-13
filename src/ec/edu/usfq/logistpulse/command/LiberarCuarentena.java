package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;

import java.util.Optional;

/**
 * <b>CONCRETE COMMAND:</b> calidad aprueba un lote en cuarentena y vuelve al rack.
 * Su inverso es {@link PonerEnCuarentena}.
 */
public final class LiberarCuarentena extends ComandoBase {

    private final String sku;
    private final String bodega;
    private final String lote;

    public LiberarCuarentena(String clave, Actor actor, String sku, String bodega, String lote) {
        super(clave, SolicitudMovimiento.para(TipoMovimiento.LIBERAR_CUARENTENA, actor, sku, bodega)
                .descripcion(String.format("LIBERAR CUARENTENA lote %s (%s) en %s", lote, sku, bodega))
                .autorizados(Rol.CALIDAD, Rol.SUPERVISOR)
                .loteExistente(lote, 0, false)
                .construir());
        this.sku = sku;
        this.bodega = bodega;
        this.lote = lote;
    }

    @Override
    public void ejecutar(OperacionesInventario operaciones) {
        operaciones.liberarCuarentena(lote, bodega);
    }

    @Override
    public Optional<ComandoInventario> inverso(Actor solicitante) {
        return Optional.of(new PonerEnCuarentena(claveCompensacion(), solicitante, sku, bodega, lote,
                "reversa de " + id()));
    }
}
