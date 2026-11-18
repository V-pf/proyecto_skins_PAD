import VolverPanel from "../components/VolverPanel";
import EditarSkin from "./EditarSkin";
import { Link } from "react-router-dom";
import Migas from "../components/Migas";
import { useAuth } from "../context/AuthContext";
import { useTienda } from "../context/TiendaContext";
import { ESTADO_PUBLICACION, etiquetaTipo } from "../lib/catalogos";
import { formatoCLP } from "../lib/formato";

export default function MisPublicaciones() {
  const { usuario } = useAuth();
  const { productos, actualizarProducto, cargando } = useTienda();
  if (!usuario) return null;
  if (cargando.productos) return <p className="container py-4">Cargando catálogo…</p>;

  // Reúne las publicaciones del usuario para mostrar su estado y las acciones disponibles.
  const mias = productos.filter((p) => p.vendedor === usuario.usuario);

  return (
    <div className="container py-4">
      <Migas items={[{ texto: "Inicio", a: "/" }, { texto: "Mi cuenta", a: "/cliente/panel" }, { texto: "Mis publicaciones" }]} />
      <VolverPanel />
      <h1 className="mb-4">Mis publicaciones</h1>
      <div className="row g-4">
        <div className="col-12">
          {mias.length === 0 ? (
            <p className="text-body-secondary">
              Todavía no has publicado skins. <Link to="/cliente/vender">Vende tu primera skin</Link>.
            </p>
          ) : (
            <div className="table-responsive">
              <table className="table align-middle">
                <caption>
                  {mias.length} publicación{mias.length === 1 ? "" : "es"}
                </caption>
                <thead>
                  <tr>
                    <th scope="col">Skin</th>
                    <th scope="col">Tipo</th>
                    <th scope="col">Precio</th>
                    <th scope="col">Estado</th>
                    <th scope="col">
                      <span className="visually-hidden">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {mias.map((p) => (
                    <tr key={p.id}>
                      <td>{p.estado === "activa" ? <Link to={`/producto/${p.id}`}>{p.nombre}</Link> : p.nombre}</td>
                      <td>{etiquetaTipo(p.tipo)}</td>
                      <td>{formatoCLP(p.precio)}</td>
                      <td>
                        <span className={`badge ${ESTADO_PUBLICACION[p.estado].clase}`}>{ESTADO_PUBLICACION[p.estado].texto}</span>
                        {p.motivoRechazo && <div className="small text-body-secondary">{p.motivoRechazo}</div>}
                      </td>
                      <td>
                        {(p.estado === "activa" || p.estado === "pendiente") && <EditarSkin producto={p}/>}
                        {(p.estado === "pendiente" || p.estado === "activa") && (
                          <button type="button" className="btn btn-sm btn-outline-light" onClick={() => window.confirm("¿Retirar esta publicación?") && actualizarProducto(p.id, { estado: "eliminada", motivoRechazo: "Retirada por el vendedor" }).catch(e=>window.alert(e.message))}>
                            Retirar
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
