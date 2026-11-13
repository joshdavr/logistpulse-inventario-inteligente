package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Cantidades;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * <b>Rol en el patrón Chain of Responsibility: REQUEST.</b>
 *
 * <p>Es <b>declarativa</b>: cada comando dice qué necesita que se revise (qué roles
 * pueden pedirla, cuántas unidades libres consume, qué lote usa, qué espacio ocupa...)
 * y cada eslabón revisa solo el requisito que le toca. Ningún eslabón hace
 * {@code switch} por tipo de operación, así que una operación nueva no obliga a
 * modificar la cadena (O de SOLID).</p>
 *
 * <p>Se construye con {@link #para}. Una solicitud mal formada (cantidades fuera de
 * rango, bodegas iguales, sin roles) ni siquiera se puede construir.</p>
 */
public final class SolicitudMovimiento {

    private final TipoMovimiento tipo;
    private final Actor actor;
    private final String sku;
    private final String bodega;
    private final String bodegaDestino;
    private final String referencia;
    private final int cantidad;
    private final String descripcion;
    private final Set<Rol> rolesAutorizados;
    private final boolean requiereSkuActivo;
    private final int consumeLibres;
    private final String loteExistente;
    private final int libresDelLote;
    private final boolean loteNoVencido;
    private final String loteNuevo;
    private final int asignablesRequeridas;
    private final String pedidoSinReservaActiva;
    private final String reservaActiva;
    private final Espacio espacio;

    private SolicitudMovimiento(Constructor c) {
        this.tipo = c.tipo;
        this.actor = c.actor;
        this.sku = c.sku;
        this.bodega = c.bodega;
        this.bodegaDestino = c.bodegaDestino;
        this.referencia = c.referencia;
        this.cantidad = c.cantidad;
        this.descripcion = c.descripcion;
        this.rolesAutorizados = Collections.unmodifiableSet(EnumSet.copyOf(c.roles));
        this.requiereSkuActivo = c.requiereSkuActivo;
        this.consumeLibres = c.consumeLibres;
        this.loteExistente = c.loteExistente;
        this.libresDelLote = c.libresDelLote;
        this.loteNoVencido = c.loteNoVencido;
        this.loteNuevo = c.loteNuevo;
        this.asignablesRequeridas = c.asignables;
        this.pedidoSinReservaActiva = c.pedidoSinReservaActiva;
        this.reservaActiva = c.reservaActiva;
        this.espacio = c.espacio;
    }

    public static Constructor para(TipoMovimiento tipo, Actor actor, String sku, String bodega) {
        return new Constructor(tipo, actor, sku, bodega);
    }

    // ------------------------------------------------------------------ datos generales

    public TipoMovimiento tipo() { return tipo; }
    public Actor actor() { return actor; }
    public String sku() { return sku; }
    public String bodega() { return bodega; }
    public String bodegaDestino() { return bodegaDestino; }
    public String referencia() { return referencia; }
    public int cantidad() { return cantidad; }
    public String descripcion() { return descripcion; }

    /** Unidades que mueve la operación (siempre >= 0, sin riesgo de desborde). */
    public int unidades() {
        return Math.abs(cantidad);
    }

    // ------------------------------------------------------------------ requisitos (cada eslabón lee el suyo)

    public Set<Rol> rolesAutorizados() { return rolesAutorizados; }
    public boolean requiereSkuActivo() { return requiereSkuActivo; }
    /** Unidades libres del SKU que consume en la bodega de origen (0 = no consume). */
    public int consumeLibres() { return consumeLibres; }
    /** Lote que debe existir (null = no usa un lote concreto). */
    public String loteExistente() { return loteExistente; }
    /** Unidades libres que necesita del lote existente (0 = ninguna). */
    public int libresDelLote() { return libresDelLote; }
    public boolean loteNoVencido() { return loteNoVencido; }
    /** Código de lote que NO debe existir todavía (null = no aplica). */
    public String loteNuevo() { return loteNuevo; }
    /** Unidades que la estrategia de picking debe poder asignar (0 = no aplica). */
    public int asignablesRequeridas() { return asignablesRequeridas; }
    /** Pedido que no debe tener ya una reserva activa (null = no aplica). */
    public String pedidoSinReservaActiva() { return pedidoSinReservaActiva; }
    /** Pedido cuya reserva debe existir y estar activa (null = no aplica). */
    public String reservaActiva() { return reservaActiva; }
    /** Espacio que ocupa (null = no ocupa espacio). */
    public Espacio espacio() { return espacio; }

