package com.apuntado.gameplay.internal;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Primera barrera de concurrencia (ADR-05, seccion 2.4 del Entregable 2): cada
 * partida tiene un unico hilo que procesa sus acciones en orden de llegada. Dos
 * jugadores que actuan al mismo tiempo nunca tocan la mesa a la vez.
 */
@Component
class ColaPorPartida {

    private static final long ESPERA_MAXIMA_SEGUNDOS = 10;

    private final Map<String, ExecutorService> colas = new ConcurrentHashMap<>();

    <T> T ejecutar(String codigo, Supplier<T> tarea) {
        ExecutorService cola = colas.computeIfAbsent(codigo,
                c -> Executors.newSingleThreadExecutor(r -> new Thread(r, "mesa-" + c)));
        try {
            return cola.submit(tarea::get).get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw relanzar(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpio la accion", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("La mesa no respondio a tiempo", e);
        }
    }

    void liberar(String codigo) {
        ExecutorService cola = colas.remove(codigo);
        if (cola != null) {
            cola.shutdown();
        }
    }

    @PreDestroy
    void cerrar() {
        colas.values().forEach(ExecutorService::shutdownNow);
    }

    private static RuntimeException relanzar(Throwable causa) {
        if (causa instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException(causa);
    }
}
