package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.observer.Evento;

import java.time.Instant;
import java.util.Objects;

/**
 * Evento que publica la central por cada intento de comando (la "auditoría de decisiones").
 * Es un {@code record} inmutable con {@link Instant}.
 *
 * @param compensaA id del comando que este intento compensa, o null si es una operación normal
 */
public record DecisionRegistrada(Instant momento, String comandoId, String clave, Actor actor,
                                 String operacion, EstadoDecision estado, String detalle,
                                 String compensaA) implements Evento {

    public DecisionRegistrada {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(estado, "estado");
    }

    public boolean esCompensacion() {
        return compensaA != null;
    }
}
