import { Link } from "react-router-dom";

export interface Miga {
  texto: string;
  a?: string;
}

export default function Migas({ items }: { items: Miga[] }) {
  return (
    <nav aria-label="Migas de pan" className="mb-3">
      <ol className="breadcrumb">
        {items.map((m, i) =>
          i === items.length - 1 || !m.a ? (
            <li key={i} className="breadcrumb-item active" aria-current="page">
              {m.texto}
            </li>
          ) : (
            <li key={i} className="breadcrumb-item">
              <Link to={m.a}>{m.texto}</Link>
            </li>
          )
        )}
      </ol>
    </nav>
  );
}
