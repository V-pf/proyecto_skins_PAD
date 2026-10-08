=======
# Skins PAD

Marketplace de skins de CS:GO con precios en pesos chilenos.

Referencias usadas para el diseño y las funciones: Skinport, CS.MONEY y el propio Mercado de la Comunidad de Steam (flujo de intercambio, exteriores/float, StatTrak™, grados de rareza).

## Qué hay en cada carpeta

```
skins-pad/
├── index.html               solo redirige a home.html
├── home.html                portada
├── catalogo.html            catálogo con filtros, orden y búsqueda (JS)
├── detalle-producto.html    una skin en detalle, con barra de float y compra
├── categorias.html          categorías por tipo de arma y por grado de rareza
├── contacto.html            formulario de contacto + preguntas frecuentes
├── login.html               ingreso (con enlaces demo a los paneles)
├── registro.html            crear cuenta
├── cliente/                 panel del comprador (panel, pedidos, carrito, vender)
├── admin/                   panel del administrador (panel, usuarios, publicaciones)
├── css/estilos.css          estilos propios
├── js/validaciones.js       validación genérica de formularios (reutilizable)
├── js/catalogo.js           filtros, orden y búsqueda del catálogo
├── js/detalle.js            barra de desgaste/float y contador de cantidad
└── img/                     logo e ilustraciones de ejemplo (SVG, dibujadas a mano)
```
=======

## Funciones agregadas respecto del ejemplo original de Botánica PAD

Estas son propias de un marketplace de ítems de videojuego y se agregaron porque tienen sentido en el rubro (inspiradas en Skinport / CS.MONEY / Steam):

- **Grado de rareza y exterior/float**: cada skin muestra su grado (consumidor a contrabando, más el grado especial ★ de cuchillos y guantes) y su float (0,0000 a 1,0000), con una barra visual que ubica el valor exacto.
- **StatTrak™**: insignia distintiva en tarjetas y detalle.
- **Filtros de catálogo en JavaScript**: por tipo de arma, grado, StatTrak™, precio máximo (con `<input type="range">`) y texto libre, más orden por precio o nombre — todo sin recargar la página.
- **Enlace de intercambio de Steam**: campo con su propia validación (formato `steamcommunity.com/tradeoffer/...`) tanto al vender una skin como al registrarse.
- **Código/URL de Steam y usuario tipo Steam**: reglas de validación dedicadas (`data-regla`) para nombres de usuario, códigos de intercambio y enlaces de perfil.
- **Medidor de fuerza de contraseña**: barra que cambia de color mientras se escribe la contraseña en el registro.
- **Panel de vendedor**: formulario "Vender una skin" con datos del ítem, precio sugerido, forma de entrega y aviso de comisión.
- **Panel de administración**: cola de publicaciones por revisar, gestión de usuarios (suspender/verificar) y rechazo de publicaciones con motivo — todo con formularios validados y modales de Bootstrap.
- **Aviso de retención de pago**: se explica en el detalle de producto y en el carrito cómo funciona la entrega (intercambio de Steam) y qué pasa si no llega a tiempo.
