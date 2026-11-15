package com.apuntado.integracion;

import static org.assertj.core.api.Assertions.assertThat;

import com.apuntado.app.ApuntadoApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * RN-01 por el canal real: cada navegador recibe el estado publico y solo su propia
 * mano. Se conecta un cliente WebSocket por jugador, como harian dos pestanas.
 */
@SpringBootTest(classes = ApuntadoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ManosPrivadasWebSocketIT {

    @LocalServerPort private int puerto;
    @Autowired private TestRestTemplate http;
    @Autowired private ObjectMapper json;

    @Test
    @DisplayName("Cada jugador recibe solo su mano; el estado público no trae cartas de nadie")
    void manosPrivadas() throws Exception {
        Map<?, ?> ana = http.postForObject("/api/salas",
                Map.of("nombre", "Ana", "modalidad", "APUNTADO", "jugadores", 2, "umbral", 101), Map.class);
        String codigo = (String) ana.get("codigo");
        Map<?, ?> beto = http.postForObject("/api/salas/" + codigo + "/jugadores", Map.of("nombre", "Beto"), Map.class);

        List<JsonNode> paraAna = conectar(codigo, ana);
        List<JsonNode> paraBeto = conectar(codigo, beto);
        esperarMano(paraAna);
        esperarMano(paraBeto);

        Set<String> cartasAna = cartas(ultimaMano(paraAna));
        Set<String> cartasBeto = cartas(ultimaMano(paraBeto));
        assertThat(cartasAna.size() + cartasBeto.size()).isEqualTo(21);
        assertThat(cartasAna).doesNotContainAnyElementsOf(cartasBeto);

        for (JsonNode m : paraAna) {
            if ("ESTADO".equals(m.get("tipo").asText())) {
                assertThat(m.get("estado").has("cartas")).isFalse();
                m.get("estado").get("jugadores").forEach(j -> assertThat(j.has("mano")).isFalse());
            }
        }
    }

    private List<JsonNode> conectar(String codigo, Map<?, ?> ingreso) throws Exception {
        List<JsonNode> recibidos = new CopyOnWriteArrayList<>();
        URI uri = URI.create("ws://localhost:" + puerto + "/ws/juego?codigo=" + codigo + "&jugador="
                + ingreso.get("jugadorId") + "&token=" + ingreso.get("token"));
        WebSocketSession sesion = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage mensaje) throws Exception {
                recibidos.add(json.readTree(mensaje.getPayload()));
            }
        }, null, uri).get(5, TimeUnit.SECONDS);
        assertThat(sesion.isOpen()).isTrue();
        return recibidos;
    }

    private void esperarMano(List<JsonNode> recibidos) throws InterruptedException {
        for (int i = 0; i < 50 && ultimaMano(recibidos) == null; i++) {
            Thread.sleep(100);
        }
        assertThat(ultimaMano(recibidos)).isNotNull();
    }

    private JsonNode ultimaMano(List<JsonNode> recibidos) {
        JsonNode ultima = null;
        for (JsonNode m : recibidos) {
            if ("MANO".equals(m.get("tipo").asText())) {
                ultima = m;
            }
        }
        return ultima;
    }

    private Set<String> cartas(JsonNode mano) {
        Set<String> resultado = new HashSet<>();
        mano.get("cartas").forEach(c -> resultado.add(c.get("valor").asText() + c.get("palo").asText()));
        return resultado;
    }
}
