import { solicitar } from "./http";
import type { Pago } from "../lib/api";

export const pagoService = {
  listar: (usuarioId: string, admin: boolean, signal?: AbortSignal) =>
    solicitar<Pago[]>(admin ? "/pagos" : "/pagos/usuario/" + usuarioId, "GET", undefined, signal),
  pagar: (datos: unknown) => solicitar<Pago>("/pagos/checkout", "POST", datos),
  registrarComprobante: (id: number, datos: unknown) =>
    solicitar<void>("/pagos/" + id + "/comprobante", "POST", datos),
  obtenerComprobante: (id: number) =>
    solicitar<{ banco: string; numeroOperacion: string; archivoUrl: string }>("/pagos/" + id + "/comprobante"),
  verificarComprobante: (id: number) =>
    solicitar<void>("/pagos/" + id + "/comprobante/verificar", "POST", {}),
  confirmar: (id: number) => solicitar<void>("/pagos/" + id + "/confirmar", "POST"),
  rechazar: (id: number, datos: unknown) => solicitar<void>("/pagos/" + id + "/rechazar", "POST", datos),
  reembolsar: (id: number) => solicitar<void>("/pagos/" + id + "/reembolsar", "POST"),
};
