package com.apuntado.shared;

import java.util.Objects;

public record Carta(Palo palo, Valor valor) {

    public Carta {
        Objects.requireNonNull(palo, "palo");
        Objects.requireNonNull(valor, "valor");
    }

    public int valorPuntos() {
        return valor.puntos();
    }

    /** Texto corto para bitacoras y mensajes, por ejemplo "7♥". */
    public String texto() {
        return valor.simbolo() + palo.simbolo();
    }
}
