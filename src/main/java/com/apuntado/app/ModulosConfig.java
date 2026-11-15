package com.apuntado.app;

import com.apuntado.deck.api.IMazo;
import com.apuntado.deck.internal.MazoServicio;
import com.apuntado.rules.api.IEvaluadorCombinaciones;
import com.apuntado.rules.api.IRegistroReglas;
import com.apuntado.rules.evaluacion.EvaluadorCombinaciones;
import com.apuntado.rules.registry.RegistroReglas;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los modulos rules y deck no usan anotaciones de Spring: son Java puro y se
 * conectan aqui. Asi las variantes de reglas siguen siendo plug-ins que el
 * registro descubre con ServiceLoader (ADR-03).
 */
@Configuration
public class ModulosConfig {

    @Bean
    public IRegistroReglas registroReglas() {
        return new RegistroReglas();
    }

    @Bean
    public IEvaluadorCombinaciones evaluadorCombinaciones() {
        return new EvaluadorCombinaciones();
    }

    @Bean
    public IMazo mazo() {
        return new MazoServicio();
    }

    @Bean
    public RandomGenerator aleatorio() {
        return new SecureRandom();
    }
}
