package com.apuntado.session.api;

import java.util.List;

public record EstadoSala(String codigo, String modalidad, int cupo, int umbral, int creadorId,
        List<String> jugadores, boolean iniciada) {

    public EstadoSala {
        jugadores = List.copyOf(jugadores);
    }
}
