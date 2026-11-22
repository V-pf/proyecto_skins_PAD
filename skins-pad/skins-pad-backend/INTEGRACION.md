# Skins PAD integrado

React consulta el gateway en el puerto 8090. Usuario usa el 8087, catálogo el 8081, carrito el 8083 y pago el 8084. Cada servicio conserva su estructura Controller–Service–Repository y su base de datos.

## Probar en Windows

Requisitos: Java 17, Maven 3.9, Node.js y npm.

1. Abre la carpeta `skins-pad/skins-pad-backend` del repositorio.
2. Dentro de la carpeta del backend, abre PowerShell y ejecuta:

   ```powershell
   .\iniciar-servicios.ps1 -Demo
   ```

   Espera el mensaje de arranque en las cinco ventanas. El modo demo usa H2 en disco. Conserva las cuentas, carritos y pagos tras reiniciar.
3. En otra terminal, dentro de la carpeta del frontend:

   ```powershell
   npm ci
   npm run dev
   ```
4. Abre http://localhost:5173. Ingresa con una de las cuentas de la tabla. Para probar una compra con saldo, el comprador de ejemplo tiene $12.500 iniciales. La comisión es el 5% truncado al peso.

| Cuenta | Usuario | Contraseña |
|---|---|---|
| Cliente con saldo | penquista_gamer | Demo1234! |
| Administrador | admin | Admin1234! |
| Cliente verificado | PenquistaSkins | Demo1234! |

## Ejecutar con MySQL

Ejecuta `crear-bases.sql` en MySQL. En PowerShell configura las variables `USUARIO_DB_PASSWORD`, `CATALOGO_DB_PASSWORD`, `CARRITO_DB_PASSWORD` y `PAGO_DB_PASSWORD`. Si usas otro usuario, configura también las correspondientes variables `*_DB_USERNAME`. Ejecuta `.\iniciar-servicios.ps1` sin el parámetro `-Demo`. Los datos de ejemplo se cargan cuando las tablas correspondientes están vacías.

## Qué pasa en una compra

- El cliente inicia sesión. React conserva el token y obtiene los datos de la cuenta del microservicio usuario.
- Las skins publicadas desde el inventario aparecen de inmediato en el catálogo. Administración puede revisarlas y retirar una publicación no apta indicando el motivo.
- El carrito pertenece al usuario. Cada publicación representa una skin única y admite una unidad.
- Al pagar se envían IDs de productos y método. El backend cotiza con los precios del catálogo y guarda el pago. Valida disponibilidad y toma las skins al confirmar la operación. Una solicitud repetida conserva el pago existente.
- El pago con saldo se debita de la billetera del comprador y la operación aparece en Mis ventas para el vendedor. El vendedor recibe el abono al completarse la entrega. Tarjeta, transferencia y Mercado Pago son simulaciones disponibles para compras de skins de demostración.
- Si el vendedor rechaza la compra o vence su plazo de entrega, se libera la publicación y se devuelve el saldo retenido al comprador. Si la venta se completa, la skin desaparece del inventario del vendedor.
- Mis pagos consulta los movimientos del servicio de pagos. El campo original `pedidoId` se conserva como referencia de pago. El sitio excluye la gestión de pedidos y la mensajería.

Los antiguos datos del navegador se ignoran. Las bases locales anteriores no se migran al servidor. Ante una interrupción entre dos microservicios durante un débito se debe revisar el pago antes de repetirlo. La aplicación está preparada para una instancia local por servicio y pruebas académicas.

## Verificación

Desde el frontend: `npm run typecheck` y `npm run build`. Desde cada microservicio: `mvn test`.

## Configurar los correos de la entrega

El servicio usuario conserva los comprobantes de registro y compra en su base de datos. Cada minuto intenta entregar los pendientes por SMTP. Para una demostración local se puede usar un servidor de pruebas en localhost, puerto 1025, y revisar su bandeja. Para un proveedor de correo configura SMTP_HOST, SMTP_PORT, SMTP_FROM, SMTP_USER, SMTP_PASSWORD, SMTP_AUTH y SMTP_STARTTLS en el entorno de usuario antes de iniciar los servicios.

## Probar el circuito de venta

1. Con una cuenta cliente verificada, selecciona una skin en Mi inventario y publícala. Comprueba que aparece en el catálogo.
2. Con otra cuenta, compra la skin con saldo. En una compra de demostración también puedes elegir un método simulado.
3. Desde el menú del vendedor entra a Mis ventas, acepta la operación y marca la entrega. Desde Mis compras, confirma la recepción con la cuenta compradora.
4. Comprueba que la skin desapareció del inventario del vendedor. Si la compra fue con saldo, revisa el abono en Billetera y retiros.
5. En Gestión web, Administración puede revisar si una publicación activa es apta. En Gestión de pagos puede consultar el historial e intervenir en disputas.

El frontend utiliza los roles cliente y administrador. El servicio usuario lee cuentas anteriores con rol comprador o vendedor como clientes. Los saldos históricos se conservan; los abonos por venta se generan al liquidar compras con esta versión.
