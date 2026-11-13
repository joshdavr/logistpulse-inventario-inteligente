package ec.edu.usfq.logistpulse;

import ec.edu.usfq.logistpulse.chain.CadenaValidacion;
import ec.edu.usfq.logistpulse.chain.EslabonValidacion;
import ec.edu.usfq.logistpulse.chain.FiltroAutorizacion;
import ec.edu.usfq.logistpulse.chain.FiltroCapacidad;
import ec.edu.usfq.logistpulse.chain.FiltroLimiteAgente;
import ec.edu.usfq.logistpulse.chain.FiltroLote;
import ec.edu.usfq.logistpulse.chain.FiltroSku;
import ec.edu.usfq.logistpulse.chain.FiltroStock;
import ec.edu.usfq.logistpulse.command.AjustarStock;
import ec.edu.usfq.logistpulse.command.AlmacenarLote;
import ec.edu.usfq.logistpulse.command.CentralDeComandos;
import ec.edu.usfq.logistpulse.command.ComandoInventario;
import ec.edu.usfq.logistpulse.command.DespacharPedido;
import ec.edu.usfq.logistpulse.command.EstadoDecision;
import ec.edu.usfq.logistpulse.command.LiberarCuarentena;
import ec.edu.usfq.logistpulse.command.LiberarReserva;
import ec.edu.usfq.logistpulse.command.PonerEnCuarentena;
import ec.edu.usfq.logistpulse.command.RecibirLote;
import ec.edu.usfq.logistpulse.command.ReservarPedido;
import ec.edu.usfq.logistpulse.command.Resultado;
import ec.edu.usfq.logistpulse.command.TransferirStock;
import ec.edu.usfq.logistpulse.consola.Reportes;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.dominio.TipoActor;
import ec.edu.usfq.logistpulse.observer.PulseFleet;
import ec.edu.usfq.logistpulse.observer.StockCambiado;
import ec.edu.usfq.logistpulse.observer.Suscriptor;
import ec.edu.usfq.logistpulse.state.VistaLote;
import ec.edu.usfq.logistpulse.strategy.picking.PickingFefo;
import ec.edu.usfq.logistpulse.strategy.picking.PickingFifo;
import ec.edu.usfq.logistpulse.strategy.picking.PickingLifo;
import ec.edu.usfq.logistpulse.strategy.picking.PoliticaPicking;
import ec.edu.usfq.logistpulse.strategy.reposicion.OrdenCompra;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static ec.edu.usfq.logistpulse.EscenarioDemo.*;

/**
 * Demo por consola: un día de operación en LOGISTPULSE Smart Inventory, con una
 * sección por patrón. Debajo de cada paso se ve qué hizo cada patrón:
 *
 * <pre>
 *   chain:      recorrido de la cadena de validación
 *   strategy:   lotes que eligió la política de picking
 *   evento:     StockCambiado publicado por lote (con la transición de State)
 *   reposicion: órdenes de compra que generó el suscriptor de reposición
 *   pulsefleet: tareas de robots que generó el suscriptor PULSEFLEET
 * </pre>
 *
 * <p>Para operar a mano, usa {@link MenuInteractivo}.</p>
 */
public class App {

    private static final String LINEA = "=".repeat(92);

    private final SistemaLogistPulse s;
    private final RelojManual reloj;
    private final List<StockCambiado> eventosDelPaso = new ArrayList<>();
    private int paso;

    App(SistemaLogistPulse sistema, RelojManual reloj) {
        this.s = sistema;
        this.reloj = reloj;
        // La consola es un suscriptor mas: asi muestra los eventos de cada paso.
        s.eventos().suscribir(StockCambiado.class, Suscriptor.con("Consola", eventosDelPaso::add));
    }

    public static void main(String[] args) {
        RelojManual reloj = EscenarioDemo.reloj();
        SistemaLogistPulse sistema = SistemaLogistPulse.crear(reloj);
        EscenarioDemo.cargarDatos(sistema.inventario());
        new App(sistema, reloj).ejecutar();
    }

    void ejecutar() {
        encabezado();
        seccionCommandYChain();
        seccionStrategy();
        seccionState();
        seccionObserver();
        resumenFinal();
    }

    // ======================================================================== secciones

