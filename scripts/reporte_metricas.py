#!/usr/bin/env python3
"""Arma el reporte de calidad que publica el pipeline (seccion 4.2 del Entregable 3).

Junta en target/metricas/resumen.md:
  - resultado de las pruebas (fitness functions y pruebas funcionales),
  - complejidad ciclomatica por metodo (Lizard) y el umbral de Checkstyle,
  - metricas de acoplamiento Ca/Ce/I/A/D (generadas por MetricasArquitecturaTest),
  - hallazgos de PMD (Ley de Demeter) y cobertura de JaCoCo.

Uso: python3 scripts/reporte_metricas.py   (despues de "mvn verify")
"""
import csv
import glob
import io
import os
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET = os.path.join(RAIZ, "target")
SALIDA = os.path.join(TARGET, "metricas")
UMBRAL_CC = 10
DESEABLE_CC = 5


def pruebas():
    grupos = defaultdict(lambda: Counter())
    for archivo in glob.glob(os.path.join(TARGET, "surefire-reports", "TEST-*.xml")):
        suite = ET.parse(archivo).getroot()
        nombre = suite.get("name")
        grupo = "Fitness functions (ArchUnit)" if nombre.startswith("architecture.") else (
            "Integración (Spring + BD + WebSocket)" if ".integracion." in nombre else "Unitarias")
        for clave in ("tests", "failures", "errors", "skipped"):
            grupos[grupo][clave] += int(suite.get(clave, 0))
    filas = ["| Grupo | Pruebas | Fallos | Errores |", "|---|---|---|---|"]
    for grupo, c in sorted(grupos.items()):
        filas.append(f"| {grupo} | {c['tests']} | {c['failures']} | {c['errors']} |")
    return "\n".join(filas)


def lizard():
    try:
        salida = subprocess.run([sys.executable, "-m", "lizard", "-l", "java", "--csv",
                                 os.path.join(RAIZ, "src", "main", "java")],
                                capture_output=True, text=True, check=False).stdout
    except FileNotFoundError:
        return None
    metodos = []
    for fila in csv.reader(io.StringIO(salida)):
        if len(fila) < 8:
            continue
        nloc, ccn, archivo, nombre = int(fila[0]), int(fila[1]), fila[6], fila[7]
        modulo = archivo.split("com/apuntado/")[-1].split("/")[0] if "com/apuntado/" in archivo else "?"
        paquete = "/".join(archivo.split("com/apuntado/")[-1].split("/")[:-1])
        metodos.append({"nloc": nloc, "ccn": ccn, "modulo": modulo, "paquete": paquete,
                        "metodo": nombre})
    return metodos


def tabla_complejidad(metodos):
    if not metodos:
        return "_Lizard no está instalado (pip install lizard)._"
    por_modulo = defaultdict(list)
    for m in metodos:
        por_modulo[m["modulo"]].append(m["ccn"])
    filas = ["| Módulo | Métodos | CC promedio | CC máxima | Métodos con CC ≥ 5 | Métodos con CC > 10 |",
             "|---|---|---|---|---|---|"]
    for modulo in sorted(por_modulo):
        v = por_modulo[modulo]
        filas.append(f"| {modulo} | {len(v)} | {sum(v) / len(v):.2f} | {max(v)} | "
                     f"{sum(1 for x in v if x >= DESEABLE_CC)} | {sum(1 for x in v if x > UMBRAL_CC)} |")
    todos = [m["ccn"] for m in metodos]
    filas.append(f"| **Total** | **{len(todos)}** | **{sum(todos) / len(todos):.2f}** | **{max(todos)}** | "
                 f"**{sum(1 for x in todos if x >= DESEABLE_CC)}** | **{sum(1 for x in todos if x > UMBRAL_CC)}** |")
    reglas = sorted((m for m in metodos if m["modulo"] == "rules"), key=lambda m: -m["ccn"])[:8]
    filas += ["", "Métodos de evaluación de ternas/cuartas y de reglas con mayor CC:", "",
              "| Método | Paquete | CC | Líneas |", "|---|---|---|---|"]
    filas += [f"| `{m['metodo']}` | {m['paquete']} | {m['ccn']} | {m['nloc']} |" for m in reglas]
    top = sorted(metodos, key=lambda m: -m["ccn"])[:5]
    filas += ["", "Los cinco métodos más complejos de todo el proyecto:", "",
              "| Método | Paquete | CC |", "|---|---|---|"]
    filas += [f"| `{m['metodo']}` | {m['paquete']} | {m['ccn']} |" for m in top]
    return "\n".join(filas)


