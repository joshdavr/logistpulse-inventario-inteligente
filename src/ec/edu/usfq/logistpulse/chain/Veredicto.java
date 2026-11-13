package ec.edu.usfq.logistpulse.chain;

import java.util.List;

/**
 * Resultado de pasar una solicitud por la cadena.
 *
 * @param eslabon   nombre del eslabón que rechazó (null si fue aprobada)
 * @param motivo    explicación del rechazo (null si fue aprobada)
 * @param recorrido traza de cada eslabón visitado, por ejemplo "SKU:ok", "Lote:RECHAZA"
 */
public record Veredicto(boolean aprobado, String eslabon, String motivo, List<String> recorrido) {

    public Veredicto {
        recorrido = List.copyOf(recorrido);
    }

    static Veredicto aprobado(List<String> recorrido) {
        return new Veredicto(true, null, null, recorrido);
    }

    static Veredicto rechazado(String eslabon, String motivo, List<String> recorrido) {
        return new Veredicto(false, eslabon, motivo, recorrido);
    }

    public String trazaLegible() {
        return String.join(" -> ", recorrido);
    }
}
