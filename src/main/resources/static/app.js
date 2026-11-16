// Cliente web del juego Apuntado. Sin frameworks: la logica de reglas vive en el
// servidor; este archivo dibuja la mesa, anima las jugadas y envia las acciones.
"use strict";

const PALOS = { PICAS: "♠", CORAZONES: "♥", DIAMANTES: "♦", TREBOLES: "♣" };
const VALORES = ["AS", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ", "J", "Q", "K"];
const SIMBOLO_VALOR = { AS: "A", DOS: "2", TRES: "3", CUATRO: "4", CINCO: "5", SEIS: "6", SIETE: "7", OCHO: "8", NUEVE: "9", DIEZ: "10", J: "J", Q: "Q", K: "K" };
const FIGURAS = new Set(["J", "Q", "K"]);
const CLAVE_SESION = "apuntado.credencial";
const SIN_MOVIMIENTO = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const DURACION = 320;

const $ = (id) => document.getElementById(id);

const app = {
  credencial: null,   // { codigo, jugadorId, token, nombre }
  ws: null,
  estado: null,       // ultimo EstadoPublico recibido
  mano: [],
  elegida: null,      // clave de la carta elegida
  nuevas: new Set(),  // cartas recien robadas, para resaltarlas
  pendientes: new Map(), // accionId -> instante de envio, para medir latencia
  ultimoCierreVisto: null,
  ultimaSecuencia: null, // secuencia del ultimo movimiento ya animado
  vueloHaciaMano: null,  // { desde: DOMRect } para la carta que voy a recibir
  repartiendo: false,
  soltada: null,         // carta arrastrada al pozo que espera la confirmacion del servidor
  sondeoSala: null,
};

// ---------- utilidades ----------
function aviso(texto, tipo = "") {
  const div = document.createElement("div");
  div.className = "aviso " + tipo;
  div.textContent = texto;
  $("avisos").appendChild(div);
  setTimeout(() => { div.classList.add("saliendo"); setTimeout(() => div.remove(), 300); }, tipo === "error" ? 4200 : 2300);
}

function claveCarta(c) { return c.valor + "-" + c.palo; }
function textoCarta(c) { return SIMBOLO_VALOR[c.valor] + PALOS[c.palo]; }
function esRoja(c) { return c.palo === "CORAZONES" || c.palo === "DIAMANTES"; }

/** Cara de una carta: indices en dos esquinas y un dibujo central (palo o figura). */
function crearCarta(c, mini = false) {
  const el = document.createElement("span");
  el.className = "carta" + (mini ? " mini" : "") + (esRoja(c) ? " roja" : "");
  const v = SIMBOLO_VALOR[c.valor];
  const p = PALOS[c.palo];
  const medio = FIGURAS.has(v) ? `<span class="medio figura">${v}<span>${p}</span></span>` : `<span class="medio">${p}</span>`;
  el.innerHTML = `<span class="indice ti">${v}<small>${p}</small></span>${medio}<span class="indice bd">${v}<small>${p}</small></span>`;
  el.title = textoCarta(c);
  el.dataset.clave = claveCarta(c);
  return el;
}

function crearDorso() {
  const el = document.createElement("span");
  el.className = "carta dorso";
  return el;
}

function grupoCartas(cartas, clase = "", retrasoBase = 0) {
  const g = document.createElement("div");
  g.className = "grupo " + clase;
  cartas.forEach((c, i) => {
    const el = crearCarta(c, true);
    el.style.animationDelay = (retrasoBase + i * 70) + "ms";
    g.appendChild(el);
  });
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

function escapar(texto) {
  return String(texto).replace(/[&<>"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[ch]));
}

// ---------- animacion: una carta que vuela de un lugar a otro ----------
/**
 * Hace volar una carta (cara o dorso) desde un rectangulo de la pantalla hasta otro.
 * Usa una copia flotante, asi el resto de la mesa se puede redibujar mientras tanto.
 */
function volar(desde, hasta, carta, { retraso = 0, duracion = DURACION, girar = 0 } = {}) {
  if (SIN_MOVIMIENTO || !desde || !hasta) return Promise.resolve();
  const el = carta ? crearCarta(carta) : crearDorso();
  el.classList.add("volando");
  Object.assign(el.style, { left: desde.left + "px", top: desde.top + "px", width: desde.width + "px", height: desde.height + "px" });
  el.style.setProperty("--cw", desde.width + "px");
  document.body.appendChild(el);
  const escala = hasta.width / desde.width;
  const anim = el.animate([
    { transform: "translate(0, 0) scale(1) rotate(0deg)", opacity: 1 },
    { transform: `translate(${hasta.left - desde.left}px, ${hasta.top - desde.top}px) scale(${escala}) rotate(${girar}deg)`, opacity: 1 },
  ], { duration: duracion, delay: retraso, easing: "cubic-bezier(.2,.8,.2,1)", fill: "both" });
  return anim.finished.catch(() => {}).then(() => el.remove());
}

function rectDe(el) { return el ? el.getBoundingClientRect() : null; }

/** Caja con forma de carta (ancho x 1,4) centrada dentro de otra caja cualquiera. */
function comoCarta(rect, ancho) {
  if (!rect) return null;
  const w = ancho || Math.min(rect.width, rect.height / 1.4);
  const h = w * 1.4;
  return { left: rect.left + (rect.width - w) / 2, top: rect.top + (rect.height - h) / 2, width: w, height: h };
}

function anchoMini() {
  const d = document.querySelector(".asiento .dorsos .carta");
  return d ? d.offsetWidth : 40;
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

function pintarPortada() {
  const cont = document.querySelector(".abanico-portada");
  const cartas = [{ valor: "AS", palo: "PICAS" }, { valor: "K", palo: "CORAZONES" }, { valor: "SIETE", palo: "DIAMANTES" },
    { valor: "SIETE", palo: "TREBOLES" }, { valor: "SIETE", palo: "CORAZONES" }];
  cartas.forEach((c, i) => {
    const el = crearCarta(c);
    const ang = (i - 2) * 9;
    el.style.transform = `translateX(-50%) rotate(${ang}deg)`;
    el.animate([{ transform: "translateX(-50%) rotate(0deg)", opacity: 0 }, { transform: `translateX(-50%) rotate(${ang}deg)`, opacity: 1 }],
      { duration: SIN_MOVIMIENTO ? 1 : 500, delay: SIN_MOVIMIENTO ? 0 : i * 80, easing: "cubic-bezier(.2,.8,.2,1)", fill: "both" });
    cont.appendChild(el);
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
    const lista = $("salaJugadores");
    if (lista.children.length !== sala.jugadores.length) {
      lista.innerHTML = sala.jugadores.map((n) => `<li>${escapar(n)}</li>`).join("");
    }
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

// ---------- conexion ----------
function entrarAMesa() {
  mostrarVista("vistaMesa");
  $("menuPartida").classList.remove("oculto");
  conectar();
}

function conectar() {
  const c = app.credencial;
  const protocolo = location.protocol === "https:" ? "wss" : "ws";
  const url = `${protocolo}://${location.host}/ws/juego?codigo=${c.codigo}&jugador=${c.jugadorId}&token=${c.token}`;
  const ws = new WebSocket(url);
  app.ws = ws;
  app.ultimaSecuencia = null; // al (re)conectar no se anima lo que ya paso
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
    if (!msg.aceptada) { cancelarSoltada(); aviso(msg.mensaje, "error"); }
    else if (msg.mensaje && msg.mensaje.startsWith("¡")) aviso(msg.mensaje, "ok");
  }
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

function soyYo(id) { return app.credencial && id === app.credencial.jugadorId; }

// ---------- estado de la mesa ----------
function recibirEstado(estado) {
  medirLatencia(estado.ultimaAccionId);
  const mov = estado.movimiento;
  const nuevo = mov && app.ultimaSecuencia !== null && mov.secuencia > app.ultimaSecuencia;
  // Los origenes de las animaciones se miden ANTES de redibujar la mesa
  const preparada = nuevo ? prepararAnimacion(mov) : null;
  if (mov) app.ultimaSecuencia = mov.secuencia;
  else if (app.ultimaSecuencia === null) app.ultimaSecuencia = 0;
  app.estado = estado;
  pintarMesa(preparada);
  if (preparada) preparada.ejecutar();

  const cierre = estado.ultimoCierre;
  const claveCierre = cierre ? cierre.ronda + "-" + estado.estado : null;
  if (estado.estado !== "EN_RONDA" && cierre && claveCierre !== app.ultimoCierreVisto) {
    app.ultimoCierreVisto = claveCierre;
    setTimeout(mostrarResultadoRonda, preparada ? DURACION + 150 : 0);
  }
  if (estado.estado === "EN_RONDA" && $("dlgRonda").open) $("dlgRonda").close();
}

/**
 * Decide que animar segun la ultima jugada. Devuelve un objeto con:
 *  - ocultarPozo: no mostrar la carta de encima del pozo hasta que llegue la que vuela;
 *  - ejecutar(): lanza las animaciones despues de redibujar.
 */
function prepararAnimacion(mov) {
  const mia = soyYo(mov.jugadorId);
  const rectMazo = rectDe($("mazoTope"));
  const rectPozo = rectDe($("topePozo"));
  const asiento = () => comoCarta(rectDe(document.querySelector(`.asiento[data-id="${mov.jugadorId}"] .dorsos`)), anchoMini());
  switch (mov.tipo) {
    case "ROBO_MAZO":
      if (mia) { app.vueloHaciaMano = { desde: rectMazo }; return null; }
      return { ejecutar: () => volar(rectMazo, asiento(), null) };
    case "ROBO_POZO":
      if (mia) { app.vueloHaciaMano = { desde: rectPozo }; return null; }
      return { ejecutar: () => volar(rectPozo, asiento(), mov.carta) };
    case "DESCARTE":
    case "CIERRE": {
      const origen = mia ? document.querySelector(`#mano .carta[data-clave="${claveCarta(mov.carta)}"]`) : null;
      let desde = mia ? comoCarta(rectDe(origen), origen && origen.offsetWidth) : asiento();
      if (mia && app.soltada && app.soltada.clave === claveCarta(mov.carta)) {
        desde = rectDe(app.soltada.fantasma); // la carta arrastrada sigue su vuelo desde donde se solto
        cancelarSoltada();
      }
      if (origen) origen.style.opacity = "0";
      return {
        ocultarPozo: true,
        ejecutar: () => volar(desde, rectDe($("topePozo")), mov.carta, { girar: (Math.random() * 16) - 8 })
          .then(() => { const t = $("topePozo"); if (t) t.style.visibility = ""; }),
      };
    }
    case "RONDA_NUEVA":
      app.repartiendo = true;
      app.mano = [];
      return {
        ejecutar: () => {
          const destinos = [...document.querySelectorAll(".asiento .dorsos")].map((d) => comoCarta(rectDe(d), anchoMini()));
          destinos.forEach((d, i) => [0, 1, 2].forEach((k) => volar(rectMazo, d, null, { retraso: (k * destinos.length + i) * 70 })));
        },
      };
    default:
      return null;
  }
}

function pintarMesa(preparada) {
  const e = app.estado;
  const modalidad = e.modalidad === "CLASICA" ? "Clásica (4-3-3)" : "Apuntado a " + e.umbral;
  const info = $("infoMesa");
  info.classList.remove("oculto");
  info.innerHTML = `<span>Mesa <b>${e.codigo}</b></span><span>${modalidad}</span><span>Ronda <b>${e.ronda}</b></span>`;
  $("reglaActual").textContent = e.descripcionModalidad;
  $("lnkMetricasMenu").href = `metricas.html?codigo=${e.codigo}`;
  pintarMarcador(e);
  pintarAsientos(e);
  pintarCentro(e, preparada);

  const yo = e.jugadores.find((j) => soyYo(j.id));
  $("miNombre").textContent = yo ? yo.nombre + " (tú)" : "";
  $("misPuntos").textContent = yo ? `· ${yo.acumulado} / ${e.umbral} pts` : "";
  const miTurno = e.estado === "EN_RONDA" && soyYo(e.turnoDe);
  $("zonaMia").classList.toggle("en-turno", miTurno);
  const msj = $("mensajeTurno");
  const texto = textoTurno(e, miTurno);
  if (msj.textContent !== texto) {
    msj.textContent = texto;
    msj.classList.toggle("mio", miTurno);
  }
  $("ultimoMovimiento").textContent = e.ultimoMovimiento || "";
  actualizarBotones();
}

function pintarMarcador(e) {
  $("tablaMarcador").innerHTML = e.jugadores.map((j) =>
    `<tr><td>${escapar(j.nombre)}${soyYo(j.id) ? " (tú)" : ""}</td><td>${j.acumulado} / ${e.umbral}${j.eliminado ? " · eliminado" : ""}</td></tr>`).join("");
}

/** Los rivales se sientan en orden de turno empezando a mi izquierda. */
function posiciones(cantidad) {
  return { 1: ["arriba"], 2: ["izquierda", "derecha"], 3: ["izquierda", "arriba", "derecha"] }[cantidad] || [];
}

function pintarAsientos(e) {
  const cont = $("asientos");
  const yo = e.jugadores.findIndex((j) => soyYo(j.id));
  const rivales = e.jugadores.slice(yo + 1).concat(e.jugadores.slice(0, Math.max(yo, 0)));
  const lugares = posiciones(rivales.length);
  rivales.forEach((j, i) => {
    let el = cont.querySelector(`.asiento[data-id="${j.id}"]`);
    if (!el) {
      el = document.createElement("div");
      el.dataset.id = j.id;
      el.innerHTML = `<div class="dorsos"></div><div class="placa"></div>`;
      cont.appendChild(el);
    }
    el.className = `asiento ${lugares[i]}` + (j.id === e.turnoDe ? " en-turno" : "") + (j.eliminado ? " eliminado" : "");
    const dorsos = el.querySelector(".dorsos");
    const cantidad = j.eliminado ? 0 : Math.min(j.cartasEnMano, 11);
    while (dorsos.children.length < cantidad) dorsos.appendChild(crearDorso());
    while (dorsos.children.length > cantidad) dorsos.lastChild.remove();
    [...dorsos.children].forEach((d, k) => {
      const ang = (k - (cantidad - 1) / 2) * 4;
      d.style.transform = `rotate(${ang}deg) translateY(${Math.abs(ang) * .6}px)`;
    });
    if (lugares[i] !== "arriba") {
      // el abanico se dibuja girado 90 grados: se reserva el alto que realmente ocupa
      const w = anchoMini();
      const largo = w * (1 + Math.max(cantidad - 1, 0) * 0.38);
      dorsos.style.margin = `${Math.max((largo - w * 1.4) / 2, 0) + 6}px 0`;
    } else {
      dorsos.style.margin = "";
    }
    const etiquetas = [];
    if (e.abridorId === j.id && e.estado === "EN_RONDA") etiquetas.push("abre");
    if (j.id === e.turnoDe) etiquetas.push("en turno");
    if (j.eliminado) etiquetas.push("eliminado");
    el.querySelector(".placa").innerHTML = `<b>${escapar(j.nombre)}</b><small>${j.acumulado} / ${e.umbral} pts · ${j.cartasEnMano} cartas</small>`
      + `<div>${etiquetas.map((t) => `<span class="etiq">${t}</span>`).join("")}</div>`;
  });
  [...cont.children].forEach((el) => { if (!rivales.some((j) => String(j.id) === el.dataset.id)) el.remove(); });
}

function pintarCentro(e, preparada) {
  $("cartasMazo").textContent = e.cartasEnMazo;
  $("cartasPozo").textContent = e.cartasEnPozo;
  const pila = $("pilaPozo");
  pila.innerHTML = "";
  if (e.cartasEnPozo > 1) {
    const debajo = crearDorso();
    debajo.className = "carta debajo";
    debajo.style.transform = "rotate(-6deg) translate(-2px, 2px)";
    debajo.style.background = "#f1ede4";
    pila.appendChild(debajo);
  }
  const tope = e.topePozo ? crearCarta(e.topePozo) : Object.assign(document.createElement("span"), { className: "carta vacia" });
  tope.id = "topePozo";
  if (e.topePozo) tope.style.transform = "rotate(3deg)";
  if (preparada && preparada.ocultarPozo && !SIN_MOVIMIENTO) tope.style.visibility = "hidden";
  pila.appendChild(tope);
  $("mazoTope").style.visibility = e.cartasEnMazo > 0 ? "" : "hidden";

  const miTurno = e.estado === "EN_RONDA" && soyYo(e.turnoDe);
  const debeRobar = miTurno && e.fase === "ROBAR";
  $("mazo").disabled = !debeRobar;
  $("pozo").disabled = !debeRobar || !e.topePozo;
  $("mazo").classList.toggle("activo", debeRobar);
  $("pozo").classList.toggle("activo", debeRobar && !!e.topePozo);
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

// ---------- mi mano ----------
function recibirMano(cartas) {
  const anteriores = new Set(app.mano.map(claveCarta));
  const robo = cartas.length === anteriores.size + 1;
  const nuevas = robo ? cartas.map(claveCarta).filter((k) => !anteriores.has(k)) : [];
  app.nuevas = new Set(nuevas);
  app.mano = cartas;
  if (app.elegida && !cartas.some((c) => claveCarta(c) === app.elegida)) app.elegida = null;
  const repartir = app.repartiendo;
  app.repartiendo = false;
  pintarMano();

  if (repartir && !SIN_MOVIMIENTO) {
    const desde = rectDe($("mazoTope"));
    [...$("mano").children].forEach((el, i) => {
      el.style.opacity = "0";
      volar(desde, comoCarta(rectDe(el), el.offsetWidth), null, { retraso: 120 + i * 60 }).then(() => { el.style.opacity = ""; });
    });
  } else if (app.vueloHaciaMano && nuevas.length === 1 && !SIN_MOVIMIENTO) {
    const el = document.querySelector(`#mano .carta[data-clave="${nuevas[0]}"]`);
    const carta = cartas.find((c) => claveCarta(c) === nuevas[0]);
    if (el) {
      el.style.opacity = "0";
      volar(app.vueloHaciaMano.desde, comoCarta(rectDe(el), el.offsetWidth), carta).then(() => { el.style.opacity = ""; });
    }
  }
  app.vueloHaciaMano = null;
}

/** Abanico: cada carta gira un poco y se monta sobre la anterior. */
function disposicion(i, n, anchoMano, anchoCarta) {
  const centro = (n - 1) / 2;
  const util = anchoMano - anchoCarta * 2.2; // margen para que el giro no saque cartas de la pantalla
  const paso = n > 1 ? Math.max(Math.min(anchoCarta * 0.72, util / (n - 1)), anchoCarta * 0.28) : 0;
  const ang = (i - centro) * Math.min(5, 40 / Math.max(n, 1));
  const x = (i - centro) * paso;
  const y = (i - centro) * (i - centro) * anchoCarta * 0.018;
  return { x, y, ang };
}

function pintarMano() {
  const cont = $("mano");
  const existentes = new Map([...cont.children].map((el) => [el.dataset.clave, el]));
  const anchoMano = cont.clientWidth;
  const anchoCarta = parseFloat(getComputedStyle(cont).getPropertyValue("--cw")) || 70;
  const muestra = cont.querySelector(".carta");
  const ancho = muestra ? muestra.offsetWidth : anchoCarta;
  app.mano.forEach((c, i) => {
    const clave = claveCarta(c);
    let el = existentes.get(clave);
    if (el) existentes.delete(clave);
    else el = crearCartaDeMano(c);
    const { x, y, ang } = disposicion(i, app.mano.length, anchoMano, ancho);
    const elevar = clave === app.elegida ? -26 : 0;
    el.style.transform = `translateX(calc(-50% + ${x}px)) translateY(${y + elevar}px) rotate(${ang}deg)`;
    el.style.zIndex = String(i + 1);
    el.dataset.indice = i;
    el.classList.toggle("elegida", clave === app.elegida);
    el.classList.toggle("nueva", app.nuevas.has(clave));
    cont.appendChild(el); // mantiene el orden del DOM igual al de la mano
  });
  existentes.forEach((el) => el.remove());
  $("cuentaMano").textContent = app.mano.length ? `· ${app.mano.length} cartas` : "";
  actualizarBotones();
}

function crearCartaDeMano(c) {
  const el = crearCarta(c);
  el.draggable = false; // el arrastre se hace con eventos de puntero: sirve igual con mouse y con el dedo
  el.addEventListener("pointerdown", (ev) => iniciarArrastre(ev, el, c));
  return el;
}

// ---------- arrastrar cartas: reordenar la mano o soltarlas en el pozo ----------
const UMBRAL_ARRASTRE = 8; // px que hay que mover antes de que un toque cuente como arrastre

function puedoDescartar() {
  const e = app.estado;
  return !!e && e.estado === "EN_RONDA" && soyYo(e.turnoDe) && e.fase === "DESCARTAR";
}

function dentro(rect, x, y, margen = 0) {
  return !!rect && x >= rect.left - margen && x <= rect.right + margen && y >= rect.top - margen && y <= rect.bottom + margen;
}

function iniciarArrastre(ev, el, carta) {
  if (ev.button !== 0 || app.soltada) return;
  ev.preventDefault();
  const clave = claveCarta(carta);
  const x0 = ev.clientX;
  const y0 = ev.clientY;
  let fantasma = null;
  let caja = null;

  const sobrePozo = (x, y) => dentro(rectDe($("pilaPozo")), x, y, 24);
  const mover = (e) => {
    const dx = e.clientX - x0;
    const dy = e.clientY - y0;
    if (!fantasma) {
      if (Math.hypot(dx, dy) < UMBRAL_ARRASTRE) return;
      caja = comoCarta(rectDe(el), el.offsetWidth);
      fantasma = crearCarta(carta);
      fantasma.classList.add("volando", "arrastre");
      Object.assign(fantasma.style, { left: caja.left + "px", top: caja.top + "px", width: caja.width + "px", height: caja.height + "px" });
      fantasma.style.setProperty("--cw", caja.width + "px");
      document.body.appendChild(fantasma);
      el.classList.add("arrastrada");
      $("pozo").classList.toggle("destino", puedoDescartar());
    }
    fantasma.style.transform = `translate(${dx}px, ${dy}px) rotate(${Math.max(-10, Math.min(10, dx / 25))}deg)`;
    $("pozo").classList.toggle("encima", puedoDescartar() && sobrePozo(e.clientX, e.clientY));
  };
  const terminar = (e) => {
    window.removeEventListener("pointermove", mover);
    window.removeEventListener("pointerup", terminar);
    window.removeEventListener("pointercancel", terminar);
    $("pozo").classList.remove("destino", "encima");
    if (!fantasma) { // fue un toque: elegir o soltar la carta
      app.elegida = app.elegida === clave ? null : clave;
      pintarMano();
      return;
    }
    if (e.type !== "pointercancel" && sobrePozo(e.clientX, e.clientY)) {
      if (puedoDescartar()) return soltarEnPozo(carta, el, fantasma);
      aviso(soyYo(app.estado && app.estado.turnoDe) ? "Primero roba del mazo o del pozo" : "Espera tu turno para descartar", "error");
    } else if (e.type !== "pointercancel" && dentro(rectDe($("mano")), e.clientX, e.clientY, 40)) {
      reordenarHasta(clave, e.clientX);
    }
    devolver(el, fantasma, caja);
  };
  window.addEventListener("pointermove", mover);
  window.addEventListener("pointerup", terminar);
  window.addEventListener("pointercancel", terminar);
}

/** La carta vuelve a su lugar en la mano si no se solto en un sitio valido. */
function devolver(el, fantasma, caja) {
  const destino = el.isConnected ? comoCarta(rectDe(el), el.offsetWidth) : caja;
  const anim = SIN_MOVIMIENTO ? null : fantasma.animate([
    { transform: fantasma.style.transform },
    { transform: `translate(${destino.left - caja.left}px, ${destino.top - caja.top}px) rotate(0deg)` },
  ], { duration: 180, easing: "ease-out", fill: "forwards" });
  const fin = () => { fantasma.remove(); el.classList.remove("arrastrada"); };
  if (anim) anim.finished.catch(() => {}).then(fin); else fin();
}

/** Soltar en el pozo = descartar. La carta queda flotando hasta que el servidor confirma. */
function soltarEnPozo(carta, el, fantasma) {
  app.elegida = null;
  const espera = setTimeout(() => cancelarSoltada(), 2500);
  app.soltada = { clave: claveCarta(carta), el, fantasma, espera };
  enviar("DESCARTAR", { carta });
}

function cancelarSoltada() {
  const s = app.soltada;
  if (!s) return;
  clearTimeout(s.espera);
  app.soltada = null;
  s.fantasma.remove();
  s.el.classList.remove("arrastrada");
}

/** Mueve la carta a la posicion de la mano mas cercana al punto donde se solto. */
function reordenarHasta(clave, x) {
  const desde = app.mano.findIndex((c) => claveCarta(c) === clave);
  const otras = [...$("mano").children].filter((n) => n.dataset.clave !== clave);
  const hasta = otras.filter((n) => { const r = n.getBoundingClientRect(); return r.left + r.width / 2 < x; }).length;
  moverCarta(desde, hasta);
}

function moverCarta(desde, hasta) {
  if (desde === hasta || Number.isNaN(desde) || Number.isNaN(hasta)) return;
  const [carta] = app.mano.splice(desde, 1);
  app.mano.splice(hasta, 0, carta);
  pintarMano();
  enviar("ORDENAR", { orden: app.mano });
}

function ordenarMano(criterio) {
  const rango = (c) => VALORES.indexOf(c.valor);
  const palo = (c) => Object.keys(PALOS).indexOf(c.palo);
  app.mano.sort(criterio === "palo" ? (a, b) => palo(a) - palo(b) || rango(a) - rango(b)
    : (a, b) => rango(a) - rango(b) || palo(a) - palo(b));
  pintarMano(); // las cartas se deslizan a su nuevo lugar con la transicion CSS
  enviar("ORDENAR", { orden: app.mano });
}

$("btnOrdenNumero").addEventListener("click", () => ordenarMano("numero"));
$("btnOrdenPalo").addEventListener("click", () => ordenarMano("palo"));
$("mazo").addEventListener("click", () => enviar("ROBAR_MAZO"));
$("pozo").addEventListener("click", () => enviar("TOMAR_POZO"));
$("btnDescartar").addEventListener("click", () => {
  const carta = app.mano.find((c) => claveCarta(c) === app.elegida);
  if (!carta) return;
  enviar("DESCARTAR", { carta });
  app.elegida = null;
});
$("btnCerrar").addEventListener("click", () => {
  const carta = app.mano.find((c) => claveCarta(c) === app.elegida);
  if (carta) enviar("CERRAR", { carta });
});
window.addEventListener("resize", () => { if (app.estado) pintarMano(); });

// ---------- fin de ronda ----------
function mostrarResultadoRonda() {
  const e = app.estado;
  const c = e.ultimoCierre;
  if (!c) return;
  const terminada = e.estado === "FINALIZADA";
  const ganador = terminada ? e.jugadores.find((j) => j.id === e.ganadorId) : null;
  $("rondaTitulo").textContent = terminada
    ? `¡${ganador ? ganador.nombre : "Alguien"} gana la partida!`
    : `${c.cerradorNombre} cerró la ronda ${c.ronda}`;

  const comb = $("rondaCombinaciones");
  comb.innerHTML = "";
  let retraso = 0;
  c.combinacionesCerrador.forEach((g) => { comb.appendChild(grupoCartas(g, "", retraso)); retraso += g.length * 70; });
  if (c.sobrantesCerrador.length) comb.appendChild(grupoCartas(c.sobrantesCerrador, "sobrante", retraso));
  const desc = document.createElement("div");
  desc.className = "sub";
  desc.textContent = "Descartó el " + textoCarta(c.descarte);
  comb.appendChild(desc);

  const tbody = $("rondaTabla");
  tbody.innerHTML = "";
  c.penalizaciones.forEach((p) => {
    const tr = document.createElement("tr");
    const celda = document.createElement("td");
    celda.appendChild(grupoCartas(p.cartasSueltas, "", 200));
    tr.innerHTML = `<td>${escapar(p.nombre)}${p.jugadorId === c.cerradorId ? " (cerró)" : ""}</td>`;
    tr.appendChild(celda);
    tr.insertAdjacentHTML("beforeend", `<td>+${p.puntos}</td><td>${p.acumulado}${p.eliminado ? ' <span class="etiq" style="background:#cfcac0">eliminado</span>' : ""}</td>`);
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

// ---------- menu, ayuda y arranque ----------
function abrirMenu(abrir) {
  $("menu").classList.toggle("abierto", abrir);
  $("menu").setAttribute("aria-hidden", String(!abrir));
}
$("btnMenu").addEventListener("click", () => abrirMenu(!$("menu").classList.contains("abierto")));
$("btnCerrarMenu").addEventListener("click", () => abrirMenu(false));
$("btnAyuda").addEventListener("click", () => $("dlgAyuda").showModal());
$("btnAyudaMenu").addEventListener("click", () => { abrirMenu(false); $("dlgAyuda").showModal(); });
$("btnSalir").addEventListener("click", () => { abrirMenu(false); salir(); });
document.querySelectorAll("[data-cerrar]").forEach((b) => b.addEventListener("click", () => b.closest("dialog").close()));

function salir() {
  try { sessionStorage.removeItem(CLAVE_SESION); } catch (e) { /* nada */ }
  const ws = app.ws;
  app.credencial = null;
  app.ws = null;
  if (ws) ws.close();
  clearInterval(app.sondeoSala);
  app.mano = [];
  app.estado = null;
  $("mano").innerHTML = "";
  $("asientos").innerHTML = "";
  $("infoMesa").classList.add("oculto");
  $("latencia").classList.add("oculto");
  $("menuPartida").classList.add("oculto");
  mostrarVista("vistaInicio");
}

(async function iniciar() {
  pintarPortada();
  try { await cargarModalidades(); } catch (err) { aviso("No se pudo contactar al servidor", "error"); }
  const params = new URLSearchParams(location.search);
  if (params.get("sala")) document.querySelector("#formUnirse [name=codigo]").value = params.get("sala");
  const guardada = leerCredencial();
  if (guardada) {
    app.credencial = guardada;
    entrarASala();
  }
})();
