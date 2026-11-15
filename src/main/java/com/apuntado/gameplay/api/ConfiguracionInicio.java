package com.apuntado.gameplay.api;

import java.util.List;

/** Lo que session le entrega a gameplay cuando la sala esta lista (RF-01 a RF-03). */
public record ConfiguracionInicio(String codigo, String modalidad, int umbral, List<Participante> participantes) {

    public ConfiguracionInicio {
        participantes = List.copyOf(participantes);
    }
}
