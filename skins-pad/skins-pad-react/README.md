# Skins PAD React
Inicia los servicios siguiendo INTEGRACION.md del backend. Después ejecuta `npm ci` y `npm run dev` en esta carpeta. Abre http://localhost:5173.

React consulta el gateway desde /api y solo conserva el token en el navegador. Vite reenvía las solicitudes al puerto 8090 durante el desarrollo. Para otra dirección, configura VITE_API_URL antes de compilar.
