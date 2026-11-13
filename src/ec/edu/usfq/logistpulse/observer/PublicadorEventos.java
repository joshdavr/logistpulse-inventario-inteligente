package ec.edu.usfq.logistpulse.observer;

/**
 * Lo único que necesitan quienes emiten eventos (el inventario y la central):
 * poder publicar. No ven cómo se gestionan los suscriptores (I y D de SOLID).
 *
 * <p><b>Contrato:</b> {@link #publicar} nunca propaga excepciones de los suscriptores.</p>
 */
public interface PublicadorEventos {

    void publicar(Evento evento);
}
