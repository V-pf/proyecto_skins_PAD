import { Link } from "react-router-dom";

export default function VolverPanel() {
  return <Link to="/cliente/panel" className="volver-panel">
    <span aria-hidden="true" />Volver a mi panel
  </Link>;
}
