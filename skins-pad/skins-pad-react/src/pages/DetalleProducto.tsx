import { metodosCompra } from "../lib/metodosCompra";
import { useOperacion } from "../lib/useOperacion";
import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import Campo from "../components/Campo";
import ListaProductos from "../components/ListaProductos";
import Migas from "../components/Migas";
import { useAuth } from "../context/AuthContext";
import { useCarrito } from "../context/CarritoContext";
import { useTienda } from "../context/TiendaContext";
import { useToast } from "../context/ToastContext";
import { ESTADO_PUBLICACION, DESGASTES, etiquetaTipo, imagenDe, infoGrado } from "../lib/catalogos";
import {
  calcularTotales,
  formatoCLP,
  formatoFloat,
  hoyISO,
} from "../lib/formato";
import { useFormulario } from "../lib/useFormulario";
import type { Errores } from "../lib/useFormulario";
import { requerido, seleccion } from "../lib/validaciones";

interface CompraForm {
  [clave: string]: string | boolean;
  nombreSteam: string;

  metodoPago: string;
}

export default function DetalleProducto() {
  const { ejecutar, ocupado } = useOperacion();
  const { id } = useParams();
  const navigate = useNavigate();
  const { productos, usuarios, pagar, cargando } = useTienda();
  const { usuario } = useAuth();
  const { agregar } = useCarrito();
  const { mostrar } = useToast();

  const [errorCompra, setErrorCompra] = useState("");

  const producto = productos.find(
    (p) => p.id === Number(id),
  );

  const form = useFormulario<CompraForm>(
    "oferta",
    {
      nombreSteam: usuario?.usuario ?? "",

      metodoPago: "",
    },
    (v): Errores<CompraForm> => ({
      nombreSteam: requerido(v.nombreSteam, "usuario"),

      metodoPago: seleccion(v.metodoPago),
    }),
  );

  if (cargando.productos) return <p className="container py-4">Cargando catálogo…</p>;
  if (!producto) {
    return (
      <div className="container py-5 text-center">
        <h1>Skin no encontrada</h1>
        <p className="text-body-secondary">
          Esta publicación no existe o ya no está disponible.
        </p>
        <Link className="btn btn-primary" to="/catalogo">
          Volver al catálogo
        </Link>
      </div>
    );
  }

  if(producto.estado!=="activa")return <main className="container py-5"><h1>{producto.nombre}</h1><p>Publicación {ESTADO_PUBLICACION[producto.estado].texto.toLowerCase()}. Esta skin ya no está disponible para comprar.</p><Link to="/catalogo">Volver al catálogo</Link></main>;
  const grado = infoGrado(producto.grado);
  const desgaste = DESGASTES[producto.desgaste];
  const vendedor = usuarios.find((u) => u.usuario === producto.vendedor);
  const posicionFloat =
    producto.float === null
      ? null
      : Math.min(100, Math.max(0, producto.float * 100));
  // Busca otras publicaciones activas y da prioridad al mismo tipo de skin.
  const relacionados = productos
    .filter((p) => p.estado === "activa" && p.id !== producto.id)
    .sort(
      (a, b) =>
        Number(b.tipo === producto.tipo) - Number(a.tipo === producto.tipo) ||
        a.id - b.id,
    )
    .slice(0, 3);

  function agregarAlCarrito() {
    void ejecutar(async () => {
      if (!producto) return;
      await agregar(producto.id);
      mostrar(`${producto.nombre} en tu carrito.`);
    });
  }

  // Valida el formulario y registra una compra de esta publicación sin pasar por el carrito.
  function comprar(e: FormEvent) {
    e.preventDefault();
    void ejecutar(async () => {
      if (!producto) return;
      setErrorCompra("");
      if (!form.validar()) return;
      if (!usuario) {
        navigate("/login", { state: { desde: `/producto/${producto.id}` } });
        return;
      }
      const totales = calcularTotales(producto.precio);
      if (form.valores.metodoPago === "SALDO") {
        if (usuario.saldo < totales.total) {
          setErrorCompra(
            `Tu saldo (${formatoCLP(usuario.saldo)}) no alcanza para pagar ${formatoCLP(totales.total)}.`,
          );
          return;
        }
      }
      const pago = await pagar([producto.id], form.valores.metodoPago);
      navigate("/cliente/pagos", {
        state: { mensaje: "Pago #" + pago.id + " registrado: " + pago.estado },
      });
    });
  }

  return (
    <div className="container py-4">
      <Migas
        items={[
          { texto: "Inicio", a: "/" },
          { texto: "Catálogo", a: "/catalogo" },
          {
            texto: etiquetaTipo(producto.tipo),
            a: `/catalogo?tipo=${producto.tipo}`,
          },
          { texto: producto.nombre },
        ]}
      />
      <div className="row g-5">
        <div className="col-lg-6">
          <figure className="detalle-figura p-4 m-0 text-center">
            <img
              src={producto.imagenes?.[0]?.urlImagen || imagenDe(producto.tipo)}
              alt={`Ilustración de ejemplo de ${producto.nombre}`}
              width={480}
              height={300}
              className="img-fluid"
            />
            <figcaption className="mt-2 text-body-secondary small">
              Imagen ilustrativa. El aspecto final depende del acabado real del
              ítem.
            </figcaption>
          </figure>
          {producto.imagenes?.slice(1).map(i=><img key={i.id} src={i.urlImagen} alt={producto.nombre} className="img-fluid mb-2" />)}
          <section className="mt-4" aria-labelledby="tit-vendedor">
            <h2 id="tit-vendedor" className="h6">
              Sobre el vendedor
            </h2>
            <aside className="stat-tile">
              <address className="mb-2" style={{ fontStyle: "normal" }}>
                <p className="mb-1">
                  <Link to={`/perfil/${producto.vendedorId}`}>{producto.vendedor}</Link>{" "}
                  {vendedor?.verificado && (
                    <span className="badge text-bg-success">Verificado</span>
                  )}
                </p>
                <p className="small text-body-secondary mb-0">
                  {
                    productos.filter(
                      (p) =>
                        p.vendedor === producto.vendedor &&
                        p.estado === "activa",
                    ).length
                  }{" "}
                  publicaciones activas
                </p>
              </address>
              <Link className="btn btn-sm btn-outline-light" to="/contacto">
                Hacer una pregunta
              </Link>
            </aside>
          </section>
        </div>

        <div className="col-lg-6">
          <p className="badge-grado mb-2">{grado.insignia}</p>
          <h1 className="mb-1">
            {producto.nombre}{" "}
            {producto.stattrak && (
              <span className="badge text-bg-warning align-middle">
                StatTrak™
              </span>
            )}
          </h1>
          {producto.coleccion && (
            <p className="text-body-secondary mb-3">
              Colección {producto.coleccion}
            </p>
          )}
          <p className="precio-hero display-6 mb-4">
            {formatoCLP(producto.precio)}
          </p>

          <dl className="row small mb-4">
            <dt className="col-5">Grado de rareza</dt>
            <dd className="col-7">
              {grado.insignia} — {grado.descripcion}
            </dd>
            <dt className="col-5">Exterior</dt>
            <dd className="col-7">{desgaste.nombre}</dd>
            <dt className="col-5">Float</dt>
            <dd className="col-7">{formatoFloat(producto.float)}</dd>
            <dt className="col-5">StatTrak™</dt>
            <dd className="col-7">{producto.stattrak ? "Sí" : "No"}</dd>
          </dl>

          {posicionFloat !== null && (
            <div className="mb-4">
              <div className="d-flex justify-content-between small text-body-secondary mb-1">
                <span>Recién fabricado</span>
                <span>Deplorable</span>
              </div>
              <div className="barra-desgaste">
                <span
                  className="marca"
                  style={{ left: `${posicionFloat}%` }}
                  aria-hidden="true"
                ></span>
              </div>
              <p className="small text-body-secondary mt-1 mb-0">
                Rango de este exterior: {desgaste.rango}
              </p>
            </div>
          )}

          {producto.descripcion && <p>{producto.descripcion}</p>}
          <div className="d-flex align-items-center gap-3 mb-4">
            <button
              type="button"
              className="btn btn-primary btn-lg flex-fill"
              onClick={agregarAlCarrito}
            >
              Agregar al carrito
            </button>
          </div>

          <details className="mb-4">
            <summary>Detalles de la entrega</summary>
            <p className="small text-body-secondary mt-2 mb-0">
              El intercambio se realiza en Steam. Puedes consultar el estado del
              pago en Mis pagos.
            </p>
          </details>

          <section aria-labelledby="tit-oferta">
            <h2 id="tit-oferta" className="h5">
              Comprar ahora
            </h2>
            <form className="mt-4" onSubmit={comprar} noValidate>
              {form.intento && Object.keys(form.errores).length > 0 && (
                <div className="alert alert-danger" role="alert">
                  Hay campos por corregir. Revisa los mensajes marcados en rojo.
                </div>
              )}
              {errorCompra && (
                <div className="alert alert-danger" role="alert">
                  {errorCompra}
                </div>
              )}
              <Campo
                form={form}
                nombre="nombreSteam"
                etiqueta="Tu nombre en Steam"
                placeholder="ej: penquista_gamer"
              />
              <p>El comprobante se enviará al correo de tu cuenta.</p>
              <Campo
                form={form}
                nombre="metodoPago"
                etiqueta="Método de pago"
                tipo="select"
                opciones={metodosCompra(usuario?.saldo ?? 0)}
                textoOpcionVacia="Selecciona un medio"
              />
              <p className="small text-body-secondary">
                {formatoCLP(producto.precio)} + 5% de comisión ={" "}
                <strong>
                  {formatoCLP(
                    calcularTotales(producto.precio).total,
                  )}
                </strong>
              </p>
              <button type="submit" className="btn btn-primary">
                Confirmar compra
              </button>
            </form>
          </section>
        </div>
      </div>

      <section className="mt-5" aria-labelledby="tit-relacionados">
        <h2 id="tit-relacionados" className="mb-3">
          También te puede interesar
        </h2>
        <ListaProductos
          productos={relacionados}
          columnas="row-cols-1 row-cols-sm-3"
        />
      </section>
    </div>
  );
}
