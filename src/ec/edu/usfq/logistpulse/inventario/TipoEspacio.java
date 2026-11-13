package ec.edu.usfq.logistpulse.inventario;

import ec.edu.usfq.logistpulse.dominio.Bodega;

/**
 * Los dos espacios que limita cada bodega. Cada constante sabe medirse a sí misma,
 * así la regla de capacidad no necesita un {@code if} por tipo.
 */
public enum TipoEspacio {

    /** Unidades físicas que caben en la bodega. */
    FISICO {
        @Override
        long usado(ConsultasInventario c, String bodega) {
            return c.fisicoEnBodega(bodega);
        }

        @Override
        int limite(Bodega b) {
            return b.capacidadUnidades();
        }

        @Override
        String motivo(Bodega b, long usado, int unidades) {
            return "Bodega " + b.codigo() + " sin espacio: " + usado + "/" + b.capacidadUnidades()
                    + " unidades, no caben " + unidades + " mas";
        }
    },

    /** Unidades reservadas a la vez (zona de preparación de pedidos). */
    ALISTAMIENTO {
        @Override
        long usado(ConsultasInventario c, String bodega) {
            return c.reservadoEnBodega(bodega);
        }

        @Override
        int limite(Bodega b) {
            return b.capacidadAlistamiento();
        }

        @Override
        String motivo(Bodega b, long usado, int unidades) {
            return "Zona de alistamiento de " + b.codigo() + " llena: " + usado + "/" + b.capacidadAlistamiento()
                    + " reservadas, no caben " + unidades + " mas";
        }
    };

    abstract long usado(ConsultasInventario consultas, String bodega);

    abstract int limite(Bodega bodega);

    abstract String motivo(Bodega bodega, long usado, int unidades);
}
