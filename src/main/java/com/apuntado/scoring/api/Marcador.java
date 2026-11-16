package com.apuntado.scoring.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Copia de solo lectura del marcador de una partida. */
public record Marcador(int umbral, Map<Integer, Integer> acumulados, Set<Integer> eliminados) {

    public Marcador {
        acumulados = Map.copyOf(acumulados);
        eliminados = Set.copyOf(eliminados);
    }

    public int acumuladoDe(int jugadorId) {
        return acumulados.getOrDefault(jugadorId, 0);
    }

    public boolean estaEliminado(int jugadorId) {
        return eliminados.contains(jugadorId);
    }

    public List<Integer> activos() {
        return acumulados.keySet().stream().filter(id -> !eliminados.contains(id)).sorted().toList();
    }
}
