package com.apuntado.gameplay.api;

import com.apuntado.shared.Carta;

/**
 * Ultima jugada de la mesa, en forma estructurada para que el navegador pueda
 * animarla. La carta solo viaja si es publica (la del pozo o la descartada);
 * la que se roba del mazo nunca se informa a los demas (RN-01).
 *
 * @param tipo RONDA_NUEVA, ROBO_MAZO, ROBO_POZO, DESCARTE o CIERRE
 */
public record Movimiento(long secuencia, String tipo, Integer jugadorId, Carta carta) {
}
