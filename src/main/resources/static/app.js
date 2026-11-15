// Cliente web del juego Apuntado. Sin frameworks: la logica de reglas vive en el
// servidor; este archivo solo muestra el estado y envia las acciones del jugador.
"use strict";

const PALOS = { PICAS: "♠", CORAZONES: "♥", DIAMANTES: "♦", TREBOLES: "♣" };
const VALORES = ["AS", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ", "J", "Q", "K"];
const SIMBOLO_VALOR = { AS: "A", DOS: "2", TRES: "3", CUATRO: "4", CINCO: "5", SEIS: "6", SIETE: "7", OCHO: "8", NUEVE: "9", DIEZ: "10", J: "J", Q: "Q", K: "K" };
const CLAVE_SESION = "apuntado.credencial";

const $ = (id) => document.getElementById(id);

const app = {
  credencial: null,   // { codigo, jugadorId, token, nombre }
  ws: null,
  estado: null,       // ultimo EstadoPublico recibido
  mano: [],
  elegida: null,      // indice de la carta elegida en la mano
  nuevas: new Set(),  // cartas recien robadas, para resaltarlas
  pendientes: new Map(), // accionId -> instante de envio, para medir latencia
  ultimoCierreVisto: null,
  sondeoSala: null,
};

// ---------- utilidades ----------
function aviso(texto, tipo = "") {
  const div = document.createElement("div");
  div.className = "aviso " + tipo;
  div.textContent = texto;
  $("avisos").appendChild(div);
  setTimeout(() => div.remove(), tipo === "error" ? 4500 : 2500);
}

function claveCarta(c) { return c.valor + "-" + c.palo; }
function textoCarta(c) { return SIMBOLO_VALOR[c.valor] + PALOS[c.palo]; }

function crearCarta(c, mini = false) {
  const el = document.createElement("span");
  el.className = "carta" + (mini ? " mini" : "") + (c.palo === "CORAZONES" || c.palo === "DIAMANTES" ? " roja" : "");
  el.innerHTML = `<span class="esquina">${SIMBOLO_VALOR[c.valor]}<small>${PALOS[c.palo]}</small></span>${PALOS[c.palo]}`;
  el.title = textoCarta(c);
  return el;
}

function grupoCartas(cartas, clase = "") {
  const g = document.createElement("div");
  g.className = "grupo " + clase;
  cartas.forEach((c) => g.appendChild(crearCarta(c, true)));
  return g;
}

async function api(metodo, ruta, cuerpo) {
  const resp = await fetch(ruta, {
    method: metodo,
    headers: { "Content-Type": "application/json" },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });
  const datos = await resp.json().catch(() => ({}));
  if (!resp.ok) throw new Error(datos.error || "Algo salió mal (" + resp.status + ")");
  return datos;
}

function guardarCredencial(c) {
  app.credencial = c;
  try { sessionStorage.setItem(CLAVE_SESION, JSON.stringify(c)); } catch (e) { /* sin almacenamiento */ }
}

function leerCredencial() {
  try { return JSON.parse(sessionStorage.getItem(CLAVE_SESION)); } catch (e) { return null; }
}

function mostrarVista(id) {
  ["vistaInicio", "vistaSala", "vistaMesa"].forEach((v) => $(v).classList.toggle("oculto", v !== id));
}

// ---------- inicio ----------
async function cargarModalidades() {
  const modalidades = await api("GET", "/api/modalidades");
  const cont = $("opcionesModalidad");
  cont.innerHTML = "";
  Object.entries(modalidades).forEach(([clave, descripcion], i) => {
    const nombre = clave === "CLASICA" ? "Clásica (4-3-3)" : clave === "APUNTADO" ? "Apuntado a 101" : clave;
    const label = document.createElement("label");
    label.className = "opcion";
    label.innerHTML = `<input type="radio" name="modalidad" value="${clave}" ${i === 0 ? "checked" : ""}>
      <span><b>${nombre}</b><small>${descripcion}</small></span>`;
    cont.appendChild(label);
  });
}

$("formCrear").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  try {
    const ingreso = await api("POST", "/api/salas", {
      nombre: f.get("nombre"), modalidad: f.get("modalidad"),
      jugadores: Number(f.get("jugadores")), umbral: Number(f.get("umbral")),
    });
    guardarCredencial(ingreso);
    entrarASala();
  } catch (err) { aviso(err.message, "error"); }
});

