package com.apuntado.session.api;

import java.util.Map;

/** SessionOrchestrator: crear la sala, incorporar jugadores y arrancar (RF-01 a RF-04). */
public interface IGestionPartida {

    IngresoSala crearSala(SolicitudSala solicitud);

    IngresoSala unirse(String codigo, String nombre);

    EstadoSala estadoSala(String codigo);

    /** Solo quien creo la sala puede arrancar antes de que se llene el cupo. */
    EstadoSala iniciar(String codigo, int jugadorId, String token);

    Map<String, String> modalidades();
}
