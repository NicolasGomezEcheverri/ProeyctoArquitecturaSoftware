package com.apuntado.history.internal;

import com.apuntado.history.api.IHistorial;
import java.util.ArrayList;
import java.util.List;

/** Esqueleto en memoria; en el MVP escribe en turno_jugada (PostgreSQL). */
public class HistorialServicio implements IHistorial {

    private final List<String> eventos = new ArrayList<>();

    @Override
    public void registrarEvento(String evento) {
        eventos.add(evento);
    }
}
