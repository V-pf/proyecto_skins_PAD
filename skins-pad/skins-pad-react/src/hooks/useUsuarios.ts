import { useLocation } from "react-router-dom";
import { useCallback } from "react";
import { usuarioService } from "../services/usuario.service";
import type { Usuario } from "../types";
import { useCarga } from "./useCarga";

const VACIO: Usuario[] = [];
export function useUsuarios(usuario: Usuario | null) {
  const estadisticas = useLocation().pathname === "/admin/estadisticas";
  const id = usuario?.id;
  const rol = usuario?.rol;
  // Administración carga el listado de cuentas. Las demás pantallas usan los datos públicos.
  const cargar = useCallback((signal: AbortSignal) =>
    usuarioService.listar(rol === "admin", signal), [id, rol]);
  return useCarga(cargar, VACIO, !estadisticas);
}
