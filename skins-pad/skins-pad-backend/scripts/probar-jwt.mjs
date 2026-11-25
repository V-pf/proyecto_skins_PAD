// Ejecutar después de «mvn -Pdemo package» en cada servicio. Usa solo bases H2 en memoria.
import assert from "node:assert/strict";
import { once } from "node:events";
import { spawn } from "node:child_process";
import { generateKeyPairSync, randomUUID, sign } from "node:crypto";
import { readdir } from "node:fs/promises";
import { createServer } from "node:net";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { setTimeout as esperar } from "node:timers/promises";

const root = fileURLToPath(new URL("../", import.meta.url));
const procesos = [];
const urls = {};
const servicios = ["usuario", "catalogo", "carrito", "pago", "gateway"];
const entorno = { ...process.env, INTERNAL_API_KEY: randomUUID() };

const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
const jwk = { ...privateKey.export({ format: "jwk" }), kid: "prueba-local" };

function firmarFixture(claims) {
  const base64 = obj => Buffer.from(JSON.stringify(obj)).toString("base64url");
  const entrada = base64({ alg: "RS256", kid: jwk.kid }) + "." + base64(claims);
  return entrada + "." + sign("RSA-SHA256", Buffer.from(entrada), privateKey).toString("base64url");
}

async function puertoLibre() {
  const socket = createServer();
  await new Promise(resolve => socket.listen(0, "127.0.0.1", resolve));
  const port = socket.address().port;
  await new Promise(resolve => socket.close(resolve));
  return port;
}

