package com.apuntado.history.internal;

import com.apuntado.history.api.EventoJuego;
import com.apuntado.history.api.IHistorial;
import com.apuntado.history.api.MetricasJugador;
import com.apuntado.history.api.TipoEvento;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escribe la bitacora en evento_auditoria y las jugadas en turno_jugada.
 * Si se llama dentro de la transaccion del cierre, el evento se confirma o se
 * revierte junto con el puntaje.
 */
@Service
public class HistorialServicio implements IHistorial {

    private static final Logger LOG = LoggerFactory.getLogger("auditoria");
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private final EventoAuditoriaRepository eventos;
    private final TurnoJugadaRepository jugadas;
    private final CalculadoraMetricas calculadora = new CalculadoraMetricas();

    public HistorialServicio(EventoAuditoriaRepository eventos, TurnoJugadaRepository jugadas) {
        this.eventos = eventos;
        this.jugadas = jugadas;
    }

    @Override
    @Transactional
    public void registrarEvento(EventoJuego evento) {
        LocalDateTime fecha = LocalDateTime.ofInstant(evento.ocurridoEn(), ZONA);
        eventos.save(new EventoAuditoriaEntity(evento.partidaId(), evento.ronda(), evento.jugadorId(),
                evento.tipo().name(), recortar(evento.detalle(), 255), evento.valorMs(), fecha));
        if (evento.tipo().esJugada() && evento.rondaId() != null && evento.jugadorId() != null) {
            int secuencia = jugadas.ultimaSecuencia(evento.rondaId()) + 1;
            String accion = recortar(evento.tipo().name() + " " + valorODefecto(evento.detalle()), 120);
            jugadas.save(new TurnoJugadaEntity(evento.rondaId(), secuencia, evento.jugadorId(), accion, fecha));
        }
        if (evento.tipo() != TipoEvento.LATENCIA_CLIENTE) {
            LOG.info("partida={} ronda={} jugador={} {} {}", evento.partidaId(), evento.ronda(),
                    evento.jugadorId(), evento.tipo(), valorODefecto(evento.detalle()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoJuego> eventosDePartida(int partidaId) {
        return eventos.findByPartidaIdOrderByIdAsc(partidaId).stream().map(this::aEvento).toList();
    }

    @Override
    public List<MetricasJugador> metricas(int partidaId) {
        return calculadora.calcular(eventosDePartida(partidaId));
    }

    private EventoJuego aEvento(EventoAuditoriaEntity e) {
        return new EventoJuego(e.getPartidaId(), e.getRonda(), null, e.getJugadorId(),
                TipoEvento.valueOf(e.getTipo()), e.getDetalle(), e.getValorMs(),
                e.getOcurridoEn().atZone(ZONA).toInstant());
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo);
    }

    private static String valorODefecto(String texto) {
        return texto == null ? "" : texto;
    }
}
