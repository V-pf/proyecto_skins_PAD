import ReputacionVendedor from "../components/ReputacionVendedor";
import { useParams } from "react-router-dom";
import { useTienda } from "../context/TiendaContext";
import ListaProductos from "../components/ListaProductos";
export default function PerfilPublico() {
  const { id } = useParams(),
    { usuarios, productos, cargando } = useTienda();
  if (cargando.usuarios || cargando.productos) return <p>Cargando perfil…</p>;
  const cuenta = usuarios.find((u) => u.id === id);
  if (!cuenta) return <p>Perfil no encontrado.</p>;
  // El perfil público muestra las publicaciones activas del vendedor.
  const activas = productos.filter(
    (p) => p.vendedorId === id && p.estado === "activa",
  );
  return (
    <main className="container py-4">
      <h1>{cuenta.usuario}</h1>
      <ReputacionVendedor id={cuenta.id} mostrarResenas />
      {cuenta.verificado && <p>Cliente verificado</p>}
      <h2 className="h5">Publicaciones activas ({activas.length})</h2>
      <ListaProductos productos={activas} />
    </main>
  );
}
