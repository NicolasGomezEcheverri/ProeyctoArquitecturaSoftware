-- Entregable 3: cambios minimos sobre el esquema del Entregable 2.
-- 1) Estado y fechas de la partida, para saber cuando termino y quien la abrio.
ALTER TABLE partida ADD COLUMN estado VARCHAR(20) DEFAULT 'EN_JUEGO' NOT NULL;
ALTER TABLE partida ADD COLUMN creada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE partida ADD COLUMN finalizada_en TIMESTAMP;

-- 2) Cada jugada guarda cuando ocurrio (RF-27) y el texto completo de la accion.
ALTER TABLE turno_jugada ADD COLUMN fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE turno_jugada ALTER COLUMN accion SET DATA TYPE VARCHAR(120);

-- 3) Bitacora de auditoria de la partida. Es la fuente de las metricas de la
--    validacion con usuarios (seccion 3): tiempos de turno, intentos de cierre,
--    acciones rechazadas y latencia medida en el navegador.
CREATE TABLE evento_auditoria (
    id          SERIAL PRIMARY KEY,
    partida_id  INT          NOT NULL REFERENCES partida(id),
    ronda       INT,
    jugador_id  INT          REFERENCES jugador(id),
    tipo        VARCHAR(40)  NOT NULL,
    detalle     VARCHAR(255),
    valor_ms    BIGINT,
    ocurrido_en TIMESTAMP    NOT NULL
);

CREATE INDEX idx_evento_partida ON evento_auditoria (partida_id, id);
