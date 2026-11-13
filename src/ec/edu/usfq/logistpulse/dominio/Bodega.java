package ec.edu.usfq.logistpulse.dominio;

import java.util.Objects;

/**
 * Bodega física de LOGISTPULSE.
 *
 * @param capacidadUnidades     máximo de unidades físicas que caben en la bodega
 * @param capacidadAlistamiento máximo de unidades reservadas al mismo tiempo
 *                              (lo que soporta la zona de preparación de pedidos)
 */
public record Bodega(String codigo, int capacidadUnidades, int capacidadAlistamiento) {

    public Bodega {
        Objects.requireNonNull(codigo, "codigo");
        if (capacidadUnidades <= 0 || capacidadAlistamiento <= 0) {
            throw new IllegalArgumentException("Las capacidades de la bodega deben ser positivas");
        }
    }
}
