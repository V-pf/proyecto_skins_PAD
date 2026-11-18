# Skins PAD

Prototipo de un mercado de skins de CS:GO con catálogo, publicaciones, carrito y seguimiento de compras y ventas.

- [Frontend React](skins-pad/skins-pad-react/) con TypeScript y Vite.
- [Backend](skins-pad/skins-pad-backend/) con los servicios de usuario, catálogo, carrito, pago y gateway.
- [Flujo de custodia](skins-pad/docs/FLUJO-CUSTODIA.md) para el seguimiento de pagos y entregas.

## Desarrollo local

1. Ejecuta el [script de creación de bases](skins-pad/skins-pad-backend/crear-bases.sql) en MySQL y configura las credenciales de conexión de cada servicio.
2. Configura una clave local compartida en la variable de entorno `INTERNAL_API_KEY` para los servicios. Desde `skins-pad/skins-pad-backend`, ejecuta `./iniciar-servicio.ps1 -Servicio usuario` en PowerShell y repite el comando en terminales separadas para `catalogo`, `carrito`, `pago` y `gateway`. El script usa Maven.
3. Desde `skins-pad/skins-pad-react`, ejecuta `npm ci` y luego `npm run dev`. Abre [Skins PAD en local](http://localhost:5173).
