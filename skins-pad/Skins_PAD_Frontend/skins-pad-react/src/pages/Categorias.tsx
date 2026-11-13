import {useTienda} from "../context/TiendaContext";
import { Link } from "react-router-dom";
import Migas from "../components/Migas";
import { GRADOS, TIPOS, imagenDe } from "../lib/catalogos";

export default function Categorias() {
  const {categorias,productos,cargando}=useTienda();
  if (cargando.categorias) return <p className="container py-4">Cargando categorías…</p>;
  return (
    <div className="container py-4">
      <Migas items={[{ texto: "Inicio", a: "/" }, { texto: "Categorías" }]} />
      <h1 className="mb-4">Categorías</h1>

      <section aria-labelledby="tit-tipos" className="mb-5">
        <h2 id="tit-tipos" className="mb-3">
          Por tipo de arma
        </h2>
        <div className="row g-4">
          {categorias.filter(c=>c.estadoHabilitada).map(c => (
            <article key={c.id} className="col-md-6 col-lg-4">
              <Link className="text-decoration-none" to={`/catalogo?categoria=${c.id}`}>
                <div className="stat-tile h-100">
                  <img src={imagenDe(productos.find(p=>p.categoriaId===c.id)?.tipo??"rifle")} alt="" width={140} height={90} className="mb-3" />
                  <h3 className="h5 text-body">{c.nombre}</h3>
                  <p className="text-body-secondary small mb-0">{c.descripcion}</p>
                </div>
              </Link>
            </article>
          ))}
        </div>
      </section>

      <section aria-labelledby="tit-grados">
        <h2 id="tit-grados" className="mb-3">
          Por grado de rareza
        </h2>
        <div className="row g-3">
          {GRADOS.map((g) => (
            <article key={g.valor} className="col-md-6 col-lg-3">
              <Link className="text-decoration-none" to={`/catalogo?grado=${g.valor}`}>
                <div className={`stat-tile producto grado-${g.valor} h-100`}>
                  <h3 className="h6 text-body">{g.etiqueta}</h3>
                  <p className="text-body-secondary small mb-0">{g.descripcion}</p>
                </div>
              </Link>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
