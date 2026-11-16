package com.apuntado.session.api;

/** Credenciales que recibe el navegador al crear o unirse a una sala. */
public record IngresoSala(String codigo, int jugadorId, String token, String nombre) {
}
