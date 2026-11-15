package com.apuntado.rules.api;

import com.apuntado.shared.Mano;

/**
 * Contrato comun de las variantes de juego (Strategy, ADR-02 y ADR-03).
 * Las implementaciones se descubren con ServiceLoader; gameplay solo conoce esta interfaz.
 */
public interface ReglaJuego {

    /** Clave con la que el registro indexa la variante, por ejemplo "CLASICA". */
    String modalidad();

    /** Texto corto que la interfaz le muestra al jugador. */
    String descripcion();

    /**
     * Valida un cierre. La mano llega con 10 cartas: el orquestador ya aparto
     * la carta que el jugador eligio para descartar.
     */
    ResultadoValidacion validarCierre(Mano mano);

    /** Puntos que anota quien cierra (0 en Clasica; sus sobrantes en Apuntado). */
    int penalizacionCerrador(Mano mano);
}
