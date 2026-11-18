import { useReputacion } from "../hooks/useReputacion";
export default function ReputacionVendedor({ id, mostrarResenas = false }: { id: string; mostrarResenas?: boolean }) {
  const { datos, error } = useReputacion(id);
  if (error) return <p role="status">{error}</p>;
  if (!datos) return <p role="status">Cargando reputación…</p>;
  return <section aria-label="Reputación del vendedor">
    {datos.trustedSeller && <span className="badge text-bg-success">Trusted seller</span>}
    <p>{datos.total ? `${datos.promedio?.toFixed(2)} / 5 · ${datos.total} reseñas` : "Sin reseñas todavía"}</p>
    {mostrarResenas && datos.resenas.slice().reverse().map(r => <article className="stat-tile mb-3" key={r.id}>
      <p>{r.puntaje} / 5 · {new Date(r.fecha).toLocaleDateString("es-CL")}</p><p>{r.comentario}</p>
    </article>)}
  </section>;
}
