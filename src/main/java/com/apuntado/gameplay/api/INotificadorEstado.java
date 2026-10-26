package com.apuntado.gameplay.api;

/** Declarada por gameplay e implementada por presentation (inversion de dependencias, ADR-05). */
public interface INotificadorEstado {

    void publicarEstadoPublico(String estado);
}
