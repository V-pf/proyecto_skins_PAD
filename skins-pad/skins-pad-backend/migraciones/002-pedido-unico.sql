-- Comprueba duplicados antes de aplicar el índice en una base existente.
SELECT pedido_id, COUNT(*) AS cantidad
FROM pagos
GROUP BY pedido_id
HAVING COUNT(*) > 1;

ALTER TABLE pagos
    ADD CONSTRAINT uk_pagos_pedido_id UNIQUE (pedido_id);
