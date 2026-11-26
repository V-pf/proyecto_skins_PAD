import { useState } from "react";
import { formatoCLP } from "../lib/formato";

export const DESGASTES = [
  { sigla: "FN", min: 0, max: 0.07, color: "#64bf86" },
  { sigla: "MW", min: 0.07, max: 0.15, color: "#a1c969" },
  { sigla: "FT", min: 0.15, max: 0.38, color: "#edbe59" },
  { sigla: "WW", min: 0.38, max: 0.45, color: "#e29353" },
  { sigla: "BS", min: 0.45, max: 1, color: "#df736d" },
] as const;

export interface Rango {
  min: number | null;
  max: number | null;
  desgaste?: typeof DESGASTES[number];
}

const formatoFloat = new Intl.NumberFormat("es-CL", { maximumSignificantDigits: 17 });

function leerNumero(texto: string, esFloat: boolean) {
  const limpio = texto.trim();
  if (!limpio) return null;
  if (esFloat) {
    return /^(?:\d+(?:[.,]\d*)?|[.,]\d+)$/.test(limpio) ? Number(limpio.replace(",", ".")) : NaN;
  }
  return /^\$?\s*(?:\d+|\d{1,3}(?:\.\d{3})+)$/.test(limpio)
    ? Number(limpio.replace(/[$.\s]/g, ""))
    : NaN;
}

interface Props {
  tipo: "precio" | "float";
  limite: number;
  rango: Rango;
  cambiar: (rango: Rango) => void;
}

export default function FiltroRango({ tipo, limite, rango, cambiar }: Props) {
  const [borradores, setBorradores] = useState<Partial<Record<"min" | "max", string>>>({});
  const esFloat = tipo === "float";
  const etiquetas = esFloat ? { min: "Mínimo", max: "Máximo" } : { min: "Desde", max: "Hasta" };
  const tope = Math.max(limite, rango.min ?? 0, rango.max ?? 0);
  const min = rango.min ?? 0;
  const max = rango.max ?? tope;
  const fondo = esFloat
    ? "linear-gradient(to right, " + DESGASTES.map(({ min, max, color }) => color + " " + min * 100 + "% " + max * 100 + "%").join(", ") + ")"
    : "var(--linea)";

  function validar(extremo: "min" | "max", texto: string) {
    const valor = leerNumero(texto, esFloat);
    if (valor === null) return { valor, error: "" };
    if (!Number.isFinite(valor) || (esFloat ? valor > 1 : !Number.isSafeInteger(valor))) {
      return { valor, error: esFloat ? "Ingresa un valor entre 0 y 1." : "Ingresa un monto válido en pesos." };
    }
    if ((extremo === "min" && rango.max !== null && valor > rango.max) ||
        (extremo === "max" && valor < min)) {
      return { valor, error: "El mínimo no puede superar al máximo." };
    }
    return { valor, error: "" };
  }

  function cambiarExtremo(extremo: "min" | "max", valor: number | null) {
    cambiar({ min: rango.min, max: rango.max, [extremo]: valor });
  }

  return (
    <fieldset className="mb-3">
      <legend>{esFloat ? "Desgaste (float)" : "Precio (CLP)"}</legend>
      <div className="filtro-rango">
        <div className="filtro-rango-pista" style={{ background: fondo }} aria-hidden="true">
          <div className="filtro-rango-seleccion" style={{
            left: min / tope * 100 + "%",
            right: (1 - max / tope) * 100 + "%",
          }} />
        </div>
        {(["min", "max"] as const).map((extremo) => (
          <input
            key={extremo}
            className="filtro-rango-barra"
            type="range"
            aria-label={(esFloat ? "Desgaste " : "Precio ") + (extremo === "min" ? "mínimo" : "máximo")}
            aria-valuetext={!esFloat && extremo === "max" && rango.max === null
              ? "Sin límite"
              : esFloat ? formatoFloat.format(extremo === "min" ? min : max) : formatoCLP(extremo === "min" ? min : max)}
            min={0}
            max={tope}
            step={esFloat ? "any" : 1}
            value={extremo === "min" ? min : max}
            style={{ zIndex: extremo === "min" && min === max && min === tope ? 2 : undefined }}
            onChange={(e) => {
              const valor = Number(e.target.value);
              setBorradores({});
              cambiarExtremo(extremo, extremo === "min" ? Math.min(valor, max) : Math.max(valor, min));
            }}
          />
        ))}
      </div>
      <div className="row g-2">
        {(["min", "max"] as const).map((extremo) => {
          const valor = rango[extremo];
          const borrador = borradores[extremo];
          const error = borrador === undefined ? "" : validar(extremo, borrador).error;
          const id = tipo + "-" + extremo;
          return (
            <div className="col-6" key={extremo}>
              <label className="form-label small mb-1" htmlFor={id}>{etiquetas[extremo]}</label>
              <input
                id={id}
                className={"form-control form-control-sm" + (error ? " is-invalid" : "")}
                inputMode={esFloat ? "decimal" : "numeric"}
                value={borrador ?? (valor === null ? "" : esFloat ? formatoFloat.format(valor) : formatoCLP(valor))}
                placeholder={extremo === "min" ? "0" : esFloat ? "1" : "Sin límite"}
                aria-invalid={error ? true : undefined}
                aria-describedby={error ? id + "-error" : undefined}
                onChange={(e) => {
                  const texto = e.target.value;
                  setBorradores({ [extremo]: texto });
                  const resultado = validar(extremo, texto);
                  if (!resultado.error) cambiarExtremo(extremo, resultado.valor);
                }}
                onBlur={() => {
                  if (!error) setBorradores((prev) => ({ ...prev, [extremo]: undefined }));
                }}
              />
              {error && <div className="invalid-feedback" id={id + "-error"}>{error}</div>}
            </div>
          );
        })}
      </div>
      {esFloat && (
        <div className="d-flex flex-wrap gap-1 mt-2">
          {DESGASTES.map((desgaste) => (
            <button
              type="button"
              key={desgaste.sigla}
              className="btn btn-sm btn-outline-light px-1"
              style={{ borderBottomColor: desgaste.color }}
              aria-pressed={rango.desgaste === desgaste}
              onClick={() => {
                setBorradores({});
                cambiar({ min: desgaste.min, max: desgaste.max, desgaste });
              }}
            >{desgaste.sigla}</button>
          ))}
        </div>
      )}
    </fieldset>
  );
}
