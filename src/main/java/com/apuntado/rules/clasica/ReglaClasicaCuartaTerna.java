package com.apuntado.rules.clasica;

import com.apuntado.rules.api.IEvaluadorCombinaciones;
import com.apuntado.rules.api.ReglaJuego;
import com.apuntado.rules.api.ResultadoCombinaciones;
import com.apuntado.rules.api.ResultadoValidacion;
import com.apuntado.rules.evaluacion.EvaluadorCombinaciones;
import com.apuntado.shared.Mano;

/** RN-06: una cuarta, dos ternas y una carta de descarte. */
public class ReglaClasicaCuartaTerna implements ReglaJuego {

    private final IEvaluadorCombinaciones evaluador = new EvaluadorCombinaciones();

    @Override
    public String modalidad() {
        return "CLASICA";
    }

    @Override
    public ResultadoValidacion validarCierre(Mano mano) {
        ResultadoCombinaciones r = evaluador.evaluarCombinaciones(mano);
        if (r.cuartas() != 1 || r.ternas() != 2) {
            return ResultadoValidacion.rechazo("Se requieren 1 cuarta y 2 ternas");
        }
        if (r.sueltas().size() != 1) {
            return ResultadoValidacion.rechazo("Debe quedar exactamente 1 carta para descartar");
        }
        return ResultadoValidacion.ok();
    }

    @Override
    public boolean esAcumulativa() {
        return false;
    }
}
