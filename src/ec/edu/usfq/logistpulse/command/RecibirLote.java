package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.Espacio;
import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>CONCRETE COMMAND:</b> registra un lote que llega al muelle (queda RECIBIDO).
 * Irreversible: un error de recepción se corrige con un ajuste o una devolución.
 */
public final class RecibirLote extends ComandoBase {

    private final String sku;
    private final String bodega;
    private final String lote;
    private final int unidades;
    private final LocalDate vencimiento;

    public RecibirLote(String clave, Actor actor, String sku, String bodega, String lote,
                       int unidades, LocalDate vencimiento) {
        super(clave, SolicitudMovimiento.para(TipoMovimiento.RECIBIR, actor, sku, bodega)
                .descripcion(String.format("RECIBIR %d x %s lote %s en %s", unidades, sku, lote, bodega))
                .autorizados(Rol.OPERADOR, Rol.SUPERVISOR)
                .skuActivo()
                .loteNuevo(lote)
                .ocupa(Espacio.fisico(bodega, unidades))
                .cantidad(unidades)
                .construir());
        this.sku = sku;
        this.bodega = bodega;
        this.lote = lote;
        this.unidades = unidades;
        this.vencimiento = Objects.requireNonNull(vencimiento, "vencimiento");
    }

    @Override
    public void ejecutar(OperacionesInventario operaciones) {
        operaciones.recibirLote(lote, sku, bodega, unidades, vencimiento);
    }

    @Override
    public Optional<ComandoInventario> inverso(Actor solicitante) {
        return Optional.empty();
    }

    @Override
    protected String datosExtra() {
        return vencimiento.toString();
    }
}
