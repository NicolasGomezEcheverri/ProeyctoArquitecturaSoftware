#!/usr/bin/env python3
"""Analisis de la validacion con usuarios (seccion 3.3 del Entregable 3).

Cruza tres fuentes que se recogen durante las sesiones:
  - docs/validacion/evidencias/metricas-*.csv y bitacora-*.csv  (exportados por el juego)
  - docs/validacion/datos/observaciones.csv                     (hoja del observador)
  - docs/validacion/datos/cuestionario.csv                      (SUS y preguntas E1 a E4)

y escribe docs/validacion/resultados.md con la tabla consolidada, el SUS y las metricas.
Los participantes se reconocen por su codigo (U01, U02...), que es el nombre que
usaron dentro del juego. Las filas con otro nombre (por ejemplo EQ) se ignoran.

Uso: python3 scripts/analisis_validacion.py [carpeta_validacion]
"""
import csv
import glob
import math
import os
import re
import sys
from collections import defaultdict

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CODIGO = re.compile(r"^U\d{2}$")
META_LATENCIA_MS = 200
META_SUS = 68


def leer(ruta):
    if not os.path.exists(ruta):
        return []
    with open(ruta, encoding="utf-8-sig") as f:
        return [fila for fila in csv.DictReader(f, delimiter=";") if any(v.strip() for v in fila.values() if v)]


def numero(texto, defecto=None):
    try:
        return float(str(texto).replace(",", "."))
    except (TypeError, ValueError):
        return defecto


def p95(valores):
    if not valores:
        return None
    ordenados = sorted(valores)
    return ordenados[max(math.ceil(0.95 * len(ordenados)) - 1, 0)]


def metricas_del_sistema(carpeta):
    por_usuario = defaultdict(lambda: {"modalidades": set(), "turnos": 0, "seg_total": 0.0,
                                       "intentos": 0, "aceptados": 0, "rechazados": 0, "acciones_rech": 0,
                                       "partidas": set()})
    for ruta in glob.glob(os.path.join(carpeta, "evidencias", "metricas-*.csv")):
        partida = os.path.basename(ruta)[len("metricas-"):-4]
        for f in leer(ruta):
            uid = f["jugador"].strip().upper()
            if not CODIGO.match(uid):
                continue
            u = por_usuario[uid]
            turnos = int(numero(f["turnos"], 0))
            u["modalidades"].add(f["modalidad"])
            u["turnos"] += turnos
            u["seg_total"] += turnos * numero(f["seg_promedio_turno"], 0)
            u["intentos"] += int(numero(f["cierres_intentados"], 0))
            u["aceptados"] += int(numero(f["cierres_aceptados"], 0))
            u["rechazados"] += int(numero(f["cierres_rechazados"], 0))
            u["acciones_rech"] += int(numero(f["acciones_rechazadas"], 0))
            u["partidas"].add(partida)
    latencias = defaultdict(list)
    for ruta in glob.glob(os.path.join(carpeta, "evidencias", "bitacora-*.csv")):
        for f in leer(ruta):
            uid = (f.get("jugador") or "").strip().upper()
            if f.get("tipo") == "LATENCIA_CLIENTE" and CODIGO.match(uid) and numero(f.get("valor_ms")) is not None:
                latencias[uid].append(numero(f["valor_ms"]))
    return por_usuario, latencias


def sus(fila):
    try:
        respuestas = [int(fila[f"sus{i}"]) for i in range(1, 11)]
    except (KeyError, ValueError):
        return None
    suma = sum((r - 1) if i % 2 == 0 else (5 - r) for i, r in enumerate(respuestas))
    return suma * 2.5


def adjetivo_sus(valor):
    if valor >= 85.5:
        return "excelente"
    if valor >= 71.4:
        return "bueno"
    if valor >= 50.9:
        return "aceptable"
    return "pobre"


def nombre_modalidad(modalidades):
    nombres = {"CLASICA": "Clásica (4-3-3)", "APUNTADO": "Apuntado (101)"}
    if len(modalidades) > 1:
        return "Ambas"
    return nombres.get(next(iter(modalidades)), "—") if modalidades else "—"


