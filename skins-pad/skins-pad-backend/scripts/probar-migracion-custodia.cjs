const fs=require('node:fs'),path=require('node:path'),{spawnSync}=require('node:child_process'),assert=require('node:assert/strict');
const stamp=Date.now(),pago='skinspad_escrow_test_pago_'+stamp,catalogo='skinspad_escrow_test_catalogo_'+stamp;
function sql(input){const r=spawnSync('C:/xampp/mysql/bin/mysql.exe',['-uroot','--batch','--skip-column-names'],{input,encoding:'utf8'});if(r.status!==0)throw new Error(r.stderr);return r.stdout.trim();}
const root=path.resolve(__dirname,'..');
let created=false;
try{
 sql('CREATE DATABASE '+pago+'; CREATE DATABASE '+catalogo+';');created=true;
 sql('USE '+pago+'; CREATE TABLE pagos(id BIGINT PRIMARY KEY,pedido_id VARCHAR(80),usuario_id VARCHAR(80),monto_total DECIMAL(15,2),estado VARCHAR(30),liquidado BIT,fecha DATETIME); CREATE TABLE pago_producto_ids(pago_id BIGINT,producto_ids BIGINT); CREATE TABLE pago_precios(pago_id BIGINT,producto_id BIGINT,precio_venta DECIMAL(15,2)); CREATE TABLE '+catalogo+'.productos(id BIGINT PRIMARY KEY,vendedor_id VARCHAR(80),nombre VARCHAR(160)); INSERT INTO '+catalogo+'.productos VALUES(1,"seller","AK-47"),(2,"seller2","AWP"),(3,"seller","Glock"),(4,"seller","USP"); INSERT INTO pagos VALUES(10,"COMPRA-10","buyer",3150,"APROBADO",1,"2026-11-14 12:00:00"),(20,"COMPRA-20","buyer",525,"PENDIENTE",0,"2026-11-14 12:00:00"),(30,"COMPRA-30","buyer",105,"APROBADO",0,"2026-11-14 12:00:00"); INSERT INTO pago_producto_ids VALUES(10,1),(10,2),(20,3),(30,4); INSERT INTO pago_precios VALUES(10,1,1000),(10,2,2000),(20,3,500),(30,4,100);');
 for(let run=0;run<2;run++)for(const file of ['000-custodia-schema.sql','001-custodia.sql'])sql(fs.readFileSync(path.join(root,'migraciones',file),'utf8').replaceAll('skinspad_pago',pago).replaceAll('skinspad_catalogo',catalogo));
 assert.equal(sql('SELECT COUNT(*) FROM '+pago+'.operaciones'),'4');
 assert.equal(sql('SELECT SUM(precio+comision) FROM '+pago+'.operaciones WHERE pago_id=10'),'3150');
 assert.equal(sql('SELECT COUNT(*) FROM '+pago+'.operaciones WHERE estado="COMPLETADA" AND historica=1'),'2');
 assert.equal(sql('SELECT COUNT(*) FROM '+pago+'.operaciones WHERE estado="PAGO_PENDIENTE" AND historica=0'),'1');
 assert.equal(sql('SELECT COUNT(*) FROM '+pago+'.custodia_asientos'),'0');
 assert.equal(sql('SELECT COUNT(*) FROM '+pago+'.pagos WHERE id=20 AND custodia=1 AND etapa_custodia="RESERVAR"'),'1');
 console.log('Migración MariaDB: importación, pendientes, conservación de montos y repetición sin duplicados correctas.');
}finally{if(created){if(!/^skinspad_escrow_test_pago_[0-9]+$/.test(pago)||!/^skinspad_escrow_test_catalogo_[0-9]+$/.test(catalogo))throw new Error('Nombre de prueba inválido');sql('DROP DATABASE '+pago+'; DROP DATABASE '+catalogo+';');}}
