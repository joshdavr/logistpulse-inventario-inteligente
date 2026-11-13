package ec.edu.usfq.logistpulse.strategy.reposicion;

/**
 * Lo que una política de reposición recomienda pedir y por qué.
 */
public record Sugerencia(int cantidad, String razon) {

    public Sugerencia {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("Una sugerencia debe pedir al menos 1 unidad");
        }
    }
}
