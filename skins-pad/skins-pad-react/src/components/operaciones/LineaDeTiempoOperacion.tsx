import {nombreEstadoOperacion,type EstadoOperacion} from './EstadoOperacionBadge';
export interface Evento {id:number;actor:string;accion:string;estado:EstadoOperacion;motivo:string|null;fecha:string}
const acciones:Record<string,string>={CREAR:'Compra registrada',VERIFICAR:'Pago verificado',VERIFICACION_ADMIN:'Verificación administrativa',ACEPTAR:'Vendedor acepta',RECHAZAR:'Vendedor rechaza',ENTREGAR:'Entrega marcada',RECIBIR:'Recepción confirmada',AUTO_LIBERAR:'Liberación por plazo',EXPIRAR:'Plazo vencido',DISPUTAR:'Disputa abierta',CONGELAR:'Operación congelada',RESOLVER_COMPRADOR:'Resolución: reembolso',RESOLVER_VENDEDOR:'Resolución: liberar fondos',CANCELAR:'Cancelación',IMPORTAR:'Historial importado'};
const accionesSimuladas: Record<string, string> = {
  VERIFICAR: "Pago simulado registrado",
  AUTO_LIBERAR: "Compra de demostración completada por plazo",
  RESOLVER_COMPRADOR: "Resolución a favor del comprador",
  RESOLVER_VENDEDOR: "Resolución a favor del vendedor",
};

export default function LineaDeTiempoOperacion({ eventos, simulado }: { eventos: Evento[]; simulado?: boolean }) {
  return (
    <ol aria-label="Historial de la operación" className="mt-3">
      {eventos.map((e) => (
        <li key={e.id} className="mb-2">
          <strong>{(simulado ? accionesSimuladas[e.accion] : undefined) ?? acciones[e.accion] ?? e.accion}</strong>
          {" · "}{nombreEstadoOperacion(e.estado, simulado)}<br />
          <small>
            <time dateTime={e.fecha}>{new Date(e.fecha).toLocaleString("es-CL", { timeZone: "America/Santiago" })}</time>
            {" · "}{e.actor}
          </small>
          {e.motivo && <p className="mb-0">{e.motivo}</p>}
        </li>
      ))}
    </ol>
  );
}
