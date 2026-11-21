-- MariaDB: aplicar con pago detenido, después de la migración de custodia.
USE skinspad_pago;
ALTER TABLE operaciones ADD COLUMN IF NOT EXISTS producto_ocupado BIGINT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_operacion_producto_ocupado ON operaciones(producto_ocupado);
-- Los reembolsos y las cancelaciones permiten volver a comprar la publicación.
UPDATE operaciones o JOIN pagos p ON p.id = o.pago_id
SET o.producto_ocupado = o.producto_id
WHERE p.custodia = 1 AND o.historica = 0 AND o.estado NOT IN ('CANCELADA','REEMBOLSADA');
