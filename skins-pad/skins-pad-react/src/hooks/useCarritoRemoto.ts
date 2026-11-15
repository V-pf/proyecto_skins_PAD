import { useCallback } from "react";
import { carritoService, type CarritoRemoto } from "../services/carrito.service";
import { useCarga } from "./useCarga";

export function useCarritoRemoto(usuarioId?: string) {
  const cargar = useCallback((signal: AbortSignal) =>
    usuarioId ? carritoService.obtenerPorUsuario(usuarioId, signal) : Promise.resolve(null), [usuarioId]);
  return useCarga<CarritoRemoto | null>(cargar, null, Boolean(usuarioId));
}
