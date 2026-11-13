package ec.edu.usfq.logistpulse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Reloj que solo avanza cuando se le pide. Se inyecta en todo el sistema, así la demo
 * y las pruebas dan siempre el mismo resultado (vencimientos, horas del ledger).
 */
public final class RelojManual extends Clock {

    private Instant ahora;
    private final ZoneId zona;

    public RelojManual(Instant inicio, ZoneId zona) {
        this.ahora = Objects.requireNonNull(inicio, "inicio");
        this.zona = Objects.requireNonNull(zona, "zona");
    }

    public void avanzar(Duration tiempo) {
        if (tiempo.isNegative()) {
            throw new IllegalArgumentException("El reloj no retrocede");
        }
        ahora = ahora.plus(tiempo);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId otraZona) {
        return new RelojManual(ahora, otraZona);
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
