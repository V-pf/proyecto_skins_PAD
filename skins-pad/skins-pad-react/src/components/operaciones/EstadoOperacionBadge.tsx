export type EstadoOperacion='PAGO_PENDIENTE'|'PAGO_RETENIDO'|'ACEPTADA_POR_VENDEDOR'|'ENTREGADA'|'EN_DISPUTA'|'CONGELADA'|'COMPLETADA'|'REEMBOLSADA'|'CANCELADA';
export const nombres:Record<EstadoOperacion,string>={PAGO_PENDIENTE:'Pago pendiente',PAGO_RETENIDO:'Fondos retenidos · espera al vendedor',ACEPTADA_POR_VENDEDOR:'Aceptada · pendiente de entrega',ENTREGADA:'Entregada · confirma recepción',EN_DISPUTA:'En disputa',CONGELADA:'Congelada por administración',COMPLETADA:'Completada',REEMBOLSADA:'Reembolsada al saldo',CANCELADA:'Cancelada'};
export function nombreEstadoOperacion(estado: EstadoOperacion, simulado = false) {
  if (simulado && estado === "PAGO_RETENIDO") return "Pago simulado · espera al vendedor";
  if (simulado && estado === "REEMBOLSADA") return "Cancelada en la simulación";
  return nombres[estado];
}

export default function EstadoOperacionBadge({ estado, simulado }: { estado: EstadoOperacion; simulado?: boolean }) {
  const clase = estado === "COMPLETADA" ? "text-bg-success"
    : ["EN_DISPUTA", "CONGELADA"].includes(estado) ? "text-bg-warning" : "text-bg-secondary";
  return <span className={`badge ${clase}`}>{nombreEstadoOperacion(estado, simulado)}</span>;
}
