package com.apuntado.gameplay.internal;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PartidaRepository extends JpaRepository<PartidaEntity, Integer> {

    Optional<PartidaEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    /** Segunda barrera: SELECT ... FOR UPDATE sobre la partida durante el cierre. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PartidaEntity p where p.id = :id")
    Optional<PartidaEntity> bloquearPorId(@Param("id") int id);
}
