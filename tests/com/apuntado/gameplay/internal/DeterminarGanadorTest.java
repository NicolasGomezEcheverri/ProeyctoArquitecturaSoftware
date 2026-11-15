package com.apuntado.gameplay.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.apuntado.scoring.api.Marcador;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RN-12: la partida termina con un solo ganador y sin empates. */
class DeterminarGanadorTest {

    @Test
    @DisplayName("Sigue la partida mientras haya dos o mas jugadores activos")
    void siguePartida() {
        Marcador m = new Marcador(101, Map.of(1, 40, 2, 101, 3, 70), Set.of(2));
        assertThat(CierreRondaTransaccional.determinarGanador(m, 1)).isNull();
    }

    @Test
    @DisplayName("Gana el unico jugador que no fue eliminado")
    void unicoActivo() {
        Marcador m = new Marcador(101, Map.of(1, 40, 2, 101, 3, 130), Set.of(2, 3));
        assertThat(CierreRondaTransaccional.determinarGanador(m, 3)).isEqualTo(1);
    }

    @Test
    @DisplayName("Si todos caen en la misma ronda gana el de menor puntaje")
    void todosEliminados() {
        Marcador m = new Marcador(101, Map.of(1, 103, 2, 120), Set.of(1, 2));
        assertThat(CierreRondaTransaccional.determinarGanador(m, 2)).isEqualTo(1);
    }

    @Test
    @DisplayName("Empate en la eliminacion: desempata quien cerro")
    void empate() {
        Marcador m = new Marcador(101, Map.of(1, 105, 2, 105), Set.of(1, 2));
        assertThat(CierreRondaTransaccional.determinarGanador(m, 2)).isEqualTo(2);
    }
}
