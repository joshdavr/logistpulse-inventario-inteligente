package ec.edu.usfq.logistpulse;

import ec.edu.usfq.logistpulse.chain.EslabonValidacion;
import ec.edu.usfq.logistpulse.command.AjustarStock;
import ec.edu.usfq.logistpulse.command.AlmacenarLote;
import ec.edu.usfq.logistpulse.command.CentralDeComandos;
import ec.edu.usfq.logistpulse.command.ComandoInventario;
import ec.edu.usfq.logistpulse.command.DecisionRegistrada;
import ec.edu.usfq.logistpulse.command.DespacharPedido;
import ec.edu.usfq.logistpulse.command.EstadoDecision;
import ec.edu.usfq.logistpulse.command.LiberarCuarentena;
import ec.edu.usfq.logistpulse.command.LiberarReserva;
import ec.edu.usfq.logistpulse.command.PonerEnCuarentena;
import ec.edu.usfq.logistpulse.command.RecibirLote;
import ec.edu.usfq.logistpulse.command.ReservarPedido;
import ec.edu.usfq.logistpulse.command.Resultado;
import ec.edu.usfq.logistpulse.command.TransferirStock;
import ec.edu.usfq.logistpulse.dominio.Bodega;
import ec.edu.usfq.logistpulse.dominio.Categoria;
import ec.edu.usfq.logistpulse.dominio.PerfilDemanda;
import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.inventario.ConsultasInventario;
import ec.edu.usfq.logistpulse.inventario.Inventario;
import ec.edu.usfq.logistpulse.inventario.OperacionesInventario;
import ec.edu.usfq.logistpulse.observer.StockCambiado;
import ec.edu.usfq.logistpulse.state.Lote;
import ec.edu.usfq.logistpulse.state.VistaLote;
import ec.edu.usfq.logistpulse.strategy.reposicion.ServicioReposicion;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ec.edu.usfq.logistpulse.EscenarioDemo.*;
import static ec.edu.usfq.logistpulse.MiniTest.*;

/**
 * Una prueba por cada hallazgo de la revisión de los repositorios.
 * Si alguno de estos problemas vuelve a aparecer, la prueba falla.
 */
public final class PruebasRevision {

    private static final LocalDate VENCE = LocalDate.of(2027, 6, 30);

    private PruebasRevision() {
    }

    private static VistaLote lote(SistemaLogistPulse s, String codigo, String bodega) {
        return s.inventario().lote(codigo, bodega).orElseThrow();
    }

    private static boolean esAbstraccion(Class<?> tipo) {
        return tipo.isInterface() || Modifier.isAbstract(tipo.getModifiers());
    }

    public static void correr() {
        seccion("HALLAZGOS DE LA REVISION");

        prueba("Propiedad: otro operador no libera una reserva ajena (ni por la cadena ni directo)", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            Resultado r = s.central().despachar(new LiberarReserva("K2", MARIO, s.inventario().reserva("P1").orElseThrow()));
            iguales(EstadoDecision.RECHAZADO, r.estado(), "por la central");
            lanza(IllegalStateException.class, () -> s.inventario().liberarReserva("P1", MARIO), "directo al receptor");
            lanza(IllegalStateException.class, () -> s.inventario().despachar("P1", AGENTE), "un agente tampoco despacha");
            iguales(10, lote(s, L_ARZ_0901, NORTE).reservado(), "la reserva sigue intacta");
            Resultado supervisor = s.central().despachar(
                    new LiberarReserva("K3", CARLOS, s.inventario().reserva("P1").orElseThrow()));
            iguales(EstadoDecision.EJECUTADO, supervisor.estado(), "un supervisor si puede");
        });

