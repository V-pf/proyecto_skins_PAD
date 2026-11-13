export type Tipo = "rifle" | "francotirador" | "pistola" | "subfusil" | "cuchillo" | "guantes" | "sticker";

export type Grado =
  | "consumidor"
  | "industrial"
  | "militar"
  | "restringido"
  | "clasificado"
  | "encubierto"
  | "contrabando"
  | "especial";

export type Desgaste = "fn" | "mw" | "ft" | "ww" | "bs" | "na";

export type EstadoPublicacion = "activa" | "pendiente" | "rechazada" | "inactiva" | "vendida" | "eliminada";

export interface Producto {
  id: number;
  nombre: string;
  tipo: Tipo;
  grado: Grado;
  desgaste: Desgaste;
  precio: number;
  stattrak: boolean;
  float: number | null;
  vendedor: string;
  estado: EstadoPublicacion;
  vendedorId?: string;
  categoriaId?: number;
  urlIntercambio?: string;
  imagenes?: {id:number;urlImagen:string;esPrincipal:boolean}[];
  coleccion?: string;
  descripcion?: string;
  motivoRechazo?: string;
}

export type Rol = "cliente" | "admin";

export interface Usuario {
  id: string;
  nombre: string;
  usuario: string;
  correo: string;
  fechaNacimiento: string;
  urlSteam: string;
  rol: Rol;
  motivo?: string;
  suspendido: boolean;
  verificado: boolean;
  saldo: number;
}

export interface ItemCarrito {
  productoId: number;
  cantidad: number;
}

