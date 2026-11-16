package com.apuntado.presentation;

import com.apuntado.gameplay.api.EstadoPublico;
import com.apuntado.gameplay.api.INotificadorEstado;
import com.apuntado.shared.Carta;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * StatePropagation: implementa la interfaz que declara gameplay (ADR-05). El
 * estado publico va a todos; la mano, solo al socket de su dueno (RN-01).
 */
@Component
class NotificadorWebSocket implements INotificadorEstado {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorWebSocket.class);

    private final SesionesConectadas sesiones;
    private final ObjectMapper json;

    NotificadorWebSocket(SesionesConectadas sesiones, ObjectMapper json) {
        this.sesiones = sesiones;
        this.json = json;
    }

    @Override
    public void publicarEstadoPublico(String codigo, EstadoPublico estado) {
        TextMessage mensaje = serializar(Map.of("tipo", "ESTADO", "estado", estado));
        sesiones.deSala(codigo).values().forEach(sesion -> enviar(sesion, mensaje));
    }

    @Override
    public void enviarManoPrivada(String codigo, int jugadorId, List<Carta> mano) {
        WebSocketSession sesion = sesiones.deSala(codigo).get(jugadorId);
        if (sesion != null) {
            enviar(sesion, serializar(Map.of("tipo", "MANO", "cartas", mano)));
        }
    }

    private TextMessage serializar(Object contenido) {
        try {
            return new TextMessage(json.writeValueAsString(contenido));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el mensaje", e);
        }
    }

    private void enviar(WebSocketSession sesion, TextMessage mensaje) {
        if (!sesion.isOpen()) {
            return;
        }
        try {
            sesion.sendMessage(mensaje);
        } catch (IOException e) {
            LOG.warn("No se pudo enviar a la sesion {}: {}", sesion.getId(), e.getMessage());
        }
    }
}
