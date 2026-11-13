package ec.edu.usfq.logistpulse.inventario;

import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.state.TransicionInvalidaException;

import java.time.LocalDate;

/**
 * Lado de <b>escritura</b> del inventario: es el RECEIVER del patrón Command.
 * Los comandos dependen de esta interfaz, no de la clase concreta (D de SOLID).
 *
 * <p><b>Contrato:</b> cada operación valida por su cuenta, aunque nadie haya pasado
 * por la cadena (defensa en profundidad), y es atómica: si falla, no cambia nada.</p>
 * <ul>
 *   <li>{@link IllegalArgumentException}: parámetros fuera de rango (cantidades, bodegas iguales).</li>
 *   <li>{@link TransicionInvalidaException}: el estado del lote no permite la operación.</li>
 *   <li>{@link IllegalStateException}: se rompería una regla de negocio
 *       (capacidad, stock, vencimiento, dueño de la reserva, SKU descontinuado).</li>
 * </ul>
 */
public interface OperacionesInventario {

    void recibirLote(String lote, String sku, String bodega, int unidades, LocalDate vencimiento);

    void almacenar(String lote, String bodega);

    /** La política de picking del SKU decide de qué lotes sale; el dueño queda registrado. */
    Reserva reservar(String pedido, String sku, String bodega, int unidades, Actor dueno);

    /** Solo el dueño de la reserva o un supervisor. */
    void liberarReserva(String pedido, Actor solicitante);

    /** Solo el dueño de la reserva o un supervisor. */
    void despachar(String pedido, Actor solicitante);

    void ajustar(String lote, String bodega, int delta);

    void transferir(String lote, String origen, String destino, int unidades);

    void ponerEnCuarentena(String lote, String bodega);

    void liberarCuarentena(String lote, String bodega);
}
