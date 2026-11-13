package ec.edu.usfq.logistpulse;

import ec.edu.usfq.logistpulse.dominio.Actor;
import ec.edu.usfq.logistpulse.dominio.Bodega;
import ec.edu.usfq.logistpulse.dominio.Categoria;
import ec.edu.usfq.logistpulse.dominio.PerfilDemanda;
import ec.edu.usfq.logistpulse.dominio.ProductoSku;
import ec.edu.usfq.logistpulse.dominio.Rol;
import ec.edu.usfq.logistpulse.inventario.Inventario;
import ec.edu.usfq.logistpulse.state.Lote;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Datos de ejemplo compartidos por la demo, el menú y las pruebas.
 *
 * <p>El reloj arranca el 12 de noviembre de 2026 a las 08:00 en Quito, para que
 * los vencimientos den siempre el mismo resultado sin importar cuándo se ejecute.</p>
 */
public final class EscenarioDemo {

    public static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    public static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    public static final String NORTE = "UIO-NORTE";
    public static final String SUR = "UIO-SUR";

    public static final String ARROZ = "ARZ-001";
    public static final String ACEITE = "ACE-010";
    public static final String LECHE = "LEC-200";
    public static final String CEMENTO = "CEM-050";
    public static final String GALLETAS = "GAL-999";

    // Lotes en UIO-NORTE
    public static final String L_ARZ_0901 = "L-ARZ-0901";   // entro antes, vence despues
    public static final String L_ARZ_1015 = "L-ARZ-1015";   // entro despues, vence antes
    public static final String L_LEC_1001 = "L-LEC-1001";   // vencido
    public static final String L_LEC_1103 = "L-LEC-1103";
    public static final String L_LEC_1108 = "L-LEC-1108";   // entro despues, vence antes
    public static final String L_ACE_0820 = "L-ACE-0820";   // en cuarentena
    public static final String L_CEM_0920 = "L-CEM-0920";
    public static final String L_CEM_1105 = "L-CEM-1105";   // el mas nuevo
    // Lotes en UIO-SUR
    public static final String L_ACE_1010 = "L-ACE-1010";
    public static final String L_GAL_0601 = "L-GAL-0601";

    // Usuarios
    public static final Actor ANDREA = Actor.humano("andrea.vera", Rol.OPERADOR);
    public static final Actor MARIO = Actor.humano("mario.paz", Rol.OPERADOR);
    public static final Actor CARLOS = Actor.humano("carlos.rios", Rol.SUPERVISOR);
    public static final Actor LUCIA = Actor.humano("lucia.mena", Rol.CALIDAD);
    public static final Actor AGENTE = Actor.agente("pulse-agent-07");
    public static final List<Actor> USUARIOS = List.of(ANDREA, MARIO, CARLOS, LUCIA, AGENTE);

    private EscenarioDemo() {
    }

    public static RelojManual reloj() {
        return new RelojManual(ZonedDateTime.of(HOY.atTime(8, 0), ZONA).toInstant(), ZONA);
    }

    /** Sistema completo con los datos de ejemplo cargados. */
    public static SistemaLogistPulse sistema() {
        SistemaLogistPulse s = SistemaLogistPulse.crear(reloj());
        cargarDatos(s.inventario());
        return s;
    }

    /**
     * Inventario inicial:
     * <pre>
     *  UIO-NORTE  capacidad 800, alistamiento 190  -> 530 unidades
     *  UIO-SUR    capacidad 400, alistamiento 100  -> 240 unidades
     * </pre>
     */
    public static void cargarDatos(Inventario inv) {
        inv.registrarSku(new ProductoSku(ARROZ, "Arroz flor 2 kg", Categoria.ABARROTES, true, new PerfilDemanda(12, 4, 30)));
        inv.registrarSku(new ProductoSku(ACEITE, "Aceite de girasol 1 L", Categoria.ABARROTES, true, new PerfilDemanda(6, 5, 10)));
        inv.registrarSku(new ProductoSku(LECHE, "Leche UHT 1 L", Categoria.PERECIBLE, true, new PerfilDemanda(20, 2, 15)));
        inv.registrarSku(new ProductoSku(CEMENTO, "Cemento 50 kg", Categoria.MATERIALES, true, new PerfilDemanda(8, 7, 30)));
        inv.registrarSku(new ProductoSku(GALLETAS, "Galletas surtidas", Categoria.ABARROTES, false, new PerfilDemanda(1, 1, 0)));

        inv.registrarBodega(new Bodega(NORTE, 800, 190));
        inv.registrarBodega(new Bodega(SUR, 400, 100));

        inv.registrarSaldoInicial(Lote.almacenado(L_ARZ_0901, ARROZ, NORTE, 60, fecha(2026, 9, 1), fecha(2027, 2, 28)));
        inv.registrarSaldoInicial(Lote.almacenado(L_ARZ_1015, ARROZ, NORTE, 80, fecha(2026, 10, 15), fecha(2026, 12, 20)));
        inv.registrarSaldoInicial(Lote.almacenado(L_LEC_1001, LECHE, NORTE, 50, fecha(2026, 10, 1), fecha(2026, 11, 5)));
        inv.registrarSaldoInicial(Lote.almacenado(L_LEC_1103, LECHE, NORTE, 90, fecha(2026, 11, 3), fecha(2026, 12, 3)));
        inv.registrarSaldoInicial(Lote.almacenado(L_LEC_1108, LECHE, NORTE, 70, fecha(2026, 11, 8), fecha(2026, 11, 25)));
        Lote aceiteRetenido = Lote.almacenado(L_ACE_0820, ACEITE, NORTE, 80, fecha(2026, 8, 20), fecha(2027, 8, 20));
        aceiteRetenido.ponerEnCuarentena();
        inv.registrarSaldoInicial(aceiteRetenido);
        inv.registrarSaldoInicial(Lote.almacenado(L_CEM_0920, CEMENTO, NORTE, 60, fecha(2026, 9, 20), fecha(2027, 3, 20)));
        inv.registrarSaldoInicial(Lote.almacenado(L_CEM_1105, CEMENTO, NORTE, 40, fecha(2026, 11, 5), fecha(2027, 5, 5)));

        inv.registrarSaldoInicial(Lote.almacenado(L_ACE_1010, ACEITE, SUR, 200, fecha(2026, 10, 10), fecha(2028, 1, 15)));
        inv.registrarSaldoInicial(Lote.almacenado(L_GAL_0601, GALLETAS, SUR, 40, fecha(2026, 6, 1), fecha(2027, 1, 1)));
    }

    private static LocalDate fecha(int anio, int mes, int dia) {
        return LocalDate.of(anio, mes, dia);
    }
}
