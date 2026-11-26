/**
 * Reglas de validación: es el mismo contenido que tenía js/validaciones.js
 * (REGLAS), pero ahora tipado y reutilizable desde los componentes de React.
 * Cada función devuelve "" si el valor es válido o el mensaje de error.
 */

type Regla = { test: (v: string) => boolean; mensaje: string };

export const REGLAS = {
  nombre: {
    test: (v) => /^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{2,60}$/.test(v.trim()),
    mensaje: "Usa solo letras y espacios (2 a 60 caracteres).",
  },
  usuario: {
    test: (v) => /^[A-Za-z0-9_]{3,20}$/.test(v.trim()),
    mensaje: "Entre 3 y 20 caracteres: letras, números o guion bajo, sin espacios.",
  },
  telefono_cl: {
    test: (v) => /^(\+?56)?\s?9\s?\d{4}\s?\d{4}$/.test(v.trim()),
    mensaje: "Usa el formato +56 9 XXXX XXXX.",
  },
  precio: {
    test: (v) => Number(v) >= 100 && Number(v) <= 50000000,
    mensaje: "Ingresa un precio entre $100 y $50.000.000.",
  },
  float_desgaste: {
    test: (v) => Number(v) >= 0 && Number(v) <= 1,
    mensaje: "El float va de 0,0000 a 1,0000.",
  },
} satisfies Record<string, Regla>;

export type NombreRegla = keyof typeof REGLAS;

export const OBLIGATORIO = "Este campo es obligatorio.";

export function vacio(v: string): boolean {
  return v.trim() === "";
}

/** Obligatorio (opcionalmente con una regla extra). */
export function requerido(v: string, regla?: NombreRegla): string {
  if (vacio(v)) return OBLIGATORIO;
  if (regla && !REGLAS[regla].test(v)) return REGLAS[regla].mensaje;
  return "";
}

/** Opcional: solo valida si el usuario escribió algo. */
export function opcional(v: string, regla: NombreRegla): string {
  if (vacio(v)) return "";
  return REGLAS[regla].test(v) ? "" : REGLAS[regla].mensaje;
}

export function correo(v: string): string {
  if (vacio(v)) return OBLIGATORIO;
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v.trim())
    ? ""
    : "Escribe un correo con formato válido, como nombre@dominio.com.";
}

export function largo(v: string, min: number, max?: number): string {
  if (vacio(v)) return OBLIGATORIO;
  if (v.trim().length < min) return `Escribe al menos ${min} caracteres.`;
  if (max !== undefined && v.length > max) return `Puedes escribir como máximo ${max} caracteres.`;
  return "";
}

export function seleccion(v: string): string {
  return vacio(v) ? "Selecciona una opción." : "";
}

export function aceptado(v: boolean, mensaje = "Debes aceptar para continuar."): string {
  return v ? "" : mensaje;
}

export function mayorDeEdad(fechaISO: string): string {
  if (vacio(fechaISO)) return OBLIGATORIO;
  const nac = new Date(fechaISO + "T00:00:00");
  if (Number.isNaN(nac.getTime())) return "Ingresa una fecha válida.";
  const hoy = new Date();
  if (nac > hoy) return "La fecha no puede ser futura.";
  let edad = hoy.getFullYear() - nac.getFullYear();
  const aunNoCumple =
    hoy.getMonth() < nac.getMonth() || (hoy.getMonth() === nac.getMonth() && hoy.getDate() < nac.getDate());
  if (aunNoCumple) edad--;
  return edad >= 18 ? "" : "Debes ser mayor de 18 años para crear una cuenta.";
}

/** Puntaje 0-4 para la barrita de fuerza de la contraseña. */
export function puntajeClave(v: string): number {
  let p = 0;
  if (v.length >= 8) p++;
  if (/[A-Z]/.test(v)) p++;
  if (/[0-9]/.test(v)) p++;
  if (/[^A-Za-z0-9]/.test(v)) p++;
  return p;
}
