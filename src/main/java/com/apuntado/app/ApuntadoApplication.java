package com.apuntado.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Raiz de composicion del monolito modular: arranca Spring y conecta los modulos.
 * Es el unico paquete que conoce las implementaciones internas de todos.
 */
@SpringBootApplication(scanBasePackages = "com.apuntado")
@EntityScan("com.apuntado")
@EnableJpaRepositories("com.apuntado")
public class ApuntadoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApuntadoApplication.class, args);
    }
}
