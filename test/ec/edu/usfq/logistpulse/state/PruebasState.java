package ec.edu.usfq.logistpulse.state;

import ec.edu.usfq.logistpulse.EscenarioDemo;
import ec.edu.usfq.logistpulse.SistemaLogistPulse;

import java.time.LocalDate;
import java.util.List;

import static ec.edu.usfq.logistpulse.EscenarioDemo.*;
import static ec.edu.usfq.logistpulse.MiniTest.*;

/** Pruebas del patrón State (ciclo de vida del lote). */
public final class PruebasState {

    private static final LocalDate VENCE = HOY.plusDays(60);

    private PruebasState() {
    }

    private static Lote almacenado(int unidades) {
        return Lote.almacenado("L-T", ARROZ, NORTE, unidades, HOY, VENCE);
    }

    public static void correr() {
        seccion("STATE");

        prueba("Ciclo completo: RECIBIDO -> ALMACENADO -> RESERVADO -> ENVIADO", () -> {
            Lote l = Lote.recibido("L-T", ARROZ, NORTE, 10, HOY, VENCE);
            iguales("RECIBIDO", l.nombreEstado(), "estado inicial");
            l.almacenar();
            l.reservar(10);
            l.despachar(10);
            iguales("ENVIADO", l.nombreEstado(), "estado final");
            iguales(List.of("RECIBIDO -> ALMACENADO", "ALMACENADO -> RESERVADO", "RESERVADO -> ENVIADO"),
                    l.transiciones(), "historial");
        });

        prueba("Un lote RECIBIDO no se puede reservar ni es asignable", () -> {
            Lote l = Lote.recibido("L-T", ARROZ, NORTE, 10, HOY, VENCE);
            verificar(!l.asignableEn(HOY), "no deberia ser asignable");
            lanza(TransicionInvalidaException.class, () -> l.reservar(1), "reservar");
            lanza(TransicionInvalidaException.class, l::ponerEnCuarentena, "cuarentena desde RECIBIDO");
        });

        prueba("Reserva parcial deja ALMACENADO; completar la deja RESERVADO; liberar vuelve", () -> {
            Lote l = almacenado(10);
            l.reservar(4);
            iguales("ALMACENADO", l.nombreEstado(), "con libres");
            l.reservar(6);
            iguales("RESERVADO", l.nombreEstado(), "sin libres");
            l.liberar(3);
            iguales("ALMACENADO", l.nombreEstado(), "vuelve a tener libres");
            iguales(7, l.reservado(), "reservado");
        });

        prueba("RESERVADO no admite mas reservas ni transferir hacia afuera", () -> {
            Lote l = almacenado(10);
            l.reservar(10);
            lanza(TransicionInvalidaException.class, () -> l.reservar(1), "reservar");
            lanza(TransicionInvalidaException.class, () -> l.retirar(1), "transferir");
        });

        prueba("Despacho parcial deja RESERVADO; el ultimo lo deja ENVIADO", () -> {
            Lote l = almacenado(10);
            l.reservar(10);
            l.despachar(4);
            iguales("RESERVADO", l.nombreEstado(), "despues del primer despacho");
            iguales(6, l.fisico(), "fisico");
            l.despachar(6);
            iguales("ENVIADO", l.nombreEstado(), "despues del ultimo");
        });

        prueba("Cuarentena solo desde ALMACENADO y sin reservas", () -> {
            Lote reservado = almacenado(10);
            reservado.reservar(10);
            lanza(TransicionInvalidaException.class, reservado::ponerEnCuarentena, "desde RESERVADO");
            Lote conReservas = almacenado(10);
            conReservas.reservar(2);
            lanza(TransicionInvalidaException.class, conReservas::ponerEnCuarentena, "con reservas activas");
            Lote libre = almacenado(10);
            libre.ponerEnCuarentena();
            iguales("CUARENTENA", libre.nombreEstado(), "desde ALMACENADO libre");
            libre.liberarCuarentena();
            iguales("ALMACENADO", libre.nombreEstado(), "al salir de cuarentena");
        });

        prueba("En CUARENTENA solo se ajusta o se libera", () -> {
            Lote l = almacenado(10);
            l.ponerEnCuarentena();
            verificar(!l.asignableEn(HOY), "no es asignable");
            lanza(TransicionInvalidaException.class, () -> l.reservar(1), "reservar");
            lanza(TransicionInvalidaException.class, () -> l.retirar(1), "transferir");
            lanza(TransicionInvalidaException.class, l::almacenar, "almacenar");
            l.ajustar(-5);
            iguales(5, l.fisico(), "dar de baja si se permite");
        });

        prueba("ENVIADO es final: toda operacion lanza TransicionInvalida", () -> {
            Lote l = almacenado(5);
            l.reservar(5);
            l.despachar(5);
            List<Runnable> operaciones = List.of(l::almacenar, () -> l.reservar(1), () -> l.liberar(1),
                    () -> l.despachar(1), l::ponerEnCuarentena, l::liberarCuarentena, () -> l.ajustar(1),
                    () -> l.retirar(1), () -> l.ingresar(1));
            for (Runnable op : operaciones) {
                lanza(TransicionInvalidaException.class, op, "operacion sobre ENVIADO");
            }
        });

        prueba("Contrato: una operacion rechazada no modifica el lote", () -> {
            Lote l = almacenado(10);
            l.reservar(8);
            IllegalStateException e = lanza(IllegalStateException.class, () -> l.ajustar(-3), "fisico < reservado");
            verificar(!(e instanceof TransicionInvalidaException), "es una invariante, no una transicion");
            lanza(IllegalStateException.class, () -> l.liberar(9), "liberar mas de lo reservado");
            lanza(IllegalStateException.class, () -> l.despachar(9), "despachar mas de lo reservado");
            iguales(10, l.fisico(), "fisico intacto");
            iguales(8, l.reservado(), "reservado intacto");
            iguales("ALMACENADO", l.nombreEstado(), "estado intacto");
        });

        prueba("Sin desbordes: cantidades enormes se rechazan antes de sumar", () -> {
            Lote l = almacenado(10);
            lanza(IllegalArgumentException.class, () -> l.ajustar(Integer.MAX_VALUE), "ajuste MAX_VALUE");
            lanza(IllegalArgumentException.class, () -> l.ajustar(Integer.MIN_VALUE), "ajuste MIN_VALUE");
            lanza(IllegalArgumentException.class, () -> l.reservar(Integer.MAX_VALUE), "reserva MAX_VALUE");
            lanza(IllegalArgumentException.class, () -> Lote.almacenado("L-X", ARROZ, NORTE, -1, HOY, VENCE), "negativo");
            iguales(10, l.fisico(), "nada cambio");
        });

        prueba("Transferir hacia un lote en CUARENTENA falla sin tocar el origen", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.inventario().transferir(L_ACE_1010, SUR, NORTE, 10);
            s.inventario().ponerEnCuarentena(L_ACE_1010, NORTE);
            lanza(TransicionInvalidaException.class,
                    () -> s.inventario().transferir(L_ACE_1010, SUR, NORTE, 5), "destino en cuarentena");
            iguales(190, s.inventario().lote(L_ACE_1010, SUR).orElseThrow().fisico(), "origen intacto");
        });

        prueba("El lote transferido conserva ingreso y vencimiento (FIFO sigue siendo justo)", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.inventario().transferir(L_ACE_1010, SUR, NORTE, 10);
            VistaLote origen = s.inventario().lote(L_ACE_1010, SUR).orElseThrow();
            VistaLote destino = s.inventario().lote(L_ACE_1010, NORTE).orElseThrow();
            iguales(origen.fechaIngreso(), destino.fechaIngreso(), "fecha de ingreso");
            iguales(origen.vencimiento(), destino.vencimiento(), "vencimiento");
            iguales("ALMACENADO", destino.nombreEstado(), "estado en destino");
        });
    }
}
