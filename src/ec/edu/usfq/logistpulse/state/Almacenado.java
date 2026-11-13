package ec.edu.usfq.logistpulse.state;

/**
 * <b>CONCRETE STATE:</b> el lote está en el rack y tiene unidades libres.
 *
 * <p>Es el único estado desde el que se puede reservar, transferir a otra
 * bodega o pasar a CUARENTENA (y esto último solo si no tiene reservas).</p>
 */
public final class Almacenado extends EstadoEnRack {

    @Override
    public String nombre() {
        return "ALMACENADO";
    }

    @Override
    public boolean admiteAsignacion() {
        return true;
    }

    @Override
    public void reservar(Lote lote, int unidades) {
        lote.moverReservado(unidades);
        lote.ubicarSegunDisponibilidad(); // si ya no quedan libres pasa a RESERVADO
    }

    @Override
    public void retirar(Lote lote, int unidades) {
        if (unidades > lote.disponible()) {
            throw new IllegalStateException("El lote " + lote.codigo() + " solo tiene "
                    + lote.disponible() + " unidades libres para transferir");
        }
        lote.moverFisico(-unidades);
        lote.ubicarSegunDisponibilidad();
    }

    @Override
    public void ponerEnCuarentena(Lote lote) {
        if (lote.reservado() > 0) {
            throw new TransicionInvalidaException(lote,
                    "pasar a cuarentena mientras tenga " + lote.reservado() + " unidades reservadas");
        }
        lote.cambiarA(new Cuarentena());
    }
}
