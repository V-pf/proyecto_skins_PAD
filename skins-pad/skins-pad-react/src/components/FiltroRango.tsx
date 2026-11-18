import { useState } from "react";

export type Rango = [number, number | null];

interface Props {
  tipo: "precio" | "float";
  tope: number;
  valor: Rango;
  cambiar: (rango: Rango) => void;
}

function mostrar(numero: number | null) {
  return numero === null ? "" : numero.toLocaleString("es-CL", { useGrouping: false, maximumFractionDigits: 20 });
}

export default function FiltroRango({ tipo, tope, valor, cambiar }: Props) {
  const precio = tipo === "precio";
  const titulo = precio ? "Precio (CLP)" : "Float (desgaste)";
  const etiquetas = precio ? ["Desde", "Hasta"] : ["Mínimo", "Máximo"];
  const [campos, setCampos] = useState(() => valor.map(mostrar));
  const [error, setError] = useState("");
  // El último punto del deslizador de precio representa un máximo abierto.
  const limite = precio ? Math.max(tope, valor[0], valor[1] ?? 0) + 1 : 1;
  const superior = valor[1] ?? limite;

  function escribir(indice: number, texto: string) {
    const nuevos = [...campos];
    nuevos[indice] = texto;
    setCampos(nuevos);
    const patron = precio ? /^\d*$/ : /^(?:\d+(?:[.,]\d*)?|[.,]\d+)?$/;
    const minimo = Number(nuevos[0].replace(",", "."));
    const maximo = nuevos[1] === "" ? (precio ? null : 1) : Number(nuevos[1].replace(",", "."));
    let mensaje = "";
    if (!nuevos.every(campo => patron.test(campo)) ||
        (precio && ![minimo, maximo ?? 0].every(Number.isSafeInteger))) {
      mensaje = precio ? "Ingresa montos enteros en pesos." : "Ingresa un float entre 0 y 1.";
    } else if (!precio && (minimo > 1 || (maximo ?? 1) > 1)) {
      mensaje = "El float debe estar entre 0 y 1.";
    } else if (maximo !== null && minimo > maximo) {
      mensaje = "El mínimo debe ser menor o igual al máximo.";
    }
    setError(mensaje);
    if (!mensaje) cambiar([minimo, maximo]);
  }

  function deslizar(indice: number, numero: number) {
    const rango: Rango = [...valor];
    if (indice === 0) rango[0] = Math.min(numero, superior, precio ? limite - 1 : limite);
    else rango[1] = precio && numero === limite ? null : Math.max(numero, valor[0]);
    setCampos(rango.map(mostrar));
    setError("");
    cambiar(rango);
  }

  return (
    <fieldset className="filtro-rango">
      <legend>{titulo}</legend>
      <div className="rango-pista">
        <span className={precio ? "rango-activo" : "rango-activo barra-desgaste"}
          style={{ clipPath: `inset(0 ${100 - 100 * superior / limite}% 0 ${100 * valor[0] / limite}%)` }} />
        {etiquetas.map((etiqueta, i) => (
          <input key={etiqueta} type="range" aria-label={`${titulo} · ${etiqueta}`}
            min={0} max={limite} step={precio ? 1 : "any"}
            style={{ zIndex: i === 0 && superior === limite && valor[0] >= limite * 0.99 ? 2 : undefined }}
            value={i === 0 ? valor[0] : superior}
            aria-valuetext={i === 1 && valor[1] === null ? "Sin límite" : mostrar(valor[i])}
            onChange={e => deslizar(i, e.target.valueAsNumber)} />
        ))}
      </div>
      <div className="rango-campos">
        {etiquetas.map((etiqueta, i) => (
          <label key={etiqueta}>{etiqueta}
            <input className="form-control form-control-sm" inputMode={precio ? "numeric" : "decimal"}
              value={campos[i]} placeholder={i === 0 ? "0" : precio ? "Sin límite" : "1"}
              aria-invalid={!!error} aria-describedby={error ? tipo + "-error" : undefined}
              onChange={e => escribir(i, e.target.value)} />
          </label>
        ))}
      </div>
      {error && <small id={tipo + "-error"} className="text-danger" role="status">{error}</small>}
    </fieldset>
  );
}
