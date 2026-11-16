package com.apuntado.gameplay.api;

import com.apuntado.shared.Carta;
import java.util.List;

/** Lo que se revela al cerrar una ronda valida (RF-16) y como quedo cada jugador. */
public record ResumenCierre(int ronda, int cerradorId, String cerradorNombre, Carta descarte,
        List<List<Carta>> combinacionesCerrador, List<Carta> sobrantesCerrador,
        List<PenalizacionRonda> penalizaciones) {

    public ResumenCierre {
        combinacionesCerrador = combinacionesCerrador.stream().map(List::copyOf).toList();
        sobrantesCerrador = List.copyOf(sobrantesCerrador);
        penalizaciones = List.copyOf(penalizaciones);
    }
}
