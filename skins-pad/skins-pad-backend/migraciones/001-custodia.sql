-- MariaDB / XAMPP. Respaldar las 4 bases y detener servicios antes de ejecutar.
-- Ejecutar primero 000-custodia-schema.sql. No arrancar los servicios entre ambos scripts.
-- No ejecutar el checkout nuevo hasta finalizar la migración.
USE skinspad_pago;
-- Preflight: deben devolver cero filas. Resolver duplicados antes del primer arranque.
SELECT pedido_id, COUNT(*) FROM pagos GROUP BY pedido_id HAVING COUNT(*)>1;
-- Los pagos históricos sin precio/vendedor se conservan congelados y de solo lectura.
START TRANSACTION;
INSERT INTO operaciones (version,pago_id,producto_id,comprador_id,vendedor_id,nombre,precio,comision,comision_bps,paga_comision,estado,retenido,historica,sincronizado,creada,actualizada,completada,causa)
SELECT 0,p.id,i.producto_ids,p.usuario_id,COALESCE(a.vendedor_id,'LEGACY_DESCONOCIDO'),COALESCE(a.nombre,CONCAT('Skin #',i.producto_ids)),COALESCE(ROUND(pr.precio_venta),0),
CASE WHEN totals.base>0 THEN FLOOR(p.monto_total*cum.base/totals.base)-FLOOR(p.monto_total*(cum.base-COALESCE(pr.precio_venta,0))/totals.base)-COALESCE(pr.precio_venta,0) ELSE 0 END,
500,'COMPRADOR',CASE WHEN p.estado='APROBADO' AND p.liquidado=1 THEN 'COMPLETADA' WHEN p.estado='REEMBOLSADO' THEN 'REEMBOLSADA' WHEN p.estado='RECHAZADO' THEN 'CANCELADA' ELSE 'CONGELADA' END,
0,1,1,p.fecha,p.fecha,CASE WHEN p.estado='APROBADO' AND p.liquidado=1 THEN p.fecha ELSE NULL END,
CASE WHEN p.estado='PENDIENTE' THEN 'LEGACY_CONCILIAR_PAGO' ELSE 'MIGRACION_HISTORICA_FECHA_APROXIMADA' END
FROM pagos p JOIN pago_producto_ids i ON i.pago_id=p.id
LEFT JOIN pago_precios pr ON pr.pago_id=p.id AND pr.producto_id=i.producto_ids
LEFT JOIN skinspad_catalogo.productos a ON a.id=i.producto_ids
LEFT JOIN (SELECT pago_id,SUM(precio_venta) base FROM pago_precios GROUP BY pago_id) totals ON totals.pago_id=p.id
LEFT JOIN (SELECT x.pago_id,x.producto_id,SUM(y.precio_venta) base FROM pago_precios x JOIN pago_precios y ON x.pago_id=y.pago_id AND y.producto_id<=x.producto_id GROUP BY x.pago_id,x.producto_id) cum ON cum.pago_id=p.id AND cum.producto_id=i.producto_ids
WHERE (p.custodia IS NULL OR p.custodia=0) AND NOT EXISTS(SELECT 1 FROM operaciones o WHERE o.pago_id=p.id AND o.producto_id=i.producto_ids);
INSERT INTO operacion_eventos(operacion_id,actor,accion,estado,motivo,fecha)
SELECT o.id,'MIGRACION','IMPORTAR',o.estado,'Histórico sin nuevos movimientos. Fecha original aproximada; no es fecha de recepción.',o.creada FROM operaciones o
WHERE o.historica=1 AND NOT EXISTS(SELECT 1 FROM operacion_eventos e WHERE e.operacion_id=o.id);
-- Solo pendientes íntegros se incorporan al nuevo flujo. Mantiene la referencia original
-- para que un débito de saldo antiguo ya efectuado no se repita.
UPDATE operaciones o JOIN pagos p ON p.id=o.pago_id
SET o.historica=0,o.estado='PAGO_PENDIENTE',o.causa=NULL,o.vence=DATE_ADD(UTC_TIMESTAMP(),INTERVAL 24 HOUR)
WHERE p.estado='PENDIENTE' AND COALESCE(p.custodia,0)=0
AND NOT EXISTS(SELECT 1 FROM (SELECT pago_id FROM operaciones WHERE precio<=0 OR vendedor_id='LEGACY_DESCONOCIDO') bad WHERE bad.pago_id=p.id);
UPDATE pagos p SET p.custodia=1,p.etapa_custodia='RESERVAR',p.liquidado=0
WHERE p.estado='PENDIENTE' AND EXISTS(SELECT 1 FROM operaciones o WHERE o.pago_id=p.id AND o.historica=0);
COMMIT;
-- Los pendientes anteriores se concilian antes de habilitar nuevas compras de esas skins.
-- No se vuelve a debitar ni a abonar automáticamente ningún pago anterior.
SELECT id,pedido_id,estado,liquidado FROM pagos WHERE COALESCE(custodia,0)=0 AND (estado='PENDIENTE' OR (estado='APROBADO' AND COALESCE(liquidado,0)=0));
