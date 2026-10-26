package com.apuntado.presentation;

import com.apuntado.gameplay.api.IAccionesJugador;
import com.apuntado.gameplay.api.ResultadoCierre;
import com.apuntado.session.api.IGestionPartida;
import com.apuntado.shared.Mano;

public class PartidaController {

    private final IGestionPartida gestion;
    private final IAccionesJugador acciones;

    public PartidaController(IGestionPartida gestion, IAccionesJugador acciones) {
        this.gestion = gestion;
        this.acciones = acciones;
    }

    public void crearPartida(String modalidad) {
        gestion.crearPartida(modalidad);
    }

    public ResultadoCierre solicitarCierre(Mano mano) {
        return acciones.solicitarCierre(mano);
    }
}
