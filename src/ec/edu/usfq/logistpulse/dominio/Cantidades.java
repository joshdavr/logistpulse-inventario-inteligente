package ec.edu.usfq.logistpulse.dominio;

/**
 * Validación única de cantidades. Pone un tope por operación para que ninguna
 * suma de enteros se desborde (por ejemplo, 530 + Integer.MAX_VALUE daría un
 * número negativo y "cabría" en cualquier bodega).
 */
public final class Cantidades {

    /** Máximo de unidades en una sola operación. */
    public static final int MAXIMO = 1_000_000;

    private Cantidades() {
    }

    /** Exige 1 <= n <= MAXIMO. */
    public static int exigirPositiva(int n, String que) {
        if (n <= 0 || n > MAXIMO) {
            throw new IllegalArgumentException(que + " debe estar entre 1 y " + MAXIMO + " (fue " + n + ")");
        }
        return n;
    }

    /** Exige 0 <= n <= MAXIMO. */
    public static int exigirNoNegativa(int n, String que) {
        if (n < 0 || n > MAXIMO) {
            throw new IllegalArgumentException(que + " debe estar entre 0 y " + MAXIMO + " (fue " + n + ")");
        }
        return n;
    }

    /** Exige un ajuste distinto de 0 con |delta| <= MAXIMO (también rechaza Integer.MIN_VALUE). */
    public static int exigirAjuste(int delta) {
        if (delta == 0 || delta < -MAXIMO || delta > MAXIMO) {
            throw new IllegalArgumentException("Un ajuste debe estar entre -" + MAXIMO + " y " + MAXIMO
                    + " y ser distinto de 0 (fue " + delta + ")");
        }
        return delta;
    }
}
