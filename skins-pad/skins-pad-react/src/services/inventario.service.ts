import { solicitar } from "./http";
import type { ProductoAPI } from "../lib/api";
import type { Tipo, Desgaste, Grado } from "../types";
export interface ItemInventario {
  id: number; assetId: string; nombre: string; tipo: Tipo | null; grado: Grado;
  desgaste: Desgaste | null; stattrak: boolean; imagen: string | null; intercambiable: boolean;
  presente: boolean; productoId: number | null; productoOrigenId: number | null;
}
export interface ItemVista { item: ItemInventario; estadoPublicacion: string | null; publicable: boolean }
export interface Inventario { items: ItemVista[] }
export interface SeriePrecios { moneda: string; criterio: string; puntos: { fecha: string; precioMedio: number; ventas: number }[] }
export const inventarioService = {
  consultar: (signal?: AbortSignal) => solicitar<Inventario>("/inventario", "GET", undefined, signal),
  publicar: (itemId: number, precio: number) => solicitar<ProductoAPI>("/inventario/publicaciones", "POST", { itemId, precio }),
  precios: (id: number, dias: number, signal?: AbortSignal) => solicitar<SeriePrecios>("/inventario/" + id + "/precios?dias=" + dias, "GET", undefined, signal),
};
