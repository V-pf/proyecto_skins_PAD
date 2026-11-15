import { Navigate } from "react-router-dom";
export default function AdminApartado({ apartado }: { apartado: "ventas" | "categorias" | "pagos" }) {
  return <Navigate to={apartado === "ventas" ? "/admin/estadisticas" : apartado === "categorias" ? "/admin/web#categorias" : "/admin/pagos"} replace/>;
}
