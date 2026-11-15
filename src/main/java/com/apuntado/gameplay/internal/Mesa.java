package com.apuntado.gameplay.internal;

import com.apuntado.gameplay.api.Participante;
import com.apuntado.gameplay.api.ResumenCierre;
import com.apuntado.rules.api.ReglaJuego;
import java.util.List;

/** Partida en curso: configuracion, ronda actual y lo ultimo que paso en la mesa. */
final class Mesa {

    enum Estado { EN_RONDA, RONDA_TERMINADA, FINALIZADA }

    private final int partidaId;
    private final String codigo;
    private final ReglaJuego regla;
    private final int umbral;
    private final List<Participante> participantes;
    private Estado estado = Estado.EN_RONDA;
    private RondaEnJuego ronda;
    private int posicionAbridor;
    private ResumenCierre ultimoCierre;
    private Integer ganadorId;
    private long secuencia;
    private String ultimaAccionId;
    private String ultimoMovimiento = "";

    Mesa(int partidaId, String codigo, ReglaJuego regla, int umbral, List<Participante> participantes,
            int posicionAbridor) {
        this.partidaId = partidaId;
        this.codigo = codigo;
        this.regla = regla;
        this.umbral = umbral;
        this.participantes = List.copyOf(participantes);
        this.posicionAbridor = posicionAbridor;
    }

    Participante autenticar(int jugadorId, String token) {
        return participantes.stream()
                .filter(p -> p.jugadorId() == jugadorId && p.token().equals(token))
                .findFirst()
                .orElseThrow(() -> new AccionInvalida("No perteneces a esta mesa"));
    }

    String nombreDe(Integer jugadorId) {
        return participantes.stream()
                .filter(p -> jugadorId != null && p.jugadorId() == jugadorId)
                .map(Participante::nombre)
                .findFirst()
                .orElse("—");
    }

    void exigirEstado(Estado esperado, String mensaje) {
        if (estado != esperado) {
            throw new AccionInvalida(mensaje);
        }
    }

    void empezarRonda(RondaEnJuego nueva) {
        this.ronda = nueva;
        this.estado = Estado.EN_RONDA;
    }

    void terminarRonda(ResumenCierre resumen, Integer ganador) {
        this.ultimoCierre = resumen;
        this.ganadorId = ganador;
        this.estado = ganador == null ? Estado.RONDA_TERMINADA : Estado.FINALIZADA;
    }

    /** La siguiente ronda la abre el siguiente jugador activo despues del abridor anterior. */
    int siguienteAbridor(List<Integer> activos) {
        for (int paso = 1; paso <= participantes.size(); paso++) {
            int candidato = (posicionAbridor + paso) % participantes.size();
            if (activos.contains(participantes.get(candidato).jugadorId())) {
                posicionAbridor = candidato;
                return participantes.get(candidato).jugadorId();
            }
        }
        throw new IllegalStateException("No quedan jugadores activos");
    }

    int abridorActual() {
        return participantes.get(posicionAbridor).jugadorId();
    }

    void registrarMovimiento(String accionId, String descripcion) {
        secuencia++;
        ultimaAccionId = accionId;
        if (descripcion != null) {
            ultimoMovimiento = descripcion;
        }
    }

    int partidaId() {
        return partidaId;
    }

    String codigo() {
        return codigo;
    }

    ReglaJuego regla() {
        return regla;
    }

    int umbral() {
        return umbral;
    }

    List<Participante> participantes() {
        return participantes;
    }

    Estado estado() {
        return estado;
    }

    RondaEnJuego ronda() {
        return ronda;
    }

    ResumenCierre ultimoCierre() {
        return ultimoCierre;
    }

    Integer ganadorId() {
        return ganadorId;
    }

    long secuencia() {
        return secuencia;
    }

    String ultimaAccionId() {
        return ultimaAccionId;
    }

    String ultimoMovimiento() {
        return ultimoMovimiento;
    }
}
