# Soporte y reportes de Skins PAD

Implementación del 18 de noviembre de 2026 sobre la versión actual del repositorio. No restaura el editor administrativo descartado. Los nombres reales del catálogo son Producto y productos, no arma. El campo armaId de soporte referencia Producto.id.

## Decisión y alcance

Ticket y Reporte son entidades independientes: un ticket mantiene una conversación de ayuda; un reporte investiga un objetivo y puede coordinar una sanción. Comparten validación, adjuntos, cuotas, notificaciones y un historial inmutable soporte_evento con entidad + entidad_id. No hay endpoints para editar o borrar eventos.

Ticket (8085) administra conversaciones en skinspad_ticket. Reporte (8086) administra denuncias, moderación y alertas de precio en skinspad_reporte. Cada servicio conserva sus adjuntos, avisos y eventos. Reporte reúne la bandeja y las estadísticas por HTTP. Pago expone las operaciones de custodia mediante una API interna autenticada.

Las referencias tipo_objetivo + objetivo_id se validan contra catálogo, usuario o la API interna de Pago. Evitan FK entre bases de servicios diferentes. La alternativa sería producto_id, operacion_id y usuario_id con una restricción de exactamente una columna; facilitaría FK solo si todas las tablas compartieran base y propietario. No se adopta aquí. Los mensajes sí tienen FK local al ticket. Los eventos/adjuntos validan el caso al crearse y no se permite borrar casos.

Supuestos: solo clientes activos crean casos; administradores gestionan. No hay correo ni integración Steam nueva. Un reporte de operación denuncia a la contraparte. Reportar NO abre una disputa ni congela fondos: el comprador debe usar Abrir disputa cuando corresponda; administración puede congelar desde el reporte. Reembolso y liberación siguen siendo responsabilidad de custodia.

## Instalación en XAMPP

1. Detener los servicios y ejecutar `skins-pad-backend/crear-bases.sql` en MySQL de XAMPP.
2. Si ya existen casos en Pago, con los destinos vacíos ejecutar una vez `skins-pad-backend/migrar-soporte.sql` antes de iniciar Ticket y Reporte. Copia los datos con sus IDs en una transacción y conserva el origen. Para una instalación nueva, omitir la migración.
3. En PowerShell, desde `skins-pad-backend`, ejecutar `.\iniciar-servicio.ps1 ticket` y `.\iniciar-servicio.ps1 reporte` en terminales independientes. Iniciar también los servicios existentes y Gateway. Hibernate actualiza el esquema al arrancar. El perfil demo usa bases H2 locales independientes y comienza con soporte vacío.
4. Configurar las credenciales con TICKET_DB_USERNAME, TICKET_DB_PASSWORD, REPORTE_DB_USERNAME y REPORTE_DB_PASSWORD si XAMPP requiere autenticación. Las URL se pueden ajustar con TICKET_DB_URL, REPORTE_DB_URL, TICKET_URL y REPORTE_URL.

## Archivos principales

Rutas relativas a la raíz del repositorio:

- skins-pad-backend/{ticket,reporte}/src/main/java/cl/skinspad/{ticket,reporte}/soporte/: entidades, DTO, controllers, servicios, validaciones, repositorios, bandeja SQL, estadísticas, archivos, cuotas y detección de precios.
- .../soporte/EstadosSoporte.java: transiciones de casos.
- .../soporte/TicketService.java: conversación y gestión de tickets.
- .../soporte/ReporteService.java y ReporteAcciones.java: solicitud persistida, ejecución y conciliación de acciones.
- .../soporte/SoporteContexto.java: pertenencia de operaciones y validación de objetivos/acciones.
- .../soporte/PrecioAtipico.java, PrecioService.java y PrecioJob.java: mediana, alertas y revisión.
- skins-pad-backend/catalogo/src/main/java/cl/skinspad/catalogo/moderacion/: adaptador de despublicación y auditoría idempotente.
- skins-pad-backend/usuario/src/main/java/cl/skinspad/usuario/moderacion/: adaptador de suspensión y auditoría idempotente.
- Gateway: AccesoFilter y application.yml incorporan rutas y verificaciones de permisos.
- skins-pad-react/src/services/soporte.service.ts y hooks/useSoporte.ts: contratos tipados, HTTP, carga, error y actualización.
- skins-pad-react/src/components/soporte/: EstadoBadge, ConversacionTicket, FormularioReporte, BandejaAdmin, evidencias, avisos y alertas.
- skins-pad-react/src/pages/soporte/: inicio, detalle de ticket, mis reportes, bandeja administrativa y resolución.
- App, Header, MenuCuenta, DetalleProducto, PerfilPublico, Operaciones y AdminEstadisticas: integración de navegación.