def tabla_consolidada(usuarios, sistema, observaciones, cuestionario):
    filas = ["| ID Usuario | Modalidad Probada | ¿Logró Cerrar Ronda Válida? | Errores / Confusiones Detectadas "
             "| Tiempo Promedio por Turno | Calificación Satisfacción (1-5) |",
             "|---|---|---|---|---|---|"]
    for uid in usuarios:
        s = sistema.get(uid)
        obs = [o for o in observaciones if o["id_usuario"].strip().upper() == uid]
        modalidades = s["modalidades"] if s else {o["modalidad"] for o in obs if o.get("modalidad")}
        t4 = [o for o in obs if o["tarea"].strip().upper() == "T4"]
        logro = (s and s["aceptados"] > 0) or any(o["resultado"].strip().lower() == "si" for o in t4)
        confusiones = "; ".join(dict.fromkeys(o["errores_o_confusiones"].strip() for o in obs
                                              if o.get("errores_o_confusiones", "").strip())) or "—"
        seg = f"{s['seg_total'] / s['turnos']:.0f} s" if s and s["turnos"] else "—"
        c = cuestionario.get(uid)
        satis = f"{int(c['e4_satisfaccion'])} / 5" if c and c.get("e4_satisfaccion") else "—"
        filas.append(f"| {uid} | {nombre_modalidad(modalidades)} | {'Sí' if logro else 'No'} | {confusiones} "
                     f"| {seg} | {satis} |")
    return "\n".join(filas)


def tabla_sistema(usuarios, sistema, latencias):
    filas = ["| ID | Partidas | Turnos | Cierres intentados | Aceptados | Rechazados | Acciones rechazadas "
             "| Latencia prom. | Latencia p95 |", "|---|---|---|---|---|---|---|---|---|"]
    for uid in usuarios:
        s = sistema.get(uid)
        if not s:
            continue
        lat = latencias.get(uid, [])
        prom = f"{sum(lat) / len(lat):.0f} ms" if lat else "—"
        filas.append(f"| {uid} | {len(s['partidas'])} | {s['turnos']} | {s['intentos']} | {s['aceptados']} "
                     f"| {s['rechazados']} | {s['acciones_rech']} | {prom} | "
                     f"{f'{p95(lat):.0f} ms' if lat else '—'} |")
    return "\n".join(filas)


def tabla_sus(usuarios, cuestionario):
    filas = ["| ID | SUS (0-100) | E1 conteo | E2 rapidez | E3 motivo del rechazo | E4 satisfacción |",
             "|---|---|---|---|---|---|"]
    puntajes = []
    for uid in usuarios:
        c = cuestionario.get(uid)
        if not c:
            continue
        valor = sus(c)
        if valor is not None:
            puntajes.append(valor)
        filas.append(f"| {uid} | {valor if valor is not None else '—'} | {c.get('e1_conteo', '—')} "
                     f"| {c.get('e2_latencia', '—')} | {c.get('e3_motivo_rechazo', '—')} "
                     f"| {c.get('e4_satisfaccion', '—')} |")
    return "\n".join(filas), puntajes


def promedio_pregunta(cuestionario, clave):
    valores = [numero(c.get(clave)) for c in cuestionario.values() if numero(c.get(clave)) is not None]
    return f"{sum(valores) / len(valores):.1f}" if valores else "—"


def tareas(observaciones, usuarios):
    filas = ["| Tarea | Participantes | Logrado sin ayuda | Con ayuda | No logrado | Tiempo mediano |",
             "|---|---|---|---|---|---|"]
    for t in ["T1", "T2", "T3", "T4", "T5"]:
        obs = [o for o in observaciones if o["tarea"].strip().upper() == t
               and o["id_usuario"].strip().upper() in usuarios]
        if not obs:
            continue
        res = [o["resultado"].strip().lower() for o in obs]
        tiempos = sorted(numero(o["tiempo_s"]) for o in obs if numero(o.get("tiempo_s")) is not None)
        mediana = f"{tiempos[len(tiempos) // 2]:.0f} s" if tiempos else "—"
        filas.append(f"| {t} | {len(obs)} | {res.count('si')} | {res.count('con_ayuda')} | {res.count('no')} "
                     f"| {mediana} |")
    return "\n".join(filas)


