package ec.edu.usfq.logistpulse.command;

/**
 * Cómo terminó un intento de ejecutar un comando.
 */
public enum EstadoDecision {
    /** Pasó todos los controles y se aplicó en el inventario. */
    EJECUTADO,
    /** Lo detuvo la cadena, el estado del lote o es irreversible; el inventario no cambió. */
    RECHAZADO,
    /** Su clave de idempotencia ya se había aplicado; se ignoró. */
    DUPLICADO,
    /** Error inesperado del receiver (defensa en profundidad). */
    FALLIDO
}
