import {Link} from "react-router-dom";
import Migas from "../components/Migas";
export default function Contacto() {
  return (
    <div className="container py-4">
      <Migas items={[{ texto: "Inicio", a: "/" }, { texto: "Ayuda" }]} />
      <h1>Ayuda</h1><p><Link className="btn btn-primary" to="/cliente/soporte">Abrir soporte y consultar mis tickets</Link></p>
      <p>
        Elige una skin y agrégala al carrito. Cada publicación corresponde a una
        única unidad.
      </p>
      <p>
        Consulta el estado de tus pagos en Mis pagos y el de tus entregas en Mis compras.
      </p>
    </div>
  );
}
