import { useOperacion } from "../lib/useOperacion";
import { useState } from "react";
import type { FormEvent } from "react";
import Modal from "../components/Modal";
import Campo from "../components/Campo";
import MenuCuenta from "../components/MenuCuenta";
import Migas from "../components/Migas";
import { useTienda } from "../context/TiendaContext";
import { ESTADO_PUBLICACION } from "../lib/catalogos";
import { formatoCLP } from "../lib/formato";
import { useFormulario } from "../lib/useFormulario";
import type { Errores } from "../lib/useFormulario";
import { largo, seleccion } from "../lib/validaciones";
import type { Producto } from "../types";

interface RechazoForm {
  [clave: string]: string | boolean;
  motivo: string;
  detalle: string;
}

const MOTIVOS = [
  { valor: "Precio fuera de mercado", etiqueta: "Precio fuera de mercado" },
  {
    valor: "Datos de la skin incompletos o incorrectos",
    etiqueta: "Datos de la skin incompletos o incorrectos",
  },
  {
    valor: "Imagen no corresponde al ítem",
    etiqueta: "Imagen no corresponde al ítem",
  },
  { valor: "Otro", etiqueta: "Otro" },
];

function requiereRevision(p: Producto) {
  return p.estado === "activa" && p.revisionApta == null;
}

export default function AdminPublicaciones({ integrado = false }: { integrado?: boolean }) {
  const Titulo = integrado ? "h3" : "h2";
  const { ejecutar, ocupado } = useOperacion();
  const { productos, actualizarProducto, cargando, recargar } = useTienda();
  const [objetivoId, setObjetivoId] = useState<number | null>(null);
  const [filtro,setFiltro]=useState("");
  const [aviso, setAviso] = useState("");

  const form = useFormulario<RechazoForm>(
    "revision",
    { motivo: "", detalle: "" },
    (v): Errores<RechazoForm> => ({
      motivo: seleccion(v.motivo),
      detalle: largo(v.detalle, 10, 300),
    }),
  );

  const objetivo = productos.find((p) => p.id === objetivoId) ?? null;
  const sinRevisar = productos.filter(requiereRevision).length;

  function marcarApta(p: Producto) {
    void ejecutar(async () => {
      await actualizarProducto(p.id, { estado: "activa" });
      setAviso(p.estado === "activa"
        ? `«${p.nombre}» quedó marcada como apta.`
        : `«${p.nombre}» quedó marcada como apta. El vendedor puede volver a publicarla desde su inventario.`);
    });
  }

  function marcarNoApta(e: FormEvent) {
    e.preventDefault();
    void ejecutar(async () => {
      if (!objetivo) return;
      if (!form.validar()) return;
      await actualizarProducto(objetivo.id, {
        estado: "rechazada",
        motivoRechazo: `${form.valores.motivo}: ${form.valores.detalle.trim()}`,
      });
      setAviso(
        `«${objetivo.nombre}» se retiró del catálogo. El vendedor puede consultar el motivo en Mis publicaciones.`,
      );
      setObjetivoId(null);
      form.reiniciar();
    });
  }

  if (cargando.productos) return <p className="container py-4">Cargando catálogo…</p>;
  return (
    <div className={integrado ? "" : "container py-4"}>
      {!integrado && <Migas
        items={[
          { texto: "Inicio", a: "/" },
          { texto: "Administración", a: "/admin/panel" },
          { texto: "Publicaciones" },
        ]}
      />}
      <Titulo className="h4 mb-4">Publicaciones</Titulo>
      <div className="row g-4">
        {!integrado && <MenuCuenta activo="/admin/publicaciones" />}
        <div className={integrado ? "col-12" : "col-lg-9"}>
          {aviso && (
            <div className="alert alert-success" role="status">
              {aviso}
            </div>
          )}
          <div className="d-flex flex-wrap align-items-center gap-3 mb-3">
            <p role="status" className="mb-0">{sinRevisar} publicaciones activas por revisar.</p>
            <button type="button" className="btn btn-outline-light" onClick={() => void ejecutar(recargar)} disabled={ocupado}>Actualizar publicaciones</button>
          </div>
          <label>Estado de publicación<select className="form-select mb-3" value={filtro} onChange={e=>setFiltro(e.target.value)}><option value="">Todos</option>{Object.entries(ESTADO_PUBLICACION).map(([k,v])=><option key={k} value={k}>{v.texto}</option>)}</select></label>
          <div className="table-responsive mb-4">
            <table className="table align-middle">
              <caption>{productos.length} publicaciones en total</caption>
              <thead>
                <tr>
                  <th scope="col">Skin</th>
                  <th scope="col">Vendedor</th>
                  <th scope="col">Precio</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Moderación</th>
                  <th scope="col">
                    <span className="visually-hidden">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {productos.filter(p=>!filtro || p.estado===filtro)
                  .sort(
                    (a, b) =>
                      Number(requiereRevision(b)) - Number(requiereRevision(a)) || b.id - a.id,
                  )
                  .map((p) => (
                    <tr
                      key={p.id}
                      className={
                        p.id === objetivoId ? "fila-seleccionada" : undefined
                      }
                    >
                      <td>{p.nombre}</td>
                      <td>{p.vendedor}</td>
                      <td>{formatoCLP(p.precio)}</td>
                      <td>
                        <span
                          className={`badge ${ESTADO_PUBLICACION[p.estado].clase}`}
                        >
                          {ESTADO_PUBLICACION[p.estado].texto}
                        </span>
                      </td>
                      <td>
                        {p.revisionApta === true ? "Apta" : p.revisionApta === false ? "No apta"
                          : requiereRevision(p) ? "Por revisar" : "—"}
                      </td>
                      <td>
                        <div className="d-flex gap-2">
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-light"
                            disabled={ocupado || !(requiereRevision(p) ||
                              (p.revisionApta === false && (p.estado === "rechazada" || p.estado === "eliminada")))}
                            onClick={() => marcarApta(p)}
                          >
                            Marcar apta
                          </button>
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-light"
                            disabled={ocupado || p.estado !== "activa"}
                            onClick={() => {
                              form.reiniciar();
                              setObjetivoId(p.id);
                              setAviso("");
                            }}
                          >
                            Marcar no apta
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>

          <Modal abierto={objetivo !== null} titulo="Retirar publicación" onCerrar={() => setObjetivoId(null)}>
          <form className="stat-tile" onSubmit={marcarNoApta} noValidate>
            {objetivo && (
              <p className="small text-body-secondary">
                Se retirará <strong>{objetivo.nombre}</strong> de {objetivo.vendedor}.
              </p>
            )}
            <Campo
              form={form}
              nombre="motivo"
              tipo="select"
              etiqueta="Motivo"
              opciones={MOTIVOS}
              textoOpcionVacia="Selecciona un motivo"
            />
            <Campo
              form={form}
              nombre="detalle"
              tipo="textarea"
              maxLength={300}
              etiqueta="Detalle para el vendedor"
              ayuda={
                <>
                  Mínimo 10 caracteres.
                  <span className="float-end">
                    {form.valores.detalle.length}/300
                  </span>
                </>
              }
            />
            <button
              type="submit"
              className="btn btn-outline-light"
              disabled={!objetivo || ocupado}
            >
              {ocupado ? "Guardando…" : "Confirmar retiro"}
            </button>
          </form>
          </Modal>
        </div>
      </div>
    </div>
  );
}
