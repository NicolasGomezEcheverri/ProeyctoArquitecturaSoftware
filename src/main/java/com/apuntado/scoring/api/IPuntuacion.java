package com.apuntado.scoring.api;

import com.apuntado.shared.Carta;
import java.util.List;

public interface IPuntuacion {

    int liquidarRonda(List<Carta> cartasSueltas);
}
