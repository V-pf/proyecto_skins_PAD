import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { useAuth } from "../context/AuthContext";

interface Props {
  children: ReactNode;
  soloAdmin?: boolean;
}

/** Si no hay sesión manda al login (y recuerda a dónde quería ir). */
export default function RutaProtegida({ children, soloAdmin = false }: Props) {
  const { usuario } = useAuth();
  const ubicacion = useLocation();

  if (!usuario) {
    return <Navigate to="/login" replace state={{ desde: ubicacion.pathname }} />;
  }
  if (soloAdmin && usuario.rol !== "admin") {
    return <Navigate to="/cliente/panel" replace />;
  }
  if (!soloAdmin && usuario.rol === "admin") return <Navigate to="/admin/panel" replace />;
  return <>{children}</>;
}
