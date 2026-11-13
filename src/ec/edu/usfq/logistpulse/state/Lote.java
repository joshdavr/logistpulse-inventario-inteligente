package ec.edu.usfq.logistpulse.state;

import ec.edu.usfq.logistpulse.dominio.Cantidades;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <b>Rol en el patrón State: CONTEXT.</b>
 *
 * <p>Un lote de un SKU dentro de una bodega. Guarda su estado actual y le delega
 * cada operación: el lote no tiene ningún {@code if (estado == ...)}.</p>
 *
 * <p>Campos privados y una invariante protegida en cada mutación
 * ({@code 0 <= reservado <= fisico}, lo que en SQL sería un CHECK). La aritmética
 * se hace en {@code long}, así que ningún valor se desborda.</p>
 */
public class Lote implements VistaLote {

    private final String codigo;
    private final String sku;
    private final String bodega;
    private final LocalDate fechaIngreso;
    private final LocalDate vencimiento;
    private int fisico;
    private int reservado;
    private EstadoLote estado;
    private final List<String> transiciones = new ArrayList<>();

    private Lote(String codigo, String sku, String bodega, int fisico,
                 LocalDate fechaIngreso, LocalDate vencimiento, EstadoLote inicial) {
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.sku = Objects.requireNonNull(sku, "sku");
        this.bodega = Objects.requireNonNull(bodega, "bodega");
        this.fechaIngreso = Objects.requireNonNull(fechaIngreso, "fechaIngreso");
        this.vencimiento = Objects.requireNonNull(vencimiento, "vencimiento");
        this.fisico = Cantidades.exigirNoNegativa(fisico, "Las unidades iniciales del lote");
        this.estado = inicial;
    }

    /** Lote que acaba de llegar al muelle: empieza RECIBIDO. */
    public static Lote recibido(String codigo, String sku, String bodega, int unidades,
                                LocalDate fechaIngreso, LocalDate vencimiento) {
        return new Lote(codigo, sku, bodega, unidades, fechaIngreso, vencimiento, new Recibido());
    }

    /** Saldo inicial que ya está en el rack (carga de datos): empieza ALMACENADO. */
    public static Lote almacenado(String codigo, String sku, String bodega, int unidades,
                                  LocalDate fechaIngreso, LocalDate vencimiento) {
        return new Lote(codigo, sku, bodega, unidades, fechaIngreso, vencimiento, new Almacenado());
    }

    /**
     * El mismo lote, vacío, en otra bodega, listo para recibir una transferencia.
     * Conserva la fecha de ingreso original para que FIFO siga siendo justo.
     */
    public Lote paraTraslado(String bodegaDestino) {
        return new Lote(codigo, sku, bodegaDestino, 0, fechaIngreso, vencimiento, new Almacenado());
    }

    // ------------------------------------------------------------- operaciones: todas delegan en el estado

    public void almacenar() {
        estado.almacenar(this);
    }

    public void reservar(int unidades) {
        estado.reservar(this, Cantidades.exigirPositiva(unidades, "La cantidad a reservar"));
    }

    public void liberar(int unidades) {
        estado.liberar(this, Cantidades.exigirPositiva(unidades, "La cantidad a liberar"));
    }

    public void despachar(int unidades) {
        estado.despachar(this, Cantidades.exigirPositiva(unidades, "La cantidad a despachar"));
    }

    public void ponerEnCuarentena() {
        estado.ponerEnCuarentena(this);
    }

    public void liberarCuarentena() {
        estado.liberarCuarentena(this);
    }

    public void ajustar(int delta) {
        estado.ajustar(this, Cantidades.exigirAjuste(delta));
    }

    public void retirar(int unidades) {
        estado.retirar(this, Cantidades.exigirPositiva(unidades, "La cantidad a transferir"));
    }

    public void ingresar(int unidades) {
        estado.ingresar(this, Cantidades.exigirPositiva(unidades, "La cantidad recibida"));
    }

    /** Lo usa el inventario para validar el destino de una transferencia antes de tocar el origen. */
    public boolean admiteIngreso() {
        return estado.admiteIngreso();
    }

    // ------------------------------------------------------------- usados solo por los estados

    void cambiarA(EstadoLote nuevo) {
        if (!nuevo.nombre().equals(estado.nombre())) {
            transiciones.add(estado.nombre() + " -> " + nuevo.nombre());
        }
        estado = nuevo;
    }

    void moverFisico(int delta) {
        long nuevo = (long) fisico + delta;
        if (nuevo < 0 || nuevo < reservado || nuevo > Integer.MAX_VALUE) {
            throw new IllegalStateException("Invariante: el lote " + codigo + " quedaria con " + nuevo
                    + " unidades fisicas y " + reservado + " reservadas");
        }
        fisico = (int) nuevo;
    }

    void moverReservado(int delta) {
        long nuevo = (long) reservado + delta;
        if (nuevo < 0 || nuevo > fisico) {
            throw new IllegalStateException("Invariante: el lote " + codigo + " quedaria con " + nuevo
                    + " unidades reservadas y " + fisico + " fisicas");
        }
        reservado = (int) nuevo;
    }

    /** En el rack, el lote está RESERVADO si ya no le quedan unidades libres; si no, ALMACENADO. */
    void ubicarSegunDisponibilidad() {
        cambiarA(disponible() == 0 && reservado > 0 ? new Reservado() : new Almacenado());
    }

    // ------------------------------------------------------------- consultas (VistaLote)

    @Override
    public int disponible() {
        return fisico - reservado;
    }

    @Override
    public boolean vencidoEn(LocalDate hoy) {
        return vencimiento.isBefore(hoy);
    }

    @Override
    public boolean asignableEn(LocalDate hoy) {
        return estado.admiteAsignacion() && !vencidoEn(hoy) && disponible() > 0;
    }

    @Override
    public boolean utilizableEn(LocalDate hoy) {
        return estado.esUtilizable() && !vencidoEn(hoy);
    }

    @Override public String codigo() { return codigo; }
    @Override public String sku() { return sku; }
    @Override public String bodega() { return bodega; }
    @Override public LocalDate fechaIngreso() { return fechaIngreso; }
    @Override public LocalDate vencimiento() { return vencimiento; }
    @Override public int fisico() { return fisico; }
    @Override public int reservado() { return reservado; }
    @Override public String nombreEstado() { return estado.nombre(); }

    @Override
    public List<String> transiciones() {
        return Collections.unmodifiableList(transiciones);
    }

    @Override
    public String toString() {
        return codigo + "@" + bodega + " [" + estado.nombre() + " fisico=" + fisico + " reservado=" + reservado + "]";
    }
}
