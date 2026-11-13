package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;
import ec.edu.usfq.logistpulse.inventario.Reserva;

import java.util.Optional;

/**
 * <b>CONCRETE COMMAND:</b> despacha lo reservado para un pedido (el stock sale de la
 * bodega). Solo su dueño o un supervisor. Irreversible: el camión ya salió.
 */
public final class DespacharPedido extends ComandoBase {

    private final String pedido;

    public DespacharPedido(String clave, Actor actor, Reserva reserva) {
        super(clave, SolicitudMovimiento.para(TipoMovimiento.DESPACHAR, actor, reserva.sku(), reserva.bodega())
                .descripcion(String.format("DESPACHAR pedido %s (%d x %s) desde %s",
                        reserva.pedido(), reserva.total(), reserva.sku(), reserva.bodega()))
                .autorizados(Rol.OPERADOR, Rol.SUPERVISOR)
                .reservaActiva(reserva.pedido())
                .referencia(reserva.pedido())
                .cantidad(reserva.total())
                .construir());
        this.pedido = reserva.pedido();
    }

    @Override
    public void ejecutar(OperacionesInventario operaciones) {
        operaciones.despachar(pedido, actor());
    }

    @Override
    public Optional<ComandoInventario> inverso(Actor solicitante) {
        return Optional.empty();
    }
}
