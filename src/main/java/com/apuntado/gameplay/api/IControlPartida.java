package com.apuntado.gameplay.api;

import java.util.Map;

public interface IControlPartida {

    /** Crea la mesa, sortea el orden y reparte la primera ronda. Devuelve el id de la partida. */
    int iniciarPartida(ConfiguracionInicio configuracion);

    Map<String, String> modalidadesDisponibles();

    /** Evita repetir un codigo de sala que ya uso otra partida. */
    boolean codigoEnUso(String codigo);
}
