package ec.edu.usfq.logistpulse.observer;

import java.time.Instant;

/**
 * Todo lo que se publica a los suscriptores. El momento es un {@link Instant}
 * (UTC), así el registro no depende de la zona horaria de quien lo lea.
 */
public interface Evento {

    Instant momento();
}
