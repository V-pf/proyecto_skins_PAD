import Migas from "../components/Migas";
export default function Contacto() {
  return (
    <div className="container py-4">
      <Migas items={[{ texto: "Inicio", a: "/" }, { texto: "Ayuda" }]} />
      <h1>Ayuda</h1>
      <p>
        Elige una skin y agrégala al carrito. Cada publicación corresponde a una
        única unidad.
      </p>
      <p>
        En Mi cuenta → Mis pagos puedes revisar el estado de tu pago y registrar
        un comprobante de transferencia. El intercambio se realiza en Steam.
      </p>
    </div>
  );
}
