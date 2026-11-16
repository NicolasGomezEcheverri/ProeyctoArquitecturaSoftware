package com.apuntado.scoring.api;

import java.util.List;

public record ResultadoLiquidacion(Marcador marcador, List<Integer> eliminadosEnEstaRonda) {

    public ResultadoLiquidacion {
        eliminadosEnEstaRonda = List.copyOf(eliminadosEnEstaRonda);
    }
}
