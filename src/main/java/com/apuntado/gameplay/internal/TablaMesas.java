package com.apuntado.gameplay.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Mesas activas en memoria, indexadas por el codigo de la sala. */
@Component
class TablaMesas {

    private final Map<String, Mesa> mesas = new ConcurrentHashMap<>();

    void agregar(Mesa mesa) {
        mesas.put(mesa.codigo(), mesa);
    }

    Mesa obtener(String codigo) {
        Mesa mesa = mesas.get(codigo);
        if (mesa == null) {
            throw new AccionInvalida("No hay una partida en curso con el código " + codigo);
        }
        return mesa;
    }

    Optional<Mesa> buscar(String codigo) {
        return Optional.ofNullable(mesas.get(codigo));
    }
}
