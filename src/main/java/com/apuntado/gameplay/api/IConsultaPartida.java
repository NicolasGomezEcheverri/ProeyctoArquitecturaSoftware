package com.apuntado.gameplay.api;

import java.util.List;
import java.util.Optional;

/** Consultas de solo lectura: estado publico, bitacora y metricas de la validacion. */
public interface IConsultaPartida {

    Optional<EstadoPublico> estadoPublico(String codigo);

    List<RegistroBitacora> bitacora(String codigo);

    List<MetricasParticipante> metricas(String codigo);
}
