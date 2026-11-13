package ec.edu.usfq.logistpulse;

import java.util.Objects;

/**
 * Mini framework de pruebas sin dependencias externas (no hace falta JUnit ni Maven).
 */
public final class MiniTest {

    private static int pasan;
    private static int fallan;

    @FunctionalInterface
    public interface Prueba {
        void correr() throws Exception;
    }

    private MiniTest() {
    }

    public static void seccion(String nombre) {
        System.out.println();
        System.out.println(nombre);
    }

    public static void prueba(String nombre, Prueba p) {
        try {
            p.correr();
            pasan++;
            System.out.println("  [PASA]  " + nombre);
        } catch (Throwable t) {
            fallan++;
            System.out.println("  [FALLA] " + nombre);
            System.out.println("          -> " + t);
        }
    }

    public static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    public static void iguales(Object esperado, Object real, String que) {
        if (!Objects.equals(esperado, real)) {
            throw new AssertionError(que + ": se esperaba <" + esperado + "> pero fue <" + real + ">");
        }
    }

    /** Verifica que la acción lance una excepción del tipo indicado y la devuelve. */
    public static <T extends Throwable> T lanza(Class<T> tipo, Runnable accion, String que) {
        try {
            accion.run();
        } catch (Throwable t) {
            if (tipo.isInstance(t)) {
                return tipo.cast(t);
            }
            throw new AssertionError(que + ": se esperaba " + tipo.getSimpleName() + " pero salio " + t);
        }
        throw new AssertionError(que + ": se esperaba " + tipo.getSimpleName() + " y no se lanzo nada");
    }

    public static int pasan() {
        return pasan;
    }

    public static int fallan() {
        return fallan;
    }
}
