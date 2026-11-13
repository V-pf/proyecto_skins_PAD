import { useTienda } from "../context/TiendaContext";
import { formatoCLP } from "../lib/formato";
import { etiquetaTipo } from "../lib/catalogos";
import { useState } from "react";
import type { Pago } from "../lib/api";
export default function Mercado({
  ventas,
  vendedorId,
}: {
  ventas: Pago[];
  vendedorId?: string;
}) {
  const { productos } = useTienda();
  const [mes, setMes] = useState("");
  const aprobados = ventas.filter((p) => p.estado === "APROBADO");
  const meses = [...new Set(aprobados.map((p) => p.fecha.slice(0, 7)))]
    .sort()
    .reverse();
  const filas = aprobados
    .filter((p) => !mes || p.fecha.startsWith(mes))
    .flatMap((p) =>
      (p.productoIds ?? []).flatMap((id) => {
        const skin = productos.find((s) => s.id === id);
        return skin && (!vendedorId || skin.vendedorId === vendedorId)
          ? [
              {
                skin: { ...skin, precio: p.precios?.[id] ?? skin.precio },
                pago: p,
              },
            ]
          : [];
      }),
    )
    .sort((a, b) => b.pago.fecha.localeCompare(a.pago.fecha));
  const tipos = new Map<string, number>();
  const nombres = new Map<string, {nombre:string; unidades:number}>();
  for (const { skin } of filas) {
    const nombre=skin.nombre.normalize("NFC").trim().replace(/\s+/g," ");
    const clave=nombre.toLocaleLowerCase("es");
    const grupo=nombres.get(clave);
    if(grupo) grupo.unidades++;
    else nombres.set(clave,{nombre,unidades:1});
    const tipo = etiquetaTipo(skin.tipo);
    tipos.set(tipo, (tipos.get(tipo) ?? 0) + 1);
  }
  return (
    <section className="mb-5">
      <header className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4"><h2>{vendedorId ? "Mis ventas" : "Estadísticas de ventas"}</h2>
      <label>
        Período{" "}
        <select
          className="form-select mb-3"
          value={mes}
          onChange={(e) => setMes(e.target.value)}
        >
          <option value="">Todo el historial</option>
          {meses.map((m) => (
            <option key={m}>{m}</option>
          ))}
        </select>
      </label></header>
      {filas.length === 0 ? (
        <p>Sin publicaciones con ventas aprobadas en este período.</p>
      ) : (
        <>
          <div className="row g-3 mb-4">
            <div className="col-sm-6"><dl className="stat-tile h-100 mb-0"><dt>Skins vendidas</dt><dd>{filas.length}</dd></dl></div>
            <div className="col-sm-6"><dl className="stat-tile h-100 mb-0"><dt>Ventas sin comisión</dt><dd>{formatoCLP(filas.reduce((s,f)=>s+f.skin.precio,0))}</dd></dl></div>
          </div>
          <div className="row g-3 mb-4"><div className="col-xl-6"><section className="stat-tile h-100 p-4">
            <h3 className="h5 mb-3">Skins más vendidas</h3>
            <ol className="ps-4 mb-0">{[...nombres.values()].sort((a,b)=>b.unidades-a.unidades || a.nombre.localeCompare(b.nombre,"es")).slice(0,5).map(g=>
              <li key={g.nombre} className="py-2"><div className="d-flex justify-content-between gap-3"><span>{g.nombre}</span><strong className="text-nowrap">{g.unidades} {g.unidades===1?"venta":"ventas"}</strong></div></li>)}</ol>
          </section></div><div className="col-xl-6"><section className="stat-tile h-100 p-4">
          <h3 className="h6">Unidades vendidas por tipo</h3>
          {[...tipos]
            .sort((a, b) => b[1] - a[1])
            .map(([tipo, n]) => (
              <div key={tipo}>
                {tipo} · {n}
                <progress
                  className="d-block w-100 mb-2"
                  value={n}
                  max={filas.length}
                />
              </div>
            ))}
          </section></div></div>
          <details className="stat-tile p-4"><summary>Publicaciones vendidas ({filas.length})</summary><div className="table-responsive mt-3">
            <table className="table">
              <caption>Publicaciones vendidas</caption>
              <thead>
                <tr>
                  <th>Fecha de compra</th>
                  <th>Publicación</th>
                  <th>Tipo</th>
                  <th>Vendedor</th>

                  <th>Precio</th>
                </tr>
              </thead>
              <tbody>
                {filas.map(({ skin, pago }) => (
                  <tr key={pago.id + "-" + skin.id}>
                    <td>{pago.fecha.slice(0, 10)}</td>
                    <td>
                      #{skin.id} · {skin.nombre}
                    </td>
                    <td>{etiquetaTipo(skin.tipo)}</td>
                    <td>{skin.vendedor}</td>

                    <td>{formatoCLP(skin.precio)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div></details>
        </>
      )}
    </section>
  );
}
