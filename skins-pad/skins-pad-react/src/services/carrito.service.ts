import { solicitar } from "./http";
import type { ItemCarrito } from "../types";

export type CarritoRemoto = { id: number; items: ItemCarrito[] };

export const carritoService = {
  obtenerPorUsuario: (usuarioId: string, signal?: AbortSignal) =>
    solicitar<CarritoRemoto>("/carritos/usuario/" + usuarioId, "GET", undefined, signal),
  obtener: (id: number, signal?: AbortSignal) =>
    solicitar<CarritoRemoto>("/carritos/" + id, "GET", undefined, signal),
  agregar: (id: number, productoId: number) =>
    solicitar<void>("/carritos/" + id + "/items", "POST", { productoId }),
  quitar: (id: number, productoId: number) =>
    solicitar<void>("/carritos/" + id + "/items/" + productoId, "DELETE"),
  vaciar: (id: number) => solicitar<void>("/carritos/" + id + "/items", "DELETE"),
};
