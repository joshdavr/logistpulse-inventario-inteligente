package ec.edu.usfq.logistpulse.state;

/**
 * <b>CONCRETE STATE (final):</b> todas las unidades del lote salieron de la bodega.
 *
 * <p>No sobrescribe ninguna operación: cualquier intento lanza
 * {@link TransicionInvalidaException}.</p>
 */
public final class Enviado implements EstadoLote {

    @Override
    public String nombre() {
        return "ENVIADO";
    }
}
