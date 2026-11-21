package cl.skinspad.steam.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalAccessFilter extends OncePerRequestFilter {
  private final byte[] key;

  public InternalAccessFilter(InternalApiKey credential) {
    key = credential.value().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!request.getServletPath().startsWith("/interno/")) {
      chain.doFilter(request, response);
      return;
    }
    String provided = request.getHeader("X-Skins-Internal");
    if (provided == null || !MessageDigest.isEqual(key, provided.getBytes(StandardCharsets.UTF_8))) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    chain.doFilter(request, response);
  }
}
