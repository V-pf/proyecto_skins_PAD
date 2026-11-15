import { useState } from "react";

export type Valores = Record<string, string | boolean>;
export type Errores<T> = Partial<Record<keyof T, string>>;

export interface FormApi<T extends Valores> {
  id: string;
  valores: T;
  /** Solo trae el error de los campos que el usuario ya tocó (o de todos tras intentar enviar). */
  errores: Errores<T>;
  intento: boolean;
  set: (nombre: keyof T, valor: string | boolean) => void;
  tocar: (nombre: keyof T) => void;
  /** Marca el intento de envío y devuelve true si no hay errores. */
  validar: () => boolean;
  reiniciar: () => void;
}

/**
 * Hook de formularios: reemplaza al validaciones.js original.
 * `validar` recibe los valores y devuelve un objeto { campo: "mensaje" } con
 * los errores (vacío = todo bien).
 */
export function useFormulario<T extends Valores>(
  id: string,
  inicial: T,
  validar: (v: T) => Errores<T>
): FormApi<T> {
  const [valores, setValores] = useState<T>(inicial);
  const [tocados, setTocados] = useState<Partial<Record<keyof T, boolean>>>({});
  const [intento, setIntento] = useState(false);

  const todos = validar(valores);
  const visibles: Errores<T> = {};
  (Object.keys(todos) as (keyof T)[]).forEach((k) => {
    const mensaje = todos[k];
    if (mensaje && (intento || tocados[k])) visibles[k] = mensaje;
  });

  return {
    id,
    valores,
    errores: visibles,
    intento,
    set: (nombre, valor) => setValores((prev) => ({ ...prev, [nombre]: valor } as T)),
    tocar: (nombre) => setTocados((prev) => ({ ...prev, [nombre]: true })),
    validar: () => {
      setIntento(true);
      return Object.values(todos).every((m) => !m);
    },
    reiniciar: () => {
      setValores(inicial);
      setTocados({});
      setIntento(false);
    },
  };
}
