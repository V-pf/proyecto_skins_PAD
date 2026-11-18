import { useOperacion } from "../lib/useOperacion";
import { useState } from "react";
import type { FormEvent } from "react";
import Campo from "../components/Campo";
import MenuCuenta from "../components/MenuCuenta";
import Migas from "../components/Migas";
import Modal from "../components/Modal";
import { useAuth } from "../context/AuthContext";
import { useTienda } from "../context/TiendaContext";
import { useFormulario } from "../lib/useFormulario";
import type { Errores } from "../lib/useFormulario";
import { largo, seleccion } from "../lib/validaciones";
import type { Usuario } from "../types";

interface GestionForm {
  [clave: string]: string | boolean;
  accion: string;
  motivo: string;
}

const ACCIONES = [
  { valor: "suspender", etiqueta: "Suspender cuenta" },
  { valor: "reactivar", etiqueta: "Reactivar cuenta" },
  { valor: "verificar", etiqueta: "Marcar como verificado" },
  { valor: "desverificar", etiqueta: "Quitar verificación" },
];

function etiquetaRol(u: Usuario): string {
  if (u.rol === "admin") return "Administrador";
  return u.verificado ? "Cliente verificado" : "Cliente";
}

export default function AdminUsuarios({ integrado = false }: { integrado?: boolean }) {
  const Titulo = integrado ? "h3" : "h2";
  const { ejecutar, ocupado } = useOperacion();
  const { usuario: yo } = useAuth();
  const { usuarios, actualizarUsuario, cargando } = useTienda();
  const [busqueda, setBusqueda] = useState("");
  const [rol, setRol] = useState("");
  const [estado, setEstado] = useState("");
  const [gestionadoId, setGestionadoId] = useState<string | null>(null);
  const [aviso, setAviso] = useState("");

  const form = useFormulario<GestionForm>(
    "gestionar-usuario",
    { accion: "", motivo: "" },
    (v): Errores<GestionForm> => ({
      accion: seleccion(v.accion),
      motivo: largo(v.motivo, 10, 200),
    }),
  );

  const gestionado = usuarios.find((u) => u.id === gestionadoId) ?? null;
  const texto = busqueda.trim().toLowerCase();
  // Filtra las cuentas por el texto de búsqueda y las opciones seleccionadas.
  const visibles = usuarios.filter(
    (u) =>
      (texto === "" ||
        u.usuario.toLowerCase().includes(texto) ||
        u.correo.toLowerCase().includes(texto)) &&
      (rol === "" || u.rol === rol) &&
      (estado === "" || (estado === "suspendido") === u.suspendido),
  );

  function cerrar() {
    setGestionadoId(null);
    form.reiniciar();
  }

  // Permite verificar, suspender o reactivar una cuenta. Evita que un administrador suspenda su propia cuenta.
  function guardar(e: FormEvent) {
    e.preventDefault();
    void ejecutar(async () => {
      if (!gestionado || !form.validar()) return;
      const { accion, motivo } = form.valores;
      if (accion === "suspender") {
        if (gestionado.id === yo?.id) {
          setAviso("No puedes suspender tu propia cuenta.");
          cerrar();
          return;
        }
        await actualizarUsuario(gestionado.id, { suspendido: true, motivo });
        setAviso(`La cuenta de ${gestionado.usuario} fue suspendida.`);
      } else if (accion === "reactivar") {
        await actualizarUsuario(gestionado.id, { suspendido: false, motivo });
        setAviso(`La cuenta de ${gestionado.usuario} fue reactivada.`);
      } else {
        await actualizarUsuario(gestionado.id, { verificado: accion === "verificar", motivo });
        setAviso(accion === "verificar" ? `${gestionado.usuario} ahora está verificado.` : `Se quitó la verificación de ${gestionado.usuario}.`);
      }
      cerrar();
    });
  }

  if (cargando.usuarios) return <p className="container py-4">Cargando usuarios…</p>;
  return (
    <div className={integrado ? "" : "container py-4"}>
      {!integrado && <Migas
        items={[
          { texto: "Inicio", a: "/" },
          { texto: "Administración", a: "/admin/panel" },
          { texto: "Usuarios" },
        ]}
      />}
      <Titulo className="h4 mb-4">Usuarios</Titulo>
      <div className="row g-4">
        {!integrado && <MenuCuenta tipo="admin" activo="/admin/usuarios" />}
        <div className={integrado ? "col-12" : "col-lg-9"}>
          {aviso && (
            <div className="alert alert-success" role="status">
              {aviso}
            </div>
          )}
          <form
            className="filtros mb-4"
            aria-label="Filtrar usuarios"
            onSubmit={(e) => e.preventDefault()}
          >
            <div className="row g-3 align-items-end">
              <div className="col-md-4">
                <label htmlFor="buscar-usuario" className="form-label">
                  Buscar por nombre o correo{" "}
                  <span className="text-body-secondary fw-normal">
                    (opcional)
                  </span>
                </label>
                <input
                  type="text"
                  className="form-control"
                  id="buscar-usuario"
                  placeholder="penquista_gamer"
                  value={busqueda}
                  onChange={(e) => setBusqueda(e.target.value)}
                />
              </div>
              <div className="col-md-4">
                <label htmlFor="rol-usuario" className="form-label">
                  Rol{" "}
                  <span className="text-body-secondary fw-normal">
                    (opcional)
                  </span>
                </label>
                <select
                  className="form-select"
                  id="rol-usuario"
                  value={rol}
                  onChange={(e) => setRol(e.target.value)}
                >
                  <option value="">Todos</option>
                  <option value="cliente">Cliente</option>
                  
                  <option value="admin">Administrador</option>
                </select>
              </div>
              <div className="col-md-4">
                <label htmlFor="estado-usuario" className="form-label">
                  Estado{" "}
                  <span className="text-body-secondary fw-normal">
                    (opcional)
                  </span>
                </label>
                <select
                  className="form-select"
                  id="estado-usuario"
                  value={estado}
                  onChange={(e) => setEstado(e.target.value)}
                >
                  <option value="">Todos</option>
                  <option value="activo">Activo</option>
                  <option value="suspendido">Suspendido</option>
                </select>
              </div>
            </div>
          </form>
          <div className="table-responsive">
            <table className="table align-middle">
              <caption>
                {visibles.length} de {usuarios.length} cuentas registradas
              </caption>
              <thead>
                <tr>
                  <th scope="col">Usuario</th>
                  <th scope="col">Correo</th>
                  <th scope="col">Rol</th>
                  <th scope="col">Estado</th>
                  <th scope="col">
                    <span className="visually-hidden">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {visibles.map((u) => (
                  <tr key={u.id}>
                    <td>{u.usuario}</td>
                    <td>{u.correo}</td>
                    <td>{etiquetaRol(u)}</td>
                    <td>
                      <span
                        className={`badge ${u.suspendido ? "text-bg-danger" : "text-bg-success"}`}
                      >
                        {u.suspendido ? "Suspendido" : "Activo"}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-light"
                        onClick={() => {
                          setAviso("");
                          setGestionadoId(u.id);
                        }}
                      >
                        Gestionar
                      </button>
                    </td>
                  </tr>
                ))}
                {visibles.length === 0 && (
                  <tr>
                    <td colSpan={5} className="text-center text-body-secondary">
                      No hay usuarios con esos filtros.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <Modal
        abierto={gestionado !== null}
        titulo={
          gestionado ? `Gestionar a ${gestionado.usuario}` : "Gestionar usuario"
        }
        onCerrar={cerrar}
      >
        <form onSubmit={guardar} noValidate>
          <Campo
            form={form}
            nombre="accion"
            tipo="select"
            etiqueta="Acción"
            opciones={ACCIONES.filter(a => a.valor === "verificar" ? !gestionado?.verificado : a.valor === "desverificar" ? gestionado?.verificado : a.valor === "suspender" ? !gestionado?.suspendido : gestionado?.suspendido)}
            textoOpcionVacia="Selecciona una acción"
          />
          <Campo
            form={form}
            nombre="motivo"
            tipo="textarea"
            maxLength={200}
            etiqueta="Motivo de la gestión"
            ayuda={
              <>
                Mínimo 10 caracteres.
                <span className="float-end">
                  {form.valores.motivo.length}/200
                </span>
              </>
            }
          />
          <div className="d-flex gap-2">
            <button type="submit" disabled={ocupado} className="btn btn-primary">
              Guardar
            </button>
            <button
              type="button"
              className="btn btn-outline-light"
              onClick={cerrar}
            >
              Cancelar
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
