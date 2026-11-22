import type { Usuario, Producto } from "../types";
export { ErrorAPI } from "../services/http";
export interface UsuarioAPI extends Usuario {
  username: string;
  email: string;
  linkSteam: string;
}
// Pasa los nombres de campos del servicio de usuarios a los que usa React.
export const usuarioUI = (u: UsuarioAPI): Usuario => ({
  ...u,
  rol: u.rol === "admin" ? "admin" : "cliente",
  usuario: u.username,
  correo: u.email,
  urlSteam: u.linkSteam,
});
// Datos de una categoría y su disponibilidad para nuevas publicaciones.
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
// Adapta el estado, el valor float y el identificador de categoría para las pantallas.
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
// Datos de un cobro para ver su estado y consultar las skins incluidas.
export interface Pago {
  id: number;
  usuarioId: string;
  estado: string;
  metodo: string;
  fecha: string;
  montoTotal: number;
  simulado?: boolean | null;
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
