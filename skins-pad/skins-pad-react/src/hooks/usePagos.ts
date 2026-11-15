import { useLocation } from "react-router-dom";
import { useCallback } from "react";
import { pagoService } from "../services/pago.service";
import type { Pago } from "../lib/api";
import type { Usuario } from "../types";
import { useCarga } from "./useCarga";

const VACIO: Pago[] = [];
export function usePagos(usuario: Usuario | null) {
  const estadisticas = useLocation().pathname === "/admin/estadisticas";
  const id = usuario?.id;
  // El alcance depende del rol, no de la ruta donde se monte la gestión.
  const global = usuario?.rol === "admin";
  const cargar = useCallback((signal: AbortSignal) =>
    id ? pagoService.listar(id, global, signal) : Promise.resolve(VACIO), [id, global]);
  return useCarga(cargar, VACIO, Boolean(id) && !estadisticas);
}
