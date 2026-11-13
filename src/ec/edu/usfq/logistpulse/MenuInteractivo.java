package ec.edu.usfq.logistpulse;

import ec.edu.usfq.logistpulse.command.AjustarStock;
import ec.edu.usfq.logistpulse.command.AlmacenarLote;
import ec.edu.usfq.logistpulse.command.ComandoInventario;
import ec.edu.usfq.logistpulse.command.DespacharPedido;
import ec.edu.usfq.logistpulse.command.LiberarCuarentena;
import ec.edu.usfq.logistpulse.command.LiberarReserva;
import ec.edu.usfq.logistpulse.command.PonerEnCuarentena;
import ec.edu.usfq.logistpulse.command.RecibirLote;
import ec.edu.usfq.logistpulse.command.ReservarPedido;
import ec.edu.usfq.logistpulse.command.Resultado;
import ec.edu.usfq.logistpulse.command.TransferirStock;
import ec.edu.usfq.logistpulse.consola.Reportes;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.inventario.Reserva;
import ec.edu.usfq.logistpulse.strategy.picking.PickingFefo;
import ec.edu.usfq.logistpulse.strategy.picking.PickingFifo;
import ec.edu.usfq.logistpulse.strategy.picking.PickingLifo;
import ec.edu.usfq.logistpulse.strategy.picking.PoliticaPicking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Menú interactivo por consola para operar el inventario a mano.
 *
 * <p>Todo pasa por la misma {@code CentralDeComandos} que usa la demo: permisos,
 * cadena, estrategias, estados, eventos y ledger aplican igual. Las opciones están en
 * un mapa (no en un {@code switch}), así agregar una opción no toca las demás.</p>
 *
 * <pre>  java -cp out ec.edu.usfq.logistpulse.MenuInteractivo</pre>
 */
public class MenuInteractivo {

    private record Opcion(String titulo, Runnable accion) {
    }

    /** Se lanza cuando se acaba la entrada (Ctrl+D / Ctrl+Z o fin del archivo). */
    private static final class FinDeEntrada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private final SistemaLogistPulse s;
    private final BufferedReader entrada;
    private final PrintStream salida;
    private final Map<String, Opcion> opciones = new LinkedHashMap<>();
    private Actor usuario;
    private int claves;

    public MenuInteractivo(SistemaLogistPulse sistema, BufferedReader entrada, PrintStream salida) {
        this.s = sistema;
        this.entrada = entrada;
        this.salida = salida;
        opciones.put("1", new Opcion("Ver inventario", this::verInventario));
        opciones.put("2", new Opcion("Recibir lote", this::recibir));
        opciones.put("3", new Opcion("Almacenar lote", this::almacenar));
        opciones.put("4", new Opcion("Reservar pedido", this::reservar));
        opciones.put("5", new Opcion("Liberar reserva", this::liberar));
        opciones.put("6", new Opcion("Despachar pedido", this::despachar));
        opciones.put("7", new Opcion("Ajustar stock", this::ajustar));
        opciones.put("8", new Opcion("Transferir entre bodegas", this::transferir));
        opciones.put("9", new Opcion("Poner lote en cuarentena", this::cuarentena));
        opciones.put("10", new Opcion("Liberar lote de cuarentena", this::liberarCuarentena));
        opciones.put("11", new Opcion("Deshacer la ultima operacion", this::deshacer));
        opciones.put("12", new Opcion("Cambiar politica de picking de un SKU", this::cambiarPolitica));
        opciones.put("13", new Opcion("Ver ledger", this::verLedger));
        opciones.put("14", new Opcion("Ver ordenes de compra y tareas de robots", this::verReacciones));
        opciones.put("15", new Opcion("Cambiar de usuario", this::elegirUsuario));
    }

    public static void main(String[] args) {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        new MenuInteractivo(EscenarioDemo.sistema(), teclado, System.out).ejecutar();
    }

