package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.SolicitudMovimiento;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Datos comunes a todos los comandos: id, clave de idempotencia, solicitud y huella.
 */
public abstract class ComandoBase implements ComandoInventario {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private final String id;
    private final String claveIdempotencia;
    private final SolicitudMovimiento solicitud;

    protected ComandoBase(String claveIdempotencia, SolicitudMovimiento solicitud) {
        if (claveIdempotencia == null || claveIdempotencia.isBlank()) {
            throw new IllegalArgumentException("Todo comando necesita una clave de idempotencia");
        }
        this.id = String.format("CMD-%03d", SECUENCIA.incrementAndGet());
        this.claveIdempotencia = claveIdempotencia;
        this.solicitud = Objects.requireNonNull(solicitud, "solicitud");
    }

    /** La compensación tiene su propia clave, así tampoco se puede aplicar dos veces. */
    protected String claveCompensacion() {
        return claveIdempotencia + "#comp";
    }

    /** Datos propios del comando que no están en la solicitud (vencimiento, razón, motivo). */
    protected String datosExtra() {
        return "";
    }

    @Override
    public final String huella() {
        return getClass().getSimpleName() + "|" + solicitud.huella() + "|" + datosExtra();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String claveIdempotencia() {
        return claveIdempotencia;
    }

    @Override
    public SolicitudMovimiento solicitud() {
        return solicitud;
    }

    @Override
    public String toString() {
        return id + " " + describir();
    }
}
