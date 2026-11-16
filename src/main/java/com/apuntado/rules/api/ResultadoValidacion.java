package com.apuntado.rules.api;

import com.apuntado.shared.Carta;
import java.util.List;

/**
 * Resultado de validar un cierre. Si es valido trae las combinaciones que lo
 * justifican, para revelarlas a la mesa (RF-16); si no, solo la condicion
 * incumplida, para no exponer la mano (RF-15).
 */
public record ResultadoValidacion(boolean valido, String condicionIncumplida,
        List<Combinacion> combinaciones, List<Carta> sobrantes) {

    public ResultadoValidacion {
        combinaciones = List.copyOf(combinaciones);
        sobrantes = List.copyOf(sobrantes);
    }

    public static ResultadoValidacion ok(ResultadoCombinaciones distribucion) {
        return new ResultadoValidacion(true, null, distribucion.combinaciones(), distribucion.sueltas());
    }

    public static ResultadoValidacion rechazo(String condicionIncumplida) {
        return new ResultadoValidacion(false, condicionIncumplida, List.of(), List.of());
    }
}
