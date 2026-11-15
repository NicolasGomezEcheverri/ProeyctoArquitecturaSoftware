package com.apuntado.gameplay.internal;

import com.apuntado.gameplay.api.EstadoPublico;
import com.apuntado.gameplay.api.INotificadorEstado;
import com.apuntado.gameplay.api.JugadorPublico;
import com.apuntado.gameplay.api.Participante;
import com.apuntado.scoring.api.IPuntuacion;
import com.apuntado.scoring.api.Marcador;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * HandVisibility (RF-07, RN-01): arma el estado publico, que no contiene ninguna
 * mano, y entrega a cada jugador solo la suya.
 */
@Component
class HandVisibility {

    private final IPuntuacion puntuacion;
    private final INotificadorEstado notificador;

    HandVisibility(IPuntuacion puntuacion, INotificadorEstado notificador) {
        this.puntuacion = puntuacion;
        this.notificador = notificador;
    }

    void difundir(Mesa mesa) {
        notificador.publicarEstadoPublico(mesa.codigo(), estadoPublico(mesa));
        enviarManos(mesa);
    }

    void enviarManos(Mesa mesa) {
        RondaEnJuego ronda = mesa.ronda();
        for (Participante participante : mesa.participantes()) {
            notificador.enviarManoPrivada(mesa.codigo(), participante.jugadorId(),
                    ronda.manoDe(participante.jugadorId()));
        }
    }

    /** Solo se llama despues de repartir la primera ronda, asi que siempre hay ronda. */
    EstadoPublico estadoPublico(Mesa mesa) {
        Marcador marcador = puntuacion.consultarMarcador(mesa.partidaId());
        RondaEnJuego ronda = mesa.ronda();
        List<JugadorPublico> jugadores = mesa.participantes().stream()
                .map(p -> new JugadorPublico(p.jugadorId(), p.nombre(), ronda.cartasDe(p.jugadorId()),
                        marcador.acumuladoDe(p.jugadorId()), marcador.estaEliminado(p.jugadorId())))
                .toList();
        boolean enRonda = mesa.estado() == Mesa.Estado.EN_RONDA;
        return new EstadoPublico(mesa.codigo(), mesa.regla().modalidad(), mesa.regla().descripcion(),
                mesa.umbral(), mesa.estado().name(), ronda.numero(),
                enRonda ? ronda.fase().name() : null, enRonda ? ronda.jugadorEnTurno() : null, jugadores,
                ronda.cartasEnMazo(), ronda.topePozo(), ronda.cartasEnPozo(), mesa.ultimoCierre(),
                mesa.ganadorId(), mesa.secuencia(), mesa.ultimaAccionId(), mesa.ultimoMovimiento());
    }
}
