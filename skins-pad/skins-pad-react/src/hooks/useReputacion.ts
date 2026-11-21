import { useEffect, useState } from "react";
import { calificacionService, type Reputacion } from "../services/calificacion.service";
export function useReputacion(id: string) {
  const [datos, setDatos] = useState<Reputacion | null>(null);
  const [error, setError] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    setDatos(null);
    setError("");
    calificacionService.consultar(id, controller.signal).then(setDatos).catch(() => {
      if (!controller.signal.aborted) setError("No se pudo cargar la reputación.");
    });
    return () => controller.abort();
  }, [id]);
  return { datos, error };
}
