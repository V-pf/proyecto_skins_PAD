const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

// Ejecuta el hook y servicio reales; sustituye React y HTTP para observar
// el endpoint solicitado sin servidor ni una sesión administrativa real.
function escenario(ruta, respuesta = []) {
  const solicitudes = [];
  function cargar(archivo) {
    const exports = {};
    const codigo = ts.transpileModule(fs.readFileSync(archivo, 'utf8'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
    }).outputText;
    vm.runInNewContext(codigo, {
      exports,
      require(nombre) {
        if (nombre === 'react') return { useCallback: fn => fn };
        if (nombre === 'react-router-dom') return { useMatch: patron => patron === ruta, useLocation: () => ({pathname:ruta}) };
        if (nombre === './useCarga') return { useCarga: (consulta, inicial, activo) => ({ consulta, activo }) };
        if (nombre === './http') return { solicitar: async (url, metodo, datos, signal) => {
          solicitudes.push({ url, metodo, signal }); return respuesta;
        }};
        return cargar(path.resolve(path.dirname(archivo), nombre + '.ts'));
      },
    }, { filename: archivo });
    return exports;
  }
  return { solicitudes, usePagos: cargar(path.resolve(__dirname, '../src/hooks/usePagos.ts')).usePagos };
}

test('el panel administrativo recibe pagos de otros usuarios pendientes de aprobación', async () => {
  const pendiente = { id: 23, usuarioId: 'comprador', estado: 'PENDIENTE' };
  const { usePagos, solicitudes } = escenario('/admin/panel', [pendiente]);
  const carga = usePagos({ id: 'administrador', rol: 'admin' });
  const signal = new AbortController().signal;
  const pagos = await carga.consulta(signal);
  assert.equal(solicitudes[0].url, '/pagos');
  assert.equal(solicitudes[0].signal, signal);
  assert.equal(pagos[0], pendiente);
  assert.equal(carga.activo, true);
});

test('el alcance administrativo no depende de rutas antiguas ni de anclas', async () => {
  for (const ruta of ['/admin/panel', '/admin/panel#pagos', '/admin/ventas', '/admin/pagos']) {
    const { usePagos, solicitudes } = escenario(ruta);
    await usePagos({ id: 'administrador', rol: 'admin' }).consulta();
    assert.equal(solicitudes[0].url, '/pagos', ruta);
  }
});

test('los clientes consultan solamente sus propios pagos', async () => {
  const { usePagos, solicitudes } = escenario('/cliente/pagos');
  await usePagos({ id: 'cliente-1', rol: 'cliente' }).consulta();
  assert.equal(solicitudes[0].url, '/pagos/usuario/cliente-1');
});

test('sin sesión no se solicitan pagos', async () => {
  const { usePagos, solicitudes } = escenario('/login');
  const carga = usePagos(null);
  await carga.consulta();
  assert.equal(carga.activo, false);
  assert.equal(solicitudes.length, 0);
});

test('estadísticas usa el agregado y no solicita el listado crudo de pagos', () => {
  const {usePagos}=escenario('/admin/estadisticas');
  assert.equal(usePagos({id:'admin',rol:'admin'}).activo,false);
});
