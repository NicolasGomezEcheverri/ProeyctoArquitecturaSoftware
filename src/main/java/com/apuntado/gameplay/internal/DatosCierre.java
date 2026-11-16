package com.apuntado.gameplay.internal;

import com.apuntado.shared.Carta;
import java.util.Map;

/** Lo necesario para registrar un cierre valido en una sola transaccion. */
record DatosCierre(int partidaId, int rondaId, int ronda, int cerradorId, String modalidad,
        Carta descarte, Map<Integer, Integer> penalizaciones) {

    DatosCierre {
        penalizaciones = Map.copyOf(penalizaciones);
    }
}
