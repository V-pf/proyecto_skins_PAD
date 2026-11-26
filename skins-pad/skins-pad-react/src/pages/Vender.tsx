import { useState } from "react";
import { Link } from "react-router-dom";
import GraficoPreciosInventario from "../components/GraficoPreciosInventario";
import { useInventario } from "../hooks/useInventario";
import { inventarioService } from "../services/inventario.service";
import { useAuth } from "../context/AuthContext";
import { useTienda } from "../context/TiendaContext";
import { DESGASTES, imagenDe } from "../lib/catalogos";
export default function Vender() {
  const { usuario } = useAuth();
  const { recargar: recargarTienda } = useTienda();
  const { datos, cargando, error, recargar } = useInventario();
  const [seleccion, setSeleccion] = useState<number | null>(null);
  const [precio, setPrecio] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const [aviso, setAviso] = useState("");
  const [errorAccion, setErrorAccion] = useState("");
  const elegido = datos?.items.find(v => v.item.id === seleccion);
  return <main className="container py-4">
    <h1 className="mb-4">Mi inventario CS2</h1>
    <p className="alert alert-secondary">
      Con una integración de Steam, vincularías tu cuenta para importar tus skins de CS2 y verlas aquí.
      Para esta demostración usamos inventarios precargados.
    </p>
    {error && <p role="alert" className="alert alert-danger">{error} <button onClick={recargar}>Reintentar</button></p>}
    {errorAccion && <p role="alert" className="alert alert-danger">{errorAccion}</p>}
    {aviso && <p role="status" className="alert alert-success">{aviso} <Link to="/cliente/publicaciones">Ver mis publicaciones</Link></p>}
    {cargando && <p role="status">Cargando inventario…</p>}
    {datos && <>
        {!usuario?.verificado && <p className="alert alert-info">Tu cuenta debe ser verificada por administración para publicar.</p>}
        {!datos.items.length && <p>Tu cuenta no tiene skins en el inventario.</p>}
        <div className="row row-cols-1 row-cols-sm-2 row-cols-xl-3 g-3">
          {datos.items.map(({ item, publicable, estadoPublicacion }) => <div className="col" key={item.id}>
            <article className={"card h-100 p-3 " + (seleccion === item.id ? "border-primary" : "")}>
              {(item.imagen || item.tipo) && <img src={item.imagen || imagenDe(item.tipo!)} alt={item.nombre} height={140} style={{ objectFit: "contain" }} />}
              <h2 className="h6 mt-2">{item.nombre}</h2>
              <p className="small">
                {item.desgaste && DESGASTES[item.desgaste]?.nombre}
              </p>
              <p className="small text-body-secondary">
                {estadoPublicacion === "RESERVADO" ? "Reservado"
                  : !item.presente ? "Ya no está en tu inventario"
                  : !item.intercambiable ? "Ítem no habilitado para la venta"
                  : !item.tipo && !item.productoOrigenId ? "Tipo de ítem no admitido para venta"
                  : estadoPublicacion && !publicable ? "Publicación " + estadoPublicacion.toLowerCase().replace(/_/g, " ")
                  : "Disponible para publicar"}
              </p>
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
              setAviso("La skin ya está publicada en el catálogo."); setSeleccion(null); setPrecio(""); recargar();
              await recargarTienda();
            } catch (e) { setErrorAccion(e instanceof Error ? e.message : "No se pudo publicar."); }
            finally { setOcupado(false); }
          }}>
            <label className="d-block">Precio de venta (CLP)<input className="form-control" type="number" min="1" max="50000000" step="1" required value={precio} onChange={e => setPrecio(e.target.value)} /></label>
            <p className="small mt-2">El comprador paga un 5% adicional de comisión. La skin aparecerá de inmediato en el catálogo.</p>
            <button className="btn btn-primary" disabled={ocupado || cargando || !usuario?.verificado}>{ocupado ? "Publicando…" : "Publicar skin"}</button>
          </form>}
        </section>}
    </>}
  </main>;
}
