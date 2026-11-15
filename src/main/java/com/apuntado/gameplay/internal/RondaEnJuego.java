package com.apuntado.gameplay.internal;

import com.apuntado.deck.api.Reparto;
import com.apuntado.shared.Carta;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado vivo de una ronda: mazo, pozo, manos y turno. Solo se modifica desde la
 * cola de su partida, por eso no necesita sincronizacion propia.
 */
final class RondaEnJuego {

    enum Fase { ROBAR, DESCARTAR }

    private final int numero;
    private final int rondaId;
    private final List<Integer> orden;
    private final Map<Integer, List<Carta>> manos = new LinkedHashMap<>();
    private final Deque<Carta> mazo;
    private final Deque<Carta> pozo = new ArrayDeque<>();
    private int posicionTurno;
    private Fase fase = Fase.DESCARTAR;

    /** Quien abre recibe 11 cartas, asi que empieza descartando (RN-04). */
    RondaEnJuego(int numero, int rondaId, List<Integer> orden, int posicionAbridor, Reparto reparto) {
        this.numero = numero;
        this.rondaId = rondaId;
        this.orden = List.copyOf(orden);
        for (int i = 0; i < orden.size(); i++) {
            manos.put(orden.get(i), new ArrayList<>(reparto.manos().get(i)));
        }
        this.mazo = new ArrayDeque<>(reparto.mazoRestante());
        this.posicionTurno = posicionAbridor;
    }

    int numero() {
        return numero;
    }

    int rondaId() {
        return rondaId;
    }

    Fase fase() {
        return fase;
    }

    int jugadorEnTurno() {
        return orden.get(posicionTurno);
    }

    List<Integer> orden() {
        return orden;
    }

    boolean participa(int jugadorId) {
        return manos.containsKey(jugadorId);
    }

    void exigirTurno(int jugadorId, Fase esperada) {
        if (!participa(jugadorId)) {
            throw new AccionInvalida("No participas en esta ronda");
        }
        if (jugadorEnTurno() != jugadorId) {
            throw new AccionInvalida("Espera tu turno");
        }
        if (fase != esperada) {
            throw new AccionInvalida(esperada == Fase.ROBAR
                    ? "Ya robaste: ahora descarta una carta o intenta cerrar"
                    : "Primero roba una carta del mazo o del pozo");
        }
    }

    List<Carta> manoDe(int jugadorId) {
        return List.copyOf(manos.getOrDefault(jugadorId, List.of()));
    }

    int cartasDe(int jugadorId) {
        return manos.getOrDefault(jugadorId, List.of()).size();
    }

    boolean mazoVacio() {
        return mazo.isEmpty();
    }

    Carta robarDelMazo(int jugadorId) {
        Carta carta = mazo.pop();
        manos.get(jugadorId).add(carta);
        fase = Fase.DESCARTAR;
        return carta;
    }

    Carta tomarDelPozo(int jugadorId) {
        if (pozo.isEmpty()) {
            throw new AccionInvalida("El pozo está vacío: roba del mazo");
        }
        Carta carta = pozo.pop();
        manos.get(jugadorId).add(carta);
        fase = Fase.DESCARTAR;
        return carta;
    }

    /** Descarta y pasa el turno al siguiente jugador (RN-08). */
    void descartar(int jugadorId, Carta carta) {
        quitarDeMano(jugadorId, carta);
        pozo.push(carta);
        posicionTurno = (posicionTurno + 1) % orden.size();
        fase = Fase.ROBAR;
    }

    /** Al cerrar, la carta elegida va al pozo pero el turno no avanza: la ronda termino. */
    void descartarAlCerrar(int jugadorId, Carta carta) {
        quitarDeMano(jugadorId, carta);
        pozo.push(carta);
    }

    List<Carta> manoSin(int jugadorId, Carta carta) {
        List<Carta> copia = new ArrayList<>(manos.get(jugadorId));
        if (!copia.remove(carta)) {
            throw new AccionInvalida("Esa carta no está en tu mano");
        }
        return copia;
    }

    void reordenar(int jugadorId, List<Carta> nuevoOrden) {
        List<Carta> actual = manos.get(jugadorId);
        if (actual == null || !mismasCartas(actual, nuevoOrden)) {
            throw new AccionInvalida("El nuevo orden no coincide con las cartas de tu mano");
        }
        actual.clear();
        actual.addAll(nuevoOrden);
    }

    /** RF-08: todo el pozo menos la carta de encima pasa a ser el nuevo mazo. */
    List<Carta> retirarPozoSinTope() {
        Carta tope = pozo.pollFirst();
        List<Carta> resto = new ArrayList<>(pozo);
        pozo.clear();
        if (tope != null) {
            pozo.push(tope);
        }
        return resto;
    }

    void reponerMazo(List<Carta> cartas) {
        mazo.addAll(cartas);
    }

    Carta topePozo() {
        return pozo.peek();
    }

    int cartasEnMazo() {
        return mazo.size();
    }

    int cartasEnPozo() {
        return pozo.size();
    }

    private void quitarDeMano(int jugadorId, Carta carta) {
        if (!manos.get(jugadorId).remove(carta)) {
            throw new AccionInvalida("Esa carta no está en tu mano");
        }
    }

    private static boolean mismasCartas(List<Carta> a, List<Carta> b) {
        if (a.size() != b.size()) {
            return false;
        }
        Map<Carta, Integer> conteo = new HashMap<>();
        a.forEach(c -> conteo.merge(c, 1, Integer::sum));
        b.forEach(c -> conteo.merge(c, -1, Integer::sum));
        return conteo.values().stream().allMatch(v -> v == 0);
    }
}
