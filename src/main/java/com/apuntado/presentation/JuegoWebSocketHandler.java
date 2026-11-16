package com.apuntado.presentation;

import com.apuntado.gameplay.api.Credencial;
import com.apuntado.gameplay.api.IAccionesJugador;
import com.apuntado.gameplay.api.ResultadoAccion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Canal en tiempo real con cada navegador. Traduce los mensajes JSON a llamadas
 * a IAccionesJugador y le responde solo a quien hizo la accion.
 */
@Component
class JuegoWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JuegoWebSocketHandler.class);
    private static final String CREDENCIAL = "credencial";

    private final IAccionesJugador acciones;
    private final SesionesConectadas sesiones;
    private final ObjectMapper json;
    private final Map<String, BiFunction<Credencial, MensajeCliente, ResultadoAccion>> despacho;

    JuegoWebSocketHandler(IAccionesJugador acciones, SesionesConectadas sesiones, ObjectMapper json) {
        this.acciones = acciones;
        this.sesiones = sesiones;
        this.json = json;
        this.despacho = crearDespacho();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) throws IOException {
        Map<String, String> parametros = UriComponentsBuilder.fromUri(sesion.getUri()).build()
                .getQueryParams().toSingleValueMap();
        Credencial credencial;
        try {
            credencial = new Credencial(parametros.get("codigo"), Integer.parseInt(parametros.get("jugador")),
                    parametros.get("token"));
        } catch (NumberFormatException e) {
            sesion.close(CloseStatus.BAD_DATA);
            return;
        }
        sesion.getAttributes().put(CREDENCIAL, credencial);
        WebSocketSession segura = sesiones.registrar(credencial.codigo(), credencial.jugadorId(), sesion);
        ResultadoAccion resultado = acciones.sincronizar(credencial);
        if (!resultado.aceptada()) {
            responder(segura, null, resultado);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession sesion, TextMessage texto) throws IOException {
        Credencial credencial = (Credencial) sesion.getAttributes().get(CREDENCIAL);
        WebSocketSession segura = sesiones.deSala(credencial.codigo()).getOrDefault(credencial.jugadorId(), sesion);
        MensajeCliente mensaje;
        try {
            mensaje = json.readValue(texto.getPayload(), MensajeCliente.class);
        } catch (JsonProcessingException e) {
            responder(segura, null, ResultadoAccion.rechazo("Mensaje no válido"));
            return;
        }
        ResultadoAccion resultado = despachar(credencial, mensaje);
        if (resultado != null) {
            responder(segura, mensaje.accionId(), resultado);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesion, CloseStatus estado) {
        Credencial credencial = (Credencial) sesion.getAttributes().get(CREDENCIAL);
        if (credencial != null) {
            sesiones.quitar(credencial.codigo(), credencial.jugadorId(), sesion);
        }
    }

    /** Tabla de despacho: cada tipo de mensaje se traduce en una accion de gameplay. */
    private ResultadoAccion despachar(Credencial c, MensajeCliente m) {
        BiFunction<Credencial, MensajeCliente, ResultadoAccion> accion =
                despacho.get(Objects.requireNonNullElse(m.tipo(), ""));
        return accion == null ? ResultadoAccion.rechazo("Acción desconocida: " + m.tipo()) : accion.apply(c, m);
    }

    private Map<String, BiFunction<Credencial, MensajeCliente, ResultadoAccion>> crearDespacho() {
        return Map.of(
                "ROBAR_MAZO", (c, m) -> acciones.robarDelMazo(c, m.accionId()),
                "TOMAR_POZO", (c, m) -> acciones.tomarDelPozo(c, m.accionId()),
                "DESCARTAR", (c, m) -> acciones.descartar(c, m.carta(), m.accionId()),
                "CERRAR", (c, m) -> acciones.solicitarCierre(c, m.carta(), m.accionId()),
                "ORDENAR", (c, m) -> acciones.reordenarMano(c, m.orden()),
                "SIGUIENTE_RONDA", (c, m) -> acciones.siguienteRonda(c),
                "SINCRONIZAR", (c, m) -> acciones.sincronizar(c),
                "LATENCIA", this::reportarLatencia);
    }

    private ResultadoAccion reportarLatencia(Credencial c, MensajeCliente m) {
        if (m.ms() != null && m.ms() >= 0) {
            acciones.reportarLatencia(c, m.ms());
        }
        return null;
    }

    private void responder(WebSocketSession sesion, String accionId, ResultadoAccion resultado) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("tipo", "RESULTADO");
        respuesta.put("accionId", accionId);
        respuesta.put("aceptada", resultado.aceptada());
        respuesta.put("mensaje", resultado.mensaje());
        try {
            sesion.sendMessage(new TextMessage(json.writeValueAsString(respuesta)));
        } catch (IOException e) {
            LOG.warn("No se pudo responder a la sesion {}: {}", sesion.getId(), e.getMessage());
        }
    }
}
