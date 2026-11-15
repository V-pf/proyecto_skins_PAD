const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];

export function formatoCLP(n: number): string {
  return "$" + Math.round(n).toLocaleString("es-CL");
}

export function formatoFloat(f: number | null): string {
  return f === null ? "—" : f.toFixed(4).replace(".", ",");
}

/** "2026-09-18" -> "18 sep 2026" (se parte el texto a mano para evitar problemas de zona horaria) */
export function fechaCorta(iso: string): string {
  const [anio, mes, dia] = iso.split("-");
  const m = Number(mes) - 1;
  if (!anio || !dia || m < 0 || m > 11) return iso;
  return `${dia} ${MESES[m]} ${anio}`;
}

export function hoyISO(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}`;
}

export const COMISION = 0.05;

// Calcula la comisión en pesos enteros y la suma al subtotal.
export function calcularTotales(subtotal: number): { subtotal: number; comision: number; total: number } {
  const comision = Math.floor(subtotal * COMISION);
  return { subtotal, comision, total: subtotal + comision };
}
