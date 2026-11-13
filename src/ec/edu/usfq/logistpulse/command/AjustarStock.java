package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.Espacio;
import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Cantidades;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;

import java.util.Objects;
import java.util.Optional;

/**
 * <b>CONCRETE COMMAND — Adjust:</b> corrige las unidades físicas de un lote (conteo
 * cíclico, merma, daño). Solo supervisión o calidad. Su inverso es el mismo ajuste
 * con signo contrario.
 */
public final class AjustarStock extends ComandoBase {

    private final String sku;
    private final String bodega;
    private final String lote;
    private final int delta;
    private final String razon;

    public AjustarStock(String clave, Actor actor, String sku, String bodega, String lote, int delta, String razon) {
        super(clave, solicitud(actor, sku, bodega, lote, Cantidades.exigirAjuste(delta), razon));
        this.sku = sku;
        this.bodega = bodega;
        this.lote = lote;
        this.delta = delta;
        this.razon = Objects.requireNonNull(razon, "razon");
    }

    /** Un ajuste negativo consume unidades libres; uno positivo ocupa espacio. */
    private static SolicitudMovimiento solicitud(Actor actor, String sku, String bodega, String lote,
                                                 int delta, String razon) {
        Objects.requireNonNull(razon, "razon");
        SolicitudMovimiento.Constructor c = SolicitudMovimiento.para(TipoMovimiento.AJUSTAR, actor, sku, bodega)
                .descripcion(String.format("AJUSTAR %+d x %s lote %s en %s [%s]", delta, sku, lote, bodega, razon))
                .autorizados(Rol.SUPERVISOR, Rol.CALIDAD)
                .cantidad(delta);
        if (delta < 0) {
            c.consumeLibres(-delta).loteExistente(lote, -delta, false);
        } else {
            c.loteExistente(lote, 0, false).ocupa(Espacio.fisico(bodega, delta));
        }
        return c.construir();
    }

    @Override
    public void ejecutar(OperacionesInventario operaciones) {
        operaciones.ajustar(lote, bodega, delta);
    }

    @Override
    public Optional<ComandoInventario> inverso(Actor solicitante) {
        return Optional.of(new AjustarStock(claveCompensacion(), solicitante, sku, bodega, lote, -delta,
                "reversa de " + id()));
    }

    @Override
    protected String datosExtra() {
        return razon;
    }
}
