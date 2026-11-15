package com.apuntado.rules.api;

import com.apuntado.shared.Mano;
import java.util.Optional;

/** HandEvaluator: reconoce ternas y cuartas en una mano (RF-12, RF-17). */
public interface IEvaluadorCombinaciones {

    /**
     * Mejor reparto libre de la mano: el que deja menos puntos sueltos.
     * Se usa para liquidar a quienes no cerraron.
     */
    ResultadoCombinaciones evaluarCombinaciones(Mano mano);

    /**
     * Busca un reparto con exactamente esa cantidad de cuartas y ternas. Si hay
     * varios, devuelve el que deja menos puntos sueltos.
     */
    Optional<ResultadoCombinaciones> buscarDistribucion(Mano mano, int cuartas, int ternas);
}
