package com.apuntado.presentation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/** Conexiones abiertas, por sala y por jugador. */
@Component
class SesionesConectadas {

    private static final int TIEMPO_ENVIO_MS = 5_000;
    private static final int BUFFER_BYTES = 512 * 1024;

    private final Map<String, Map<Integer, WebSocketSession>> porSala = new ConcurrentHashMap<>();

    WebSocketSession registrar(String codigo, int jugadorId, WebSocketSession sesion) {
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(sesion, TIEMPO_ENVIO_MS, BUFFER_BYTES);
        porSala.computeIfAbsent(codigo, c -> new ConcurrentHashMap<>()).put(jugadorId, segura);
        return segura;
    }

    void quitar(String codigo, int jugadorId, WebSocketSession sesion) {
        Map<Integer, WebSocketSession> sala = porSala.get(codigo);
        if (sala != null) {
            sala.computeIfPresent(jugadorId, (id, actual) -> actual.getId().equals(sesion.getId()) ? null : actual);
        }
    }

    Map<Integer, WebSocketSession> deSala(String codigo) {
        return porSala.getOrDefault(codigo, Map.of());
    }
}
