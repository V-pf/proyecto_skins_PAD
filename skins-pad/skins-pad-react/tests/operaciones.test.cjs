const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");
const React = require("react");
const { renderToStaticMarkup } = require("react-dom/server");

function vista(rol, estado, simulado = false) {
  const exports = {};
  const fuente = fs.readFileSync(path.join(__dirname, "../src/pages/Operaciones.tsx"), "utf8");
  const codigo = ts.transpileModule(fuente, {
    compilerOptions: {
      jsx: ts.JsxEmit.ReactJSX,
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
    },
  }).outputText;
  vm.runInNewContext(codigo, {
    exports,
    Date,
    require(nombre) {
      if (nombre === "react" || nombre === "react/jsx-runtime") return require(nombre);
      if (nombre === "react-router-dom") {
        return {
          Link: ({ to, children }) => React.createElement("a", { href: to }, children),
          useLocation: () => ({}),
        };
      }
      if (nombre.includes("useOperaciones")) {
        return { default: () => ({
          datos: [{
            operacion: { id: 1, pagoId: 1, nombre: "AK-47", estado, precio: 1000, comision: 50,
              retenido: !simulado && estado !== "PAGO_PENDIENTE", sincronizado: true },
            eventos: [], metodo: simulado ? "TARJETA" : "SALDO", simulado,
          }],
          error: "", cargando: false, recargar() {},
        }) };
      }
      if (nombre.includes("formato")) return { formatoCLP: (v) => "$" + v };
      if (nombre.includes("operacion.service")) return { operacionService: {} };
      return { default: () => null };
    },
  });
  return renderToStaticMarkup(React.createElement(exports.default, { vista: rol }));
}

test("el vendedor puede aceptar la venta con saldo ya debitado", () => {
  const html = vista("ventas", "PAGO_RETENIDO");
  assert.match(html, /Aceptar venta/);
  assert.match(html, /Rechazar y reembolsar/);
  assert.doesNotMatch(html, /Confirmar recepción y liberar fondos|Resolver y liberar/);
});

test("el comprador confirma recepción y puede abrir disputa", () => {
  const html = vista("compras", "ENTREGADA");
  assert.match(html, /Confirmar recepción y liberar fondos/);
  assert.match(html, /Abrir disputa/);
  assert.doesNotMatch(html, /Marcar como entregada/);
});

test("administración consulta una compra normal sin acciones sobre el pago", () => {
  const html = vista("excepciones", "PAGO_PENDIENTE");
  assert.doesNotMatch(html, /Verificar pago|Congelar operación|Confirmar pago recibido|Aceptar venta|Confirmar recepción/);
});

test("administración conserva las acciones para resolver disputas", () => {
  const disputa = vista("excepciones", "EN_DISPUTA");
  assert.match(disputa, /Resolver a favor del comprador y reembolsar/);
  assert.match(disputa, /Resolver y liberar al vendedor/);
  assert.doesNotMatch(vista("excepciones", "ENTREGADA"), /Resolver y liberar al vendedor/);
});

test("el vendedor de una skin de prueba ve que el pago fue simulado", () => {
  const html = vista("ventas", "PAGO_RETENIDO", true);
  assert.match(html, /Operación de demostración/);
  assert.match(html, /Aceptar venta/);
  assert.doesNotMatch(html, /Fondos en custodia|Rechazar y reembolsar/);
});
