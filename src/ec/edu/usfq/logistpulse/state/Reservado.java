package ec.edu.usfq.logistpulse.state;

/**
 * <b>CONCRETE STATE:</b> todas las unidades del lote están comprometidas con pedidos.
 *
 * <p>Permite liberar (vuelve a ALMACENADO), despachar (pasa a ENVIADO cuando
 * sale la última unidad), ajustar hacia arriba y recibir transferencias.
 * No permite reservar más, transferir hacia afuera ni pasar a cuarentena.</p>
 */
public final class Reservado extends EstadoEnRack {

    @Override
    public String nombre() {
        return "RESERVADO";
    }
}
