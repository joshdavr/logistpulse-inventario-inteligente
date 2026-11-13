package ec.edu.usfq.logistpulse.dominio;

/**
 * Datos de demanda de un SKU que usan las políticas de reposición.
 *
 * @param demandaDiaria     unidades que se consumen en promedio por día
 * @param tiempoEntregaDias días que tarda el proveedor en entregar (lead time)
 * @param stockSeguridad    colchón para absorber variaciones de demanda
 */
public record PerfilDemanda(double demandaDiaria, int tiempoEntregaDias, int stockSeguridad) {

    public PerfilDemanda {
        if (!Double.isFinite(demandaDiaria) || demandaDiaria < 0 || demandaDiaria > Cantidades.MAXIMO) {
            throw new IllegalArgumentException("La demanda diaria debe estar entre 0 y " + Cantidades.MAXIMO);
        }
        if (tiempoEntregaDias < 0 || tiempoEntregaDias > 3650) {
            throw new IllegalArgumentException("El tiempo de entrega debe estar entre 0 y 3650 dias");
        }
        Cantidades.exigirNoNegativa(stockSeguridad, "El stock de seguridad");
    }

    /** ROP = demanda diaria x tiempo de entrega + stock de seguridad (sin desbordes). */
    public int puntoDeReorden() {
        double rop = Math.ceil(demandaDiaria * tiempoEntregaDias) + stockSeguridad;
        return (int) Math.min(rop, Integer.MAX_VALUE);
    }

    public double demandaAnual() {
        return demandaDiaria * 365;
    }
}
