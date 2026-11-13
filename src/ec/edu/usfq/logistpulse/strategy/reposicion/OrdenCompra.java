package ec.edu.usfq.logistpulse.strategy.reposicion;

import java.util.Objects;

/**
 * Orden de compra sugerida por el servicio de reposición.
 */
public class OrdenCompra {

    public enum Estado { PENDIENTE, RECIBIDA }

    private final String id;
    private final String sku;
    private final String bodega;
    private final int cantidad;
    private final String politica;
    private final String razon;
    private Estado estado = Estado.PENDIENTE;

    OrdenCompra(String id, String sku, String bodega, int cantidad, String politica, String razon) {
        this.id = Objects.requireNonNull(id);
        this.sku = Objects.requireNonNull(sku);
        this.bodega = Objects.requireNonNull(bodega);
        this.cantidad = cantidad;
        this.politica = Objects.requireNonNull(politica);
        this.razon = Objects.requireNonNull(razon);
    }

    void marcarRecibida() {
        estado = Estado.RECIBIDA;
    }

    public boolean pendiente() {
        return estado == Estado.PENDIENTE;
    }

    public String id() { return id; }
    public String sku() { return sku; }
    public String bodega() { return bodega; }
    public int cantidad() { return cantidad; }
    public String politica() { return politica; }
    public String razon() { return razon; }
    public Estado estado() { return estado; }

    public String describir() {
        return String.format("%s %-9s pedir %4d x %s a %s (%s: %s)",
                id, estado, cantidad, sku, bodega, politica, razon);
    }
}
