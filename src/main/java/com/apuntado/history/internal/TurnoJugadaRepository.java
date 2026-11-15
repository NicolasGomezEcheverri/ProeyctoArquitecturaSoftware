package com.apuntado.history.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface TurnoJugadaRepository extends JpaRepository<TurnoJugadaEntity, Integer> {

    @Query("select coalesce(max(t.secuencia), 0) from TurnoJugadaEntity t where t.rondaId = :rondaId")
    int ultimaSecuencia(int rondaId);
}
