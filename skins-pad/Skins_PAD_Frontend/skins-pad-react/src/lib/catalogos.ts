import type { Desgaste, EstadoPublicacion, Grado, Producto, Tipo } from "../types";

export const TIPOS: { valor: Tipo; etiqueta: string; descripcion: string }[] = [
  { valor: "rifle", etiqueta: "Rifles", descripcion: "Armas de fuego automáticas de media distancia." },
  { valor: "francotirador", etiqueta: "Francotiradores", descripcion: "Rifles de largo alcance con mira telescópica." },
  { valor: "pistola", etiqueta: "Pistolas", descripcion: "Armas secundarias, desde las básicas hasta las más codiciadas." },
  { valor: "subfusil", etiqueta: "Subfusiles", descripcion: "Armas ligeras de corto alcance y alta cadencia." },
  { valor: "cuchillo", etiqueta: "Cuchillos", descripcion: "Skins ★ para el arma cuerpo a cuerpo." },
  { valor: "guantes", etiqueta: "Guantes", descripcion: "Skins ★ para las manos del personaje." },
  { valor: "sticker", etiqueta: "Stickers", descripcion: "Adhesivos que se aplican sobre las armas." },
];

export const GRADOS: { valor: Grado; etiqueta: string; insignia: string; descripcion: string }[] = [
  { valor: "consumidor", etiqueta: "Grado consumidor", insignia: "Consumidor", descripcion: "El grado más común y de menor valor." },
  { valor: "industrial", etiqueta: "Grado industrial", insignia: "Industrial", descripcion: "Muy comunes, con acabados sencillos." },
  { valor: "militar", etiqueta: "Grado militar", insignia: "Militar", descripcion: "Frecuentes y de precio accesible." },
  { valor: "restringido", etiqueta: "Restringido", insignia: "Restringido", descripcion: "Menos comunes, con diseños más elaborados." },
  { valor: "clasificado", etiqueta: "Clasificado", insignia: "Clasificado", descripcion: "Raros y muy buscados por los coleccionistas." },
  { valor: "encubierto", etiqueta: "Encubierto", insignia: "Encubierto", descripcion: "El nivel más alto de las armas normales. Son escasas y caras." },
  { valor: "contrabando", etiqueta: "Contrabando", insignia: "Contrabando", descripcion: "Retirados de circulación: extremadamente raros." },
  { valor: "especial", etiqueta: "Especial ★", insignia: "Especial ★", descripcion: "Cuchillos y guantes: los ítems más exclusivos del juego." },
];

export const DESGASTES: Record<Desgaste, { nombre: string; rango: string }> = {
  fn: { nombre: "Recién fabricado", rango: "0,00 a 0,07" },
  mw: { nombre: "Poco usado", rango: "0,07 a 0,15" },
  ft: { nombre: "Algo desgastado", rango: "0,15 a 0,38" },
  ww: { nombre: "Bastante desgastado", rango: "0,38 a 0,45" },
  bs: { nombre: "Deplorable", rango: "0,45 a 1,00" },
  na: { nombre: "Sin desgaste", rango: "No aplica" },
};

export function desgastePorFloat(f: number): Desgaste {
  if (f < 0.07) return "fn";
  if (f < 0.15) return "mw";
  if (f < 0.38) return "ft";
  if (f < 0.45) return "ww";
  return "bs";
}

export function etiquetaTipo(tipo: Tipo): string {
  return TIPOS.find((t) => t.valor === tipo)?.etiqueta ?? tipo;
}

export function infoGrado(grado: Grado) {
  return GRADOS.find((g) => g.valor === grado) ?? GRADOS[0];
}

export function imagenDe(tipo: Tipo): string {
  return `/img/arma-${tipo}.svg`;
}

export function subtituloProducto(p: Producto): string {
  const d = DESGASTES[p.desgaste].nombre;
  return p.float === null ? d : `${d}, float ${p.float.toFixed(4).replace(".", ",")}`;
}

export const ESTADO_PUBLICACION: Record<EstadoPublicacion, { texto: string; clase: string }> = {
  activa: { texto: "Publicada", clase: "text-bg-success" },
  pendiente: { texto: "Pendiente", clase: "text-bg-warning" },
  inactiva: {texto:"Reservada",clase:"text-bg-secondary"},
  vendida: {texto:"Vendida",clase:"text-bg-secondary"},
  eliminada: {texto:"Retirada",clase:"text-bg-secondary"},
  rechazada: { texto: "Rechazada", clase: "text-bg-danger" },
};
