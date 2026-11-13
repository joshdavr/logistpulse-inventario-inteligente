package ec.edu.usfq.logistpulse.strategy.picking;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resultado de aplicar una política de picking: de qué lotes y cuánto de cada uno.
 */
public record PlanPicking(String politica, int solicitado, List<AsignacionLote> asignaciones) {

    public PlanPicking {
        asignaciones = List.copyOf(asignaciones);
    }

    public int total() {
        return asignaciones.stream().mapToInt(AsignacionLote::cantidad).sum();
    }

    public boolean cubreTodo() {
        return total() == solicitado;
    }

    public String describir() {
        String lineas = asignaciones.isEmpty() ? "(ningun lote)"
                : asignaciones.stream().map(AsignacionLote::toString).collect(Collectors.joining(", "));
        return politica + ": " + lineas;
    }
}
