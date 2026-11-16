package com.apuntado.gameplay.internal;

import com.apuntado.gameplay.api.Participante;
import com.apuntado.gameplay.api.PenalizacionRonda;
import com.apuntado.gameplay.api.ResultadoAccion;
import com.apuntado.gameplay.api.ResumenCierre;
import com.apuntado.history.api.EventoJuego;
import com.apuntado.history.api.IHistorial;
import com.apuntado.history.api.TipoEvento;
import com.apuntado.rules.api.Combinacion;
import com.apuntado.rules.api.IEvaluadorCombinaciones;
import com.apuntado.rules.api.ResultadoCombinaciones;
import com.apuntado.rules.api.ResultadoValidacion;
import com.apuntado.scoring.api.Marcador;
import com.apuntado.shared.Carta;
import com.apuntado.shared.Mano;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * CloseRequest (RF-13 a RF-17): recibe el intento de cierre, se lo pasa a la regla
 * de la modalidad y, si es valido, calcula las penalizaciones y pide el registro
 * transaccional. No sabe si la regla es Clasica o Apuntado: solo usa ReglaJuego.
 */
@Component
class CloseRequest {

    private final IEvaluadorCombinaciones evaluador;
    private final CierreRondaTransaccional cierreTransaccional;
    private final IHistorial historial;

    CloseRequest(IEvaluadorCombinaciones evaluador, CierreRondaTransaccional cierreTransaccional,
            IHistorial historial) {
        this.evaluador = evaluador;
        this.cierreTransaccional = cierreTransaccional;
        this.historial = historial;
    }

    ResultadoAccion procesar(Mesa mesa, Participante cerrador, Carta descarte) {
        mesa.exigirEstado(Mesa.Estado.EN_RONDA, "La ronda ya terminó");
        RondaEnJuego ronda = mesa.ronda();
        ronda.exigirTurno(cerrador.jugadorId(), RondaEnJuego.Fase.DESCARTAR);
        Mano manoAlCerrar = new Mano(ronda.manoSin(cerrador.jugadorId(), descarte));
        ResultadoValidacion validacion = mesa.regla().validarCierre(manoAlCerrar);
        if (!validacion.valido()) {
            historial.registrarEvento(EventoJuego.de(mesa.partidaId(), ronda.numero(), ronda.rondaId(),
                    cerrador.jugadorId(), TipoEvento.CIERRE_RECHAZADO, validacion.condicionIncumplida()));
            return ResultadoAccion.rechazo(validacion.condicionIncumplida());
        }
        Map<Integer, Integer> penalizaciones = new LinkedHashMap<>();
        Map<Integer, List<Carta>> sueltas = new HashMap<>();
        penalizaciones.put(cerrador.jugadorId(), mesa.regla().penalizacionCerrador(manoAlCerrar));
        sueltas.put(cerrador.jugadorId(), validacion.sobrantes());
        liquidarPerdedores(ronda, cerrador.jugadorId(), penalizaciones, sueltas);

        ResultadoCierreRonda resultado = cierreTransaccional.registrar(new DatosCierre(mesa.partidaId(),
                ronda.rondaId(), ronda.numero(), cerrador.jugadorId(), mesa.regla().modalidad(), descarte,
                penalizaciones));
        ronda.descartarAlCerrar(cerrador.jugadorId(), descarte);
        ResumenCierre resumen = resumir(mesa, cerrador, descarte, validacion, penalizaciones, sueltas,
                resultado.liquidacion().marcador());
        mesa.terminarRonda(resumen, resultado.ganadorId());
        return ResultadoAccion.ok("¡Cierre válido! Ganaste la ronda " + ronda.numero());
    }

    /** RF-17: a cada perdedor se le cuentan las cartas que no logra combinar. */
    private void liquidarPerdedores(RondaEnJuego ronda, int cerradorId, Map<Integer, Integer> penalizaciones,
            Map<Integer, List<Carta>> sueltas) {
        for (Integer jugadorId : ronda.orden()) {
            if (jugadorId != cerradorId) {
                ResultadoCombinaciones mejor = evaluador.evaluarCombinaciones(new Mano(ronda.manoDe(jugadorId)));
                penalizaciones.put(jugadorId, mejor.puntosSueltos());
                sueltas.put(jugadorId, mejor.sueltas());
            }
        }
    }

    private ResumenCierre resumir(Mesa mesa, Participante cerrador, Carta descarte, ResultadoValidacion validacion,
            Map<Integer, Integer> penalizaciones, Map<Integer, List<Carta>> sueltas, Marcador marcador) {
        List<PenalizacionRonda> filas = new ArrayList<>();
        penalizaciones.forEach((id, puntos) -> filas.add(new PenalizacionRonda(id, mesa.nombreDe(id), puntos,
                sueltas.get(id), marcador.acumuladoDe(id), marcador.estaEliminado(id))));
        List<List<Carta>> combinaciones = validacion.combinaciones().stream().map(Combinacion::cartas).toList();
        return new ResumenCierre(mesa.ronda().numero(), cerrador.jugadorId(), cerrador.nombre(), descarte,
                combinaciones, validacion.sobrantes(), filas);
    }
}
