package com.apuntado.gameplay.api;

import com.apuntado.shared.Carta;
import java.util.List;

/**
 * Lo que todos ven de la mesa. Las manos solo aparecen como cantidad de cartas.
 *
 * @param fase           ROBAR o DESCARTAR, para el jugador en turno
 * @param ultimaAccionId id que envio el navegador con la ultima accion aceptada;
 *                       con el, ese navegador mide la latencia de ida y vuelta
 * @param movimiento     la ultima jugada, para animarla en la mesa
 * @param abridorId      quien abrio la ronda actual (recibio 11 cartas)
 */
public record EstadoPublico(String codigo, String modalidad, String descripcionModalidad, int umbral,
        String estado, int ronda, String fase, Integer turnoDe, List<JugadorPublico> jugadores,
        int cartasEnMazo, Carta topePozo, int cartasEnPozo, ResumenCierre ultimoCierre,
        Integer ganadorId, long secuencia, String ultimaAccionId, String ultimoMovimiento,
        Movimiento movimiento, Integer abridorId) {

    public EstadoPublico {
        jugadores = List.copyOf(jugadores);
    }
}
