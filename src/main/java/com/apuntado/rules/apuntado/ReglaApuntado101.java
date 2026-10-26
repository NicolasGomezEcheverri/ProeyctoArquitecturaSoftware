package com.apuntado.rules.apuntado;

import com.apuntado.rules.api.IEvaluadorCombinaciones;
import com.apuntado.rules.api.ReglaJuego;
import com.apuntado.rules.api.ResultadoCombinaciones;
import com.apuntado.rules.api.ResultadoValidacion;
import com.apuntado.rules.evaluacion.EvaluadorCombinaciones;
import com.apuntado.shared.Carta;
import com.apuntado.shared.Mano;

/** RN-07: tres ternas y cartas sueltas que sumen menos de 5 puntos. */
public class ReglaApuntado101 implements ReglaJuego {

    private static final int LIMITE_SUELTAS = 5;

    private final IEvaluadorCombinaciones evaluador = new EvaluadorCombinaciones();

    @Override
    public String modalidad() {
        return "APUNTADO";
    }

    @Override
    public ResultadoValidacion validarCierre(Mano mano) {
        ResultadoCombinaciones r = evaluador.evaluarCombinaciones(mano);
        if (r.ternas() != 3) {
            return ResultadoValidacion.rechazo("Se requieren 3 ternas");
        }
        int suma = r.sueltas().stream().mapToInt(Carta::valorPuntos).sum();
        if (suma >= LIMITE_SUELTAS) {
            return ResultadoValidacion.rechazo("Las cartas sueltas deben sumar menos de 5 puntos");
        }
        return ResultadoValidacion.ok();
    }

    @Override
    public boolean esAcumulativa() {
        return true;
    }
}
