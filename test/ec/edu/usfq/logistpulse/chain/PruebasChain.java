package ec.edu.usfq.logistpulse.chain;

import ec.edu.usfq.logistpulse.EscenarioDemo;
import ec.edu.usfq.logistpulse.SistemaLogistPulse;
import ec.edu.usfq.logistpulse.command.AjustarStock;
import ec.edu.usfq.logistpulse.command.ComandoInventario;
import ec.edu.usfq.logistpulse.command.DespacharPedido;
import ec.edu.usfq.logistpulse.command.LiberarReserva;
import ec.edu.usfq.logistpulse.command.PonerEnCuarentena;
import ec.edu.usfq.logistpulse.command.RecibirLote;
import ec.edu.usfq.logistpulse.command.ReservarPedido;
import ec.edu.usfq.logistpulse.command.TransferirStock;
import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.dominio.TipoMovimiento;

import java.time.LocalDate;
import java.util.List;

import static ec.edu.usfq.logistpulse.EscenarioDemo.*;
import static ec.edu.usfq.logistpulse.MiniTest.*;

/** Pruebas del patrón Chain of Responsibility. */
public final class PruebasChain {

    private static final LocalDate VENCE = LocalDate.of(2027, 6, 30);

    private PruebasChain() {
    }

    private static Veredicto evaluar(SistemaLogistPulse s, ComandoInventario comando) {
        return s.cadena().evaluar(comando.solicitud(), s.inventario());
    }

    private static ReservarPedido reserva(Actor a, String sku, String bodega, int n) {
        return new ReservarPedido("K", a, "P-NUEVO", sku, bodega, n);
    }

    public static void correr() {
        seccion("CHAIN OF RESPONSIBILITY");

        prueba("Una reserva valida recorre los 5 eslabones en orden", () -> {
            Veredicto v = evaluar(EscenarioDemo.sistema(), reserva(ANDREA, ARROZ, NORTE, 10));
            verificar(v.aprobado(), "deberia aprobarse");
            iguales(List.of("Permisos:ok", "SKU:ok", "Stock:ok", "Lote:ok", "Capacidad:ok"), v.recorrido(), "recorrido");
        });

        prueba("Permisos: cada operacion declara que roles la pueden pedir", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            iguales("Permisos", evaluar(s, new AjustarStock("K", ANDREA, ARROZ, NORTE, L_ARZ_0901, 5, "x")).eslabon(),
                    "un operador no ajusta");
            iguales("Permisos", evaluar(s, new PonerEnCuarentena("K", AGENTE, ARROZ, NORTE, L_ARZ_0901, "x")).eslabon(),
                    "un agente no pone cuarentenas");
            iguales("Permisos", evaluar(s, reserva(LUCIA, ARROZ, NORTE, 5)).eslabon(), "calidad no reserva");
            verificar(evaluar(s, new AjustarStock("K", LUCIA, ARROZ, NORTE, L_ARZ_0901, 5, "x")).aprobado(),
                    "calidad si ajusta");
        });

