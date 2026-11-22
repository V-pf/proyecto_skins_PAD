import { Link } from "react-router-dom";

interface Enlace {
  a?: string;
  texto: string;
}

export const ENLACES_CUENTA_CLIENTE = [
  {a:"/cliente/soporte",texto:"Soporte"},
  {a:"/cliente/soporte/reportes",texto:"Mis reportes"},
  { a: "/cliente/compras", texto: "Mis compras" },
  { a: "/cliente/ventas", texto: "Mis ventas" },
  { a: "/cliente/pagos", texto: "Mis pagos" },
  { a: "/cliente/carrito", texto: "Carrito" },
  { a: "/cliente/inventario", texto: "Mi inventario / Vender" },
  { a: "/cliente/publicaciones", texto: "Mis publicaciones" },
  { a: "/cliente/billetera", texto: "Billetera y retiros" },
  { a: "/cliente/perfil", texto: "Editar perfil" },
];

const CLIENTE: Enlace[] = [
  { a: "/cliente/panel", texto: "Resumen" },
  ...ENLACES_CUENTA_CLIENTE,
];

const ADMIN: Enlace[] = [
  {a:"/admin/soporte",texto:"Soporte y reportes"},
  { a: "/admin/estadisticas", texto: "Estadísticas" },
  { a: "/admin/pagos", texto: "Gestión de Pagos" },
  { a: "/admin/web", texto: "Gestión web" },
];

export default function MenuCuenta({ tipo, activo }: { tipo: "cliente" | "admin"; activo: string }) {
  const enlaces = tipo === "cliente" ? CLIENTE : ADMIN;
  const titulo = tipo === "cliente" ? "Mi cuenta" : "Administración";
  return (
    <aside className="col-lg-3">
      <h2 className="h6 text-body-secondary">{titulo}</h2>
      <nav aria-label={titulo}>
        <div className="list-group">
          {enlaces.map((e) =>
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
