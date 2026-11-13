package ec.edu.usfq.logistpulse.strategy.picking;

import ec.edu.usfq.logistpulse.state.VistaLote;

import java.util.Comparator;

/**
 * <b>Rol en el patrón Strategy: STRATEGY (interfaz) — PickingPolicy.</b>
 *
 * <p>La política <b>solo ordena</b>: dice qué lote va primero. El filtro de
 * seguridad (descartar vencidos, en cuarentena o sin unidades libres) y el
 * relleno del pedido los hace el contexto ({@link ServicioPicking}), igual para
 * todas las políticas.</p>
 *
 * <p><b>Contrato:</b> {@link #prioridad()} devuelve un comparador no nulo y total
 * (desempata por código de lote), y nunca lanza excepciones.</p>
 */
public interface PoliticaPicking {

    String nombre();

    Comparator<VistaLote> prioridad();
}
