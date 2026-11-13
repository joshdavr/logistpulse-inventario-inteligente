package ec.edu.usfq.logistpulse.inventario;

import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Bodega;
import ec.edu.usfq.logistpulse.dominio.Cantidades;
import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;
import ec.edu.usfq.logistpulse.observer.PublicadorEventos;
import ec.edu.usfq.logistpulse.observer.StockCambiado;
import ec.edu.usfq.logistpulse.state.Lote;
import ec.edu.usfq.logistpulse.state.TransicionInvalidaException;
import ec.edu.usfq.logistpulse.state.VistaLote;
import ec.edu.usfq.logistpulse.strategy.picking.AsignacionLote;
import ec.edu.usfq.logistpulse.strategy.picking.AsignadorLotes;
import ec.edu.usfq.logistpulse.strategy.picking.PlanPicking;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * El inventario en memoria. Implementa las dos caras: {@link ConsultasInventario}
 * (lectura) y {@link OperacionesInventario} (el RECEIVER del Command).
 *
 * <p>Se cruza con los otros patrones solo a través de abstracciones inyectadas:</p>
 * <ul>
 *   <li><b>Strategy:</b> pide el plan de lotes a un {@link AsignadorLotes}; no elige lotes por su cuenta.</li>
 *   <li><b>State:</b> cada {@link Lote} decide si la operación es una transición válida.</li>
 *   <li><b>Observer:</b> al terminar cada operación publica un {@link StockCambiado} por lote tocado.</li>
 * </ul>
 *
 * <p><b>Defensa en profundidad:</b> cada operación valida capacidad, stock, vencimiento y
 * dueño por su cuenta, con las mismas reglas que usa la cadena. Si alguien llama directo
 * al receptor saltándose la cadena, igual no puede dejar el inventario inconsistente.</p>
 */
public class Inventario implements ConsultasInventario, OperacionesInventario {

    private final Clock reloj;
    private final PublicadorEventos eventos;
    private final AsignadorLotes asignador;

    private final Map<String, ProductoSku> catalogo = new LinkedHashMap<>();
    private final Map<String, Bodega> bodegas = new LinkedHashMap<>();
    private final Map<String, Lote> lotes = new LinkedHashMap<>();       // clave: codigoLote@bodega
    private final Map<String, Reserva> reservas = new LinkedHashMap<>(); // clave: pedido

    public Inventario(Clock reloj, PublicadorEventos eventos, AsignadorLotes asignador) {
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.asignador = Objects.requireNonNull(asignador, "asignador");
    }

    // =================================================================== carga de datos (no publica eventos)

    public void registrarSku(ProductoSku producto) {
        catalogo.put(producto.codigo(), producto);
    }

    public void registrarBodega(Bodega bodega) {
        bodegas.put(bodega.codigo(), bodega);
    }

    /** Saldo que ya existía antes de arrancar el sistema (migración). También respeta la capacidad. */
    public void registrarSaldoInicial(Lote lote) {
        exigirSku(lote.sku());
        String clave = clave(lote.codigo(), lote.bodega());
        if (lotes.containsKey(clave)) {
            throw new IllegalArgumentException("El lote ya existe: " + clave);
        }
        exigirEspacio(lote.bodega(), TipoEspacio.FISICO, lote.fisico());
        lotes.put(clave, lote);
    }

    // =================================================================== consultas

    @Override
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    @Override
    public Optional<ProductoSku> sku(String codigo) {
        return Optional.ofNullable(catalogo.get(codigo));
    }

    @Override
    public Optional<Bodega> bodega(String codigo) {
        return Optional.ofNullable(bodegas.get(codigo));
    }

    @Override
    public List<Bodega> bodegas() {
        return List.copyOf(bodegas.values());
    }

    @Override
    public Optional<VistaLote> lote(String codigoLote, String bodega) {
        return Optional.ofNullable(lotes.get(clave(codigoLote, bodega)));
    }

