package ec.edu.usfq.logistpulse.dominio;

import java.util.Locale;
import java.util.Objects;

/**
 * Persona o agente de IA que solicita una operación.
 *
 * @param rol define qué operaciones puede pedir; un agente de IA siempre es OPERADOR
 */
public record Actor(String nombre, TipoActor tipo, Rol rol) {

    public Actor {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(rol, "rol");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El actor necesita un nombre");
        }
        if (tipo == TipoActor.AGENTE_IA && rol != Rol.OPERADOR) {
            throw new IllegalArgumentException("Un agente de IA solo puede tener rol OPERADOR");
        }
    }

    public static Actor humano(String nombre, Rol rol) {
        return new Actor(nombre, TipoActor.HUMANO, rol);
    }

    public static Actor agente(String nombre) {
        return new Actor(nombre, TipoActor.AGENTE_IA, Rol.OPERADOR);
    }

    public boolean esAgente() {
        return tipo == TipoActor.AGENTE_IA;
    }

    public boolean esSupervisor() {
        return rol == Rol.SUPERVISOR;
    }

    /** Misma identidad (nombre y tipo), sin importar el rol con el que llegue. */
    public boolean esLaMismaQue(Actor otro) {
        return otro != null && nombre.equals(otro.nombre) && tipo == otro.tipo;
    }

    @Override
    public String toString() {
        return nombre + " (" + (esAgente() ? "IA" : rol.name().toLowerCase(Locale.ROOT)) + ")";
    }
}
