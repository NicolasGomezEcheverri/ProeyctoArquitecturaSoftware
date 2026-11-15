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
@Table(name = "ronda")
public class RondaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "partida_id", nullable = false)
    private int partidaId;

    @Column(nullable = false)
    private int numero;

    @Column(name = "tipo_cierre", length = 20)
    private String tipoCierre;

    @Column(name = "cerrador_id")
    private Integer cerradorId;

    private LocalDateTime fecha;

    @Version
    private Integer version;

    protected RondaEntity() {
    }

    RondaEntity(int partidaId, int numero) {
        this.partidaId = partidaId;
        this.numero = numero;
        this.fecha = LocalDateTime.now();
    }

    boolean estaCerrada() {
        return cerradorId != null;
    }

    void cerrar(String modalidad, int cerrador) {
        this.tipoCierre = modalidad;
        this.cerradorId = cerrador;
    }

    public Integer getId() {
        return id;
    }

    public int getNumero() {
        return numero;
    }

    public Integer getCerradorId() {
        return cerradorId;
    }

    public String getTipoCierre() {
        return tipoCierre;
    }
}
