package com.apuntado.shared;

/**
 * Valor de una carta. Puntos segun RN-05: numericas su valor, J, Q, K y A valen 10.
 * El rango (1 a 13) sirve para reconocer escaleras; el As es 1, pero tambien puede
 * cerrar una escalera por arriba (Q-K-A).
 */
public enum Valor {
    AS(10, "A"), DOS(2, "2"), TRES(3, "3"), CUATRO(4, "4"), CINCO(5, "5"), SEIS(6, "6"),
    SIETE(7, "7"), OCHO(8, "8"), NUEVE(9, "9"), DIEZ(10, "10"), J(10, "J"), Q(10, "Q"), K(10, "K");

    private final int puntos;
    private final String simbolo;

    Valor(int puntos, String simbolo) {
        this.puntos = puntos;
        this.simbolo = simbolo;
    }

    public int puntos() {
        return puntos;
    }

    public String simbolo() {
        return simbolo;
    }

    /** Posicion en la secuencia A, 2, ..., K (1 a 13). */
    public int rango() {
        return ordinal() + 1;
    }
}
