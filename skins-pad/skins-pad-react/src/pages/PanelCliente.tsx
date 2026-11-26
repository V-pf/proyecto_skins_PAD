import {useEffect,useState} from "react";
import Mercado from "./Mercado";
import {solicitar} from "../services/http";
import type {Pago} from "../lib/api";
import { Link, useLocation } from "react-router-dom";
import ListaProductos from "../components/ListaProductos";
import Migas from "../components/Migas";
import { useAuth } from "../context/AuthContext";
import { useCarrito } from "../context/CarritoContext";
import { useTienda } from "../context/TiendaContext";
import { fechaCorta, formatoCLP } from "../lib/formato";


export default function PanelCliente() {
  const { usuario } = useAuth();
  const { pagos, productos, cargando } = useTienda();
  const { cantidadTotal, items, cargando: cargandoCarrito } = useCarrito();
  const [ventas,setVentas]=useState<Pago[]>([]);
  const [errorVentas,setErrorVentas]=useState("");
  // Recarga las ventas propias cuando cambia la cuenta o se actualizan los pagos.
  useEffect(()=>{let activo=true;solicitar<Pago[]>("/pagos/mis-ventas").then(v=>{if(activo)setVentas(v)}).catch(e=>{if(activo)setErrorVentas(e.message)});return ()=>{activo=false}},[usuario?.id,pagos]);
  const ubicacion = useLocation();
  const mensaje = (ubicacion.state as { mensaje?: string } | null)?.mensaje;

  if (!usuario) return null; // RutaProtegida ya redirige; esto es solo para TypeScript

  if (cargando.pagos || cargando.productos || cargandoCarrito) return <p className="container py-4">Cargando cuenta…</p>;
  // Separa los pagos de la cuenta para calcular el resumen de compras.
  const misPagos = pagos.filter((p) => p.usuarioId === usuario.id);
  const compras = misPagos.filter((p) => p.estado === "APROBADO").length;
  const pendientes = misPagos.filter((p) => p.estado === "PENDIENTE").length;
  const enCarrito = new Set(items.map((i) => i.productoId));
  const recomendados = productos
    // Sugiere publicaciones activas de otros vendedores que todavía no están en el carrito.
    .filter((p) => p.estado === "activa" && !enCarrito.has(p.id) && p.vendedor !== usuario.usuario)
    .slice(0, 3);

  return (
    <div className="container py-4">
      <Migas items={[{ texto: "Inicio", a: "/" }, { texto: "Mi cuenta" }]} />
      {mensaje && (
        <div className="alert alert-success" role="status">
          {mensaje}
        </div>
      )}
      <div className="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-4">
        <div>
          <h1 className="mb-1">Hola, {usuario.usuario}</h1>
          <p className="text-body-secondary mb-0">Aquí ves el resumen de tu actividad en Skins PAD.</p>
        </div>
        <span className={`badge align-self-center ${usuario.verificado ? "text-bg-success" : "text-bg-secondary"}`}>
          {usuario.verificado ? "Cuenta verificada" : "Cuenta sin verificar"}
        </span>
      </div>
      <div className="row g-3 mb-5">
        <div className="col-6 col-lg-3">
          <dl className="stat-tile mb-0">
            <dt>Compras realizadas</dt>
            <dd>{compras}</dd>
          </dl>
        </div>
        <div className="col-6 col-lg-3">
          <dl className="stat-tile mb-0">
            <dt>Pagos pendientes</dt>
            <dd>{pendientes}</dd>
          </dl>
        </div>
        <div className="col-6 col-lg-3">
          <dl className="stat-tile mb-0">
            <dt>En el carrito</dt>
            <dd>{cantidadTotal}</dd>
          </dl>
        </div>
        <div className="col-6 col-lg-3">
          <dl className="stat-tile mb-0">
            <dt>Saldo en billetera</dt>
            <dd>{formatoCLP(usuario.saldo)}</dd>
          </dl>
        </div>
      </div>
      {productos.filter(p=>p.vendedorId===usuario.id && p.motivoRechazo && (p.estado==="eliminada" || p.estado==="rechazada")).map(p=><p key={p.id} className="alert alert-warning">«{p.nombre}» · {p.estado}. Motivo: {p.motivoRechazo}</p>)}
      {errorVentas?<p role="alert">{errorVentas}</p>:<Mercado ventas={ventas} vendedorId={usuario.id}/>}
      <section aria-labelledby="tit-actividad" className="mb-5">
        <h2 id="tit-actividad" className="h5">
          Actividad reciente
        </h2>
        {misPagos.length === 0 ? (
          <p className="text-body-secondary">
            Todavía no tienes compras. <Link to="/catalogo">Explora el catálogo</Link>.
          </p>
        ) : (
          <table className="table align-middle">
            <caption className="visually-hidden">Actividad reciente de la cuenta</caption>
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Actividad</th>
                <th scope="col">Monto</th>
                <th scope="col">Estado</th>
              </tr>
            </thead>
            <tbody>
              {misPagos.slice(0, 3).map((p) => (
                <tr key={p.id}>
                  <td>
                    <time dateTime={p.fecha}>{fechaCorta(p.fecha.slice(0,10))}</time>
                  </td>
                  <td>Compra: {p.metodo}</td>
                  <td>{formatoCLP(p.montoTotal)}</td>
                  <td>
                    <span className={`badge ${"text-bg-secondary"}`}>{p.estado}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
      {recomendados.length > 0 && (
        <section aria-labelledby="tit-recomendado">
          <h2 id="tit-recomendado" className="h5">
            Basado en tus compras
          </h2>
          <ListaProductos productos={recomendados} columnas="row-cols-1 row-cols-sm-3" />
        </section>
      )}
    </div>
  );
}
