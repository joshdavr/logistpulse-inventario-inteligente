package ec.edu.usfq.logistpulse.observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <b>Rol en el patrón Observer: PUBLISHER (notificador).</b>
 *
 * <p>Guarda la lista de suscriptores, cada uno con el tipo de evento que le
 * interesa (como un {@code @EventListener} de Spring). Se pueden dar de alta o de
 * baja mientras el sistema corre.</p>
 *
 * <ul>
 *   <li>Si un suscriptor falla, los demás igual reciben el evento; el fallo queda registrado.</li>
 *   <li>Hoy se notifica en orden de suscripción, pero ningún suscriptor debe depender de eso.</li>
 * </ul>
 */
public class BusEventos implements PublicadorEventos {

    private record Registro(Class<? extends Evento> tipo, Suscriptor<?> suscriptor) {
    }

    private final List<Registro> registros = new ArrayList<>();
    private final List<String> fallosDeEntrega = new ArrayList<>();
    private int publicados;

    public <E extends Evento> void suscribir(Class<E> tipo, Suscriptor<? super E> suscriptor) {
        registros.add(new Registro(Objects.requireNonNull(tipo), Objects.requireNonNull(suscriptor)));
    }

    /** Da de baja al suscriptor de todos los tipos de evento a los que estaba suscrito. */
    public boolean desuscribir(Suscriptor<?> suscriptor) {
        return registros.removeIf(r -> r.suscriptor() == suscriptor);
    }

    public boolean estaSuscrito(Suscriptor<?> suscriptor) {
        return registros.stream().anyMatch(r -> r.suscriptor() == suscriptor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publicar(Evento evento) {
        Objects.requireNonNull(evento, "evento");
        publicados++;
        // Se recorre una copia: un suscriptor podria darse de baja mientras se le notifica.
        for (Registro registro : List.copyOf(registros)) {
            if (!registro.tipo().isInstance(evento)) {
                continue;
            }
            Suscriptor<Evento> suscriptor = (Suscriptor<Evento>) registro.suscriptor();
            try {
                suscriptor.alRecibir(evento);
            } catch (RuntimeException e) {
                fallosDeEntrega.add(suscriptor.nombre() + ": " + e.getMessage());
            }
        }
    }

    public int publicados() {
        return publicados;
    }

    public List<String> fallosDeEntrega() {
        return Collections.unmodifiableList(fallosDeEntrega);
    }
}
