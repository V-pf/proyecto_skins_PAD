import { formatoCLP } from "../lib/formato";
import {useEffect,useRef} from "react";
import { Link } from "react-router-dom";
import MenuCuenta from "../components/MenuCuenta";
import Migas from "../components/Migas";
import { useTienda } from "../context/TiendaContext";

export default function AdminPanel() {
  const { usuarios, productos, cargando, recargar } = useTienda();

  const actualizar=useRef(recargar);
  actualizar.current=recargar;
  useEffect(()=>{const id=setInterval(()=>void actualizar.current(),60000);return ()=>clearInterval(id)},[]);
  if (cargando.usuarios || cargando.productos) return <p className="container py-4">Cargando administración…</p>;
  const pendientes = productos.filter((p) => p.estado === "pendiente");

  return (
    <div className="container py-4">
      <Migas items={[{ texto: "Inicio", a: "/" }, { texto: "Administración" }]} />
      <h1 className="mb-1">Panel de administración</h1>
      <p className="text-body-secondary mb-4">Resumen en tiempo real de lo que pasa en la plataforma.</p>
      <div className="row g-3 mb-5">
        <div className="col-6 col-lg-4">
          <dl className="stat-tile mb-0">
            <dt>Usuarios registrados</dt>
            <dd>{usuarios.length}</dd>
          </dl>
        </div>
        <div className="col-6 col-lg-4">
          <dl className="stat-tile mb-0">
            <dt>Publicaciones activas</dt>
            <dd>{productos.filter((p) => p.estado === "activa").length}</dd>
          </dl>
        </div>
        <div className="col-6 col-lg-4">
          <dl className="stat-tile mb-0">
            <dt>Por revisar</dt>
            <dd>{pendientes.length}</dd>
          </dl>
        </div>

      </div>
      <div className="row g-4">
        <MenuCuenta tipo="admin" activo="/admin/panel" />
        <div className="col-lg-9">


          <section aria-labelledby="tit-cola" className="mb-5">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <h2 id="tit-cola" className="h5 mb-0">
                Publicaciones esperando revisión
              </h2>
              <Link to="/admin/publicaciones">Ver todas</Link>
            </div>
            {pendientes.length === 0 ? (
              <p className="text-body-secondary">No hay publicaciones pendientes. ¡Todo al día!</p>
            ) : (
              <table className="table align-middle">
                <caption className="visually-hidden">Publicaciones pendientes de revisión</caption>
                <thead>
                  <tr>
                    <th scope="col">Skin</th>
                    <th scope="col">Vendedor</th>
                    <th scope="col">Precio</th>
                  </tr>
                </thead>
                <tbody>
                  {pendientes.map((p) => (
                    <tr key={p.id}>
                      <td>{p.nombre}</td>
                      <td>{p.vendedor}</td>
                      <td>{formatoCLP(p.precio)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>

        </div>
      </div>
    </div>
  );
}
