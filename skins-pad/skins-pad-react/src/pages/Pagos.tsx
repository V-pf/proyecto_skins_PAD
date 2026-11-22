import { Link, useLocation, useSearchParams } from "react-router-dom";
import Operaciones from "./Operaciones";
import type { Pago } from "../lib/api";
import { useTienda } from "../context/TiendaContext";
import { useAuth } from "../context/AuthContext";
import { formatoCLP } from "../lib/formato";

function Fila({ pago }: { pago: Pago }) {
  return (
    <article className="stat-tile mb-3">
      <h3 className="h6">
        Pago #{pago.id} {pago.simulado && <span className="badge text-bg-secondary">Simulado</span>}
      </h3>
      <p>
        {pago.fecha.slice(0, 10)} · {formatoCLP(pago.montoTotal)} · {pago.metodo} · {pago.estado}
        {(pago.cuotas ?? 1) > 1 && ` · ${pago.cuotas} cuotas`}
      </p>
      {pago.numeroOperacion && <p>Transferencia n.º {pago.numeroOperacion}</p>}
      {pago.estado === "RECHAZADO" && <p>{pago.referenciaExterna || "El pago fue rechazado."}</p>}
    </article>
  );
}

export default function Pagos({ gestion = false, mostrarTitulo = true }: { gestion?: boolean; mostrarTitulo?: boolean }) {
  const { pagos, recargar, cargando } = useTienda();
  const { usuario } = useAuth();
  const { state } = useLocation();
  const [params, setParams] = useSearchParams();
  const filtroEstado = params.get("estado") ?? "";
  const administrar = gestion && usuario?.rol === "admin";
  const visibles = administrar
    ? pagos.filter((p) => !filtroEstado || p.estado === filtroEstado)
        .sort((a, b) => b.fecha.localeCompare(a.fecha))
    : pagos.filter((p) => p.usuarioId === usuario?.id);

  if (cargando.pagos) return <p className="container py-4">Cargando pagos…</p>;
  return (
    <section className={administrar ? "" : "container py-4"}>
      {mostrarTitulo && <h2>{administrar ? "Gestión de pagos" : "Mis pagos"}</h2>}
      {administrar
        ? <Operaciones vista="excepciones" integrado />
        : <p><Link to="/cliente/compras">Ver entregas y operaciones en Mis compras</Link></p>}
      {administrar && (
        <label className="d-block mb-3">
          Estado
          <select className="form-select" value={filtroEstado} onChange={(e) => {
            const next = new URLSearchParams(params);
            if (e.target.value) next.set("estado", e.target.value);
            else next.delete("estado");
            setParams(next);
          }}>
            <option value="">Todos</option>
            {["PENDIENTE", "APROBADO", "RECHAZADO", "REEMBOLSADO"].map((estado) => (
              <option key={estado}>{estado}</option>
            ))}
          </select>
        </label>
      )}
      {state?.mensaje && <p role="status">{state.mensaje}</p>}
      <button className="btn btn-outline-light mb-3" onClick={() => void recargar()}>Actualizar</button>
      {!visibles.length && <p>No hay pagos registrados.</p>}
      <details open={!administrar}>
        <summary>Historial de pagos</summary>
        {visibles.map((pago) => <Fila key={pago.id} pago={pago} />)}
      </details>
    </section>
  );
}
