import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import FormularioResena from "../components/FormularioResena";
import BotonReporte from "../components/soporte/FormularioReporte";
import EstadoOperacionBadge from "../components/operaciones/EstadoOperacionBadge";
import LineaDeTiempoOperacion from "../components/operaciones/LineaDeTiempoOperacion";
import useOperaciones from "../hooks/useOperaciones";
import { formatoCLP } from "../lib/formato";
import { operacionService, type VistaOperacion } from "../services/operacion.service";

function Plazo({ vence }: { vence: string | null }) {
  const [ahora, setAhora] = useState(Date.now());
  useEffect(() => {
    if (!vence) return;
    const temporizador = setInterval(() => setAhora(Date.now()), 1000);
    return () => clearInterval(temporizador);
  }, [vence]);
  if (!vence) return null;
  const segundos = Math.max(0, Math.floor((new Date(vence).getTime() - ahora) / 1000));
  const restante = segundos ? `${Math.floor(segundos / 3600)} h ${Math.floor(segundos % 3600 / 60)} min`
    : "Vencido, procesando resolución";
  return <p>Plazo <time dateTime={vence}>{new Date(vence).toLocaleString("es-CL")}</time> · {restante}</p>;
}

type AccionFila = [accion: string, etiqueta: string, requiereMotivo: boolean];

function accionesDe({ operacion, simulado }: VistaOperacion, admin: boolean, vendedor: boolean): AccionFila[] {
  const { estado, historica, accionPendiente, retenido } = operacion;
  if (historica || accionPendiente) return [];
  if (admin) {
    if (estado !== "EN_DISPUTA" && estado !== "CONGELADA") return [];
    const acciones: AccionFila[] = [];
    if (estado === "EN_DISPUTA") acciones.push(["congelar", "Congelar operación", true]);
    const resolverComprador = retenido ? "Resolver a favor del comprador y reembolsar" : "Resolver a favor del comprador";
    acciones.push(["resolver-comprador", resolverComprador, true]);
    if (retenido || simulado) {
      acciones.push(["resolver-vendedor", simulado ? "Resolver a favor del vendedor" : "Resolver y liberar al vendedor", true]);
    }
    return acciones;
  }
  if (vendedor) {
    if (estado === "PAGO_RETENIDO") {
      return [["aceptar", "Aceptar venta", false], ["rechazar", simulado ? "Rechazar compra" : "Rechazar y reembolsar", true]];
    }
    return estado === "ACEPTADA_POR_VENDEDOR" ? [["entregar", "Marcar como entregada", false]] : [];
  }
  if (estado === "ENTREGADA") {
    return [
      ["recibir", simulado ? "Confirmar recepción" : "Confirmar recepción y liberar fondos", false],
      ["disputar", "Abrir disputa", true],
    ];
  }
  if (estado === "ACEPTADA_POR_VENDEDOR") return [["disputar", "Abrir disputa", true]];
  return estado === "PAGO_PENDIENTE" ? [["cancelar", "Cancelar compra sin pago", false]] : [];
}

