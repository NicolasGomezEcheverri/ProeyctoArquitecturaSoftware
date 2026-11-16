package com.apuntado.history.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Traza de movimientos de cartas de una ronda (RF-27). */
@Entity
@Table(name = "turno_jugada")
public class TurnoJugadaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ronda_id", nullable = false)
    private int rondaId;

    @Column(nullable = false)
    private int secuencia;

    @Column(name = "jugador_id", nullable = false)
    private int jugadorId;

    @Column(nullable = false, length = 120)
    private String accion;

    private LocalDateTime fecha;

    protected TurnoJugadaEntity() {
    }

    TurnoJugadaEntity(int rondaId, int secuencia, int jugadorId, String accion, LocalDateTime fecha) {
        this.rondaId = rondaId;
        this.secuencia = secuencia;
        this.jugadorId = jugadorId;
        this.accion = accion;
        this.fecha = fecha;
    }

    public Integer getId() {
        return id;
    }

    public int getSecuencia() {
        return secuencia;
    }

    public String getAccion() {
        return accion;
    }
}
