import VolverPanel from "../components/VolverPanel";
import { useEffect, useRef, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { solicitar } from "../services/http";
import { formatoCLP, fechaCorta } from "../lib/formato";
import { useOperacion } from "../lib/useOperacion";
type Retiro = { id: number; monto: number; fecha: string };
export default function Billetera() {
  const { usuario, refrescar } = useAuth();
  const { ejecutar, ocupado } = useOperacion();
  const [monto, setMonto] = useState("");
  const [historial, setHistorial] = useState<Retiro[]>([]);
  const [aviso, setAviso] = useState("");
  const referencia = useRef(crypto.randomUUID());
  const ruta = "/usuarios/" + usuario?.id + "/retiros";
  // Al entrar a la billetera se consulta el historial de retiros de la cuenta.
  useEffect(() => {
    let activo = true;
    void refrescar();
    solicitar<Retiro[]>(ruta)
      .then((r) => {
        if (activo) setHistorial(r);
      })
      .catch((e) => {
        if (activo) setAviso(e.message);
      });
    return () => {
      activo = false;
    };
  }, [ruta, refrescar]);
  if (!usuario) return null;
  return (
    <main className="container py-4">
      <VolverPanel />
      <h1>Billetera y retiros</h1>
      <div className="row g-4">
        <section className="col-12">
          <h2 className="h5">Saldo disponible</h2>
          <p className="display-6">{formatoCLP(usuario.saldo)}</p>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void ejecutar(async () => {
                // El retiro usa una referencia que se conserva si hay que volver a enviar la solicitud.
                await solicitar(ruta, "POST", {
                  monto: Number(monto),
                  referencia: referencia.current,
                });
                referencia.current = crypto.randomUUID();
                setMonto("");
                setAviso(
                  "Retiro solicitado. El monto quedó descontado del saldo disponible.",
                );
                await refrescar();
                setHistorial(await solicitar<Retiro[]>(ruta));
              });
            }}
          >
            <label>
              Monto del retiro (CLP)
              <input
                required
                className="form-control"
                type="number"
                min="1"
                max={usuario.saldo}
                value={monto}
                onChange={(e) => setMonto(e.target.value)}
              />
            </label>
            <button
              disabled={ocupado || usuario.saldo < 1}
              className="btn btn-primary ms-2"
            >
              Solicitar retiro
            </button>
          </form>
          <p role="status">{aviso}</p>
          <h2 className="h5 mt-4">Retiros solicitados</h2>
          {historial.length === 0 ? (
            <p>Sin solicitudes registradas.</p>
          ) : (
            <ul>
              {historial.map((r) => (
                <li key={r.id}>
                  Solicitud #{r.id} · {fechaCorta(r.fecha.slice(0, 10))} ·{" "}
                  {formatoCLP(r.monto)}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </main>
  );
}
