import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import Campo from "../components/Campo";
import MedidorClave from "../components/MedidorClave";
import { useAuth } from "../context/AuthContext";
import { useFormulario } from "../lib/useFormulario";
import type { Errores } from "../lib/useFormulario";
import { aceptado, correo, mayorDeEdad, opcional, requerido } from "../lib/validaciones";

interface RegistroForm {
  [clave: string]: string | boolean;
  nombre: string;
  usuario: string;
  correo: string;
  clave: string;
  confirmar: string;
  fechaNacimiento: string;
  urlSteam: string;
  terminos: boolean;
  mayorEdad: boolean;
}

const INICIAL: RegistroForm = {
  nombre: "",
  usuario: "",
  correo: "",
  clave: "",
  confirmar: "",
  fechaNacimiento: "",
  urlSteam: "",
  terminos: false,
  mayorEdad: false,
};

export default function Registro() {
  const { registrar } = useAuth();
  const navigate = useNavigate();
  const [errorServidor, setErrorServidor] = useState("");
  const [enviando, setEnviando] = useState(false);

  const form = useFormulario<RegistroForm>(
    "registro",
    INICIAL,
    (v): Errores<RegistroForm> => ({
      nombre: requerido(v.nombre, "nombre"),
      usuario: requerido(v.usuario, "usuario"),
      correo: correo(v.correo),
      clave: v.clave === "" ? "Este campo es obligatorio." : v.clave.length < 8 ? "Escribe al menos 8 caracteres." : "",
      confirmar: v.confirmar === "" ? "Este campo es obligatorio." : v.confirmar !== v.clave ? "No coincide con el campo anterior." : "",
      fechaNacimiento: mayorDeEdad(v.fechaNacimiento),
      urlSteam: opcional(v.urlSteam, "url_steam"),
      terminos: aceptado(v.terminos),
      mayorEdad: aceptado(v.mayorEdad),
    })
  );

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setErrorServidor("");
    if (!form.validar()) return;
    const v = form.valores;
    setEnviando(true);
    const error = await registrar({
      nombre: v.nombre,
      usuario: v.usuario,
      correo: v.correo,
      clave: v.clave,
      fechaNacimiento: v.fechaNacimiento,
      urlSteam: v.urlSteam,
    });
    setEnviando(false);
    if (error) {
      setErrorServidor(error);
      return;
    }
    // la cuenta ya quedó creada y con la sesión iniciada: vamos a su panel
    navigate("/cliente/panel", { replace: true, state: { mensaje: "¡Tu cuenta fue creada! Ya iniciaste sesión." } });
  }

  return (
    <div className="container py-5" style={{ maxWidth: "560px" }}>
      <h1 className="mb-2 text-center">Crear cuenta</h1>
      <p className="text-body-secondary text-center mb-4">Vender skins requiere verificar tu identidad más adelante desde tu panel.</p>
      <form onSubmit={enviar} noValidate>
        {form.intento && Object.keys(form.errores).length > 0 && (
          <div className="alert alert-danger" role="alert">
            Hay campos por corregir. Revisa los mensajes marcados en rojo.
          </div>
        )}
        {errorServidor && (
          <div className="alert alert-danger" role="alert">
            {errorServidor}
          </div>
        )}
        <Campo form={form} nombre="nombre" etiqueta="Nombre completo" placeholder="Camila Torres" autoComplete="name" />
        <Campo form={form} nombre="usuario" etiqueta="Nombre de usuario" placeholder="penquista_gamer" ayuda="Entre 3 y 20 caracteres: letras, números o guion bajo." autoComplete="username" />
        <Campo form={form} nombre="correo" etiqueta="Correo electrónico" tipo="email" placeholder="camila@correo.com" autoComplete="email" />
        <Campo
          form={form}
          nombre="clave"
          tipo="password"
          etiqueta="Contraseña"
          autoComplete="new-password"
          extra={<MedidorClave valor={form.valores.clave} />}
          ayuda="Mínimo 8 caracteres. Combina mayúsculas, números y símbolos para que sea más fuerte."
        />
        <Campo form={form} nombre="confirmar" tipo="password" etiqueta="Confirmar contraseña" autoComplete="new-password" />
        <Campo form={form} nombre="fechaNacimiento" tipo="date" etiqueta="Fecha de nacimiento" />
        <Campo
          form={form}
          nombre="urlSteam"
          etiqueta={
            <>
              Enlace a tu perfil de Steam <span className="text-body-secondary fw-normal"></span>
            </>
          }
          placeholder="https://steamcommunity.com/id/tu-usuario"
        />
        <Campo form={form} nombre="terminos" tipo="checkbox" etiqueta="He leído y acepto los términos de uso y la política de privacidad." />
        <Campo form={form} nombre="mayorEdad" tipo="checkbox" etiqueta="Declaro que soy mayor de 18 años." />
        <button type="submit" className="btn btn-primary" disabled={enviando}>
          {enviando ? "Creando cuenta..." : "Crear cuenta"}
        </button>
      </form>
      <p className="text-center text-body-secondary mt-3">
        ¿Ya tienes cuenta? <Link to="/login">Ingresa aquí</Link>.
      </p>
    </div>
  );
}
