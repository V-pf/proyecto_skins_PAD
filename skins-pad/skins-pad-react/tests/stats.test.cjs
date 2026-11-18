const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
function load(name) {
  const exports = {};
  const code = ts.transpileModule(fs.readFileSync(path.join(__dirname, '../src/lib/stats', name + '.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  vm.runInNewContext(code, { exports, Intl, Date });
  return exports;
}
const period = load('period');
const format = load('format');
test('los presets incluyen ambos extremos y respetan meses y años bisiestos', () => {
  assert.equal(period.range('7', '2026-11-14').from, '2026-11-08');
  assert.equal(period.range('30', '2026-11-14').from, '2026-10-16');
  assert.equal(period.range('previous', '2024-03-15').to, '2024-02-29');
  assert.equal(period.range('previous', '2026-01-15').from, '2025-12-01');
  assert.equal(period.range('all', '2026-11-14').from, '');
});
test('hoy se calcula en Santiago, no con el día UTC del navegador', () => {
  assert.equal(period.todaySantiago(new Date('2026-11-14T01:00:00Z')), '2026-11-13');
  assert.equal(period.todaySantiago(new Date('2026-11-14T04:00:00Z')), '2026-11-14');
});
test('CSV protege fórmulas y conserva comillas y separadores', () => {
  assert.equal(format.csvCell('=SUM(A1)'), '"\'=SUM(A1)"');
  assert.equal(format.csvCell('  +CMD'), '"\'  +CMD"');
  assert.equal(format.csvCell('AK; "Red"'), '"AK; ""Red"""');
  assert.equal(format.csvCell(null), '""');
});
test('los importes desconocidos no se presentan como cero', () => {
  assert.equal(format.money(null), 'No disponible');
  assert.equal(format.money(0), '$0');
  assert.equal(format.money(4216789), '$4.216.789');
});
