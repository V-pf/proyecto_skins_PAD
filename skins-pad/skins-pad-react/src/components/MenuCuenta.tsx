import { Link } from "react-router-dom";

interface Enlace {
  a?: string;
  texto: string;
}

const ADMIN: Enlace[] = [
  { a: "/admin/estadisticas", texto: "Estadísticas" },
  { a: "/admin/pagos", texto: "Gestión de Pagos" },
  { a: "/admin/web", texto: "Gestión web" },
];

export default function MenuCuenta({ activo }: { activo: string }) {
  const titulo = "Administración";
  return (
    <aside className="col-lg-3">
      <h2 className="h6 text-body-secondary">{titulo}</h2>
      <nav aria-label={titulo}>
        <div className="list-group">
          {ADMIN.map((e) =>
            e.a ? (
              <Link
                key={e.texto}
                to={e.a}
                className={`list-group-item list-group-item-action${e.a === activo ? " active" : ""}`}
                aria-current={e.a === activo ? "page" : undefined}
              >
                {e.texto}
              </Link>
            ) : (
              <span key={e.texto} className="list-group-item disabled" aria-disabled="true">
                {e.texto} <small>(próximamente)</small>
              </span>
            )
          )}
        </div>
      </nav>
    </aside>
  );
}
