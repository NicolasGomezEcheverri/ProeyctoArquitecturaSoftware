package com.apuntado.scoring.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface PuntajeRepository extends JpaRepository<PuntajeAcumuladoEntity, PuntajeId> {
}
