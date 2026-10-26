package com.apuntado.rules.registry;

import com.apuntado.rules.api.IRegistroReglas;
import com.apuntado.rules.api.ReglaJuego;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Descubre las variantes con ServiceLoader (META-INF/services), por lo que
 * no tiene dependencia de compilacion hacia ninguna variante (ADR-03).
 */
public class RegistroReglas implements IRegistroReglas {

    private final Map<String, ReglaJuego> reglas = new HashMap<>();

    public RegistroReglas() {
        for (ReglaJuego regla : ServiceLoader.load(ReglaJuego.class)) {
            reglas.put(regla.modalidad(), regla);
        }
    }

    @Override
    public ReglaJuego obtenerRegla(String modalidad) {
        ReglaJuego regla = reglas.get(modalidad);
        if (regla == null) {
            throw new IllegalArgumentException("Modalidad no registrada: " + modalidad);
        }
        return regla;
    }

    @Override
    public Set<String> modalidadesDisponibles() {
        return Set.copyOf(reglas.keySet());
    }
}
