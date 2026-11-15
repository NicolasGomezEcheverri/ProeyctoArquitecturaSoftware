package com.apuntado.gameplay.api;

import com.apuntado.shared.Carta;
import java.util.List;

/** Acciones que un jugador puede pedir durante la partida (RF-09 a RF-13). */
public interface IAccionesJugador {

    ResultadoAccion robarDelMazo(Credencial credencial, String accionId);

    ResultadoAccion tomarDelPozo(Credencial credencial, String accionId);

    ResultadoAccion descartar(Credencial credencial, Carta carta, String accionId);

    /** Pide cerrar la ronda descartando la carta indicada (RF-13, RF-14). */
    ResultadoAccion solicitarCierre(Credencial credencial, Carta descarte, String accionId);

    ResultadoAccion reordenarMano(Credencial credencial, List<Carta> nuevoOrden);

    ResultadoAccion siguienteRonda(Credencial credencial);

    /** Reenvia el estado de la mesa y la mano del jugador (al conectar o reconectar). */
    ResultadoAccion sincronizar(Credencial credencial);

    /** Latencia medida en el navegador entre enviar una accion y ver la mesa actualizada. */
    void reportarLatencia(Credencial credencial, long milisegundos);
}
