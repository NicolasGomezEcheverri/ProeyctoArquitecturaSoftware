package com.apuntado.rules.api;

public record ResultadoValidacion(boolean valido, String condicionIncumplida) {

    public static ResultadoValidacion ok() {
        return new ResultadoValidacion(true, null);
    }

    public static ResultadoValidacion rechazo(String condicionIncumplida) {
        return new ResultadoValidacion(false, condicionIncumplida);
    }
}
