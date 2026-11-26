-- XAMPP MariaDB. Servicios detenidos. Ejecutar antes de 001-custodia.sql.
USE skinspad_pago;
ALTER TABLE pagos ADD COLUMN IF NOT EXISTS custodia BIT NULL,
 ADD COLUMN IF NOT EXISTS etapa_custodia VARCHAR(255) NULL,
 ADD COLUMN IF NOT EXISTS error_custodia VARCHAR(500) NULL;
-- Falla si existen referencias duplicadas: conciliar antes de continuar.
CREATE UNIQUE INDEX IF NOT EXISTS uk_pago_pedido ON pagos(pedido_id);
CREATE TABLE IF NOT EXISTS operaciones (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,version BIGINT NOT NULL DEFAULT 0,
 pago_id BIGINT NOT NULL,producto_id BIGINT NOT NULL,
 comprador_id VARCHAR(80) NOT NULL,vendedor_id VARCHAR(80) NOT NULL,
 nombre VARCHAR(255) NOT NULL,precio BIGINT NOT NULL,
 comision BIGINT NOT NULL,comision_bps INT NOT NULL,paga_comision VARCHAR(255) NOT NULL,
 estado VARCHAR(40) NOT NULL,causa VARCHAR(255),retenido BIT NOT NULL,historica BIT NOT NULL,
 sincronizado BIT NOT NULL,error_sincronizacion VARCHAR(500),
 creada DATETIME(6) NOT NULL,actualizada DATETIME(6),vence DATETIME(6),completada DATETIME(6),
 accion_pendiente VARCHAR(40),actor_pendiente VARCHAR(80),motivo_pendiente VARCHAR(1000),
 UNIQUE KEY uk_operacion_linea(pago_id,producto_id),INDEX ix_operacion_vendedor(vendedor_id,estado),
 INDEX ix_operacion_comprador(comprador_id,estado),INDEX ix_operacion_plazo(estado,vence),INDEX ix_operacion_fin(completada)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS operacion_eventos (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,operacion_id BIGINT NOT NULL,
 actor VARCHAR(80) NOT NULL,accion VARCHAR(40) NOT NULL,estado VARCHAR(40) NOT NULL,
 motivo VARCHAR(1000),fecha DATETIME(6) NOT NULL,INDEX ix_eventos_operacion(operacion_id,id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS custodia_asientos (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,operacion_id BIGINT NOT NULL,tipo VARCHAR(30) NOT NULL,
 monto BIGINT NOT NULL,actor VARCHAR(80) NOT NULL,fecha DATETIME(6) NOT NULL,
 UNIQUE KEY uk_asiento_operacion_tipo(operacion_id,tipo)
) ENGINE=InnoDB;
