package ec.edu.usfq.logistpulse.strategy.picking;

import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.state.VistaLote;

import java.time.LocalDate;
import java.util.List;

/**
 * Abstracción del contexto de picking. El inventario depende de esta interfaz y no
 * de una política concreta (D de SOLID): nunca hace {@code new PickingFefo()}.
 */
public interface AsignadorLotes {

    /**
     * Decide de qué lotes sale un pedido.
     *
     * @param candidatos lotes del SKU en la bodega; los que no se pueden asignar hoy
     *                   se descartan aquí mismo (filtro de seguridad)
     * @return nunca null; si no alcanza, {@link PlanPicking#cubreTodo()} es false
     * @throws IllegalArgumentException si la cantidad no está entre 1 y {@code Cantidades.MAXIMO}
     */
    PlanPicking planificar(ProductoSku producto, List<? extends VistaLote> candidatos, int cantidad, LocalDate hoy);
}
