package com.apuntado.rules.clasica;

import com.apuntado.rules.api.IEvaluadorCombinaciones;
import com.apuntado.rules.api.ReglaJuego;
import com.apuntado.rules.api.ResultadoValidacion;
import com.apuntado.rules.evaluacion.EvaluadorCombinaciones;
import com.apuntado.shared.Mano;

/**
 * RN-06: una cuarta, dos ternas y una carta de descarte (4-3-3 + 1).
 * El descarte ya lo aparto el orquestador, asi que aqui las 10 cartas
 * restantes tienen que quedar cubiertas por la cuarta y las dos ternas.
 */
public class ReglaClasicaCuartaTerna implements ReglaJuego {

    static final int CARTAS_AL_CERRAR = 10;

    private final IEvaluadorCombinaciones evaluador = new EvaluadorCombinaciones();

    @Override
    public String modalidad() {
        return "CLASICA";
    }

    @Override
    public String descripcion() {
        return "Clásica (4-3-3): cierra quien tenga 1 cuarta y 2 ternas y descarte la carta sobrante.";
    }

    @Override
    public ResultadoValidacion validarCierre(Mano mano) {
        if (mano.tamano() != CARTAS_AL_CERRAR) {
            return ResultadoValidacion.rechazo("Para cerrar debes tener 11 cartas y elegir una para descartar");
        }
        return evaluador.buscarDistribucion(mano, 1, 2)
                .map(ResultadoValidacion::ok)
                .orElseGet(() -> ResultadoValidacion.rechazo(
                        "En Clásica necesitas 1 cuarta y 2 ternas con las 10 cartas que te quedan"));
    }

    @Override
    public int penalizacionCerrador(Mano mano) {
        return 0;
    }
}
