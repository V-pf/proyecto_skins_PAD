import { useMatch } from "react-router-dom";
import { useCallback } from "react";
import { pagoService } from "../services/pago.service";
import type { Pago } from "../lib/api";
import type { Usuario } from "../types";
import { useCarga } from "./useCarga";

const VACIO: Pago[] = [];
export function usePagos(usuario: Usuario | null) {
  const id = usuario?.id;
  const ventas = useMatch("/admin/ventas");
  const gestion = useMatch("/admin/pagos");
  const global = usuario?.rol === "admin" && Boolean(ventas || gestion);
  const cargar = useCallback((signal: AbortSignal) =>
    id ? pagoService.listar(id, global, signal) : Promise.resolve(VACIO), [id, global]);
  return useCarga(cargar, VACIO, Boolean(id));
}
