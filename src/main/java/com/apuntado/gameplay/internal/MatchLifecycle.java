package com.apuntado.gameplay.internal;

import com.apuntado.deck.api.IMazo;
import com.apuntado.deck.api.Reparto;
import com.apuntado.gameplay.api.ConfiguracionInicio;
import com.apuntado.gameplay.api.IControlPartida;
import com.apuntado.gameplay.api.Participante;
import com.apuntado.history.api.EventoJuego;
import com.apuntado.history.api.IHistorial;
import com.apuntado.history.api.TipoEvento;
import com.apuntado.rules.api.IRegistroReglas;
import com.apuntado.rules.api.ReglaJuego;
import com.apuntado.scoring.api.IPuntuacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/** MatchLifecycle: arranque de la partida y de cada ronda (RF-04 a RF-06). */
@Component
class MatchLifecycle implements IControlPartida {

    private final IRegistroReglas registroReglas;
    private final IMazo mazo;
    private final IPuntuacion puntuacion;
    private final IHistorial historial;
    private final RegistroPartida registroPartida;
    private final TablaMesas mesas;
    private final ColaPorPartida cola;
    private final HandVisibility visibilidad;
    private final RandomGenerator aleatorio;

    MatchLifecycle(IRegistroReglas registroReglas, IMazo mazo, IPuntuacion puntuacion, IHistorial historial,
            RegistroPartida registroPartida, TablaMesas mesas, ColaPorPartida cola, HandVisibility visibilidad,
            RandomGenerator aleatorio) {
        this.registroReglas = registroReglas;
        this.mazo = mazo;
        this.puntuacion = puntuacion;
        this.historial = historial;
        this.registroPartida = registroPartida;
        this.mesas = mesas;
        this.cola = cola;
        this.visibilidad = visibilidad;
        this.aleatorio = aleatorio;
    }

    @Override
    public int iniciarPartida(ConfiguracionInicio configuracion) {
        ReglaJuego regla = registroReglas.obtenerRegla(configuracion.modalidad());
        List<Participante> orden = sortearOrden(configuracion.participantes());
        int partidaId = registroPartida.crearPartida(configuracion.codigo(), regla.modalidad(),
                configuracion.umbral(), orden);
        puntuacion.abrirMarcador(partidaId, orden.stream().map(Participante::jugadorId).toList(),
                configuracion.umbral());
        Mesa mesa = new Mesa(partidaId, configuracion.codigo(), regla, configuracion.umbral(), orden,
                aleatorio.nextInt(orden.size()));
        mesas.agregar(mesa);
        historial.registrarEvento(EventoJuego.de(partidaId, null, null, null, TipoEvento.PARTIDA_INICIADA,
                regla.modalidad() + ", umbral " + configuracion.umbral() + ", " + orden.size() + " jugadores"));
        cola.ejecutar(mesa.codigo(), () -> {
            repartirRonda(mesa, mesa.abridorActual(), 1);
            visibilidad.difundir(mesa);
            return null;
        });
        return partidaId;
    }

    @Override
    public Map<String, String> modalidadesDisponibles() {
        return registroReglas.modalidadesDisponibles();
    }

    @Override
    public boolean codigoEnUso(String codigo) {
        return mesas.buscar(codigo).isPresent() || registroPartida.codigoEnUso(codigo);
    }

    /** Se llama desde la cola de la mesa cuando alguien pide la siguiente ronda. */
    void siguienteRonda(Mesa mesa) {
        if (mesa.estado() == Mesa.Estado.FINALIZADA) {
            throw new AccionInvalida("La partida ya terminó");
        }
        mesa.exigirEstado(Mesa.Estado.RONDA_TERMINADA, "La ronda actual todavía no termina");
        List<Integer> activos = puntuacion.consultarMarcador(mesa.partidaId()).activos();
        int abridor = mesa.siguienteAbridor(activos);
        repartirRonda(mesa, abridor, mesa.ronda().numero() + 1);
    }

    private void repartirRonda(Mesa mesa, int abridorId, int numero) {
        List<Integer> activos = puntuacion.consultarMarcador(mesa.partidaId()).activos();
        List<Integer> orden = mesa.participantes().stream().map(Participante::jugadorId)
                .filter(activos::contains).toList();
        int rondaId = registroPartida.abrirRonda(mesa.partidaId(), numero);
        Reparto reparto = mazo.repartir(mazo.generarYBarajar(aleatorio), orden.size(), orden.indexOf(abridorId));
        mesa.empezarRonda(new RondaEnJuego(numero, rondaId, orden, orden.indexOf(abridorId), reparto));
        mesa.registrarMovimiento(null, "Ronda " + numero + ": abre " + mesa.nombreDe(abridorId), "RONDA_NUEVA",
                abridorId, null);
        historial.registrarEvento(EventoJuego.de(mesa.partidaId(), numero, rondaId, abridorId,
                TipoEvento.RONDA_INICIADA, "Abre con 11 cartas"));
        historial.registrarEvento(EventoJuego.de(mesa.partidaId(), numero, rondaId, abridorId,
                TipoEvento.TURNO_INICIADO, null));
    }

    /** RF-04: el orden de los turnos se sortea, no se negocia. */
    private List<Participante> sortearOrden(List<Participante> participantes) {
        List<Participante> orden = new ArrayList<>(participantes);
        for (int i = orden.size() - 1; i > 0; i--) {
            int j = aleatorio.nextInt(i + 1);
            Participante temporal = orden.get(i);
            orden.set(i, orden.get(j));
            orden.set(j, temporal);
        }
        return orden;
    }
}
