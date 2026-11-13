package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <b>Rol en el patrón Chain of Responsibility: HANDLER base.</b>
 *
 * <p>Cada eslabón decide dos cosas:</p>
 * <ol>
 *   <li>{@link #atiende}: si la solicitud declara el requisito que le toca. Si no, la reenvía.</li>
 *   <li>{@link #revisar}: si la aprueba (vacío) o la detiene con un motivo.</li>
 * </ol>
 * <p>El recorrido está fijado aquí (método plantilla), así ningún eslabón concreto
 * puede olvidarse de pasar la solicitud al siguiente. Los eslabones solo leen el
 * inventario ({@link ConsultasInventario}); no pueden modificarlo.</p>
 *
 * <p><b>Contrato:</b> {@link #revisar} no lanza excepciones ni modifica nada; devuelve
 * vacío o un motivo legible.</p>
 */
public abstract class EslabonValidacion {

    private EslabonValidacion siguiente;

    /** Enlaza el siguiente y lo devuelve: {@code a.enlazar(b).enlazar(c)}. */
    public EslabonValidacion enlazar(EslabonValidacion siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    EslabonValidacion siguiente() {
        return siguiente;
    }

    public final Veredicto evaluar(SolicitudMovimiento solicitud, ConsultasInventario inventario) {
        return recorrer(solicitud, inventario, new ArrayList<>());
    }

    private Veredicto recorrer(SolicitudMovimiento solicitud, ConsultasInventario inventario, List<String> traza) {
        if (!atiende(solicitud)) {
            traza.add(nombre() + ":no aplica");
        } else {
            Optional<String> motivo = revisar(solicitud, inventario);
            if (motivo.isPresent()) {
                traza.add(nombre() + ":RECHAZA");
                return Veredicto.rechazado(nombre(), motivo.get(), traza);
            }
            traza.add(nombre() + ":ok");
        }
        return siguiente == null ? Veredicto.aprobado(traza) : siguiente.recorrer(solicitud, inventario, traza);
    }

    protected boolean atiende(SolicitudMovimiento solicitud) {
        return true;
    }

    protected abstract Optional<String> revisar(SolicitudMovimiento solicitud, ConsultasInventario inventario);

    public abstract String nombre();
}
