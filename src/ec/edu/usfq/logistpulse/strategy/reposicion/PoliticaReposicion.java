package ec.edu.usfq.logistpulse.strategy.reposicion;

import ec.edu.usfq.logistpulse.dominio.PerfilDemanda;

import java.util.Optional;

/**
 * <b>Rol en el patrón Strategy: STRATEGY (interfaz) — ReplenishmentPolicy.</b>
 *
 * <p>Decide cuándo y cuánto pedir al proveedor.</p>
 *
 * <p><b>Contrato:</b> con {@code posicionInventario >= 0} devuelve vacío si todavía
 * no hace falta pedir, o una sugerencia con cantidad positiva. Nunca devuelve null
 * ni lanza excepciones. Los parámetros inválidos se rechazan al construir la política.</p>
 */
public interface PoliticaReposicion {

    String nombre();

    /**
     * @param perfil             demanda y tiempo de entrega del SKU
     * @param posicionInventario unidades utilizables + unidades ya pedidas y no recibidas (>= 0)
     */
    Optional<Sugerencia> evaluar(PerfilDemanda perfil, int posicionInventario);
}
