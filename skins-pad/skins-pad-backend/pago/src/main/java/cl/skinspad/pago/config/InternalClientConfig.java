package cl.skinspad.pago.config;
@org.springframework.context.annotation.Configuration
public class InternalClientConfig {
 @org.springframework.context.annotation.Bean feign.Request.Options timeouts(){return new feign.Request.Options(3000,java.util.concurrent.TimeUnit.MILLISECONDS,5000,java.util.concurrent.TimeUnit.MILLISECONDS,true);}

 @org.springframework.context.annotation.Bean feign.RequestInterceptor internal(InternalApiKey credential){return t->t.header("X-Skins-Internal",credential.value());}
}
