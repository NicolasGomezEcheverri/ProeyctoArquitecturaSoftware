package com.apuntado.presentation;

import com.apuntado.gameplay.api.EstadoPublico;
import com.apuntado.gameplay.api.IConsultaPartida;
import com.apuntado.gameplay.api.MetricasParticipante;
import com.apuntado.gameplay.api.RegistroBitacora;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultas de solo lectura de una partida: estado publico, bitacora de auditoria y
 * metricas. Son la evidencia de la validacion con usuarios (seccion 3).
 */
@RestController
@RequestMapping("/api/partidas/{codigo}")
class PartidaController {

    private static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

    private final IConsultaPartida consultas;
    private final ExportadorCsv csv = new ExportadorCsv();

    PartidaController(IConsultaPartida consultas) {
        this.consultas = consultas;
    }

    @GetMapping("/estado")
    ResponseEntity<EstadoPublico> estado(@PathVariable String codigo) {
        return ResponseEntity.of(consultas.estadoPublico(codigo.toUpperCase()));
    }

    @GetMapping("/bitacora")
    List<RegistroBitacora> bitacora(@PathVariable String codigo) {
        return consultas.bitacora(codigo.toUpperCase());
    }

    @GetMapping("/bitacora.csv")
    ResponseEntity<String> bitacoraCsv(@PathVariable String codigo) {
        return archivo("bitacora-" + codigo + ".csv", csv.bitacora(consultas.bitacora(codigo.toUpperCase())));
    }

    @GetMapping("/metricas")
    List<MetricasParticipante> metricas(@PathVariable String codigo) {
        return consultas.metricas(codigo.toUpperCase());
    }

    @GetMapping("/metricas.csv")
    ResponseEntity<String> metricasCsv(@PathVariable String codigo) {
        return archivo("metricas-" + codigo + ".csv", csv.metricas(consultas.metricas(codigo.toUpperCase())));
    }

    private ResponseEntity<String> archivo(String nombre, String contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(CSV)
                .body(contenido);
    }
}
