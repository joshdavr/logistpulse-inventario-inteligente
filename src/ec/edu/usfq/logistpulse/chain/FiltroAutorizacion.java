package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;
import ec.edu.usfq.logistpulse.inventario.Reserva;

import java.util.Optional;

/**
 * <b>Eslabón 0 (CONCRETE HANDLER) — Permisos.</b>
 *
 * <ul>
 *   <li>El rol del actor debe estar entre los que la operación declara como autorizados.</li>
 *   <li>Si la operación gestiona una reserva (liberar o despachar), el actor debe ser su
 *       dueño o un supervisor. Así otro operador o un agente no pueden liberar reservas ajenas.</li>
 * </ul>
 * <p>La regla de propiedad vive en {@link Reserva#puedeGestionar}; el inventario usa la
 * misma regla al ejecutar, así que tampoco se puede saltar llamando directo al receptor.</p>
 */
public class FiltroAutorizacion extends EslabonValidacion {

    @Override
    public String nombre() {
        return "Permisos";
    }

    @Override
    protected Optional<String> revisar(SolicitudMovimiento s, ConsultasInventario inventario) {
        if (!s.rolesAutorizados().contains(s.actor().rol())) {
            return Optional.of("El rol " + s.actor().rol() + " de " + s.actor().nombre() + " no puede "
                    + s.tipo() + " (autorizados: " + s.rolesAutorizados() + ")");
        }
        if (s.reservaActiva() != null) {
            Optional<Reserva> reserva = inventario.reserva(s.reservaActiva());
            if (reserva.isPresent() && !reserva.get().puedeGestionar(s.actor())) {
                return Optional.of("Solo el dueno de la reserva " + s.reservaActiva() + " ("
                        + reserva.get().dueno().nombre() + ") o un supervisor puede " + s.tipo());
            }
        }
        return Optional.empty();
    }
}
