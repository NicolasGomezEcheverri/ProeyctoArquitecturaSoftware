package com.apuntado.rules;

import static com.apuntado.soporte.Cartas.mano;
import static org.assertj.core.api.Assertions.assertThat;

import com.apuntado.rules.api.ResultadoValidacion;
import com.apuntado.rules.apuntado.ReglaApuntado101;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Casos borde pedidos en la guia del Entregable 3 (Unit &amp; Functional Testing Checklist). */
class ReglaApuntado101Test {

    private final ReglaApuntado101 regla = new ReglaApuntado101();

    @Test
    @DisplayName("Tres ternas y sobrante de 4 puntos: valido, y quien cierra se apunta 4")
    void sobranteMenorQueCinco() {
        ResultadoValidacion r = regla.validarCierre(mano("7P 7C 7D 2C 3C 4C KP KC KD 4T"));
        assertThat(r.valido()).isTrue();
        assertThat(r.sobrantes()).hasSize(1);
        assertThat(regla.penalizacionCerrador(mano("7P 7C 7D 2C 3C 4C KP KC KD 4T"))).isEqualTo(4);
    }

    @Test
    @DisplayName("Sobrante de exactamente 5 puntos: invalido (el limite es estricto)")
    void sobranteDeCincoEsInvalido() {
        ResultadoValidacion r = regla.validarCierre(mano("7P 7C 7D 2C 3C 4C KP KC KD 5T"));
        assertThat(r.valido()).isFalse();
        assertThat(r.condicionIncumplida()).contains("5 puntos");
    }

    @Test
    @DisplayName("Sobrante de 6 puntos: invalido (el caso del usuario U02 de la guia)")
    void sobranteDeSeisEsInvalido() {
        assertThat(regla.validarCierre(mano("7P 7C 7D 2C 3C 4C KP KC KD 6T")).valido()).isFalse();
    }

    @Test
    @DisplayName("Ternas incompletas (dos ternas y cartas sueltas): invalido")
    void ternasIncompletas() {
        ResultadoValidacion r = regla.validarCierre(mano("7P 7C 7D 2C 3C 4C KP KC 9D AT"));
        assertThat(r.valido()).isFalse();
        assertThat(r.condicionIncumplida()).contains("3 ternas");
    }

    @Test
    @DisplayName("Un As suelto vale 10 y no deja cerrar")
    void asSueltoValeDiez() {
        assertThat(regla.validarCierre(mano("7P 7C 7D 2C 3C 4C KP KC KD AT")).valido()).isFalse();
    }

    @Test
    @DisplayName("Con una cuarta, una de sus cartas puede quedar como sobrante si vale poco")
    void cuartaConSobrantePequeno() {
        ResultadoValidacion r = regla.validarCierre(mano("2P 2C 2D 2T 5C 6C 7C JP JC JD"));
        assertThat(r.valido()).isTrue();
        assertThat(regla.penalizacionCerrador(mano("2P 2C 2D 2T 5C 6C 7C JP JC JD"))).isEqualTo(2);
    }

    @Test
    @DisplayName("El As puede ir en escalera baja (A-2-3) y el sobrante de 3 puntos deja cerrar")
    void sobranteMinimo() {
        assertThat(regla.validarCierre(mano("AP 2P 3P 8C 8D 8T QC QD QT 3D")).valido()).isTrue();
    }
}
