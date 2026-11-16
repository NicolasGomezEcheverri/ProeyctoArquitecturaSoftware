package com.apuntado.rules.evaluacion;

/** Que reparto se busca: uno libre o uno con un numero exacto de cuartas y ternas. */
record CriterioBusqueda(boolean libre, int cuartas, int ternas) {

    static CriterioBusqueda libreMenosPuntos() {
        return new CriterioBusqueda(true, 0, 0);
    }

    static CriterioBusqueda exacto(int cuartas, int ternas) {
        return new CriterioBusqueda(false, cuartas, ternas);
    }

    boolean excede(int cuartasElegidas, int ternasElegidas) {
        return !libre && (cuartasElegidas > cuartas || ternasElegidas > ternas);
    }

    boolean cumple(int cuartasElegidas, int ternasElegidas) {
        return libre || (cuartasElegidas == cuartas && ternasElegidas == ternas);
    }
}
