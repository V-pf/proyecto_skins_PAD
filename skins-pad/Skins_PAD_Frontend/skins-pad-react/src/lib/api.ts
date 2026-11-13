import type { Usuario, Producto } from "../types";
export { ErrorAPI } from "../services/http";
export interface UsuarioAPI extends Usuario {
  username: string;
  email: string;
  linkSteam: string;
}
export const usuarioUI = (u: UsuarioAPI): Usuario => ({
  ...u,
  rol: u.rol === "admin" ? "admin" : "cliente",
  usuario: u.username,
  correo: u.email,
  urlSteam: u.linkSteam,
});
export interface Categoria {
  id: number;
  nombre: string;
  estadoHabilitada: boolean;
  descripcion: string;
}
export interface ProductoAPI extends Omit<Producto, "estado"> {
  estado: string;
  floatValor: number | null;
  categoria: Categoria;
}
export function productoUI(p: ProductoAPI): Producto {
  const estados: Record<string, Producto["estado"]> = {
    ACTIVO: "activa",
    PENDIENTE_APROBACION: "pendiente",
    RECHAZADO: "rechazada",
    VENDIDO: "vendida",
    INACTIVO: "inactiva",
    ELIMINADO: "eliminada",
  };
  return {
    ...p,
    estado: estados[p.estado] || "inactiva",
    float: p.floatValor,
    categoriaId: p.categoria.id,
  };
}
export interface Pago {
  id: number;
  usuarioId: string;
  estado: string;
  metodo: string;
  fecha: string;
  montoTotal: number;
  numeroOperacion?: string;
  liquidado?:boolean;
  cuotas?: number;
  referenciaExterna?: string;
  precios?:Record<string,number>;
  productoIds?: number[];
}
export function fallo(e: unknown) {
  return e instanceof Error
    ? e.message
    : "No se pudo conectar con el servidor.";
}
