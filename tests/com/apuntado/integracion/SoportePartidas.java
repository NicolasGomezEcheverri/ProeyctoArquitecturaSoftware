package com.apuntado.integracion;

import static com.apuntado.soporte.Cartas.lista;

import com.apuntado.deck.api.IMazo;
import com.apuntado.deck.api.Reparto;
import com.apuntado.deck.internal.MazoServicio;
import com.apuntado.gameplay.api.EstadoPublico;
import com.apuntado.gameplay.api.INotificadorEstado;
import com.apuntado.shared.Carta;
import com.apuntado.shared.Palo;
import com.apuntado.shared.Valor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Dobles de prueba para las pruebas de integracion: un mazo "arreglado" para conocer
 * las manos de antemano, un azar que siempre da 0 y un notificador que guarda lo
 * que se habria enviado por WebSocket.
 */
@TestConfiguration
public class SoportePartidas {

    @Bean
    @Primary
    MazoArreglado mazoArreglado() {
        return new MazoArreglado();
    }

    @Bean
    @Primary
    RandomGenerator azarFijo() {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public int nextInt(int limite) {
                return 0;
            }
        };
    }

    @Bean
    @Primary
    NotificadorEnMemoria notificadorEnMemoria() {
        return new NotificadorEnMemoria();
    }

    /** Devuelve la baraja preparada una sola vez; despues baraja normal. */
    public static class MazoArreglado implements IMazo {
        private final MazoServicio real = new MazoServicio();
        private volatile List<Carta> proxima;

        /** Prepara una baraja en la que quien abre recibe {@code abridor} y el otro {@code otro}. */
        public void prepararDosJugadores(String abridor, String otro) {
            List<Carta> a = lista(abridor);
            List<Carta> b = lista(otro);
            List<Carta> baraja = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                baraja.add(a.get(i));
                baraja.add(b.get(i));
            }
            baraja.add(a.get(10));
            for (Palo palo : Palo.values()) {
                for (Valor valor : Valor.values()) {
                    Carta carta = new Carta(palo, valor);
                    if (!baraja.contains(carta)) {
                        baraja.add(carta);
                    }
                }
            }
            proxima = baraja;
        }

        @Override
        public List<Carta> generarYBarajar(RandomGenerator aleatorio) {
            List<Carta> baraja = proxima;
            proxima = null;
            return baraja != null ? new ArrayList<>(baraja) : real.generarYBarajar(aleatorio);
        }

        @Override
        public Reparto repartir(List<Carta> mazo, int jugadores, int posicionAbridor) {
            return real.repartir(mazo, jugadores, posicionAbridor);
        }

        @Override
        public List<Carta> reconstruirDesdePozo(List<Carta> pozoSinTope, RandomGenerator aleatorio) {
            return real.reconstruirDesdePozo(pozoSinTope, aleatorio);
        }
    }

    public static class NotificadorEnMemoria implements INotificadorEstado {
        private final Map<String, EstadoPublico> estados = new ConcurrentHashMap<>();
        private final Map<String, List<Carta>> manos = new ConcurrentHashMap<>();

        @Override
        public void publicarEstadoPublico(String codigo, EstadoPublico estado) {
            estados.put(codigo, estado);
        }

        @Override
        public void enviarManoPrivada(String codigo, int jugadorId, List<Carta> mano) {
            manos.put(codigo + "/" + jugadorId, mano);
        }

        public EstadoPublico estado(String codigo) {
            return estados.get(codigo);
        }

        public List<Carta> mano(String codigo, int jugadorId) {
            return manos.get(codigo + "/" + jugadorId);
        }
    }
}
