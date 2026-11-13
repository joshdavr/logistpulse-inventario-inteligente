package ec.edu.usfq.logistpulse.state;

/**
 * <b>CONCRETE STATE (desvío):</b> el lote está aislado por calidad.
 *
 * <p>Permite salir de cuarentena (vuelve a ALMACENADO) y ajustar
 * (por ejemplo, dar de baja unidades dañadas o vencidas). Nada más.</p>
 */
public final class Cuarentena implements EstadoLote {

    @Override
    public String nombre() {
        return "CUARENTENA";
    }

    @Override
    public void liberarCuarentena(Lote lote) {
        lote.cambiarA(new Almacenado());
    }

    @Override
    public void ajustar(Lote lote, int delta) {
        lote.moverFisico(delta);
    }
}
