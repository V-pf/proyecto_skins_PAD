# Steam, inventario y catálogo

| Servicio | Puerto local | Datos y operaciones |
| --- | --- | --- |
| `steam` | 8086 | Valida OpenID y guarda `steam_estado` y `vinculo_steam`. Consulta el inventario de CS2 por cuenta vinculada. |
| `inventario` | 8082 | Guardar y sincronizar los ítems de cada usuario. Mostrar Mi inventario e historial de precios. |
| `catalogo` | 8081 | Crear, moderar y vender publicaciones. Conserva el ID del ítem y una copia de sus datos de presentación. |

Tras verificar la vinculación, Steam solicita la primera sincronización a
inventario. Si falla, el usuario puede usar **Sincronizar**. Al publicar o
reservar, catálogo consulta a inventario por una API interna para verificar
que la copia sigue disponible. Los servicios internos comparten la credencial
`.internal-api-key` en local. Fuera de localhost, configura `INTERNAL_API_KEY`
con el mismo valor en los servicios. El frontend usa las mismas rutas del gateway.

Desde `skins-pad/skins-pad-backend`:

```powershell
.\iniciar-servicios.ps1 -Demo
```

El modo demo usa H2. El lanzador espera a usuario y el servicio inventario
carga los ítems de prueba de cada cliente durante el arranque.
Para usar MySQL/MariaDB, crea las bases incluidas en `crear-bases.sql` y arranca
sin `-Demo`. Configura los prefijos `STEAM_DB_` e `INVENTARIO_DB_`
(`URL`, `USERNAME` y `PASSWORD`) si necesitas cambiar los valores locales.

Si cambian los puertos o las direcciones internas, configura `STEAM_SERVICE_URL`
e `INVENTARIO_SERVICE_URL` en los servicios consumidores y en el gateway.
Si cambia la dirección pública del gateway, define `STEAM_CALLBACK_URL`
en steam. Debe terminar en `/api/steam/callback`.

## Actualizar una instalación con datos previos

1. Detén los servicios, haz una copia de la base de catálogo y conserva el
   directorio de datos de cada servicio al actualizar. Las publicaciones y
   sus IDs de inventario ya están en catálogo.
2. Crea los esquemas de destino iniciando solo `steam` e `inventario` una vez
   y luego detén ambos. Si usas H2, inicia cada uno desde una consola con
   `-Demo` y establece `$env:INVENTARIO_DEMO = 'false'` en la consola de
   inventario antes de iniciarlo. Así no genera IDs antes de importar los viejos.
3. Para H2, ejecuta `.\scripts\migrar-inventario.ps1 -Demo` desde el backend.
   Para MySQL/MariaDB, configura `CATALOGO_DB_URL`, `STEAM_DB_URL` e
   `INVENTARIO_DB_URL` y los usuarios y contraseñas de cada base, y ejecuta
   `.\scripts\migrar-inventario.ps1`.
4. Inicia los servicios normalmente. Si habías desactivado temporalmente el demo
   en tu consola, elimina esa variable de la sesión con
   `Remove-Item Env:INVENTARIO_DEMO -ErrorAction SilentlyContinue` antes del arranque.

El script copia vínculos reales y los ítems con sus IDs. Conserva las tablas
antiguas para recuperación y no hace falta copiarlas de vuelta. Los intentos
de OpenID abiertos durante la actualización se deben iniciar nuevamente.
Una instalación nueva puede omitir la migración.

Si quieres retirar las tablas antiguas, comprueba primero que los vínculos y
las publicaciones existentes funcionen con la aplicación actualizada y que
tengas una copia de seguridad. Las tres tablas antiguas se pueden eliminar
entonces **solo en la base de catálogo**. Los nombres son
`steam_estado`, `vinculo_steam` e `item_inventario`.
