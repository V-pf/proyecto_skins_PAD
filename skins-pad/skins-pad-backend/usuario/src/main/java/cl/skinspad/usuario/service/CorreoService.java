package cl.skinspad.usuario.service;

import cl.skinspad.usuario.domain.CorreoPendiente;
import cl.skinspad.usuario.repository.CorreoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CorreoService {
  private final CorreoRepository correos;
  private final JavaMailSender smtp;

  @Value("${skinspad.remitente}")
  private String remitente;

  // Cada referencia se guarda una sola vez en la cola de envío.
  public void encolar(String ref, String email, String asunto, String texto) {
    if (correos.existsById(ref)) return;
    var correo = new CorreoPendiente();
    correo.setReferencia(ref);
    correo.setDestinatario(email);
    correo.setAsunto(asunto);
    correo.setContenido(texto);
    correos.save(correo);
  }

  // Se envían los correos pendientes por tandas. Si el servidor falla, quedan en cola para el próximo intento.
  @Scheduled(fixedDelay = 60000)
  public void enviar() {
    for (var correo : correos.findTop50ByEnviadoFalseOrderByReferencia()) {
      var mensaje = new SimpleMailMessage();
      mensaje.setFrom(remitente);
      mensaje.setTo(correo.getDestinatario());
      mensaje.setSubject(correo.getAsunto());
      mensaje.setText(correo.getContenido());
      try {
        smtp.send(mensaje);
      } catch (org.springframework.mail.MailException e) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("SMTP no disponible. Comprobante pendiente {}", correo.getReferencia());
        return;
      }
      correo.setEnviado(true);
      correos.save(correo);
    }
  }
}
