package com.apuntado.scoring.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.apuntado.scoring.api.ResultadoLiquidacion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Deteccion exacta del umbral: 100 sigue en juego, 101 queda eliminado (RN-09). */
class PuntuacionServicioTest {

    private PuntajeRepository repositorio;
    private PuntuacionServicio servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(PuntajeRepository.class);
        servicio = new PuntuacionServicio(repositorio);
        servicio.abrirMarcador(1, List.of(10, 20, 30), 101);
    }

    @Test
    @DisplayName("Acumula las penalizaciones ronda tras ronda")
    void acumula() {
        servicio.liquidarRonda(1, 100, Map.of(10, 0, 20, 35, 30, 12));
        servicio.liquidarRonda(1, 101, Map.of(10, 4, 20, 20, 30, 0));
        assertThat(servicio.consultarMarcador(1).acumulados()).containsEntry(10, 4).containsEntry(20, 55)
                .containsEntry(30, 12);
        verify(repositorio, times(6)).save(any());
    }

    @Test
    @DisplayName("Con 100 puntos el jugador sigue; con 101 exactos queda eliminado")
    void umbralExacto() {
        servicio.liquidarRonda(1, 100, Map.of(10, 0, 20, 100, 30, 100));
        ResultadoLiquidacion r = servicio.liquidarRonda(1, 101, Map.of(10, 0, 20, 1, 30, 0));
        assertThat(r.eliminadosEnEstaRonda()).containsExactly(20);
        assertThat(r.marcador().estaEliminado(30)).isFalse();
        assertThat(r.marcador().activos()).containsExactly(10, 30);
    }

    @Test
    @DisplayName("Un eliminado no se vuelve a reportar como eliminado en rondas siguientes")
    void eliminadoUnaSolaVez() {
        servicio.liquidarRonda(1, 100, Map.of(10, 0, 20, 120, 30, 3));
        ResultadoLiquidacion r = servicio.liquidarRonda(1, 101, Map.of(10, 0, 30, 4));
        assertThat(r.eliminadosEnEstaRonda()).isEmpty();
        assertThat(r.marcador().eliminados()).containsExactly(20);
    }
}
