package com.apuntado.history.api;

import java.time.Instant;

/**
 * Un registro de la bitacora de auditoria.
 *
 * @param ronda    numero de ronda dentro de la partida (null en eventos de partida)
 * @param rondaId  identificador de la fila en la tabla ronda (null si no aplica)
 * @param valorMs  duracion asociada, por ejemplo la latencia medida en el navegador
 */
public record EventoJuego(int partidaId, Integer ronda, Integer rondaId, Integer jugadorId,
        TipoEvento tipo, String detalle, Long valorMs, Instant ocurridoEn) {

    public static EventoJuego de(int partidaId, Integer ronda, Integer rondaId, Integer jugadorId,
            TipoEvento tipo, String detalle) {
        return new EventoJuego(partidaId, ronda, rondaId, jugadorId, tipo, detalle, null, Instant.now());
    }
}
