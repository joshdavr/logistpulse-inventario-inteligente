package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.dominio.Cantidades;
import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;

import java.util.Optional;

/**
 * <b>Eslabón opcional (extensión) — LimiteIA:</b> un agente de IA no puede mover más
 * de cierto número de unidades sin que un humano lo apruebe.
 *
 * <p>Demuestra la ventaja del patrón: se agrega un control nuevo sin modificar ni
 * los comandos ni la central que los despacha.</p>
 */
public class FiltroLimiteAgente extends EslabonValidacion {

    private final int maximoUnidades;

    public FiltroLimiteAgente(int maximoUnidades) {
        this.maximoUnidades = Cantidades.exigirPositiva(maximoUnidades, "El limite del agente");
    }

    @Override
    public String nombre() {
        return "LimiteIA";
    }

    @Override
    protected boolean atiende(SolicitudMovimiento s) {
        return s.actor().esAgente();
    }

    @Override
    protected Optional<String> revisar(SolicitudMovimiento s, ConsultasInventario inventario) {
        if (s.unidades() > maximoUnidades) {
            return Optional.of("Un agente de IA no puede mover mas de " + maximoUnidades
                    + " unidades sin aprobacion humana (pidio " + s.unidades() + ")");
        }
        return Optional.empty();
    }
}
