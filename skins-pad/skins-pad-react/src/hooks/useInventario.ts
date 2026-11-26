import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { inventarioService, type Inventario } from "../services/inventario.service";
export function useInventario() {
  const { usuario } = useAuth();
  const [datos, setDatos] = useState<Inventario | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const recargar = useCallback(() => setRevision(n => n + 1), []);
  useEffect(() => {
    const controller = new AbortController();
    setDatos(null); setCargando(true); setError("");
    async function cargar() {
      try {
        const inventario = await inventarioService.consultar(controller.signal);
        if (!controller.signal.aborted) setDatos(inventario);
      } catch (e) { if (!controller.signal.aborted) setError(e instanceof Error ? e.message : "No se pudo cargar el inventario."); }
      finally { if (!controller.signal.aborted) setCargando(false); }
    }
    void cargar();
    return () => controller.abort();
  }, [usuario?.id, revision]);
  return { datos, cargando, error, recargar };
}
