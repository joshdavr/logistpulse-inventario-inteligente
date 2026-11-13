package ec.edu.usfq.logistpulse.strategy.reposicion;

import ec.edu.usfq.logistpulse.dominio.Categoria;
import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;
import ec.edu.usfq.logistpulse.observer.StockCambiado;
import ec.edu.usfq.logistpulse.observer.Suscriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Usa dos patrones a la vez:
 * <ul>
 *   <li><b>Strategy (CONTEXT — Replenishment):</b> elige la política de reposición por SKU o por categoría.</li>
 *   <li><b>Observer (CONCRETE SUBSCRIBER — Reposición):</b> se entera de cada {@link StockCambiado}
 *       sin que el inventario lo conozca, y decide si hay que pedir.</li>
 * </ul>
 *
 * <p>Solo lee el inventario ({@link ConsultasInventario}). Posición de inventario =
 * stock utilizable (no vencido, fuera de cuarentena) + órdenes pendientes; contar lo
 * pendiente evita pedir dos veces lo mismo.</p>
 */
public class ServicioReposicion implements Suscriptor<StockCambiado> {

    private final ConsultasInventario inventario;
    private final Map<Categoria, PoliticaReposicion> porCategoria = new EnumMap<>(Categoria.class);
    private final Map<String, PoliticaReposicion> porSku = new HashMap<>();
    private final PoliticaReposicion porDefecto;
    private final List<OrdenCompra> ordenes = new ArrayList<>();

    public ServicioReposicion(ConsultasInventario inventario, PoliticaReposicion porDefecto) {
        this.inventario = Objects.requireNonNull(inventario, "inventario");
        this.porDefecto = Objects.requireNonNull(porDefecto, "porDefecto");
    }

    public void usarParaCategoria(Categoria categoria, PoliticaReposicion politica) {
        porCategoria.put(Objects.requireNonNull(categoria), Objects.requireNonNull(politica));
    }

    public void usarParaSku(String sku, PoliticaReposicion politica) {
        porSku.put(Objects.requireNonNull(sku), Objects.requireNonNull(politica));
    }

    public PoliticaReposicion politicaPara(ProductoSku producto) {
        PoliticaReposicion propia = porSku.get(producto.codigo());
        return propia != null ? propia : porCategoria.getOrDefault(producto.categoria(), porDefecto);
    }

    @Override
    public String nombre() {
        return "Reposicion";
    }

    @Override
    public void alRecibir(StockCambiado evento) {
        ProductoSku producto = inventario.sku(evento.sku()).orElse(null);
        if (producto == null || !producto.activo()) {
            return; // lo descontinuado no se repone
        }
        if (evento.movimiento() == TipoMovimiento.RECIBIR) {
            cerrarOrdenPendiente(evento.sku(), evento.bodega());
        }
        int posicion = posicionInventario(evento.sku(), evento.bodega());
        PoliticaReposicion politica = politicaPara(producto);
        politica.evaluar(producto.perfil(), posicion).ifPresent(s -> ordenes.add(new OrdenCompra(
                String.format("OC-%03d", ordenes.size() + 1), evento.sku(), evento.bodega(),
                s.cantidad(), politica.nombre(), s.razon())));
    }

    public int posicionInventario(String sku, String bodega) {
        long posicion = (long) inventario.stockUtilizable(sku, bodega) + pendiente(sku, bodega);
        return (int) Math.min(posicion, Integer.MAX_VALUE);
    }

    private long pendiente(String sku, String bodega) {
        return ordenes.stream()
                .filter(o -> o.pendiente() && o.sku().equals(sku) && o.bodega().equals(bodega))
                .mapToLong(OrdenCompra::cantidad)
                .sum();
    }

    private void cerrarOrdenPendiente(String sku, String bodega) {
        ordenes.stream()
                .filter(o -> o.pendiente() && o.sku().equals(sku) && o.bodega().equals(bodega))
                .findFirst()
                .ifPresent(OrdenCompra::marcarRecibida);
    }

    public List<OrdenCompra> ordenes() {
        return Collections.unmodifiableList(ordenes);
    }
}
