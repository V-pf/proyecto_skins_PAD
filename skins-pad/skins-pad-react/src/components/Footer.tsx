import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="sitio-footer mt-5 py-5">
      <div className="container">
        <div className="row g-4">
          <div className="col-md-5">
            <p className="h5 mb-2">Skins PAD</p>
            <p className="small text-body-secondary mb-0">
              Marketplace de skins de CS:GO con precios en pesos chilenos. Proyecto académico, sin afiliación con Valve
              Corporation. Counter-Strike y sus skins son marcas de Valve.
            </p>
          </div>
          <nav className="col-6 col-md-3" aria-label="Enlaces de la tienda">
            <p className="h6">Tienda</p>
            <ul className="list-unstyled small mb-0">
              <li>
                <Link to="/catalogo">Catálogo</Link>
              </li>
              <li>
                <Link to="/cliente/vender">Vender una skin</Link>
              </li>
            </ul>
          </nav>
          <nav className="col-6 col-md-4" aria-label="Enlaces de ayuda">
            <p className="h6">Ayuda</p>
            <ul className="list-unstyled small mb-0">
              <li>
                <a href="mailto:soporte@skinspad.example">soporte@skinspad.example</a>
              </li>
            </ul>
          </nav>
        </div>
        <p className="small text-body-secondary border-top pt-3 mt-4 mb-0">
          © 2026 Skins PAD. Proyecto académico.
        </p>
      </div>
    </footer>
  );
}
