package com.apuntado.rules.evaluacion;

import com.apuntado.rules.api.IEvaluadorCombinaciones;
import com.apuntado.rules.api.ResultadoCombinaciones;
import com.apuntado.shared.Mano;

/**
 * Esqueleto: todavia no reconoce ternas ni cuartas.
 * El algoritmo real se implementa en el MVP, manteniendo CC <= 10 por metodo.
 */
public class EvaluadorCombinaciones implements IEvaluadorCombinaciones {

    @Override
    public ResultadoCombinaciones evaluarCombinaciones(Mano mano) {
        return new ResultadoCombinaciones(0, 0, mano.cartas());
    }
}
