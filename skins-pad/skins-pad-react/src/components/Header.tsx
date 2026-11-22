import AvisosSoporte from "./soporte/AvisosSoporte";
import { useEffect, useState } from "react";

import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCarrito } from "../context/CarritoContext";
import { ENLACES_CUENTA_CLIENTE } from "./MenuCuenta";

export default function Header() {
  const { usuario, salir } = useAuth();
  const { cantidadTotal } = useCarrito();
  const navigate = useNavigate();
  const ubicacion = useLocation();
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [cuentaAbierta, setCuentaAbierta] = useState(false);


  // al cambiar de página se cierran los menús
  useEffect(() => {
    setMenuAbierto(false);
    setCuentaAbierta(false);
  }, [ubicacion.pathname, ubicacion.search]);

  function cerrarSesion() {
    salir();
    navigate("/");
  }

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
            onClick={() => setMenuAbierto((v) => !v)}
          >
            <span className="navbar-toggler-icon"></span>
          </button>
          <div className={`collapse navbar-collapse${menuAbierto ? " show" : ""}`} id="menu-principal">
            {usuario?.rol === "admin" ? <ul className="navbar-nav me-auto"><li className="nav-item"><NavLink className={claseLink} to="/admin/panel">Panel de administrador</NavLink></li><li className="nav-item"><NavLink className={claseLink} to="/admin/soporte">Soporte y reportes</NavLink></li></ul> : <ul className="navbar-nav me-auto">
              <li className="nav-item">
                <NavLink className={claseLink} to="/catalogo">
                  Catálogo
                </NavLink>
              </li>
              <li className="nav-item">
                <NavLink className={claseLink} to="/categorias">
                  Categorías
                </NavLink>
              </li>
              <li className="nav-item">
                <NavLink className={claseLink} to="/cliente/inventario">
                  Vender
                </NavLink>
              </li>
              <li className="nav-item">
                <NavLink className={claseLink} to="/contacto">
                  Contacto y ayuda
                </NavLink>
              </li>
            </ul>}
            <ul className="navbar-nav align-items-lg-center">
              {usuario&&<li className="nav-item"><AvisosSoporte/></li>}
              {usuario?.rol !== "admin" && <li className="nav-item">
                <NavLink className={claseLink} to="/cliente/carrito">
                  Carrito{" "}
                  <span className="badge text-bg-warning" aria-label={`${cantidadTotal} ítems en el carrito`}>
                    {cantidadTotal}
                  </span>
                </NavLink>
              </li>}
              <li className="nav-item dropdown">
                <button
                  type="button"
                  className="nav-link dropdown-toggle btn btn-link"
                  aria-controls="menu-cuenta"
                  aria-expanded={cuentaAbierta}
                  onClick={() => setCuentaAbierta((v) => !v)}
                >
                  {usuario ? usuario.usuario : "Mi cuenta"}
                </button>
                <ul id="menu-cuenta" className={`dropdown-menu dropdown-menu-end${cuentaAbierta ? " show" : ""}`}>
                  {usuario ? (
                    <>
                      {usuario.rol !== "admin" && ENLACES_CUENTA_CLIENTE.map(({ a, texto }) => (
                        <li key={a}>
                          <Link className="dropdown-item" to={a} onClick={() => setCuentaAbierta(false)}>
                            {texto}
                          </Link>
                        </li>
                      ))}
                      {usuario.rol === "admin" && (
                        <li>
                          <Link className="dropdown-item" to="/admin/panel">
                            Panel de administrador
                          </Link>
                        </li>
                      )}
                      <li>
                        <hr className="dropdown-divider" />
                      </li>
                      <li>
                        <button type="button" className="dropdown-item" onClick={cerrarSesion}>
                          Cerrar sesión
                        </button>
                      </li>
                    </>
                  ) : (
                    <>
                      <li>
                        <Link className="dropdown-item" to="/login">
                          Ingresar
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item" to="/registro">
                          Crear cuenta
                        </Link>
                      </li>
                    </>
                  )}
                </ul>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    </header>
  );
}
