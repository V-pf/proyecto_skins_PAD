# Microservicio Carrito — Skins PAD

Servicio Spring Boot independiente. Puerto local: `8083`.

- Swagger UI: http://localhost:8083/swagger-ui.html
- OpenAPI JSON: http://localhost:8083/v3/api-docs
- Base de datos MySQL: `skinspad_carrito`.
- Variables de conexión: `CARRITO_DB_URL`, `CARRITO_DB_USERNAME` y `CARRITO_DB_PASSWORD`.
- Pruebas y reporte JaCoCo: `mvn clean verify`, luego abrir `target/site/jacoco/index.html`.

MySQL es obligatorio para iniciar el servicio. Antes de ejecutarlo, inicia MySQL Server y crea las cuatro bases con `mysql/init.sql`. No se incluye H2 ni consola H2. Hibernate crea/actualiza las tablas al iniciar (`ddl-auto: update`).
