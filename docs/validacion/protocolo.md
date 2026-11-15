# Protocolo de validación con usuarios reales

Entregable 3, sección 3. Este documento define cómo se hace la prueba; los resultados van en `resultados.md`, que genera `scripts/analisis_validacion.py` a partir de los datos recogidos.

## 1. Qué queremos saber

| Pregunta | Métrica | De dónde sale |
|---|---|---|
| ¿Una persona que no participó en el desarrollo logra cerrar una ronda válida sin ayuda? | Tasa de éxito en cierre: cierres aceptados / participantes que lo intentaron | Bitácora (`CIERRE_ACEPTADO`) y hoja de observación |
| ¿Entiende por qué el sistema rechaza un cierre? | Cierres rechazados por participante y si después corrigió la jugada; respuesta a la pregunta E3 | Bitácora (`CIERRE_RECHAZADO`), observación y cuestionario |
| ¿Se equivoca con las reglas de turno? | Acciones rechazadas (robar dos veces, jugar fuera de turno) | Bitácora (`ACCION_RECHAZADA`) |
| ¿El conteo de puntos es claro? | Pregunta E1 (1 a 5) y comentarios | Cuestionario |
| ¿El juego responde rápido? | Latencia medida en el navegador (promedio y p95) y latencia percibida (E2) | Bitácora (`LATENCIA_CLIENTE`) y cuestionario |
| ¿Qué tan usable es en general? | Puntaje SUS (0 a 100) y satisfacción de 1 a 5 | Cuestionario |
| ¿Cuánto tarda un turno? | Segundos promedio por turno | Bitácora (`TURNO_INICIADO` → `DESCARTE`) |

Metas de referencia, tomadas de los drivers del Entregable 1:

- 100 % de los participantes cierran al menos una ronda válida.
- Latencia p95 por debajo de 200 ms (Consistencia de datos).
- SUS de 68 o más (promedio de la industria).

## 2. Participantes

- **Cantidad:** de 3 a 5 personas, ajenas al equipo de desarrollo (amigos, familiares, compañeros de otros cursos).
- **Mezcla buscada:** al menos una persona que ya juegue Apuntado o Rummy y al menos una que nunca lo haya jugado.
- **Identificación:** cada participante recibe un código (U01, U02…). Ese código es el nombre que escribe al entrar a la mesa, para que la bitácora quede ligada a su hoja sin guardar nombres reales.
- **Consentimiento:** antes de empezar se lee `consentimiento.md` y se anota la aceptación (verbal o firmada).

## 3. Montaje

- Un portátil corre el servidor con `docker compose up --build` (o `mvn spring-boot:run -Dspring-boot.run.profiles=local`).
- Cada participante usa su celular o un computador conectado a la misma red Wi-Fi y entra a `http://<ip-del-portátil>:8080`. Si no hay red disponible, se usan pestañas distintas de un mismo equipo.
- Las partidas son de 2 a 4 jugadores. Si hay pocos participantes, un integrante del equipo completa la mesa **sin dar indicaciones** y su código es `EQ`. Sus datos se excluyen del análisis.
- Se graba la pantalla del participante (con su permiso) o se toman fotos y capturas en los momentos clave: cierre rechazado, fin de ronda y fin de partida.

## 4. Roles del equipo

| Rol | Qué hace |
|---|---|
| Moderador | Lee el guion, entrega las tareas una por una y no explica la interfaz |
| Observador | Llena `plantillas/observaciones.csv`: tiempos, dudas y errores que ve |
| Técnico | Vigila el servidor y exporta la bitácora y las métricas al final de cada partida |

## 5. Tareas (sin asistencia técnica)

Antes de la primera partida el participante puede leer la ayuda del juego (botón **¿Cómo se juega?**) durante 2 minutos como máximo. Después, el moderador solo puede repetir la tarea, no explicar cómo hacerla.

| Id | Tarea | Se considera lograda cuando |
|---|---|---|
| T1 | Crear o unirse a una partida y elegir la modalidad indicada (Clásica o Apuntado) | Aparece en la mesa con la modalidad correcta |
| T2 | Jugar 3 turnos completos: robar del mazo o del pozo, ordenar la mano y descartar | La bitácora registra 3 descartes suyos |
| T3 | Intentar a propósito un cierre que no cumple la regla | El sistema lo rechaza y el participante explica con sus palabras por qué |
| T4 | Cerrar una ronda válida | La bitácora registra `CIERRE_ACEPTADO` con su código |
| T5 | Solo en Apuntado: jugar rondas seguidas hasta ver la acumulación de puntos y el fin de la partida | El participante identifica quién quedó eliminado y por qué |

Para T4 se le da tiempo suficiente; si después de 10 minutos de ronda no lo ha logrado, se registra como "no logrado" y se sigue. Para no depender de la suerte del reparto, se pueden usar partidas con umbral bajo (por ejemplo 50) en T5.

## 6. Datos que se guardan

1. **Bitácora del sistema:** al final de cada partida el técnico descarga `/api/partidas/<código>/bitacora.csv` y `/metricas.csv` y los guarda en `evidencias/` como `bitacora-<código>.csv` y `metricas-<código>.csv`.
2. **Observación:** una fila por participante y tarea en `datos/observaciones.csv` (plantilla en `plantillas/`).
3. **Cuestionario:** al terminar, cada participante responde `cuestionario-post.md` y sus respuestas se pasan a `datos/cuestionario.csv`.
4. **Fotos, capturas y videos:** en `evidencias/` o en un enlace de Drive anotado en `evidencias/README.md`.

## 7. Análisis

```bash
python3 scripts/analisis_validacion.py
```

El script cruza las tres fuentes y escribe `docs/validacion/resultados.md` con:

- la tabla consolidada de la sección 3.3 de la guía (ID, modalidad, si cerró una ronda válida, errores o confusiones, tiempo promedio por turno y satisfacción);
- el SUS por participante y el promedio;
- la tasa de éxito en cierre y la latencia (promedio y p95).

Los comentarios de la columna "Errores / confusiones" salen de la hoja de observación: el script no los inventa.

## 8. Limitaciones que hay que declarar en el informe

- La muestra es pequeña (3 a 5 personas): sirve para encontrar problemas de usabilidad, no para generalizar.
- La latencia medida en una red local no representa una conexión por internet.
- Los participantes son conocidos del equipo, lo que puede inflar la satisfacción. Por eso pesa más lo que se observa que lo que se declara.
