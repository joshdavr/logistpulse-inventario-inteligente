package ec.edu.usfq.logistpulse.strategy.reposicion;

import ec.edu.usfq.logistpulse.dominio.PerfilDemanda;

import java.util.Optional;

/**
 * <b>CONCRETE STRATEGY — EOQ (cantidad económica de pedido).</b>
 *
 * <p>Pide cuando la posición llega al ROP, pero la cantidad es la que minimiza
 * el costo total de pedir + mantener inventario:</p>
 * <pre>  EOQ = raiz( 2 x D x S / H )   D = demanda anual, S = costo por pedido, H = costo anual de mantener 1 unidad</pre>
 */
public final class LoteEconomico implements PoliticaReposicion {

    private final double costoPorPedido;
    private final double costoMantenerAnual;

    public LoteEconomico(double costoPorPedido, double costoMantenerAnual) {
        if (costoPorPedido <= 0 || costoMantenerAnual <= 0) {
            throw new IllegalArgumentException("Los costos del EOQ deben ser positivos");
        }
        this.costoPorPedido = costoPorPedido;
        this.costoMantenerAnual = costoMantenerAnual;
    }

    @Override
    public String nombre() {
        return "EOQ";
    }

    public int eoq(PerfilDemanda perfil) {
        return (int) Math.ceil(Math.sqrt(2 * perfil.demandaAnual() * costoPorPedido / costoMantenerAnual));
    }

    @Override
    public Optional<Sugerencia> evaluar(PerfilDemanda perfil, int posicion) {
        int rop = perfil.puntoDeReorden();
        int cantidad = eoq(perfil);
        if (posicion > rop || cantidad == 0) {
            return Optional.empty();
        }
        return Optional.of(new Sugerencia(cantidad,
                "posicion " + posicion + " <= ROP " + rop + ", se pide EOQ = " + cantidad));
    }
}
