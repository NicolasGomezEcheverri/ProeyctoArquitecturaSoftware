package com.apuntado.rules.evaluacion;

import com.apuntado.rules.api.Combinacion.Forma;
import com.apuntado.shared.Carta;
import com.apuntado.shared.Palo;
import com.apuntado.shared.Valor;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enumera todas las ternas y cuartas que se pueden formar con una mano:
 * grupos (mismo valor) y escaleras (mismo palo, valores consecutivos; el As
 * vale como 1 o despues de la K, pero no da la vuelta K-A-2).
 */
final class GeneradorCandidatas {

    private static final int AS_ALTO = 14;
    private static final int ULTIMO_INICIO_ESCALERA = 12;

    List<Candidata> generar(List<Carta> cartas) {
        List<Candidata> candidatas = new ArrayList<>();
        agregarGrupos(cartas, candidatas);
        agregarEscaleras(cartas, candidatas);
        return candidatas;
    }

    private void agregarGrupos(List<Carta> cartas, List<Candidata> candidatas) {
        Map<Valor, List<Integer>> porValor = new EnumMap<>(Valor.class);
        for (int i = 0; i < cartas.size(); i++) {
            porValor.computeIfAbsent(cartas.get(i).valor(), v -> new ArrayList<>()).add(i);
        }
        for (List<Integer> indices : porValor.values()) {
            agregarSubgrupos(indices, candidatas);
        }
    }

    /** De un grupo de 3 o 4 cartas iguales salen todas sus ternas y su cuarta. */
    private void agregarSubgrupos(List<Integer> indices, List<Candidata> candidatas) {
        int n = indices.size();
        for (int subconjunto = 0; subconjunto < (1 << n); subconjunto++) {
            int tamano = Integer.bitCount(subconjunto);
            if (tamano == 3 || tamano == 4) {
                candidatas.add(new Candidata(mascara(indices, subconjunto), Forma.GRUPO));
            }
        }
    }

    private int mascara(List<Integer> indices, int subconjunto) {
        int mascara = 0;
        for (int j = 0; j < indices.size(); j++) {
            if ((subconjunto & (1 << j)) != 0) {
                mascara |= 1 << indices.get(j);
            }
        }
        return mascara;
    }

    private void agregarEscaleras(List<Carta> cartas, List<Candidata> candidatas) {
        for (Palo palo : Palo.values()) {
            Map<Integer, Integer> porRango = indicesPorRango(cartas, palo);
            for (int inicio = 1; inicio <= ULTIMO_INICIO_ESCALERA; inicio++) {
                agregarEscalerasDesde(porRango, inicio, candidatas);
            }
        }
    }

    private Map<Integer, Integer> indicesPorRango(List<Carta> cartas, Palo palo) {
        Map<Integer, Integer> porRango = new HashMap<>();
        for (int i = 0; i < cartas.size(); i++) {
            Carta carta = cartas.get(i);
            if (carta.palo() == palo) {
                porRango.put(carta.valor().rango(), i);
                if (carta.valor() == Valor.AS) {
                    porRango.put(AS_ALTO, i);
                }
            }
        }
        return porRango;
    }

    /** Escaleras de 3 y de 4 que empiezan en el rango dado. */
    private void agregarEscalerasDesde(Map<Integer, Integer> porRango, int inicio, List<Candidata> candidatas) {
        int mascara = 0;
        for (int largo = 1; largo <= 4; largo++) {
            Integer indice = porRango.get(inicio + largo - 1);
            if (indice == null) {
                return;
            }
            mascara |= 1 << indice;
            if (largo >= 3) {
                candidatas.add(new Candidata(mascara, Forma.ESCALERA));
            }
        }
    }
}
