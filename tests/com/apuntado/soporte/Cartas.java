package com.apuntado.soporte;

import com.apuntado.shared.Carta;
import com.apuntado.shared.Mano;
import com.apuntado.shared.Palo;
import com.apuntado.shared.Valor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Notacion corta para escribir manos en las pruebas: valor + inicial del palo.
 * Ejemplos: "7P" = 7 de picas, "10C" = 10 de corazones, "AD" = As de diamantes, "KT" = K de treboles.
 */
public final class Cartas {

    private static final Map<String, Valor> VALORES = Map.ofEntries(
            Map.entry("A", Valor.AS), Map.entry("2", Valor.DOS), Map.entry("3", Valor.TRES),
            Map.entry("4", Valor.CUATRO), Map.entry("5", Valor.CINCO), Map.entry("6", Valor.SEIS),
            Map.entry("7", Valor.SIETE), Map.entry("8", Valor.OCHO), Map.entry("9", Valor.NUEVE),
            Map.entry("10", Valor.DIEZ), Map.entry("J", Valor.J), Map.entry("Q", Valor.Q), Map.entry("K", Valor.K));
    private static final Map<Character, Palo> PALOS = Map.of(
            'P', Palo.PICAS, 'C', Palo.CORAZONES, 'D', Palo.DIAMANTES, 'T', Palo.TREBOLES);

    private Cartas() {
    }

    public static Carta c(String codigo) {
        char palo = codigo.charAt(codigo.length() - 1);
        String valor = codigo.substring(0, codigo.length() - 1);
        return new Carta(PALOS.get(palo), VALORES.get(valor));
    }

    public static List<Carta> lista(String cartas) {
        return Arrays.stream(cartas.trim().split("\\s+")).map(Cartas::c).toList();
    }

    public static Mano mano(String cartas) {
        return new Mano(lista(cartas));
    }
}
