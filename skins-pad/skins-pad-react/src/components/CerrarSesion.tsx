import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function CerrarSesion({ className = "btn btn-sm btn-outline-secondary", alCerrar }: { className?: string; alCerrar?: () => void }) {
  const { salir } = useAuth();
  const navigate = useNavigate();
  async function cerrar() {
    alCerrar?.();
    await salir();
    navigate("/", { replace: true });
  }
  return <button type="button" className={className} onClick={cerrar}>Cerrar sesión</button>;
}