    @Override
    public List<VistaLote> lotes() {
        return List.copyOf(lotes.values());
    }

    @Override
    public List<VistaLote> lotesDe(String sku, String bodega) {
        return List.copyOf(lotesInternos(sku, bodega));
    }

    @Override
    public Optional<Reserva> reserva(String pedido) {
        return Optional.ofNullable(reservas.get(pedido));
    }

    @Override
    public int stockLibre(String sku, String bodega) {
        return sumarDisponible(sku, bodega, l -> true);
    }

    @Override
    public int stockAsignable(String sku, String bodega) {
        LocalDate hoy = hoy();
        return sumarDisponible(sku, bodega, l -> l.asignableEn(hoy));
    }

    @Override
    public int stockUtilizable(String sku, String bodega) {
        LocalDate hoy = hoy();
        return sumarDisponible(sku, bodega, l -> l.utilizableEn(hoy));
    }

    @Override
    public int fisicoEnBodega(String bodega) {
        return lotes.values().stream().filter(l -> l.bodega().equals(bodega)).mapToInt(Lote::fisico).sum();
    }

    @Override
    public int reservadoEnBodega(String bodega) {
        return lotes.values().stream().filter(l -> l.bodega().equals(bodega)).mapToInt(Lote::reservado).sum();
    }

    @Override
    public Optional<String> verificarEspacio(String codigoBodega, TipoEspacio tipo, int unidades) {
        Bodega b = bodegas.get(codigoBodega);
        if (b == null) {
            return Optional.of("La bodega " + codigoBodega + " no existe");
        }
        long usado = tipo.usado(this, codigoBodega);
        if (usado + (long) unidades > tipo.limite(b)) { // en long: no se desborda
            return Optional.of(tipo.motivo(b, usado, unidades));
        }
        return Optional.empty();
    }

    private List<Lote> lotesInternos(String sku, String bodega) {
        return lotes.values().stream().filter(l -> l.sku().equals(sku) && l.bodega().equals(bodega)).toList();
    }

    private int sumarDisponible(String sku, String bodega, Predicate<Lote> filtro) {
        return lotesInternos(sku, bodega).stream().filter(filtro).mapToInt(Lote::disponible).sum();
    }

    // =================================================================== operaciones (RECEIVER del Command)

    @Override
    public void recibirLote(String codigo, String sku, String bodega, int unidades, LocalDate vencimiento) {
        Cantidades.exigirPositiva(unidades, "La cantidad recibida");
        exigirSkuActivo(sku);
        String clave = clave(codigo, bodega);
        if (lotes.containsKey(clave)) {
            throw new IllegalStateException("El lote " + clave + " ya existe");
        }
        exigirEspacio(bodega, TipoEspacio.FISICO, unidades);
        Lote nuevo = Lote.recibido(codigo, sku, bodega, unidades, hoy(), vencimiento);
        lotes.put(clave, nuevo);
        publicar(List.of(Foto.nuevo(nuevo)), TipoMovimiento.RECIBIR, null);
    }

    @Override
    public void almacenar(String codigo, String bodega) {
        Lote lote = exigirLote(codigo, bodega);
        Foto foto = Foto.de(lote);
        lote.almacenar();
        publicar(List.of(foto), TipoMovimiento.ALMACENAR, null);
    }

