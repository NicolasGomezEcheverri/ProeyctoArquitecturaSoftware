package com.apuntado.gameplay.api;

import com.apuntado.shared.Carta;
import java.util.List;

/**
 * Declarada por gameplay e implementada por presentation (inversion de dependencias, ADR-05).
 * El estado publico no incluye ninguna mano; cada mano viaja solo a su dueno (RN-01).
 */
public interface INotificadorEstado {

    void publicarEstadoPublico(String codigo, EstadoPublico estado);

    void enviarManoPrivada(String codigo, int jugadorId, List<Carta> mano);
}
