package ec.edu.usfq.logistpulse.consola;

import ec.edu.usfq.logistpulse.command.DecisionRegistrada;
import ec.edu.usfq.logistpulse.dominio.Bodega;
import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;
import ec.edu.usfq.logistpulse.observer.Ledger;
import ec.edu.usfq.logistpulse.state.VistaLote;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Presentación en consola. Está separada del inventario y del ledger
 * (S de SOLID): ellos guardan datos, esta clase solo los formatea.
 */
public final class Reportes {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Reportes() {
    }

    public static String inventario(ConsultasInventario inv) {
        LocalDate hoy = inv.hoy();
        StringBuilder sb = new StringBuilder();
        String formato = "  %-11s %-10s %-8s %-11s %6s %6s %6s  %-10s %s%n";
        sb.append(String.format(formato, "LOTE", "BODEGA", "SKU", "ESTADO", "FISICO", "RESERV", "LIBRE", "INGRESO", "VENCE"));
        List<VistaLote> ordenados = new ArrayList<>(inv.lotes());
        ordenados.sort(Comparator.comparing(VistaLote::bodega).thenComparing(VistaLote::sku).thenComparing(VistaLote::codigo));
        for (VistaLote l : ordenados) {
            String vence = l.vencimiento() + (l.vencidoEn(hoy) ? " (vencido)" : "");
            sb.append(String.format(formato, l.codigo(), l.bodega(), l.sku(), l.nombreEstado(),
                    l.fisico(), l.reservado(), l.disponible(), l.fechaIngreso(), vence));
        }
        for (Bodega b : inv.bodegas()) {
            sb.append(String.format("  > %-10s ocupacion %d/%d unidades | alistamiento %d/%d reservadas%n",
                    b.codigo(), inv.fisicoEnBodega(b.codigo()), b.capacidadUnidades(),
                    inv.reservadoEnBodega(b.codigo()), b.capacidadAlistamiento()));
        }
        return sb.toString();
    }

    public static String ledger(Ledger ledger, ZoneId zona) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (DecisionRegistrada d : ledger.decisiones()) {
            n++;
            sb.append(String.format("  #%02d %s %s %-9s %-24s %s%s%n", n,
                    HORA.format(d.momento().atZone(zona)), d.comandoId(), d.estado(), d.actor(), d.operacion(),
                    d.esCompensacion() ? "  <compensa " + d.compensaA() + ">" : ""));
        }
        return sb.toString();
    }
}
