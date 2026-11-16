package com.apuntado.gameplay.internal;

import com.apuntado.history.api.EventoJuego;
import com.apuntado.history.api.IHistorial;
import com.apuntado.history.api.TipoEvento;
import com.apuntado.scoring.api.IPuntuacion;
import com.apuntado.scoring.api.Marcador;
import com.apuntado.scoring.api.ResultadoLiquidacion;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierre y liquidacion de una ronda como una sola transaccion ACID (seccion 2.4
 * del Entregable 2). O se guardan juntos el cierre, los puntajes, los eventos y
 * el fin de partida, o no se guarda nada.
 */
@Service
class CierreRondaTransaccional {

    private final PartidaRepository partidas;
    private final RondaRepository rondas;
    private final IPuntuacion puntuacion;
    private final IHistorial historial;

    CierreRondaTransaccional(PartidaRepository partidas, RondaRepository rondas,
            IPuntuacion puntuacion, IHistorial historial) {
        this.partidas = partidas;
        this.rondas = rondas;
        this.puntuacion = puntuacion;
        this.historial = historial;
    }

    @Transactional
    public ResultadoCierreRonda registrar(DatosCierre datos) {
        PartidaEntity partida = partidas.bloquearPorId(datos.partidaId())
                .orElseThrow(() -> new IllegalStateException("No existe la partida " + datos.partidaId()));
        RondaEntity ronda = rondas.findById(datos.rondaId())
                .orElseThrow(() -> new IllegalStateException("No existe la ronda " + datos.rondaId()));
        if (ronda.estaCerrada()) {
            throw new AccionInvalida("Esta ronda ya fue cerrada");
        }
        ronda.cerrar(datos.modalidad(), datos.cerradorId());
        ResultadoLiquidacion liquidacion =
                puntuacion.liquidarRonda(datos.partidaId(), datos.rondaId(), datos.penalizaciones());
        registrarEventos(datos, liquidacion);
        Integer ganador = determinarGanador(liquidacion.marcador(), datos.cerradorId());
        if (ganador != null) {
            partida.finalizar(ganador);
            historial.registrarEvento(EventoJuego.de(datos.partidaId(), datos.ronda(), datos.rondaId(), ganador,
                    TipoEvento.PARTIDA_FINALIZADA, "Gana la partida"));
        }
        return new ResultadoCierreRonda(liquidacion, ganador);
    }

    private void registrarEventos(DatosCierre datos, ResultadoLiquidacion liquidacion) {
        historial.registrarEvento(EventoJuego.de(datos.partidaId(), datos.ronda(), datos.rondaId(),
                datos.cerradorId(), TipoEvento.CIERRE_ACEPTADO,
                "Descarta " + datos.descarte().texto() + "; penalizaciones " + datos.penalizaciones()));
        for (Integer eliminado : liquidacion.eliminadosEnEstaRonda()) {
            historial.registrarEvento(EventoJuego.de(datos.partidaId(), datos.ronda(), datos.rondaId(), eliminado,
                    TipoEvento.JUGADOR_ELIMINADO,
                    "Acumula " + liquidacion.marcador().acumuladoDe(eliminado) + " puntos"));
        }
    }

    /**
     * RN-12: la partida termina cuando queda un solo jugador. Si en la misma ronda
     * caen todos, gana el de menor acumulado (y en empate, quien cerro).
     */
    static Integer determinarGanador(Marcador marcador, int cerradorId) {
        List<Integer> activos = marcador.activos();
        if (activos.size() > 1) {
            return null;
        }
        if (activos.size() == 1) {
            return activos.get(0);
        }
        return marcador.acumulados().keySet().stream()
                .min(Comparator.comparingInt(marcador::acumuladoDe)
                        .thenComparing(id -> id != cerradorId))
                .orElse(cerradorId);
    }
}
