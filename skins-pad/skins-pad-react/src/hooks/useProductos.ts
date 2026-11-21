import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { productoService } from "../services/producto.service";
import type { Producto } from "../types";
import { useCarga } from "./useCarga";

const VACIO: Producto[] = [];
export function useProductos() {
  const { pathname } = useLocation();
  const carga = useCarga(productoService.listar, VACIO, pathname !== "/admin/estadisticas");
  const { recargar } = carga;
  useEffect(() => {
    if (!["/catalogo", "/cliente/publicaciones", "/cliente/carrito"].includes(pathname) &&
        !pathname.startsWith("/producto/")) return;
    let activa = true;
    let timer: ReturnType<typeof setTimeout>;
    let turno = 0;
    async function actualizar() {
      clearTimeout(timer);
      const actual = ++turno;
      if (document.hidden) return;
      await recargar();
      if (activa && turno === actual) timer = setTimeout(actualizar, 10000);
    }
    void actualizar();
    window.addEventListener("focus", actualizar);
    document.addEventListener("visibilitychange", actualizar);
    return () => {
      activa = false;
      clearTimeout(timer);
      window.removeEventListener("focus", actualizar);
      document.removeEventListener("visibilitychange", actualizar);
    };
  }, [pathname, recargar]);
  return carga;
}
