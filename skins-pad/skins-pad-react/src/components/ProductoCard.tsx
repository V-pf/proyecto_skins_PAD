import {useOperacion} from "../lib/useOperacion";
import { Link } from "react-router-dom";
import { useCarrito } from "../context/CarritoContext";
import { useToast } from "../context/ToastContext";
import { imagenDe, infoGrado, subtituloProducto } from "../lib/catalogos";
import { formatoCLP } from "../lib/formato";
import type { Producto } from "../types";

export default function ProductoCard({ producto: p }: { producto: Producto }) {
  const {ejecutar}=useOperacion();
  const { agregar } = useCarrito();
  const { mostrar } = useToast();

  return (
    <article className={`card producto grado-${p.grado} h-100`}>
      <figure className="producto-figura m-0">
        <img src={p.imagenes?.[0]?.urlImagen || imagenDe(p.tipo)} alt={`Ilustración de ejemplo de ${p.nombre}`} width={320} height={200} loading="lazy" />
        <figcaption>
          <span className="badge-grado">{infoGrado(p.grado).insignia}</span>
        </figcaption>
      </figure>
      <div className="card-body d-flex flex-column">
        {p.demo && <span className="badge text-bg-warning mb-2">Ítem de prueba</span>}
        <h3 className="h6 card-title mb-1">
          <Link to={`/producto/${p.id}`}>{p.nombre}</Link>{" "}
          {p.stattrak && (
            <span className="badge text-bg-warning" title="StatTrak™">
              ST™
            </span>
          )}
        </h3>
        <p className="small text-body-secondary mb-2">{subtituloProducto(p)}</p>
        <p className="precio fs-4 mt-auto mb-2">{formatoCLP(p.precio)}</p>
        <div className="d-flex gap-2">
          <Link className="btn btn-sm btn-outline-light flex-fill" to={`/producto/${p.id}`}>
            Ver detalle
          </Link>
          <button
            type="button"
            className="btn btn-sm btn-primary flex-fill"
            onClick={() => void ejecutar(async()=>{
              await agregar(p.id);
              mostrar(`${p.nombre} se agregó al carrito.`);
            })}
          >
            Agregar
          </button>
        </div>
      </div>
    </article>
  );
}
