package com.apuntado.gameplay.api;

import com.apuntado.shared.Carta;
import java.util.List;

public record PenalizacionRonda(int jugadorId, String nombre, int puntos, List<Carta> cartasSueltas,
        int acumulado, boolean eliminado) {

    public PenalizacionRonda {
        cartasSueltas = List.copyOf(cartasSueltas);
    }
}
