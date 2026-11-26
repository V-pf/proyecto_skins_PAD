const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");
const axios = require("axios");

function token(exp) {
  return "test." + Buffer.from(JSON.stringify({ exp, sid: "misma-sesion" })).toString("base64url") + ".test";
}

function escenario(inicio, estadoRenovacion = 200) {
  let actual = inicio;
  const nuevo = token(Math.floor(Date.now() / 1000) + 900);
  const peticiones = [];
  let renovaciones = 0;
  const cliente = axios.create({ adapter: async config => {
    peticiones.push(config.headers.get("Authorization"));
    return { status: 200, config, data: {} };
  } });
  const transporte = { create: () => cliente, post: async (_url, _body, config) => {
    assert.equal(config.headers["X-Skins-Sesion"], "misma-sesion");
    assert.equal(config.withCredentials, true);
    renovaciones++;
    await new Promise(resolve => setImmediate(resolve));
    return { status: estadoRenovacion, data: { token: nuevo } };
  } };
  const almacen = { leerSesion: () => actual, limpiarSesion: () => { actual = null; },
    reemplazarToken: (antes, despues) => {
      if (actual !== antes) return false;
      actual = despues;
      return true;
    } };
  const fuente = fs.readFileSync(path.join(__dirname, "../src/services/http.ts"), "utf8")
    .replace("import.meta.env.VITE_API_URL", '"/api"');
  const js = ts.transpileModule(fuente, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true,
  } }).outputText;
  const ventana = new EventTarget();
  const exports = {};
  vm.runInNewContext(js, { exports, require: name => name === "axios" ? transporte : almacen,
    atob, Event, window: ventana });
  return { ventana, mantener: exports.mantenerSesionActiva, pedir: () => exports.solicitar("/carritos/1"), nuevo, peticiones,
    renovaciones: () => renovaciones, actual: () => actual };
}

test("navegar después de caducar renueva una sola vez para dos solicitudes simultáneas", async () => {
  const s = escenario(token(1));
  await Promise.all([s.pedir(), s.pedir()]);
  assert.equal(s.renovaciones(), 1);
  assert.equal(s.actual(), s.nuevo);
  assert.deepEqual(s.peticiones, Array(2).fill("Bearer " + s.nuevo));
});

test("un error temporal al renovar conserva la sesión para reintentar", async () => {
  const anterior = token(1);
  const s = escenario(anterior, 503);
  await assert.rejects(s.pedir(), e => e.status === 503);
  assert.equal(s.actual(), anterior);
  assert.equal(s.peticiones.length, 0);
});

test("una renovación revocada cierra la sesión y detiene la solicitud", async () => {
  const s = escenario(token(1), 401);
  await assert.rejects(s.pedir(), e => e.status === 401);
  assert.equal(s.actual(), null);
  assert.equal(s.peticiones.length, 0);
});

test("la actividad de navegación renueva aunque la pantalla no haga una solicitud de datos", async () => {
  const s = escenario(token(1));
  const detener = s.mantener();
  s.ventana.dispatchEvent(new Event("keydown"));
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(s.actual(), s.nuevo);
  assert.equal(s.renovaciones(), 1);
  assert.equal(s.peticiones.length, 0);
  detener();
});