    private void encabezado() {
        System.out.println(LINEA);
        System.out.println(" LOGISTPULSE Smart Inventory  |  Command + Chain + Strategy + State + Observer");
        System.out.println(" Fecha simulada: " + HOY + " 08:00 (" + ZONA + ")");
        System.out.println(" Cadena de validacion: " + CadenaValidacion.describir(s.cadena()));
        System.out.println(" Usuarios: " + USUARIOS);
        System.out.println(LINEA);
        System.out.println(" Catalogo y politicas (Strategy) por SKU:");
        for (String codigo : List.of(ARROZ, ACEITE, LECHE, CEMENTO, GALLETAS)) {
            ProductoSku p = s.inventario().sku(codigo).orElseThrow();
            System.out.printf("  %-8s %-22s %-10s picking %-5s reposicion %-8s ROP %3d%s%n",
                    p.codigo(), p.descripcion(), p.categoria(),
                    s.picking().politicaPara(p).nombre(), s.reposicion().politicaPara(p).nombre(),
                    p.perfil().puntoDeReorden(), p.activo() ? "" : "  (descontinuado)");
        }
        System.out.println();
        System.out.println(" Inventario inicial:");
        System.out.print(Reportes.inventario(s.inventario()));
    }

    private void seccionCommandYChain() {
        titulo("1. COMMAND + CHAIN OF RESPONSIBILITY",
                "Cada operacion es un objeto (Command) y antes de ejecutarse pasa por la cadena de validacion.");
        ejecutar("Andrea reserva 40 sacos de arroz para el pedido PED-2001",
                new ReservarPedido("REQ-5001", ANDREA, "PED-2001", ARROZ, NORTE, 40));
        ejecutar("La API no recibio respuesta (timeout) y reintenta la misma solicitud",
                new ReservarPedido("REQ-5001", ANDREA, "PED-2001", ARROZ, NORTE, 40));
        ejecutar("Por un error, la API reusa la clave REQ-5001 para OTRO pedido",
                new ReservarPedido("REQ-5001", ANDREA, "PED-2005", ARROZ, NORTE, 15));
        ejecutar("Mario intenta liberar la reserva de Andrea",
                new LiberarReserva("REQ-6001", MARIO, s.inventario().reserva("PED-2001").orElseThrow()));
        ejecutar("El agente intenta ajustar stock (solo supervision o calidad pueden)",
                new AjustarStock("IA-9000", AGENTE, ARROZ, NORTE, L_ARZ_0901, 10, "conteo"));
        ejecutar("El agente pide un SKU que no existe",
                new ReservarPedido("IA-9001", AGENTE, "PED-2090", "XYZ-000", NORTE, 5));
        ejecutar("El agente pide galletas, un SKU descontinuado",
                new ReservarPedido("IA-9002", AGENTE, "PED-2091", GALLETAS, SUR, 10));
        ejecutar("El agente pide mas arroz del que hay",
                new ReservarPedido("IA-9003", AGENTE, "PED-2092", ARROZ, NORTE, 120));
        ejecutar("Andrea pide aceite en el norte, pero el unico lote esta en cuarentena",
                new ReservarPedido("REQ-5002", ANDREA, "PED-2093", ACEITE, NORTE, 30));
        ejecutar("El agente reserva 155 de leche y la zona de alistamiento no da abasto",
                new ReservarPedido("IA-9004", AGENTE, "PED-2094", LECHE, NORTE, 155));

        subtitulo("Extension: se agrega el eslabon LimiteIA sin tocar comandos ni central");
        EslabonValidacion conLimite = CadenaValidacion.armar(new FiltroLimiteAgente(50), new FiltroAutorizacion(),
                new FiltroSku(), new FiltroStock(), new FiltroLote(), new FiltroCapacidad());
        CentralDeComandos centralConLimite = new CentralDeComandos(s.inventario(), s.inventario(), conLimite,
                s.eventos(), s.reloj());
        System.out.println("  Nueva cadena: " + CadenaValidacion.describir(conLimite));
        ejecutar(centralConLimite, "El agente quiere trasladar 60 de aceite al norte",
                new TransferirStock("IA-9005", AGENTE, ACEITE, L_ACE_1010, SUR, NORTE, 60));
        TransferirStock chico = new TransferirStock("IA-9006", AGENTE, ACEITE, L_ACE_1010, SUR, NORTE, 45);
        System.out.println("      (simulacion sin ejecutar) con 45 unidades la misma cadena aprueba:");
        System.out.println("         chain:      " + conLimite.evaluar(chico.solicitud(), s.inventario()).trazaLegible());

        subtitulo("Deshacer que falla no pierde el comando");
        Resultado traslado = ejecutar("Andrea traslada 45 de aceite al norte",
                new TransferirStock("REQ-5003", ANDREA, ACEITE, L_ACE_1010, SUR, NORTE, 45));
        ejecutar("Andrea reserva esos 45 de aceite (es el unico aceite asignable en el norte)",
                new ReservarPedido("REQ-5004", ANDREA, "PED-2006", ACEITE, NORTE, 45));
        compensar("Carlos intenta revertir el traslado, pero el aceite ya esta reservado",
                traslado.comandoId(), CARLOS);
        System.out.println("      " + traslado.comandoId() + " sigue en el historial ("
                + s.central().operacionesDeshacibles() + " operaciones deshacibles); nada se perdio.");
        deshacer("Carlos deshace la ultima operacion (la reserva; como supervisor puede)", CARLOS);
        deshacer("Carlos deshace la siguiente: ahora si se revierte el traslado", CARLOS);

        subtitulo("Capacidad");
        ejecutar("Llega un camion con 300 sacos de cemento, pero no caben",
                new RecibirLote("REQ-5005", ANDREA, CEMENTO, NORTE, "L-CEM-1112", 300, LocalDate.of(2027, 6, 30)));

        subtitulo("Defensa en profundidad: llamar directo al receptor, saltandose la cadena");
        directo("ajustar +500 de cemento", () -> s.inventario().ajustar(L_CEM_0920, NORTE, 500));
        directo("transferir desde un lote en cuarentena", () -> s.inventario().transferir(L_ACE_0820, NORTE, SUR, 10));
        directo("Mario libera la reserva de Andrea", () -> s.inventario().liberarReserva("PED-2001", MARIO));
        directo("recibir Integer.MAX_VALUE unidades", () -> new RecibirLote("X", ANDREA, LECHE, SUR, "L-X",
                Integer.MAX_VALUE, LocalDate.of(2027, 1, 1)));
    }

