package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;

import java.util.Optional;

/**
 * <b>Eslabón 4 (CONCRETE HANDLER) — Capacidad:</b> la operación cabe en el espacio que
 * declara ocupar (físico o de alistamiento, en la bodega que indique).
 *
 * <p>Usa la misma regla que el inventario aplica al ejecutar
 * ({@link ConsultasInventario#verificarEspacio}), así que no hay dos versiones de la regla.</p>
 */
public class FiltroCapacidad extends EslabonValidacion {

    @Override
    public String nombre() {
        return "Capacidad";
    }

    @Override
    protected boolean atiende(SolicitudMovimiento s) {
        return s.espacio() != null;
    }

    @Override
    protected Optional<String> revisar(SolicitudMovimiento s, ConsultasInventario inventario) {
        Espacio e = s.espacio();
        return inventario.verificarEspacio(e.bodega(), e.tipo(), e.unidades());
    }
}
