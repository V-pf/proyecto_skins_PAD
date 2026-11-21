import { solicitar } from "./http";
export interface Resena { id: number; operacionId: number; puntaje: number; comentario: string; fecha: string }
export interface Reputacion { promedio: number | null; total: number; trustedSeller: boolean; resenas: Resena[] }
export const calificacionService = {
  consultar: (id: string, signal?: AbortSignal) => solicitar<Reputacion>(`/calificaciones/vendedor/${encodeURIComponent(id)}`, "GET", undefined, signal),
  crear: (operacionId: number, puntaje: number, comentario: string) => solicitar<Resena>("/calificaciones", "POST", { operacionId, puntaje, comentario }),
};