    @Override
    public Reserva reservar(String pedido, String sku, String bodega, int unidades, Actor dueno) {
        Cantidades.exigirPositiva(unidades, "La cantidad a reservar");
        Objects.requireNonNull(dueno, "dueno");
        ProductoSku producto = exigirSkuActivo(sku);
        Reserva previa = reservas.get(pedido);
        if (previa != null && previa.activa()) {
            throw new IllegalStateException("El pedido " + pedido + " ya tiene una reserva activa");
        }
        exigirEspacio(bodega, TipoEspacio.ALISTAMIENTO, unidades);
        PlanPicking plan = asignador.planificar(producto, lotesInternos(sku, bodega), unidades, hoy());
        if (!plan.cubreTodo()) {
            throw new IllegalStateException("La politica " + plan.politica() + " solo cubre "
                    + plan.total() + " de " + unidades + " unidades");
        }
        // Se valida todo el plan antes de tocar un solo lote: la reserva es atomica.
        List<Lote> elegidos = new ArrayList<>();
        for (AsignacionLote a : plan.asignaciones()) {
            Lote lote = exigirLote(a.lote(), bodega);
            if (!lote.asignableEn(hoy()) || lote.disponible() < a.cantidad()) {
                throw new IllegalStateException("El plan de picking eligio el lote " + a.lote() + " que no sirve");
            }
            elegidos.add(lote);
        }
        List<Foto> fotos = new ArrayList<>();
        for (int i = 0; i < elegidos.size(); i++) {
            fotos.add(Foto.de(elegidos.get(i)));
            elegidos.get(i).reservar(plan.asignaciones().get(i).cantidad());
        }
        Reserva reserva = new Reserva(pedido, sku, bodega, plan.politica(), dueno, plan.asignaciones());
        reservas.put(pedido, reserva);
        publicar(fotos, TipoMovimiento.RESERVAR, pedido);
        return reserva;
    }

    @Override
    public void liberarReserva(String pedido, Actor solicitante) {
        Reserva reserva = exigirReservaGestionable(pedido, solicitante);
        List<Foto> fotos = new ArrayList<>();
        for (AsignacionLote a : reserva.lineas()) {
            Lote lote = exigirLote(a.lote(), reserva.bodega());
            fotos.add(Foto.de(lote));
            lote.liberar(a.cantidad());
        }
        reserva.cambiarA(Reserva.Estado.LIBERADA);
        publicar(fotos, TipoMovimiento.LIBERAR, pedido);
    }

    @Override
    public void despachar(String pedido, Actor solicitante) {
        Reserva reserva = exigirReservaGestionable(pedido, solicitante);
        List<Foto> fotos = new ArrayList<>();
        for (AsignacionLote a : reserva.lineas()) {
            Lote lote = exigirLote(a.lote(), reserva.bodega());
            fotos.add(Foto.de(lote));
            lote.despachar(a.cantidad());
        }
        reserva.cambiarA(Reserva.Estado.DESPACHADA);
        publicar(fotos, TipoMovimiento.DESPACHAR, pedido);
    }

    @Override
    public void ajustar(String codigo, String bodega, int delta) {
        Cantidades.exigirAjuste(delta);
        Lote lote = exigirLote(codigo, bodega);
        if (delta > 0) {
            exigirEspacio(bodega, TipoEspacio.FISICO, delta);
        }
        Foto foto = Foto.de(lote);
        lote.ajustar(delta);
        publicar(List.of(foto), TipoMovimiento.AJUSTAR, null);
    }

    @Override
    public void transferir(String codigo, String origen, String destino, int unidades) {
        Cantidades.exigirPositiva(unidades, "La cantidad a transferir");
        if (origen.equals(destino)) {
            throw new IllegalArgumentException("Origen y destino no pueden ser la misma bodega");
        }
        Lote desde = exigirLote(codigo, origen);
        if (desde.vencidoEn(hoy())) {
            throw new IllegalStateException("El lote " + codigo + " esta vencido y no se transfiere");
        }
        exigirEspacio(destino, TipoEspacio.FISICO, unidades);
        Lote hacia = lotes.get(clave(codigo, destino));
        // Se valida el destino ANTES de tocar el origen: la transferencia es atomica.
        if (hacia != null && !hacia.admiteIngreso()) {
            throw new TransicionInvalidaException(hacia, "recibir una transferencia");
        }
        Foto fotoDesde = Foto.de(desde);
        desde.retirar(unidades); // el estado del lote de origen decide si se puede
        Foto fotoHacia;
        if (hacia == null) {
            hacia = desde.paraTraslado(destino);
            lotes.put(clave(codigo, destino), hacia);
            fotoHacia = Foto.nuevo(hacia);
        } else {
            fotoHacia = Foto.de(hacia);
        }
        hacia.ingresar(unidades);
        publicar(List.of(fotoDesde, fotoHacia), TipoMovimiento.TRANSFERIR, origen + "->" + destino);
    }

