import { puntajeClave } from "../lib/validaciones";

const ANCHOS = [0, 25, 50, 75, 100];
const COLORES = ["#e0616f", "#e0616f", "#e0b35f", "#a9c95f", "#5fbf8f"];

export default function MedidorClave({ valor }: { valor: string }) {
  const p = puntajeClave(valor);
  return (
    <div className="medidor-clave" aria-hidden="true">
      <span style={{ width: `${ANCHOS[p]}%`, background: COLORES[p] }}></span>
    </div>
  );
}
