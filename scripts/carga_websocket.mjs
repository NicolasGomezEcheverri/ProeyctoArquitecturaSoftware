// Prueba tecnica de latencia (no reemplaza la validacion con usuarios).
// Abre una mesa de 4 jugadores simulados, juega N turnos por WebSocket y mide el
// tiempo entre enviar una accion y recibir la mesa actualizada (mismo calculo que
// hace el navegador). Requiere Node 22 o superior (WebSocket nativo).
//
// Uso: node scripts/carga_websocket.mjs [http://localhost:8080] [turnos=200] [mesas=1]
const BASE = process.argv[2] || "http://localhost:8080";
const TURNOS = Number(process.argv[3] || 200);
const MESAS = Number(process.argv[4] || 1);
const WS_BASE = BASE.replace(/^http/, "ws");

async function post(ruta, cuerpo) {
  const r = await fetch(BASE + ruta, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(cuerpo) });
  if (!r.ok) throw new Error(`${ruta}: ${r.status} ${await r.text()}`);
  return r.json();
}

function conectar(ingreso) {
  return new Promise((resolver, rechazar) => {
    const ws = new WebSocket(`${WS_BASE}/ws/juego?codigo=${ingreso.codigo}&jugador=${ingreso.jugadorId}&token=${ingreso.token}`);
    const jugador = { ...ingreso, ws, mano: [], estado: null, pendientes: new Map() };
    ws.onmessage = (ev) => {
      const m = JSON.parse(ev.data);
      if (m.tipo === "MANO") jugador.mano = m.cartas;
      if (m.tipo === "ESTADO") {
        jugador.estado = m.estado;
        const espera = jugador.pendientes.get(m.estado.ultimaAccionId);
        if (espera) { jugador.pendientes.delete(m.estado.ultimaAccionId); espera(); }
      }
    };
    ws.onopen = () => resolver(jugador);
    ws.onerror = rechazar;
  });
}

function accion(jugador, tipo, extra = {}) {
  const accionId = crypto.randomUUID();
  const inicio = performance.now();
  return new Promise((resolver) => {
    jugador.pendientes.set(accionId, () => resolver(performance.now() - inicio));
    jugador.ws.send(JSON.stringify({ tipo, accionId, ...extra }));
    setTimeout(() => { if (jugador.pendientes.delete(accionId)) resolver(null); }, 5000);
  });
}

const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

async function jugarMesa(n) {
  const creador = await post("/api/salas", { nombre: `bot${n}-1`, modalidad: "APUNTADO", jugadores: 4, umbral: 101 });
  const ingresos = [creador];
  for (let i = 2; i <= 4; i++) ingresos.push(await post(`/api/salas/${creador.codigo}/jugadores`, { nombre: `bot${n}-${i}` }));
  const jugadores = await Promise.all(ingresos.map(conectar));
  await esperar(500);
  const muestras = [];
  for (let t = 0; t < TURNOS; t++) {
    const enTurno = jugadores.find((j) => j.estado && j.jugadorId === j.estado.turnoDe);
    if (!enTurno) { await esperar(50); continue; }
    if (enTurno.estado.fase === "ROBAR") muestras.push(await accion(enTurno, "ROBAR_MAZO"));
    await esperar(5);
    const carta = enTurno.mano[enTurno.mano.length - 1];
    muestras.push(await accion(enTurno, "DESCARTAR", { carta }));
  }
  jugadores.forEach((j) => j.ws.close());
  return { codigo: creador.codigo, muestras: muestras.filter((m) => m !== null) };
}

const resultados = await Promise.all(Array.from({ length: MESAS }, (_, i) => jugarMesa(i + 1)));
const todas = resultados.flatMap((r) => r.muestras).sort((a, b) => a - b);
const pct = (p) => todas[Math.max(Math.ceil(p * todas.length) - 1, 0)];
console.log(JSON.stringify({
  servidor: BASE, mesas: MESAS, acciones: todas.length,
  promedio_ms: +(todas.reduce((a, b) => a + b, 0) / todas.length).toFixed(1),
  p50_ms: +pct(0.5).toFixed(1), p95_ms: +pct(0.95).toFixed(1), max_ms: +todas[todas.length - 1].toFixed(1),
  partidas: resultados.map((r) => r.codigo),
}, null, 2));
