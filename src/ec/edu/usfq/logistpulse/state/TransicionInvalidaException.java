package ec.edu.usfq.logistpulse.state;

/**
 * Se lanza cuando se pide a un lote una operación que su estado actual no permite
 * (por ejemplo, reservar un lote RECIBIDO o ajustar un lote ENVIADO).
 */
public class TransicionInvalidaException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public TransicionInvalidaException(Lote lote, String accion) {
        super("Transicion invalida: el lote " + lote.codigo() + "@" + lote.bodega()
                + " esta " + lote.nombreEstado() + " y no permite " + accion);
    }
}
