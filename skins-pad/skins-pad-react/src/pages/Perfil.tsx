import VolverPanel from "../components/VolverPanel";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { solicitar } from "../services/http";
import { useOperacion } from "../lib/useOperacion";
export default function Perfil() {
  const { usuario, refrescar } = useAuth();
  const { ejecutar, ocupado } = useOperacion();
  const [aviso, setAviso] = useState("");
  if (!usuario) return null;
  return (
    <main className="container py-4">
      <VolverPanel />
      <h1>Editar perfil</h1>
      <div className="row g-3">
        <form
          className="col-lg-6"
          onSubmit={(e) => {
            e.preventDefault();
            const datos = Object.fromEntries(new FormData(e.currentTarget));
            void ejecutar(async () => {
              await solicitar(
                "/usuarios/" + usuario.id + "/perfil",
                "PATCH",
                datos,
              );
              // Actualiza la sesión para que los datos editados aparezcan en el resto de la aplicación.
              await refrescar();
              setAviso("Perfil actualizado.");
            });
          }}
        >
          <label className="d-block mb-3">
            Nombre completo
            <input
              className="form-control"
              name="nombre"
              required
              maxLength={120}
              defaultValue={usuario.nombre}
            />
          </label>
          <label className="d-block mb-3">
            Correo
            <input
              className="form-control"
              name="email"
              type="email"
              required
              defaultValue={usuario.correo}
            />
          </label>
          <label className="d-block mb-3">
            Perfil de Steam
            <input
              className="form-control"
              name="linkSteam"
              type="url"
              defaultValue={usuario.urlSteam}
            />
          </label>
          <button disabled={ocupado} className="btn btn-primary">
            Guardar cambios
          </button>
          <p role="status">{aviso}</p>
        </form>
      </div>
    </main>
  );
}
