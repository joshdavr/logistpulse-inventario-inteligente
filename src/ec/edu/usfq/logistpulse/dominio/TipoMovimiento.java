package ec.edu.usfq.logistpulse.dominio;

/**
 * Todas las operaciones que LOGISTPULSE modela como comandos.
 */
public enum TipoMovimiento {
    /** Llega un lote nuevo al muelle. */
    RECIBIR,
    /** El lote pasa del muelle al rack (putaway). */
    ALMACENAR,
    /** Se apartan unidades para un pedido; la estrategia de picking elige los lotes. */
    RESERVAR,
    /** Se cancela una reserva y las unidades vuelven a estar libres. */
    LIBERAR,
    /** Sale el pedido reservado. */
    DESPACHAR,
    /** Corrección de unidades físicas (conteo, merma, daño). */
    AJUSTAR,
    /** Movimiento de unidades libres entre bodegas. */
    TRANSFERIR,
    /** Se aísla un lote por calidad. */
    CUARENTENA,
    /** Calidad aprueba el lote y vuelve al rack. */
    LIBERAR_CUARENTENA
}
