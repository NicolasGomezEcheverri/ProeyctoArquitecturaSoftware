package com.apuntado.gameplay.internal;

import com.apuntado.gameplay.api.EstadoPublico;
import com.apuntado.gameplay.api.IConsultaPartida;
import com.apuntado.gameplay.api.MetricasParticipante;
import com.apuntado.gameplay.api.RegistroBitacora;
import com.apuntado.history.api.IHistorial;
import com.apuntado.history.api.MetricasJugador;
import com.apuntado.scoring.api.IPuntuacion;
import com.apuntado.scoring.api.Marcador;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Consultas de solo lectura. La bitacora y las metricas salen de history; gameplay
 * solo les agrega los nombres y el marcador, porque es quien conoce la mesa.
 */
@Service
class ConsultaPartida implements IConsultaPartida {

    private final TablaMesas mesas;
    private final HandVisibility visibilidad;
    private final RegistroPartida registroPartida;
    private final IHistorial historial;
    private final IPuntuacion puntuacion;

    ConsultaPartida(TablaMesas mesas, HandVisibility visibilidad, RegistroPartida registroPartida,
            IHistorial historial, IPuntuacion puntuacion) {
        this.mesas = mesas;
        this.visibilidad = visibilidad;
        this.registroPartida = registroPartida;
        this.historial = historial;
        this.puntuacion = puntuacion;
    }

    @Override
    public Optional<EstadoPublico> estadoPublico(String codigo) {
        return mesas.buscar(codigo).filter(m -> m.ronda() != null).map(visibilidad::estadoPublico);
    }

    @Override
    public List<RegistroBitacora> bitacora(String codigo) {
        Integer partidaId = registroPartida.partidaIdDe(codigo);
        if (partidaId == null) {
            return List.of();
        }
        Optional<Mesa> mesa = mesas.buscar(codigo);
        return historial.eventosDePartida(partidaId).stream()
                .map(e -> new RegistroBitacora(e.ocurridoEn(), e.ronda(), e.jugadorId(),
                        mesa.map(m -> m.nombreDe(e.jugadorId())).orElse(String.valueOf(e.jugadorId())),
                        e.tipo().name(), e.detalle(), e.valorMs()))
                .toList();
    }

    @Override
    public List<MetricasParticipante> metricas(String codigo) {
        Integer partidaId = registroPartida.partidaIdDe(codigo);
        Optional<Mesa> mesa = mesas.buscar(codigo);
        if (partidaId == null || mesa.isEmpty()) {
            return List.of();
        }
        Marcador marcador = puntuacion.consultarMarcador(partidaId);
        String modalidad = mesa.get().regla().modalidad();
        return historial.metricas(partidaId).stream()
                .map(m -> fila(m, mesa.get(), modalidad, marcador))
                .toList();
    }

    private MetricasParticipante fila(MetricasJugador m, Mesa mesa, String modalidad, Marcador marcador) {
        return new MetricasParticipante(m.jugadorId(), mesa.nombreDe(m.jugadorId()), modalidad, m.turnos(),
                m.segundosPromedioTurno(), m.cierresIntentados(), m.cierresAceptados(), m.cierresRechazados(),
                m.accionesRechazadas(), m.latenciaPromedioMs(), m.latenciaP95Ms(), m.muestrasLatencia(),
                marcador.acumuladoDe(m.jugadorId()), marcador.estaEliminado(m.jugadorId()));
    }
}
