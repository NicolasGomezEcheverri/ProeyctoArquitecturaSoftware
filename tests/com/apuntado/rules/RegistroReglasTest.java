package com.apuntado.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.apuntado.rules.registry.RegistroReglas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistroReglasTest {

    @Test
    @DisplayName("ServiceLoader descubre las dos variantes sin que el registro las importe")
    void descubreLasVariantes() {
        RegistroReglas registro = new RegistroReglas();
        assertThat(registro.modalidadesDisponibles()).containsKeys("CLASICA", "APUNTADO");
        assertThat(registro.obtenerRegla("APUNTADO").modalidad()).isEqualTo("APUNTADO");
    }

    @Test
    void modalidadDesconocida() {
        assertThatThrownBy(() -> new RegistroReglas().obtenerRegla("CONTINENTAL"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
