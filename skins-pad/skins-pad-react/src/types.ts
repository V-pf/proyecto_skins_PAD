// Datos que describen el tipo, la rareza y el desgaste de una skin.
export type Tipo = "rifle" | "francotirador" | "pistola" | "subfusil" | "escopeta" | "ametralladora" | "cuchillo" | "guantes" | "sticker";

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

// Publicación del catálogo con sus datos de venta y su estado de revisión.
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
  inventarioId?: number | null;
  demo?: boolean | null;
  revisionApta?: boolean | null;
  categoriaId?: number;
  urlIntercambio?: string;
  imagenes?: {id:number;urlImagen:string;esPrincipal:boolean}[];
  coleccion?: string;
  descripcion?: string;
  motivoRechazo?: string;
}

export type Rol = "cliente" | "admin";

// Datos de la cuenta que usan el perfil, la sesión y la billetera.
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

// Identifica la publicación y la cantidad guardada en una línea del carrito.
export interface ItemCarrito {
  productoId: number;
  cantidad: number;
}

