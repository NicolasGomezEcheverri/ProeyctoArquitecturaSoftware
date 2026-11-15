# Proyecto Apuntado - Arquitectura de Software

[![CI · Fitness Functions](https://github.com/NicolasGomezEcheverri/ProeyctoArquitecturaSoftware/actions/workflows/ci.yml/badge.svg)](https://github.com/NicolasGomezEcheverri/ProeyctoArquitecturaSoftware/actions/workflows/ci.yml)

MVP del juego de cartas **Apuntado** con sus dos variantes:

- **Clásica (4-3-3):** se cierra con 1 cuarta, 2 ternas y 1 carta de descarte.
- **Apuntado a 101:** se cierra con 3 ternas si lo que sobra suma menos de 5 puntos. Las penalizaciones se acumulan entre rondas y queda eliminado quien llega a 101 (el umbral se puede configurar).

El sistema es un **Monolito Modular** (Entregable 2, ADR-03). Corre como un solo servidor Spring Boot con una base PostgreSQL, y los navegadores se conectan en tiempo real por WebSocket.

## Jugar en dos minutos

### Opción 1: Docker (recomendada)

```bash
git clone https://github.com/NicolasGomezEcheverri/ProeyctoArquitecturaSoftware.git
cd ProeyctoArquitecturaSoftware
docker compose up --build
```

Abre <http://localhost:8080>. Desde otro dispositivo de la misma red usa `http://<ip-de-tu-equipo>:8080`.

### Opción 2: sin Docker (base en memoria)

Requiere JDK 17 o superior y Maven 3.8 o superior.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

El perfil `local` usa H2 en memoria con las mismas migraciones de PostgreSQL, así que no hay que instalar ninguna base de datos. Los datos se pierden al apagar el servidor.

### Opción 3: en la nube (Render)

El archivo `render.yaml` es un *Blueprint* de Render. En render.com ve a **New → Blueprint** y elige este repositorio. Render crea la base PostgreSQL y el servicio web a partir del `Dockerfile`.

### Cómo se arma una partida

1. Una persona crea la mesa: escribe su nombre, elige la modalidad, el número de jugadores (2 a 4) y el límite de puntos.
2. Comparte el código de 5 letras o el enlace de invitación.
3. Los demás entran con ese código. La partida arranca sola cuando se llena la mesa, o antes si quien la creó toca **Empezar ya**.

Para probar solo, abre varias pestañas: cada pestaña cuenta como un jugador distinto.

## Estructura del código

```
src/main/java/com/apuntado
├── presentation/   REST de la sala, WebSocket del juego, StatePropagation
├── session/        sala de espera, validación de la configuración (RF-01 a RF-04)
├── gameplay/       mediador: turnos, cierre, ciclo de la partida, visibilidad de manos
├── rules/          api/ (ReglaJuego), registry/ (ServiceLoader), evaluacion/ (ternas y cuartas),
│                   clasica/ y apuntado/ (las dos variantes, como plug-ins)
├── deck/           mazo y pozo
├── scoring/        marcador, penalizaciones y eliminación
├── history/        bitácora de auditoría y métricas de la validación
├── shared/         Carta, Palo, Valor, Mano
└── app/            raíz de composición (arranque de Spring)
src/main/resources
├── db/migration/   esquema PostgreSQL (Flyway): V1 del Entregable 2, V2 de auditoría
└── static/         cliente web (HTML, CSS y JavaScript, sin frameworks)
tests/              fitness functions (architecture/) y pruebas unitarias y de integración
docs/               diagramas, protocolo y evidencias de la validación con usuarios
```

Cada módulo expone un paquete `api` y esconde su `internal`. Las variantes de reglas no se conocen entre sí y `gameplay` solo ve el contrato `ReglaJuego`. Para agregar una variante nueva basta con crear la clase en `rules/` y registrarla en `META-INF/services/com.apuntado.rules.api.ReglaJuego`.

## Pruebas y gobernanza

```bash
mvn clean verify                     # todo: pruebas, fitness functions, Checkstyle, PMD y JaCoCo
python3 scripts/reporte_metricas.py  # resumen en target/metricas/resumen.md (requiere: pip install lizard)
```

| Qué se verifica | Herramienta | Dónde |
|---|---|---|
| Aislamiento de capas, plug-ins sin acoplamiento cruzado, ausencia de ciclos | ArchUnit | `tests/architecture/ArchitectureFitnessTest.java` |
| Ca, Ce, inestabilidad, abstracción y distancia a la secuencia principal | ArchUnit (metrics) | `tests/architecture/MetricasArquitecturaTest.java` |
| Complejidad ciclomática ≤ 10, sin `System.out` ni `catch` vacíos | Checkstyle (rompe el build) | `config/checkstyle/checkstyle-cc.xml` |
| Ley de Deméter, acoplamiento entre objetos | PMD (reporte) | `config/pmd/reglas-calidad.xml` |
| Cobertura de pruebas | JaCoCo | `target/site/jacoco/index.html` |

El pipeline `.github/workflows/ci.yml` ejecuta todo esto en cada *push* y *pull request*, hace una prueba de humo contra PostgreSQL y publica los reportes como artefacto.

## Bitácora y métricas de una partida

El servidor guarda cada acción en la tabla `evento_auditoria`. Con el código de la partida se consultan:

- `/metricas.html?codigo=XXXXX`: tabla por jugador (turnos, segundos por turno, cierres intentados y aceptados, acciones rechazadas, latencia).
- `/api/partidas/XXXXX/bitacora.csv` y `/api/partidas/XXXXX/metricas.csv`: los mismos datos en CSV.

Estos archivos son la evidencia de la validación con usuarios (`docs/validacion/`).

## Desarrollo en IDE

Abre la carpeta raíz (donde está `pom.xml`) con IntelliJ IDEA, Eclipse o VS Code. La clase principal es `com.apuntado.app.ApuntadoApplication`. Para usar la base en memoria, activa el perfil `local`.
