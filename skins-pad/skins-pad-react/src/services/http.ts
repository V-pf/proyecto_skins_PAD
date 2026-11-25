import axios from "axios";
import { leerSesion, limpiarSesion, reemplazarToken } from "../lib/storage";

export class ErrorAPI extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

const cliente = axios.create({
  withCredentials: true,
  baseURL: import.meta.env.VITE_API_URL || "/api",
  // Conserva el tratamiento de códigos HTTP que ya utiliza AuthContext.
  validateStatus: () => true,
});

// Esta lectura solo decide cuándo renovar. La autoridad sobre los permisos es usuario.
function datosToken(token: string): { sid: string; exp: number } | null {
  try {
    return JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
  } catch {
    return null;
  }
}

const renovaciones = new Map<string, Promise<string>>();

async function renovar(actual: string, sid: string): Promise<string> {
  const respuesta = await axios.post<{ token: string }>("/auth/renovar", null, {
    baseURL: cliente.defaults.baseURL,
    withCredentials: true,
    headers: { "X-Skins-Sesion": sid },
    validateStatus: () => true,
  });
  if (respuesta.status !== 200) {
    if ([401, 403].includes(respuesta.status) && leerSesion() === actual) {
      limpiarSesion();
      window.dispatchEvent(new Event("skins:sesion-cerrada"));
    }
    throw new ErrorAPI(respuesta.status, "No se pudo renovar la sesión.");
  }
  const nuevo = respuesta.data.token;
  if (reemplazarToken(actual, nuevo)) return nuevo;
  const vigente = leerSesion();
  if (vigente && datosToken(vigente)?.sid === sid) return vigente;
  throw new ErrorAPI(401, "La sesión cambió durante la solicitud.");
}

function renovarUnaVez(actual: string, sid: string): Promise<string> {
  let pendiente = renovaciones.get(actual);
  if (!pendiente) {
    pendiente = renovar(actual, sid).finally(() => renovaciones.delete(actual));
    renovaciones.set(actual, pendiente);
  }
  return pendiente;
}

export function mantenerSesionActiva(): () => void {
  let siguienteRevision = 0;
  const revisar = () => {
    const ahora = Date.now();
    if (ahora < siguienteRevision) return;
    siguienteRevision = ahora + 30000;
    const actual = leerSesion();
    const datos = actual && datosToken(actual);
    if (actual && datos && datos.exp * 1000 <= ahora + 60000) {
      // Si falla la red, la siguiente interacción vuelve a intentarlo.
      void renovarUnaVez(actual, datos.sid).catch(() => {});
    }
  };
  const eventos = ["pointerdown", "keydown", "scroll", "focus"];
  eventos.forEach(evento => window.addEventListener(evento, revisar, { passive: true, capture: true }));
  return () => eventos.forEach(evento => window.removeEventListener(evento, revisar, true));
}

cliente.interceptors.request.use(async (config) => {
  const actual = leerSesion();
  if (!actual || config.url?.startsWith("/auth/login") || config.url === "/auth/registro") return config;
  let token = actual;
  const datos = datosToken(actual);
  if (datos?.sid && datos.exp * 1000 <= Date.now() + 60000) {
    token = await renovarUnaVez(actual, datos.sid);
  }
  config.headers.set("Authorization", "Bearer " + token);
  return config;
});

// Recupera una clave rotada o un desfase de reloj. Solo reintenta una vez por petición.
cliente.interceptors.response.use(async (respuesta) => {
  const config = respuesta.config as typeof respuesta.config & { renovada?: boolean };
  const bearer = config.headers.get("Authorization");
  if (respuesta.status !== 401 || config.renovada || typeof bearer !== "string") return respuesta;
  const anterior = bearer.slice(7);
  const datos = datosToken(anterior);
  const actual = leerSesion();
  if (!datos?.sid || !actual || datosToken(actual)?.sid !== datos.sid) return respuesta;
  if (actual === anterior) await renovarUnaVez(actual, datos.sid);
  config.renovada = true;
  return cliente.request(config);
});

export async function solicitar<T>(
  ruta: string,
  metodo = "GET",
  datos?: unknown,
  signal?: AbortSignal,
): Promise<T> {
  const respuesta = await cliente.request<T>({
    url: ruta,
    method: metodo,
    data: datos,
    signal,
    headers: datos === undefined ? undefined : { "Content-Type": "application/json" },
  });
  if (respuesta.status < 200 || respuesta.status >= 300) {
    const cuerpo = respuesta.data as { message?: string } | undefined;
    throw new ErrorAPI(
      respuesta.status,
      cuerpo?.message || "No se pudo completar la operación (" + respuesta.status + ").",
    );
  }
  return (respuesta.status === 204 || respuesta.data === "" ? undefined : respuesta.data) as T;
}

export async function descargarAdjunto(id:number,nombre:string,servicio:"tickets"|"reportes"):Promise<void>{
 const respuesta=await cliente.get('/'+servicio+'/adjuntos/'+id,{responseType:'blob'});
 if(respuesta.status!==200)throw new Error('No se pudo descargar la evidencia ('+respuesta.status+').');
 const url=URL.createObjectURL(respuesta.data);const enlace=document.createElement('a');enlace.href=url;enlace.download=nombre;enlace.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
}
