package com.apuntado.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.apuntado.deck.api.Reparto;
import com.apuntado.deck.internal.MazoServicio;
import com.apuntado.shared.Carta;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MazoServicioTest {

    private final MazoServicio mazo = new MazoServicio();

    @Test
    @DisplayName("La baraja tiene 52 cartas distintas")
    void barajaCompleta() {
        List<Carta> cartas = mazo.generarYBarajar(new Random(7));
        assertThat(cartas).hasSize(52);
        assertThat(new HashSet<>(cartas)).hasSize(52);
    }

    @Test
    @DisplayName("Con la misma semilla se obtiene el mismo orden (partidas reproducibles en pruebas)")
    void barajadoReproducible() {
        assertThat(mazo.generarYBarajar(new Random(3))).isEqualTo(mazo.generarYBarajar(new Random(3)));
    }

    @Test
    @DisplayName("RN-04: diez cartas por jugador y once para quien abre")
    void repartoInicial() {
        Reparto reparto = mazo.repartir(mazo.generarYBarajar(new Random(1)), 4, 2);
        assertThat(reparto.manos()).extracting(List::size).containsExactly(10, 10, 11, 10);
        assertThat(reparto.mazoRestante()).hasSize(52 - 41);
    }

    @Test
    void noAlcanzanLasCartas() {
        assertThatThrownBy(() -> mazo.repartir(mazo.generarYBarajar(new Random(1)), 6, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-08: el pozo se convierte en mazo sin perder cartas")
    void reconstruirDesdePozo() {
        List<Carta> pozo = mazo.generarYBarajar(new Random(9)).subList(0, 12);
        assertThat(mazo.reconstruirDesdePozo(pozo, new Random(2))).containsExactlyInAnyOrderElementsOf(pozo);
    }
}
