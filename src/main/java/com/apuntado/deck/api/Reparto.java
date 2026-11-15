package com.apuntado.deck.api;

import com.apuntado.shared.Carta;
import java.util.List;

/** Manos repartidas (en orden de turno) y lo que queda como mazo de robo. */
public record Reparto(List<List<Carta>> manos, List<Carta> mazoRestante) {

    public Reparto {
        manos = manos.stream().map(List::copyOf).toList();
        mazoRestante = List.copyOf(mazoRestante);
    }
}
