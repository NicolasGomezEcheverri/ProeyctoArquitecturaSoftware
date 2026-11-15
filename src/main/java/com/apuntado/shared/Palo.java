package com.apuntado.shared;

public enum Palo {
    PICAS("♠"), CORAZONES("♥"), DIAMANTES("♦"), TREBOLES("♣");

    private final String simbolo;

    Palo(String simbolo) {
        this.simbolo = simbolo;
    }

    public String simbolo() {
        return simbolo;
    }
}
