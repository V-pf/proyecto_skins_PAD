package cl.skinspad.gateway;
@org.springframework.stereotype.Component
public class InternalHeaderFilter implements org.springframework.cloud.gateway.filter.GlobalFilter,org.springframework.core.Ordered {
 public int getOrder(){return -100;}
 public reactor.core.publisher.Mono<Void> filter(org.springframework.web.server.ServerWebExchange x,org.springframework.cloud.gateway.filter.GatewayFilterChain c){return c.filter(x.mutate().request(x.getRequest().mutate().headers(h->h.remove("X-Skins-Internal")).build()).build());}
}
