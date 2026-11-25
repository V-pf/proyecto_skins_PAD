# Autenticación JWT en Skins PAD

## Recorrido de una solicitud

| Paso | Responsable | Qué ocurre |
| --- | --- | --- |
| 1. Inicio de sesión | React · `AuthProvider.ingresar` y `authService.ingresar` | Envía las credenciales a `POST /api/auth/login` a través del gateway. La opción «recordar» controla la persistencia en el navegador. |
| 2. Creación de sesión | Usuario · `AuthController.login` y `UsuarioService.iniciarSesion` | Comprueba contraseña y suspensión. Genera un secreto aleatorio y guarda su huella SHA-256 como identificador de sesión. |
| 3. Emisión | Usuario · `TokensJwt.emitir` | Firma con RS256 un JWT de 15 minutos. Incluye emisor, audiencia, ID de usuario, ID de sesión, rol al emitir y fechas. La respuesta contiene usuario y JWT. El secreto de renovación viaja en una cookie HttpOnly con SameSite=Strict y ruta `/api/auth`. |
| 4. Transporte | React · `guardarSesion`, `solicitar` e interceptor de petición en `services/http.ts` | Guarda el JWT en sessionStorage o localStorage según «recordar». Axios añade `Authorization: Bearer <jwt>` a las solicitudes. |
| 5. Entrada | Gateway · `AccesoFilter.filter` y `permitido` | Comprueba el JWT con `VerificadorJwt.verificar`, consulta la sesión vigente en usuario y aplica las reglas de acceso a la ruta. Reenvía el Bearer al servicio de destino. |
| 6. Autorización del recurso | Servicio de destino | Carrito usa `CarritoAccessFilter.doFilterInternal` para comprobar el titular. Catálogo usa `CatalogOwnerFilter.doFilterInternal` para las escrituras. Pago usa `UsuarioJwtClient.validate` desde pagos, operaciones, soporte y estadísticas. Usuario protege sus recursos con `WalletOwnerFilter.doFilterInternal`. |

Los consumidores obtienen las claves públicas desde `GET /api/auth/jwks`. El módulo compartido `seguridad` usa Nimbus para validar la firma, el algoritmo, el emisor, la audiencia y la vigencia. El rol incluido en el JWT refleja el momento de emisión. Las decisiones de acceso usan el rol, la verificación y la suspensión vigentes que devuelve usuario.

## Renovación y revocación

`mantenerSesionActiva` comprueba el vencimiento durante la navegación. El interceptor también lo comprueba antes de enviar una solicitud. Cuando queda un minuto o menos, `renovarUnaVez` agrupa las renovaciones concurrentes del mismo JWT y `renovar` llama a `POST /api/auth/renovar`. El navegador envía la cookie HttpOnly y React indica qué sesión quiere renovar mediante `X-Skins-Sesion`. Esto permite tener sesiones distintas abiertas en pestañas diferentes.

`AuthController.renovar` comprueba que la cookie corresponde a esa sesión. `UsuarioService.renovarSesion` verifica en la base de datos que sigue abierta y dentro del período de renovación, y comprueba el estado de la cuenta. Usuario emite otro JWT y React sustituye el anterior. Una respuesta de autenticación 401 permite un único reintento de la petición. Un error temporal al renovar conserva la sesión.

`AuthController.logout` llama a `UsuarioService.cerrarSesion`, registra el cierre y caduca la cookie. Desde el cierre, la siguiente solicitud protegida y la siguiente renovación se rechazan. Durante una suspensión también se deniegan ambas. Retirar un rol o permiso afecta a la siguiente operación que lo requiere y permite seguir usando las demás. Esta garantía requiere que usuario esté disponible para consultar el estado vigente.

## Despliegue

- Actualizar el frontend, el gateway y los servicios como una misma versión. Las sesiones UUID anteriores requieren un nuevo login. Se conservan las cuentas y sus datos.
- Configurar en usuario `SKINS_JWT_PRIVATE_JWK` con una clave RSA privada de al menos 2048 bits y un `kid`. Todas las réplicas de usuario deben usar la misma clave. En desarrollo, omitirla genera una clave temporal al arrancar.
- Para HTTPS terminado en un proxy, configurar `SKINS_COOKIE_SECURE=true` en usuario. El despliegue debe servir React y su API en el mismo sitio para que la cookie SameSite=Strict viaje con la renovación.
- Conservar la configuración interna entre servicios para operaciones de saldo, reservas y moderación. Esas rutas siguen exigiendo la credencial de servicio.
- La configuración del proyecto usa Hibernate con `ddl-auto: update`. La tabla `sesiones` incorpora `ultima_renovacion`. La ventana de renovación es de 30 días desde la última renovación satisfactoria. Con «recordar» se extiende también la duración de la cookie. Sin esa opción, el navegador usa una cookie de sesión.

## Reproducción de las pruebas

En cada directorio de servicio, ejecutar `mvn -Pdemo clean package`. Desde la raíz del repositorio, ejecutar `node skins-pad-backend/scripts/probar-jwt.mjs`. El script inicia cinco procesos Java en puertos locales y crea bases H2 en memoria. En `skins-pad-react`, ejecutar `node --test tests`, `npm run typecheck` y `npm run build`.

## Resultado de la verificación del 25 de noviembre de 2026

Pasaron 105 pruebas Maven (8 de usuario, 5 de gateway, 11 de catálogo, 4 de carrito y 77 de pago), 23 pruebas del frontend, la comprobación de tipos y la compilación de producción de Vite.

La prueba HTTP con los cinco servicios arrancados confirmó login, renovación, aislamiento de cookies entre sesiones, consulta de la clave pública, rechazo del UUID anterior y rechazo de JWT con firma alterada, vencimiento, emisor o audiencia incorrectos. También comprobó titularidad de carrito, acceso directo a los servicios, checkout y revocación de acceso con JWT previamente emitidos.

La primera ejecución HTTP detectó un filtro de usuario que aún buscaba el JWT completo como identificador en la base. Se corrigió el filtro y se repitió la prueba completa. Las pruebas del cliente ejercitaron Axios y la lógica de actividad con transporte simulado. La comprobación HTTP usó H2 y tráfico local. El proxy HTTPS y la base de producción quedan fuera del alcance de esa ejecución.