$("formUnirse").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  const codigo = String(f.get("codigo")).trim().toUpperCase();
  try {
    const ingreso = await api("POST", `/api/salas/${codigo}/jugadores`, { nombre: f.get("nombre") });
    guardarCredencial(ingreso);
    entrarASala();
  } catch (err) { aviso(err.message, "error"); }
});

// ---------- sala de espera ----------
function entrarASala() {
  mostrarVista("vistaSala");
  $("salaCodigo").textContent = app.credencial.codigo;
  revisarSala();
  clearInterval(app.sondeoSala);
  app.sondeoSala = setInterval(revisarSala, 1500);
}

async function revisarSala() {
  try {
    const sala = await api("GET", `/api/salas/${app.credencial.codigo}`);
    if (sala.iniciada) {
      clearInterval(app.sondeoSala);
      entrarAMesa();
      return;
    }
    const modalidad = sala.modalidad === "CLASICA" ? "Clásica" : "Apuntado";
    $("salaDetalle").textContent = `${modalidad} · hasta ${sala.umbral} puntos · ${sala.jugadores.length} de ${sala.cupo} jugadores`;
    $("salaJugadores").innerHTML = sala.jugadores.map((n) => `<li>${escapar(n)}</li>`).join("");
    const soyCreador = sala.creadorId === app.credencial.jugadorId;
    $("btnEmpezar").classList.toggle("oculto", !soyCreador || sala.jugadores.length < 2);
  } catch (err) {
    clearInterval(app.sondeoSala);
    aviso(err.message, "error");
    salir();
  }
}

$("btnEmpezar").addEventListener("click", async () => {
  try {
    await api("POST", `/api/salas/${app.credencial.codigo}/iniciar`,
      { jugadorId: String(app.credencial.jugadorId), token: app.credencial.token });
    revisarSala();
  } catch (err) { aviso(err.message, "error"); }
});

$("btnCopiar").addEventListener("click", async () => {
  const enlace = `${location.origin}${location.pathname}?sala=${app.credencial.codigo}`;
  try { await navigator.clipboard.writeText(enlace); aviso("Enlace copiado", "ok"); }
  catch (e) { aviso(enlace); }
});

// ---------- mesa ----------
function entrarAMesa() {
  mostrarVista("vistaMesa");
  conectar();
}

function conectar() {
  const c = app.credencial;
  const protocolo = location.protocol === "https:" ? "wss" : "ws";
  const url = `${protocolo}://${location.host}/ws/juego?codigo=${c.codigo}&jugador=${c.jugadorId}&token=${c.token}`;
  const ws = new WebSocket(url);
  app.ws = ws;
  ws.onmessage = (ev) => recibir(JSON.parse(ev.data));
  ws.onclose = () => {
    if (app.ws === ws && app.credencial) {
      aviso("Se perdió la conexión. Reconectando…", "error");
      setTimeout(conectar, 2000);
    }
  };
}

function enviar(tipo, extra = {}) {
  if (!app.ws || app.ws.readyState !== WebSocket.OPEN) { aviso("Sin conexión con el servidor", "error"); return; }
  const accionId = (crypto.randomUUID ? crypto.randomUUID() : String(Date.now() + Math.random()));
  app.pendientes.set(accionId, performance.now());
  app.ws.send(JSON.stringify({ tipo, accionId, ...extra }));
}

