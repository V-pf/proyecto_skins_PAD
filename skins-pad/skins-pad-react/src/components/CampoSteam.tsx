import type { FormApi, Valores } from "../lib/useFormulario";
import { errorSteam, prefijosSteam, type TipoEnlaceSteam } from "../lib/enlaceSteam";

interface Props<T extends Valores> {
  form: FormApi<T>;
  nombre: keyof T & string;
  tipo: TipoEnlaceSteam;
}

export default function CampoSteam<T extends Valores>({ form, nombre, tipo }: Props<T>) {
  const id = form.id + "-" + nombre;
  const prefijo = prefijosSteam[tipo];
  const perfil = tipo === "perfil";
  const error = form.errores[nombre];

  function actualizar(texto: string) {
    const sufijo = texto.slice(prefijo.length);
    const completoValido = texto.startsWith(prefijo) && sufijo && !errorSteam(tipo, sufijo);
    form.set(nombre, completoValido ? sufijo : texto);
    form.tocar(nombre);
  }

  return <div className="mb-3">
    <label className="form-label" htmlFor={id}>{perfil ? "Enlace a tu perfil de Steam" : "Enlace de intercambio de Steam"}</label>
    <div className="input-group campo-steam">
      <span className="input-group-text" id={id + "-prefijo"}>{prefijo}</span>
      <input id={id} name={nombre} type="text" inputMode={perfil ? "text" : "numeric"}
        className={"form-control" + (error ? " is-invalid" : "")}
        placeholder={perfil ? "MinePablo" : "1872638"} value={String(form.valores[nombre])}
        aria-invalid={!!error} aria-required={!perfil}
        aria-describedby={[id + "-prefijo", !perfil && id + "-ayuda", error && id + "-error"].filter(Boolean).join(" ")}
        onChange={e => actualizar(e.target.value)} onBlur={() => form.tocar(nombre)}
        onPaste={e => {
          const texto = e.clipboardData.getData("text");
          if (texto.includes("://")) {
            e.preventDefault();
            actualizar(texto);
          }
        }} />
    </div>
    {!perfil && <div className="form-text" id={id + "-ayuda"}>Escribe el identificador numérico del intercambio o pega el enlace completo del prototipo.</div>}
    {error && <div className="invalid-feedback d-block" id={id + "-error"} aria-live="polite">{error}</div>}
  </div>;
}
