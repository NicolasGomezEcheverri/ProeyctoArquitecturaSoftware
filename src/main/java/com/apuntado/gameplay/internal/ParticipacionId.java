package com.apuntado.gameplay.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record ParticipacionId(
        @Column(name = "partida_id") Integer partidaId,
        @Column(name = "jugador_id") Integer jugadorId) implements Serializable {
}
