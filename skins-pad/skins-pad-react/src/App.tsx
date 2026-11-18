import { Link, Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import RutaProtegida from "./components/RutaProtegida";
import Home from "./pages/Home";
import Catalogo from "./pages/Catalogo";
import DetalleProducto from "./pages/DetalleProducto";
import Contacto from "./pages/Contacto";
import Login from "./pages/Login";
import Registro from "./pages/Registro";
import Carrito from "./pages/Carrito";
import PanelCliente from "./pages/PanelCliente";
import Operaciones from "./pages/Operaciones";
import Pagos from "./pages/Pagos";
import Vender from "./pages/Vender";
import MisPublicaciones from "./pages/MisPublicaciones";
import AdminPanel from "./pages/AdminPanel";

import Billetera from "./pages/Billetera";
import Perfil from "./pages/Perfil";
import PerfilPublico from "./pages/PerfilPublico";
function NoEncontrada() {
  return (
    <div className="container py-5 text-center">
      <h1>Página no encontrada</h1>
      <p className="text-body-secondary">La dirección que buscas no existe.</p>
      <Link className="btn btn-primary" to="/">
        Volver al inicio
      </Link>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/catalogo" element={<Catalogo />} />
        <Route path="/producto/:id" element={<DetalleProducto />} />
        <Route path="/contacto" element={<Contacto />} />
        <Route path="/login" element={<Login />} />
        <Route path="/registro" element={<Registro />} />

        <Route path="/perfil/:id" element={<PerfilPublico/>}/>
        <Route path="/cliente/billetera" element={<RutaProtegida><Billetera/></RutaProtegida>}/>
        <Route path="/cliente/perfil" element={<RutaProtegida><Perfil/></RutaProtegida>}/>
        <Route path="/cliente/carrito" element={<Carrito />} />
        <Route path="/cliente/panel" element={<RutaProtegida><PanelCliente /></RutaProtegida>} />
        <Route path="/cliente/compras" element={<RutaProtegida><Operaciones vista="compras"/></RutaProtegida>}/>
        <Route path="/cliente/ventas" element={<RutaProtegida><Operaciones vista="ventas"/></RutaProtegida>}/>
        <Route path="/cliente/pagos" element={<RutaProtegida><Pagos /></RutaProtegida>} />
        <Route path="/cliente/vender" element={<RutaProtegida><Vender /></RutaProtegida>} />
        <Route path="/cliente/publicaciones" element={<RutaProtegida><MisPublicaciones /></RutaProtegida>} />

        {(["estadisticas", "pagos", "web"] as const).map(seccion =>
          <Route key={seccion} path={`/admin/${seccion}`} element={<RutaProtegida soloAdmin><AdminPanel seccion={seccion}/></RutaProtegida>}/>
        )}
        <Route path="/admin/panel" element={<RutaProtegida soloAdmin><Navigate to="/admin/estadisticas" replace/></RutaProtegida>}/>
        <Route path="/admin/ventas" element={<RutaProtegida soloAdmin><Navigate to="/admin/estadisticas" replace/></RutaProtegida>}/>
        {(["usuarios", "publicaciones", "categorias"] as const).map(seccion =>
          <Route key={seccion} path={`/admin/${seccion}`} element={<RutaProtegida soloAdmin><Navigate to={`/admin/web#${seccion}`} replace/></RutaProtegida>}/>
        )}

        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
  );
}
