import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

// Todas las transiciones pasan por el gateway y los servicios normales.
// No borra tablas ni altera registros ajenos a los ejemplos identificados.
export async function cargarDemo({ base = 'http://localhost:8090/api', passwordAdmin = 'Admin1234!' } = {}) {
  const url = new URL(base);
  if (!['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)) throw Error('La carga demo solo admite un servidor local.');
  const tokens = [];
  async function api(ruta, metodo = 'GET', body, token) {
    const r = await fetch(base + ruta, { method: metodo, signal: AbortSignal.timeout(20000),
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      body: body === undefined ? undefined : JSON.stringify(body) });
    const texto = await r.text();
    if (!r.ok) throw Error(`${metodo} ${ruta}: ${r.status} ${texto}`);
    return texto ? JSON.parse(texto) : undefined;
  }
  async function login(nombre, password = 'Demo1234!') {
    const sesion = await api('/auth/login', 'POST', { identificador: nombre, password });
    tokens.push(sesion.token);
    return sesion;
  }
  try {
    const admin = await login('admin', passwordAdmin);
    const cuentas = await api('/usuarios', 'GET', undefined, admin.token);
    async function cuenta(username, verificado = false, suspendido = false) {
      let u = cuentas.find(u => u.username === username);
      if (u) return u;
      u = await api('/auth/registro', 'POST', { nombre: `Cuenta demo ${username}`, username,
        email: `${username}@skinspad.example`, password: 'Demo1234!', fechaNacimiento: '1998-01-01' });
      if (verificado || suspendido) u = await api(`/usuarios/${u.id}/estado`, 'PATCH', {
        verificado, suspendido, motivo: 'Estado inicial del escenario de demostración',
      }, admin.token);
      cuentas.push(u);
      return u;
    }
    const vendedor = await cuenta('vendedor_demo', true);
    const comprador = await cuenta('comprador_demo');
    await cuenta('sin_verificar_demo');
    await cuenta('suspendido_demo', false, true);
    await cuenta('verificado_demo', true);
    if (!vendedor.verificado || vendedor.suspendido) throw Error('vendedor_demo fue moderado. Reactívalo/verifícalo desde Gestión web para crear nuevos ejemplos.');
    const venta = await login(vendedor.username);
    const compra = await login(comprador.username);
    const categorias = await api('/categorias');
    const cats = {};
    for (const [tipo, nombre] of [['rifle', 'Rifles'], ['pistola', 'Pistolas'], ['cuchillo', 'Cuchillos']]) {
      const categoria = categorias.find(c => c.nombre === nombre && c.estadoHabilitada);
      if (!categoria) throw Error('Debe existir la categoría habilitada ' + nombre + '.');
      cats[tipo] = categoria;
    }
    const productos = await api('/productos');
    async function producto(clave, tipo, precio, estado = 'ACTIVO') {
      const nombre = `[DEMO] ${clave}`;
      let p = productos.find(p => p.nombre === nombre && p.vendedorId === vendedor.id);
      if (p) return p;
      const datos = { vendedorId: vendedor.id, vendedor: vendedor.username, categoriaId: cats[tipo].id,
        nombre, tipo, grado: 'clasificado', desgaste: 'mw', precio, stattrak: false, floatValor: 0.12,
        descripcion: 'Publicación de prueba. Usa esta skin para ensayar el flujo indicado en su nombre.',
        estado: 'PENDIENTE_APROBACION' };
      p = await api('/productos', 'POST', datos, venta.token);
      if (estado !== 'PENDIENTE_APROBACION') p = await api(`/productos/${p.id}`, 'PUT', {
        ...datos, estado, motivoRechazo: estado === 'RECHAZADO' ? 'Demo: imagen y descripción requieren corrección.' : null,
      }, admin.token);
      if (clave.includes('galería')) {
        const png = await readFile(new URL('../skins-pad-react/public/demo/imagen-prueba.png', import.meta.url));
        await api(`/productos/${p.id}/imagenes`, 'POST', { urlImagen: 'data:image/png;base64,' + png.toString('base64'), esPrincipal: true }, venta.token);
      }
      productos.push(p);
      return p;
    }
    await producto('AK-47 · pendiente de aprobación', 'rifle', 25000, 'PENDIENTE_APROBACION');
    await producto('USP-S · pendiente de aprobación', 'pistola', 8500, 'PENDIENTE_APROBACION');
    await producto('P250 · rechazada con motivo', 'pistola', 1500, 'RECHAZADO');
    const retirada = await producto('AK-47 · retirada', 'rifle', 10000);
    if (retirada.estado === 'ACTIVO' && !retirada.motivoRechazo) {
      await api(`/productos/${retirada.id}?motivo=${encodeURIComponent('Demo: retirada por el vendedor')}`, 'DELETE', undefined, venta.token);
    }
    const carritoSkin = await producto('USP-S · carrito y compra', 'pistola', 2000);
    await producto('AK-47 · editar precio y retirar', 'rifle', 15000);
    await producto('Cuchillo · imágenes, galería y perfil público', 'cuchillo', 85000);

    const pagos = await api('/pagos', 'GET', undefined, admin.token);
    async function pago(clave, tipo, precio, metodo, destino, comprobante = false) {
      const solicitudId = `demo-v1-${clave}`;
      let p = pagos.find(p => p.pedidoId === `COMPRA-${solicitudId}`);
      // Una segunda ejecución conserva aprobaciones, rechazos y reembolsos hechos por el usuario.
      if (p) return p;
      const skin = await producto(`Compra · ${clave}`, tipo, precio);
      p = await api('/pagos/checkout', 'POST', { solicitudId, productoIds: [skin.id], metodo }, compra.token);
      if (comprobante) {
        await api(`/pagos/${p.id}/comprobante`, 'POST', { banco: 'Banco de prueba (simulado)',
          numeroOperacion: `DEMO-${clave}`, archivoUrl: 'http://localhost:5173/demo/comprobante.html' }, compra.token);
      }
      if (destino === 'APROBADO' || destino === 'REEMBOLSADO') {
        if (metodo === 'TRANSFERENCIA') await api(`/pagos/${p.id}/comprobante/verificar`, 'POST', {}, admin.token);
        p = await api(`/pagos/${p.id}/confirmar`, 'POST', undefined, admin.token);
      }
      if (destino === 'RECHAZADO') p = await api(`/pagos/${p.id}/rechazar`, 'POST', { motivo: 'Demo: comprobante no corresponde al monto de la compra.' }, admin.token);
      if (destino === 'REEMBOLSADO') p = await api(`/pagos/${p.id}/reembolsar`, 'POST', undefined, admin.token);
      pagos.push(p);
      return p;
    }
    await pago('transferencia-por-revisar', 'rifle', 12000, 'TRANSFERENCIA', 'PENDIENTE', true);
    await pago('transferencia-sin-comprobante', 'pistola', 3500, 'TRANSFERENCIA', 'PENDIENTE');
    await pago('tarjeta-pendiente', 'cuchillo', 65000, 'TARJETA', 'PENDIENTE');
    await pago('mercadopago-pendiente', 'pistola', 6500, 'MERCADO_PAGO', 'PENDIENTE');
    await pago('venta-rifle', 'rifle', 30000, 'TRANSFERENCIA', 'APROBADO', true);
    await pago('venta-pistola', 'pistola', 18000, 'TARJETA', 'APROBADO');
    await pago('venta-cuchillo', 'cuchillo', 90000, 'MERCADO_PAGO', 'APROBADO');
    await pago('pago-rechazado', 'pistola', 4500, 'TRANSFERENCIA', 'RECHAZADO', true);
    await pago('pago-reembolsado', 'rifle', 22000, 'TARJETA', 'REEMBOLSADO');

    // Una venta al comprador demo le da saldo real dentro de la simulación,
    // sin usar un endpoint que permita cargar dinero desde el frontend.
    const refSaldo = 'demo-v1-saldo-comprador';
    if (!pagos.some(p => p.pedidoId === `COMPRA-${refSaldo}`)) {
      // La cuenta dedicada permite ensayar compras y ventas con el mismo rol cliente.
      if (!comprador.verificado) await api(`/usuarios/${comprador.id}/estado`, 'PATCH', { verificado: true }, admin.token);
      let skin = productos.find(p => p.nombre === '[DEMO] Venta para saldo del comprador' && p.vendedorId === comprador.id);
      const datos = { vendedorId: comprador.id, vendedor: comprador.username, categoriaId: cats.rifle.id,
        nombre: '[DEMO] Venta para saldo del comprador', tipo: 'rifle', grado: 'clasificado', desgaste: 'mw',
        precio: 100000, floatValor: 0.12, stattrak: false, estado: 'PENDIENTE_APROBACION' };
      if (!skin) {
        skin = await api('/productos', 'POST', datos, compra.token);
        skin = await api(`/productos/${skin.id}`, 'PUT', { ...datos, estado: 'ACTIVO' }, admin.token);
      }
      const p = await api('/pagos/checkout', 'POST', { solicitudId: refSaldo, productoIds: [skin.id], metodo: 'TARJETA' }, venta.token);
      await api(`/pagos/${p.id}/confirmar`, 'POST', undefined, admin.token);
    }
    await pago('compra-con-saldo', 'pistola', 1000, 'SALDO', 'APROBADO');
    const retiros = await api(`/usuarios/${vendedor.id}/retiros`, 'GET', undefined, venta.token);
    const referencia = 'de000000-0000-4000-8000-000000000001';
    if (!retiros.some(r => r.referencia === `RETIRO-${referencia}`)) {
      await api(`/usuarios/${vendedor.id}/retiros`, 'POST', { monto: 5000, referencia }, venta.token);
    }
    const carrito = await api(`/carritos/usuario/${comprador.id}`, 'GET', undefined, compra.token);
    if (!carrito.items.some(i => i.productoId === carritoSkin.id) && carritoSkin.estado === 'ACTIVO') {
      await api(`/carritos/${carrito.id}/items`, 'POST', { productoId: carritoSkin.id }, compra.token);
    }
    const resultado = { usuariosDemo: (await api('/usuarios', 'GET', undefined, admin.token)).filter(u => u.username.endsWith('_demo')).length,
      publicacionesDemo: (await api('/productos')).filter(p => p.nombre.startsWith('[DEMO]')).length,
      pagosDemo: (await api('/pagos', 'GET', undefined, admin.token)).filter(p => p.pedidoId?.startsWith('COMPRA-demo-v1-')).length };
    console.log('Datos de demostración disponibles:', resultado);
    return resultado;
  } finally {
    for (const token of tokens) await api('/auth/logout', 'POST', undefined, token).catch(() => {});
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  cargarDemo({ base: process.env.DEMO_API_URL || 'http://localhost:8090/api', passwordAdmin: process.env.DEMO_ADMIN_PASSWORD || 'Admin1234!' })
    .catch(error => { console.error(error.message); process.exitCode = 1; });
}
