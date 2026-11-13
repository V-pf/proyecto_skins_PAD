package cl.skinspad.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.*;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.*;

@Component
public class AccesoFilter implements GlobalFilter, Ordered {
  private final WebClient cliente = WebClient.create();
  private final ObjectMapper mapper;

  @Value("${USUARIO_URL:http://localhost:8087}")
  String usuarios;

  @Value("${CATALOGO_URL:http://localhost:8081}")
  String productos;

  @Value("${CARRITO_URL:http://localhost:8083}")
  String carritos;

  @Value("${PAGO_URL:http://localhost:8084}")
  String pagos;

  public AccesoFilter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public int getOrder() {
    return -10;
  }

  private Mono<Map> recurso(String url) {
    return cliente.get().uri(url).retrieve().bodyToMono(Map.class);
  }

  private Mono<Void> error(ServerWebExchange x, HttpStatus s) {
    x.getResponse().setStatusCode(s);
    return x.getResponse().setComplete();
  }

  public Mono<Void> filter(ServerWebExchange x, GatewayFilterChain chain) {
    var request = x.getRequest();
    String p = request.getPath().value(), m = request.getMethod().name();
    if (m.equals("OPTIONS")
        || (m.equals("GET")
            && (p.startsWith("/api/productos")
                || p.startsWith("/api/categorias")
                || p.equals("/api/usuarios/publicos")))
        || (m.equals("POST") && (p.equals("/api/auth/login") || p.equals("/api/auth/registro"))))
      return chain.filter(x);
    if (p.contains("/saldo/")
        || p.equals("/api/productos/reservas")
        || (p.equals("/api/pagos") && m.equals("POST"))) return error(x, HttpStatus.FORBIDDEN);
    String auth = request.getHeaders().getFirst("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) return error(x, HttpStatus.UNAUTHORIZED);
    String token = auth.substring(7);
    return cliente
        .get()
        .uri(usuarios + "/api/auth/validar?token={token}", token)
        .retrieve()
        .bodyToMono(Map.class)
        .flatMap(
            u -> {
              if (Boolean.TRUE.equals(u.get("suspendido"))) return error(x, HttpStatus.FORBIDDEN);
              return DataBufferUtils.join(request.getBody(), 65536)
                  .map(
                      buf -> {
                        byte[] b = new byte[buf.readableByteCount()];
                        buf.read(b);
                        DataBufferUtils.release(buf);
                        return b;
                      })
                  .defaultIfEmpty(new byte[0])
                  .flatMap(
                      bytes -> {
                        Map body;
                        try {
                          body = bytes.length == 0 ? Map.of() : mapper.readValue(bytes, Map.class);
                        } catch (Exception e) {
                          return error(x, HttpStatus.BAD_REQUEST);
                        }
                        return permitido(x, p, m, u, body, token)
                            .flatMap(
                                ok -> {
                                  if (!ok) return error(x, HttpStatus.FORBIDDEN);
                                  var base =
                                      request
                                          .mutate()
                                          .headers(
                                              h ->
                                                  h.set(
                                                      "X-Skins-Usuario",
                                                      String.valueOf(u.get("id"))))
                                          .build();
                                  var copia =
                                      new ServerHttpRequestDecorator(base) {
                                        @Override
                                        public Flux<DataBuffer> getBody() {
                                          return bytes.length == 0
                                              ? Flux.empty()
                                              : Flux.defer(
                                                  () ->
                                                      Flux.just(
                                                          x.getResponse()
                                                              .bufferFactory()
                                                              .wrap(bytes)));
                                        }
                                      };
                                  return chain.filter(x.mutate().request(copia).build());
                                });
                      });
            })
        .onErrorResume(
            WebClientResponseException.class,
            e ->
                error(
                    x,
                    e.getStatusCode().is4xxClientError()
                        ? HttpStatus.UNAUTHORIZED
                        : HttpStatus.SERVICE_UNAVAILABLE))
        .onErrorResume(e -> error(x, HttpStatus.SERVICE_UNAVAILABLE));
  }

  private Mono<Boolean> permitido(
      ServerWebExchange x, String p, String m, Map u, Map body, String token) {
    boolean admin = "admin".equals(u.get("rol")), leer = m.equals("GET");
    String id = String.valueOf(u.get("id"));
    if (p.equals("/api/auth/validar"))
      return Mono.just(token.equals(x.getRequest().getQueryParams().getFirst("token")));
    if (p.equals("/api/pagos/mis-ventas")) return Mono.just(leer);
    if (p.equals("/api/auth/logout") || p.equals("/api/pagos/checkout")) return Mono.just(true);
    if (admin) return Mono.just(true);
    if (p.equals("/api/usuarios/" + id + "/retiros") || p.equals("/api/usuarios/" + id + "/perfil"))
      return Mono.just(true);
    if (p.equals("/api/usuarios/" + id)) return Mono.just(leer);
    if (p.equals("/api/carritos/usuario/" + id) || p.equals("/api/pagos/usuario/" + id))
      return Mono.just(true);
    if (p.matches("/api/carritos/[0-9]+(/.*)?"))
      return recurso(carritos + "/api/carritos/" + p.split("/")[3])
          .map(c -> id.equals(c.get("usuarioId")));
    if (p.matches("/api/pagos/[0-9]+(/.*)?"))
      return recurso(pagos + "/api/pagos/" + p.split("/")[3])
          .map(c -> id.equals(c.get("usuarioId")) && (leer || p.endsWith("/comprobante")));
    if (p.matches("/api/productos/[0-9]+/imagenes(/[0-9]+)?"))
      return recurso(productos + "/api/productos/" + p.split("/")[3])
          .map(
              c ->
                  id.equals(c.get("vendedorId"))
                      && ("ACTIVO".equals(c.get("estado"))
                          || "PENDIENTE_APROBACION".equals(c.get("estado"))));
    boolean autor =
        id.equals(body.get("vendedorId")) && u.get("username").equals(body.get("vendedor"));
    if (p.equals("/api/productos"))
      return Mono.just(
          Boolean.TRUE.equals(u.get("verificado"))
              && autor
              && m.equals("POST")
              && "PENDIENTE_APROBACION".equals(body.get("estado")));
    if (p.matches("/api/productos/[0-9]+") && m.equals("PUT"))
      return recurso(productos + p)
          .map(
              c ->
                  autor
                      && id.equals(c.get("vendedorId"))
                      && ("ACTIVO".equals(c.get("estado"))
                          || "PENDIENTE_APROBACION".equals(c.get("estado")))
                      && ("ELIMINADO".equals(body.get("estado"))
                          || c.get("estado").equals(body.get("estado"))));
    return Mono.just(false);
  }
}
