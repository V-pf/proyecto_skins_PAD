# Adaptación de los ejemplos EcoMarket a Skins-Pad

Se tomó como referencia el patrón de ambos ZIP de ejemplo:

- Cada microservicio tiene su dependencia `mysql-connector-j`, JPA y una URL JDBC propia.
- Cada microservicio usa una base de datos/esquema independiente.
- Carrito consulta el catálogo a través de HTTP y no lee las tablas de catálogo directamente.
- Pago consulta al microservicio de usuarios por HTTP; el saldo permanece bajo la administración de `usuario`.

## Cómo se refleja en Skins-Pad

- `catalogo` → `skinspad_catalogo`
- `usuario` → `skinspad_usuario`
- `carrito` → `skinspad_carrito`
- `pago` → `skinspad_pago`
- `carrito` usa `CatalogoClient` de OpenFeign para consultar `GET /api/productos/{id}`.
- `pago` usa `UsuarioFeignClient` para consultar `GET /api/usuarios/{id}` y solicitar débitos/reintegros de saldo.
- El Gateway queda en `8080`; los microservicios usan los puertos `8081` a `8084`.

## Importante

El ejemplo de EcoMarket configura `catalogo.service.url=localhost:8081`. Para evitar ambigüedades con el cliente HTTP, la URL usada aquí incluye el protocolo: `http://localhost:8081`.

H2 se eliminó por completo para ajustarse al requisito de usar MySQL como base de datos predeterminada. Ejecuta `mysql/init.sql` para crear las bases locales; cada servicio lee sus credenciales de variables de entorno y no las guarda en el código.
