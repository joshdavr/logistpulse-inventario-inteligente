package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;
import ec.edu.usfq.logistpulse.inventario.Reserva;
import ec.edu.usfq.logistpulse.state.VistaLote;

import java.util.Optional;

/**
 * <b>Eslabón 3 (CONCRETE HANDLER) — Lote:</b> los lotes y reservas involucrados sirven.
 *
 * <p>Revisa, en orden, solo los requisitos que la solicitud declara:</p>
 * <ul>
 *   <li>lote nuevo: el código no existe todavía en esa bodega;</li>
 *   <li>pedido sin reserva activa: no se reserva dos veces el mismo pedido;</li>
 *   <li>asignables: hay suficientes unidades almacenadas y no vencidas para la estrategia;</li>
 *   <li>reserva activa: la reserva del pedido existe, sigue activa y es de ese SKU y bodega;</li>
 *   <li>lote existente: existe, es del SKU, no está vencido (si se exige) y tiene las unidades libres.</li>
 * </ul>
 * <p>No hay {@code switch} por tipo de operación. Si la transición es legal para el
 * estado del lote lo decide el patrón State al ejecutar.</p>
 */
public class FiltroLote extends EslabonValidacion {

    @Override
    public String nombre() {
        return "Lote";
    }

    @Override
    protected boolean atiende(SolicitudMovimiento s) {
        return s.loteNuevo() != null || s.pedidoSinReservaActiva() != null || s.asignablesRequeridas() > 0
                || s.reservaActiva() != null || s.loteExistente() != null;
    }

    @Override
    protected Optional<String> revisar(SolicitudMovimiento s, ConsultasInventario inventario) {
        if (s.loteNuevo() != null && inventario.lote(s.loteNuevo(), s.bodega()).isPresent()) {
            return Optional.of("El lote " + s.loteNuevo() + " ya existe en " + s.bodega());
        }
        if (s.pedidoSinReservaActiva() != null) {
            Optional<Reserva> previa = inventario.reserva(s.pedidoSinReservaActiva());
            if (previa.isPresent() && previa.get().activa()) {
                return Optional.of("El pedido " + s.pedidoSinReservaActiva() + " ya tiene una reserva activa");
            }
        }
        if (s.asignablesRequeridas() > 0) {
            int asignables = inventario.stockAsignable(s.sku(), s.bodega());
            if (asignables < s.asignablesRequeridas()) {
                int noAptas = inventario.stockLibre(s.sku(), s.bodega()) - asignables;
                return Optional.of("Solo hay " + asignables + " unidades asignables de " + s.sku() + " en "
                        + s.bodega() + " (" + noAptas + " libres estan vencidas, en cuarentena o sin almacenar)");
            }
        }
        if (s.reservaActiva() != null) {
            Optional<String> motivo = revisarReserva(s, inventario);
            if (motivo.isPresent()) {
                return motivo;
            }
        }
        if (s.loteExistente() != null) {
            return revisarLote(s, inventario);
        }
        return Optional.empty();
    }

    private Optional<String> revisarReserva(SolicitudMovimiento s, ConsultasInventario inventario) {
        Optional<Reserva> reserva = inventario.reserva(s.reservaActiva());
        if (reserva.isEmpty()) {
            return Optional.of("El pedido " + s.reservaActiva() + " no tiene reserva");
        }
        Reserva r = reserva.get();
        if (!r.activa()) {
            return Optional.of("La reserva del pedido " + s.reservaActiva() + " ya esta " + r.estado());
        }
        if (!r.sku().equals(s.sku()) || !r.bodega().equals(s.bodega())) {
            return Optional.of("La reserva " + s.reservaActiva() + " es de " + r.sku() + " en " + r.bodega());
        }
        return Optional.empty();
    }

    private Optional<String> revisarLote(SolicitudMovimiento s, ConsultasInventario inventario) {
        Optional<VistaLote> encontrado = inventario.lote(s.loteExistente(), s.bodega());
        if (encontrado.isEmpty()) {
            return Optional.of("El lote " + s.loteExistente() + " no existe en " + s.bodega());
        }
        VistaLote lote = encontrado.get();
        if (!lote.sku().equals(s.sku())) {
            return Optional.of("El lote " + lote.codigo() + " es del SKU " + lote.sku() + ", no de " + s.sku());
        }
        if (s.loteNoVencido() && lote.vencidoEn(inventario.hoy())) {
            return Optional.of("El lote " + lote.codigo() + " vencio el " + lote.vencimiento());
        }
        if (lote.disponible() < s.libresDelLote()) {
            return Optional.of("El lote " + lote.codigo() + " solo tiene " + lote.disponible()
                    + " unidades libres (se piden " + s.libresDelLote() + ")");
        }
        return Optional.empty();
    }
}