async function iniciar(nombre) {
  const directorio = path.join(root, nombre);
  const target = path.join(directorio, "target");
  const jar = (await readdir(target)).find(f => f.endsWith(".jar"));
  assert.ok(jar, "Falta el paquete demo de " + nombre);
  const clase = nombre[0].toUpperCase() + nombre.slice(1) + "Application";
  const args = ["-Xmx224m", "-XX:ActiveProcessorCount=2", "-Dspring.devtools.restart.enabled=false",
    "-jar", path.join(target, jar), "--spring.profiles.active=demo",
    "--server.address=127.0.0.1", "--server.port=" + new URL(urls[nombre]).port,
    "--spring.datasource.url=jdbc:h2:mem:jwt;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "--spring.jpa.hibernate.ddl-auto=create-drop", "--spring.mail.host=127.0.0.1"];
  const ejecutable = process.platform === "win32" && process.env.JAVA_HOME
    ? path.join(process.env.JAVA_HOME, "bin", "java.exe") : "java";
  const child = spawn(ejecutable, args, { cwd: directorio, env: { ...entorno, SKINS_JWT_PRIVATE_JWK: nombre === "usuario" ? JSON.stringify(jwk) : "" }, windowsHide: true });
  procesos.push(child);
  let log = "";
  child.stdout.on("data", b => { log = (log + b).slice(-16000); });
  child.stderr.on("data", b => { log = (log + b).slice(-16000); });
  for (let i = 0; i < 180; i++) {
    if (child.exitCode !== null) throw Error(nombre + " no arrancó\n" + log.slice(-4500));
    if (log.includes("Started " + clase)) { console.log(nombre + " listo"); return; }
    await esperar(500);
  }
  throw Error("Tiempo de arranque agotado para " + nombre + "\n" + log.slice(-1500));
}

async function pedir(servicio, ruta, status, { metodo = "GET", token, datos, headers = {} } = {}) {
  const response = await fetch(urls[servicio] + "/api" + ruta, {
    method: metodo, headers: { ...headers, ...(token && { Authorization: "Bearer " + token }),
      ...(datos && { "Content-Type": "application/json" }) }, body: datos && JSON.stringify(datos),
  });
  assert.equal(response.status, status, servicio + " " + metodo + " " + ruta + " devolvió " + response.status);
  return response;
}

async function login(identificador, password) {
  const resp = await pedir("gateway", "/auth/login?recordar=true", 200,
    { metodo: "POST", datos: { identificador, password } });
  const { token } = await resp.json();
  const claims = JSON.parse(Buffer.from(token.split(".")[1], "base64url"));
  assert.equal(claims.exp - claims.iat, 900);
  const cookie = resp.headers.get("set-cookie");
  assert.match(cookie, /HttpOnly/i);
  assert.match(cookie, /SameSite=Strict/i);
  return { token, sid: claims.sid, cookie: cookie.split(";")[0] };
}

try {
  for (const nombre of servicios) {
    urls[nombre] = "http://127.0.0.1:" + await puertoLibre();
    entorno[nombre.toUpperCase() + "_URL"] = urls[nombre];
  }
  for (const nombre of servicios) await iniciar(nombre);
  const usuario = await login("penquista_gamer", "Demo1234!");
  const admin = await login("admin", "Admin1234!");
  const renovar = (sesion, status) => pedir("gateway", "/auth/renovar", status, {
    metodo: "POST", headers: { Cookie: sesion.cookie, "X-Skins-Sesion": sesion.sid } });
  await pedir("gateway", "/auth/validar", 401, { token: randomUUID() });
  const { keys } = await (await pedir("gateway", "/auth/jwks", 200)).json();
  assert.equal(keys[0].kty, "RSA"); assert.equal(keys[0].d, undefined);
  const nuevo = (await (await renovar(usuario, 200)).json()).token;
  await pedir("gateway", "/auth/validar", 200, { token: nuevo });
  await renovar({ ...usuario, cookie: admin.cookie }, 401);
  console.log("Login, clave pública, renovación y aislamiento entre sesiones correctos.");

  const claims = JSON.parse(Buffer.from(nuevo.split(".")[1], "base64url"));
  for (const cambio of [{ exp: claims.iat - 1 }, { aud: "otra-api" }, { iss: "otro-emisor" }]) {
    const token = firmarFixture({ ...claims, ...cambio });
    await pedir("gateway", "/auth/validar", 401, { token });
    await pedir("usuario", "/auth/validar", 401, { token });
    await pedir("carrito", "/carritos/usuario/u1", 401, { token });
    await pedir("pago", "/pagos", 401, { token });
    await pedir("catalogo", "/categorias", 401, { token, metodo: "POST", datos: {} });
  }
  const carrito = await (await pedir("gateway", "/carritos/usuario/u1", 200, usuario)).json();
  await pedir("gateway", "/carritos/" + carrito.id, 200, usuario);
  await pedir("carrito", "/carritos/" + carrito.id, 401);
  await pedir("carrito", "/carritos/usuario/u3", 403, usuario);
  await pedir("usuario", "/usuarios/u3", 403, usuario);
  await pedir("catalogo", "/categorias", 403, { ...usuario, metodo: "POST", datos: {} });
  await pedir("catalogo", "/productos/reservas", 403, { ...usuario, metodo: "POST", datos: {} });
  await pedir("pago", "/pagos", 403, usuario);
  const alterado = usuario.token.split(".");
  alterado[2] = (alterado[2][0] === "A" ? "B" : "A") + alterado[2].slice(1);
  for (const [servicio, ruta] of [["gateway", "/auth/validar"], ["carrito", "/carritos/usuario/u1"],
    ["usuario", "/usuarios/u1"], ["pago", "/pagos"]]) {
    await pedir(servicio, ruta, 401, { token: alterado.join(".") });
  }
  await pedir("catalogo", "/categorias", 401, { token: alterado.join("."), metodo: "POST", datos: {} });
  const productos = await (await pedir("gateway", "/productos", 200)).json();
  const producto = productos.find(p => p.estado === "ACTIVO" && p.vendedorId !== "u1");
  await pedir("gateway", "/pagos/checkout", 201, { ...usuario, metodo: "POST",
    datos: { solicitudId: randomUUID(), metodo: "TRANSFERENCIA", productoIds: [producto.id] } });
  console.log("Firma, titularidad, rutas internas y checkout verificados con los cinco procesos reales.");

  const cambiar = (id, datos) => pedir("gateway", "/usuarios/" + id + "/estado", 200,
    { ...admin, metodo: "PATCH", datos });
  await cambiar("u1", { suspendido: true, motivo: "Prueba aislada" });
  await pedir("carrito", "/carritos/usuario/u1", 403, { token: nuevo });
  await renovar(usuario, 403);
  await cambiar("u1", { suspendido: false });
  await pedir("gateway", "/auth/logout", 204, { token: nuevo, metodo: "POST" });
  await pedir("pago", "/pagos", 401, usuario);
  await renovar(usuario, 401);
  await pedir("gateway", "/usuarios", 200, admin);
  await cambiar("u2", { rol: "cliente" });
  await pedir("gateway", "/usuarios", 403, admin);
  await pedir("usuario", "/usuarios", 403, admin);
  await pedir("pago", "/pagos", 403, admin);
  await pedir("gateway", "/auth/validar", 200, admin);
  console.log("Suspensión, cierre y retirada de permisos efectivos con JWT previamente emitidos.");
} finally {
  await Promise.all(procesos.map(async child => {
    if (child.exitCode !== null || child.signalCode !== null) return;
    const cierre = once(child, "close");
    child.kill();
    await cierre;
  }));
}
