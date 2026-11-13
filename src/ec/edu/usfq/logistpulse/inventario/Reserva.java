package ec.edu.usfq.logistpulse.inventario;

import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.strategy.picking.AsignacionLote;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Unidades apartadas para un pedido, repartidas en uno o más lotes según la
 * política de picking. Recuerda quién la creó: solo su dueño o un supervisor
 * pueden liberarla o despacharla.
 */
public class Reserva {

    public enum Estado { ACTIVA, LIBERADA, DESPACHADA }

    private final String pedido;
    private final String sku;
    private final String bodega;
    private final String politica;
    private final Actor dueno;
    private final List<AsignacionLote> lineas;
    private Estado estado = Estado.ACTIVA;

    Reserva(String pedido, String sku, String bodega, String politica, Actor dueno, List<AsignacionLote> lineas) {
        this.pedido = Objects.requireNonNull(pedido);
        this.sku = Objects.requireNonNull(sku);
        this.bodega = Objects.requireNonNull(bodega);
        this.politica = Objects.requireNonNull(politica);
        this.dueno = Objects.requireNonNull(dueno);
        this.lineas = List.copyOf(lineas);
    }

    void cambiarA(Estado nuevo) {
        estado = nuevo;
    }

    /**
     * Regla única de propiedad: la usan la cadena (eslabón Permisos) y el inventario.
     * Puede gestionar la reserva su dueño o cualquier supervisor.
     */
    public boolean puedeGestionar(Actor actor) {
        return dueno.esLaMismaQue(actor) || (actor != null && actor.esSupervisor());
    }

    public boolean activa() {
        return estado == Estado.ACTIVA;
    }

    public int total() {
        return lineas.stream().mapToInt(AsignacionLote::cantidad).sum();
    }

    public String pedido() { return pedido; }
    public String sku() { return sku; }
    public String bodega() { return bodega; }
    public String politica() { return politica; }
    public Actor dueno() { return dueno; }
    public List<AsignacionLote> lineas() { return lineas; }
    public Estado estado() { return estado; }

    public String describir() {
        return politica + ": " + lineas.stream().map(AsignacionLote::toString).collect(Collectors.joining(", "));
    }
}