    /** Todos los datos que identifican la operación (para la idempotencia por huella). */
    public String huella() {
        return String.join("|", tipo.name(), actor.nombre(), actor.tipo().name(), sku, bodega,
                String.valueOf(bodegaDestino), String.valueOf(loteExistente), String.valueOf(loteNuevo),
                String.valueOf(referencia), String.valueOf(cantidad));
    }

    @Override
    public String toString() {
        return descripcion;
    }

    // ================================================================== constructor paso a paso

    /** Arma la solicitud y valida todo al final, en {@link #construir()}. */
    public static final class Constructor {

        private final TipoMovimiento tipo;
        private final Actor actor;
        private final String sku;
        private final String bodega;
        private String bodegaDestino;
        private String referencia;
        private int cantidad;
        private String descripcion;
        private final Set<Rol> roles = EnumSet.noneOf(Rol.class);
        private boolean requiereSkuActivo;
        private int consumeLibres;
        private String loteExistente;
        private int libresDelLote;
        private boolean loteNoVencido;
        private String loteNuevo;
        private int asignables;
        private String pedidoSinReservaActiva;
        private String reservaActiva;
        private Espacio espacio;

        private Constructor(TipoMovimiento tipo, Actor actor, String sku, String bodega) {
            this.tipo = Objects.requireNonNull(tipo, "tipo");
            this.actor = Objects.requireNonNull(actor, "actor");
            this.sku = Objects.requireNonNull(sku, "sku");
            this.bodega = Objects.requireNonNull(bodega, "bodega");
        }

        public Constructor descripcion(String texto) {
            this.descripcion = texto;
            return this;
        }

        public Constructor autorizados(Rol... permitidos) {
            Collections.addAll(roles, permitidos);
            return this;
        }

        public Constructor skuActivo() {
            this.requiereSkuActivo = true;
            return this;
        }

        public Constructor consumeLibres(int unidades) {
            this.consumeLibres = Cantidades.exigirPositiva(unidades, "Las unidades libres a consumir");
            return this;
        }

        /**
         * @param libresNecesarias unidades libres que debe tener el lote (0 si no consume)
         * @param noVencido        true si un lote vencido no sirve para esta operación
         */
        public Constructor loteExistente(String lote, int libresNecesarias, boolean noVencido) {
            this.loteExistente = Objects.requireNonNull(lote, "lote");
            this.libresDelLote = Cantidades.exigirNoNegativa(libresNecesarias, "Las unidades del lote");
            this.loteNoVencido = noVencido;
            return this;
        }

        public Constructor loteNuevo(String lote) {
            this.loteNuevo = Objects.requireNonNull(lote, "lote");
            return this;
        }

        public Constructor asignables(int unidades) {
            this.asignables = Cantidades.exigirPositiva(unidades, "Las unidades a asignar");
            return this;
        }

        public Constructor pedidoSinReservaActiva(String pedido) {
            this.pedidoSinReservaActiva = Objects.requireNonNull(pedido, "pedido");
            return this;
        }

        public Constructor reservaActiva(String pedido) {
            this.reservaActiva = Objects.requireNonNull(pedido, "pedido");
            return this;
        }

        public Constructor ocupa(Espacio requerido) {
            this.espacio = Objects.requireNonNull(requerido, "espacio");
            return this;
        }

        public Constructor destino(String bodegaDestino) {
            this.bodegaDestino = Objects.requireNonNull(bodegaDestino, "bodegaDestino");
            return this;
        }

        public Constructor referencia(String pedido) {
            this.referencia = Objects.requireNonNull(pedido, "referencia");
            return this;
        }

        /** Cantidad informativa; en un ajuste puede ser negativa. */
        public Constructor cantidad(int valor) {
            if (valor < -Cantidades.MAXIMO || valor > Cantidades.MAXIMO) {
                throw new IllegalArgumentException("La cantidad debe estar entre -" + Cantidades.MAXIMO
                        + " y " + Cantidades.MAXIMO + " (fue " + valor + ")");
            }
            this.cantidad = valor;
            return this;
        }

        public SolicitudMovimiento construir() {
            if (descripcion == null || descripcion.isBlank()) {
                throw new IllegalArgumentException("La solicitud necesita una descripcion");
            }
            if (roles.isEmpty()) {
                throw new IllegalArgumentException("La solicitud debe declarar que roles la pueden pedir");
            }
            if (bodega.equals(bodegaDestino)) {
                throw new IllegalArgumentException("Origen y destino no pueden ser la misma bodega");
            }
            return new SolicitudMovimiento(this);
        }
    }
}
