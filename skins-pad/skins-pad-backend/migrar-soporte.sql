-- Ejecutar después de crear-bases.sql y antes de iniciar los servicios nuevos.
-- Usar una conexión con permisos de lectura en Pago y escritura en ambos destinos.
USE skinspad_ticket;
CREATE TABLE IF NOT EXISTS skinspad_ticket.soporte_ticket LIKE skinspad_pago.soporte_ticket;
CREATE TABLE IF NOT EXISTS skinspad_ticket.soporte_ticket_mensaje LIKE skinspad_pago.soporte_ticket_mensaje;
CREATE TABLE IF NOT EXISTS skinspad_ticket.soporte_cuota (usuario_id VARCHAR(80) PRIMARY KEY, dia DATE NOT NULL, tickets INT NOT NULL DEFAULT 0) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS skinspad_ticket.soporte_adjunto LIKE skinspad_pago.soporte_adjunto;
CREATE TABLE IF NOT EXISTS skinspad_ticket.soporte_evento LIKE skinspad_pago.soporte_evento;
CREATE TABLE IF NOT EXISTS skinspad_ticket.soporte_notificacion LIKE skinspad_pago.soporte_notificacion;
CREATE TABLE IF NOT EXISTS skinspad_reporte.soporte_reporte LIKE skinspad_pago.soporte_reporte;
CREATE TABLE IF NOT EXISTS skinspad_reporte.soporte_objetivo LIKE skinspad_pago.soporte_objetivo;
CREATE TABLE IF NOT EXISTS skinspad_reporte.soporte_alerta_precio LIKE skinspad_pago.soporte_alerta_precio;
CREATE TABLE IF NOT EXISTS skinspad_reporte.soporte_cuota (usuario_id VARCHAR(80) PRIMARY KEY, dia DATE NOT NULL, reportes INT NOT NULL DEFAULT 0) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS skinspad_reporte.soporte_adjunto LIKE skinspad_pago.soporte_adjunto;
CREATE TABLE IF NOT EXISTS skinspad_reporte.soporte_evento LIKE skinspad_pago.soporte_evento;
CREATE TABLE IF NOT EXISTS skinspad_reporte.soporte_notificacion LIKE skinspad_pago.soporte_notificacion;
-- CREATE TABLE LIKE no copia las claves foráneas.
SET @fk_mensaje = IF(EXISTS (
 SELECT 1 FROM information_schema.KEY_COLUMN_USAGE
 WHERE TABLE_SCHEMA='skinspad_ticket' AND TABLE_NAME='soporte_ticket_mensaje'
 AND COLUMN_NAME='ticket_id' AND REFERENCED_TABLE_NAME='soporte_ticket'
), 'SELECT 1', 'ALTER TABLE skinspad_ticket.soporte_ticket_mensaje ADD CONSTRAINT fk_soporte_mensaje_ticket FOREIGN KEY (ticket_id) REFERENCES skinspad_ticket.soporte_ticket(id)');
PREPARE fk_stmt FROM @fk_mensaje;
EXECUTE fk_stmt;
DEALLOCATE PREPARE fk_stmt;
DROP PROCEDURE IF EXISTS migrar_soporte_v1;
DELIMITER //
CREATE PROCEDURE migrar_soporte_v1()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 INSERT INTO skinspad_ticket.soporte_ticket (id, version, usuario_id, asunto, categoria, estado, prioridad, fecha_creacion, fecha_actualizacion, fecha_cierre, admin_asignado_id, operacion_id, arma_id) SELECT id, version, usuario_id, asunto, categoria, estado, prioridad, fecha_creacion, fecha_actualizacion, fecha_cierre, admin_asignado_id, operacion_id, arma_id FROM skinspad_pago.soporte_ticket;
 INSERT INTO skinspad_ticket.soporte_ticket_mensaje (id, ticket_id, autor_id, rol_autor, contenido, fecha) SELECT id, ticket_id, autor_id, rol_autor, contenido, fecha FROM skinspad_pago.soporte_ticket_mensaje;
 INSERT INTO skinspad_ticket.soporte_cuota (usuario_id,dia,tickets) SELECT usuario_id,dia,tickets FROM skinspad_pago.soporte_cuota;
 INSERT INTO skinspad_ticket.soporte_adjunto (id, entidad, entidad_id, mensaje_id, nombre, mime, datos) SELECT id, entidad, entidad_id, mensaje_id, nombre, mime, datos FROM skinspad_pago.soporte_adjunto WHERE entidad='TICKET';
 INSERT INTO skinspad_ticket.soporte_evento (id, entidad, entidad_id, autor, fecha, accion, anterior, nuevo, motivo) SELECT id, entidad, entidad_id, autor, fecha, accion, anterior, nuevo, motivo FROM skinspad_pago.soporte_evento WHERE entidad='TICKET';
 INSERT INTO skinspad_ticket.soporte_notificacion (id, clave, destinatario, texto, enlace, fecha) SELECT id, clave, destinatario, texto, enlace, fecha FROM skinspad_pago.soporte_notificacion WHERE enlace LIKE '%/tickets/%';
 INSERT INTO skinspad_reporte.soporte_reporte (id, version, reportante_id, tipo_objetivo, objetivo_id, denunciado_id, clave_abierta, motivo, descripcion, evidencia_urls, estado, prioridad, fecha_creacion, fecha_resolucion, admin_resolutor_id, admin_asignado_id, resolucion, accion_tomada, motivo_interno, referencia_accion, estado_solicitado, accion_pendiente, error_accion) SELECT id, version, reportante_id, tipo_objetivo, objetivo_id, denunciado_id, clave_abierta, motivo, descripcion, evidencia_urls, estado, prioridad, fecha_creacion, fecha_resolucion, admin_resolutor_id, admin_asignado_id, resolucion, accion_tomada, motivo_interno, referencia_accion, estado_solicitado, accion_pendiente, error_accion FROM skinspad_pago.soporte_reporte;
 INSERT INTO skinspad_reporte.soporte_objetivo (clave) SELECT clave FROM skinspad_pago.soporte_objetivo;
 INSERT INTO skinspad_reporte.soporte_alerta_precio (producto_id, version, huella, huella_validada, estado, nombre, precio, mediana, comparables, explicacion, admin_id, motivo_revision, fecha_revision, actualizada) SELECT producto_id, version, huella, huella_validada, estado, nombre, precio, mediana, comparables, explicacion, admin_id, motivo_revision, fecha_revision, actualizada FROM skinspad_pago.soporte_alerta_precio;
 INSERT INTO skinspad_reporte.soporte_cuota (usuario_id,dia,reportes) SELECT usuario_id,dia,reportes FROM skinspad_pago.soporte_cuota;
 INSERT INTO skinspad_reporte.soporte_adjunto (id, entidad, entidad_id, mensaje_id, nombre, mime, datos) SELECT id, entidad, entidad_id, mensaje_id, nombre, mime, datos FROM skinspad_pago.soporte_adjunto WHERE entidad='REPORTE';
 INSERT INTO skinspad_reporte.soporte_evento (id, entidad, entidad_id, autor, fecha, accion, anterior, nuevo, motivo) SELECT id, entidad, entidad_id, autor, fecha, accion, anterior, nuevo, motivo FROM skinspad_pago.soporte_evento WHERE entidad IN ('REPORTE','PRECIO');
 INSERT INTO skinspad_reporte.soporte_notificacion (id, clave, destinatario, texto, enlace, fecha) SELECT id, clave, destinatario, texto, enlace, fecha FROM skinspad_pago.soporte_notificacion WHERE enlace NOT LIKE '%/tickets/%';
 COMMIT;
END//
DELIMITER ;
CALL migrar_soporte_v1();
DROP PROCEDURE migrar_soporte_v1;
