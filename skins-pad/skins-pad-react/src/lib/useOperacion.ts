import { useRef, useState } from "react";
import { useToast } from "../context/ToastContext";
import { fallo } from "./api";
export function useOperacion() {
  const lock = useRef(false),
    [ocupado, setOcupado] = useState(false);
  const { mostrar } = useToast();
  // Bloquea clics repetidos mientras se guarda un cambio y muestra el error si falla.
  async function ejecutar(fn: () => Promise<void>) {
    if (lock.current) return;
    lock.current = true;
    setOcupado(true);
    try {
      await fn();
    } catch (e) {
      mostrar(fallo(e), "danger");
    } finally {
      lock.current = false;
      setOcupado(false);
    }
  }
  return { ejecutar, ocupado };
}
