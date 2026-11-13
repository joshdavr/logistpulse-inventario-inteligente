package ec.edu.usfq.logistpulse.state;

/**
 * Comportamiento compartido por ALMACENADO y RESERVADO: los dos están en el rack,
 * así que ambos pueden liberar reservas, despachar lo reservado, ajustarse y
 * recibir transferencias. Después de cada operación el lote se reubica en
 * ALMACENADO o RESERVADO según le queden unidades libres.
 */
abstract class EstadoEnRack implements EstadoLote {

    @Override
    public boolean admiteIngreso() {
        return true;
    }

    @Override
    public boolean esUtilizable() {
        return true;
    }

    @Override
    public void liberar(Lote lote, int unidades) {
        lote.moverReservado(-unidades);
        lote.ubicarSegunDisponibilidad();
    }

    @Override
    public void despachar(Lote lote, int unidades) {
        if (unidades > lote.reservado()) {
            throw new IllegalStateException("Solo se despacha lo reservado: el lote " + lote.codigo()
                    + " tiene " + lote.reservado() + " reservadas y se piden " + unidades);
        }
        lote.moverReservado(-unidades);
        lote.moverFisico(-unidades);
        if (lote.fisico() == 0) {
            lote.cambiarA(new Enviado());
        } else {
            lote.ubicarSegunDisponibilidad();
        }
    }

    @Override
    public void ajustar(Lote lote, int delta) {
        lote.moverFisico(delta);
        lote.ubicarSegunDisponibilidad();
    }

    @Override
    public void ingresar(Lote lote, int unidades) {
        lote.moverFisico(unidades);
        lote.ubicarSegunDisponibilidad();
    }
}
