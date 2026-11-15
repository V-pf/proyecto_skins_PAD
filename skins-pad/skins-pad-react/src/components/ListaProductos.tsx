import ProductoCard from "./ProductoCard";
import type { Producto } from "../types";

interface Props {
  productos: Producto[];
  columnas?: string;
}

export default function ListaProductos({ productos, columnas = "row-cols-1 row-cols-sm-2 row-cols-lg-4" }: Props) {
  return (
    <ul className={`row ${columnas} g-4 list-unstyled`}>
      {productos.map((p) => (
        <li className="col" key={p.id}>
          <ProductoCard producto={p} />
        </li>
      ))}
    </ul>
  );
}
