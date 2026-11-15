import { useEffect } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import Header from "./Header";
import Footer from "./Footer";

export default function Layout() {
  const { usuario } = useAuth();
  const { pathname, hash } = useLocation();

  // al cambiar de página volvemos arriba (o bajamos al #ancla si la hay)
  useEffect(() => {
    if (hash) {
      document.getElementById(hash.slice(1))?.scrollIntoView();
    } else {
      window.scrollTo(0, 0);
    }
  }, [pathname, hash]);

  if (usuario?.rol === "admin" && !pathname.startsWith("/admin/")) return <Navigate to="/admin/panel" replace />;
  return (
    <>
      <Header />
      <main id="contenido">
        <Outlet />
      </main>
      {usuario?.rol !== "admin" && <Footer />}
    </>
  );
}