    public void ejecutar() {
        salida.println("=== LOGISTPULSE Smart Inventory - menu interactivo ===");
        salida.println("Fecha simulada: " + s.inventario().hoy() + ". Escribe 0 para salir.");
        try {
            elegirUsuario();
            while (true) {
                mostrarOpciones();
                String eleccion = leer("Opcion");
                if (eleccion.equals("0")) {
                    break;
                }
                Opcion opcion = opciones.get(eleccion);
                if (opcion == null) {
                    salida.println("  Opcion no valida: " + eleccion);
                    continue;
                }
                try {
                    opcion.accion().run();
                } catch (FinDeEntrada e) {
                    throw e;
                } catch (RuntimeException e) {
                    salida.println("  No se pudo: " + e.getMessage());
                }
            }
        } catch (FinDeEntrada e) {
            salida.println();
        }
        salida.println("Hasta luego.");
    }

    // ===================================================================== opciones

    private void elegirUsuario() {
        salida.println("Usuarios:");
        for (int i = 0; i < EscenarioDemo.USUARIOS.size(); i++) {
            salida.println("  " + (i + 1) + ") " + EscenarioDemo.USUARIOS.get(i));
        }
        int n = leerEntero("Usuario (numero)", 1, EscenarioDemo.USUARIOS.size());
        usuario = EscenarioDemo.USUARIOS.get(n - 1);
        salida.println("  Ahora operas como " + usuario);
    }

    private void verInventario() {
        salida.print(Reportes.inventario(s.inventario()));
    }

    private void recibir() {
        String clave = clave();
        String sku = leer("SKU");
        String bodega = leer("Bodega");
        String lote = leer("Codigo del lote nuevo");
        int unidades = leerEntero("Unidades", 1, Integer.MAX_VALUE);
        LocalDate vence = leerFecha("Vencimiento (AAAA-MM-DD)");
        enviar(new RecibirLote(clave, usuario, sku, bodega, lote, unidades, vence));
    }

    private void almacenar() {
        String clave = clave();
        enviar(new AlmacenarLote(clave, usuario, leer("SKU"), leer("Bodega"), leer("Lote")));
    }

    private void reservar() {
        String clave = clave();
        String pedido = leer("Pedido");
        String sku = leer("SKU");
        String bodega = leer("Bodega");
        int unidades = leerEntero("Unidades", 1, Integer.MAX_VALUE);
        ReservarPedido comando = new ReservarPedido(clave, usuario, pedido, sku, bodega, unidades);
        if (enviar(comando).exitoso()) {
            comando.reserva().ifPresent(r -> salida.println("  Lotes elegidos: " + r.describir()));
        }
    }

    private void liberar() {
        String clave = clave();
        buscarReserva().ifPresent(r -> enviar(new LiberarReserva(clave, usuario, r)));
    }

    private void despachar() {
        String clave = clave();
        buscarReserva().ifPresent(r -> enviar(new DespacharPedido(clave, usuario, r)));
    }

    private void ajustar() {
        String clave = clave();
        String sku = leer("SKU");
        String bodega = leer("Bodega");
        String lote = leer("Lote");
        int delta = leerEntero("Ajuste (+ suma, - resta)", Integer.MIN_VALUE, Integer.MAX_VALUE);
        enviar(new AjustarStock(clave, usuario, sku, bodega, lote, delta, leer("Razon")));
    }

    private void transferir() {
        String clave = clave();
        String sku = leer("SKU");
        String lote = leer("Lote");
        String origen = leer("Bodega origen");
        String destino = leer("Bodega destino");
        int unidades = leerEntero("Unidades", 1, Integer.MAX_VALUE);
        enviar(new TransferirStock(clave, usuario, sku, lote, origen, destino, unidades));
    }

    private void cuarentena() {
        String clave = clave();
        String sku = leer("SKU");
        String bodega = leer("Bodega");
        String lote = leer("Lote");
        enviar(new PonerEnCuarentena(clave, usuario, sku, bodega, lote, leer("Motivo")));
    }

