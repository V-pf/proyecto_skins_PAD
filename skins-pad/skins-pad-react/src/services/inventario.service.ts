import { solicitar } from "./http";
import type { ProductoAPI } from "../lib/api";
import type { Tipo, Desgaste, Grado } from "../types";
export interface ItemInventario {
  id: number; assetId: string; nombre: string; tipo: Tipo | null; grado: Grado;
  desgaste: Desgaste; stattrak: boolean; imagen: string | null; intercambiable: boolean;
  presente: boolean; demo: boolean; productoId: number | null;
}
export interface ItemVista { item: ItemInventario; estadoPublicacion: string | null; publicable: boolean }
export interface Inventario { steamId: string | null; demo: boolean; sincronizado: string | null; items: ItemVista[] }
export interface SeriePrecios { demo: boolean; moneda: string; criterio: string; puntos: { fecha: string; precioMedio: number; ventas: number }[] }
export const inventarioService = {
  consultar: (signal?: AbortSignal) => solicitar<Inventario>("/inventario", "GET", undefined, signal),
  sincronizar: (signal?: AbortSignal) => solicitar<Inventario>("/inventario/sincronizar", "POST", undefined, signal),
  vincular: () => solicitar<{ url: string }>("/steam/vincular", "POST"),
  publicar: (itemId: number, precio: number) => solicitar<ProductoAPI>("/inventario/publicaciones", "POST", { itemId, precio }),
  precios: (id: number, dias: number, signal?: AbortSignal) => solicitar<SeriePrecios>("/inventario/" + id + "/precios?dias=" + dias, "GET", undefined, signal),
};
