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
- El catálogo y las categorías vienen del servidor. Los vendedores publican skins y administración decide su estado.
- El carrito pertenece al usuario. Cada publicación representa una skin única y admite una unidad.
- Al pagar se envían IDs de productos y método. El backend cotiza con los precios del catálogo y guarda el pago. Valida disponibilidad y toma las skins al confirmar la operación. Una solicitud repetida conserva el pago existente.
- Saldo descuenta la billetera en usuario. Transferencia permite registrar un comprobante por URL y requiere revisión administrativa. Tarjeta y Mercado Pago registran pagos pendientes para confirmación manual. El proyecto no incluye credenciales ni una pasarela externa para estos métodos.
- Rechazar un pago libera la publicación. Reembolsar un pago con saldo también repone la billetera. Para pagos externos, la devolución monetaria se gestiona fuera del sitio antes de registrar el reembolso.
- Mis pagos consulta los movimientos del servicio de pagos. El campo original `pedidoId` se conserva como referencia de pago. El sitio excluye la gestión de pedidos y la mensajería.

Los antiguos datos del navegador se ignoran. Las bases locales anteriores no se migran al servidor. Ante una interrupción entre dos microservicios durante un débito se debe revisar el pago antes de repetirlo. La aplicación está preparada para una instancia local por servicio y pruebas académicas.

## Verificación

Desde el frontend: `npm run typecheck` y `npm run build`. Desde cada microservicio: `mvn test`.

## Configurar los correos de la entrega

El servicio usuario conserva los comprobantes de registro y compra en su base de datos. Cada minuto intenta entregar los pendientes por SMTP. Para una demostración local se puede usar un servidor de pruebas en localhost, puerto 1025, y revisar su bandeja. Para un proveedor de correo configura SMTP_HOST, SMTP_PORT, SMTP_FROM, SMTP_USER, SMTP_PASSWORD, SMTP_AUTH y SMTP_STARTTLS en el entorno de usuario antes de iniciar los servicios.

## Probar el circuito de venta

1. Publica con una cuenta cliente verificada. Desde Administración, aprueba la publicación.
2. Con una cuenta distinta, agrega la skin al carrito y paga. El saldo se valida y debita al confirmar. Las transferencias requieren comprobante y verificación administrativa. Tarjeta y Mercado Pago se registran para confirmación manual.
3. Revisa los pagos en Administración → Gestión de pagos y abre Estadísticas de ventas. El gráfico y la tabla muestran las publicaciones asociadas. Los pagos históricos sin identificadores de producto solo aportan al total de pagos aprobados.
4. En la cuenta del vendedor abre Mi panel para revisar sus ventas por período y Billetera y retiros para solicitar un retiro en pesos enteros. La solicitud se registra y descuenta el saldo disponible. La transferencia bancaria al vendedor se procesa fuera del sistema.
5. Si se interrumpe la acreditación de una compra aprobada, Administración permite completarla desde la ficha del pago. Las referencias de movimiento evitan repetir un débito o abono. Un reembolso revierte las ganancias, incluso si el vendedor ya solicitó un retiro.

El frontend utiliza los roles cliente y administrador. El servicio usuario lee cuentas anteriores con rol comprador o vendedor como clientes. Los saldos históricos se conservan; los abonos por venta se generan al liquidar compras con esta versión.
