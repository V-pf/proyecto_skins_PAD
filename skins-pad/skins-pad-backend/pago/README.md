# Microservicio Pago — Skins PAD

Servicio Spring Boot independiente. Puerto local: `8084`.

- Swagger UI: http://localhost:8084/swagger-ui.html
- OpenAPI JSON: http://localhost:8084/v3/api-docs
- Base de datos MySQL: `skinspad_pago`.
- Variables de conexión: `PAGO_DB_URL`, `PAGO_DB_USERNAME` y `PAGO_DB_PASSWORD`.
- Pruebas y reporte JaCoCo: `mvn clean verify`, luego abrir `target/site/jacoco/index.html`.

MySQL es obligatorio para iniciar el servicio. Antes de ejecutarlo, inicia MySQL Server y crea las cuatro bases con `mysql/init.sql`. No se incluye H2 ni consola H2. Hibernate crea/actualiza las tablas al iniciar (`ddl-auto: update`).
