package com.apuntado.gameplay.api;

/** Jugador sentado en la mesa. El token identifica su navegador en cada accion. */
public record Participante(int jugadorId, String nombre, String token) {
}
