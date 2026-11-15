import { useEffect, useRef } from "react";
import { NavLink } from "react-router-dom";
import { useTienda } from "../context/TiendaContext";
import AdminEstadisticas from "./AdminEstadisticas";
import AdminCategorias from "./AdminCategorias";
import AdminPublicaciones from "./AdminPublicaciones";
import AdminUsuarios from "./AdminUsuarios";
import Pagos from "./Pagos";

export default function AdminPanel({ seccion = "estadisticas" }: { seccion?: "estadisticas" | "pagos" | "web" }) {
  const { recargar } = useTienda();
  const actualizar = useRef(recargar);
  actualizar.current = recargar;
  useEffect(() => {
    if (seccion === "estadisticas") return;
    let enCurso = false;
    const refrescar = async () => {
      if (enCurso || document.visibilityState === "hidden") return;
      enCurso = true;
      try { await actualizar.current(); } finally { enCurso = false; }
    };
    void refrescar();
    const timer = setInterval(() => void refrescar(), 30000);
    window.addEventListener("focus", refrescar);
    document.addEventListener("visibilitychange", refrescar);
    return () => {
      clearInterval(timer);
      window.removeEventListener("focus", refrescar);
      document.removeEventListener("visibilitychange", refrescar);
    };
  }, [seccion]);

  const titulo = seccion === "estadisticas" ? "Estadísticas" : seccion === "pagos" ? "Gestor de Pagos" : "Gestión web";
  return <div className="container py-4 admin-panel">
    <nav className="nav nav-tabs mb-4" aria-label="Menú de administración">
      <NavLink className="nav-link" to="/admin/estadisticas">Estadísticas</NavLink>
      <NavLink className="nav-link" to="/admin/pagos">Gestión de Pagos</NavLink>
      <NavLink className="nav-link" to="/admin/web">Gestión web</NavLink>
    </nav>
    <h1 className="mb-4">{titulo}</h1>
    {seccion === "estadisticas" && <AdminEstadisticas/>}
    {seccion === "pagos" && <section className="admin-modulo" aria-label="Gestor de Pagos"><Pagos gestion mostrarTitulo={false}/></section>}
    {seccion === "web" && <section className="admin-modulo" aria-label="Gestión web">
      <section id="usuarios" className="admin-submodulo" aria-label="Gestión de usuarios"><AdminUsuarios integrado/></section>
      <section id="publicaciones" className="admin-submodulo" aria-label="Gestión de publicaciones"><AdminPublicaciones integrado/></section>
      <section id="categorias" className="admin-submodulo" aria-label="Gestión de categorías"><AdminCategorias integrado/></section>
    </section>}
  </div>;
}
