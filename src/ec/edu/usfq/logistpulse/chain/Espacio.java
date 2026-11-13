package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.dominio.Cantidades;
import ec.edu.usfq.logistpulse.inventario.TipoEspacio;

import java.util.Objects;

/**
 * Espacio que una operación necesita en una bodega (lo revisa el eslabón Capacidad).
 */
public record Espacio(String bodega, TipoEspacio tipo, int unidades) {

    public Espacio {
        Objects.requireNonNull(bodega, "bodega");
        Objects.requireNonNull(tipo, "tipo");
        Cantidades.exigirPositiva(unidades, "El espacio requerido");
    }

    public static Espacio fisico(String bodega, int unidades) {
        return new Espacio(bodega, TipoEspacio.FISICO, unidades);
    }

    public static Espacio alistamiento(String bodega, int unidades) {
        return new Espacio(bodega, TipoEspacio.ALISTAMIENTO, unidades);
    }
}
