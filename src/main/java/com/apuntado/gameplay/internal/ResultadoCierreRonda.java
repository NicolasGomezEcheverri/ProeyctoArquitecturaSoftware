package com.apuntado.gameplay.internal;

import com.apuntado.scoring.api.ResultadoLiquidacion;

record ResultadoCierreRonda(ResultadoLiquidacion liquidacion, Integer ganadorId) {
}
