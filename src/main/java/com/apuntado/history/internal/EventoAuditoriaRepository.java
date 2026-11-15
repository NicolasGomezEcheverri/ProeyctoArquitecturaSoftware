package com.apuntado.history.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoriaEntity, Integer> {

    List<EventoAuditoriaEntity> findByPartidaIdOrderByIdAsc(int partidaId);
}
