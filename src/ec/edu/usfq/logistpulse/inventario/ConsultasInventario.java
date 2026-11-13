package ec.edu.usfq.logistpulse.inventario;

import ec.edu.usfq.logistpulse.dominio.Bodega;
import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.state.VistaLote;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lado de <b>lectura</b> del inventario (I de SOLID). Lo usan la cadena de validación,
 * el servicio de reposición y los reportes, que no necesitan modificar nada.
 * Todo lo que devuelve es de solo lectura.
 */
public interface ConsultasInventario {

    LocalDate hoy();

    Optional<ProductoSku> sku(String codigo);

    Optional<Bodega> bodega(String codigo);

    List<Bodega> bodegas();

    Optional<VistaLote> lote(String codigoLote, String bodega);

    List<VistaLote> lotes();

    /** Todos los lotes de un SKU en una bodega, en cualquier estado. */
    List<VistaLote> lotesDe(String sku, String bodega);

    Optional<Reserva> reserva(String pedido);

    /** Unidades sin comprometer del SKU en la bodega, en cualquier estado (incluye vencidas y en cuarentena). */
    int stockLibre(String sku, String bodega);

    /** Unidades que la estrategia de picking puede tomar hoy (ALMACENADO, no vencidas). */
    int stockAsignable(String sku, String bodega);

    /** Unidades libres que cuentan para reponer (RECIBIDO/ALMACENADO/RESERVADO, no vencidas). */
    int stockUtilizable(String sku, String bodega);

    int fisicoEnBodega(String bodega);

    int reservadoEnBodega(String bodega);

    /**
     * La regla única de capacidad: la usan tanto la cadena (eslabón Capacidad)
     * como el propio inventario al ejecutar, así no hay dos versiones de la regla.
     *
     * @return el motivo si NO cabe; vacío si cabe
     */
    Optional<String> verificarEspacio(String bodega, TipoEspacio tipo, int unidades);
}
