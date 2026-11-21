import { useEffect, useState } from "react";
import { inventarioService, type SeriePrecios } from "../services/inventario.service";
export function usePreciosInventario(itemId: number, dias: number) {
  const [datos, setDatos] = useState<SeriePrecios | null>(null);
  const [error, setError] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    setDatos(null); setError("");
    inventarioService.precios(itemId, dias, controller.signal).then(resultado => {
      if (!controller.signal.aborted) setDatos(resultado);
    }).catch(e => { if (!controller.signal.aborted) setError(e instanceof Error ? e.message : "No se pudieron cargar los precios."); });
    return () => controller.abort();
  }, [itemId, dias]);
  return { datos, error };
}