    private void seccionStrategy() {
        titulo("2. STRATEGY",
                "La politica de picking solo ordena; el contexto filtra y reparte. La de reposicion decide cuanto pedir.");
        compararPicking(LECHE, NORTE, 100);
        ejecutar("Andrea reserva 100 de leche (PERECIBLE usa FEFO; el pedido se reparte entre 2 lotes)",
                new ReservarPedido("REQ-5006", ANDREA, "PED-2002", LECHE, NORTE, 100));
        ejecutar("Andrea reserva 20 de cemento (MATERIALES usa LIFO)",
                new ReservarPedido("REQ-5007", ANDREA, "PED-2003", CEMENTO, NORTE, 20));

        subtitulo("Cambio de estrategia en ejecucion");
        System.out.println("  El lote L-ARZ-1015 vence el 2026-12-20: Carlos cambia ARZ-001 de FIFO a FEFO.");
        s.picking().usarParaSku(ARROZ, new PickingFefo());
        compararPicking(ARROZ, NORTE, 25);
        ejecutar("Andrea reserva 25 de arroz, ahora con FEFO",
                new ReservarPedido("REQ-5008", ANDREA, "PED-2004", ARROZ, NORTE, 25));
    }

    private void seccionState() {
        titulo("3. STATE",
                "Cada lote recorre RECIBIDO -> ALMACENADO -> RESERVADO -> ENVIADO, con CUARENTENA como desvio.");
        ejecutar("Llega el lote L-LEC-1112 con 190 litros (la orden de reposicion de leche)",
                new RecibirLote("REQ-5009", ANDREA, LECHE, NORTE, "L-LEC-1112", 190, LocalDate.of(2026, 12, 10)));
        ejecutar("El lote se ubica en el rack",
                new AlmacenarLote("REQ-5010", ANDREA, LECHE, NORTE, "L-LEC-1112"));
        ejecutar("Lucia (calidad) intenta aislar L-LEC-1108, pero esta RESERVADO",
                new PonerEnCuarentena("QA-301", LUCIA, LECHE, NORTE, L_LEC_1108, "sospecha de cadena de frio"));
        Resultado despacho = ejecutar("Carlos despacha el pedido PED-2002 de Andrea (supervisor)",
                new DespacharPedido("REQ-5011", CARLOS, s.inventario().reserva("PED-2002").orElseThrow()));
        compensar("Alguien intenta deshacer el despacho", despacho.comandoId(), CARLOS);
        ejecutar("Lucia aisla el lote de leche vencido",
                new PonerEnCuarentena("QA-302", LUCIA, LECHE, NORTE, L_LEC_1001, "vencido, pendiente de destruccion"));
        ejecutar("Lucia aprueba el aceite que estaba retenido",
                new LiberarCuarentena("QA-303", LUCIA, ACEITE, NORTE, L_ACE_0820));
        ejecutar("Lucia intenta ajustar un lote que ya fue ENVIADO",
                new AjustarStock("QA-304", LUCIA, LECHE, NORTE, L_LEC_1108, 5, "conteo"));

        VistaLote enviado = s.inventario().lote(L_LEC_1108, NORTE).orElseThrow();
        System.out.println();
        System.out.println("  Historial de estados de " + enviado.codigo() + ": " + String.join(", ", enviado.transiciones()));
    }

