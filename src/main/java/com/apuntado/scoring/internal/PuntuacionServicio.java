package com.apuntado.scoring.internal;

import com.apuntado.scoring.api.IPuntuacion;
import com.apuntado.scoring.api.Marcador;
import com.apuntado.scoring.api.ResultadoLiquidacion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Scoreboard. Escribe cada liquidacion en puntaje_acumulado y mantiene una copia
 * en memoria para responder rapido. La copia solo se actualiza cuando la
 * transaccion del cierre confirma: si la base hace rollback, el marcador no cambia.
 */
@Service
public class PuntuacionServicio implements IPuntuacion {

    private final PuntajeRepository repositorio;
    private final EvaluadorEliminacion evaluadorEliminacion = new EvaluadorEliminacion();
    private final Map<Integer, Marcador> marcadores = new ConcurrentHashMap<>();

    public PuntuacionServicio(PuntajeRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void abrirMarcador(int partidaId, List<Integer> jugadores, int umbral) {
        Map<Integer, Integer> ceros = new LinkedHashMap<>();
        jugadores.forEach(id -> ceros.put(id, 0));
        marcadores.put(partidaId, new Marcador(umbral, ceros, Set.of()));
    }

    @Override
    @Transactional
    public ResultadoLiquidacion liquidarRonda(int partidaId, int rondaId, Map<Integer, Integer> penalizaciones) {
        Marcador actual = consultarMarcador(partidaId);
        Map<Integer, Integer> acumulados = new LinkedHashMap<>(actual.acumulados());
        Set<Integer> eliminados = new HashSet<>(actual.eliminados());
        List<Integer> nuevosEliminados = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entrada : penalizaciones.entrySet()) {
            int jugadorId = entrada.getKey();
            int acumulado = actual.acumuladoDe(jugadorId) + entrada.getValue();
            repositorio.save(new PuntajeAcumuladoEntity(rondaId, jugadorId, entrada.getValue(), acumulado));
            acumulados.put(jugadorId, acumulado);
            if (evaluadorEliminacion.quedaEliminado(acumulado, actual.umbral()) && eliminados.add(jugadorId)) {
                nuevosEliminados.add(jugadorId);
            }
        }
        repositorio.flush();
        Marcador nuevo = new Marcador(actual.umbral(), acumulados, eliminados);
        publicarAlConfirmar(partidaId, nuevo);
        return new ResultadoLiquidacion(nuevo, nuevosEliminados);
    }

    @Override
    public Marcador consultarMarcador(int partidaId) {
        Marcador marcador = marcadores.get(partidaId);
        if (marcador == null) {
            throw new IllegalStateException("La partida " + partidaId + " no tiene marcador abierto");
        }
        return marcador;
    }

    private void publicarAlConfirmar(int partidaId, Marcador nuevo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            marcadores.put(partidaId, nuevo);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                marcadores.put(partidaId, nuevo);
            }
        });
    }
}
