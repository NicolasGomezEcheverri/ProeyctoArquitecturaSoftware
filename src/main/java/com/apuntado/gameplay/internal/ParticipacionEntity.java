package com.apuntado.gameplay.internal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "participacion")
public class ParticipacionEntity {

    @EmbeddedId
    private ParticipacionId id;

    @Column(name = "orden_turno", nullable = false)
    private int ordenTurno;

    private int reingresos;

    protected ParticipacionEntity() {
    }

    ParticipacionEntity(int partidaId, int jugadorId, int ordenTurno) {
        this.id = new ParticipacionId(partidaId, jugadorId);
        this.ordenTurno = ordenTurno;
    }

    public ParticipacionId getId() {
        return id;
    }

    public int getOrdenTurno() {
        return ordenTurno;
    }
}
