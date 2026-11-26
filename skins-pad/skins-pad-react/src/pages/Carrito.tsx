import { useOperacion } from "../lib/useOperacion";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Migas from "../components/Migas";
import Modal from "../components/Modal";
import { useAuth } from "../context/AuthContext";
import { useCarrito } from "../context/CarritoContext";
import { useTienda } from "../context/TiendaContext";
import { DESGASTES, imagenDe } from "../lib/catalogos";
import { formatoCLP } from "../lib/formato";

export default function Carrito() {
  const { ejecutar, ocupado } = useOperacion();
  const { usuario } = useAuth();
  const { pagar, cargando: cargaTienda } = useTienda();
  const { lineas, totales, cantidadTotal, quitar, vaciar, cargando } =
    useCarrito();
  const navigate = useNavigate();
  const [modalAbierto, setModalAbierto] = useState(false);
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
      if (usuario.saldo < totales.total) {
        setError(
          `Tu saldo (${formatoCLP(usuario.saldo)}) no alcanza para pagar ${formatoCLP(totales.total)}.`,
        );
        return;
      }
      // Primero se registra el pago. Si luego falla el vaciado, se avisa que el pago ya quedó registrado.
      const pago = await pagar(lineas.map((l) => l.producto.id));
      try {
        await vaciar();
      } catch {
        window.alert(
          "El pago se registró. Revisa el carrito antes de volver a comprar.",
        );
      }
      setModalAbierto(false);
      navigate("/cliente/compras", {
        state: { mensaje: pago.estado === "PENDIENTE"
          ? `El pago #${pago.id} sigue procesándose.`
          : `Compra #${pago.id} registrada. Ya está disponible para el vendedor.` },
      });
    });
  }

  if (usuario && (cargando || cargaTienda.productos)) return <p className="container py-4">Cargando carrito…</p>;
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
      <Modal
        abierto={modalAbierto}
        titulo="Confirmar pago con saldo"
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
              disabled={ocupado || lineas.length === 0}
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
        {usuario && <p>Saldo disponible en tu billetera <strong>{formatoCLP(usuario.saldo)}</strong></p>}
        <p className="small text-body-secondary mt-3 mb-0">
          Total a pagar: <strong>{formatoCLP(totales.total)}</strong>
        </p>
      </Modal>
    </div>
  );
}
