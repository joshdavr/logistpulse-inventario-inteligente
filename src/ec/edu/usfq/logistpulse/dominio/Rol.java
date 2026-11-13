package ec.edu.usfq.logistpulse.dominio;

/**
 * Qué puede hacer un actor. Cada comando declara qué roles lo pueden ejecutar.
 */
public enum Rol {
    /** Opera la bodega: recibe, almacena, reserva, despacha y traslada. Solo gestiona sus propias reservas. */
    OPERADOR,
    /** Puede todo, incluido gestionar reservas ajenas y ajustar stock. */
    SUPERVISOR,
    /** Control de calidad: cuarentenas y ajustes por merma o daño. */
    CALIDAD
}