        prueba("Permisos: solo el dueno o un supervisor gestiona una reserva", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            var reserva = s.inventario().reserva("P1").orElseThrow();
            Veredicto ajeno = evaluar(s, new LiberarReserva("K2", MARIO, reserva));
            iguales("Permisos", ajeno.eslabon(), "otro operador");
            verificar(ajeno.motivo().contains("andrea.vera"), ajeno.motivo());
            iguales("Permisos", evaluar(s, new DespacharPedido("K3", AGENTE, reserva)).eslabon(), "un agente");
            verificar(evaluar(s, new LiberarReserva("K4", ANDREA, reserva)).aprobado(), "la duena");
            verificar(evaluar(s, new DespacharPedido("K5", CARLOS, reserva)).aprobado(), "un supervisor");
            Actor otraAndrea = Actor.humano("andrea.vera", Rol.SUPERVISOR);
            verificar(evaluar(s, new LiberarReserva("K6", otraAndrea, reserva)).aprobado(), "misma persona con otro rol");
        });

        prueba("Un SKU inexistente se corta en el eslabon SKU", () -> {
            Veredicto v = evaluar(EscenarioDemo.sistema(), reserva(ANDREA, "NO-EXISTE", NORTE, 1));
            iguales("SKU", v.eslabon(), "eslabon");
            iguales(2, v.recorrido().size(), "no visita mas eslabones");
        });

        prueba("Descontinuado: no se reserva ni se recibe, pero su stock si se ajusta", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            iguales("SKU", evaluar(s, reserva(ANDREA, GALLETAS, SUR, 1)).eslabon(), "reservar");
            iguales("SKU", evaluar(s, new RecibirLote("K", ANDREA, GALLETAS, SUR, "L-N", 5, VENCE)).eslabon(), "recibir");
            verificar(evaluar(s, new AjustarStock("K", LUCIA, GALLETAS, SUR, L_GAL_0601, -5, "merma")).aprobado(),
                    "dar de baja stock existente si se permite");
        });

        prueba("Sin unidades libres suficientes se rechaza en Stock", () -> {
            Veredicto v = evaluar(EscenarioDemo.sistema(), reserva(ANDREA, ARROZ, NORTE, 141));
            iguales("Stock", v.eslabon(), "eslabon");
            verificar(v.motivo().contains("140"), "informa cuanto hay: " + v.motivo());
        });

        prueba("Hay stock pero esta en cuarentena: se rechaza en Lote", () -> {
            Veredicto v = evaluar(EscenarioDemo.sistema(), reserva(ANDREA, ACEITE, NORTE, 10));
            iguales(List.of("Permisos:ok", "SKU:ok", "Stock:ok", "Lote:RECHAZA"), v.recorrido(), "recorrido");
        });

        prueba("Lo vencido no cuenta como asignable", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            iguales(210, s.inventario().stockLibre(LECHE, NORTE), "libres (incluye vencido)");
            iguales(160, s.inventario().stockAsignable(LECHE, NORTE), "asignables (sin vencido)");
            iguales("Lote", evaluar(s, reserva(ANDREA, LECHE, NORTE, 161)).eslabon(), "eslabon");
        });

        prueba("Un pedido con reserva activa no se reserva dos veces", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            Veredicto v = evaluar(s, new ReservarPedido("K2", ANDREA, "P1", ARROZ, NORTE, 5));
            iguales("Lote", v.eslabon(), "eslabon");
            verificar(v.motivo().contains("reserva activa"), v.motivo());
        });

        prueba("Un lote vencido no se transfiere", () -> {
            Veredicto v = evaluar(EscenarioDemo.sistema(), new TransferirStock("K", ANDREA, LECHE, L_LEC_1001, NORTE, SUR, 10));
            iguales("Lote", v.eslabon(), "eslabon");
            verificar(v.motivo().contains("vencio"), v.motivo());
        });

        prueba("No se recibe un lote con un codigo que ya existe", () -> {
            iguales("Lote", evaluar(EscenarioDemo.sistema(),
                    new RecibirLote("K", ANDREA, ARROZ, NORTE, L_ARZ_0901, 5, VENCE)).eslabon(), "eslabon");
        });

        prueba("Exceder la zona de alistamiento se rechaza en Capacidad", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", LECHE, NORTE, 100));
            iguales("Capacidad", evaluar(s, reserva(ANDREA, ARROZ, NORTE, 100)).eslabon(), "100 + 100 > 190");
        });

        prueba("Recepcion, ajuste o transferencia que no caben se rechazan en Capacidad", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            iguales("Capacidad", evaluar(s, new RecibirLote("K", ANDREA, LECHE, NORTE, "L-N", 300, VENCE)).eslabon(),
                    "recepcion (530 + 300 > 800)");
            iguales("Capacidad", evaluar(s, new AjustarStock("K", CARLOS, CEMENTO, NORTE, L_CEM_0920, 500, "x")).eslabon(),
                    "ajuste +500");
            s.central().despachar(new RecibirLote("K1", ANDREA, LECHE, NORTE, "L-LEC-X", 260, VENCE));
            iguales("Capacidad", evaluar(s, new TransferirStock("K", ANDREA, ACEITE, L_ACE_1010, SUR, NORTE, 50)).eslabon(),
                    "transferencia (790 + 50 > 800)");
        });

        prueba("LIBERAR se reenvia sin revisar Stock ni Capacidad", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            s.central().despachar(new ReservarPedido("K1", ANDREA, "P1", ARROZ, NORTE, 10));
            Veredicto v = evaluar(s, new LiberarReserva("K2", ANDREA, s.inventario().reserva("P1").orElseThrow()));
            iguales(List.of("Permisos:ok", "SKU:ok", "Stock:no aplica", "Lote:ok", "Capacidad:no aplica"),
                    v.recorrido(), "recorrido");
        });

        prueba("Se agrega un eslabon nuevo sin tocar al cliente (LimiteIA)", () -> {
            SistemaLogistPulse s = EscenarioDemo.sistema();
            EslabonValidacion conLimite = CadenaValidacion.armar(new FiltroLimiteAgente(50), new FiltroAutorizacion(),
                    new FiltroSku(), new FiltroStock(), new FiltroLote(), new FiltroCapacidad());
            iguales("LimiteIA -> Permisos -> SKU -> Stock -> Lote -> Capacidad",
                    CadenaValidacion.describir(conLimite), "cadena");
            iguales("LimiteIA", conLimite.evaluar(reserva(AGENTE, ARROZ, NORTE, 60).solicitud(), s.inventario()).eslabon(),
                    "agente");
            verificar(conLimite.evaluar(reserva(ANDREA, ARROZ, NORTE, 60).solicitud(), s.inventario()).aprobado(),
                    "humano");
        });

        prueba("Una solicitud mal formada ni siquiera se puede crear", () -> {
            lanza(IllegalArgumentException.class, () -> reserva(ANDREA, ARROZ, NORTE, 0), "cantidad 0");
            lanza(IllegalArgumentException.class, () -> new TransferirStock("K", ANDREA, ARROZ, L_ARZ_0901,
                    NORTE, NORTE, 5), "origen = destino");
            lanza(IllegalArgumentException.class, () -> new AjustarStock("K", CARLOS, ARROZ, NORTE, L_ARZ_0901, 0, "x"),
                    "ajuste 0");
            lanza(IllegalArgumentException.class, () -> SolicitudMovimiento.para(TipoMovimiento.RESERVAR, ANDREA,
                    ARROZ, NORTE).descripcion("x").construir(), "sin roles autorizados");
        });

        prueba("Ningun eslabon depende del tipo de operacion (O de SOLID)", () -> {
            // Una operacion inventada, armada solo con requisitos, recorre la cadena sin cambiarla.
            SolicitudMovimiento inventada = SolicitudMovimiento.para(TipoMovimiento.AJUSTAR, LUCIA, ARROZ, NORTE)
                    .descripcion("INVENTARIO CICLICO de prueba").autorizados(Rol.CALIDAD)
                    .loteExistente(L_ARZ_0901, 0, true).construir();
            Veredicto v = EscenarioDemo.sistema().cadena().evaluar(inventada, EscenarioDemo.sistema().inventario());
            iguales(List.of("Permisos:ok", "SKU:ok", "Stock:no aplica", "Lote:ok", "Capacidad:no aplica"),
                    v.recorrido(), "recorrido");
        });
    }
}
