package ec.edu.usfq.logistpulse.command;

import ec.edu.usfq.logistpulse.chain.Veredicto;

/**
 * Lo que la central devuelve a quien envió el comando (API o agente).
 *
 * @param comandoId en un DUPLICADO es el id del comando original
 * @param veredicto null si el comando no llegó a la cadena (duplicado o irreversible)
 */
public record Resultado(EstadoDecision estado, String comandoId, String mensaje, Veredicto veredicto) {

    public boolean exitoso() {
        return estado == EstadoDecision.EJECUTADO;
    }

    @Override
    public String toString() {
        return estado + " " + comandoId + " | " + mensaje;
    }
}
