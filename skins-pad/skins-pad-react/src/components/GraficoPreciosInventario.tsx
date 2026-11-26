import { useState } from "react";
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip } from "recharts";
import { usePreciosInventario } from "../hooks/usePreciosInventario";
import { formatoCLP } from "../lib/formato";
export default function GraficoPreciosInventario({ itemId }: { itemId: number }) {
  const [dias, setDias] = useState(30);
  const { datos, error } = usePreciosInventario(itemId, dias);
  return <section className="stat-tile my-3" aria-label="Historial de precios de venta">
    <div className="d-flex flex-wrap justify-content-between gap-2">
      <h3 className="h5">Precios de venta</h3>
      <label>Período<select className="form-select" value={dias} onChange={e => setDias(Number(e.target.value))}>
        {[7, 30, 90].map(n => <option value={n} key={n}>Últimos {n} días</option>)}
      </select></label>
    </div>
    {error ? <p role="alert">{error}</p> : !datos ? <p role="status">Cargando precios…</p> : <>
      <p className="small text-body-secondary">Ventas completadas en Skins PAD · Precio medio diario en CLP, sin comisión. Mismo acabado, desgaste y variante.</p>
      {!datos.puntos.length ? <p>No hay ventas de esta variante durante el período seleccionado.</p> : <>
        <div style={{ width: "100%", height: 260 }}>
          <ResponsiveContainer><LineChart data={datos.puntos} margin={{ top: 10, right: 20, left: 15, bottom: 5 }} accessibilityLayer>
            <CartesianGrid strokeDasharray="3 3" stroke="#555" />
            <XAxis dataKey="fecha" tickFormatter={v => String(v).slice(5)} stroke="#aaa" />
            <YAxis tickFormatter={v => formatoCLP(Number(v))} width={90} stroke="#aaa" domain={["auto", "auto"]} />
            <Tooltip formatter={v => [formatoCLP(Number(v)), "Precio medio"]} contentStyle={{ background: "#20252b", borderColor: "#555", color: "#fff" }} />
            <Line type="linear" dataKey="precioMedio" stroke="#4ec9b0" strokeWidth={2} dot={datos.puntos.length < 10} />
          </LineChart></ResponsiveContainer>
        </div>
        <details><summary>Ver precios y cantidad de ventas</summary><div className="table-responsive"><table className="table">
          <thead><tr><th>Fecha</th><th>Precio medio</th><th>Ventas</th></tr></thead>
          <tbody>{datos.puntos.map(p => <tr key={p.fecha}><td>{p.fecha}</td><td>{formatoCLP(p.precioMedio)}</td><td>{p.ventas}</td></tr>)}</tbody>
        </table></div></details>
      </>}
    </>}
  </section>;
}
