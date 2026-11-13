package ec.edu.usfq.logistpulse.dominio;

import java.util.Objects;

/**
 * Producto del catálogo.
 *
 * @param activo un SKU descontinuado ya no admite recepciones ni reservas nuevas,
 *               pero su stock existente sí se puede despachar, ajustar o mover
 */
public record ProductoSku(String codigo, String descripcion, Categoria categoria,
                          boolean activo, PerfilDemanda perfil) {

    public ProductoSku {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(descripcion, "descripcion");
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(perfil, "perfil");
    }
}
