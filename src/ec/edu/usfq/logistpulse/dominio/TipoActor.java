package ec.edu.usfq.logistpulse.dominio;

/**
 * Quién origina una operación de inventario.
 * Humanos y agentes de IA pasan exactamente por los mismos guardrails.
 */
public enum TipoActor {
    HUMANO,
    AGENTE_IA
}
