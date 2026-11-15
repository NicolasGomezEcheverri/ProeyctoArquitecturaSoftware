package com.apuntado.rules.registry;

import com.apuntado.rules.api.IRegistroReglas;
import com.apuntado.rules.api.ReglaJuego;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Descubre las variantes con ServiceLoader (META-INF/services), por lo que
 * no tiene dependencia de compilacion hacia ninguna variante (ADR-03).
 */
public class RegistroReglas implements IRegistroReglas {

    private final Map<String, ReglaJuego> reglas = new LinkedHashMap<>();

    public RegistroReglas() {
        this(ServiceLoader.load(ReglaJuego.class));
    }

    /** Permite registrar variantes a mano en las pruebas. */
    public RegistroReglas(Iterable<ReglaJuego> variantes) {
        for (ReglaJuego regla : variantes) {
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
    public Map<String, String> modalidadesDisponibles() {
        Map<String, String> modalidades = new LinkedHashMap<>();
        reglas.forEach((clave, regla) -> modalidades.put(clave, regla.descripcion()));
        return Collections.unmodifiableMap(modalidades);
    }
}
