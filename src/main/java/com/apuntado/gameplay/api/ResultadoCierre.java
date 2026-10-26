package com.apuntado.gameplay.api;

/** Respuesta al cliente; no expone tipos de rules ni revela la mano. */
public record ResultadoCierre(boolean aceptado, String condicionIncumplida) { }
