package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;
import ec.edu.usfq.logistpulse.inventario.Reserva;

import java.util.Optional;

/**
 * <b>CONCRETE COMMAND — Release:</b> cancela la reserva de un pedido y devuelve sus
 * unidades al stock libre. Solo su dueño o un supervisor. Su inverso vuelve a reservar.
 */
public final class LiberarReserva extends ComandoBase {

    private final String pedido;
    private final String sku;
    private final String bodega;
    private final int unidades;

    public LiberarReserva(String clave, Actor actor, Reserva reserva) {
        super(clave, SolicitudMovimiento.para(TipoMovimiento.LIBERAR, actor, reserva.sku(), reserva.bodega())
                .descripcion(String.format("LIBERAR reserva %s (%d x %s) en %s",
                        reserva.pedido(), reserva.total(), reserva.sku(), reserva.bodega()))
                .autorizados(Rol.OPERADOR, Rol.SUPERVISOR)
                .reservaActiva(reserva.pedido())
                .referencia(reserva.pedido())
                .cantidad(reserva.total())
                .construir());
        this.pedido = reserva.pedido();
        this.sku = reserva.sku();
        this.bodega = reserva.bodega();
        this.unidades = reserva.total();
    }

    @Override
    public void ejecutar(OperacionesInventario operaciones) {
        operaciones.liberarReserva(pedido, actor());
    }

    @Override
    public Optional<ComandoInventario> inverso(Actor solicitante) {
        return Optional.of(new ReservarPedido(claveCompensacion(), solicitante, pedido, sku, bodega, unidades));
    }
}
