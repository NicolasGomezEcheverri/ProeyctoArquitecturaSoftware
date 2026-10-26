package com.apuntado.shared;

import java.util.List;

public record Mano(List<Carta> cartas) {
    public Mano {
        cartas = List.copyOf(cartas);
    }
}
