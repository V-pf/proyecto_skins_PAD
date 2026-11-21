import { solicitar } from './http';
export type TipoObjetivo = 'PUBLICACION'|'OPERACION'|'USUARIO';
export type EstadoCaso = 'ABIERTO'|'EN_REVISION'|'RESUELTO'|'RECHAZADO'|'CERRADO';
export type Prioridad = 'NORMAL'|'ALTA'|'URGENTE';
export type AccionReporte = 'SIN_ACCION'|'ADVERTENCIA'|'PUBLICACION_DESPUBLICADA'|'OPERACION_CONGELADA'|'OPERACION_REEMBOLSADA'|'USUARIO_SUSPENDIDO';
export interface Archivo { nombre:string; mime:string; base64:string }
export interface Adjunto {id:number;nombre:string;mime:string;mensajeId:number|null}
export interface Evento {id:number;autor:string;fecha:string;accion:string;anterior:string|null;nuevo:string|null;motivo:string}
export interface Mensaje {id:number;autor:string;rol:string;contenido:string;fecha:string}
export interface Ticket {id:number;version:number;asunto:string;categoria:string;estado:string;prioridad:Prioridad|null;asignado:string|null;operacionId:number|null;armaId:number|null;creada:string;actualizada:string;mensajes:Mensaje[];adjuntos:Adjunto[];eventos:Evento[]}
export interface Reporte {id:number;version:number;tipoObjetivo:TipoObjetivo;objetivoId:string;motivo:string;descripcion:string;estado:string;prioridad:Prioridad|null;asignado:string|null;creada:string;resolucion:string|null;accionTomada:AccionReporte|null;accionPendiente:boolean;errorAccion:string|null;evidencia:string[];adjuntos:Adjunto[]}
export interface AlertaPrecio {productoId:number;version:number;nombre:string;estado:string;precio:number;mediana:number|null;comparables:number;explicacion:string}
export interface ContextoReporte {reporte:Reporte;objetivo:Record<string,unknown>;precio:AlertaPrecio|null;rechazosReportante:number;otrosObjetivo:Reporte[];historialDenunciado:Reporte[];eventos:Evento[];acciones:AccionReporte[]}
export interface FilaCaso {id:number;clase:'TICKET'|'REPORTE';titulo:string;estado:string;prioridad:Prioridad;tipoObjetivo:string;fecha:string;asignado:string|null}
export interface PaginaCasos {filas:FilaCaso[];total:number;pagina:number;tamano:number}
export interface Grupo {etiqueta:string;cantidad:number}
export interface ResumenSoporte {pendientes:number;tickets:Grupo[];reportes:Grupo[];horasTicket:number|null;horasReporte:number|null;motivos:Grupo[];preciosPendientes:number;porcentajeRechazados:number;denunciados:Grupo[]}
export interface AvisoSoporte {id:number;texto:string;enlace:string;fecha:string}
export interface CrearTicket {asunto:string;categoria:string;contenido:string;operacionId?:number;armaId?:number;adjuntos:Archivo[]}
export interface CrearReporte {tipoObjetivo:TipoObjetivo;objetivoId:string;motivo:string;descripcion:string;evidenciaUrls:string[];adjuntos:Archivo[]}
export const soporte = {
 crearTicket:(r:CrearTicket)=>solicitar<Ticket>('/tickets','POST',r),
 responder:(id:number,contenido:string,adjuntos:Archivo[])=>solicitar<Ticket>('/tickets/'+id+'/mensajes','POST',{contenido,adjuntos}),
 cerrar:(t:Ticket)=>solicitar<Ticket>('/tickets/'+t.id+'/cerrar','POST',{version:t.version,motivo:'Cierre solicitado por el usuario'}),
 cambiarTicket:(t:Ticket,estado:EstadoCaso,prioridad:Prioridad,motivo:string)=>solicitar<Ticket>('/admin/tickets/'+t.id+'/estado','PATCH',{version:t.version,estado,prioridad,motivo}),
 asignar:(tipo:'tickets'|'reportes',id:number,version:number,adminId:string|null,motivo:string)=>solicitar('/admin/'+tipo+'/'+id+'/asignar','POST',{version,adminId,motivo}),
 crearReporte:(r:CrearReporte)=>solicitar<Reporte>('/reportes','POST',r),
 resolver:(r:Reporte,estado:EstadoCaso,accion:AccionReporte,motivo:string,resumenPublico:string)=>solicitar<Reporte>('/admin/reportes/'+r.id+'/resolver','POST',{version:r.version,estado,accion,motivo,resumenPublico,confirmado:true}),
 reintentar:(id:number)=>solicitar<Reporte>('/admin/reportes/'+id+'/reintentar','POST'),
 analizar:()=>solicitar<AlertaPrecio[]>('/admin/soporte/precios/analizar','POST'),
 validarPrecio:(a:AlertaPrecio,motivo:string)=>solicitar('/admin/soporte/precios/'+a.productoId+'/validar','POST',{version:a.version,motivo,confirmado:true}),
};
export const categoriasTicket=['CUENTA','PAGO','SALDO','PUBLICACION','ERROR_TECNICO','OTRO'];
export const motivosReporte=['PRECIO_EXORBITANTE','PUBLICACION_FALSA','SKIN_NO_COINCIDE_CON_DESCRIPCION','FRAUDE_EN_OPERACION','SUPLANTACION','OTRO'];
export const etiquetasAccion:Record<AccionReporte,string>={SIN_ACCION:'Sin sanción',ADVERTENCIA:'Advertir al usuario',PUBLICACION_DESPUBLICADA:'Despublicar publicación',OPERACION_CONGELADA:'Congelar operación',OPERACION_REEMBOLSADA:'Resolver a favor del comprador',USUARIO_SUSPENDIDO:'Suspender usuario'};
const etiquetas:Record<string,string>={PUBLICACION:'publicación',OPERACION:'operación',USUARIO:'usuario',CUENTA:'cuenta',PAGO:'pago',SALDO:'saldo',ERROR_TECNICO:'error técnico',OTRO:'otro',PRECIO_EXORBITANTE:'precio atípico',PUBLICACION_FALSA:'publicación falsa',SKIN_NO_COINCIDE_CON_DESCRIPCION:'skin no coincide con la descripción',FRAUDE_EN_OPERACION:'fraude en operación',SUPLANTACION:'suplantación',ABIERTO:'abierto',EN_REVISION:'en revisión',RESUELTO:'resuelto',RECHAZADO:'rechazado',CERRADO:'cerrado',NORMAL:'normal',ALTA:'alta',URGENTE:'urgente'};
export const textoEnum=(v:string)=>etiquetas[v]??v.split('_').join(' ').toLocaleLowerCase('es-CL');
