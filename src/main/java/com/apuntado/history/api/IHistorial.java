package com.apuntado.history.api;

import java.util.List;

/** RoundEventLog + MatchHistoryRepository (RF-27, RF-28). */
public interface IHistorial {

    void registrarEvento(EventoJuego evento);

    List<EventoJuego> eventosDePartida(int partidaId);

    List<MetricasJugador> metricas(int partidaId);
}
