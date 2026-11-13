package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.Espacio;
import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;
import ec.edu.usfq.logistpulse.inventario.Reserva;

import java.util.Optional;

/**
 * <b>CONCRETE COMMAND — Reserve:</b> el {@code POST /reservations}.
 *
 * <p>No dice de qué lote sacar: eso lo decide la estrategia de picking del SKU, y el
 * pedido se puede repartir entre varios lotes. Al ejecutarse guarda la reserva,
 * porque su inverso necesita saber exactamente qué lotes liberar. Quien la crea
 * queda como dueño.</p>
 */
public final class ReservarPedido extends ComandoBase {

    private final String pedido;
    private final String sku;
    private final String bodega;
    private final int unidades;
    private Reserva reserva;

    public ReservarPedido(String clave, Actor actor, String pedido, String sku, String bodega, int unidades) {
        super(clave, SolicitudMovimiento.para(TipoMovimiento.RESERVAR, actor, sku, bodega)
                .descripcion(String.format("RESERVAR %d x %s en %s para %s", unidades, sku, bodega, pedido))
                .autorizados(Rol.OPERADOR, Rol.SUPERVISOR)
                .skuActivo()
                .consumeLibres(unidades)
                .asignables(unidades)
                .pedidoSinReservaActiva(pedido)
                .ocupa(Espacio.alistamiento(bodega, unidades))
                .referencia(pedido)
                .cantidad(unidades)
                .construir());
        this.pedido = pedido;
        this.sku = sku;
        this.bodega = bodega;
        this.unidades = unidades;
    }

    @Override
    public void ejecutar(OperacionesInventario operaciones) {
        reserva = operaciones.reservar(pedido, sku, bodega, unidades, actor());
    }

    /** La reserva creada (vacío si el comando todavía no se ejecutó). */
    public Optional<Reserva> reserva() {
        return Optional.ofNullable(reserva);
    }

    @Override
    public Optional<ComandoInventario> inverso(Actor solicitante) {
        return reserva().map(r -> new LiberarReserva(claveCompensacion(), solicitante, r));
    }
}
