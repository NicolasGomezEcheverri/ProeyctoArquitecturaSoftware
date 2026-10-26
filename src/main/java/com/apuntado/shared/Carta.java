package com.apuntado.shared;

public record Carta(Palo palo, Valor valor) {

    public int valorPuntos() {
        return valor.puntos();
    }
}