function recibir(msg) {
  if (msg.tipo === "ESTADO") return recibirEstado(msg.estado);
  if (msg.tipo === "MANO") return recibirMano(msg.cartas);
  if (msg.tipo === "RESULTADO") {
    if (msg.accionId) app.pendientes.delete(msg.accionId);
    if (!msg.aceptada) aviso(msg.mensaje, "error");
    else if (msg.mensaje && msg.mensaje.startsWith("¡")) aviso(msg.mensaje, "ok");
  }
}

function recibirEstado(estado) {
  medirLatencia(estado.ultimaAccionId);
  app.estado = estado;
  pintarMesa();
  const cierre = estado.ultimoCierre;
  const claveCierre = cierre ? cierre.ronda + "-" + estado.estado : null;
  if (estado.estado !== "EN_RONDA" && cierre && claveCierre !== app.ultimoCierreVisto) {
    app.ultimoCierreVisto = claveCierre;
    mostrarResultadoRonda();
  }
  if (estado.estado === "EN_RONDA" && $("dlgRonda").open) $("dlgRonda").close();
}

/** Latencia percibida: desde que se envio la accion hasta ver la mesa actualizada. */
function medirLatencia(accionId) {
  if (!accionId || !app.pendientes.has(accionId)) return;
  const ms = Math.round(performance.now() - app.pendientes.get(accionId));
  app.pendientes.delete(accionId);
  const el = $("latencia");
  el.textContent = ms + " ms";
  el.classList.remove("oculto");
  if (app.ws && app.ws.readyState === WebSocket.OPEN) {
    app.ws.send(JSON.stringify({ tipo: "LATENCIA", ms }));
  }
}

function recibirMano(cartas) {
  const anteriores = new Set(app.mano.map(claveCarta));
  const elegidaClave = app.elegida != null && app.mano[app.elegida] ? claveCarta(app.mano[app.elegida]) : null;
  // Solo se resalta la carta recien robada (la mano crece en una); en una ronda nueva no.
  const robo = cartas.length === anteriores.size + 1;
  app.nuevas = new Set(robo ? cartas.map(claveCarta).filter((k) => !anteriores.has(k)) : []);
  app.mano = cartas;
  const idx = cartas.findIndex((c) => claveCarta(c) === elegidaClave);
  app.elegida = idx >= 0 ? idx : null;
  pintarMano();
}

function soyYo(id) { return app.credencial && id === app.credencial.jugadorId; }

function pintarMesa() {
  const e = app.estado;
  const modalidad = e.modalidad === "CLASICA" ? "Clásica (4-3-3)" : "Apuntado a " + e.umbral;
  const info = $("infoMesa");
  info.classList.remove("oculto");
  info.innerHTML = `<span>Mesa <b>${e.codigo}</b></span><span>${modalidad}</span><span>Ronda <b>${e.ronda}</b></span>`;
  $("reglaActual").textContent = e.descripcionModalidad;

  $("jugadores").innerHTML = "";
  e.jugadores.forEach((j) => {
    const div = document.createElement("div");
    div.className = "jugador" + (j.id === e.turnoDe ? " en-turno" : "") + (soyYo(j.id) ? " yo" : "") + (j.eliminado ? " eliminado" : "");
    const pct = Math.min(100, Math.round((j.acumulado / e.umbral) * 100));
    const etiqueta = j.eliminado ? '<span class="etiqueta gris">eliminado</span>'
      : j.id === e.turnoDe ? '<span class="etiqueta">en turno</span>' : "";
    div.innerHTML = `<div class="nombre"><span>${escapar(j.nombre)}${soyYo(j.id) ? " (tú)" : ""}</span>${etiqueta}</div>
      <div class="detalle">${j.cartasEnMano} cartas · ${j.acumulado} / ${e.umbral} pts</div>
      <div class="barra-puntos ${pct >= 75 ? "peligro" : ""}"><span style="width:${pct}%"></span></div>`;
    $("jugadores").appendChild(div);
  });

  $("cartasMazo").textContent = e.cartasEnMazo;
  $("cartasPozo").textContent = e.cartasEnPozo;
  const tope = e.topePozo ? crearCarta(e.topePozo) : Object.assign(document.createElement("span"), { className: "carta vacia" });
  tope.id = "topePozo";
  $("topePozo").replaceWith(tope);

  const miTurno = e.estado === "EN_RONDA" && soyYo(e.turnoDe);
  const debeRobar = miTurno && e.fase === "ROBAR";
  $("mazo").disabled = !debeRobar;
  $("pozo").disabled = !debeRobar || !e.topePozo;
  $("mazo").classList.toggle("activo", debeRobar);
  $("pozo").classList.toggle("activo", debeRobar && !!e.topePozo);
  $("mensajeTurno").textContent = textoTurno(e, miTurno);
  $("ultimoMovimiento").textContent = e.ultimoMovimiento || "";
  actualizarBotones();
}