## Estados y transiciones

| Estado actual | Acción | Actor | Siguiente |
|---|---|---|---|
| ABIERTO | Iniciar revisión | Administrador | EN_REVISION |
| ABIERTO / EN_REVISION | Resolver | Administrador | RESUELTO |
| ABIERTO / EN_REVISION | Rechazar con motivo | Administrador | RECHAZADO |
| ABIERTO / EN_REVISION / RESUELTO / RECHAZADO | Cerrar ticket | Propietario o administrador | CERRADO |
| ABIERTO / EN_REVISION | Añadir mensaje a ticket | Propietario o administrador | Sin cambio |
| EN_REVISION | Aplicar acción y seguir investigando | Administrador | EN_REVISION |

No se reabren casos terminales; puede crearse otro caso. Reportes no usan CERRADO. Clientes ven En atención, Finalizado o Cerrado, además de su conversación/resumen público. Administradores ven estados completos, prioridad, asignación y auditoría. Cambiar prioridad no equivale a cambiar el estado. La primera fecha terminal del ticket se conserva al cerrarlo después de resolverlo.

## Flujo de resolución y consistencia

1. El servidor valida sesión, rol, versión y acción permitida según el objetivo actual.
2. Bloquea el reporte y guarda una solicitud con referencia REP-id-UUID, actor, motivo privado, acción y estado deseado. Confirma esta transacción antes de llamar al servicio remoto.
3. Ejecuta la acción existente: CatalogoService.eliminarProducto para despublicar, UsuarioService.actualizarEstado para suspender, CustodiaService.accion para congelar o resolver a favor del comprador. Advertencia crea una notificación; no cambia el saldo ni sanciona automáticamente.
4. Solo después de confirmación finaliza el reporte, agrega un evento y avisa al reportante. Puede dejarlo EN_REVISION tras congelar para resolver posteriormente.
5. Si se pierde la respuesta, el reporte conserva accionPendiente y el job o el botón Reintentar conciliación repite la MISMA referencia. Catálogo/usuario guardan esa referencia única en moderacion_reporte dentro de la misma transacción que el cambio. Custodia comprueba el evento correlacionado y conserva su idempotencia contable.
6. Un rechazo definitivo 400/404/409 del adaptador deja el reporte abierto, registra ACCION_RECHAZADA y permite elegir otra acción. Una falla de transporte deja la acción pendiente; no finge éxito ni devuelve fondos por segunda vez.

No existe transacción distribuida: es una coordinación persistida con reintentos. Un bloqueo por otra operación, administrador deshabilitado o una acción de custodia sin confirmación requiere conciliación en Gestión de Pagos. No se elimina una solicitud incierta para ejecutar otra sanción a ciegas. Las órdenes externas incluyen referencia y tipo de acción, pero nunca el motivo privado ni la identidad del reportante; el motivo completo queda en soporte_evento.

Despublicar utiliza las reglas actuales de catálogo: ACTIVO o PENDIENTE_APROBACION sin reserva pasa a ELIMINADO. Una publicación reservada no se despublica desde soporte. Congelar/reembolsar utiliza MaquinaOperacion; una venta ya completada no se puede reembolsar saltando esas reglas. No se restauró ni inventó un editor de estados de armas.

## Seguridad, privacidad y abuso

