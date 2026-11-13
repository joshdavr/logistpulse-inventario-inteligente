package ec.edu.usfq.logistpulse.chain;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma cadenas de validación. El cliente solo recibe la cabeza de la cadena
 * y no sabe cuántos eslabones hay detrás.
 */
public final class CadenaValidacion {

    private CadenaValidacion() {
    }

    /**
     * Permisos -> SKU -> Stock -> Lote -> Capacidad.
     * Los cuatro últimos son los de la diapositiva, en su orden; Permisos se agregó
     * adelante (como el filtro de autorización de Spring Security) sin tocar a los demás.
     */
    public static EslabonValidacion estandar() {
        return armar(new FiltroAutorizacion(), new FiltroSku(), new FiltroStock(), new FiltroLote(), new FiltroCapacidad());
    }

    /** Enlaza los eslabones en el orden recibido y devuelve el primero. */
    public static EslabonValidacion armar(EslabonValidacion... eslabones) {
        if (eslabones.length == 0) {
            throw new IllegalArgumentException("La cadena necesita al menos un eslabon");
        }
        for (int i = 0; i < eslabones.length - 1; i++) {
            eslabones[i].enlazar(eslabones[i + 1]);
        }
        return eslabones[0];
    }

    /** Devuelve algo como "SKU -> Stock -> Lote -> Capacidad". */
    public static String describir(EslabonValidacion cabeza) {
        List<String> nombres = new ArrayList<>();
        for (EslabonValidacion e = cabeza; e != null; e = e.siguiente()) {
            nombres.add(e.nombre());
        }
        return String.join(" -> ", nombres);
    }
}
