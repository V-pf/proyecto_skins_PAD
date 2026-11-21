import { useOperacion } from "../lib/useOperacion";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import MenuCuenta from "../components/MenuCuenta";
import Migas from "../components/Migas";
import Modal from "../components/Modal";
import { useAuth } from "../context/AuthContext";
import { useCarrito } from "../context/CarritoContext";
import { useTienda } from "../context/TiendaContext";
import { DESGASTES, imagenDe } from "../lib/catalogos";
import { formatoCLP, hoyISO } from "../lib/formato";

const METODOS = [
  { valor: "Tarjeta", etiqueta: "Tarjeta de Crédito / Débito" },
  { valor: "Transferencia", etiqueta: "Transferencia Bancaria" },
  { valor: "Mercado Pago", etiqueta: "Mercado Pago / Wallet" },
  { valor: "Saldo", etiqueta: "Saldo en la billetera Skins PAD" },
];

export default function Carrito() {
  const { ejecutar, ocupado } = useOperacion();
  const { usuario } = useAuth();
  const { pagar, cargando: cargaTienda } = useTienda();
  const { lineas, totales, cantidadTotal, quitar, vaciar, cargando } =
    useCarrito();
  const navigate = useNavigate();
  const [modalAbierto, setModalAbierto] = useState(false);
  const [metodo, setMetodo] = useState("Tarjeta");
  const [error, setError] = useState("");

  // Si falta la sesión, se recuerda el carrito para volver después de entrar.
  function irAPagar() {
    if (!usuario) {
      navigate("/login", { state: { desde: "/cliente/carrito" } });
      return;
    }
    setError("");
    setModalAbierto(true);
  }

  function confirmarPago() {
    void ejecutar(async () => {
      if (!usuario || lineas.length === 0) return;
      // El total del carrito sirve para avisar de saldo insuficiente antes de enviar la compra.
      if (metodo === "Saldo") {
        if (usuario.saldo < totales.total) {
          setError(
            `Tu saldo (${formatoCLP(usuario.saldo)}) no alcanza para pagar ${formatoCLP(totales.total)}.`,
          );
          return;
        }
      }
      // Primero se registra el pago. Si luego falla el vaciado, se avisa que el pago ya quedó registrado.
      const pago = await pagar(
        lineas.map((l) => l.producto.id),
        metodo,
      );
      try {
        await vaciar();
      } catch {
        window.alert(
          "El pago se registró. Revisa el carrito antes de volver a comprar.",
        );
      }
      setModalAbierto(false);
      navigate("/cliente/compras", {
        state: { mensaje: "Pago #" + pago.id + " registrado: " + pago.estado },
      });
    });
  }

  if (usuario && (cargando || cargaTienda.productos)) return <p className="container py-4">Cargando carrito…</p>;
  const contenido = (
    <>
      {lineas.some((l) => l.producto.estado !== "activa") && (
        <p className="alert alert-warning">
          Quita las skins que ya no están disponibles para continuar.
        </p>
      )}
      {lineas.length === 0 ? (
        <div className="stat-tile text-center py-5">
          <p className="h5">Tu carrito está vacío</p>
          <p className="text-body-secondary">
            Explora el catálogo y agrega las skins que te interesen.
          </p>
          <Link to="/catalogo" className="btn btn-primary">
            Ir al catálogo
          </Link>
        </div>
      ) : (
        <>
          <div className="table-responsive mb-4">
            <table className="table align-middle">
              <caption>
                {cantidadTotal} ítem{cantidadTotal === 1 ? "" : "s"} en el
                carrito
              </caption>
              <thead>
                <tr>
                  <th scope="col">Skin</th>
                  <th scope="col">Exterior</th>

                  <th scope="col">Precio</th>
                  <th scope="col">
                    <span className="visually-hidden">Quitar</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {lineas.map(({ producto, cantidad }) => (
                  <tr key={producto.id}>
                    <td>
                      <div className="d-flex align-items-center gap-3">
                        <img
                          src={producto.imagenes?.[0]?.urlImagen || imagenDe(producto.tipo)}
                          alt=""
                          width={64}
                          height={40}
                        />
                        <Link to={`/producto/${producto.id}`}>
                          {producto.nombre}
                        </Link>
                      </div>
                    </td>
                    <td>{DESGASTES[producto.desgaste].nombre}</td>
                    <td>{formatoCLP(producto.precio * cantidad)}</td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-light"
                        onClick={() => void ejecutar(() => quitar(producto.id))}
                      >
                        Quitar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="row justify-content-between align-items-start">
            <div className="col-md-5 mb-3">
              <button
                type="button"
                className="btn btn-sm btn-link p-0"
                onClick={() => { if (window.confirm("¿Vaciar todo el carrito?")) void ejecutar(vaciar); }}
              >
                Vaciar carrito
              </button>
            </div>
            <div className="col-md-5">
              <dl className="row">
                <dt className="col-7">Subtotal</dt>
                <dd className="col-5 text-end">
                  {formatoCLP(totales.subtotal)}
                </dd>
                <dt className="col-7">Comisión de la plataforma (5%)</dt>
                <dd className="col-5 text-end">
                  {formatoCLP(totales.comision)}
                </dd>
                <dt className="col-7 fw-semibold">Total</dt>
                <dd className="col-5 text-end fw-semibold precio">
                  {formatoCLP(totales.total)}
                </dd>
              </dl>
              <button
                type="button"
                className="btn btn-primary w-100"
                disabled={
                  ocupado || lineas.some((l) => l.producto.estado !== "activa")
                }
                onClick={irAPagar}
              >
                Ir a pagar
              </button>
              {!usuario && (
                <p className="small text-body-secondary mt-2 mb-0">
                  Te pediremos iniciar sesión para pagar.
                </p>
              )}
            </div>
          </div>
        </>
      )}
    </>
  );

  return (
    <div className="container py-4">
      <Migas
        items={[
          { texto: "Inicio", a: "/" },
          ...(usuario ? [{ texto: "Mi cuenta", a: "/cliente/panel" }] : []),
          { texto: "Carrito" },
        ]}
      />
      <h1 className="mb-4">Carrito</h1>
      {usuario ? (
        <div className="row g-4">
          <MenuCuenta tipo="cliente" activo="/cliente/carrito" />
          <div className="col-lg-9">{contenido}</div>
        </div>
      ) : (
        contenido
      )}

      <Modal
        abierto={modalAbierto}
        titulo="Selecciona tu método de pago"
        onCerrar={() => setModalAbierto(false)}
        pie={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setModalAbierto(false)}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="btn btn-success"
              onClick={confirmarPago}
            >
              Pagar y finalizar
            </button>
          </>
        }
      >
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        <p className="fw-bold mb-2">Método de pago:</p>
        {METODOS.map((m) => (
          <div className="form-check mb-2" key={m.valor}>
            <input
              className="form-check-input"
              type="radio"
              name="metodoPago"
              id={`metodo-${m.valor}`}
              checked={metodo === m.valor}
              onChange={() => setMetodo(m.valor)}
            />
            <label className="form-check-label" htmlFor={`metodo-${m.valor}`}>
              {m.etiqueta}
              {m.valor === "Saldo" && usuario
                ? ` (${formatoCLP(usuario.saldo)} disponibles)`
                : ""}
            </label>
          </div>
        ))}
        <p className="small text-body-secondary mt-3 mb-0">
          Total a pagar: <strong>{formatoCLP(totales.total)}</strong>
        </p>
      </Modal>
    </div>
  );
}
