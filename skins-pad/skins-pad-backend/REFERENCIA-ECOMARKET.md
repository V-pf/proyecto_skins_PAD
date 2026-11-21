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
- El Gateway usa `8090`; usuario usa `8087`, catálogo `8081`, carrito `8083` y pago `8084`.

## Importante

El ejemplo de EcoMarket configura `catalogo.service.url=localhost:8081`. Para evitar ambigüedades con el cliente HTTP, la URL usada aquí incluye el protocolo: `http://localhost:8081`.

MySQL es la base de datos predeterminada. Los perfiles `demo` de los microservicios usan H2 en disco. Ejecuta `crear-bases.sql` para crear las cuatro bases MySQL; cada servicio lee sus credenciales desde variables de entorno.
