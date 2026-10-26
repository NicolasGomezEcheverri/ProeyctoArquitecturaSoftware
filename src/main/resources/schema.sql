CREATE TABLE jugador (
    id SERIAL PRIMARY KEY,
    nombre_usuario VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE partida (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    modalidad_cierre VARCHAR(20) NOT NULL,
    ganador_id INT REFERENCES jugador(id),
    version INT DEFAULT 0,
    umbral_puntos INT DEFAULT 101,
    monto_apuesta DECIMAL(10,2),
    pinta VARCHAR(20)
);

CREATE TABLE participacion (
    partida_id INT REFERENCES partida(id),
    jugador_id INT REFERENCES jugador(id),
    orden_turno INT NOT NULL,
    reingresos INT DEFAULT 0 CHECK (reingresos >= 0),
    PRIMARY KEY (partida_id, jugador_id),
    UNIQUE (partida_id, orden_turno)
);

CREATE TABLE ronda (
    id SERIAL PRIMARY KEY,
    partida_id INT NOT NULL,
    numero INT NOT NULL,
    tipo_cierre VARCHAR(20),
    cerrador_id INT,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    UNIQUE (partida_id, numero),
    FOREIGN KEY (partida_id) REFERENCES partida(id),
    FOREIGN KEY (partida_id, cerrador_id) REFERENCES participacion(partida_id, jugador_id)
);

CREATE TABLE mano_jugador (
    id SERIAL PRIMARY KEY,
    ronda_id INT NOT NULL REFERENCES ronda(id),
    jugador_id INT NOT NULL REFERENCES jugador(id),
    UNIQUE (ronda_id, jugador_id)
);

CREATE TABLE carta (
    id SERIAL PRIMARY KEY,
    palo VARCHAR(20) NOT NULL,
    valor VARCHAR(20) NOT NULL,
    UNIQUE (palo, valor)
);

CREATE TABLE carta_en_ronda (
    ronda_id INT NOT NULL REFERENCES ronda(id),
    carta_id INT NOT NULL REFERENCES carta(id),
    mano_id INT REFERENCES mano_jugador(id),
    ubicacion VARCHAR(20) NOT NULL CHECK (ubicacion IN ('MAZO', 'POZO', 'MANO')),
    posicion INT,
    PRIMARY KEY (ronda_id, carta_id)
);

CREATE TABLE turno_jugada (
    id SERIAL PRIMARY KEY,
    ronda_id INT NOT NULL REFERENCES ronda(id),
    secuencia INT NOT NULL,
    jugador_id INT NOT NULL REFERENCES jugador(id),
    accion VARCHAR(50) NOT NULL,
    UNIQUE (ronda_id, secuencia)
);

CREATE TABLE puntaje_acumulado (
    ronda_id INT NOT NULL REFERENCES ronda(id),
    jugador_id INT NOT NULL REFERENCES jugador(id),
    penalizacion INT NOT NULL CHECK (penalizacion >= 0),
    acumulado INT NOT NULL CHECK (acumulado >= 0),
    PRIMARY KEY (ronda_id, jugador_id)
);

CREATE TABLE movimiento_apuesta (
    id SERIAL PRIMARY KEY,
    ronda_id INT NOT NULL REFERENCES ronda(id),
    pagador_id INT NOT NULL REFERENCES jugador(id),
    receptor_id INT NOT NULL REFERENCES jugador(id),
    monto DECIMAL(10,2) NOT NULL CHECK (monto > 0),
    CHECK (pagador_id != receptor_id)
);