    @Override
    public void ponerEnCuarentena(String codigo, String bodega) {
        Lote lote = exigirLote(codigo, bodega);
        Foto foto = Foto.de(lote);
        lote.ponerEnCuarentena();
        publicar(List.of(foto), TipoMovimiento.CUARENTENA, null);
    }

    @Override
    public void liberarCuarentena(String codigo, String bodega) {
        Lote lote = exigirLote(codigo, bodega);
        Foto foto = Foto.de(lote);
        lote.liberarCuarentena();
        publicar(List.of(foto), TipoMovimiento.LIBERAR_CUARENTENA, null);
    }

    // =================================================================== Observer: publicar al terminar

    /** Estado de un lote antes de una operación, para calcular qué cambió. */
    private record Foto(Lote lote, String estado, int fisico, int reservado) {
        static Foto de(Lote l) {
            return new Foto(l, l.nombreEstado(), l.fisico(), l.reservado());
        }

        static Foto nuevo(Lote l) {
            return new Foto(l, "NUEVO", 0, 0);
        }

        StockCambiado cambio(Instant momento, TipoMovimiento movimiento, String referencia) {
            return new StockCambiado(momento, movimiento, lote.sku(), lote.bodega(), lote.codigo(),
                    lote.fisico() - fisico, lote.reservado() - reservado, estado, lote.nombreEstado(), referencia);
        }
    }

    /** Se publica solo cuando la operación terminó completa: nadie ve un estado a medias. */
    private void publicar(List<Foto> fotos, TipoMovimiento movimiento, String referencia) {
        Instant ahora = reloj.instant();
        for (Foto foto : fotos) {
            eventos.publicar(foto.cambio(ahora, movimiento, referencia));
        }
    }

    // =================================================================== reglas compartidas

    private ProductoSku exigirSku(String sku) {
        ProductoSku p = catalogo.get(sku);
        if (p == null) {
            throw new IllegalStateException("SKU desconocido: " + sku);
        }
        return p;
    }

    private ProductoSku exigirSkuActivo(String sku) {
        ProductoSku p = exigirSku(sku);
        if (!p.activo()) {
            throw new IllegalStateException("El SKU " + sku + " esta descontinuado");
        }
        return p;
    }

    private void exigirEspacio(String bodega, TipoEspacio tipo, int unidades) {
        Optional<String> motivo = verificarEspacio(bodega, tipo, unidades);
        if (motivo.isPresent()) {
            throw new IllegalStateException(motivo.get());
        }
    }

    private Lote exigirLote(String codigo, String bodega) {
        Lote lote = lotes.get(clave(codigo, bodega));
        if (lote == null) {
            throw new IllegalStateException("No existe el lote " + codigo + " en " + bodega);
        }
        return lote;
    }

    private Reserva exigirReservaGestionable(String pedido, Actor solicitante) {
        Reserva r = reservas.get(pedido);
        if (r == null || !r.activa()) {
            throw new IllegalStateException("El pedido " + pedido + " no tiene una reserva activa");
        }
        if (!r.puedeGestionar(solicitante)) {
            throw new IllegalStateException("Solo el dueno de la reserva " + pedido + " ("
                    + r.dueno().nombre() + ") o un supervisor puede gestionarla");
        }
        return r;
    }

    private static String clave(String codigoLote, String bodega) {
        return codigoLote + "@" + bodega;
    }
}
