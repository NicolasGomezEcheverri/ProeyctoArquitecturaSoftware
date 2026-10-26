package com.apuntado.shared;

/** Valor de una carta. Puntos segun RN-05: numericas su valor, J, Q, K y A valen 10. */
public enum Valor {
    AS(10), DOS(2), TRES(3), CUATRO(4), CINCO(5), SEIS(6), SIETE(7),
    OCHO(8), NUEVE(9), DIEZ(10), J(10), Q(10), K(10);

    private final int puntos;

    Valor(int puntos) {
        this.puntos = puntos;
    }

    public int puntos() {
        return puntos;
    }
}
