import { useState } from "react";
import { useLocation } from "react-router-dom";
import type { Pago } from "../lib/api";
import { pagoService } from "../services/pago.service";
import { useTienda } from "../context/TiendaContext";
import { useAuth } from "../context/AuthContext";
import { useOperacion } from "../lib/useOperacion";
import { formatoCLP } from "../lib/formato";
function Fila({ p, admin }: { p: Pago; admin: boolean }) {
  const { ejecutar, ocupado } = useOperacion(),
    { recargar } = useTienda();
  const [banco, setBanco] = useState(""),
    [numero, setNumero] = useState(""),
    [url, setUrl] = useState(""),
    [aviso, setAviso] = useState("");
  const accion = (operacion: () => Promise<void>) =>
    ejecutar(async () => {
      await operacion();
      setAviso("Operación registrada.");
      await recargar();
    });
  return (
    <article className="stat-tile mb-3">
      <h3 className="h6">Pago #{p.id}</h3>
      <p>
        {p.fecha.slice(0, 10)} · {formatoCLP(p.montoTotal)} · {p.metodo} ·{" "}
        {p.estado}{(p.cuotas??1)>1 && ` · ${p.cuotas} cuotas`}
      </p>
      {p.numeroOperacion && <p>Transferencia Nº {p.numeroOperacion}</p>}
      {p.estado === "RECHAZADO" && <p role="status">{p.referenciaExterna || "El pago fue rechazado."}</p>}
      {aviso && <p role="status">{aviso}</p>}
      {p.estado === "PENDIENTE" && p.metodo === "TRANSFERENCIA" && !admin && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void accion(() => pagoService.registrarComprobante(p.id, {
              banco,
              numeroOperacion: numero,
              archivoUrl: url,
            }));
          }}
        >
          <label>
            Banco
            <input
              className="form-control"
              required
              value={banco}
              onChange={(e) => setBanco(e.target.value)}
            />
          </label>
          <label>
            Número de operación
            <input
              className="form-control"
              required
              value={numero}
              onChange={(e) => setNumero(e.target.value)}
            />
          </label>
          <label>
            Enlace al comprobante
            <input
              className="form-control"
              type="url"
              required
              value={url}
              onChange={(e) => setUrl(e.target.value)}
            />
          </label>
          <button disabled={ocupado} className="btn btn-primary">
            Registrar comprobante
          </button>
        </form>
      )}
      {admin && p.estado === "PENDIENTE" && (
        <div className="d-flex gap-2">
          {p.metodo === "TRANSFERENCIA" && (
            <button
              disabled={ocupado}
              onClick={() =>
                void ejecutar(async () => {
                  const c = await pagoService.obtenerComprobante(p.id);
                  if (
                    window.confirm(
                      c.banco +
                        " · " +
                        c.numeroOperacion +
                        " · " +
                        c.archivoUrl +
                        "\n¿Verificaste este comprobante?",
                    )
                  ) {
                    await pagoService.verificarComprobante(p.id);
                    setAviso("Comprobante verificado.");
                  }
                })
              }
            >
              Revisar comprobante
            </button>
          )}
          <button disabled={ocupado} onClick={() => void accion(() => pagoService.confirmar(p.id))}>
            Confirmar pago recibido
          </button>
          <button
            disabled={ocupado}
            onClick={() =>
              void accion(() => pagoService.rechazar(p.id, {
                motivo: "Rechazado por administración",
              }))
            }
          >
            Rechazar
          </button>
        </div>
      )}
      {admin && p.estado==="APROBADO" && p.liquidado===false && <button disabled={ocupado} onClick={()=>void accion(()=>pagoService.confirmar(p.id))}>Completar acreditación de la venta</button>}
      {admin && p.estado === "APROBADO" && (
        <button
          disabled={ocupado}
          onClick={() => {
            if (
              window.confirm(
                "¿Registrar el reembolso? Para pagos externos, gestiona primero la devolución con el proveedor.",
              )
            )
              void accion(() => pagoService.reembolsar(p.id));
          }}
        >
          Registrar reembolso
        </button>
      )}
    </article>
  );
}
export default function Pagos({ gestion = false }: { gestion?: boolean }) {
  const { pagos, recargar, cargando } = useTienda(),
    { usuario } = useAuth(),
    { state } = useLocation();
  const administrar = gestion && usuario?.rol === "admin";
  const visibles = administrar ? pagos : pagos.filter(p => p.usuarioId === usuario?.id);
  if (cargando.pagos) return <p className="container py-4">Cargando pagos…</p>;
  return (
    <section className="container py-4">
      <h2>{administrar ? "Gestión de pagos" : "Mis pagos"}</h2>
      {state?.mensaje && <p role="status">{state.mensaje}</p>}
      <button
        className="btn btn-outline-light mb-3"
        onClick={() => void recargar()}
      >
        Actualizar
      </button>
      {!visibles.length && <p>No hay pagos registrados.</p>}
      {visibles.map((p) => (
        <Fila key={p.id} p={p} admin={administrar} />
      ))}
    </section>
  );
}
