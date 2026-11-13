package ec.edu.usfq.logistpulse.dominio;

/**
 * Familia de productos. Las políticas de picking y de reposición
 * (patrón Strategy) se asignan por categoría y se pueden sobrescribir por SKU.
 */
public enum Categoria {
    /** Lácteos y frescos: importa que salga primero lo que vence primero. */
    PERECIBLE,
    /** Granos, aceites: rotación por orden de llegada. */
    ABARROTES,
    /** Materiales de construcción apilados: se toma lo último que llegó. */
    MATERIALES
}
