import { useLocation } from "react-router-dom";
import { productoService } from "../services/producto.service";
import type { Producto } from "../types";
import { useCarga } from "./useCarga";

const VACIO: Producto[] = [];
export function useProductos() {
  const estadisticas = useLocation().pathname === "/admin/estadisticas";
  return useCarga(productoService.listar, VACIO, !estadisticas);
}
