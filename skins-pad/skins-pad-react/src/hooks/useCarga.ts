import { useCallback, useEffect, useRef, useState, type SetStateAction } from "react";
import { fallo } from "../lib/api";

/** Cada recurso tiene su propia carga. Las respuestas antiguas se descartan. */
export function useCarga<T>(
  cargar: (signal: AbortSignal) => Promise<T>,
  inicial: T,
  activo = true,
) {
  const [estado, setEstado] = useState({ cargar, datos: inicial, cargando: activo, error: "", resuelto: false });
  const turno = useRef(0);
  const cargarActual = useRef(cargar);
  cargarActual.current = cargar;
  const montado = useRef(true);
  const peticion = useRef<AbortController | null>(null);

  const recargar = useCallback(async () => {
    if (!montado.current || cargarActual.current !== cargar) return;
    const actual = ++turno.current;
    peticion.current?.abort();
    const control = new AbortController();
    peticion.current = control;
    setEstado((prev) => ({
      cargar,
      datos: activo && prev.cargar === cargar ? prev.datos : inicial,
      cargando: activo,
      resuelto: activo && prev.cargar === cargar && prev.resuelto,
      error: "",
    }));
    if (!activo) return;
    try {
      const datos = await cargar(control.signal);
      if (actual === turno.current && !control.signal.aborted)
        setEstado({ cargar, datos, cargando: false, error: "", resuelto: true });
    } catch (e) {
      if (actual === turno.current && !control.signal.aborted)
        setEstado((prev) => ({ ...prev, cargando: false, error: fallo(e), resuelto: true }));
    }
  }, [cargar, inicial, activo]);

  useEffect(() => {
    montado.current = true;
    void recargar();
    return () => {
      montado.current = false;
      turno.current++;
      peticion.current?.abort();
    };
  }, [recargar]);

  const setDatos = useCallback((valor: SetStateAction<T>) => {
    if (!montado.current || cargarActual.current !== cargar) return;
    setEstado((prev) => {
      // Una operación de la cuenta anterior no puede modificar la cuenta actual.
      if (prev.cargar !== cargar) return prev;
      const datos = typeof valor === "function" ? (valor as (prev: T) => T)(prev.datos) : valor;
      return { ...prev, datos };
    });
  }, [cargar]);

  const vigente = estado.cargar === cargar;
  return {
    datos: activo && vigente ? estado.datos : inicial,
    cargando: activo && (!vigente || estado.cargando),
    cargaInicial: activo && (!vigente || (!estado.resuelto && estado.cargando)),
    error: vigente && activo ? estado.error : "",
    recargar,
    setDatos,
  };
}
