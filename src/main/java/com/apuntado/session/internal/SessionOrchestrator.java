package com.apuntado.session.internal;

import com.apuntado.gameplay.api.IControlPartida;
import com.apuntado.session.api.IGestionPartida;

public class SessionOrchestrator implements IGestionPartida {

    private final IControlPartida control;

    public SessionOrchestrator(IControlPartida control) {
        this.control = control;
    }

    @Override
    public void crearPartida(String modalidad) {
        control.iniciarPartida(modalidad);
    }
}
