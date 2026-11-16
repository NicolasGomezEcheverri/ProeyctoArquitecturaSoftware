# Evidencias técnicas

Estas evidencias las generó el equipo con pruebas automatizadas. **No son la validación con usuarios**, que está en `../validacion/`.

| Archivo | Qué muestra |
|---|---|
| `capturas/` | Capturas de la interfaz tomadas con Playwright mientras varios navegadores jugaban: inicio, mesa de 4 jugadores, animaciones de robo, cierre y reparto, fin de ronda, menú con el marcador y vista en celular. |
| `carga-websocket-postgres-10-mesas.json` | Resultado de `scripts/carga_websocket.mjs` contra el servidor con PostgreSQL: 10 mesas de 4 jugadores simulados (40 conexiones) jugando 200 turnos cada una. Mide el tiempo entre enviar una acción y recibir la mesa actualizada. |
| `fitness-function-detecta-violacion.txt` | Salida de ArchUnit cuando se metió a propósito una dependencia de `rules.clasica` hacia `rules.apuntado`: el build falla con las reglas 2A y 2F. Después se revirtió el cambio. |

La prueba de carga se corrió en la misma máquina que el servidor, así que mide sobre todo el tiempo de procesamiento y persistencia, no la red.
