package com.apuntado.scoring.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record PuntajeId(
        @Column(name = "ronda_id") Integer rondaId,
        @Column(name = "jugador_id") Integer jugadorId) implements Serializable {
}
