import { solicitar } from "./http";
import { usuarioUI, type UsuarioAPI } from "../lib/api";

export const authService = {
  async validar() {
    return usuarioUI(await solicitar<UsuarioAPI>("/auth/validar"));
  },
  async ingresar(identificador: string, password: string, recordar: boolean) {
    const respuesta = await solicitar<{ usuario: UsuarioAPI; token: string }>(
      "/auth/login?recordar=" + recordar, "POST", { identificador, password },
    );
    return { token: respuesta.token, usuario: usuarioUI(respuesta.usuario) };
  },
  registrar: (datos: unknown) => solicitar<void>("/auth/registro", "POST", datos),
  salir: () => solicitar<void>("/auth/logout", "POST"),
};
