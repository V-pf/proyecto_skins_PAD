import VolverPanel from "../components/VolverPanel";
import CampoSteam from "../components/CampoSteam";
import { errorSteam, urlSteam } from "../lib/enlaceSteam";
import { useOperacion } from "../lib/useOperacion";
import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import Campo from "../components/Campo";
import Migas from "../components/Migas";
import { useAuth } from "../context/AuthContext";
import { useTienda } from "../context/TiendaContext";
import { GRADOS, TIPOS, desgastePorFloat } from "../lib/catalogos";
import { useFormulario } from "../lib/useFormulario";
import type { Errores } from "../lib/useFormulario";
import { aceptado, requerido, seleccion } from "../lib/validaciones";
import type { Grado, Tipo } from "../types";

interface VenderForm {
  [clave: string]: string | boolean;
  nombre: string;
  tipo: string;
  categoria: string;
  grado: string;
  float: string;
  stattrak: boolean;
  precio: string;
  urlIntercambio: string;
  descripcion: string;
  acepta: boolean;
}

const INICIAL: VenderForm = {
  nombre: "",
  tipo: "",
  categoria: "",
  grado: "",
  float: "",
  stattrak: false,
  precio: "",
  urlIntercambio: "",
  descripcion: "",
  acepta: false,
};

