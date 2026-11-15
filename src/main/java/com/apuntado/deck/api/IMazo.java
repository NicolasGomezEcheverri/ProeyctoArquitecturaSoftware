package com.apuntado.deck.api;

import com.apuntado.shared.Carta;
import java.util.List;
import java.util.random.RandomGenerator;

/** DeckManagement: ciclo de vida del mazo y del pozo (RF-05, RF-06, RF-08). */
public interface IMazo {

    /** Baraja de 52 cartas (sin comodines) en orden aleatorio. */
    List<Carta> generarYBarajar(RandomGenerator aleatorio);

    /**
     * RN-04: diez cartas por jugador y una mas para quien abre.
     * El mazo recibido se consume desde el principio.
     */
    Reparto repartir(List<Carta> mazo, int jugadores, int posicionAbridor);

    /** Arma un mazo nuevo con el pozo, dejando la carta de encima en la mesa. */
    List<Carta> reconstruirDesdePozo(List<Carta> pozoSinTope, RandomGenerator aleatorio);
}
