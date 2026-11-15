package com.apuntado.gameplay.internal;

import com.apuntado.deck.api.IMazo;
import com.apuntado.gameplay.api.Credencial;
import com.apuntado.gameplay.api.IAccionesJugador;
import com.apuntado.gameplay.api.Participante;
import com.apuntado.gameplay.api.ResultadoAccion;
import com.apuntado.history.api.EventoJuego;
import com.apuntado.history.api.IHistorial;
import com.apuntado.history.api.TipoEvento;
import com.apuntado.shared.Carta;
import java.time.Instant;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Service;

/**
 * Mediador del ciclo de la partida (seccion 1.2 del Entregable 2): recibe las
 * acciones, las pone en la cola de su mesa y delega. Coordina, pero no valida
 * combinaciones, no puntua y no baraja.
 */
@Service
public class TurnOrchestrator implements IAccionesJugador {

    private final TablaMesas mesas;
    private final ColaPorPartida cola;
    private final CloseRequest cierre;
    private final MatchLifecycle ciclo;
    private final HandVisibility visibilidad;
    private final IMazo mazo;
    private final IHistorial historial;
    private final RandomGenerator aleatorio;

    TurnOrchestrator(TablaMesas mesas, ColaPorPartida cola, CloseRequest cierre, MatchLifecycle ciclo,
            HandVisibility visibilidad, IMazo mazo, IHistorial historial, RandomGenerator aleatorio) {
        this.mesas = mesas;
        this.cola = cola;
        this.cierre = cierre;
        this.ciclo = ciclo;
        this.visibilidad = visibilidad;
        this.mazo = mazo;
        this.historial = historial;
        this.aleatorio = aleatorio;
    }

    @Override
    public ResultadoAccion robarDelMazo(Credencial credencial, String accionId) {
        return ejecutar(credencial, accionId, (mesa, jugador) -> {
            RondaEnJuego ronda = enRonda(mesa);
            ronda.exigirTurno(jugador.jugadorId(), RondaEnJuego.Fase.ROBAR);
            reconstruirSiHaceFalta(mesa, ronda);
            ronda.robarDelMazo(jugador.jugadorId());
            registrar(mesa, jugador, TipoEvento.ROBO_MAZO, "Roba del mazo");
            return movimiento(mesa, accionId, jugador.nombre() + " robó del mazo");
        });
    }

    @Override
    public ResultadoAccion tomarDelPozo(Credencial credencial, String accionId) {
        return ejecutar(credencial, accionId, (mesa, jugador) -> {
            RondaEnJuego ronda = enRonda(mesa);
            ronda.exigirTurno(jugador.jugadorId(), RondaEnJuego.Fase.ROBAR);
            Carta carta = ronda.tomarDelPozo(jugador.jugadorId());
            registrar(mesa, jugador, TipoEvento.ROBO_POZO, "Toma " + carta.texto() + " del pozo");
            return movimiento(mesa, accionId, jugador.nombre() + " tomó el " + carta.texto() + " del pozo");
        });
    }

    @Override
    public ResultadoAccion descartar(Credencial credencial, Carta carta, String accionId) {
        return ejecutar(credencial, accionId, (mesa, jugador) -> {
            RondaEnJuego ronda = enRonda(mesa);
            ronda.exigirTurno(jugador.jugadorId(), RondaEnJuego.Fase.DESCARTAR);
            ronda.descartar(jugador.jugadorId(), carta);
            registrar(mesa, jugador, TipoEvento.DESCARTE, "Descarta " + carta.texto());
            historial.registrarEvento(EventoJuego.de(mesa.partidaId(), ronda.numero(), ronda.rondaId(),
                    ronda.jugadorEnTurno(), TipoEvento.TURNO_INICIADO, null));
            return movimiento(mesa, accionId, jugador.nombre() + " descartó el " + carta.texto());
        });
    }

    @Override
    public ResultadoAccion solicitarCierre(Credencial credencial, Carta descarte, String accionId) {
        return ejecutar(credencial, accionId, (mesa, jugador) -> {
            ResultadoAccion resultado = cierre.procesar(mesa, jugador, descarte);
            if (resultado.aceptada()) {
                mesa.registrarMovimiento(accionId, jugador.nombre() + " cerró la ronda " + mesa.ronda().numero());
            }
            return resultado;
        });
    }

    @Override
    public ResultadoAccion reordenarMano(Credencial credencial, List<Carta> nuevoOrden) {
        return cola.ejecutar(credencial.codigo(), () -> {
            try {
                Mesa mesa = mesas.obtener(credencial.codigo());
                Participante jugador = mesa.autenticar(credencial.jugadorId(), credencial.token());
                enRonda(mesa).reordenar(jugador.jugadorId(), nuevoOrden);
                return ResultadoAccion.ok("Mano ordenada");
            } catch (AccionInvalida e) {
                return ResultadoAccion.rechazo(e.getMessage());
            }
        });
    }

