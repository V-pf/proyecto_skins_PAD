package cl.skinspad.ticket.soporte;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.*;
@org.springframework.stereotype.Component public class SoporteBodyLimit extends org.springframework.web.filter.OncePerRequestFilter {
 protected void doFilterInternal(HttpServletRequest r,HttpServletResponse response,FilterChain chain)throws IOException,ServletException{
  if(!(r.getRequestURI().startsWith("/api/tickets")||r.getRequestURI().startsWith("/api/reportes"))||!java.util.Set.of("POST","PATCH").contains(r.getMethod())){chain.doFilter(r,response);return;}
  byte[] bytes=r.getInputStream().readNBytes(2200001);if(bytes.length>2200000){response.sendError(413,"Solicitud demasiado grande");return;}
  chain.doFilter(new HttpServletRequestWrapper(r){public ServletInputStream getInputStream(){var in=new ByteArrayInputStream(bytes);return new ServletInputStream(){public int read(){return in.read();}public boolean isFinished(){return in.available()==0;}public boolean isReady(){return true;}public void setReadListener(ReadListener l){throw new UnsupportedOperationException();}};}public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}},response);
 }
}
