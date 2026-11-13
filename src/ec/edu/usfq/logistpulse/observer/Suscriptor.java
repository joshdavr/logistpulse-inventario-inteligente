package ec.edu.usfq.logistpulse.observer;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * <b>Rol en el patrón Observer: SUBSCRIBER (interfaz común).</b>
 *
 * <p><b>Contrato:</b> puede lanzar una RuntimeException; el publicador la captura,
 * la registra y sigue notificando a los demás. Un suscriptor no debe depender del
 * orden en que se notifica (no está garantizado).</p>
 *
 * @param <E> tipo de evento que le interesa
 */
@FunctionalInterface
public interface Suscriptor<E extends Evento> {

    void alRecibir(E evento);

    default String nombre() {
        return getClass().getSimpleName();
    }

    /** Crea un suscriptor con nombre a partir de una lambda o referencia a método. */
    static <E extends Evento> Suscriptor<E> con(String nombre, Consumer<? super E> accion) {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(accion, "accion");
        return new Suscriptor<>() {
            @Override
            public void alRecibir(E evento) {
                accion.accept(evento);
            }

            @Override
            public String nombre() {
                return nombre;
            }
        };
    }
}