function textoTurno(e, miTurno) {
  if (e.estado === "FINALIZADA") return "Partida terminada";
  if (e.estado === "RONDA_TERMINADA") return "Ronda terminada";
  if (!miTurno) {
    const j = e.jugadores.find((x) => x.id === e.turnoDe);
    return "Turno de " + (j ? j.nombre : "…");
  }
  return e.fase === "ROBAR" ? "Tu turno: roba del mazo o toma del pozo" : "Tu turno: descarta una carta o cierra la ronda";
}

function actualizarBotones() {
  const e = app.estado;
  const puedeDescartar = e && e.estado === "EN_RONDA" && soyYo(e.turnoDe) && e.fase === "DESCARTAR" && app.elegida != null;
  $("btnDescartar").disabled = !puedeDescartar;
  $("btnCerrar").disabled = !puedeDescartar;
}

function pintarMano() {
  const cont = $("mano");
  cont.innerHTML = "";
  app.mano.forEach((c, i) => {
    const el = crearCarta(c);
    if (i === app.elegida) el.classList.add("elegida");
    if (app.nuevas.has(claveCarta(c))) el.classList.add("nueva");
    el.draggable = true;
    el.dataset.indice = i;
    el.addEventListener("click", () => { app.elegida = app.elegida === i ? null : i; pintarMano(); });
    el.addEventListener("dragstart", (ev) => { ev.dataTransfer.setData("text/plain", i); el.classList.add("arrastrando"); });
    el.addEventListener("dragend", () => el.classList.remove("arrastrando"));
    el.addEventListener("dragover", (ev) => ev.preventDefault());
    el.addEventListener("drop", (ev) => { ev.preventDefault(); moverCarta(Number(ev.dataTransfer.getData("text/plain")), i); });
    cont.appendChild(el);
  });
  $("cuentaMano").textContent = app.mano.length ? `(${app.mano.length} cartas)` : "";
  actualizarBotones();
}

function moverCarta(desde, hasta) {
  if (desde === hasta || Number.isNaN(desde)) return;
  const elegidaClave = app.elegida != null ? claveCarta(app.mano[app.elegida]) : null;
  const [carta] = app.mano.splice(desde, 1);
  app.mano.splice(hasta, 0, carta);
  app.elegida = elegidaClave ? app.mano.findIndex((c) => claveCarta(c) === elegidaClave) : null;
  pintarMano();
  enviar("ORDENAR", { orden: app.mano });
}

function ordenarMano(criterio) {
  const rango = (c) => VALORES.indexOf(c.valor);
  const palo = (c) => Object.keys(PALOS).indexOf(c.palo);
  const elegidaClave = app.elegida != null ? claveCarta(app.mano[app.elegida]) : null;
  app.mano.sort(criterio === "palo" ? (a, b) => palo(a) - palo(b) || rango(a) - rango(b)
    : (a, b) => rango(a) - rango(b) || palo(a) - palo(b));
  app.elegida = elegidaClave ? app.mano.findIndex((c) => claveCarta(c) === elegidaClave) : null;
  pintarMano();
  enviar("ORDENAR", { orden: app.mano });
}

