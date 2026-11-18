import { Link } from "react-router-dom";
import ListaProductos from "../components/ListaProductos";
import { useTienda } from "../context/TiendaContext";
import { TIPOS, imagenDe } from "../lib/catalogos";
import type { Producto } from "../types";

const IDS_DESTACADOS = [1, 2, 6, 4];

export default function Home() {
  const { productos, usuarios, cargando } = useTienda();
  if (cargando.productos || cargando.usuarios) return <p className="container py-4">Cargando catálogo…</p>;
  const activos = productos.filter((p) => p.estado === "activa");
  const destacados = activos.slice(0,4);
  const recientes = [...activos].sort((a, b) => b.id - a.id).slice(0, 4);

  return (
    <>
      <section className="hero">
        <div className="container">
          <div className="row align-items-center g-5">
            <div className="col-lg-7">
              <h1 className="mb-3">Compra y vende skins de CS:GO con respaldo real</h1>
              <p className="fs-5 text-body-secondary mb-4" style={{ maxWidth: "46ch" }}>
                Miles de vendedores de la Región del Biobío y de todo Chile. Precios en pesos, entrega por intercambio de
                Steam y cada publicación pasa por revisión antes de salir a la venta.
              </p>
              <div className="d-flex flex-wrap gap-2 mb-4">
                <Link to="/catalogo" className="btn btn-primary btn-lg">
                  Explorar catálogo
                </Link>
                <Link to="/cliente/vender" className="btn btn-outline-light btn-lg">
                  Vender una skin
                </Link>
              </div>
              <dl className="hero-precio-tira" aria-label="Estado del mercado, datos de ejemplo">
                <div>
                  <dt>Publicaciones activas</dt>
                  <dd>{activos.length.toLocaleString("es-CL")}</dd>
                </div>
                <div>
                  <dt>Vendedores verificados</dt>
                  <dd>{usuarios.filter(u=>u.rol==="cliente"&&u.verificado).length}</dd>
                </div>
                <div>
                  <dt>Precio máximo del catálogo</dt>
                  <dd>{Math.max(0,...activos.map(p=>p.precio)).toLocaleString("es-CL",{style:"currency",currency:"CLP"})}</dd>
                </div>
                <div>
                  <dt>Tipos disponibles</dt>
                  <dd>{new Set(activos.map(p=>p.tipo)).size}</dd>
                </div>
              </dl>
            </div>
            <div className="col-lg-5">
              <figure className="detalle-figura p-4 m-0 text-center">
                <img src="/img/arma-cuchillo.svg" alt="Ilustración de ejemplo de un cuchillo skin" width={420} height={260} className="img-fluid" />
                <figcaption className="mt-2 text-body-secondary small">★ Karambit | Doppler — publicación destacada de ejemplo</figcaption>
              </figure>
            </div>
          </div>
        </div>
      </section>

      <section className="container py-5" aria-labelledby="tit-categorias">
        <div className="d-flex justify-content-between align-items-end mb-3">
          <h2 id="tit-categorias" className="mb-0">
            Busca por tipo de arma
          </h2>
          <Link to="/categorias">Ver todas las categorías</Link>
        </div>
        <div className="row g-3">
          {TIPOS.filter((t) => t.valor !== "sticker").map((t) => (
            <Link key={t.valor} className="col-6 col-md-3 text-decoration-none" to={`/catalogo?tipo=${t.valor}`}>
              <div className="stat-tile h-100 text-center py-4">
                <img src={imagenDe(t.valor)} alt="" width={90} height={56} className="mb-2" />
                <p className="mb-0 text-body">{t.etiqueta}</p>
              </div>
            </Link>
          ))}
        </div>
      </section>

      <section className="container py-5" aria-labelledby="tit-destacados">
        <div className="d-flex justify-content-between align-items-end mb-3">
          <h2 id="tit-destacados" className="mb-0">
            Destacados de la semana
          </h2>
          <Link to="/catalogo">Ver todo el catálogo</Link>
        </div>
        <ListaProductos productos={destacados} />
      </section>

      <section className="container py-5" aria-labelledby="tit-como">
        <h2 id="tit-como" className="mb-4">
          Cómo funciona
        </h2>
        <ol className="row g-4 list-unstyled">
          <li className="col-md-4">
            <div className="stat-tile h-100">
              <p className="h5">1. Elige tu skin</p>
              <p className="text-body-secondary mb-0">Filtra por arma, grado de rareza, desgaste (float) y precio hasta encontrar la indicada.</p>
            </div>
          </li>
          <li className="col-md-4">
            <div className="stat-tile h-100">
              <p className="h5">2. Paga con confianza</p>
              <p className="text-body-secondary mb-0">El dinero queda retenido hasta que confirmas que recibiste el ítem en tu inventario de Steam.</p>
            </div>
          </li>
          <li className="col-md-4">
            <div className="stat-tile h-100">
              <p className="h5">3. Recibe el intercambio</p>
              <p className="text-body-secondary mb-0">El vendedor te envía una oferta de intercambio de Steam. La aceptas y listo.</p>
            </div>
          </li>
        </ol>
      </section>

      <section className="container py-5" aria-labelledby="tit-novedades">
        <div className="d-flex justify-content-between align-items-end mb-3">
          <h2 id="tit-novedades" className="mb-0">
            Recién publicadas
          </h2>
        </div>
        <ListaProductos productos={recientes} />
      </section>
    </>
  );
}
