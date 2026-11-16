package com.apuntado.deck.internal;

import com.apuntado.deck.api.IMazo;
import com.apuntado.deck.api.Reparto;
import com.apuntado.shared.Carta;
import com.apuntado.shared.Palo;
import com.apuntado.shared.Valor;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public class MazoServicio implements IMazo {

    static final int CARTAS_POR_JUGADOR = 10;

    @Override
    public List<Carta> generarYBarajar(RandomGenerator aleatorio) {
        List<Carta> cartas = new ArrayList<>();
        for (Palo palo : Palo.values()) {
            for (Valor valor : Valor.values()) {
                cartas.add(new Carta(palo, valor));
            }
        }
        barajar(cartas, aleatorio);
        return cartas;
    }

    @Override
    public Reparto repartir(List<Carta> mazo, int jugadores, int posicionAbridor) {
        if (jugadores * CARTAS_POR_JUGADOR + 1 > mazo.size()) {
            throw new IllegalArgumentException("No alcanzan las cartas para " + jugadores + " jugadores");
        }
        List<List<Carta>> manos = new ArrayList<>();
        for (int j = 0; j < jugadores; j++) {
            manos.add(new ArrayList<>());
        }
        int siguiente = 0;
        for (int vuelta = 0; vuelta < CARTAS_POR_JUGADOR; vuelta++) {
            for (List<Carta> mano : manos) {
                mano.add(mazo.get(siguiente++));
            }
        }
        manos.get(posicionAbridor).add(mazo.get(siguiente++));
        return new Reparto(manos, mazo.subList(siguiente, mazo.size()));
    }

    @Override
    public List<Carta> reconstruirDesdePozo(List<Carta> pozoSinTope, RandomGenerator aleatorio) {
        List<Carta> nuevoMazo = new ArrayList<>(pozoSinTope);
        barajar(nuevoMazo, aleatorio);
        return nuevoMazo;
    }

    /** Fisher-Yates con el generador recibido, para poder repetir partidas en las pruebas. */
    private void barajar(List<Carta> cartas, RandomGenerator aleatorio) {
        for (int i = cartas.size() - 1; i > 0; i--) {
            int j = aleatorio.nextInt(i + 1);
            Carta temporal = cartas.get(i);
            cartas.set(i, cartas.get(j));
            cartas.set(j, temporal);
        }
    }
}