    private void liberarCuarentena() {
        String clave = clave();
        enviar(new LiberarCuarentena(clave, usuario, leer("SKU"), leer("Bodega"), leer("Lote")));
    }

    private void deshacer() {
        if (s.central().proximoADeshacer().isEmpty()) {
            salida.println("  No hay operaciones para deshacer.");
            return;
        }
        mostrar(s.central().deshacerUltimo(usuario));
    }

    private void cambiarPolitica() {
        if (!usuario.esSupervisor()) {
            salida.println("  Solo un supervisor puede cambiar politicas.");
            return;
        }
        String sku = leer("SKU");
        if (s.inventario().sku(sku).isEmpty()) {
            salida.println("  El SKU " + sku + " no existe.");
            return;
        }
        Map<String, PoliticaPicking> politicas = Map.of("FEFO", new PickingFefo(), "FIFO", new PickingFifo(),
                "LIFO", new PickingLifo());
        PoliticaPicking elegida = politicas.get(leer("Politica (FEFO, FIFO o LIFO)").toUpperCase(Locale.ROOT));
        if (elegida == null) {
            salida.println("  Politica no valida.");
            return;
        }
        s.picking().usarParaSku(sku, elegida);
        salida.println("  " + sku + " ahora usa " + elegida.nombre());
    }

    private void verLedger() {
        salida.print(Reportes.ledger(s.ledger(), EscenarioDemo.ZONA));
    }

    private void verReacciones() {
        salida.println("Ordenes de compra:");
        s.reposicion().ordenes().forEach(o -> salida.println("  " + o.describir()));
        salida.println("Tareas de robots:");
        s.pulseFleet().tareas().forEach(t -> salida.println("  " + t.describir()));
    }

    // ===================================================================== ayudas

    private Resultado enviar(ComandoInventario comando) {
        Resultado r = s.central().despachar(comando);
        mostrar(r);
        return r;
    }

    private void mostrar(Resultado r) {
        salida.println("  => " + r.estado() + " " + r.comandoId() + (r.exitoso() ? "" : " | " + r.mensaje()));
        if (r.veredicto() != null) {
            salida.println("     cadena: " + r.veredicto().trazaLegible());
        }
    }

    private Optional<Reserva> buscarReserva() {
        String pedido = leer("Pedido");
        Optional<Reserva> reserva = s.inventario().reserva(pedido);
        if (reserva.isEmpty()) {
            salida.println("  No existe una reserva para el pedido " + pedido);
        }
        return reserva;
    }

    private String clave() {
        String escrita = leerOpcional("Clave de idempotencia (Enter = automatica)");
        return escrita.isBlank() ? String.format("MENU-%03d", ++claves) : escrita;
    }

    private void mostrarOpciones() {
        salida.println();
        salida.println("Usuario: " + usuario);
        opciones.forEach((tecla, opcion) -> salida.printf("  %2s) %s%n", tecla, opcion.titulo()));
        salida.println("   0) Salir");
    }

    private String leerOpcional(String pregunta) {
        salida.print(pregunta + ": ");
        salida.flush();
        try {
            String linea = entrada.readLine();
            if (linea == null) {
                throw new FinDeEntrada();
            }
            return linea.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String leer(String pregunta) {
        while (true) {
            String texto = leerOpcional(pregunta);
            if (!texto.isEmpty()) {
                return texto;
            }
            salida.println("  (no puede quedar vacio)");
        }
    }

    private int leerEntero(String pregunta, int minimo, int maximo) {
        while (true) {
            String texto = leer(pregunta);
            try {
                int n = Integer.parseInt(texto);
                if (n >= minimo && n <= maximo) {
                    return n;
                }
            } catch (NumberFormatException e) {
                // se vuelve a preguntar
            }
            salida.println("  (escribe un numero entero valido)");
        }
    }

    private LocalDate leerFecha(String pregunta) {
        while (true) {
            try {
                return LocalDate.parse(leer(pregunta));
            } catch (DateTimeParseException e) {
                salida.println("  (usa el formato AAAA-MM-DD)");
            }
        }
    }
}
