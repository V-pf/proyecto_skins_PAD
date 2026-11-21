package cl.skinspad.calificacion.service;
import cl.skinspad.calificacion.client.*;
import cl.skinspad.calificacion.domain.Resena;
import cl.skinspad.calificacion.repository.ResenaRepository;
import java.math.*;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @RequiredArgsConstructor
public class CalificacionService {
    private final ResenaRepository resenas;
    private final UsuarioClient usuarios;
    private final PagoClient pagos;
    public record Reputacion(BigDecimal promedio, int total, boolean trustedSeller, List<Resena> resenas) {}
    public Reputacion consultar(String vendedorId) {
        return calcular(resenas.findByVendedorIdOrderByIdAsc(vendedorId));
    }
    // Reproduce el historial inmutable para conservar el estatus entre ambos umbrales.
    public static Reputacion calcular(List<Resena> historial) {
        BigDecimal suma = BigDecimal.ZERO;
        boolean trusted = false;
        int cantidad = 0;
        for (Resena resena : historial) {
            suma = suma.add(resena.getPuntaje());
            cantidad++;
            BigDecimal divisor = BigDecimal.valueOf(cantidad);
            if (suma.compareTo(new BigDecimal("4.0").multiply(divisor)) > 0) trusted = true;
            if (suma.compareTo(new BigDecimal("3.5").multiply(divisor)) < 0) trusted = false;
        }
        BigDecimal promedio = cantidad == 0 ? null : suma.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
        return new Reputacion(promedio, cantidad, trusted, historial);
    }
    @Transactional
    public Resena crear(String auth, Long operacionId, BigDecimal puntaje, String comentario) {
        if (auth == null || !auth.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {
            var usuario = usuarios.validar(auth.substring(7));
            if (usuario.suspendido() || !"cliente".equals(usuario.rol())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            var operacion = pagos.obtener(operacionId, auth).operacion();
            if (!usuario.id().equals(operacion.compradorId()) || usuario.id().equals(operacion.vendedorId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            if (!"COMPLETADA".equals(operacion.estado())) throw new ResponseStatusException(HttpStatus.CONFLICT, "La compra debe estar completada.");
            if (resenas.existsByOperacionId(operacionId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta compra ya tiene una reseña.");
            var resena = new Resena();
            resena.setOperacionId(operacionId);
            resena.setCompradorId(usuario.id());
            resena.setVendedorId(operacion.vendedorId());
            resena.setPuntaje(puntaje);
            resena.setComentario(comentario.trim());
            resena.setFecha(Instant.now());
            return resenas.saveAndFlush(resena);
        } catch (feign.FeignException e) {
            throw new ResponseStatusException(e.status() < 0 || e.status() >= 500 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.valueOf(e.status()), "No se pudo validar la compra o la sesión.");
        }
    }
}
