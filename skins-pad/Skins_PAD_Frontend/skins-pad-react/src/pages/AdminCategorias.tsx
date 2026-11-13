import { useState } from "react";
import { useTienda } from "../context/TiendaContext";
import { solicitar } from "../services/http";
import { useOperacion } from "../lib/useOperacion";
export default function AdminCategorias() {
  const { categorias, recargar } = useTienda(),
    { ejecutar, ocupado } = useOperacion();
  const [id, setId] = useState(""),
    [aviso, setAviso] = useState("");
  const actual = categorias.find((c) => String(c.id) === id);
  return (
    <section className="stat-tile p-4">
      <h2 className="h3 mb-4">Gestión de categorías</h2>
      <label>
        Categoría
        <select
          className="form-select"
          value={id}
          onChange={(e) => {
            setId(e.target.value);
            setAviso("");
          }}
        >
          <option value="">Crear categoría</option>
          {categorias.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}{c.estadoHabilitada ? "" : " (deshabilitada)"}
            </option>
          ))}
        </select>
      </label>
      <form
        className="d-grid gap-3 mt-4"
        key={id}
        onSubmit={(e) => {
          e.preventDefault();
          const datos = Object.fromEntries(new FormData(e.currentTarget));
          void ejecutar(async () => {
            await solicitar(
              "/categorias" + (id ? "/" + id : ""),
              id ? "PUT" : "POST",
              datos,
            );
            await recargar();
            setAviso("Categoría guardada.");
          });
        }}
      >
        <label>
          Nombre
          <input
            className="form-control"
            name="nombre"
            required
            maxLength={80}
            defaultValue={actual?.nombre}
          />
        </label>
        <label>
          Descripción
          <input
            className="form-control"
            name="descripcion"
            maxLength={500}
            defaultValue={actual?.descripcion}
          />
        </label>
        <button disabled={ocupado} className="btn btn-primary">
          Guardar categoría
        </button>
      </form>
      {actual && <div className="d-flex gap-3 border-top pt-3 mt-4">
        <button className="btn btn-outline-warning" disabled={ocupado || !actual.estadoHabilitada} onClick={()=>void ejecutar(async()=>{
          await solicitar("/categorias/"+id+"/deshabilitar","PATCH"); await recargar(); setAviso("Categoría deshabilitada.");
        })}>Deshabilitar</button>
        <button className="btn btn-outline-danger" disabled={ocupado} onClick={()=>{
          if(window.confirm("¿Eliminar la categoría "+actual.nombre+"?"))void ejecutar(async()=>{
            await solicitar("/categorias/"+id,"DELETE");setId("");await recargar();setAviso("Categoría eliminada.");
          });
        }}>Eliminar</button>
      </div>}
      <p role="status">{aviso}</p>
    </section>
  );
}
