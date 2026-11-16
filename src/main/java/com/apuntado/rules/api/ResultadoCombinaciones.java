package com.apuntado.rules.api;

import com.apuntado.shared.Carta;
import java.util.List;

/** Una forma de repartir la mano en combinaciones y cartas sueltas. */
public record ResultadoCombinaciones(List<Combinacion> combinaciones, List<Carta> sueltas) {

    public ResultadoCombinaciones {
        combinaciones = List.copyOf(combinaciones);
        sueltas = List.copyOf(sueltas);
    }

    public int puntosSueltos() {
        return sueltas.stream().mapToInt(Carta::valorPuntos).sum();
    }

    public long ternas() {
        return combinaciones.stream().filter(Combinacion::esTerna).count();
    }

    public long cuartas() {
        return combinaciones.stream().filter(Combinacion::esCuarta).count();
    }
}
