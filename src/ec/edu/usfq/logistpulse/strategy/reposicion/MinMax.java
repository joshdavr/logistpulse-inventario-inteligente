package ec.edu.usfq.logistpulse.strategy.reposicion;

import ec.edu.usfq.logistpulse.dominio.PerfilDemanda;

import java.util.Optional;

/**
 * <b>CONCRETE STRATEGY — Min-Max.</b>
 *
 * <p>Si la posición cae al mínimo o por debajo, se pide lo necesario para volver al máximo.</p>
 */
public final class MinMax implements PoliticaReposicion {

    private final int minimo;
    private final int maximo;

    public MinMax(int minimo, int maximo) {
        if (minimo < 0 || maximo <= minimo) {
            throw new IllegalArgumentException("Se necesita 0 <= minimo < maximo");
        }
        this.minimo = minimo;
        this.maximo = maximo;
    }

    @Override
    public String nombre() {
        return "MIN-MAX";
    }

    @Override
    public Optional<Sugerencia> evaluar(PerfilDemanda perfil, int posicion) {
        if (posicion > minimo) {
            return Optional.empty();
        }
        int cantidad = maximo - posicion;
        return Optional.of(new Sugerencia(cantidad,
                "posicion " + posicion + " <= min " + minimo + ", se completa hasta max " + maximo));
    }
}
