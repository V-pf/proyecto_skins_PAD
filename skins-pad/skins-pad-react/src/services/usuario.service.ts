import { solicitar } from "./http";
import { usuarioUI, type UsuarioAPI } from "../lib/api";
import type { Usuario } from "../types";

export const usuarioService = {
  async listar(admin: boolean, signal?: AbortSignal) {
    const usuarios = await solicitar<UsuarioAPI[]>(
      admin ? "/usuarios" : "/usuarios/publicos", "GET", undefined, signal,
    );
    return usuarios.map(usuarioUI);
  },
  async actualizarEstado(id: string, datos: Partial<Usuario>) {
    return usuarioUI(await solicitar<UsuarioAPI>("/usuarios/" + id + "/estado", "PATCH", datos));
  },
};
