package com.apuntado.history.api;

public enum TipoEvento {
    PARTIDA_INICIADA, RONDA_INICIADA, TURNO_INICIADO,
    ROBO_MAZO(true), ROBO_POZO(true), DESCARTE(true), MAZO_RECONSTRUIDO,
    CIERRE_ACEPTADO(true), CIERRE_RECHAZADO(true), ACCION_RECHAZADA,
    JUGADOR_ELIMINADO, PARTIDA_FINALIZADA, LATENCIA_CLIENTE;

    private final boolean jugada;

    TipoEvento() {
        this(false);
    }

    TipoEvento(boolean jugada) {
        this.jugada = jugada;
    }

    /** Las jugadas tambien quedan en turno_jugada, la traza de movimientos de la ronda (RF-27). */
    public boolean esJugada() {
        return jugada;
    }
}
