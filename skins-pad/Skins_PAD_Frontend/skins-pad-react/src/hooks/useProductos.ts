import { productoService } from "../services/producto.service";
import type { Producto } from "../types";
import { useCarga } from "./useCarga";

const VACIO: Producto[] = [];
export function useProductos() {
  return useCarga(productoService.listar, VACIO);
}
