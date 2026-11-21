import Imagenes from "./Imagenes";
import { useState } from "react";
import type { Producto } from "../types";
import { useTienda } from "../context/TiendaContext";
import { useOperacion } from "../lib/useOperacion";
export default function EditarSkin({ producto: p }: { producto: Producto }) {
  const [abierto, setAbierto] = useState(false),
    { categorias, actualizarProducto } = useTienda(),
    { ejecutar, ocupado } = useOperacion();
  return abierto ? (
    <>
      {!p.inventarioId && <Imagenes producto={p} />}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          // Recoge solo los campos que se editan aquí y conserva los demás datos de la publicación.
          const d = new FormData(e.currentTarget);
          void ejecutar(async () => {
            await actualizarProducto(p.id, {
              nombre: p.inventarioId ? p.nombre : String(d.get("nombre")),
              descripcion: p.inventarioId ? p.descripcion : String(d.get("descripcion")),
              categoriaId: p.inventarioId ? p.categoriaId : Number(d.get("categoria")),
              precio: Number(d.get("precio")),
            });
            setAbierto(false);
          });
        }}
      >
        {!p.inventarioId && <>
        <label>
          Nombre
          <input
            required
            maxLength={160}
            className="form-control"
            name="nombre"
            defaultValue={p.nombre}
          />
        </label>
        <label>
          Descripción
          <textarea
            maxLength={1500}
            className="form-control"
            name="descripcion"
            defaultValue={p.descripcion}
          />
        </label>
        <label>
          Categoría
          <select
            name="categoria"
            className="form-select"
            defaultValue={p.categoriaId}
          >
            {categorias
              .filter((c) => c.estadoHabilitada)
              .map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                </option>
              ))}
          </select>
        </label>
        </>}
        <label>
          Precio CLP
          <input
            name="precio"
            className="form-control"
            readOnly={p.estado !== "activa"}
            type="number"
            min="1"
            max="50000000"
            required
            defaultValue={p.precio}
          />
        </label>
        <button disabled={ocupado}>Guardar</button>
        <button type="button" onClick={() => setAbierto(false)}>
          Cancelar
        </button>
      </form>
    </>
  ) : (
    <button
      className="btn btn-sm btn-outline-light"
      onClick={() => setAbierto(true)}
    >
      Editar
    </button>
  );
}
