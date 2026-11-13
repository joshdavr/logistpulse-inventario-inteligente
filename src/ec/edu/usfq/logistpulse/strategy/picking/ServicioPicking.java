package ec.edu.usfq.logistpulse.strategy.picking;

import ec.edu.usfq.logistpulse.dominio.Cantidades;
import ec.edu.usfq.logistpulse.dominio.Categoria;
import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.state.VistaLote;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <b>Rol en el patrón Strategy: CONTEXT — Picking.</b>
 *
 * <ol>
 *   <li>Elige la política: la del SKU si tiene una propia, si no la de su categoría.</li>
 *   <li>Aplica el filtro de seguridad común: solo lotes asignables hoy.</li>
 *   <li>Ordena con la política y rellena el pedido lote por lote.</li>
 * </ol>
 * <p>Las políticas se pueden cambiar mientras el sistema corre.</p>
 */
public class ServicioPicking implements AsignadorLotes {

    private final Map<Categoria, PoliticaPicking> porCategoria = new EnumMap<>(Categoria.class);
    private final Map<String, PoliticaPicking> porSku = new HashMap<>();
    private final PoliticaPicking porDefecto;

    public ServicioPicking(PoliticaPicking porDefecto) {
        this.porDefecto = Objects.requireNonNull(porDefecto, "porDefecto");
    }

    public void usarParaCategoria(Categoria categoria, PoliticaPicking politica) {
        porCategoria.put(Objects.requireNonNull(categoria), Objects.requireNonNull(politica, "politica"));
    }

    /** Un SKU puede tener su propia política, que gana sobre la de su categoría. */
    public void usarParaSku(String sku, PoliticaPicking politica) {
        porSku.put(Objects.requireNonNull(sku), Objects.requireNonNull(politica, "politica"));
    }

    public PoliticaPicking politicaPara(ProductoSku producto) {
        PoliticaPicking propia = porSku.get(producto.codigo());
        return propia != null ? propia : porCategoria.getOrDefault(producto.categoria(), porDefecto);
    }

    @Override
    public PlanPicking planificar(ProductoSku producto, List<? extends VistaLote> candidatos, int cantidad, LocalDate hoy) {
        return planificarCon(politicaPara(producto), candidatos, cantidad, hoy);
    }

    /** Mismo filtro y relleno con una política cualquiera; sirve para comparar estrategias sin reservar. */
    public PlanPicking planificarCon(PoliticaPicking politica, List<? extends VistaLote> candidatos,
                                     int cantidad, LocalDate hoy) {
        Cantidades.exigirPositiva(cantidad, "La cantidad a asignar");
        List<VistaLote> ordenados = new ArrayList<>();
        for (VistaLote lote : candidatos) {
            if (lote.asignableEn(hoy)) { // filtro de seguridad comun a todas las politicas
                ordenados.add(lote);
            }
        }
        ordenados.sort(politica.prioridad());

        List<AsignacionLote> plan = new ArrayList<>();
        int falta = cantidad;
        for (VistaLote lote : ordenados) {
            if (falta == 0) {
                break;
            }
            int toma = Math.min(falta, lote.disponible());
            plan.add(new AsignacionLote(lote.codigo(), toma));
            falta -= toma;
        }
        return new PlanPicking(politica.nombre(), cantidad, plan);
    }
}
