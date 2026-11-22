import { solicitar } from "./http";
import type { Pago } from "../lib/api";

export const pagoService = {
  listar: (usuarioId: string, admin: boolean, signal?: AbortSignal) =>
    solicitar<Pago[]>(admin ? "/pagos" : "/pagos/usuario/" + usuarioId, "GET", undefined, signal),
  pagar: (datos: unknown) => solicitar<Pago>("/pagos/checkout", "POST", datos),
};
