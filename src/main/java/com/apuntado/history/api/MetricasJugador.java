package com.apuntado.history.api;

/** Metricas de un jugador calculadas a partir de la bitacora (seccion 3 del Entregable 3). */
public record MetricasJugador(int jugadorId, int turnos, double segundosPromedioTurno,
        int cierresIntentados, int cierresAceptados, int cierresRechazados,
        int accionesRechazadas, Long latenciaPromedioMs, Long latenciaP95Ms, int muestrasLatencia) {
}
