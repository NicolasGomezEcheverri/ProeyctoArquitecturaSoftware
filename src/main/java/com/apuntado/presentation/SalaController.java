package com.apuntado.presentation;

import com.apuntado.session.api.EstadoSala;
import com.apuntado.session.api.IGestionPartida;
import com.apuntado.session.api.IngresoSala;
import com.apuntado.session.api.SolicitudSala;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API REST de la sala de espera. La partida en si viaja por WebSocket. */
@RestController
@RequestMapping("/api")
class SalaController {

    private final IGestionPartida gestion;

    SalaController(IGestionPartida gestion) {
        this.gestion = gestion;
    }

    @GetMapping("/modalidades")
    Map<String, String> modalidades() {
        return gestion.modalidades();
    }

    @PostMapping("/salas")
    @ResponseStatus(HttpStatus.CREATED)
    IngresoSala crear(@RequestBody SolicitudSala solicitud) {
        return gestion.crearSala(solicitud);
    }

    @PostMapping("/salas/{codigo}/jugadores")
    @ResponseStatus(HttpStatus.CREATED)
    IngresoSala unirse(@PathVariable String codigo, @RequestBody Map<String, String> cuerpo) {
        return gestion.unirse(codigo, cuerpo.get("nombre"));
    }

    @GetMapping("/salas/{codigo}")
    EstadoSala estado(@PathVariable String codigo) {
        return gestion.estadoSala(codigo);
    }

    @PostMapping("/salas/{codigo}/iniciar")
    EstadoSala iniciar(@PathVariable String codigo, @RequestBody Map<String, String> cuerpo) {
        return gestion.iniciar(codigo, Integer.parseInt(cuerpo.get("jugadorId")), cuerpo.get("token"));
    }
}
