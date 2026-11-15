package com.apuntado.rules.apuntado;

import com.apuntado.rules.api.IEvaluadorCombinaciones;
import com.apuntado.rules.api.ReglaJuego;
import com.apuntado.rules.api.ResultadoCombinaciones;
import com.apuntado.rules.api.ResultadoValidacion;
import com.apuntado.rules.evaluacion.EvaluadorCombinaciones;
import com.apuntado.shared.Mano;
import java.util.Optional;

/**
 * RN-07: tres ternas y cartas sobrantes que sumen menos de 5 puntos.
 * Quien cierra "apunta" lo que le sobro; 5 puntos exactos ya no alcanzan.
 */
public class ReglaApuntado101 implements ReglaJuego {

    static final int LIMITE_SOBRANTES = 5;
    static final int CARTAS_AL_CERRAR = 10;

    private final IEvaluadorCombinaciones evaluador = new EvaluadorCombinaciones();

    @Override
    public String modalidad() {
        return "APUNTADO";
    }

    @Override
    public String descripcion() {
        return "Apuntado a 101: cierra quien tenga 3 ternas y le sobren menos de 5 puntos; "
                + "pierde quien llegue a 101.";
    }

    @Override
    public ResultadoValidacion validarCierre(Mano mano) {
        if (mano.tamano() != CARTAS_AL_CERRAR) {
            return ResultadoValidacion.rechazo("Para cerrar debes tener 11 cartas y elegir una para descartar");
        }
        Optional<ResultadoCombinaciones> distribucion = evaluador.buscarDistribucion(mano, 0, 3);
        if (distribucion.isEmpty()) {
            return ResultadoValidacion.rechazo("En Apuntado necesitas 3 ternas");
        }
        int sobrantes = distribucion.get().puntosSueltos();
        if (sobrantes >= LIMITE_SOBRANTES) {
            return ResultadoValidacion.rechazo("Tienes las 3 ternas, pero te sobran " + sobrantes
                    + " puntos y deben ser menos de " + LIMITE_SOBRANTES);
        }
        return ResultadoValidacion.ok(distribucion.get());
    }

    @Override
    public int penalizacionCerrador(Mano mano) {
        return evaluador.buscarDistribucion(mano, 0, 3)
                .map(ResultadoCombinaciones::puntosSueltos)
                .orElse(0);
    }
}
