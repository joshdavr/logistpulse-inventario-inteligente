package ec.edu.usfq.logistpulse.state;

import java.time.LocalDate;
import java.util.List;

/**
 * Vista de solo lectura de un lote. Es lo que el inventario entrega hacia afuera,
 * para que nadie pueda modificar un lote sin pasar por el inventario.
 */
public interface VistaLote {

    String codigo();

    String sku();

    String bodega();

    LocalDate fechaIngreso();

    LocalDate vencimiento();

    int fisico();

    int reservado();

    /** fisico - reservado. */
    int disponible();

    String nombreEstado();

    boolean vencidoEn(LocalDate hoy);

    /** True si la estrategia de picking puede tomar unidades de este lote hoy. */
    boolean asignableEn(LocalDate hoy);

    /** True si sus unidades libres cuentan para calcular la reposición. */
    boolean utilizableEn(LocalDate hoy);

    /** Historial de cambios de estado, por ejemplo "ALMACENADO -> RESERVADO". */
    List<String> transiciones();
}