        prueba("Idempotencia por huella: misma clave con otros datos se rechaza y se audita", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            Resultado r = s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 99));
            iguales(EstadoDecision.RECHAZADO, r.estado(), "no es un falso OK");
            DecisionRegistrada auditada = s.ledger().decisiones().get(1);
            verificar(auditada.detalle().contains("otra operacion"), auditada.detalle());
            iguales(10, lote(s, L_ARZ_0901, NORTE).reservado(), "solo se aplico la primera");
        });

        prueba("Si deshacer falla, el comando no se pierde y el intento queda en el ledger", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado traslado = s.central().despachar(new TransferirStock("K1", ANDREA, ACEITE, L_ACE_1010, SUR, NORTE, 40));
            s.central().despachar(new ReservarPedido("K2", ANDREA, "P2", ACEITE, NORTE, 30)); // reserva parte del destino
            int antes = s.central().operacionesDeshacibles();
            Resultado fallo = s.central().compensar(traslado.comandoId(), CARLOS);
            iguales(EstadoDecision.RECHAZADO, fallo.estado(), "la compensacion no procede");
            iguales(antes, s.central().operacionesDeshacibles(), "el historial no perdio nada");
            iguales(160, lote(s, L_ACE_1010, SUR).fisico(), "origen sin cambios");
            iguales(40, lote(s, L_ACE_1010, NORTE).fisico(), "destino sin cambios");
            DecisionRegistrada ultima = s.ledger().decisiones().get(s.ledger().decisiones().size() - 1);
            iguales(traslado.comandoId(), ultima.compensaA(), "el intento fallido esta en el ledger");
            s.central().deshacerUltimo(CARLOS);   // primero la reserva (LIFO)
            Resultado ok = s.central().deshacerUltimo(CARLOS); // ahora si el traslado
            verificar(ok.exitoso(), ok.toString());
            iguales(200, lote(s, L_ACE_1010, SUR).fisico(), "traslado revertido");
        });

        prueba("deshacerUltimo que falla deja el comando en el tope del historial", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            Resultado despacho = s.central().despachar(
                    new DespacharPedido("K2", ANDREA, s.inventario().reserva("P1").orElseThrow()));
            Resultado intento = s.central().deshacerUltimo(CARLOS); // un despacho es irreversible
            iguales(EstadoDecision.RECHAZADO, intento.estado(), "no se puede deshacer");
            iguales(2, s.central().operacionesDeshacibles(), "no salio del historial");
            iguales(despacho.comandoId(), s.central().proximoADeshacer().orElseThrow(), "sigue en el tope");
            iguales(EstadoDecision.RECHAZADO, s.central().deshacerUltimo(CARLOS).estado(),
                    "el segundo intento apunta al mismo comando, no se salta al anterior");
            iguales(50, lote(s, L_ARZ_0901, NORTE).fisico(), "el despacho sigue hecho (60 - 10)");
        });

        prueba("Defensa en profundidad: el receptor valida por su cuenta, sin la cadena", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            lanza(IllegalStateException.class, () -> s.inventario().transferir(L_LEC_1001, NORTE, SUR, 10),
                    "transferir un lote vencido");
            lanza(IllegalStateException.class, () -> s.inventario().recibirLote("L-N", GALLETAS, SUR, 5, VENCE),
                    "recibir un SKU descontinuado");
            lanza(IllegalStateException.class, () -> s.inventario().reservar("P1", GALLETAS, SUR, 5, ANDREA),
                    "reservar un SKU descontinuado");
            s.inventario().reservar("P2", ARROZ, NORTE, 10, ANDREA);
            lanza(IllegalStateException.class, () -> s.inventario().reservar("P2", ARROZ, NORTE, 10, ANDREA),
                    "reservar dos veces el mismo pedido");
            s.inventario().reservar("P3", LECHE, NORTE, 100, ANDREA); // alistamiento: 110/190
            lanza(IllegalStateException.class, () -> s.inventario().reservar("P4", ARROZ, NORTE, 100, ANDREA),
                    "reservar mas de lo que cabe en alistamiento (110 + 100 > 190)");
            iguales(110, s.inventario().reservadoEnBodega(NORTE), "el alistamiento no se paso");
            lanza(IllegalStateException.class, () -> s.inventario().recibirLote(L_ARZ_0901, ARROZ, NORTE, 5, VENCE),
                    "recibir un codigo de lote que ya existe");
            iguales(240, s.inventario().fisicoEnBodega(SUR), "nada cambio en el sur");
        });

        prueba("Todas las operaciones pasan por la cadena, no solo Reserve", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            s.central().despachar(new ReservarPedido("K2", ANDREA, "P2", ARROZ, NORTE, 5));
            List<ComandoInventario> comandos = List.of(
                    new LiberarReserva("K3", ANDREA, s.inventario().reserva("P1").orElseThrow()),
                    new DespacharPedido("K4", ANDREA, s.inventario().reserva("P2").orElseThrow()),
                    new AjustarStock("K5", CARLOS, CEMENTO, NORTE, L_CEM_0920, 5, "conteo"),
                    new TransferirStock("K6", ANDREA, ACEITE, L_ACE_1010, SUR, NORTE, 10),
                    new RecibirLote("K7", ANDREA, LECHE, NORTE, "L-LEC-X", 10, VENCE),
                    new AlmacenarLote("K8", ANDREA, LECHE, NORTE, "L-LEC-X"),
                    new PonerEnCuarentena("K9", LUCIA, CEMENTO, NORTE, L_CEM_0920, "x"),
                    new LiberarCuarentena("K10", LUCIA, CEMENTO, NORTE, L_CEM_0920));
            for (ComandoInventario c : comandos) {
                Resultado r = s.central().despachar(c);
                verificar(r.exitoso(), c.describir() + " -> " + r);
                verificar(r.veredicto().trazaLegible().startsWith("Permisos:ok -> SKU:ok"),
                        c.solicitud().tipo() + " no paso por la cadena");
            }
        });

        prueba("Transferir desde un lote en cuarentena se rechaza (central y receptor directo)", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado r = s.central().despachar(new TransferirStock("K1", ANDREA, ACEITE, L_ACE_0820, NORTE, SUR, 10));
            iguales(EstadoDecision.RECHAZADO, r.estado(), "por la central");
            lanza(IllegalStateException.class, () -> s.inventario().transferir(L_ACE_0820, NORTE, SUR, 10), "directo");
            iguales(80, lote(s, L_ACE_0820, NORTE).fisico(), "el lote sigue completo");
        });

        prueba("Ajuste +500 sobre la capacidad se rechaza en la cadena y en el receptor", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            Resultado r = s.central().despachar(new AjustarStock("K1", CARLOS, CEMENTO, NORTE, L_CEM_0920, 500, "x"));
            iguales(EstadoDecision.RECHAZADO, r.estado(), "por la central");
            lanza(IllegalStateException.class, () -> s.inventario().ajustar(L_CEM_0920, NORTE, 500), "directo");
            lanza(IllegalStateException.class, () -> s.inventario().recibirLote("L-N", LECHE, NORTE, 300, VENCE),
                    "recepcion directa que no cabe");
            iguales(530, s.inventario().fisicoEnBodega(NORTE), "la bodega no supero su capacidad");
        });

        prueba("Un pedido de 70 con dos lotes de 50 se reparte entre los lotes", () -> {
            SistemaLogistPulse s = SistemaLogistPulse.crear(EscenarioDemo.reloj());
            Inventario inv = s.inventario();
            inv.registrarSku(new ProductoSku("SKU-1", "Producto", Categoria.ABARROTES, true, new PerfilDemanda(1, 1, 0)));
            inv.registrarBodega(new Bodega("B1", 500, 200));
            inv.registrarSaldoInicial(Lote.almacenado("L1", "SKU-1", "B1", 50, HOY.minusDays(9), VENCE));
            inv.registrarSaldoInicial(Lote.almacenado("L2", "SKU-1", "B1", 50, HOY.minusDays(3), VENCE));
            Resultado r = s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", "SKU-1", "B1", 70));
            verificar(r.exitoso(), r.toString());
            iguales("FIFO: L1 x50, L2 x20", inv.reserva("P1").orElseThrow().describir(), "reparto");
        });

        prueba("Desbordamiento de enteros: MAX_VALUE y MIN_VALUE se rechazan", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            lanza(IllegalArgumentException.class, () -> new RecibirLote("K", ANDREA, LECHE, SUR, "L-X",
                    Integer.MAX_VALUE, VENCE), "recepcion MAX_VALUE");
            lanza(IllegalArgumentException.class, () -> new ReservarPedido("K", ANDREA, "P", ARROZ, NORTE,
                    Integer.MAX_VALUE), "reserva MAX_VALUE");
            lanza(IllegalArgumentException.class, () -> new AjustarStock("K", CARLOS, ARROZ, NORTE, L_ARZ_0901,
                    Integer.MIN_VALUE, "x"), "ajuste MIN_VALUE");
            lanza(IllegalArgumentException.class, () -> s.inventario().recibirLote("L-X", LECHE, SUR,
                    Integer.MAX_VALUE, VENCE), "receptor directo MAX_VALUE");
            lanza(IllegalArgumentException.class, () -> s.inventario().ajustar(L_ARZ_0901, NORTE, Integer.MAX_VALUE),
                    "ajuste directo MAX_VALUE");
            iguales(240, s.inventario().fisicoEnBodega(SUR), "nada cambio");
        });

        prueba("Ciclo completo del stock: reservar, liberar y despachar", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 30));
            iguales(110, s.inventario().stockLibre(ARROZ, NORTE), "reservar baja lo libre");
            iguales(140, lote(s, L_ARZ_0901, NORTE).fisico() + lote(s, L_ARZ_1015, NORTE).fisico(), "el fisico no cambia");
            s.central().despachar(new LiberarReserva("K2", ANDREA, s.inventario().reserva("P1").orElseThrow()));
            iguales(140, s.inventario().stockLibre(ARROZ, NORTE), "liberar lo sube");
            s.central().despachar(new ReservarPedido("K3", ANDREA, "P2", ARROZ, NORTE, 30));
            s.central().despachar(new DespacharPedido("K4", ANDREA, s.inventario().reserva("P2").orElseThrow()));
            iguales(110, lote(s, L_ARZ_0901, NORTE).fisico() + lote(s, L_ARZ_1015, NORTE).fisico(),
                    "despachar saca el stock fisico");
        });

        prueba("Existen recepciones, mermas, conteos y traslados entre bodegas", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            List<ComandoInventario> operaciones = List.of(
                    new RecibirLote("K1", ANDREA, ARROZ, NORTE, "L-ARZ-NUEVO", 20, VENCE),
                    new AjustarStock("K2", LUCIA, LECHE, NORTE, L_LEC_1001, -50, "merma por vencimiento"),
                    new AjustarStock("K3", CARLOS, CEMENTO, NORTE, L_CEM_0920, 2, "conteo ciclico"),
                    new TransferirStock("K4", ANDREA, ACEITE, L_ACE_1010, SUR, NORTE, 25));
            for (ComandoInventario c : operaciones) {
                verificar(s.central().despachar(c).exitoso(), c.describir());
            }
        });

        prueba("Ledger tipado: records inmutables con Instant", () -> {
            for (Class<?> evento : List.of(DecisionRegistrada.class, StockCambiado.class)) {
                verificar(evento.isRecord(), evento.getSimpleName() + " deberia ser record");
                RecordComponent momento = Arrays.stream(evento.getRecordComponents())
                        .filter(c -> c.getName().equals("momento")).findFirst().orElseThrow();
                iguales(Instant.class, momento.getType(), evento.getSimpleName() + ".momento");
            }
        });

        prueba("Dominio encapsulado: hacia afuera los lotes son de solo lectura", () -> {
            Set<String> metodos = Arrays.stream(VistaLote.class.getMethods()).map(Method::getName).collect(Collectors.toSet());
            for (String mutador : List.of("reservar", "liberar", "despachar", "ajustar", "retirar", "ingresar", "almacenar")) {
                verificar(!metodos.contains(mutador), "VistaLote no deberia exponer " + mutador);
            }
            String devuelve = ConsultasInventario.class.getMethod("lote", String.class, String.class)
                    .getGenericReturnType().getTypeName();
            verificar(devuelve.contains("VistaLote"), "las consultas deberian devolver VistaLote, no " + devuelve);
        });

        prueba("D de SOLID: comandos, eslabones y servicios dependen de abstracciones", () -> {
            verificar(esAbstraccion(ComandoInventario.class.getMethod("ejecutar", OperacionesInventario.class)
                    .getParameterTypes()[0]), "ejecutar(OperacionesInventario)");
            Method revisar = EslabonValidacion.class.getDeclaredMethod("revisar",
                    ec.edu.usfq.logistpulse.chain.SolicitudMovimiento.class, ConsultasInventario.class);
            verificar(esAbstraccion(revisar.getParameterTypes()[1]), "revisar(..., ConsultasInventario)");
            for (Class<?> clase : List.of(Inventario.class, CentralDeComandos.class, ServicioReposicion.class)) {
                for (Constructor<?> c : clase.getConstructors()) {
                    for (Class<?> p : c.getParameterTypes()) {
                        verificar(esAbstraccion(p), clase.getSimpleName() + " recibe " + p.getSimpleName() + " concreto");
                    }
                }
            }
        });

        prueba("I de SOLID: lectura y escritura del inventario son interfaces separadas", () -> {
            Set<String> lectura = Arrays.stream(ConsultasInventario.class.getMethods()).map(Method::getName)
                    .collect(Collectors.toSet());
            Set<String> escritura = Arrays.stream(OperacionesInventario.class.getMethods()).map(Method::getName)
                    .collect(Collectors.toSet());
            verificar(lectura.stream().noneMatch(escritura::contains), "no comparten metodos");
        });

        prueba("Reloj inyectado: la fecha decide los vencimientos, no el reloj del sistema", () -> {
            SistemaLogistPulse hoy = EscenarioDemo.sistema();
            RelojManual mesDespues = new RelojManual(ZonedDateTime.of(HOY.plusMonths(1).atTime(8, 0), ZONA).toInstant(), ZONA);
            SistemaLogistPulse despues = SistemaLogistPulse.crear(mesDespues);
            EscenarioDemo.cargarDatos(despues.inventario());
            iguales(160, hoy.inventario().stockAsignable(LECHE, NORTE), "leche asignable hoy");
            iguales(0, despues.inventario().stockAsignable(LECHE, NORTE), "un mes despues toda la leche vencio");
        });
    }
}
