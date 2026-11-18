import { solicitar } from "./http";
import type { Categoria } from "../lib/api";

export const categoriaService = {
  listar: (signal?: AbortSignal) =>
    solicitar<Categoria[]>("/categorias", "GET", undefined, signal),
};
