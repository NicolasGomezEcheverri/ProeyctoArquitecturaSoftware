package com.apuntado.rules.evaluacion;

import com.apuntado.rules.api.Combinacion.Forma;

/**
 * Combinacion posible dentro de una mano, guardada como mascara de bits sobre
 * las posiciones de las cartas (la mano nunca pasa de 11 cartas).
 */
record Candidata(int mascara, Forma forma) {

    int tamano() {
        return Integer.bitCount(mascara);
    }

    int menorIndice() {
        return Integer.numberOfTrailingZeros(mascara);
    }

    boolean esCuarta() {
        return tamano() == 4;
    }
}