def checkstyle():
    ruta = os.path.join(TARGET, "checkstyle-result.xml")
    if not os.path.exists(ruta):
        return "_Sin reporte de Checkstyle._"
    errores = ET.parse(ruta).getroot().iter("error")
    n = sum(1 for _ in errores)
    estado = "✅ sin violaciones" if n == 0 else f"❌ {n} violaciones"
    return (f"Checkstyle (CC ≤ {UMBRAL_CC} por método, sin `System.out` ni `catch` vacíos) sobre todo "
            f"`src/main/java`: {estado}.")


def pmd():
    ruta = os.path.join(TARGET, "pmd.xml")
    if not os.path.exists(ruta):
        return "_Sin reporte de PMD._"
    ns = {"p": "http://pmd.sourceforge.net/report/2.0.0"}
    conteo = Counter()
    demeter = []
    for archivo in ET.parse(ruta).getroot().findall("p:file", ns):
        for v in archivo.findall("p:violation", ns):
            conteo[v.get("rule")] += 1
            if v.get("rule") == "LawOfDemeter":
                demeter.append(f"- `{archivo.get('name').split('com/apuntado/')[-1]}:{v.get('beginline')}`: "
                               f"{v.text.strip()}")
    filas = ["| Regla PMD | Hallazgos |", "|---|---|"]
    filas += [f"| {regla} | {n} |" for regla, n in sorted(conteo.items())]
    if demeter:
        filas += ["", "Hallazgos de Ley de Deméter:", ""] + demeter
    return "\n".join(filas)


def cobertura():
    ruta = os.path.join(TARGET, "site", "jacoco", "jacoco.csv")
    if not os.path.exists(ruta):
        return "_Sin reporte de JaCoCo._"
    por_modulo = defaultdict(lambda: [0, 0])
    with open(ruta, encoding="utf-8") as f:
        for fila in csv.DictReader(f):
            modulo = fila["PACKAGE"].replace("com.apuntado.", "").split(".")[0]
            por_modulo[modulo][0] += int(fila["LINE_COVERED"])
            por_modulo[modulo][1] += int(fila["LINE_COVERED"]) + int(fila["LINE_MISSED"])
    filas = ["| Módulo | Líneas cubiertas |", "|---|---|"]
    total = [0, 0]
    for modulo, (cub, tot) in sorted(por_modulo.items()):
        filas.append(f"| {modulo} | {100 * cub / tot:.0f} % ({cub}/{tot}) |")
        total[0] += cub
        total[1] += tot
    filas.append(f"| **Total** | **{100 * total[0] / total[1]:.0f} %** |")
    return "\n".join(filas)


def main():
    os.makedirs(SALIDA, exist_ok=True)
    modularidad = os.path.join(SALIDA, "modularidad.md")
    partes = [
        "# Reporte de gobernanza y calidad\n",
        "## Pruebas\n", pruebas(), "",
        "## Complejidad ciclomática (Lizard)\n", checkstyle(), "", tabla_complejidad(lizard()), "",
        "## Acoplamiento y distancia a la secuencia principal\n",
        open(modularidad, encoding="utf-8").read().split("\n", 3)[-1] if os.path.exists(modularidad)
        else "_Ejecuta MetricasArquitecturaTest._", "",
        "## PMD: Ley de Deméter, acoplamiento y complejidad\n", pmd(), "",
        "## Cobertura de pruebas (JaCoCo)\n", cobertura(), "",
    ]
    texto = "\n".join(partes)
    with open(os.path.join(SALIDA, "resumen.md"), "w", encoding="utf-8") as f:
        f.write(texto)
    resumen_gh = os.environ.get("GITHUB_STEP_SUMMARY")
    if resumen_gh:
        with open(resumen_gh, "a", encoding="utf-8") as f:
            f.write(texto)
    print(texto)


if __name__ == "__main__":
    main()