- Gateway valida sesión/rol y consulta acceso al caso o adjunto; el servicio propietario vuelve a comprobar propietario/rol. Un cliente no puede acceder a tickets/reportes ajenos. Las operaciones requieren ser comprador o vendedor.
- Los adaptadores internos no se publican por Gateway; exigen la clave interna y un administrador activo validado por el servicio de usuarios. El navegador nunca envía actor/clave interna.
- DTO de entrada con lista blanca; campos desconocidos, estado/prioridad/asignación enviados por cliente y números fraccionarios para IDs se rechazan.
- Textos normalizados NFKC, recortados y limitados; HTML y controles peligrosos se rechazan. React muestra texto escapado. Asunto 160, contenido/descripción 4000, motivo de resolución 400, resumen público 1000 caracteres.
- Máximo diario por America/Santiago: 5 tickets y 10 reportes. Una fila bloqueada por usuario serializa el consumo; la cuota se revierte si falla la creación.
- Clave única de reporte abierto por usuario/tipo/id; se libera al resolver/rechazar. IDs numéricos se normalizan para impedir duplicados con ceros iniciales. No se permite autoreporte ni reportar la propia publicación.
- Tres usuarios distintos con reportes abiertos sobre una publicación elevan la prioridad a URGENTE. Se bloquea una fila por objetivo para que el conteo sea consistente. No se despublica automáticamente.
- Rechazos anteriores del reportante se muestran como contexto administrativo; no hay sanciones por conteo automático.
- Reportante solo ve sus datos y resumen público. Denunciado no recibe acceso al caso ni identidad del reportante. Administración debe redactar un resumen sin datos sensibles.
- Adjuntos: hasta 3 PNG/JPEG por creación o mensaje, 512 KiB cada uno, dimensiones máximas 4096 y 10 millones de píxeles. Se valida el formato real y se recodifica para eliminar metadatos/contenido añadido. Límite HTTP de 2.200.000 bytes. SVG, HTML y MIME falso se rechazan.
- Descargas autenticadas con Content-Disposition attachment, nosniff, CSP sandbox y no-store. URLs: máximo 3 HTTPS, 500 caracteres cada una, sin credenciales; servidor no descarga URLs externas. Enlaces se abren con noopener/noreferrer.
- @Version y versión esperada devuelven 409 a una edición administrativa obsoleta; bloqueo de fila serializa resolución, conversación y cuotas. No hay endpoints de edición/borrado de auditoría. Un administrador de base de datos sigue teniendo capacidad de modificar tablas: no es un almacenamiento criptográficamente inviolable.

## API

Todas las rutas siguientes incluyen el prefijo /api y Authorization: Bearer sesión. C = cliente propietario, A = administrador. Crear devuelve 201; consultar o modificar devuelve 200 con DTO, nunca entidad JPA.

| Método y ruta | Permiso | Body / respuesta |
|---|---|---|
| POST /tickets | C | NuevoTicket → TicketVista |
| GET /tickets/mis-tickets | C | Lista de tickets propios, sin conversaciones |
| GET /tickets/{id} | C/A | TicketVista con mensajes y adjuntos; eventos solo A |
| POST /tickets/{id}/mensajes | C/A | contenido, adjuntos → TicketVista |
| POST /tickets/{id}/cerrar | C/A | version, motivo → TicketVista |
| PATCH /admin/tickets/{id}/estado | A | version, estado, prioridad opcional, motivo → TicketVista |
| POST /admin/tickets/{id}/asignar | A | version, adminId nullable, motivo → TicketVista |
| POST /reportes | C | NuevoReporte → ReporteVista |
| GET /reportes/mis-reportes | C | Lista propia |
| GET /reportes/{id} | C/A | ReporteVista, datos privados omitidos para C |
| GET /admin/reportes/{id} | A | reporte, objetivo mínimo, precio, historial, acciones permitidas |
| GET /admin/reportes/{id}/eventos | A | Lista de EventoVista |
| POST /admin/reportes/{id}/asignar | A | version, adminId nullable, motivo |
| POST /admin/reportes/{id}/resolver | A | Resolver → ReporteVista; revisar accionPendiente |
| POST /admin/reportes/{id}/reintentar | A | Sin body; conserva referencia original |
| GET /admin/soporte/bandeja | A | Pagina: filas, total, pagina, tamano |
| GET /admin/tickets o /admin/reportes | A | La misma bandeja restringida por clase |
| GET /admin/soporte/stats | A | Resumen de métricas |
| GET /admin/soporte/precios | A | Alertas pendientes |
| POST /admin/soporte/precios/analizar | A | Ejecutar análisis, devuelve pendientes |
| POST /admin/soporte/precios/{id}/validar | A | version, motivo, confirmado:true |
| GET /soporte/notificaciones | C/A | Últimos 50 avisos del destinatario; ADMIN es bandeja compartida |
| GET /tickets/adjuntos/{id} o /reportes/adjuntos/{id} | C/A propietario | Imagen descargable |
| GET /soporte/acceso?tipo=TICKET&id=1 | C/A propietario | permitido:true; usado por Gateway |

