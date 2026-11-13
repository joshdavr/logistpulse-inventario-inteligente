package ec.edu.usfq.logistpulse.state;

/**
 * <b>Rol en el patrón State: STATE (interfaz).</b>
 *
 * <p><b>Contrato (lo cumplen todos los estados):</b></p>
 * <ul>
 *   <li>Una operación que el estado no permite lanza {@link TransicionInvalidaException}
 *       y no modifica el lote.</li>
 *   <li>Una operación permitida puede lanzar {@link IllegalStateException} solo si rompería
 *       la invariante {@code 0 <= reservado <= fisico}; en ese caso tampoco modifica el lote.</li>
 *   <li>No lanza ninguna otra excepción.</li>
 * </ul>
 *
 * <p>Cada operación tiene una implementación por defecto que rechaza; cada estado
 * concreto sobrescribe solo lo que permite. Así las transiciones legales quedan
 * escritas en un solo lugar por estado y no en condicionales repartidos.</p>
 *
 * <pre>
 *   RECIBIDO --almacenar--> ALMACENADO --reservar todo--> RESERVADO --despachar todo--> ENVIADO
 *                               |    ^                        |
 *                               |    +-------liberar----------+
 *                  cuarentena   v    | liberar cuarentena
 *                            CUARENTENA
 * </pre>
 */
public interface EstadoLote {

    String nombre();

    /** ¿La estrategia de picking puede tomar unidades de este lote? */
    default boolean admiteAsignacion() {
        return false;
    }

    /** ¿El lote puede recibir unidades transferidas desde otra bodega? */
    default boolean admiteIngreso() {
        return false;
    }

    /** ¿Sus unidades libres cuentan como stock para calcular la reposición? */
    default boolean esUtilizable() {
        return false;
    }

    default void almacenar(Lote lote) {
        throw new TransicionInvalidaException(lote, "almacenar");
    }

    default void reservar(Lote lote, int unidades) {
        throw new TransicionInvalidaException(lote, "reservar");
    }

    default void liberar(Lote lote, int unidades) {
        throw new TransicionInvalidaException(lote, "liberar reservas");
    }

    default void despachar(Lote lote, int unidades) {
        throw new TransicionInvalidaException(lote, "despachar");
    }

    default void ponerEnCuarentena(Lote lote) {
        throw new TransicionInvalidaException(lote, "pasar a cuarentena");
    }

    default void liberarCuarentena(Lote lote) {
        throw new TransicionInvalidaException(lote, "salir de cuarentena");
    }

    default void ajustar(Lote lote, int delta) {
        throw new TransicionInvalidaException(lote, "ajustes");
    }

    default void retirar(Lote lote, int unidades) {
        throw new TransicionInvalidaException(lote, "transferir unidades hacia otra bodega");
    }

    default void ingresar(Lote lote, int unidades) {
        throw new TransicionInvalidaException(lote, "recibir una transferencia");
    }
}
