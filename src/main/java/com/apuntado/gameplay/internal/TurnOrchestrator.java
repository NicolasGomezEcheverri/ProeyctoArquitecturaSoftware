package com.apuntado.gameplay.internal;

import com.apuntado.deck.api.IMazo;
import com.apuntado.gameplay.api.IAccionesJugador;
import com.apuntado.gameplay.api.IControlPartida;
import com.apuntado.gameplay.api.INotificadorEstado;
import com.apuntado.gameplay.api.ResultadoCierre;
import com.apuntado.history.api.IHistorial;
import com.apuntado.rules.api.IRegistroReglas;
import com.apuntado.rules.api.ReglaJuego;
import com.apuntado.rules.api.ResultadoValidacion;
import com.apuntado.shared.Mano;

/** Mediador del ciclo de la partida: coordina, no valida ni puntua. */
public class TurnOrchestrator implements IAccionesJugador, IControlPartida {

    private final IMazo mazo;
    private final IRegistroReglas registro;
    private final IHistorial historial;
    private final INotificadorEstado notificador;
    private ReglaJuego regla;

    public TurnOrchestrator(IMazo mazo, IRegistroReglas registro,
            IHistorial historial, INotificadorEstado notificador) {
        this.mazo = mazo;
        this.registro = registro;
        this.historial = historial;
        this.notificador = notificador;
    }

    @Override
    public void iniciarPartida(String modalidad) {
        this.regla = registro.obtenerRegla(modalidad);
        mazo.generarYBarajar();
        historial.registrarEvento("PARTIDA_INICIADA " + modalidad);
        notificador.publicarEstadoPublico("PARTIDA_INICIADA");
    }

    @Override
    public ResultadoCierre solicitarCierre(Mano mano) {
        ResultadoValidacion r = regla.validarCierre(mano);
        historial.registrarEvento(r.valido() ? "CIERRE_ACEPTADO" : "CIERRE_RECHAZADO");
        if (r.valido()) {
            notificador.publicarEstadoPublico("RONDA_CERRADA");
        }
        return new ResultadoCierre(r.valido(), r.condicionIncumplida());
    }
}
