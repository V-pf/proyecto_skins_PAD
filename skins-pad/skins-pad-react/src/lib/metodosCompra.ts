import { formatoCLP } from "./formato";

export function metodosCompra(saldo: number) {
  return [
    { valor: "TARJETA", etiqueta: "Tarjeta de crédito / débito" },
    { valor: "SALDO", etiqueta: `Saldo en la billetera Skins PAD (${formatoCLP(saldo)} disponibles)` },
  ];
}