export default function Vender() {
  const [resultado, setResultado] = useState<{
    tipo: "exito" | "error" | "validacion";
    mensaje: string;
  } | null>(null);
  const aviso = useRef<HTMLDivElement>(null);
  const { ejecutar, ocupado } = useOperacion(mensaje => setResultado({ tipo: "error", mensaje }));
  useEffect(() => {
    if (resultado) aviso.current?.scrollIntoView({ block: "nearest" });
  }, [resultado]);
  const { usuario } = useAuth();
  const { crearProducto, categorias, cargando } = useTienda();

  const form = useFormulario<VenderForm>(
    "vender",
    INICIAL,
    (v): Errores<VenderForm> => ({
      nombre:
        v.nombre.trim().length < 3
          ? v.nombre.trim() === ""
            ? "Este campo es obligatorio."
            : "Escribe al menos 3 caracteres."
          : "",
      categoria: seleccion(v.categoria),
      tipo: seleccion(v.tipo),
      grado: seleccion(v.grado),
      float: v.tipo === "sticker" ? "" : requerido(v.float, "float_desgaste"),
      precio: requerido(v.precio, "precio"),
      urlIntercambio: errorSteam("intercambio", v.urlIntercambio),
      acepta: aceptado(v.acepta),
    }),
  );

  if (!usuario) return null;
  // La cuenta debe estar verificada antes de usar el formulario de publicación.
  if (!usuario.verificado) return <div className="container py-4"><VolverPanel /><p>Tu cuenta debe ser verificada por administración antes de publicar.</p></div>;
  if (cargando.categorias) return <p className="container py-4">Cargando categorías…</p>;

  function publicar(e: FormEvent) {
    e.preventDefault();
    void ejecutar(async () => {
      if (!usuario) return;
      if (!form.validar()) {
        setResultado({ tipo: "validacion", mensaje: "Hay campos por corregir. Revisa los mensajes junto a cada campo." });
        return;
      }
      setResultado(null);
      const v = form.valores;
      const tipo = v.tipo as Tipo;
      // Los stickers no tienen desgaste. En las otras skins se calcula a partir del float.
      const float = tipo === "sticker" ? null : Number(v.float);
      // La publicación se envía como pendiente para que la revise administración.
      const nueva = await crearProducto({
        nombre: v.nombre.trim(),
        tipo,
        categoriaId:Number(v.categoria),
        grado: v.grado as Grado,
        desgaste: float === null ? "na" : desgastePorFloat(float),
        precio: Math.round(Number(v.precio)),
        stattrak: v.stattrak,
        float,
        vendedor: usuario.usuario,
        urlIntercambio: urlSteam("intercambio", v.urlIntercambio),
        estado: "pendiente",
        descripcion: v.descripcion.trim() || undefined,
      });

      setResultado({ tipo: "exito", mensaje: `«${nueva.nombre}» quedó enviada a revisión.` });
      form.reiniciar();
    });
  }

  return (
    <div className="container py-4">
      <Migas
        items={[
          { texto: "Inicio", a: "/" },
          { texto: "Mi cuenta", a: "/cliente/panel" },
          { texto: "Vender una skin" },
        ]}
      />
      <VolverPanel />
      <h1 className="mb-2">Vender una skin</h1>
      <p className="text-body-secondary mb-4" style={{ maxWidth: "70ch" }}>
        Completa los datos del ítem tal como aparecen en tu inventario de Steam.
        Revisamos cada publicación antes de que quede visible en el catálogo.
      </p>
      <div className="row g-4">
        <div className="col-12">
          <form onSubmit={publicar} noValidate>
            <fieldset className="mb-4">
              <legend className="h6">Datos de la skin</legend>
              <Campo form={form} nombre="categoria" tipo="select" etiqueta="Categoría" opciones={categorias.filter(c=>c.estadoHabilitada).map(c=>({valor:String(c.id),etiqueta:c.nombre}))} textoOpcionVacia="Selecciona una categoría"/>
              <Campo
                form={form}
                nombre="nombre"
                etiqueta="Nombre exacto de la skin"
                placeholder="AK-47 | Redline"
              />
              <Campo
                form={form}
                nombre="tipo"
                tipo="select"
                etiqueta="Tipo de arma"
                opciones={TIPOS.map((t) => ({
                  valor: t.valor,
                  etiqueta: t.etiqueta,
                }))}
                textoOpcionVacia="Selecciona un tipo"
              />
              <Campo
                form={form}
                nombre="grado"
                tipo="select"
                etiqueta="Grado de rareza"
                opciones={GRADOS.map((g) => ({
                  valor: g.valor,
                  etiqueta: g.etiqueta,
                }))}
                textoOpcionVacia="Selecciona un grado"
              />
              <Campo
                form={form}
                nombre="float"
                tipo="number"
                etiqueta="Valor de float"
                placeholder="0.2461"
                step="0.0001"
                min="0"
                max="1"
                ayuda="Un número entre 0,0000 y 1,0000. Lo encuentras en el inventario de Steam."
              />
              <Campo
                form={form}
                nombre="stattrak"
                tipo="checkbox"
                etiqueta="Es StatTrak™"
              />
            </fieldset>
            <fieldset className="mb-4">
              <legend className="h6">Precio</legend>
              <Campo
                form={form}
                nombre="precio"
                tipo="number"
                etiqueta="Precio de venta (CLP)"
                placeholder="28990"
                step="10"
                ayuda="Entre $100 y $50.000.000."
              />
              <CampoSteam form={form} nombre="urlIntercambio" tipo="intercambio" />
            </fieldset>
            <Campo
              form={form}
              nombre="descripcion"
              tipo="textarea"
              maxLength={300}
              etiqueta={
                <>
                  Descripción para el comprador{" "}
                  <span className="text-body-secondary fw-normal">
                    (opcional)
                  </span>
                </>
              }
              ayuda={
                <>
                  Cuenta detalles como el patrón, si tiene stickers o si es de
                  una caída especial.
                  <span className="float-end">
                    {form.valores.descripcion.length}/300
                  </span>
                </>
              }
            />
            <Campo
              form={form}
              nombre="acepta"
              tipo="checkbox"
              etiqueta="Entiendo que Skins PAD acredita mi venta al aprobarse el pago y suma un 5% de comisión al total del comprador."
            />
            <button type="submit" className="btn btn-primary" disabled={ocupado}>
              {ocupado ? "Publicando…" : "Publicar skin"}
            </button>
            {resultado && (resultado.tipo !== "validacion" || Object.keys(form.errores).length > 0) && (
              <div ref={aviso} className={`alert mt-3 ${resultado.tipo === "exito" ? "alert-success" : "alert-danger"}`}
                role={resultado.tipo === "exito" ? "status" : "alert"}>
                {resultado.mensaje}{" "}
                {resultado.tipo === "exito" && <Link to="/cliente/publicaciones">Ver mis publicaciones</Link>}
              </div>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}