function Fila({ v, vista, recargar }: { v: VistaOperacion; vista: string; recargar: () => void }) {
  const o = v.operacion;
  const admin = vista === "excepciones" || vista === "historial";
  const vendedor = vista === "ventas";
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const acciones = accionesDe(v, admin, vendedor);

  async function actuar([accion, etiqueta, requiereMotivo]: AccionFila) {
    if (requiereMotivo && !motivo.trim()) {
      setError("Escribe un motivo antes de continuar.");
      return;
    }
    const confirmacion = o.retenido && (accion === "resolver-comprador" || accion === "rechazar")
      ? "La devolución se abonará al saldo de la billetera del comprador."
      : "La acción quedará registrada en el historial.";
    if (!window.confirm(`¿${etiqueta}? ${confirmacion}`)) return;
    setOcupado(true);
    setError("");
    try {
      await operacionService.actuar(o.id, accion, motivo);
      setMotivo("");
      recargar();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo completar la acción.");
    } finally {
      setOcupado(false);
    }
  }

  return (
    <article className="stat-tile mb-3 p-3" aria-label={`Operación ${o.id}`}>
      <div className="d-flex justify-content-between flex-wrap gap-2">
        <h2 className="h5">{o.nombre} · #{o.id}</h2>
        <EstadoOperacionBadge estado={o.estado} simulado={v.simulado} />
      </div>
      <p>Pago #{o.pagoId} · {v.metodo} · Precio {formatoCLP(o.precio)} · Comisión {formatoCLP(o.comision)}</p>
      {v.simulado && <p>Operación de demostración con importes ficticios.</p>}
      {o.retenido && <p>Fondos en custodia hasta confirmar la entrega.</p>}
      {!v.simulado && o.estado === "COMPLETADA" && <p>Fondos liberados al vendedor.</p>}
      {!v.simulado && o.estado === "REEMBOLSADA" && <p>Devolución abonada al saldo de la billetera.</p>}
      <Plazo vence={o.vence} />
      {!admin && (
        <>
          <BotonReporte tipo="OPERACION" objetivoId={String(o.id)} />
          <p>Si necesitas detener la liberación automática, abre una disputa cuando esté disponible.</p>
        </>
      )}
      {!admin && !vendedor && o.estado === "COMPLETADA" && o.vendedorId && (
        <FormularioResena operacionId={o.id} vendedorId={o.vendedorId} />
      )}
      {o.historica && <p>Operación histórica de consulta. No genera movimientos de dinero.</p>}
      {(o.errorSincronizacion || v.pagoAtascado || o.accionPendiente) && (
        <p role="status" className="alert alert-warning">
          {o.errorSincronizacion || "Operación en recuperación automática. No repitas el pago."}
        </p>
      )}
      {o.estado === "ACEPTADA_POR_VENDEDOR" && vendedor && (
        <p>{v.simulado ? "Marca la entrega para continuar la simulación."
          : "Envía la skin mediante un intercambio en Steam y marca la entrega."}</p>
      )}
      {o.estado === "ENTREGADA" && !admin && !vendedor && !v.simulado && (
        <p>Confirma cuando recibas la skin. Si hay un problema, abre una disputa antes de que venza el plazo de liberación automática.</p>
      )}
      {acciones.some(([, , requiereMotivo]) => requiereMotivo) && (
        <label className="d-block mb-2">
          Motivo o evidencia
          <textarea className="form-control" maxLength={1000} value={motivo} onChange={(e) => setMotivo(e.target.value)} />
        </label>
      )}
      {error && <p className="alert alert-danger" role="alert">{error}</p>}
      <div className="d-flex flex-wrap gap-2">
        {acciones.map((accion) => (
          <button key={accion[0]} className="btn btn-outline-light" disabled={ocupado || v.pagoAtascado}
            onClick={() => void actuar(accion)}>
            {accion[1]}
          </button>
        ))}
      </div>
      <details className="mt-3">
        <summary>Historial y notificaciones ({v.eventos.length})</summary>
        <LineaDeTiempoOperacion eventos={v.eventos} simulado={v.simulado} />
      </details>
    </article>
  );
}

export default function Operaciones({ vista = "compras", integrado = false }: { vista?: string; integrado?: boolean }) {
  const { state } = useLocation();
  const [historial, setHistorial] = useState(false);
  const [estado, setEstado] = useState("");
  const seleccion = vista === "excepciones" && historial ? "historial" : vista;
  const { datos, error, cargando, recargar } = useOperaciones(seleccion);
  const visibles = datos.filter((v) => !estado || v.operacion.estado === estado);
  const novedades = datos.filter(({ operacion }) => !operacion.historica &&
    ["PAGO_RETENIDO", "ENTREGADA", "EN_DISPUTA", "CONGELADA"].includes(operacion.estado)).length;

  return (
    <section className={integrado ? "" : "container py-4"}>
      <h2>{vista === "ventas" ? "Mis ventas" : vista === "excepciones" ? "Operaciones y excepciones" : "Mis compras"}</h2>
      {state?.mensaje && <p className="alert alert-info" role="status">{state.mensaje}</p>}
      <p role="status" aria-live="polite">{novedades} operaciones con novedades o acciones pendientes.</p>
      {!integrado && (
        <nav className="d-flex gap-3 mb-3" aria-label="Operaciones">
          <Link to="/cliente/compras">Mis compras</Link>
          <Link to="/cliente/ventas">Mis ventas</Link>
          <Link to="/cliente/pagos">Mis pagos</Link>
        </nav>
      )}
      {vista === "excepciones" && (
        <label className="d-block mb-2">
          <input type="checkbox" checked={historial} onChange={(e) => setHistorial(e.target.checked)} /> Incluir historial completo
        </label>
      )}
      <div className="d-flex gap-2 mb-3">
        <label>
          Estado
          <select className="form-select" value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            {Array.from(new Set(datos.map((v) => v.operacion.estado))).map((valor) => <option key={valor}>{valor}</option>)}
          </select>
        </label>
        <button className="btn btn-outline-light align-self-end" onClick={recargar} disabled={cargando}>Actualizar</button>
      </div>
      {cargando && <p role="status">Cargando operaciones…</p>}
      {error && <p role="alert" className="alert alert-danger">{error} <button onClick={recargar}>Reintentar</button></p>}
      {!cargando && !error && !visibles.length && <p>No hay operaciones en esta vista.</p>}
      {!error && visibles.map((v) => <Fila key={v.operacion.id} v={v} vista={seleccion} recargar={recargar} />)}
    </section>
  );
}