def comentarios(cuestionario):
    lineas = []
    for uid, c in sorted(cuestionario.items()):
        for clave, titulo in (("lo_mas_confuso", "Lo más confuso"), ("que_cambiaria", "Qué cambiaría")):
            if c.get(clave, "").strip():
                lineas.append(f"- **{uid}, {titulo.lower()}:** “{c[clave].strip()}”")
    return "\n".join(lineas) or "_Sin comentarios registrados._"


def main():
    carpeta = sys.argv[1] if len(sys.argv) > 1 else os.path.join(RAIZ, "docs", "validacion")
    observaciones = leer(os.path.join(carpeta, "datos", "observaciones.csv"))
    cuestionario = {f["id_usuario"].strip().upper(): f
                    for f in leer(os.path.join(carpeta, "datos", "cuestionario.csv"))
                    if CODIGO.match(f["id_usuario"].strip().upper())}
    sistema, latencias = metricas_del_sistema(carpeta)
    usuarios = sorted(set(sistema) | set(cuestionario)
                      | {o["id_usuario"].strip().upper() for o in observaciones
                         if CODIGO.match(o["id_usuario"].strip().upper())})
    salida = os.path.join(carpeta, "resultados.md")
    if not usuarios:
        texto = "# Resultados de la validación\n\nTodavía no hay datos en `datos/` ni en `evidencias/`.\n"
        open(salida, "w", encoding="utf-8").write(texto)
        print(texto)
        return

    intentaron = [u for u in usuarios if u in sistema and sistema[u]["intentos"] > 0]
    exito = [u for u in intentaron if sistema[u]["aceptados"] > 0]
    todas_lat = [v for u in usuarios for v in latencias.get(u, [])]
    sus_tabla, puntajes = tabla_sus(usuarios, cuestionario)
    sus_prom = sum(puntajes) / len(puntajes) if puntajes else None

    resumen = [f"- Participantes: **{len(usuarios)}** ({', '.join(usuarios)})."]
    if intentaron:
        resumen.append(f"- Tasa de éxito en cierre: **{len(exito)} de {len(intentaron)}** participantes "
                       f"({100 * len(exito) / len(intentaron):.0f} %) cerraron al menos una ronda válida.")
    if todas_lat:
        estado = "cumple" if p95(todas_lat) < META_LATENCIA_MS else "no cumple"
        resumen.append(f"- Latencia medida en el navegador: promedio **{sum(todas_lat) / len(todas_lat):.0f} ms**, "
                       f"p95 **{p95(todas_lat):.0f} ms** ({len(todas_lat)} muestras); {estado} la meta "
                       f"de {META_LATENCIA_MS} ms.")
    if sus_prom is not None:
        resumen.append(f"- SUS promedio: **{sus_prom:.1f}** ({adjetivo_sus(sus_prom)}; la referencia es {META_SUS}).")
    resumen.append(f"- Promedios del cuestionario (1 a 5): conteo {promedio_pregunta(cuestionario, 'e1_conteo')}, "
                   f"rapidez {promedio_pregunta(cuestionario, 'e2_latencia')}, motivo del rechazo "
                   f"{promedio_pregunta(cuestionario, 'e3_motivo_rechazo')}, satisfacción "
                   f"{promedio_pregunta(cuestionario, 'e4_satisfaccion')}.")

    texto = "\n".join([
        "# Resultados de la validación con usuarios", "",
        "Generado por `scripts/analisis_validacion.py` a partir de la bitácora del juego, "
        "la hoja de observación y el cuestionario.", "",
        "## Resumen", "", *resumen, "",
        "## Tabla consolidada de métricas de usuario", "", tabla_consolidada(usuarios, sistema, observaciones,
                                                                             cuestionario), "",
        "## Métricas registradas por el sistema", "", tabla_sistema(usuarios, sistema, latencias), "",
        "## Tareas", "", tareas(observaciones, usuarios), "",
        "## Cuestionario", "", sus_tabla, "",
        "## Comentarios textuales", "", comentarios(cuestionario), "",
    ])
    with open(salida, "w", encoding="utf-8") as f:
        f.write(texto)
    print(texto)


if __name__ == "__main__":
    main()
