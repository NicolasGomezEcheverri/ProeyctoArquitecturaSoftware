package com.apuntado.gameplay.api;

import java.time.Instant;

public record RegistroBitacora(Instant ocurridoEn, Integer ronda, Integer jugadorId, String jugador,
        String tipo, String detalle, Long valorMs) {
}
