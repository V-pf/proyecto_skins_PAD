# API Gateway de Skins PAD

Gateway Spring Cloud Gateway en el puerto `8080`. El frontend debe consumir esta única base URL:

```text
http://localhost:8080
```

Rutas:

| Prefijo | Servicio destino | Puerto por defecto |
|---|---|---:|
| `/api/auth/**`, `/api/usuarios/**` | usuario | 8082 |
| `/api/productos/**`, `/api/categorias/**` | catálogo | 8081 |
| `/api/carritos/**` | carrito | 8083 |
| `/api/pagos/**` | pago | 8084 |

Las URLs de destino pueden modificarse con `USUARIO_URL`, `CATALOGO_URL`, `CARRITO_URL` y `PAGO_URL`. El Gateway enruta las solicitudes; cada microservicio sigue siendo responsable de su lógica y sus datos.
