package com.apuntado.scoring.internal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Fila de puntaje_acumulado: la PK (ronda, jugador) impide liquidar dos veces la misma ronda. */
@Entity
@Table(name = "puntaje_acumulado")
public class PuntajeAcumuladoEntity {

    @EmbeddedId
    private PuntajeId id;

    @Column(nullable = false)
    private int penalizacion;

    @Column(nullable = false)
    private int acumulado;

    protected PuntajeAcumuladoEntity() {
    }

    PuntajeAcumuladoEntity(int rondaId, int jugadorId, int penalizacion, int acumulado) {
        this.id = new PuntajeId(rondaId, jugadorId);
        this.penalizacion = penalizacion;
        this.acumulado = acumulado;
    }

    public PuntajeId getId() {
        return id;
    }

    public int getPenalizacion() {
        return penalizacion;
    }

    public int getAcumulado() {
        return acumulado;
    }
}
