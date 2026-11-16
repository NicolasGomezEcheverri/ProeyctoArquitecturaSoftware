package com.apuntado.rules.evaluacion;

import com.apuntado.rules.api.IEvaluadorCombinaciones;
import com.apuntado.rules.api.ResultadoCombinaciones;
import com.apuntado.shared.Mano;
import java.util.Optional;

/** HandEvaluator: genera las combinaciones posibles y busca el mejor reparto. */
public class EvaluadorCombinaciones implements IEvaluadorCombinaciones {

    private final GeneradorCandidatas generador = new GeneradorCandidatas();

    @Override
    public ResultadoCombinaciones evaluarCombinaciones(Mano mano) {
        // Un reparto libre siempre existe: en el peor caso todas las cartas quedan sueltas.
        return buscar(mano, CriterioBusqueda.libreMenosPuntos()).orElseThrow();
    }

    @Override
    public Optional<ResultadoCombinaciones> buscarDistribucion(Mano mano, int cuartas, int ternas) {
        return buscar(mano, CriterioBusqueda.exacto(cuartas, ternas));
    }

    private Optional<ResultadoCombinaciones> buscar(Mano mano, CriterioBusqueda criterio) {
        return new BusquedaParticion(mano.cartas(), generador.generar(mano.cartas()), criterio).ejecutar();
    }
}