    private void seccionObserver() {
        titulo("4. OBSERVER",
                "El inventario publica StockCambiado; Reposicion, PULSEFLEET y el Ledger reaccionan sin que el inventario los conozca.");
        System.out.println("  PULSEFLEET entra en mantenimiento: se da de baja de los eventos.");
        s.eventos().desuscribir(s.pulseFleet());
        Resultado traslado = ejecutar("Andrea traslada 50 de aceite al norte mientras la flota no esta",
                new TransferirStock("REQ-5012", ANDREA, ACEITE, L_ACE_1010, SUR, NORTE, 50));

        System.out.println();
        System.out.println("  PULSEFLEET vuelve a estar en linea: se suscribe otra vez.");
        s.eventos().suscribir(StockCambiado.class, s.pulseFleet());
        compensar("Carlos revierte el traslado; ahora si se crean tareas de robot", traslado.comandoId(), CARLOS);
        compensar("Mario pide la misma reversion por error", traslado.comandoId(), MARIO);
    }

    private void resumenFinal() {
        System.out.println();
        System.out.println(LINEA);
        System.out.println(" PULSEFLEET - tareas de robots:");
        s.pulseFleet().tareas().forEach(t -> System.out.println("  " + t.describir()));
        System.out.println();
        System.out.println(" Reposicion - ordenes de compra:");
        s.reposicion().ordenes().forEach(o -> System.out.println("  " + o.describir()));
        System.out.println();
        System.out.println(" Inventario final:");
        System.out.print(Reportes.inventario(s.inventario()));
        System.out.println();
        System.out.println(" Ledger - auditoria de decisiones (hora de Quito):");
        System.out.print(Reportes.ledger(s.ledger(), ZONA));
        System.out.println(LINEA);
        System.out.printf(" Decisiones: %d | ejecutadas %d | rechazadas %d | duplicadas %d | fallidas %d%n",
                s.ledger().decisiones().size(),
                s.ledger().contar(EstadoDecision.EJECUTADO),
                s.ledger().contar(EstadoDecision.RECHAZADO),
                s.ledger().contar(EstadoDecision.DUPLICADO),
                s.ledger().contar(EstadoDecision.FALLIDO));
        System.out.printf(" Humanos: %d | Agentes de IA: %d | todos por la misma cadena%n",
                s.ledger().contarPorActor(TipoActor.HUMANO), s.ledger().contarPorActor(TipoActor.AGENTE_IA));
        System.out.printf(" Eventos publicados: %d | movimientos de stock en el ledger: %d | fallos de entrega: %d%n",
                s.eventos().publicados(), s.ledger().movimientos().size(), s.eventos().fallosDeEntrega().size());
        System.out.println(LINEA);
    }

    // ======================================================================== ayudas de impresion

    private void compararPicking(String sku, String bodega, int unidades) {
        ProductoSku producto = s.inventario().sku(sku).orElseThrow();
        String activa = s.picking().politicaPara(producto).nombre();
        System.out.println();
        System.out.println("  Que lotes tomaria cada estrategia para " + unidades + " x " + sku + " en " + bodega + ":");
        for (PoliticaPicking p : List.of(new PickingFefo(), new PickingFifo(), new PickingLifo())) {
            String plan = s.picking().planificarCon(p, s.inventario().lotesDe(sku, bodega), unidades,
                    s.inventario().hoy()).describir();
            System.out.printf("     %s%s%n", plan, p.nombre().equals(activa) ? "   <- politica activa" : "");
        }
    }

