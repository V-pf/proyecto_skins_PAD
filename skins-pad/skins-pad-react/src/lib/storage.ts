const PREFIJO = "skinspad:servidor:";
/* La sesión vive en sessionStorage (se pierde al cerrar la pestaña) o en
   localStorage si el usuario marcó "mantener la sesión iniciada". */
export function leerSesion(): string | null {
  try {
    return (
      window.sessionStorage.getItem(PREFIJO + "sesion") ?? window.localStorage.getItem(PREFIJO + "sesion")
    );
  } catch {
    return null;
  }
}

export function guardarSesion(usuarioId: string, recordar: boolean): void {
  try {
    limpiarSesion();
    (recordar ? window.localStorage : window.sessionStorage).setItem(PREFIJO + "sesion", usuarioId);
  } catch {
    // ignorar
  }
}

export function limpiarSesion(): void {
  try {
    window.sessionStorage.removeItem(PREFIJO + "sesion");
    window.localStorage.removeItem(PREFIJO + "sesion");
  } catch {
    // ignorar
  }
}

export function reemplazarToken(actual: string, nuevo: string): boolean {
  if (leerSesion() !== actual) return false;
  const clave = PREFIJO + "sesion";
  const destino = sessionStorage.getItem(clave) === actual ? sessionStorage : localStorage;
  destino.setItem(clave, nuevo);
  return true;
}
