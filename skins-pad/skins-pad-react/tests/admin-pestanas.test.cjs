const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const React = require('react');
const { renderToStaticMarkup } = require('react-dom/server');

function panel(seccion) {
  const exports = {};
  const fuente = fs.readFileSync(path.resolve(__dirname, '../src/pages/AdminPanel.tsx'), 'utf8');
  vm.runInNewContext(ts.transpileModule(fuente, { compilerOptions: {
    jsx: ts.JsxEmit.ReactJSX, module: ts.ModuleKind.CommonJS,
  } }).outputText, { exports, require(nombre) {
    if (nombre === 'react' || nombre === 'react/jsx-runtime') return require(nombre);
    if (nombre === 'react-router-dom') return { NavLink: ({ to, children }) => React.createElement('a', { href: to }, children) };
    if (nombre.includes('TiendaContext')) return { useTienda: () => ({ pagos: [], cargando: {}, recargar: async () => {} }) };
    return { default: () => React.createElement('div', { 'data-modulo': nombre.slice(2) }) };
  }});
  return renderToStaticMarkup(React.createElement(exports.default, { seccion }));
}

test('Estadísticas es la pestaña inicial y no monta las gestiones', () => {
  const html = panel(undefined);
  assert.match(html, /<h1[^>]*>Estadísticas<\/h1>/);
  assert.match(html, /data-modulo="AdminEstadisticas"/);
  assert.doesNotMatch(html, /data-modulo="(?:Pagos|AdminUsuarios|AdminPublicaciones|AdminCategorias)"/);
});
test('Gestor de Pagos monta exclusivamente la gestión de pagos', () => {
  const html = panel('pagos');
  assert.match(html, /<h1[^>]*>Gestor de Pagos<\/h1>/);
  assert.match(html, /data-modulo="Pagos"/);
  assert.doesNotMatch(html, /data-modulo="(?:AdminEstadisticas|AdminUsuarios|AdminPublicaciones|AdminCategorias)"/);
});
test('Gestión web respeta el orden usuarios, publicaciones y categorías', () => {
  const html = panel('web');
  assert.match(html, /<h1[^>]*>Gestión web<\/h1>/);
  assert.ok(html.indexOf('data-modulo="AdminUsuarios"') < html.indexOf('data-modulo="AdminPublicaciones"'));
  assert.ok(html.indexOf('data-modulo="AdminPublicaciones"') < html.indexOf('data-modulo="AdminCategorias"'));
  assert.doesNotMatch(html, /data-modulo="(?:AdminEstadisticas|Pagos)"/);
  assert.deepEqual([...html.matchAll(/href="([^"]+)"/g)].map(m => m[1]), ['/admin/estadisticas', '/admin/pagos', '/admin/web']);
});
