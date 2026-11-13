package ec.edu.usfq.logistpulse.observer;

import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>CONCRETE SUBSCRIBER — PULSEFLEET</b> (flota de robots AMR + gemelo digital 3D).
 *
 * <p>Escucha {@link StockCambiado} y, cuando el movimiento es físico, crea una tarea
 * para un robot y actualiza la zona del lote en el gemelo digital.</p>
 *
 * <p>Qué hacer con cada movimiento está en una tabla de {@link Regla}s y no en un
 * {@code switch}: un movimiento nuevo se atiende agregando una regla, sin tocar
 * esta clase (O de SOLID). Lo que no tiene regla (reservar, liberar, ajustar) se ignora.</p>
 */
public class PulseFleet implements Suscriptor<StockCambiado> {

    /** Una orden de trabajo para un robot móvil autónomo. */
    public record TareaAmr(String id, String robot, String lote, String desde, String hacia, String motivo) {
        public String describir() {
            return id + " " + robot + " lleva " + lote + " de " + desde + " a " + hacia + " (" + motivo + ")";
        }
    }

    /** Si el evento sale del lote, entra a él o da igual. */
    public enum Sentido {
        CUALQUIERA {
            @Override
            boolean coincide(int deltaFisico) {
                return true;
            }
        },
        SALIDA {
            @Override
            boolean coincide(int deltaFisico) {
                return deltaFisico < 0;
            }
        },
        ENTRADA {
            @Override
            boolean coincide(int deltaFisico) {
                return deltaFisico > 0;
            }
        };

        abstract boolean coincide(int deltaFisico);
    }

    /**
     * @param desde       zona de origen (null si el lote solo aparece en el gemelo)
     * @param conRobot    true si hace falta mover el lote con un robot
     */
    public record Regla(TipoMovimiento movimiento, Sentido sentido, String desde, String hacia,
                        String motivo, boolean conRobot) {
        public Regla {
            Objects.requireNonNull(movimiento);
            Objects.requireNonNull(sentido);
            Objects.requireNonNull(hacia);
            Objects.requireNonNull(motivo);
        }
    }

    /** Las reglas de una bodega típica de LOGISTPULSE. */
    public static List<Regla> reglasEstandar() {
        return List.of(
                new Regla(TipoMovimiento.RECIBIR, Sentido.CUALQUIERA, null, "MUELLE", "lote recibido", false),
                new Regla(TipoMovimiento.ALMACENAR, Sentido.CUALQUIERA, "MUELLE", "RACK", "ubicar lote recibido", true),
                new Regla(TipoMovimiento.DESPACHAR, Sentido.CUALQUIERA, "RACK", "ANDEN", "preparar pedido", true),
                new Regla(TipoMovimiento.CUARENTENA, Sentido.CUALQUIERA, "RACK", "ZONA-CUARENTENA", "aislar lote", true),
                new Regla(TipoMovimiento.LIBERAR_CUARENTENA, Sentido.CUALQUIERA, "ZONA-CUARENTENA", "RACK",
                        "lote aprobado por calidad", true),
                new Regla(TipoMovimiento.TRANSFERIR, Sentido.SALIDA, "RACK", "CAMION", "traslado", true),
                new Regla(TipoMovimiento.TRANSFERIR, Sentido.ENTRADA, null, "RACK", "llega traslado", false));
    }

    private final List<String> robots;
    private final List<Regla> reglas;
    private final List<TareaAmr> tareas = new ArrayList<>();
    private final Map<String, String> gemelo = new LinkedHashMap<>();
    private int turno;

    public PulseFleet(List<String> robots, List<Regla> reglas) {
        if (robots.isEmpty()) {
            throw new IllegalArgumentException("La flota necesita al menos un robot");
        }
        this.robots = List.copyOf(robots);
        this.reglas = List.copyOf(reglas);
    }

    @Override
    public String nombre() {
        return "PULSEFLEET";
    }

    @Override
    public void alRecibir(StockCambiado e) {
        reglas.stream()
                .filter(r -> r.movimiento() == e.movimiento() && r.sentido().coincide(e.deltaFisico()))
                .findFirst()
                .ifPresent(r -> aplicar(r, e));
    }

    private void aplicar(Regla regla, StockCambiado e) {
        String lote = e.lote() + "@" + e.bodega();
        if (regla.conRobot()) {
            String robot = robots.get(turno % robots.size());
            turno++;
            String motivo = e.referencia() == null ? regla.motivo() : regla.motivo() + " " + e.referencia();
            tareas.add(new TareaAmr(String.format("T-%03d", tareas.size() + 1), robot, lote,
                    regla.desde(), regla.hacia(), motivo));
        }
        gemelo.put(lote, regla.hacia());
    }

    public List<TareaAmr> tareas() {
        return Collections.unmodifiableList(tareas);
    }

    /** Zona donde el gemelo digital ubica al lote, por ejemplo RACK o ANDEN. */
    public Optional<String> zonaEnGemelo(String lote, String bodega) {
        return Optional.ofNullable(gemelo.get(lote + "@" + bodega));
    }
}
