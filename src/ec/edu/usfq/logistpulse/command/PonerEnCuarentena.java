package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;

import java.util.Objects;
import java.util.Optional;

/**
 * <b>CONCRETE COMMAND:</b> calidad aísla un lote. Su inverso es {@link LiberarCuarentena}.
 */
public final class PonerEnCuarentena extends ComandoBase {

    private final String sku;
    private final String bodega;
    private final String lote;
    private final String motivo;

    public PonerEnCuarentena(String clave, Actor actor, String sku, String bodega, String lote, String motivo) {
        super(clave, SolicitudMovimiento.para(TipoMovimiento.CUARENTENA, actor, sku, bodega)
                .descripcion(String.format("CUARENTENA lote %s (%s) en %s [%s]", lote, sku, bodega, motivo))
                .autorizados(Rol.CALIDAD, Rol.SUPERVISOR)
                .loteExistente(lote, 0, false)
                .construir());
        this.sku = sku;
        this.bodega = bodega;
        this.lote = lote;
        this.motivo = Objects.requireNonNull(motivo, "motivo");
    }

    @Override
    public void ejecutar(OperacionesInventario operaciones) {
        operaciones.ponerEnCuarentena(lote, bodega);
    }

    @Override
    public Optional<ComandoInventario> inverso(Actor solicitante) {
        return Optional.of(new LiberarCuarentena(claveCompensacion(), solicitante, sku, bodega, lote));
    }

    @Override
    protected String datosExtra() {
        return motivo;
    }
}