Filtros bandeja: clase=TICKET/REPORTE, estado (incluye PENDIENTES), prioridad, tipo=PUBLICACION/OPERACION/USUARIO, desde/hasta YYYY-MM-DD, pagina desde 0, tamano 1–100 (20 por defecto). Fechas inclusivas por día de Santiago. Orden fijo: urgente, alta, normal; luego más antiguo. Los filtros persisten en query params del navegador.

Ejemplo NuevoTicket:
~~~json
{"asunto":"Consulta de pago","categoria":"PAGO","contenido":"Necesito ayuda con mi operación","operacionId":12,"adjuntos":[]}
~~~
Ejemplo NuevoReporte:
~~~json
{"tipoObjetivo":"PUBLICACION","objetivoId":"23","motivo":"PUBLICACION_FALSA","descripcion":"La descripción no corresponde","evidenciaUrls":["https://ejemplo.cl/evidencia"],"adjuntos":[]}
~~~
Ejemplo Resolver (con versión del GET actual):
~~~json
{"version":0,"estado":"EN_REVISION","accion":"OPERACION_CONGELADA","motivo":"Evidencia pendiente de revisión","resumenPublico":"Estamos revisando tu operación","confirmado":true}
~~~
Archivo: nombre, mime (image/png o image/jpeg), base64 sin prefijo data:. Las categorías/motivos/acciones completas están en EstadosSoporte.java y soporte.service.ts.

Errores: 400 validación/transición mal formada; 401 sesión ausente/inválida; 403 rol/propiedad; 404 inexistente; 409 versión/estado/duplicado/reserva; 413 cuerpo demasiado grande; 429 cuota diaria; 502 dependencia no disponible (contrato existente de pago). Una resolución puede devolver 200 con accionPendiente=true porque la solicitud quedó guardada para recuperación: no equivale a sanción confirmada. Consultar errorAccion y el historial.

## Precio atípico

Se usan alertas, no denuncias automáticas: evita inflar denuncias de usuarios y presentar una desviación estadística como prueba de fraude. Job cada 15 minutos, primera ejecución tras 60 segundos; también hay análisis manual. No cambia estado ni precio de una publicación.

Comparables: otro producto ACTIVO, no reservado, nombre normalizado idéntico, mismo tipo, desgaste y StatTrak, precio positivo. Mínimo 5 por defecto (nunca menos de 3). Mediana ordenada, promedio de los dos valores centrales si hay cantidad par. Desviación estricta >3 veces o <0,3 veces; la igualdad no alerta. No hay moneda extranjera ni conversión.

Se excluyen stickers, grado especial/contrabando, float ausente o extremo (<0,01 o >0,95). El esquema no tiene atributos estructurados de stickers aplicados/patrones raros: no puede garantizar comparabilidad de esas primas. Por eso la señal requiere investigación, nunca sanción automática. Sin comparables muestra Sin datos suficientes.

