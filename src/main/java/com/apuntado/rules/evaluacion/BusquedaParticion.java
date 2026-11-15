package com.apuntado.rules.evaluacion;

import com.apuntado.rules.api.Combinacion;
import com.apuntado.rules.api.ResultadoCombinaciones;
import com.apuntado.shared.Carta;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Busqueda con retroceso (backtracking) del mejor reparto de una mano.
 *
 * <p>Recorre las cartas en orden. Para cada carta todavia libre prueba dos cosas:
 * dejarla suelta o usarla como la primera carta de alguna combinacion que no choque
 * con las ya elegidas. Como cada combinacion se prueba solo desde su carta de menor
 * posicion, cada reparto se visita una sola vez. Con manos de 10 u 11 cartas el arbol
 * es pequeno y ademas se poda cuando los puntos sueltos ya superan al mejor reparto.
 */
final class BusquedaParticion {

    private final List<Carta> cartas;
    private final List<List<Candidata>> porMenorIndice;
    private final CriterioBusqueda criterio;
    private final Deque<Candidata> elegidas = new ArrayDeque<>();
    private List<Candidata> mejor;
    private int mejorPuntos = Integer.MAX_VALUE;

    BusquedaParticion(List<Carta> cartas, List<Candidata> candidatas, CriterioBusqueda criterio) {
        this.cartas = cartas;
        this.criterio = criterio;
        this.porMenorIndice = new ArrayList<>();
        for (int i = 0; i < cartas.size(); i++) {
            porMenorIndice.add(new ArrayList<>());
        }
        for (Candidata candidata : candidatas) {
            porMenorIndice.get(candidata.menorIndice()).add(candidata);
        }
    }

    Optional<ResultadoCombinaciones> ejecutar() {
        explorar(new Paso(0, 0, 0, 0, 0));
        return Optional.ofNullable(mejor).map(this::armarResultado);
    }

    private void explorar(Paso paso) {
        if (debePodar(paso)) {
            return;
        }
        if (paso.indice() == cartas.size()) {
            registrarSiCumple(paso);
            return;
        }
        if (paso.cubre(paso.indice())) {
            explorar(paso.avanzar());
            return;
        }
        ramificar(paso);
    }

    /** Ya hay un reparto mejor o ya se pasaron las cuartas/ternas pedidas. */
    private boolean debePodar(Paso paso) {
        return paso.puntosSueltos() >= mejorPuntos || criterio.excede(paso.cuartas(), paso.ternas());
    }

    /** La carta actual empieza alguna combinacion libre, o se queda suelta. */
    private void ramificar(Paso paso) {
        for (Candidata candidata : porMenorIndice.get(paso.indice())) {
            if (paso.libre(candidata)) {
                elegidas.push(candidata);
                explorar(paso.usar(candidata));
                elegidas.pop();
            }
        }
        explorar(paso.dejarSuelta(cartas.get(paso.indice()).valorPuntos()));
    }

    private void registrarSiCumple(Paso paso) {
        if (criterio.cumple(paso.cuartas(), paso.ternas())) {
            mejorPuntos = paso.puntosSueltos();
            mejor = new ArrayList<>(elegidas);
        }
    }

    private ResultadoCombinaciones armarResultado(List<Candidata> elegidasFinales) {
        List<Combinacion> combinaciones = new ArrayList<>();
        int cubiertas = 0;
        for (Candidata candidata : elegidasFinales) {
            combinaciones.add(new Combinacion(candidata.forma(), cartasDe(candidata)));
            cubiertas |= candidata.mascara();
        }
        combinaciones.sort(Comparator.comparingInt((Combinacion c) -> -c.cartas().size()));
        List<Carta> sueltas = new ArrayList<>();
        for (int i = 0; i < cartas.size(); i++) {
            if ((cubiertas & (1 << i)) == 0) {
                sueltas.add(cartas.get(i));
            }
        }
        return new ResultadoCombinaciones(combinaciones, sueltas);
    }

    private List<Carta> cartasDe(Candidata candidata) {
        List<Carta> resultado = new ArrayList<>();
        for (int i = 0; i < cartas.size(); i++) {
            if ((candidata.mascara() & (1 << i)) != 0) {
                resultado.add(cartas.get(i));
            }
        }
        resultado.sort(Comparator.comparingInt((Carta c) -> c.valor().rango()));
        return resultado;
    }

    /** Estado inmutable de un nodo del arbol de busqueda. */
    private record Paso(int indice, int cubiertas, int puntosSueltos, int cuartas, int ternas) {

        boolean cubre(int posicion) {
            return (cubiertas & (1 << posicion)) != 0;
        }

        boolean libre(Candidata candidata) {
            return (cubiertas & candidata.mascara()) == 0;
        }

        Paso avanzar() {
            return new Paso(indice + 1, cubiertas, puntosSueltos, cuartas, ternas);
        }

        Paso usar(Candidata candidata) {
            int nuevasCuartas = cuartas + (candidata.esCuarta() ? 1 : 0);
            int nuevasTernas = ternas + (candidata.esCuarta() ? 0 : 1);
            return new Paso(indice + 1, cubiertas | candidata.mascara(), puntosSueltos, nuevasCuartas, nuevasTernas);
        }

        Paso dejarSuelta(int puntos) {
            return new Paso(indice + 1, cubiertas | (1 << indice), puntosSueltos + puntos, cuartas, ternas);
        }
    }
}
