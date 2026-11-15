package com.apuntado.presentation;

import com.apuntado.gameplay.api.MetricasParticipante;
import com.apuntado.gameplay.api.RegistroBitacora;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** CSV con separador ";" para que Excel en espanol lo abra sin preguntar. */
final class ExportadorCsv {

    String bitacora(List<RegistroBitacora> registros) {
        StringBuilder sb = new StringBuilder("fecha;ronda;jugador_id;jugador;tipo;detalle;valor_ms\n");
        for (RegistroBitacora r : registros) {
            sb.append(fila(r.ocurridoEn(), r.ronda(), r.jugadorId(), r.jugador(), r.tipo(), r.detalle(), r.valorMs()));
        }
        return sb.toString();
    }

    String metricas(List<MetricasParticipante> filas) {
        StringBuilder sb = new StringBuilder("jugador_id;jugador;modalidad;turnos;seg_promedio_turno;"
                + "cierres_intentados;cierres_aceptados;cierres_rechazados;acciones_rechazadas;"
                + "latencia_promedio_ms;latencia_p95_ms;muestras_latencia;acumulado;eliminado\n");
        for (MetricasParticipante m : filas) {
            sb.append(fila(m.jugadorId(), m.nombre(), m.modalidad(), m.turnos(), m.segundosPromedioTurno(),
                    m.cierresIntentados(), m.cierresAceptados(), m.cierresRechazados(), m.accionesRechazadas(),
                    m.latenciaPromedioMs(), m.latenciaP95Ms(), m.muestrasLatencia(), m.acumulado(), m.eliminado()));
        }
        return sb.toString();
    }

    private static String fila(Object... valores) {
        return Stream.of(valores)
                .map(v -> Objects.toString(v, "").replace(";", ",").replace("\n", " "))
                .collect(Collectors.joining(";")) + "\n";
    }
}
