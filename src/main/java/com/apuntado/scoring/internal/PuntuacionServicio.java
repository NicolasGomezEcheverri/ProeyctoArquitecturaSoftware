package com.apuntado.scoring.internal;

import com.apuntado.scoring.api.IPuntuacion;
import com.apuntado.shared.Carta;
import java.util.List;

public class PuntuacionServicio implements IPuntuacion {

    @Override
    public int liquidarRonda(List<Carta> cartasSueltas) {
        return cartasSueltas.stream().mapToInt(Carta::valorPuntos).sum();
    }
}
