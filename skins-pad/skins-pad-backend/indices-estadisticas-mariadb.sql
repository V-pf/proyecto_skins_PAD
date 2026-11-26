-- Opcional. MariaDB 10.4 (XAMPP). No modifica datos de negocio.
-- Revisar SHOW INDEX y EXPLAIN antes de ejecutar en una base grande.
-- Las FKs pueden haber creado ya índices de categoria_id / vendedor_id.
CREATE INDEX IF NOT EXISTS idx_stats_pagos_fecha_estado ON skinspad_pago.pagos(fecha,estado);
CREATE INDEX IF NOT EXISTS idx_stats_pagos_estado_fecha ON skinspad_pago.pagos(estado,fecha);
CREATE INDEX IF NOT EXISTS idx_stats_pagos_metodo_fecha ON skinspad_pago.pagos(metodo,fecha);
CREATE INDEX IF NOT EXISTS idx_stats_productos_categoria_fecha ON skinspad_catalogo.productos(categoria_id,fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_stats_productos_vendedor_estado ON skinspad_catalogo.productos(vendedor_id,estado);
CREATE INDEX IF NOT EXISTS idx_stats_productos_fecha ON skinspad_catalogo.productos(fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_stats_lineas_producto_pago ON skinspad_pago.operaciones(producto_id,pago_id);
