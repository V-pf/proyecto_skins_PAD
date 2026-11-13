import { solicitar } from "./http";
import { productoUI, type ProductoAPI } from "../lib/api";

export const productoService = {
  async listar(signal?: AbortSignal) {
    const productos = await solicitar<ProductoAPI[]>("/productos", "GET", undefined, signal);
    return productos.map(productoUI);
  },
  async crear(datos: unknown) {
    return productoUI(await solicitar<ProductoAPI>("/productos", "POST", datos));
  },
  async actualizar(id: number, datos: unknown) {
    return productoUI(await solicitar<ProductoAPI>("/productos/" + id, "PUT", datos));
  },
};
