package com.apuntado.gameplay.api;

/** Fila de la tabla consolidada de metricas de la seccion 3.3. */
public record MetricasParticipante(int jugadorId, String nombre, String modalidad, int turnos,
        double segundosPromedioTurno, int cierresIntentados, int cierresAceptados, int cierresRechazados,
        int accionesRechazadas, Long latenciaPromedioMs, Long latenciaP95Ms, int muestrasLatencia,
        int acumulado, boolean eliminado) {
}
