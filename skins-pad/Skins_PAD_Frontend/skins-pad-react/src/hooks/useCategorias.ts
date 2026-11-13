import { categoriaService } from "../services/categoria.service";
import type { Categoria } from "../lib/api";
import { useCarga } from "./useCarga";

const VACIO: Categoria[] = [];
export function useCategorias() {
  return useCarga(categoriaService.listar, VACIO);
}