Revisada y válida guarda una huella de nombre, atributos y precio. El job no vuelve a alertar mientras esa huella no cambie, aunque cambie el mercado; una modificación de la publicación obliga a revisar otra vez. Confirmación exige versión vigente, motivo y huella actual.

Consulta equivalente de candidatos, ejecutable en catálogo (el código actual obtiene una proyección compacta vía Feign y normaliza en Java):
~~~sql
SELECT id, nombre, tipo, desgaste, stattrak, float_valor, grado, precio
FROM productos
WHERE id <> :id AND estado = 'ACTIVO' AND reserva_pago IS NULL
  AND LOWER(TRIM(nombre)) = LOWER(TRIM(:nombre))
  AND tipo = :tipo AND desgaste = :desgaste AND stattrak = :stattrak
  AND precio > 0
ORDER BY precio;
~~~
La normalización NFKC/espacios internos y exclusiones se aplican en Java; LOWER/TRIM por sí solos no son equivalentes. La mediana se calcula después de esas exclusiones. Índice sugerido para catálogo a escala: (estado,tipo,desgaste,stattrak); considerar nombre normalizado persistido si se migra a consulta incremental. No se agrega una columna solo para esta primera versión.

## Avisos y auditoría

| Evento | Destinatario | Dónde se genera |
|---|---|---|
| Ticket creado | Administradores | TicketService.crear |
| Mensaje del cliente | Administradores | TicketService.responder |
| Respuesta administrativa | Dueño del ticket | TicketService.responder |
| Cambio de estado/prioridad | Dueño o administración si cierra cliente | TicketService.cambiar |
| Asignación de ticket | Administradores | TicketService.asignar |
| Reporte creado/urgente | Administradores | ReporteService.crear |
| Acción confirmada/resolución | Reportante | ReporteService.finalizar |
| Advertencia | Denunciado, texto genérico | ReporteService.finalizar |
| Acción sin confirmar | Administradores | ReporteService.fallo |
| Precio atípico nuevo | Administradores | PrecioService.analizar |

Persistencia transaccional y clave única por evento/referencia evitan repetir notificaciones al reintentar. El frontend consulta avisos cada 30 segundos mientras la pestaña está visible. Son últimos avisos, no un contador de no leídos: esta versión no implementa recibos de lectura ni correo. Auditoría y acción solicitada/confirmada son eventos distintos con la misma referencia, no duplicados.

## Métricas y consultas

SoporteStats entrega agregados sin mezclar ingresos ni modificar la definición de venta del dashboard. Tarjeta Pendientes de revisión y enlaces urgentes en Estadísticas; detalle en Soporte. No se agregan ni se liberan fondos por generar métricas.

~~~sql
SELECT estado, COUNT(*) FROM soporte_ticket GROUP BY estado;
SELECT estado, COUNT(*) FROM soporte_reporte GROUP BY estado;
SELECT AVG(TIMESTAMPDIFF(SECOND,fecha_creacion,fecha_cierre))/3600.0
FROM soporte_ticket WHERE fecha_cierre IS NOT NULL;
SELECT AVG(TIMESTAMPDIFF(SECOND,fecha_creacion,fecha_resolucion))/3600.0
FROM soporte_reporte WHERE fecha_resolucion IS NOT NULL;
SELECT motivo, COUNT(*) FROM soporte_reporte GROUP BY motivo;
SELECT COUNT(*) FROM soporte_alerta_precio WHERE estado='PENDIENTE';
SELECT 100.0 * SUM(CASE WHEN estado='RECHAZADO' THEN 1 ELSE 0 END)
 / NULLIF(COUNT(*),0) FROM soporte_reporte WHERE estado IN ('RESUELTO','RECHAZADO');
SELECT denunciado_id, COUNT(*) FROM soporte_reporte
GROUP BY denunciado_id ORDER BY COUNT(*) DESC, denunciado_id LIMIT 20;
SELECT COUNT(*) FROM soporte_reporte WHERE reportante_id=:usuario AND estado='RECHAZADO';
~~~
Promedio de tickets incluye cierres solicitados por el usuario; no es exclusivamente tiempo de respuesta del administrador. Si no hay cierres devuelve null. Tasa de rechazos usa finalizados como denominador y devuelve 0 si no hay ninguno. Denuncias por usuario no equivalen a fraude demostrado. Identificadores de denunciados solo se exponen a administradores.

