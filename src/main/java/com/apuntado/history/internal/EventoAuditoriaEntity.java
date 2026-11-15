package com.apuntado.history.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "evento_auditoria")
public class EventoAuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "partida_id", nullable = false)
    private int partidaId;

    private Integer ronda;

    @Column(name = "jugador_id")
    private Integer jugadorId;

    @Column(nullable = false, length = 40)
    private String tipo;

    private String detalle;

    @Column(name = "valor_ms")
    private Long valorMs;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    protected EventoAuditoriaEntity() {
    }

    EventoAuditoriaEntity(int partidaId, Integer ronda, Integer jugadorId, String tipo,
            String detalle, Long valorMs, LocalDateTime ocurridoEn) {
        this.partidaId = partidaId;
        this.ronda = ronda;
        this.jugadorId = jugadorId;
        this.tipo = tipo;
        this.detalle = detalle;
        this.valorMs = valorMs;
        this.ocurridoEn = ocurridoEn;
    }

    public Integer getId() {
        return id;
    }

    public int getPartidaId() {
        return partidaId;
    }

    public Integer getRonda() {
        return ronda;
    }

    public Integer getJugadorId() {
        return jugadorId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDetalle() {
        return detalle;
    }

    public Long getValorMs() {
        return valorMs;
    }

    public LocalDateTime getOcurridoEn() {
        return ocurridoEn;
    }
}
