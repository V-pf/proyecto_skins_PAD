import { useLocation } from "react-router-dom";
import { categoriaService } from "../services/categoria.service";
import type { Categoria } from "../lib/api";
import { useCarga } from "./useCarga";

const VACIO: Categoria[] = [];
export function useCategorias() {
  const estadisticas = useLocation().pathname === "/admin/estadisticas";
  return useCarga(categoriaService.listar, VACIO, !estadisticas);
}
