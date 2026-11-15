package com.apuntado.gameplay.api;

import com.apuntado.shared.Carta;
import java.util.List;

/**
 * Lo que todos ven de la mesa. Las manos solo aparecen como cantidad de cartas.
 *
 * @param fase           ROBAR o DESCARTAR, para el jugador en turno
 * @param ultimaAccionId id que envio el navegador con la ultima accion aceptada;
 *                       con el, ese navegador mide la latencia de ida y vuelta
 */
public record EstadoPublico(String codigo, String modalidad, String descripcionModalidad, int umbral,
        String estado, int ronda, String fase, Integer turnoDe, List<JugadorPublico> jugadores,
        int cartasEnMazo, Carta topePozo, int cartasEnPozo, ResumenCierre ultimoCierre,
        Integer ganadorId, long secuencia, String ultimaAccionId, String ultimoMovimiento) {

    public EstadoPublico {
        jugadores = List.copyOf(jugadores);
    }
}
