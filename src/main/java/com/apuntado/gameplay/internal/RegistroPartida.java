package com.apuntado.gameplay.internal;

import com.apuntado.gameplay.api.Participante;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Escrituras de gameplay que no son el cierre: crear la partida y abrir rondas. */
@Service
class RegistroPartida {

    private final PartidaRepository partidas;
    private final ParticipacionRepository participaciones;
    private final RondaRepository rondas;

    RegistroPartida(PartidaRepository partidas, ParticipacionRepository participaciones, RondaRepository rondas) {
        this.partidas = partidas;
        this.participaciones = participaciones;
        this.rondas = rondas;
    }

    @Transactional
    public int crearPartida(String codigo, String modalidad, int umbral, List<Participante> ordenTurno) {
        PartidaEntity partida = partidas.save(new PartidaEntity(codigo, modalidad, umbral));
        for (int i = 0; i < ordenTurno.size(); i++) {
            participaciones.save(new ParticipacionEntity(partida.getId(), ordenTurno.get(i).jugadorId(), i + 1));
        }
        return partida.getId();
    }

    @Transactional
    public int abrirRonda(int partidaId, int numero) {
        return rondas.save(new RondaEntity(partidaId, numero)).getId();
    }

    @Transactional(readOnly = true)
    public boolean codigoEnUso(String codigo) {
        return partidas.existsByCodigo(codigo);
    }

    @Transactional(readOnly = true)
    public Integer partidaIdDe(String codigo) {
        return partidas.findByCodigo(codigo).map(PartidaEntity::getId).orElse(null);
    }
}
