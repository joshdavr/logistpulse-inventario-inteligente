package ec.edu.usfq.logistpulse.strategy.reposicion;

import ec.edu.usfq.logistpulse.dominio.PerfilDemanda;

import java.util.Optional;

/**
 * <b>CONCRETE STRATEGY — Punto de reorden (ROP) con cantidad fija.</b>
 *
 * <p>Cuando la posición baja a ROP = demanda diaria x tiempo de entrega + stock
 * de seguridad, se pide siempre la misma cantidad.</p>
 */
public final class PuntoDeReorden implements PoliticaReposicion {

    private final int cantidadFija;

    public PuntoDeReorden(int cantidadFija) {
        if (cantidadFija <= 0) {
            throw new IllegalArgumentException("La cantidad fija debe ser positiva");
        }
        this.cantidadFija = cantidadFija;
    }

    @Override
    public String nombre() {
        return "ROP";
    }

    @Override
    public Optional<Sugerencia> evaluar(PerfilDemanda perfil, int posicion) {
        int rop = perfil.puntoDeReorden();
        if (posicion > rop) {
            return Optional.empty();
        }
        return Optional.of(new Sugerencia(cantidadFija,
                "posicion " + posicion + " <= ROP " + rop + ", se pide la cantidad fija " + cantidadFija));
    }
}
