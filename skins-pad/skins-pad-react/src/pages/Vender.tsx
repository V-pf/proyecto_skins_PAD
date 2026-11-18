import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import MenuCuenta from "../components/MenuCuenta";
import GraficoPreciosInventario from "../components/GraficoPreciosInventario";
import { useInventario } from "../hooks/useInventario";
import { inventarioService } from "../services/inventario.service";
import { useAuth } from "../context/AuthContext";
import { useTienda } from "../context/TiendaContext";
import { DESGASTES, imagenDe } from "../lib/catalogos";
export default function Vender() {
  const { usuario } = useAuth();
  const { recargar: recargarTienda } = useTienda();
  const { datos, cargando, error, recargar, sincronizar } = useInventario();
  const [seleccion, setSeleccion] = useState<number | null>(null);
  const [precio, setPrecio] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const [aviso, setAviso] = useState("");
  const [errorAccion, setErrorAccion] = useState("");
  const [params] = useSearchParams();
  const elegido = datos?.items.find(v => v.item.id === seleccion);
  async function vincular() {
    setOcupado(true); setErrorAccion("");
    try { const { url } = await inventarioService.vincular(); window.location.assign(url); }
    catch (e) { setErrorAccion(e instanceof Error ? e.message : "No se pudo iniciar la vinculación."); setOcupado(false); }
  }
  return <main className="container py-4">
    <h1>Mi inventario CS2</h1>
    <div className="row g-4">
      <MenuCuenta tipo="cliente" activo="/cliente/inventario" />
      <div className="col-lg-9">
        {params.get("steam") === "vinculado" && <p className="alert alert-success">Steam vinculado. Tu inventario se importó correctamente.</p>}
        {params.get("steam") === "sincronizacion-pendiente" && <p className="alert alert-warning">Steam vinculado. Haz público tu inventario y pulsa Sincronizar para completar la importación.</p>}
        {params.get("steam") === "error" && <p className="alert alert-danger">No se pudo verificar la vinculación con Steam. Inténtalo de nuevo con un perfil que no esté vinculado a otra cuenta.</p>}
        {error && <p role="alert" className="alert alert-danger">{error} <button onClick={recargar}>Reintentar</button></p>}
        {errorAccion && <p role="alert" className="alert alert-danger">{errorAccion}</p>}
        {aviso && <p role="status" className="alert alert-success">{aviso} <Link to="/cliente/publicaciones">Ver mis publicaciones</Link></p>}
        {cargando && <p role="status">Cargando inventario…</p>}
        {datos && <>
          {datos.demo && <p className="alert alert-warning">Inventario de prueba. Los ítems y el historial simulado permiten practicar el flujo de publicación.</p>}
          {datos.steamId ? <p>Perfil vinculado · <a href={"https://steamcommunity.com/profiles/" + datos.steamId} target="_blank" rel="noreferrer">Ver en Steam</a></p> : <>
            <p>Vincula tu cuenta de Steam para importar automáticamente tu inventario de CS2. Tu inventario debe ser público.</p>
            <button className="btn btn-primary mb-3" disabled={ocupado || cargando} onClick={() => void vincular()}>Vincular con Steam</button>
          </>}
          {(datos.steamId || datos.demo) && <>
            <div className="d-flex align-items-center gap-3 mb-3">
              <button className="btn btn-outline-light" disabled={ocupado || cargando} onClick={() => void sincronizar()}>Sincronizar inventario</button>
              {datos.sincronizado && <small>Actualizado {new Date(datos.sincronizado).toLocaleString("es-CL")}</small>}
            </div>
            {!usuario?.verificado && <p className="alert alert-info">Tu cuenta debe ser verificada por administración para publicar.</p>}
            {!cargando && !datos.items.length && <p>Tu inventario de CS2 está vacío.</p>}
            <div className="row row-cols-1 row-cols-sm-2 row-cols-xl-3 g-3">
              {datos.items.map(({ item, publicable, estadoPublicacion }) => <div className="col" key={item.id}>
                <article className={"card h-100 p-3 " + (seleccion === item.id ? "border-primary" : "")}>
                  {(item.imagen || item.tipo) && <img src={item.imagen || imagenDe(item.tipo!)} alt={item.nombre} height={140} style={{ objectFit: "contain" }} />}
                  <h2 className="h6 mt-2">{item.nombre}</h2>
                  <p className="small">{DESGASTES[item.desgaste].nombre}</p>
                  <p className="small text-body-secondary">{!item.presente ? "Ya no está en tu inventario" : !item.intercambiable ? "Intercambio bloqueado en Steam" : !item.tipo ? "Tipo de ítem no admitido para venta" : estadoPublicacion && !publicable ? "Publicación " + estadoPublicacion.toLowerCase().replace(/_/g, " ") : "Disponible para publicar"}</p>
                  <button className="btn btn-outline-light mt-auto" disabled={ocupado || cargando} onClick={() => { setSeleccion(item.id); setPrecio(""); setAviso(""); }}>Seleccionar</button>
                </article>
              </div>)}
            </div>
            {elegido && <section className="mt-4" key={elegido.item.id}>
              <h2 className="h4">{elegido.item.nombre}</h2>
              <GraficoPreciosInventario itemId={elegido.item.id} />
              {elegido.publicable && <form onSubmit={async e => {
                e.preventDefault(); setOcupado(true); setErrorAccion(""); setAviso("");
                try {
                  await inventarioService.publicar(elegido.item.id, Number(precio));
                  setAviso("Publicación enviada a revisión."); setSeleccion(null); setPrecio(""); recargar();
                  await recargarTienda();
                } catch (e) { setErrorAccion(e instanceof Error ? e.message : "No se pudo publicar."); }
                finally { setOcupado(false); }
              }}>
                <label className="d-block">Precio de venta (CLP)<input className="form-control" type="number" min="1" max="50000000" step="1" required value={precio} onChange={e => setPrecio(e.target.value)} /></label>
                <p className="small mt-2">El comprador paga un 5% adicional de comisión. La publicación será revisada por administración.</p>
                <button className="btn btn-primary" disabled={ocupado || cargando || !usuario?.verificado}>{ocupado ? "Publicando…" : "Publicar skin"}</button>
              </form>}
            </section>}
          </>}
        </>}
      </div>
    </div>
  </main>;
}
