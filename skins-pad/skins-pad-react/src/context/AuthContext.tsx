import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from "react";
import type { ReactNode } from "react";
import type { Usuario } from "../types";
import { ErrorAPI, fallo } from "../lib/api";
import { authService } from "../services/auth.service";
import { guardarSesion, leerSesion, limpiarSesion } from "../lib/storage";
export interface DatosRegistro {
  nombre: string;
  usuario: string;
  correo: string;
  clave: string;
  fechaNacimiento: string;
}

interface Auth {
  usuario: Usuario | null;
  /** Devuelve null si salió bien, o el mensaje de error. */
  ingresar: (
    identificador: string,
    clave: string,
    recordar: boolean,
  ) => Promise<string | null>;
  registrar: (datos: DatosRegistro) => Promise<string | null>;
  salir: () => Promise<void>;
  refrescar: () => Promise<void>;
}

const Ctx = createContext<Auth | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  // Al abrir la aplicación se valida la sesión guardada. Un fallo de conexión permite reintentar.
  const refrescar = useCallback(async () => {
    const token = leerSesion();
    try {
      if (!token) {
        setUsuario(null);
        return;
      }
      const u = await authService.validar(token);
      if (u.suspendido) {
        limpiarSesion();
        setUsuario(null);
      } else setUsuario(u);
      setError("");
    } catch (e) {
      if (e instanceof ErrorAPI && [400, 401, 403, 404].includes(e.status)) {
        limpiarSesion();
        setUsuario(null);
      } else setError(fallo(e));
    } finally {
      setCargando(false);
    }
  }, []);
  useEffect(() => {
    void refrescar();
  }, [refrescar]);
  // La opción «recordar» decide dónde se guarda el token tras iniciar sesión.
  async function ingresar(
    identificador: string,
    clave: string,
    recordar: boolean,
  ) {
    try {
      const r = await authService.ingresar(identificador, clave);
      guardarSesion(r.token, recordar);
      setUsuario(r.usuario);
      return null;
    } catch (e) {
      return fallo(e);
    }
  }
  // Crea la cuenta y usa los mismos datos para iniciar sesión enseguida.
  async function registrar(d: DatosRegistro) {
    try {
      await authService.registrar({
        nombre: d.nombre,
        email: d.correo,
        username: d.usuario,
        password: d.clave,
        fechaNacimiento: d.fechaNacimiento,
      });
      return ingresar(d.usuario, d.clave, false);
    } catch (e) {
      return fallo(e);
    }
  }
  // La sesión del navegador se limpia aunque falle la solicitud de cierre.
  async function salir() {
    try {
      await authService.salir();
    } catch (e) {
      window.alert(fallo(e));
    } finally {
      limpiarSesion();
      setUsuario(null);
    }
  }
  if (cargando) return <p className="container py-4">Validando sesión…</p>;
  if (error)
    return (
      <div className="container" role="alert">
        {error}{" "}
        <button
          onClick={() => {
            setError("");
            void refrescar();
          }}
        >
          Reintentar
        </button>
      </div>
    );
  return (
    <Ctx.Provider value={{ usuario, ingresar, registrar, salir, refrescar }}>
      {children}
    </Ctx.Provider>
  );
}
export function useAuth() {
  const c = useContext(Ctx);
  if (!c) throw new Error("Falta AuthProvider");
  return c;
}