    private Resultado ejecutar(String historia, ComandoInventario comando) {
        return ejecutar(s.central(), historia, comando);
    }

    private Resultado ejecutar(CentralDeComandos central, String historia, ComandoInventario comando) {
        Antes antes = Antes.tomar(s);
        Resultado r = central.despachar(comando);
        imprimir(historia, comando.describir() + "  | " + comando.actor(), r);
        if (comando instanceof ReservarPedido reserva && r.exitoso()) {
            reserva.reserva().ifPresent(res -> System.out.println("         strategy:   " + res.describir()));
        }
        imprimirReacciones(antes);
        return r;
    }

    private void compensar(String historia, String comandoId, Actor quien) {
        Antes antes = Antes.tomar(s);
        Resultado r = s.central().compensar(comandoId, quien);
        imprimir(historia, "COMPENSAR " + comandoId + "  | " + quien, r);
        imprimirReacciones(antes);
    }

    private void deshacer(String historia, Actor quien) {
        Antes antes = Antes.tomar(s);
        String objetivo = s.central().proximoADeshacer().orElse("-");
        Resultado r = s.central().deshacerUltimo(quien);
        imprimir(historia, "DESHACER ULTIMA (" + objetivo + ")  | " + quien, r);
        imprimirReacciones(antes);
    }

    private void directo(String descripcion, Runnable accion) {
        try {
            accion.run();
            System.out.println("  receptor directo: " + descripcion + " -> ACEPTADO (esto seria un error)");
        } catch (RuntimeException e) {
            System.out.println("  receptor directo: " + descripcion + " -> RECHAZADO: " + e.getMessage());
        }
    }

    private void imprimir(String historia, String operacion, Resultado r) {
        paso++;
        reloj.avanzar(Duration.ofMinutes(2));
        System.out.printf("%n [%02d] %s%n", paso, historia);
        System.out.println("      " + operacion);
        switch (r.estado()) {
            case EJECUTADO -> System.out.println("      => EJECUTADO " + r.comandoId());
            case DUPLICADO -> System.out.println("      => DUPLICADO  " + r.mensaje());
            default -> System.out.println("      => " + r.estado() + "  " + r.mensaje());
        }
        if (r.veredicto() != null) {
            System.out.println("         chain:      " + r.veredicto().trazaLegible());
        }
    }

    private void imprimirReacciones(Antes antes) {
        for (StockCambiado e : eventosDelPaso) {
            System.out.println("         evento:     " + e.describir());
        }
        eventosDelPaso.clear();
        List<OrdenCompra> ordenes = s.reposicion().ordenes();
        for (int i = 0; i < ordenes.size(); i++) {
            OrdenCompra o = ordenes.get(i);
            if (i >= antes.ordenes) {
                System.out.println("         reposicion: " + o.id() + " pedir " + o.cantidad() + " x " + o.sku()
                        + " a " + o.bodega() + " (" + o.politica() + ": " + o.razon() + ")");
            } else if (antes.pendientes.contains(o.id()) && !o.pendiente()) {
                System.out.println("         reposicion: " + o.id() + " queda RECIBIDA");
            }
        }
        List<PulseFleet.TareaAmr> tareas = s.pulseFleet().tareas();
        for (int i = antes.tareas; i < tareas.size(); i++) {
            System.out.println("         pulsefleet: " + tareas.get(i).describir());
        }
        if (!s.eventos().estaSuscrito(s.pulseFleet()) && antes.publicados < s.eventos().publicados()) {
            System.out.println("         pulsefleet: (dado de baja: no recibio los eventos)");
        }
    }

    /** Lo que había antes de un paso, para mostrar solo lo nuevo. */
    private record Antes(int ordenes, List<String> pendientes, int tareas, int publicados) {
        static Antes tomar(SistemaLogistPulse s) {
            return new Antes(s.reposicion().ordenes().size(),
                    s.reposicion().ordenes().stream().filter(OrdenCompra::pendiente).map(OrdenCompra::id).toList(),
                    s.pulseFleet().tareas().size(), s.eventos().publicados());
        }
    }

    private static void titulo(String nombre, String descripcion) {
        System.out.println();
        System.out.println(LINEA);
        System.out.println(" " + nombre);
        System.out.println(" " + descripcion);
        System.out.println(LINEA);
    }

    private static void subtitulo(String texto) {
        System.out.println();
        System.out.println("  --- " + texto + " ---");
    }
}
