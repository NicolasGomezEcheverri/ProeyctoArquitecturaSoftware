package com.apuntado.rules.api;

import com.apuntado.shared.Carta;
import java.util.List;

/** Una terna o una cuarta: mismo valor (grupo) o mismo palo consecutivo (escalera). */
public record Combinacion(Forma forma, List<Carta> cartas) {

    public enum Forma { GRUPO, ESCALERA }

    public Combinacion {
        cartas = List.copyOf(cartas);
    }

    public boolean esCuarta() {
        return cartas.size() == 4;
    }

    public boolean esTerna() {
        return cartas.size() == 3;
    }
}
