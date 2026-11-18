import { useState } from "react";
import { calificacionService } from "../services/calificacion.service";
import { useReputacion } from "../hooks/useReputacion";
export default function FormularioResena({ operacionId, vendedorId }: { operacionId: number; vendedorId: string }) {
  const { datos, error } = useReputacion(vendedorId);
  const [puntaje, setPuntaje] = useState("5");
  const [comentario, setComentario] = useState("");
  const [enviada, setEnviada] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [aviso, setAviso] = useState("");
  if (enviada || datos?.resenas.some(r => r.operacionId === operacionId)) return <p role="status">Ya calificaste esta compra.</p>;
  if (error || !datos) return <p role="status">{error || "Cargando reseña…"}</p>;
  return <form className="mt-3" onSubmit={async e => {
    e.preventDefault(); setOcupado(true); setAviso("");
    try { await calificacionService.crear(operacionId, Number(puntaje), comentario); setEnviada(true); }
    catch (error) { setAviso(error instanceof Error ? error.message : "No se pudo guardar la reseña."); }
    finally { setOcupado(false); }
  }}>
    <h3 className="h6">Califica al vendedor</h3>
    <label className="d-block">Puntaje (1 a 5)<input className="form-control" type="number" min="1" max="5" step="0.01" required value={puntaje} onChange={e => setPuntaje(e.target.value)} /></label>
    <label className="d-block">Reseña<textarea className="form-control" required maxLength={1000} value={comentario} onChange={e => setComentario(e.target.value)} /></label>
    {aviso && <p role="alert">{aviso}</p>}
    <button className="btn btn-primary mt-2" disabled={ocupado}>{ocupado ? "Enviando…" : "Publicar reseña"}</button>
  </form>;
}
