package ec.edu.usfq.logistpulse;

import ec.edu.usfq.logistpulse.chain.PruebasChain;
import ec.edu.usfq.logistpulse.command.PruebasCommand;
import ec.edu.usfq.logistpulse.observer.PruebasObserver;
import ec.edu.usfq.logistpulse.state.PruebasState;
import ec.edu.usfq.logistpulse.strategy.PruebasStrategy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static ec.edu.usfq.logistpulse.MiniTest.*;

/**
 * Corre todas las pruebas: una sección por patrón, una por cada hallazgo de la revisión
 * y las de integración (demo y menú). Termina con código 1 si alguna falla, así GitHub
 * Actions marca el commit en rojo.
 */
public final class PruebasLogistPulse {

    private PruebasLogistPulse() {
    }

    public static void main(String[] args) {
        PruebasCommand.correr();
        PruebasChain.correr();
        PruebasStrategy.correr();
        PruebasState.correr();
        PruebasObserver.correr();
        PruebasRevision.correr();

        seccion("INTEGRACION");
        prueba("La demo completa corre de principio a fin sin fallos", () -> {
            String salida = capturar(() -> App.main(new String[0]));
            verificar(salida.contains("[31]") && !salida.contains("[32]"), "deberia mostrar exactamente 31 pasos");
            verificar(salida.contains("fallidas 0"), "no deberia haber comandos fallidos");
            verificar(salida.contains("fallos de entrega: 0"), "ningun suscriptor deberia fallar");
            verificar(!salida.contains("ACEPTADO (esto seria un error)"), "el receptor dejo pasar algo indebido");
        });

        prueba("El menu interactivo atiende operaciones, permisos y entradas invalidas", () -> {
            String guion = String.join("\n",
                    "9", "x", "1",                                    // usuario fuera de rango, texto, Andrea
                    "4", "", "PED-9", "ARZ-001", "UIO-NORTE", "abc", "10", // reservar (con un numero invalido)
                    "15", "2",                                        // cambiar a Mario
                    "5", "", "PED-9",                                 // Mario intenta liberar
                    "99",                                             // opcion invalida
                    "13",                                             // ver ledger
                    "0") + "\n";
            String salida = capturarMenu(guion);
            verificar(salida.contains("Lotes elegidos: FIFO: L-ARZ-0901 x10"), "la reserva deberia ejecutarse");
            verificar(salida.contains("[Permisos] Solo el dueno de la reserva PED-9"), "Mario no deberia poder liberar");
            verificar(salida.contains("Opcion no valida: 99"), "deberia avisar la opcion invalida");
            verificar(salida.contains("Hasta luego."), "deberia terminar limpio");
        });

        prueba("El menu termina limpio si la entrada se corta a mitad de una operacion", () -> {
            String salida = capturarMenu("1\n2\n\nLEC-200\n");
            verificar(salida.contains("Hasta luego."), salida);
        });

        System.out.printf("%nResultado: %d pasan, %d fallan%n", pasan(), fallan());
        System.exit(fallan() == 0 ? 0 : 1);
    }

    private static String capturarMenu(String guion) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream salida = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new MenuInteractivo(EscenarioDemo.sistema(), new BufferedReader(new StringReader(guion)), salida).ejecutar();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static String capturar(Runnable accion) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            accion.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
