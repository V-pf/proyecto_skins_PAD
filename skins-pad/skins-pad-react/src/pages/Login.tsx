import { useState } from "react";
import type { FormEvent } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import Campo from "../components/Campo";
import { useAuth } from "../context/AuthContext";
import { useFormulario } from "../lib/useFormulario";
import type { Errores } from "../lib/useFormulario";
import { requerido } from "../lib/validaciones";

interface LoginForm {
  [clave: string]: string | boolean;
  identificador: string;
  clave: string;
  recordar: boolean;
}

export default function Login() {
  const { usuario, ingresar } = useAuth();

  const ubicacion = useLocation();
  const desde = (ubicacion.state as { desde?: string } | null)?.desde;
  const [errorServidor, setErrorServidor] = useState("");
  const [enviando, setEnviando] = useState(false);

  const form = useFormulario<LoginForm>(
    "login",
    { identificador: "", clave: "", recordar: false },
    (v): Errores<LoginForm> => ({
      identificador: requerido(v.identificador),
      clave: requerido(v.clave),
    })
  );

  // si ya hay sesión no tiene sentido mostrar el login
  if (usuario) return <Navigate to="/" replace />;

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setErrorServidor("");
    if (!form.validar()) return;
    setEnviando(true);
    const error = await ingresar(form.valores.identificador, form.valores.clave, form.valores.recordar);
    setEnviando(false);
    if (error) {
      setErrorServidor(error);
      return;
    }
  }

  function rellenar(identificador: string, clave: string) {
    form.set("identificador", identificador);
    form.set("clave", clave);
  }

  return (
    <div className="container py-5" style={{ maxWidth: "480px" }}>
      <h1 className="mb-2 text-center">Ingresar</h1>
      <p className="text-body-secondary text-center mb-4">Usa tu cuenta de Skins PAD, o entra con Steam.</p>
      {desde && (
        <div className="alert alert-secondary" role="status">
          Inicia sesión para continuar.
        </div>
      )}
      <button type="button" className="btn btn-outline-light w-100 mb-4" disabled title="Botón de ejemplo, sin integración real">
        Ingresar con Steam
      </button>
      <div className="d-flex align-items-center gap-3 mb-4">
        <hr className="flex-fill linea-divisoria" />
        <span className="text-body-secondary small">o con tu correo</span>
        <hr className="flex-fill linea-divisoria" />
      </div>
      <form onSubmit={enviar} noValidate>
        {errorServidor && (
          <div className="alert alert-danger" role="alert">
            {errorServidor}
          </div>
        )}
        <Campo form={form} nombre="identificador" etiqueta="Usuario o correo" placeholder="penquista_gamer" autoComplete="username" />
        <Campo form={form} nombre="clave" tipo="password" etiqueta="Contraseña" placeholder="••••••••" autoComplete="current-password" />
        <Campo form={form} nombre="recordar" tipo="checkbox" etiqueta="Mantener la sesión iniciada en este dispositivo" />
        <button type="submit" className="btn btn-primary" disabled={enviando}>
          {enviando ? "Ingresando..." : "Ingresar"}
        </button>
      </form>
      <p className="text-center text-body-secondary mt-4">
        ¿No tienes cuenta? <Link to="/registro">Crea una aquí</Link>.
      </p>

      <div className="alert alert-secondary mt-4">
        <p className="fw-semibold mb-1">Acceso de demostración</p>
        <p className="small mb-2">Rellena el formulario con una cuenta de ejemplo:</p>
        <div className="d-flex gap-2 flex-wrap">
          <button type="button" className="btn btn-sm btn-outline-light" onClick={() => rellenar("penquista_gamer", "Demo1234!")}>
            Cliente de ejemplo
          </button>
          <button type="button" className="btn btn-sm btn-outline-light" onClick={() => rellenar("vendedor_demo", "Demo1234!")}>
            Vendedor de ejemplo
          </button>
          <button type="button" className="btn btn-sm btn-outline-light" onClick={() => rellenar("admin", "Admin1234!")}>
            Administrador de ejemplo
          </button>
        </div>
      </div>
    </div>
  );
}
