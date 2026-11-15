package com.apuntado.gameplay.api;

/** Respuesta privada al jugador que hizo la accion; nunca revela manos ajenas. */
public record ResultadoAccion(boolean aceptada, String mensaje) {

    public static ResultadoAccion ok(String mensaje) {
        return new ResultadoAccion(true, mensaje);
    }

    public static ResultadoAccion rechazo(String mensaje) {
        return new ResultadoAccion(false, mensaje);
    }
}
