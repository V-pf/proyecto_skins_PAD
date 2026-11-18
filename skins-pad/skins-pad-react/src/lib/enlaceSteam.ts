import { OBLIGATORIO } from "./validaciones";

export const prefijosSteam = {
  perfil: "https://steamcommunity.com/id/",
  intercambio: "https://steamcommunity.com/tradeoffer/",
};
export type TipoEnlaceSteam = keyof typeof prefijosSteam;

export function errorSteam(tipo: TipoEnlaceSteam, sufijo: string) {
  if (!sufijo) return tipo === "perfil" ? "" : OBLIGATORIO;
  if (prefijosSteam[tipo].length + sufijo.length > 500) return "Link inválido";
  if (tipo === "intercambio") return /^\d+$/.test(sufijo) ? "" : "Link inválido";
  try {
    const segmento = decodeURIComponent(sufijo);
    if (/[\s/\\?#:\u0000-\u001f\u007f]/u.test(segmento) || segmento === "." || segmento === "..") return "Link inválido";
  } catch {
    return "Link inválido";
  }
  return "";
}

export function urlSteam(tipo: TipoEnlaceSteam, sufijo: string) {
  return sufijo ? prefijosSteam[tipo] + sufijo : "";
}
