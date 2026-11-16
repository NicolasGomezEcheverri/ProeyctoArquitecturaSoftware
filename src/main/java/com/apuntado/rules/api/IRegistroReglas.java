package com.apuntado.rules.api;

import java.util.Map;

public interface IRegistroReglas {

    ReglaJuego obtenerRegla(String modalidad);

    /** Modalidad -> descripcion, en el orden en que se registraron. */
    Map<String, String> modalidadesDisponibles();
}
