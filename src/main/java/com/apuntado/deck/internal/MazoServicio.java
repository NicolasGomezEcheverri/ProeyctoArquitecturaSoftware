package com.apuntado.deck.internal;

import com.apuntado.deck.api.IMazo;
import com.apuntado.shared.Carta;
import com.apuntado.shared.Palo;
import com.apuntado.shared.Valor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MazoServicio implements IMazo {

    @Override
    public List<Carta> generarYBarajar() {
        List<Carta> cartas = new ArrayList<>();
        for (Palo palo : Palo.values()) {
            for (Valor valor : Valor.values()) {
                cartas.add(new Carta(palo, valor));
            }
        }
        Collections.shuffle(cartas);
        return cartas;
    }
}
