package com.apuntado.rules;

import static com.apuntado.soporte.Cartas.mano;
import static org.assertj.core.api.Assertions.assertThat;

import com.apuntado.rules.api.ResultadoValidacion;
import com.apuntado.rules.clasica.ReglaClasicaCuartaTerna;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Casos borde de la modalidad Clasica (4-3-3 + descarte). La mano llega sin la carta descartada. */
class ReglaClasicaCuartaTernaTest {

    private final ReglaClasicaCuartaTerna regla = new ReglaClasicaCuartaTerna();

    @Test
    @DisplayName("Cuarta de grupo + dos ternas: cierre valido y quien cierra no anota")
    void cierreValidoConCuartaDeGrupo() {
        ResultadoValidacion r = regla.validarCierre(mano("7P 7C 7D 7T 2C 3C 4C KP KC KD"));
        assertThat(r.valido()).isTrue();
        assertThat(r.combinaciones()).hasSize(3);
        assertThat(r.sobrantes()).isEmpty();
        assertThat(regla.penalizacionCerrador(mano("7P 7C 7D 7T 2C 3C 4C KP KC KD"))).isZero();
    }

    @Test
    @DisplayName("La cuarta tambien puede ser una escalera")
    void cierreValidoConCuartaEscalera() {
        assertThat(regla.validarCierre(mano("9D 10D JD QD 5P 5C 5T AP AC AD")).valido()).isTrue();
    }

    @Test
    @DisplayName("Tres ternas y una suelta no alcanzan en Clasica")
    void tresTernasNoAlcanzan() {
        ResultadoValidacion r = regla.validarCierre(mano("7P 7C 7D 2C 3C 4C KP KC KD 9T"));
        assertThat(r.valido()).isFalse();
        assertThat(r.condicionIncumplida()).contains("1 cuarta y 2 ternas");
        assertThat(r.combinaciones()).isEmpty();
    }

    @Test
    @DisplayName("Dos cuartas y una pareja tampoco")
    void dosCuartasNoAlcanzan() {
        assertThat(regla.validarCierre(mano("7P 7C 7D 7T 2C 3C 4C 5C KP KC")).valido()).isFalse();
    }

    @Test
    @DisplayName("Si no se aparto el descarte (11 cartas) se rechaza")
    void tamanoIncorrecto() {
        ResultadoValidacion r = regla.validarCierre(mano("7P 7C 7D 7T 2C 3C 4C KP KC KD 9T"));
        assertThat(r.valido()).isFalse();
        assertThat(r.condicionIncumplida()).contains("11 cartas");
    }
}
