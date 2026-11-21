import axios from "axios";
import { leerSesion } from "../lib/storage";

export class ErrorAPI extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

const cliente = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
  // Conserva el tratamiento de códigos HTTP que ya utiliza AuthContext.
  validateStatus: () => true,
});

// Envía el token guardado junto con cada solicitud que tenga una sesión activa.
cliente.interceptors.request.use((config) => {
  const token = leerSesion();
  if (token) config.headers.set("Authorization", "Bearer " + token);
  return config;
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
