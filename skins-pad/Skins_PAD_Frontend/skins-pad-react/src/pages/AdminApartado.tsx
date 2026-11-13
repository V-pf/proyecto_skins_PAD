import { useEffect, useRef } from "react";
import MenuCuenta from "../components/MenuCuenta";
import Mercado from "./Mercado";
import Pagos from "./Pagos";
import AdminCategorias from "./AdminCategorias";
import { useTienda } from "../context/TiendaContext";

export default function AdminApartado({apartado}:{apartado:"ventas"|"categorias"|"pagos"}) {
  const { pagos,cargando,recargar }=useTienda();
  const actualizar=useRef(recargar);
  actualizar.current=recargar;
  useEffect(()=>{
    if(apartado!=="ventas")return;
    const timer=setInterval(()=>void actualizar.current(),60000);
    return ()=>clearInterval(timer);
  },[apartado]);
  return <div className="container py-4"><div className="row g-4">
    <MenuCuenta tipo="admin" activo={`/admin/${apartado}`}/>
    <div className="col-lg-9">{apartado==="ventas"
      ? cargando.productos || cargando.pagos ? <p>Cargando ventas…</p> : <Mercado ventas={pagos}/>
      : apartado==="categorias" ? <AdminCategorias/> : <Pagos gestion/>}</div>
  </div></div>;
}
