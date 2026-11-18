# Microservicio Catalogo — Skins PAD

Servicio Spring Boot independiente. Puerto local: `8081`.

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs
- Base de datos MySQL: `skinspad_catalogo`.
- Variables de conexión: `CATALOGO_DB_URL`, `CATALOGO_DB_USERNAME` y `CATALOGO_DB_PASSWORD`.
- Pruebas y reporte JaCoCo: `mvn clean verify`, luego abrir `target/site/jacoco/index.html`.

MySQL es obligatorio para iniciar el servicio. Antes de ejecutarlo, inicia MySQL Server y crea las cuatro bases con `mysql/init.sql`. No se incluye H2 ni consola H2. Hibernate crea/actualiza las tablas al iniciar (`ddl-auto: update`).
