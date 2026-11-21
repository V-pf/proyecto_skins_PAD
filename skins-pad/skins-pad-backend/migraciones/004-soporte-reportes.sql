-- Instalación nueva en MySQL/MariaDB. Ejecutar después de crear-bases.sql.
USE skinspad_ticket;
CREATE TABLE IF NOT EXISTS soporte_ticket (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0,
 usuario_id VARCHAR(80) NOT NULL, asunto VARCHAR(160) NOT NULL, categoria VARCHAR(30) NOT NULL,
 estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO', prioridad VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
 fecha_creacion DATETIME(6) NOT NULL,fecha_actualizacion DATETIME(6) NOT NULL,fecha_cierre DATETIME(6),
 admin_asignado_id VARCHAR(80),operacion_id BIGINT,arma_id BIGINT,
 INDEX ix_ticket_usuario_fecha(usuario_id,fecha_creacion),INDEX ix_ticket_estado_prioridad(estado,prioridad,fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_ticket_mensaje (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,ticket_id BIGINT NOT NULL,autor_id VARCHAR(80) NOT NULL,rol_autor VARCHAR(20) NOT NULL,contenido VARCHAR(4000) NOT NULL,fecha DATETIME(6) NOT NULL,
 CONSTRAINT fk_soporte_mensaje_ticket FOREIGN KEY(ticket_id) REFERENCES soporte_ticket(id),INDEX ix_mensaje_ticket(ticket_id,fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_evento (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,entidad VARCHAR(15) NOT NULL,entidad_id BIGINT NOT NULL,autor VARCHAR(80) NOT NULL,fecha DATETIME(6) NOT NULL,
 accion VARCHAR(40) NOT NULL,anterior VARCHAR(200),nuevo VARCHAR(200),motivo VARCHAR(1200),INDEX ix_soporte_evento(entidad,entidad_id,fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_notificacion (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,clave VARCHAR(160) NOT NULL UNIQUE,destinatario VARCHAR(80) NOT NULL,texto VARCHAR(200) NOT NULL,enlace VARCHAR(120) NOT NULL,fecha DATETIME(6) NOT NULL,
 INDEX ix_soporte_notificacion(destinatario,fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_adjunto (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,entidad VARCHAR(15) NOT NULL,entidad_id BIGINT NOT NULL,mensaje_id BIGINT,nombre VARCHAR(100) NOT NULL,mime VARCHAR(30) NOT NULL,datos LONGBLOB NOT NULL,INDEX ix_soporte_adjunto(entidad,entidad_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_cuota (usuario_id VARCHAR(80) PRIMARY KEY,dia DATE NOT NULL,tickets INT NOT NULL DEFAULT 0) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
USE skinspad_reporte;
CREATE TABLE IF NOT EXISTS soporte_reporte (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,version BIGINT NOT NULL DEFAULT 0,reportante_id VARCHAR(80) NOT NULL,
 tipo_objetivo VARCHAR(20) NOT NULL,objetivo_id VARCHAR(80) NOT NULL,denunciado_id VARCHAR(80) NOT NULL,
 clave_abierta VARCHAR(255) UNIQUE,motivo VARCHAR(60) NOT NULL,descripcion VARCHAR(4000) NOT NULL,evidencia_urls VARCHAR(1600),
 estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',prioridad VARCHAR(20) NOT NULL DEFAULT 'NORMAL',fecha_creacion DATETIME(6) NOT NULL,
 fecha_resolucion DATETIME(6),admin_resolutor_id VARCHAR(80),admin_asignado_id VARCHAR(80),resolucion VARCHAR(1000),
 accion_tomada VARCHAR(40),motivo_interno VARCHAR(1000),referencia_accion VARCHAR(80),estado_solicitado VARCHAR(20),accion_pendiente BIT NOT NULL DEFAULT b'0',error_accion VARCHAR(300),
 INDEX ix_reporte_usuario_fecha(reportante_id,fecha_creacion),INDEX ix_reporte_objetivo(tipo_objetivo,objetivo_id),INDEX ix_reporte_estado_prioridad(estado,prioridad,fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_evento (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,entidad VARCHAR(15) NOT NULL,entidad_id BIGINT NOT NULL,autor VARCHAR(80) NOT NULL,fecha DATETIME(6) NOT NULL,
 accion VARCHAR(40) NOT NULL,anterior VARCHAR(200),nuevo VARCHAR(200),motivo VARCHAR(1200),INDEX ix_soporte_evento(entidad,entidad_id,fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_notificacion (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,clave VARCHAR(160) NOT NULL UNIQUE,destinatario VARCHAR(80) NOT NULL,texto VARCHAR(200) NOT NULL,enlace VARCHAR(120) NOT NULL,fecha DATETIME(6) NOT NULL,
 INDEX ix_soporte_notificacion(destinatario,fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_adjunto (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,entidad VARCHAR(15) NOT NULL,entidad_id BIGINT NOT NULL,mensaje_id BIGINT,nombre VARCHAR(100) NOT NULL,mime VARCHAR(30) NOT NULL,datos LONGBLOB NOT NULL,INDEX ix_soporte_adjunto(entidad,entidad_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_cuota (usuario_id VARCHAR(80) PRIMARY KEY,dia DATE NOT NULL,reportes INT NOT NULL DEFAULT 0) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_alerta_precio (
 producto_id BIGINT PRIMARY KEY,version BIGINT NOT NULL DEFAULT 0,huella VARCHAR(64) NOT NULL,huella_validada VARCHAR(64),estado VARCHAR(30) NOT NULL,nombre VARCHAR(200),
 precio DECIMAL(38,2),mediana DECIMAL(38,2),comparables INT NOT NULL DEFAULT 0,explicacion VARCHAR(250),admin_id VARCHAR(80),motivo_revision VARCHAR(1000),fecha_revision DATETIME(6),actualizada DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS soporte_objetivo (clave VARCHAR(120) PRIMARY KEY) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
USE skinspad_catalogo;
CREATE TABLE IF NOT EXISTS moderacion_reporte (referencia VARCHAR(80) PRIMARY KEY,objetivo VARCHAR(80) NOT NULL,admin_id VARCHAR(80) NOT NULL,motivo VARCHAR(1000) NOT NULL,accion VARCHAR(40) NOT NULL,fecha DATETIME(6) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
USE skinspad_usuario;
CREATE TABLE IF NOT EXISTS moderacion_reporte (referencia VARCHAR(80) PRIMARY KEY,objetivo VARCHAR(80) NOT NULL,admin_id VARCHAR(80) NOT NULL,motivo VARCHAR(1000) NOT NULL,accion VARCHAR(40) NOT NULL,fecha DATETIME(6) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
