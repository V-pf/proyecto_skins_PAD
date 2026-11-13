import { useOperacion } from "../lib/useOperacion";
import { useState } from "react";
import type { FormEvent } from "react";
import Campo from "../components/Campo";
import MenuCuenta from "../components/MenuCuenta";
import Migas from "../components/Migas";
import { useTienda } from "../context/TiendaContext";
import { ESTADO_PUBLICACION } from "../lib/catalogos";
import { formatoCLP } from "../lib/formato";
import { useFormulario } from "../lib/useFormulario";
import type { Errores } from "../lib/useFormulario";
import { largo, seleccion } from "../lib/validaciones";

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

export default function AdminPublicaciones() {
  const { ejecutar, ocupado } = useOperacion();
  const { productos, actualizarProducto, cargando } = useTienda();
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

  function aprobar(id: number) {
    void ejecutar(async () => {
      const p = productos.find((x) => x.id === id);
      if (!p) return;
      await actualizarProducto(id, {
        estado: "activa",
        motivoRechazo: undefined,
      });
      if (objetivoId === id) setObjetivoId(null);
      setAviso(`«${p.nombre}» fue aprobada y ya aparece en el catálogo.`);
    });
  }

  function rechazar(e: FormEvent) {
    e.preventDefault();
    void ejecutar(async () => {
      if (!objetivo) return;
      if (!form.validar()) return;
      await actualizarProducto(objetivo.id, {
        estado: objetivo.estado==="activa"?"eliminada":"rechazada",
        motivoRechazo: `${form.valores.motivo}: ${form.valores.detalle.trim()}`,
      });
      setAviso(
        `«${objetivo.nombre}» fue moderada. El motivo se puede consultar en Mis publicaciones.`,
      );
      setObjetivoId(null);
      form.reiniciar();
    });
  }

  if (cargando.productos) return <p className="container py-4">Cargando catálogo…</p>;
  return (
    <div className="container py-4">
      <Migas
        items={[
          { texto: "Inicio", a: "/" },
          { texto: "Administración", a: "/admin/panel" },
          { texto: "Publicaciones" },
        ]}
      />
      <h1 className="mb-4">Publicaciones</h1>
      <div className="row g-4">
        <MenuCuenta tipo="admin" activo="/admin/publicaciones" />
        <div className="col-lg-9">
          {aviso && (
            <div className="alert alert-success" role="status">
              {aviso}
            </div>
          )}
          <label>Estado de publicación<select className="form-select mb-3" value={filtro} onChange={e=>setFiltro(e.target.value)}><option value="">Todos</option>{Object.entries(ESTADO_PUBLICACION).map(([k,v])=><option key={k} value={k}>{v.texto}</option>)}</select></label>
          <div className="table-responsive mb-4">
            <table className="table align-middle">
              <caption>
                {productos.length} publicaciones en total (
                {productos.filter((p) => p.estado === "pendiente").length}{" "}
                pendientes)
              </caption>
              <thead>
                <tr>
                  <th scope="col">Skin</th>
                  <th scope="col">Vendedor</th>
                  <th scope="col">Precio</th>
                  <th scope="col">Estado</th>
                  <th scope="col">
                    <span className="visually-hidden">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {productos.filter(p=>!filtro || p.estado===filtro)
                  .sort(
                    (a, b) =>
                      Number(b.estado === "pendiente") -
                        Number(a.estado === "pendiente") || b.id - a.id,
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
                        <div className="d-flex gap-2">
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-light"
                            disabled={p.estado !== "pendiente"}
                            onClick={() => aprobar(p.id)}
                          >
                            Aprobar
                          </button>
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-light"
                            disabled={p.estado !== "pendiente" && p.estado !== "activa"}
                            onClick={() => {
                              setObjetivoId(p.id);
                              setAviso("");
                            }}
                          >
                            {p.estado==="activa"?"Retirar":"Rechazar"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>

          <form className="stat-tile" onSubmit={rechazar} noValidate>
            <h2 className="h6">Rechazar con motivo</h2>
            {objetivo ? (
              <p className="small text-body-secondary">
                Publicación seleccionada: <strong>{objetivo.nombre}</strong> de{" "}
                {objetivo.vendedor}.
              </p>
            ) : (
              <p className="small text-body-secondary">
                Presiona «Rechazar» en una fila de la tabla para elegir la
                publicación.
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
              disabled={!objetivo}
            >
              Enviar rechazo
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
