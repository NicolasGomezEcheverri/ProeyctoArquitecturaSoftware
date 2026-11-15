package com.apuntado.scoring.api;

import java.util.List;
import java.util.Map;

/**
 * RoundScoring + Scoreboard + EliminationEvaluator (RF-17, RF-18, RF-20).
 * Es la unica puerta para modificar el marcador (RN-11).
 */
public interface IPuntuacion {

    void abrirMarcador(int partidaId, List<Integer> jugadores, int umbral);

    /**
     * Suma las penalizaciones de la ronda al acumulado de cada jugador y evalua
     * la eliminacion. Debe ejecutarse dentro de la transaccion del cierre.
     */
    ResultadoLiquidacion liquidarRonda(int partidaId, int rondaId, Map<Integer, Integer> penalizaciones);

    Marcador consultarMarcador(int partidaId);
}
