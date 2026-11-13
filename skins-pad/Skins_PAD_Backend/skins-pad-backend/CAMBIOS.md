# Cambios de la versión MySQL-only

- Eliminada la dependencia `com.h2database:h2` de `catalogo`, `usuario`, `carrito` y `pago`.
- Eliminadas las configuraciones de datasource H2 y la consola `/h2-console`.
- MySQL quedó como conexión predeterminada en los cuatro `application.yml`, con bases separadas y credenciales por variables de entorno.
- Eliminados los archivos `application-mysql.yml` porque ya no existe un perfil H2/MySQL alternativo.
- Se conservan las pruebas unitarias basadas en Mockito; no usan `@SpringBootTest`, `@DataJpaTest` ni datasource real, por lo que no dependen de H2 ni necesitan MySQL para ejecutarse.
- Añadido `mysql/verificar-mysql.ps1` para comprobar desde el equipo del usuario la conexión local y la existencia de las cuatro bases.

## Limitación de validación

La disponibilidad del servidor MySQL del usuario no se puede comprobar desde este entorno de edición. El script de verificación debe ejecutarse en el mismo equipo donde se ejecutará el backend. Tampoco se pudo ejecutar Maven aquí; ejecutar `mvn clean verify` en cada módulo para confirmar compilación y pruebas.
