package com.apuntado.session.internal;

import com.apuntado.gameplay.api.ConfiguracionInicio;
import com.apuntado.gameplay.api.IControlPartida;
import com.apuntado.gameplay.api.Participante;
import com.apuntado.session.api.EstadoSala;
import com.apuntado.session.api.IGestionPartida;
import com.apuntado.session.api.IngresoSala;
import com.apuntado.session.api.SolicitudSala;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SessionOrchestrator. Valida la configuracion (RF-02, RN-02, RN-03), registra a
 * los jugadores y, cuando la sala se llena o el creador lo pide, le entrega la
 * mesa a gameplay.
 */
@Service
public class SessionOrchestrator implements IGestionPartida {

    static final int MIN_JUGADORES = 2;
    static final int MAX_JUGADORES = 4;
    static final int UMBRAL_MINIMO = 20;
    static final int UMBRAL_MAXIMO = 500;
    private static final String LETRAS_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LARGO_CODIGO = 5;
    private static final int LARGO_NOMBRE = 20;

    private final IControlPartida control;
    private final JugadorRepository jugadores;
    private final Map<String, Sala> salas = new ConcurrentHashMap<>();
    private final SecureRandom aleatorio = new SecureRandom();

    public SessionOrchestrator(IControlPartida control, JugadorRepository jugadores) {
        this.control = control;
        this.jugadores = jugadores;
    }

    @Override
    @Transactional
    public IngresoSala crearSala(SolicitudSala solicitud) {
        validar(solicitud);
        Sala sala = new Sala(nuevoCodigo(), solicitud.modalidad(), solicitud.jugadores(), solicitud.umbral());
        Participante creador = registrarJugador(solicitud.nombre());
        sala.agregar(creador);
        salas.put(sala.codigo(), sala);
        return ingreso(sala, creador);
    }

    @Override
    public IngresoSala unirse(String codigo, String nombre) {
        Sala sala = sala(codigo);
        Participante nuevo = registrarJugador(nombre);
        sala.agregar(nuevo);
        if (sala.llena()) {
            arrancar(sala);
        }
        return ingreso(sala, nuevo);
    }

    @Override
    public EstadoSala estadoSala(String codigo) {
        return sala(codigo).estado();
    }

    @Override
    public EstadoSala iniciar(String codigo, int jugadorId, String token) {
        Sala sala = sala(codigo);
        Participante creador = sala.creador();
        if (creador.jugadorId() != jugadorId || !creador.token().equals(token)) {
            throw new IllegalArgumentException("Solo quien creó la sala puede iniciar la partida");
        }
        if (sala.jugadores().size() < MIN_JUGADORES) {
            throw new IllegalStateException("Se necesitan al menos " + MIN_JUGADORES + " jugadores");
        }
        arrancar(sala);
        return sala.estado();
    }

    @Override
    public Map<String, String> modalidades() {
        return control.modalidadesDisponibles();
    }

    private void arrancar(Sala sala) {
        if (!sala.marcarIniciada()) {
            return;
        }
        try {
            control.iniciarPartida(new ConfiguracionInicio(sala.codigo(), sala.modalidad(), sala.umbral(),
                    sala.jugadores()));
        } catch (RuntimeException e) {
            sala.revertirInicio();
            throw e;
        }
    }

    private void validar(SolicitudSala solicitud) {
        if (!control.modalidadesDisponibles().containsKey(solicitud.modalidad())) {
            throw new IllegalArgumentException("Modalidad desconocida: " + solicitud.modalidad());
        }
        if (solicitud.jugadores() < MIN_JUGADORES || solicitud.jugadores() > MAX_JUGADORES) {
            throw new IllegalArgumentException("La mesa admite de 2 a 4 jugadores (RN-02)");
        }
        if (solicitud.umbral() < UMBRAL_MINIMO || solicitud.umbral() > UMBRAL_MAXIMO) {
            throw new IllegalArgumentException("El umbral debe estar entre 20 y 500 puntos");
        }
    }

    private Participante registrarJugador(String nombre) {
        String limpio = nombre == null ? "" : nombre.strip();
        if (limpio.isEmpty() || limpio.length() > LARGO_NOMBRE) {
            throw new IllegalArgumentException("Escribe un nombre de 1 a 20 caracteres");
        }
        String sufijo = HexFormat.of().formatHex(bytesAleatorios(3));
        JugadorEntity jugador = jugadores.save(new JugadorEntity(limpio + "#" + sufijo));
        String token = HexFormat.of().formatHex(bytesAleatorios(16));
        return new Participante(jugador.getId(), limpio, token);
    }

    private String nuevoCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < LARGO_CODIGO; i++) {
                sb.append(LETRAS_CODIGO.charAt(aleatorio.nextInt(LETRAS_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (salas.containsKey(codigo) || control.codigoEnUso(codigo));
        return codigo;
    }

    private byte[] bytesAleatorios(int cantidad) {
        byte[] bytes = new byte[cantidad];
        aleatorio.nextBytes(bytes);
        return bytes;
    }

    private Sala sala(String codigo) {
        Sala sala = codigo == null ? null : salas.get(codigo.strip().toUpperCase());
        if (sala == null) {
            throw new IllegalArgumentException("No existe una sala con el código " + codigo);
        }
        return sala;
    }

    private static IngresoSala ingreso(Sala sala, Participante participante) {
        return new IngresoSala(sala.codigo(), participante.jugadorId(), participante.token(), participante.nombre());
    }
}
