package com.apuntado.rules.api;

import com.apuntado.shared.Mano;

/** Contrato comun de las variantes de juego (Strategy). */
public interface ReglaJuego {

    /** Clave con la que el registro indexa la variante, por ejemplo "CLASICA". */
    String modalidad();

    ResultadoValidacion validarCierre(Mano mano);

    boolean esAcumulativa();
}
