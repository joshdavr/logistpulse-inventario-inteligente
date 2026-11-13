package ec.edu.usfq.logistpulse.observer;

import ec.edu.usfq.logistpulse.command.DecisionRegistrada;
import ec.edu.usfq.logistpulse.command.EstadoDecision;
import ec.edu.usfq.logistpulse.dominio.TipoActor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>CONCRETE SUBSCRIBER — Ledger</b> (auditoría de decisiones).
 *
 * <p>Se suscribe a dos tipos de evento con un suscriptor por tipo (sin
 * {@code instanceof}): las decisiones de la central y los cambios de stock. Guarda
 * {@code record}s inmutables con {@link java.time.Instant}. Solo se puede agregar; no
 * hay métodos para editar ni borrar.</p>
 */
public class Ledger {

    private final List<DecisionRegistrada> decisiones = new ArrayList<>();
    private final List<StockCambiado> movimientos = new ArrayList<>();
    private final Suscriptor<DecisionRegistrada> suscriptorDecisiones = Suscriptor.con("Ledger", decisiones::add);
    private final Suscriptor<StockCambiado> suscriptorMovimientos = Suscriptor.con("Ledger", movimientos::add);

    public Suscriptor<DecisionRegistrada> suscriptorDecisiones() {
        return suscriptorDecisiones;
    }

    public Suscriptor<StockCambiado> suscriptorMovimientos() {
        return suscriptorMovimientos;
    }

    public List<DecisionRegistrada> decisiones() {
        return Collections.unmodifiableList(decisiones);
    }

    public List<StockCambiado> movimientos() {
        return Collections.unmodifiableList(movimientos);
    }

    public long contar(EstadoDecision estado) {
        return decisiones.stream().filter(d -> d.estado() == estado).count();
    }

    public long contarPorActor(TipoActor tipo) {
        return decisiones.stream().filter(d -> d.actor().tipo() == tipo).count();
    }
}
