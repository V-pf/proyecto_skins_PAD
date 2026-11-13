import { createContext, useCallback, useContext, useRef, useState } from "react";
import type { ReactNode } from "react";

interface Toast {
  mostrar: (mensaje: string, tipo?: "success" | "danger" | "info") => void;
}

const Ctx = createContext<Toast | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [aviso, setAviso] = useState<{ mensaje: string; tipo: string } | null>(null);
  const temporizador = useRef<number | undefined>(undefined);

  const mostrar = useCallback((mensaje: string, tipo: "success" | "danger" | "info" = "success") => {
    setAviso({ mensaje, tipo });
    window.clearTimeout(temporizador.current);
    temporizador.current = window.setTimeout(() => setAviso(null), 3500);
  }, []);

  return (
    <Ctx.Provider value={{ mostrar }}>
      {children}
      <div className="toast-zona" aria-live="polite">
        {aviso && (
          <div className={`alert alert-${aviso.tipo} shadow mb-0`} role="status">
            {aviso.mensaje}
          </div>
        )}
      </div>
    </Ctx.Provider>
  );
}

export function useToast(): Toast {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useToast debe usarse dentro de <ToastProvider>");
  return ctx;
}
