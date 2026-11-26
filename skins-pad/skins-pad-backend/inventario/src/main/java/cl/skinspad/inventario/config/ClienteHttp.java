package cl.skinspad.inventario.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class ClienteHttp {
  private ClienteHttp() {}

  public static RestClient cliente(String url) {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    factory.setReadTimeout(Duration.ofSeconds(20));
    return RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }
}
