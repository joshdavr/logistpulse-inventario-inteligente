package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.Espacio;
import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;

import java.util.Optional;

/**
 * <b>CONCRETE COMMAND — Transfer:</b> mueve unidades libres de un lote entre bodegas.
 * Su inverso es la misma transferencia en sentido contrario.
 */
public final class TransferirStock extends ComandoBase {

    private final String sku;
    private final String lote;
    private final String origen;
    private final String destino;
    private final int unidades;

    public TransferirStock(String clave, Actor actor, String sku, String lote,
                           String origen, String destino, int unidades) {
        super(clave, SolicitudMovimiento.para(TipoMovimiento.TRANSFERIR, actor, sku, origen)
                .descripcion(String.format("TRANSFERIR %d x %s lote %s %s -> %s", unidades, sku, lote, origen, destino))
                .autorizados(Rol.OPERADOR, Rol.SUPERVISOR)
                .consumeLibres(unidades)
                .loteExistente(lote, unidades, true)
                .ocupa(Espacio.fisico(destino, unidades))
                .destino(destino)
                .cantidad(unidades)
                .construir());
        this.sku = sku;
        this.lote = lote;
        this.origen = origen;
        this.destino = destino;
        this.unidades = unidades;
    }

    @Override
    public void ejecutar(OperacionesInventario operaciones) {
        operaciones.transferir(lote, origen, destino, unidades);
    }

    @Override
    public Optional<ComandoInventario> inverso(Actor solicitante) {
        return Optional.of(new TransferirStock(claveCompensacion(), solicitante, sku, lote, destino, origen, unidades));
    }
}
