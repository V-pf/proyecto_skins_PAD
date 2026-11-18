import { useTienda } from "../context/TiendaContext";
import { formatoCLP } from "../lib/formato";
import { etiquetaTipo } from "../lib/catalogos";
import { useState } from "react";
import type { Pago } from "../lib/api";
export default function Mercado({
  ventas,
  vendedorId,
  mostrarTitulo = true,
}: {
  ventas: Pago[];
  vendedorId?: string;
  mostrarTitulo?: boolean;
}) {
  const { productos, usuarios, categorias } = useTienda();
  const [mes, setMes] = useState("");
  const aprobados = ventas.filter((p) => p.estado === "APROBADO");
  const meses = [...new Set(aprobados.map((p) => p.fecha.slice(0, 7)))]
    .sort()
    .reverse();
  // Arma una fila por skin vendida, usando el precio guardado en el pago cuando está disponible.
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
  // Cuenta las unidades por tipo y agrupa los nombres aunque cambien las mayúsculas o los espacios.
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
  const clientes = usuarios.filter(u => u.rol === "cliente");
  const vendedores = clientes.filter(u => filas.some(f => f.skin.vendedorId === u.id || (!f.skin.vendedorId && f.skin.vendedor === u.usuario)));
  const porcentaje = clientes.length ? vendedores.length / clientes.length * 100 : 0;
  const total = filas.reduce((s, f) => s + f.skin.precio, 0);
  const pedidos = new Set(filas.map(f => f.pago.id)).size;
  const porCategoria = new Map<string, number>();
  const porMes = new Map<string, number>();
  for (const { skin, pago } of filas) {
    const categoria = categorias.find(c => c.id === skin.categoriaId)?.nombre ?? "Sin categoría";
    porCategoria.set(categoria, (porCategoria.get(categoria) ?? 0) + 1);
    const periodo = pago.fecha.slice(0, 7);
    porMes.set(periodo, (porMes.get(periodo) ?? 0) + skin.precio);
  }
  const colores = ["#60a5fa", "#fbbf24", "#34d399", "#f472b6", "#a78bfa", "#fb923c", "#22d3ee"];
  let acumulado = 0;
  const segmentos = [...porCategoria].sort((a,b) => b[1] - a[1]).map(([nombre, cantidad], i) => {
    const inicio = acumulado;
    acumulado += cantidad / filas.length * 100;
    return { nombre, cantidad, porcentaje: cantidad / filas.length * 100, color: colores[i % colores.length], inicio, fin: acumulado };
  });
  const gradiente = segmentos.map(s => s.color + " " + s.inicio + "% " + s.fin + "%").join(", ");
  return (
    <section className="mb-5">
      <header className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">{mostrarTitulo && <h2>{vendedorId ? "Mis ventas" : "Estadísticas"}</h2>}
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
      {!vendedorId && <>
        <div className="row g-3 mb-4">
          {[
            ["Clientes registrados", clientes.length],
            ["Clientes con ventas aprobadas", porcentaje.toLocaleString("es-CL", { maximumFractionDigits: 1 }) + "%"],
            ["Operaciones aprobadas", pedidos],
            ["Venta promedio por operación", formatoCLP(pedidos ? total / pedidos : 0)],
          ].map(([titulo, valor]) => <div className="col-sm-6 col-xl-3" key={titulo}><dl className="stat-tile h-100 mb-0"><dt>{titulo}</dt><dd>{valor}</dd></dl></div>)}
        </div>
        <p className="small text-body-secondary">Clientes: cuentas con rol cliente, sin administradores. Vendedores: {vendedores.length} clientes con al menos una venta aprobada en el período seleccionado. Importes sin comisión; se excluyen pagos pendientes, rechazados y reembolsados.</p>
        <div className="row g-3 mb-4">
          <div className="col-xl-6"><section className="stat-tile h-100"><h3 className="h5">Porcentaje de ventas por categoría</h3>
            {segmentos.length ? <><div className="admin-pie mx-auto my-4" role="img" aria-label={segmentos.map(s => s.nombre + ": " + s.porcentaje.toFixed(1) + "%").join("; ")} style={{ background: "conic-gradient(" + gradiente + ")" }}/>
              <ul className="list-unstyled mb-0">{segmentos.map(s => <li key={s.nombre} className="d-flex gap-2 align-items-center mb-2"><span className="admin-legend" style={{background: s.color}} aria-hidden="true"/><span>{s.nombre}: {s.porcentaje.toFixed(1)}% ({s.cantidad})</span></li>)}</ul>
              <p className="small text-body-secondary mt-3">Distribución por cantidad de skins vendidas.</p></> : <p>Sin ventas aprobadas para graficar.</p>}
          </section></div>
          <div className="col-xl-6"><section className="stat-tile h-100"><h3 className="h5">Ventas mensuales sin comisión</h3>
            {[...porMes].sort((a,b) => a[0].localeCompare(b[0])).map(([periodo, monto]) => <div key={periodo} className="mb-3"><div className="d-flex justify-content-between gap-2"><span>{periodo}</span><strong>{formatoCLP(monto)}</strong></div><progress aria-label={"Ventas de " + periodo} className="w-100" value={monto} max={Math.max(...porMes.values(), 1)}/></div>)}
            {!porMes.size && <p>Sin ventas aprobadas para graficar.</p>}
          </section></div>
        </div>
      </>}
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
