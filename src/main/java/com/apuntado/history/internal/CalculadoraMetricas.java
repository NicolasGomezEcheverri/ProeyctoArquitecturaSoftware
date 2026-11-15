package com.apuntado.history.internal;

import com.apuntado.history.api.EventoJuego;
import com.apuntado.history.api.MetricasJugador;
import com.apuntado.history.api.TipoEvento;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte la bitacora en las metricas de la validacion con usuarios:
 * duracion de cada turno, intentos de cierre, acciones rechazadas y latencia.
 */
final class CalculadoraMetricas {

    List<MetricasJugador> calcular(List<EventoJuego> eventos) {
        Map<Integer, Acumulador> porJugador = new LinkedHashMap<>();
        Map<Integer, Instant> turnoAbierto = new HashMap<>();
        for (EventoJuego evento : eventos) {
            if (evento.jugadorId() != null) {
                Acumulador acumulador = porJugador.computeIfAbsent(evento.jugadorId(), id -> new Acumulador());
                acumulador.registrar(evento, turnoAbierto);
            }
        }
        List<MetricasJugador> resultado = new ArrayList<>();
        porJugador.forEach((id, acumulador) -> resultado.add(acumulador.resumen(id)));
        return resultado;
    }

    private static final class Acumulador {
        private final List<Long> duracionesTurno = new ArrayList<>();
        private final List<Long> latencias = new ArrayList<>();
        private int aceptados;
        private int rechazados;
        private int accionesRechazadas;

        void registrar(EventoJuego evento, Map<Integer, Instant> turnoAbierto) {
            TipoEvento tipo = evento.tipo();
            if (tipo == TipoEvento.TURNO_INICIADO) {
                turnoAbierto.put(evento.jugadorId(), evento.ocurridoEn());
            } else if (tipo == TipoEvento.DESCARTE || tipo == TipoEvento.CIERRE_ACEPTADO) {
                cerrarTurno(evento, turnoAbierto.remove(evento.jugadorId()));
            } else if (tipo == TipoEvento.CIERRE_RECHAZADO) {
                rechazados++;
            } else if (tipo == TipoEvento.ACCION_RECHAZADA) {
                accionesRechazadas++;
            } else if (tipo == TipoEvento.LATENCIA_CLIENTE && evento.valorMs() != null) {
                latencias.add(evento.valorMs());
            }
        }

        private void cerrarTurno(EventoJuego evento, Instant inicio) {
            if (evento.tipo() == TipoEvento.CIERRE_ACEPTADO) {
                aceptados++;
            }
            if (inicio != null) {
                duracionesTurno.add(Duration.between(inicio, evento.ocurridoEn()).toMillis());
            }
        }

        MetricasJugador resumen(int jugadorId) {
            double promedioTurno = duracionesTurno.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0;
            Long promedioLatencia = latencias.isEmpty() ? null
                    : Math.round(latencias.stream().mapToLong(Long::longValue).average().orElse(0));
            return new MetricasJugador(jugadorId, duracionesTurno.size(), Math.round(promedioTurno * 10) / 10.0,
                    aceptados + rechazados, aceptados, rechazados, accionesRechazadas,
                    promedioLatencia, percentil95(latencias), latencias.size());
        }

        private static Long percentil95(List<Long> valores) {
            if (valores.isEmpty()) {
                return null;
            }
            List<Long> ordenados = valores.stream().sorted().toList();
            int posicion = (int) Math.ceil(0.95 * ordenados.size()) - 1;
            return ordenados.get(Math.max(posicion, 0));
        }
    }
}
