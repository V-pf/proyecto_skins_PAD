import type { ChangeEvent, ReactNode } from "react";
import type { FormApi, Valores } from "../lib/useFormulario";

interface Props<T extends Valores> {
  form: FormApi<T>;
  nombre: keyof T & string;
  etiqueta: ReactNode;
  tipo?: "text" | "password" | "email" | "number" | "date" | "tel" | "textarea" | "select" | "checkbox";
  placeholder?: string;
  prefijo?: string;
  transformar?: (valor: string) => string;
  ayuda?: ReactNode;
  opciones?: { valor: string; etiqueta: string }[];
  textoOpcionVacia?: string;
  filas?: number;
  maxLength?: number;
  step?: string;
  min?: string;
  max?: string;
  autoComplete?: string;
  /** contenido extra debajo del input (por ejemplo el medidor de contraseña) */
  extra?: ReactNode;
}

type ElementoForm = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

/** Un campo de formulario de Bootstrap conectado al hook useFormulario. */
export default function Campo<T extends Valores>({
  form,
  nombre,
  etiqueta,
  tipo = "text",
  placeholder,
  prefijo,
  transformar,
  ayuda,
  opciones,
  textoOpcionVacia = "Selecciona una opción",
  filas = 4,
  maxLength,
  step,
  min,
  max,
  autoComplete,
  extra,
}: Props<T>) {
  const id = `${form.id}-${nombre}`;
  const error = form.errores[nombre];
  const valor = form.valores[nombre];
  const describe = [prefijo ? id + "-prefijo" : "", ayuda ? `${id}-ayuda` : "", error ? `${id}-error` : ""].filter(Boolean).join(" ") || undefined;

  const alCambiar = (e: ChangeEvent<ElementoForm>) => {
    const el = e.target;
    form.set(nombre, el instanceof HTMLInputElement && el.type === "checkbox"
      ? el.checked
      : transformar?.(el.value) ?? el.value);
  };

  const comunes = {
    id,
    name: nombre,
    onChange: alCambiar,
    onBlur: () => form.tocar(nombre),
    "aria-describedby": describe,
    "aria-invalid": error ? true : undefined,
  };
  const claseError = error ? " is-invalid" : "";

  const mensajeError = (
    <div id={`${id}-error`} className="invalid-feedback">
      {error}
    </div>
  );
  const textoAyuda = ayuda ? (
    <div id={`${id}-ayuda`} className="form-text">
      {ayuda}
    </div>
  ) : null;

  if (tipo === "checkbox") {
    return (
      <div className="form-check mb-3">
        <input {...comunes} type="checkbox" className={`form-check-input${claseError}`} checked={Boolean(valor)} />
        <label className="form-check-label" htmlFor={id}>
          {etiqueta}
        </label>
        {mensajeError}
      </div>
    );
  }

  let control: ReactNode;
  if (tipo === "select") {
    control = (
      <select {...comunes} className={`form-select${claseError}`} value={String(valor)}>
        <option value="">{textoOpcionVacia}</option>
        {opciones?.map((o) => (
          <option key={o.valor} value={o.valor}>
            {o.etiqueta}
          </option>
        ))}
      </select>
    );
  } else if (tipo === "textarea") {
    control = (
      <textarea
        {...comunes}
        className={`form-control${claseError}`}
        rows={filas}
        maxLength={maxLength}
        placeholder={placeholder}
        value={String(valor)}
      />
    );
  } else {
    control = (
      <input
        {...comunes}
        type={tipo}
        className={`form-control${claseError}`}
        placeholder={placeholder}
        maxLength={maxLength}
        step={step}
        min={min}
        max={max}
        autoComplete={autoComplete}
        value={String(valor)}
      />
    );
  }

  return (
    <div className="mb-3">
      <label htmlFor={id} className="form-label">
        {etiqueta}
      </label>
      {prefijo ? (
        <div className="input-group has-validation campo-con-prefijo">
          <span className="input-group-text" id={id + "-prefijo"}>{prefijo}</span>
          {control}
          {mensajeError}
        </div>
      ) : control}
      {extra}
      {textoAyuda}
      {!prefijo && mensajeError}
    </div>
  );
}
