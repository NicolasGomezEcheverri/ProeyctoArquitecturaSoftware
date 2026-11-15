package com.apuntado.gameplay.internal;

import static com.apuntado.soporte.Cartas.c;
import static com.apuntado.soporte.Cartas.lista;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.apuntado.deck.api.Reparto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Reglas de turno (RF-09 a RF-11, RN-08) sobre una ronda de dos jugadores. */
class RondaEnJuegoTest {

    private RondaEnJuego ronda;

    @BeforeEach
    void preparar() {
        Reparto reparto = new Reparto(
                List.of(lista("AP 2P 3P 4P 5P 6P 7P 8P 9P 10P JP"), lista("AC 2C 3C 4C 5C 6C 7C 8C 9C 10C")),
                lista("KD QD JD"));
        ronda = new RondaEnJuego(1, 50, List.of(1, 2), 0, reparto);
    }

    @Test
    @DisplayName("Quien abre empieza con 11 cartas y debe descartar")
    void abridorDescarta() {
        assertThat(ronda.jugadorEnTurno()).isEqualTo(1);
        assertThat(ronda.fase()).isEqualTo(RondaEnJuego.Fase.DESCARTAR);
        assertThatThrownBy(() -> ronda.exigirTurno(1, RondaEnJuego.Fase.ROBAR))
                .hasMessageContaining("Ya robaste");
    }

    @Test
    @DisplayName("Nadie puede jugar fuera de su turno")
    void fueraDeTurno() {
        assertThatThrownBy(() -> ronda.exigirTurno(2, RondaEnJuego.Fase.ROBAR)).hasMessageContaining("Espera tu turno");
    }

    @Test
    @DisplayName("Descartar pasa el turno y el siguiente debe robar")
    void descartarPasaTurno() {
        ronda.descartar(1, c("JP"));
        assertThat(ronda.jugadorEnTurno()).isEqualTo(2);
        assertThat(ronda.fase()).isEqualTo(RondaEnJuego.Fase.ROBAR);
        assertThat(ronda.topePozo()).isEqualTo(c("JP"));
        assertThatThrownBy(() -> ronda.exigirTurno(2, RondaEnJuego.Fase.DESCARTAR))
                .hasMessageContaining("Primero roba");
    }

    @Test
    @DisplayName("Tomar del pozo entrega la carta de encima")
    void tomarDelPozo() {
        ronda.descartar(1, c("JP"));
        assertThat(ronda.tomarDelPozo(2)).isEqualTo(c("JP"));
        assertThat(ronda.cartasDe(2)).isEqualTo(11);
    }

    @Test
    @DisplayName("No se puede descartar una carta que no esta en la mano")
    void cartaAjena() {
        assertThatThrownBy(() -> ronda.descartar(1, c("KD"))).hasMessageContaining("no está en tu mano");
    }

    @Test
    @DisplayName("Reordenar solo acepta las mismas cartas")
    void reordenar() {
        List<com.apuntado.shared.Carta> invertida = new java.util.ArrayList<>(ronda.manoDe(2));
        java.util.Collections.reverse(invertida);
        ronda.reordenar(2, invertida);
        assertThat(ronda.manoDe(2)).isEqualTo(invertida);
        assertThatThrownBy(() -> ronda.reordenar(2, lista("KD QD"))).isInstanceOf(AccionInvalida.class);
    }

    @Test
    @DisplayName("RF-08: al reconstruir, la carta de encima del pozo se queda en la mesa")
    void retirarPozoSinTope() {
        ronda.descartar(1, c("JP"));
        ronda.robarDelMazo(2);
        ronda.descartar(2, c("10C"));
        assertThat(ronda.retirarPozoSinTope()).containsExactly(c("JP"));
        assertThat(ronda.topePozo()).isEqualTo(c("10C"));
        assertThat(ronda.cartasEnPozo()).isEqualTo(1);
    }
}