La bandeja usa UNION ALL de ambos casos, filtra en SQL antes de LIMIT/OFFSET y calcula total. Migración incluye índices usuario+fecha, estado+prioridad+fecha, tipo+objetivo, eventos entidad+id+fecha, destinatario+fecha y claves únicas para cuota/duplicados/idempotencia. Fechas del modelo son Instant; cuotas y filtros convierten explícitamente America/Santiago.

A escala: paginar también los historiales/listas personales, reemplazar findAll del contexto por consultas limitadas y agrupar comparables por clave para evitar el análisis O(n²). Hoy el job usa una proyección compacta del catálogo, apropiada para el tamaño actual. Cachear agregados administrativos 30–60 segundos cuando su costo lo justifique; no cachear permisos ni estado de ejecución. Precalcular medianas por grupo si el catálogo crece. No se incluyen esas optimizaciones sin medirlas.

## Configuración de Ticket y Reporte

Los valores ya tienen estos defaults en @Value; pueden añadirse a application.yml:
~~~yaml
soporte:
  tickets-dia: 5
  reportes-dia: 10
  umbral-reportes: 3
  precio-minimo: 5
  precio-superior: 3
  precio-inferior: 0.3
  precio-job-ms: 900000
  job-inicial-ms: 60000
  reintento-ms: 60000
~~~

## Fases y comprobación manual

1. Modelo/migración: tablas nuevas con versiones, índices y auditoría. Probar reinicio con MySQL y comprobar tablas soporte_* y moderacion_reporte; no cambian filas anteriores.
2. Tickets: crear consulta como cliente, responder como admin, cerrar con versión actual. Deben aparecer conversación y avisos; otro cliente recibe 403.
3. Reportes/acciones: reportar una publicación ajena; ver prioridad urgente con tres reportantes; comprobar que sigue publicada hasta acción administrativa. Resolver una operación propia congelándola y luego reembolsando con las reglas de custodia.
4. Precio: crear suficientes comparables reales en un entorno de pruebas, ejecutar análisis, revisar mediana; marcar válida y repetir análisis sin duplicar alerta. Sin comparables debe indicar falta de datos.
5. Frontend usuario: Soporte, Mis tickets, Mis reportes y botones Reportar en producto, perfil y compras/ventas. Probar formulario, adjuntos, carga, errores y navegación con teclado.
6. Frontend administrador: filtros/paginación, asignación, contexto/historial, motivo y confirmación obligatorios; acceso desde la alerta del dashboard. Reporte no disponible por dependencia debe impedir acciones incompatibles.
7. Avisos/pruebas: simular servicio dependiente caído; reporte queda pendiente y no finalizado. Recuperar servicio y reintentar misma referencia: una acción efectiva y una notificación final. Dos administradores con igual versión: uno recibe 409.

## Validación automatizada y límites

Las pruebas del proyecto cubren transiciones, cuotas diarias y concurrencia, IDOR, roles, mass assignment, XSS, adjuntos, duplicados/autoreporte, urgencia, privacidad, recuperación e idempotencia, acciones reales de custodia con clientes remotos simulados, estadísticas y precios. Catálogo/usuario prueban adaptadores internos contra H2 y clave/rol; Gateway prueba rutas administrativas. Frontend incluye renderizado SSR de formularios/conversación y regresiones existentes, TypeScript y build Vite.

Esto no sustituye una prueba end-to-end en navegador con MySQL/XAMPP real. No se verificó visualmente la interfaz en navegador ni se ejecutó la migración en la base del usuario. El build advierte del tamaño del bundle principal existente; se recomienda dividir rutas con carga diferida en una mejora de rendimiento posterior. Avisos son in-app, no correo, push ni lectura individual. No hay restauración del editor administrativo descartado.