$("btnOrdenNumero").addEventListener("click", () => ordenarMano("numero"));
$("btnOrdenPalo").addEventListener("click", () => ordenarMano("palo"));
$("mazo").addEventListener("click", () => enviar("ROBAR_MAZO"));
$("pozo").addEventListener("click", () => enviar("TOMAR_POZO"));
$("btnDescartar").addEventListener("click", () => {
  if (app.elegida == null) return;
  enviar("DESCARTAR", { carta: app.mano[app.elegida] });
  app.elegida = null;
});
$("btnCerrar").addEventListener("click", () => {
  if (app.elegida == null) return;
  enviar("CERRAR", { carta: app.mano[app.elegida] });
});

// ---------- fin de ronda ----------
function mostrarResultadoRonda() {
  const e = app.estado;
  const c = e.ultimoCierre;
  const terminada = e.estado === "FINALIZADA";
  const ganador = terminada ? e.jugadores.find((j) => j.id === e.ganadorId) : null;
  $("rondaTitulo").textContent = terminada
    ? `¡${ganador ? ganador.nombre : "Alguien"} gana la partida!`
    : `${c.cerradorNombre} cerró la ronda ${c.ronda}`;

  const comb = $("rondaCombinaciones");
  comb.innerHTML = "";
  c.combinacionesCerrador.forEach((g) => comb.appendChild(grupoCartas(g)));
  if (c.sobrantesCerrador.length) comb.appendChild(grupoCartas(c.sobrantesCerrador, "sobrante"));
  const desc = document.createElement("div");
  desc.className = "sub";
  desc.textContent = "Descartó el " + textoCarta(c.descarte);
  comb.appendChild(desc);

  const tbody = $("rondaTabla");
  tbody.innerHTML = "";
  c.penalizaciones.forEach((p) => {
    const tr = document.createElement("tr");
    const celda = document.createElement("td");
    celda.appendChild(grupoCartas(p.cartasSueltas));
    tr.innerHTML = `<td>${escapar(p.nombre)}${p.jugadorId === c.cerradorId ? " (cerró)" : ""}</td>`;
    tr.appendChild(celda);
    tr.insertAdjacentHTML("beforeend", `<td>+${p.puntos}</td><td>${p.acumulado}${p.eliminado ? ' <span class="etiqueta gris">eliminado</span>' : ""}</td>`);
    tbody.appendChild(tr);
  });

  $("btnSiguiente").classList.toggle("oculto", terminada);
  $("btnNueva").classList.toggle("oculto", !terminada);
  const lnk = $("lnkMetricas");
  lnk.href = `metricas.html?codigo=${e.codigo}`;
  lnk.classList.toggle("oculto", !terminada);
  if (!$("dlgRonda").open) $("dlgRonda").showModal();
}

$("btnSiguiente").addEventListener("click", () => enviar("SIGUIENTE_RONDA"));
$("btnNueva").addEventListener("click", () => { $("dlgRonda").close(); salir(); });

// ---------- ayuda y arranque ----------
$("btnAyuda").addEventListener("click", () => $("dlgAyuda").showModal());
document.querySelectorAll("[data-cerrar]").forEach((b) => b.addEventListener("click", () => b.closest("dialog").close()));

function salir() {
  try { sessionStorage.removeItem(CLAVE_SESION); } catch (e) { /* nada */ }
  const ws = app.ws;
  app.credencial = null;
  app.ws = null;
  if (ws) ws.close();
  app.mano = [];
  app.estado = null;
  $("infoMesa").classList.add("oculto");
  $("latencia").classList.add("oculto");
  mostrarVista("vistaInicio");
}

function escapar(texto) {
  return String(texto).replace(/[&<>"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[ch]));
}

(async function iniciar() {
  try { await cargarModalidades(); } catch (err) { aviso("No se pudo contactar al servidor", "error"); }
  const params = new URLSearchParams(location.search);
  if (params.get("sala")) document.querySelector("#formUnirse [name=codigo]").value = params.get("sala");
  const guardada = leerCredencial();
  if (guardada) {
    app.credencial = guardada;
    entrarASala();
  }
})();
