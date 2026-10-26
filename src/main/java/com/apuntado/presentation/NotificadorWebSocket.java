package com.apuntado.presentation;

import com.apuntado.gameplay.api.INotificadorEstado;

/** Esqueleto: en el MVP publica el estado por WebSockets a los 4 clientes. */
public class NotificadorWebSocket implements INotificadorEstado {

    @Override
    public void publicarEstadoPublico(String estado) {
        System.out.println("[WS] " + estado);
    }
}