    @Override
    public ResultadoAccion siguienteRonda(Credencial credencial) {
        return ejecutar(credencial, null, (mesa, jugador) -> {
            ciclo.siguienteRonda(mesa);
            return ResultadoAccion.ok("Nueva ronda repartida");
        });
    }

    @Override
    public ResultadoAccion sincronizar(Credencial credencial) {
        return cola.ejecutar(credencial.codigo(), () -> {
            try {
                Mesa mesa = mesas.obtener(credencial.codigo());
                mesa.autenticar(credencial.jugadorId(), credencial.token());
                if (mesa.ronda() == null) {
                    return ResultadoAccion.rechazo("La partida se está preparando");
                }
                visibilidad.difundir(mesa);
                return ResultadoAccion.ok("Sincronizado");
            } catch (AccionInvalida e) {
                return ResultadoAccion.rechazo(e.getMessage());
            }
        });
    }

    @Override
    public void reportarLatencia(Credencial credencial, long milisegundos) {
        mesas.buscar(credencial.codigo())
                .filter(mesa -> mesa.participantes().stream().anyMatch(p -> p.jugadorId() == credencial.jugadorId()
                        && p.token().equals(credencial.token())))
                .ifPresent(mesa -> {
                    Integer ronda = mesa.ronda() == null ? null : mesa.ronda().numero();
                    historial.registrarEvento(new EventoJuego(mesa.partidaId(), ronda, null,
                            credencial.jugadorId(), TipoEvento.LATENCIA_CLIENTE, null, milisegundos, Instant.now()));
                });
    }

    /**
     * Plantilla comun de las acciones: autentica, aplica la accion dentro de la cola de
     * la mesa y, si fue aceptada, difunde el nuevo estado. Si rompe una regla de turno,
     * queda en la bitacora como accion rechazada (insumo de la metrica de errores).
     */
    private ResultadoAccion ejecutar(Credencial credencial, String accionId, Accion accion) {
        return cola.ejecutar(credencial.codigo(), () -> {
            Mesa mesa = null;
            Participante jugador = null;
            try {
                mesa = mesas.obtener(credencial.codigo());
                jugador = mesa.autenticar(credencial.jugadorId(), credencial.token());
                ResultadoAccion resultado = accion.aplicar(mesa, jugador);
                if (resultado.aceptada()) {
                    visibilidad.difundir(mesa);
                }
                return resultado;
            } catch (AccionInvalida e) {
                registrarRechazo(mesa, jugador, e.getMessage());
                return ResultadoAccion.rechazo(e.getMessage());
            }
        });
    }

    private ResultadoAccion movimiento(Mesa mesa, String accionId, String descripcion) {
        mesa.registrarMovimiento(accionId, descripcion);
        return ResultadoAccion.ok(descripcion);
    }

    private RondaEnJuego enRonda(Mesa mesa) {
        mesa.exigirEstado(Mesa.Estado.EN_RONDA, "La ronda no está en juego");
        return mesa.ronda();
    }

    /** RF-08: si el mazo se acabo, el pozo (menos su carta de encima) se baraja como mazo nuevo. */
    private void reconstruirSiHaceFalta(Mesa mesa, RondaEnJuego ronda) {
        if (!ronda.mazoVacio()) {
            return;
        }
        List<Carta> nuevoMazo = mazo.reconstruirDesdePozo(ronda.retirarPozoSinTope(), aleatorio);
        if (nuevoMazo.isEmpty()) {
            throw new AccionInvalida("No quedan cartas para robar: toma la carta del pozo");
        }
        ronda.reponerMazo(nuevoMazo);
        historial.registrarEvento(EventoJuego.de(mesa.partidaId(), ronda.numero(), ronda.rondaId(), null,
                TipoEvento.MAZO_RECONSTRUIDO, nuevoMazo.size() + " cartas pasan del pozo al mazo"));
    }

    private void registrar(Mesa mesa, Participante jugador, TipoEvento tipo, String detalle) {
        RondaEnJuego ronda = mesa.ronda();
        historial.registrarEvento(EventoJuego.de(mesa.partidaId(), ronda.numero(), ronda.rondaId(),
                jugador.jugadorId(), tipo, detalle));
    }

    private void registrarRechazo(Mesa mesa, Participante jugador, String motivo) {
        if (mesa == null || jugador == null) {
            return;
        }
        Integer ronda = mesa.ronda() == null ? null : mesa.ronda().numero();
        historial.registrarEvento(EventoJuego.de(mesa.partidaId(), ronda, null, jugador.jugadorId(),
                TipoEvento.ACCION_RECHAZADA, motivo));
    }

    @FunctionalInterface
    private interface Accion {
        ResultadoAccion aplicar(Mesa mesa, Participante jugador);
    }
}
