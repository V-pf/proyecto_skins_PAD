import { createContext, useContext } from "react";
import type { ReactNode } from "react";
import { carritoService } from "../services/carrito.service";
import { useCarritoRemoto } from "../hooks/useCarritoRemoto";
import { useAuth } from "./AuthContext";
import { useTienda } from "./TiendaContext";
import { calcularTotales } from "../lib/formato";
import type { ItemCarrito, Producto } from "../types";



interface Carrito {
  cargando: boolean;
  items: ItemCarrito[];
  /** Los ítems del carrito unidos con los datos del producto (si todavía existe). */
  lineas: { producto: Producto; cantidad: number }[];
  cantidadTotal: number;
  totales: { subtotal: number; comision: number; total: number };
  agregar: (productoId: number, cantidad?: number) => Promise<void>;

  quitar: (productoId: number) => Promise<void>;
  vaciar: () => Promise<void>;
  enCarrito: (productoId: number) => number;
}

const Ctx = createContext<Carrito | null>(null);

export function CarritoProvider({ children }: { children: ReactNode }) {
  const { productos } = useTienda();
  const { usuario } = useAuth();
  const carga = useCarritoRemoto(usuario?.id);
  const cart = carga.datos;
  const items = cart?.items ?? [];
  async function cambiar(operacion: (id: number) => Promise<void>) {
    if (!usuario) throw new Error("Inicia sesión para usar el carrito.");
    if (!cart) throw new Error("El carrito todavía no está disponible.");
    await operacion(cart.id);
    carga.setDatos(await carritoService.obtener(cart.id));
  }
  const lineas = items.flatMap((it) => {
    const producto = productos.find((p) => p.id === it.productoId);
    return producto ? [{ producto, cantidad: it.cantidad }] : [];
  });
  const subtotal = lineas.reduce(
    (suma, l) => suma + l.producto.precio * l.cantidad,
    0,
  );

  const valor: Carrito = {
    cargando: carga.cargaInicial,
    items,
    lineas,
    cantidadTotal: lineas.reduce((suma, l) => suma + l.cantidad, 0),
    totales: calcularTotales(subtotal),
    agregar: (productoId) => cambiar((id) => carritoService.agregar(id, productoId)),
    quitar: (productoId) => cambiar((id) => carritoService.quitar(id, productoId)),

    vaciar: () => cambiar(carritoService.vaciar),
    enCarrito: (productoId) =>
      items.find((i) => i.productoId === productoId)?.cantidad ?? 0,
  };

  return (
    <Ctx.Provider value={valor}>
      {carga.error && <div className="container py-4"><p role="alert">{carga.error} <button onClick={() => void carga.recargar()}>Reintentar</button></p></div>}
      {children}
    </Ctx.Provider>
  );
}

export function useCarrito(): Carrito {
  const ctx = useContext(Ctx);
  if (!ctx)
    throw new Error("useCarrito debe usarse dentro de <CarritoProvider>");
  return ctx;
}
