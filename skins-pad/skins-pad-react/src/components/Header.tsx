import { useEffect, useRef, useState } from "react";

import { Link, NavLink, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCarrito } from "../context/CarritoContext";

import CerrarSesion from "./CerrarSesion";

const secciones = [
  ["/cliente/panel", "Resumen"],
  ["/cliente/compras", "Mis compras"],
  ["/cliente/ventas", "Mis Ventas"],
  ["/cliente/publicaciones", "Mis publicaciones"],
  ["/cliente/pagos", "Pagos"],
  ["/cliente/billetera", "Billetera y retiros"],
  ["/cliente/perfil", "Editar Perfil"],
];
const acceso = [["/login", "Ingresar"], ["/registro", "Crear cuenta"]];

export default function Header() {
  const { usuario } = useAuth();
  const { cantidadTotal } = useCarrito();
  const ubicacion = useLocation();
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [cuentaAbierta, setCuentaAbierta] = useState(false);


  const cuenta = useRef<HTMLLIElement>(null);
  const botonCuenta = useRef<HTMLButtonElement>(null);
  function cerrarMenus() {
    setCuentaAbierta(false);
    setMenuAbierto(false);
  }
  useEffect(() => {
    if (!cuentaAbierta) return;
    function cerrarFuera(e: PointerEvent) {
      if (e.target instanceof Node && !cuenta.current?.contains(e.target)) setCuentaAbierta(false);
    }
    document.addEventListener("pointerdown", cerrarFuera);
    return () => document.removeEventListener("pointerdown", cerrarFuera);
  }, [cuentaAbierta]);

  // al cambiar de página se cierran los menús
  useEffect(() => {
    setMenuAbierto(false);
    setCuentaAbierta(false);
  }, [ubicacion.pathname, ubicacion.search]);

  const claseLink = ({ isActive }: { isActive: boolean }) => `nav-link${isActive ? " active" : ""}`;

  return (
    <header className="sitio-header sticky-top">
      <a className="visually-hidden-focusable" href="#contenido">
        Saltar al contenido
      </a>
      <nav className="navbar navbar-expand-lg" aria-label="Menú principal">
        <div className="container">
          <Link className="navbar-brand d-flex align-items-center gap-2" to={usuario?.rol === "admin" ? "/admin/panel" : "/"}>
            <img src="/img/logo.svg" alt="" width={32} height={32} />
            Skins PAD
          </Link>
          <button
            className="navbar-toggler"
            type="button"
            aria-controls="menu-principal"
            aria-expanded={menuAbierto}
            aria-label="Abrir menú"
            onClick={() => { setMenuAbierto(v => !v); setCuentaAbierta(false); }}
          >
            <span className="navbar-toggler-icon"></span>
          </button>
          <div className={`collapse navbar-collapse${menuAbierto ? " show" : ""}`} id="menu-principal">
            {usuario?.rol === "admin" ? <ul className="navbar-nav me-auto"><li className="nav-item"><NavLink className={claseLink} to="/admin/panel">Panel de administrador</NavLink></li></ul> : <ul className="navbar-nav me-auto">
              <li className="nav-item">
                <NavLink className={claseLink} to="/catalogo">
                  Catálogo
                </NavLink>
              </li>
              <li className="nav-item">
                <NavLink className={claseLink} to="/cliente/vender">
                  Vender una skin
                </NavLink>
              </li>
              <li className="nav-item">
                <NavLink className={claseLink} to="/contacto">
                  Contacto y ayuda
                </NavLink>
              </li>
            </ul>}
            <ul className="navbar-nav align-items-lg-center">
              {usuario?.rol !== "admin" && <li className="nav-item">
                <NavLink className={claseLink} to="/cliente/carrito">
                  Carrito{" "}
                  <span className="badge text-bg-warning" aria-label={`${cantidadTotal} ítems en el carrito`}>
                    {cantidadTotal}
                  </span>
                </NavLink>
              </li>}
              <li ref={cuenta} className="nav-item dropdown"
                onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget)) setCuentaAbierta(false); }}
                onKeyDown={e => {
                  if (e.key === "Escape" && cuentaAbierta) {
                    setCuentaAbierta(false);
                    botonCuenta.current?.focus();
                  }
                }}>
                <button ref={botonCuenta} type="button" className="nav-link dropdown-toggle btn btn-link"
                  aria-controls="opciones-cuenta" aria-expanded={cuentaAbierta}
                  onClick={() => setCuentaAbierta(v => !v)}>{usuario?.usuario ?? "Mi cuenta"}</button>
                <ul id="opciones-cuenta" className={`dropdown-menu dropdown-menu-end${cuentaAbierta ? " show" : ""}`}>
                  {usuario?.rol !== "admin" && (usuario ? secciones : acceso).map(([ruta, texto]) => (
                    <li key={ruta}><Link className="dropdown-item" to={ruta} onClick={cerrarMenus}>{texto}</Link></li>
                  ))}
                  {usuario && <li><CerrarSesion className="dropdown-item" alCerrar={cerrarMenus} /></li>}
                </ul>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    </header>
  );
}
