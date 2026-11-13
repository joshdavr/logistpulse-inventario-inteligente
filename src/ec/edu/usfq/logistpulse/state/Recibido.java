package ec.edu.usfq.logistpulse.state;

/**
 * <b>CONCRETE STATE:</b> el lote está en el muelle y todavía no tiene ubicación.
 *
 * <p>Permite: almacenar (pasa a ALMACENADO) y ajustar (conteo al recibir).
 * No se puede reservar: nadie sabe aún dónde está físicamente.</p>
 */
public final class Recibido implements EstadoLote {

    @Override
    public String nombre() {
        return "RECIBIDO";
    }

    @Override
    public boolean esUtilizable() {
        return true; // llegará al rack pronto: cuenta para no volver a pedirlo
    }

    @Override
    public void almacenar(Lote lote) {
        lote.cambiarA(new Almacenado());
    }

    @Override
    public void ajustar(Lote lote, int delta) {
        lote.moverFisico(delta);
    }
}
