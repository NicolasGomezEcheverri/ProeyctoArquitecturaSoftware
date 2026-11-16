package com.apuntado.gameplay.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "partida")
public class PartidaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "modalidad_cierre", nullable = false, length = 20)
    private String modalidadCierre;

    @Column(name = "ganador_id")
    private Integer ganadorId;

    /** Cuarta barrera: bloqueo optimista sobre la partida. */
    @Version
    private Integer version;

    @Column(name = "umbral_puntos")
    private int umbralPuntos;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "creada_en")
    private LocalDateTime creadaEn;

    @Column(name = "finalizada_en")
    private LocalDateTime finalizadaEn;

    protected PartidaEntity() {
    }

    PartidaEntity(String codigo, String modalidadCierre, int umbralPuntos) {
        this.codigo = codigo;
        this.modalidadCierre = modalidadCierre;
        this.umbralPuntos = umbralPuntos;
        this.estado = "EN_JUEGO";
        this.creadaEn = LocalDateTime.now();
    }

    void finalizar(int ganador) {
        this.ganadorId = ganador;
        this.estado = "FINALIZADA";
        this.finalizadaEn = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getModalidadCierre() {
        return modalidadCierre;
    }

    public Integer getGanadorId() {
        return ganadorId;
    }

    public String getEstado() {
        return estado;
    }

    public Integer getVersion() {
        return version;
    }
}
