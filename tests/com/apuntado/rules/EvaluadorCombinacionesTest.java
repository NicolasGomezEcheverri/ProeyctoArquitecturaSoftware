package com.apuntado.rules;

import static com.apuntado.soporte.Cartas.lista;
import static com.apuntado.soporte.Cartas.mano;
import static org.assertj.core.api.Assertions.assertThat;

import com.apuntado.rules.api.Combinacion;
import com.apuntado.rules.api.ResultadoCombinaciones;
import com.apuntado.rules.evaluacion.EvaluadorCombinaciones;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvaluadorCombinacionesTest {

    private final EvaluadorCombinaciones evaluador = new EvaluadorCombinaciones();

    @Test
    @DisplayName("Reconoce una terna de grupo (mismo valor, distinto palo)")
    void ternaDeGrupo() {
        ResultadoCombinaciones r = evaluador.evaluarCombinaciones(mano("7P 7C 7D 2T"));
        assertThat(r.ternas()).isEqualTo(1);
        assertThat(r.sueltas()).containsExactlyElementsOf(lista("2T"));
        assertThat(r.puntosSueltos()).isEqualTo(2);
    }

    @Test
    @DisplayName("Reconoce escaleras de 3 y 4 del mismo palo")
    void escaleras() {
        ResultadoCombinaciones r = evaluador.evaluarCombinaciones(mano("4C 5C 6C 7C 9P 10P JP"));
        assertThat(r.cuartas()).isEqualTo(1);
        assertThat(r.ternas()).isEqualTo(1);
        assertThat(r.sueltas()).isEmpty();
        assertThat(r.combinaciones()).extracting(Combinacion::forma).containsOnly(Combinacion.Forma.ESCALERA);
    }

    @Test
    @DisplayName("El As sirve antes del 2 y despues de la K")
    void asBajoYAlto() {
        assertThat(evaluador.evaluarCombinaciones(mano("AD 2D 3D")).ternas()).isEqualTo(1);
        assertThat(evaluador.evaluarCombinaciones(mano("QD KD AD")).ternas()).isEqualTo(1);
    }

    @Test
    @DisplayName("No hay escalera que de la vuelta K-A-2")
    void sinVuelta() {
        ResultadoCombinaciones r = evaluador.evaluarCombinaciones(mano("KD AD 2D"));
        assertThat(r.combinaciones()).isEmpty();
        assertThat(r.puntosSueltos()).isEqualTo(22);
    }

    @Test
    @DisplayName("Palos distintos no forman escalera")
    void escaleraConPalosMezclados() {
        assertThat(evaluador.evaluarCombinaciones(mano("4C 5P 6C")).combinaciones()).isEmpty();
    }

    @Test
    @DisplayName("Entre varios repartos posibles elige el que deja menos puntos sueltos")
    void eligeElMejorReparto() {
        // El 7C puede ir en la terna de sietes o en la escalera 5-6-7 de corazones.
        // Usarlo en la escalera deja sueltos 7P y 7D (14 pts); en la terna deja 5C y 6C (11 pts).
        ResultadoCombinaciones r = evaluador.evaluarCombinaciones(mano("7P 7C 7D 5C 6C"));
        assertThat(r.puntosSueltos()).isEqualTo(11);
    }

    @Test
    @DisplayName("Una cuarta de grupo tambien aporta ternas cuando se piden solo ternas")
    void cuartaComoTerna() {
        Optional<ResultadoCombinaciones> r = evaluador.buscarDistribucion(mano("9P 9C 9D 9T 2C 3C 4C"), 0, 2);
        assertThat(r).isPresent();
        assertThat(r.get().sueltas()).hasSize(1);
        assertThat(r.get().puntosSueltos()).isEqualTo(9);
    }

    @Test
    @DisplayName("Ninguna carta queda en dos combinaciones a la vez")
    void combinacionesDisjuntas() {
        ResultadoCombinaciones r = evaluador.evaluarCombinaciones(mano("5C 6C 7C 7P 7D 7T 8C"));
        var usadas = new HashSet<>();
        r.combinaciones().forEach(c -> c.cartas().forEach(carta -> assertThat(usadas.add(carta)).isTrue()));
        assertThat(r.sueltas()).isEmpty();
    }
}
