import { createContext, useContext, useRef, type ReactNode } from "react";
import type { Categoria, Pago } from "../lib/api";
import { useAuth } from "./AuthContext";
import type { Usuario, Producto } from "../types";
import { productoService } from "../services/producto.service";
import { usuarioService } from "../services/usuario.service";
import { pagoService } from "../services/pago.service";
import { useProductos } from "../hooks/useProductos";
import { useUsuarios } from "../hooks/useUsuarios";
import { useCategorias } from "../hooks/useCategorias";
import { usePagos } from "../hooks/usePagos";
interface Tienda {
  productos: Producto[];
  usuarios: Usuario[];
  categorias: Categoria[];
  pagos: Pago[];
  cargando: { productos: boolean; usuarios: boolean; categorias: boolean; pagos: boolean };
  recargar: () => Promise<void>;
  crearProducto: (p: Omit<Producto, "id">) => Promise<Producto>;
  actualizarProducto: (id: number, parche: Partial<Producto>) => Promise<void>;
  actualizarUsuario: (id: string, parche: Partial<Usuario>) => Promise<void>;
  pagar: (ids: number[], metodo: string) => Promise<Pago>;
}
const Ctx = createContext<Tienda | null>(null);
export function TiendaProvider({ children }: { children: ReactNode }) {
  const { usuario, refrescar } = useAuth();
  // Cada lista se carga por su cuenta para que las pantallas puedan consultar su estado por separado.
  const listaProductos = useProductos();
  const listaUsuarios = useUsuarios(usuario);
  const listaCategorias = useCategorias();
  const listaPagos = usePagos(usuario);
  const { datos: productos, setDatos: setProductos } = listaProductos;
  const { datos: usuarios, setDatos: setUsuarios } = listaUsuarios;
  const { datos: categorias } = listaCategorias;
  const { datos: pagos } = listaPagos;
  const solicitud = useRef({id:crypto.randomUUID(),firma:""});
  async function recargar() {
    await Promise.all([
      listaProductos.recargar(), listaUsuarios.recargar(),
      listaCategorias.recargar(), listaPagos.recargar(),
    ]);
  }
  const estados = {
    activa: "ACTIVO",
    pendiente: "PENDIENTE_APROBACION",
    rechazada: "RECHAZADO",
    vendida: "VENDIDO",
    eliminada: "ELIMINADO",
    inactiva: "INACTIVO",
  };
  const nombres: Record<string, string> = {
    rifle: "Rifles",
    francotirador: "Rifles de francotirador",
    pistola: "Pistolas",
    subfusil: "Subfusiles",
    cuchillo: "Cuchillos",
    guantes: "Guantes",
    sticker: "Stickers",
  };
  // Prepara la publicación para el catálogo y busca una categoría habilitada por tipo si falta el ID.
  function body(p: Omit<Producto, "id">) {
    const categoriaId =
      p.categoriaId ??
      categorias.find((c) => c.estadoHabilitada && c.nombre === nombres[p.tipo])
        ?.id;
    if (!categoriaId) throw new Error("Categoría no disponible.");
    return {
      ...p,
      imagenes: undefined,
      vendedorId: p.vendedorId ?? usuario?.id,
      floatValor: p.float,
      categoriaId,
      estado: estados[p.estado],
    };
  }
  const valor: Tienda = {
    productos,
    usuarios,
    categorias,
    pagos,
    recargar,
    cargando: {
      productos: listaProductos.cargaInicial, usuarios: listaUsuarios.cargaInicial,
      categorias: listaCategorias.cargaInicial, pagos: listaPagos.cargaInicial,
    },
    // Incorpora la publicación devuelta por el servidor y vuelve a consultar el catálogo.
    async crearProducto(p) {
      const nuevo = await productoService.crear(body(p));
      setProductos((prev) => [...prev, nuevo]);
      await listaProductos.recargar();
      return nuevo;
    },
    // Combina los cambios con los datos actuales porque el catálogo recibe la publicación completa.
    async actualizarProducto(id, parche) {
      const actual = productos.find((p) => p.id === id);
      if (!actual) throw new Error("Producto no encontrado");
      if (parche.estado === "eliminada") {
        await productoService.retirar(id, parche.motivoRechazo ?? "Retirada por el vendedor");
        setProductos(prev => prev.map(p => p.id === id ? { ...p, ...parche } : p));
        await listaProductos.recargar();
        return;
      }
      const nuevo = await productoService.actualizar(id, body({ ...actual, ...parche }));
      setProductos((prev) => prev.map((p) => (p.id === id ? nuevo : p)));
      await listaProductos.recargar();
    },
    // Si el cambio afecta a la cuenta activa, también se actualiza la sesión.
    async actualizarUsuario(id, parche) {
      const u = await usuarioService.actualizarEstado(id, parche);
      setUsuarios((prev) => prev.map((x) => (x.id === id ? u : x)));
      await listaUsuarios.recargar();
      if (usuario?.id === id) await refrescar();
    },
    async pagar(ids, metodo) {
      const metodos: Record<string, string> = {
        Saldo: "SALDO",
        Transferencia: "TRANSFERENCIA",
        Tarjeta: "TARJETA",
        "Mercado Pago": "MERCADO_PAGO",
      };
      // Si se reintenta la misma compra, se conserva la referencia para consultar el pago ya iniciado.
      const firma=JSON.stringify([usuario?.id,ids,metodo]);
      if(solicitud.current.firma!==firma)solicitud.current={id:crypto.randomUUID(),firma};
      const pago = await pagoService.pagar({
        productoIds: ids,
        metodo: metodos[metodo],
        solicitudId: solicitud.current.id,
      });
      if (pago.estado !== "PENDIENTE") solicitud.current = {id:crypto.randomUUID(),firma:""};
      await refrescar();
      await recargar();
      if (pago.estado !== "APROBADO" && pago.estado !== "PENDIENTE")
        throw new Error("El pago no se completó. Revisa la disponibilidad de la skin.");
      return pago;
    },
  };
  const cargas = [listaProductos, listaUsuarios, listaCategorias, listaPagos];
  return (
    <Ctx.Provider value={valor}>
      {cargas.map((carga, indice) => carga.error && (
        <aside key={indice} className="alert alert-danger">
          {carga.error} <button onClick={() => void carga.recargar()}>Reintentar</button>
        </aside>
      ))}
      {children}
    </Ctx.Provider>
  );
}
export function useTienda() {
  const c = useContext(Ctx);
  if (!c) throw new Error("Falta TiendaProvider");
  return c;
}
