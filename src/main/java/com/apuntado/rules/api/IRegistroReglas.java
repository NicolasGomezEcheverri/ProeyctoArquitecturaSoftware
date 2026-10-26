package com.apuntado.rules.api;

import java.util.Set;

public interface IRegistroReglas {

    ReglaJuego obtenerRegla(String modalidad);

    Set<String> modalidadesDisponibles();
}
