package com.apuntado.session.internal;

import com.apuntado.gameplay.api.Participante;
import com.apuntado.session.api.EstadoSala;
import java.util.ArrayList;
import java.util.List;

/** Sala de espera: existe desde que alguien la crea hasta que la partida arranca. */
final class Sala {

    private final String codigo;
    private final String modalidad;
    private final int cupo;
    private final int umbral;
    private final List<Participante> jugadores = new ArrayList<>();
    private boolean iniciada;

    Sala(String codigo, String modalidad, int cupo, int umbral) {
        this.codigo = codigo;
        this.modalidad = modalidad;
        this.cupo = cupo;
        this.umbral = umbral;
    }

    synchronized void agregar(Participante participante) {
        if (iniciada) {
            throw new IllegalStateException("La partida de esta sala ya empezó");
        }
        if (jugadores.size() >= cupo) {
            throw new IllegalStateException("La sala ya está completa");
        }
        jugadores.add(participante);
    }

    synchronized boolean llena() {
        return jugadores.size() == cupo;
    }

    /** Marca la sala como iniciada una sola vez; devuelve false si ya lo estaba. */
    synchronized boolean marcarIniciada() {
        if (iniciada) {
            return false;
        }
        iniciada = true;
        return true;
    }

    synchronized void revertirInicio() {
        iniciada = false;
    }

    synchronized List<Participante> jugadores() {
        return List.copyOf(jugadores);
    }

    synchronized Participante creador() {
        return jugadores.get(0);
    }

    synchronized EstadoSala estado() {
        return new EstadoSala(codigo, modalidad, cupo, umbral, creador().jugadorId(),
                jugadores.stream().map(Participante::nombre).toList(), iniciada);
    }

    String codigo() {
        return codigo;
    }

    String modalidad() {
        return modalidad;
    }

    int umbral() {
        return umbral;
    }
}
