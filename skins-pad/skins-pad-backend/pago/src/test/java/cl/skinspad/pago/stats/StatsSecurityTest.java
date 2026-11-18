package cl.skinspad.pago.stats;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;
import cl.skinspad.pago.repository.PagoRepository;

class StatsSecurityTest {
  @Test void rechazaAccesoDirectoSinTokenYClientesAunqueFalsifiquenHeaders() {
    var repo=mock(PagoRepository.class);var catalog=mock(StatsCatalogClient.class);var users=mock(StatsUserClient.class);
    var service=new StatsService(repo,catalog,users,mock(cl.skinspad.pago.escrow.OperacionRepository.class));
    assertEquals(401,assertThrows(ResponseStatusException.class,()->service.get(null,null,null,false,null,null)).getStatusCode().value());
    when(users.validate("cliente")).thenReturn(new StatsUserClient.Identity("u1","cliente",false));
    assertEquals(403,assertThrows(ResponseStatusException.class,()->service.get("Bearer cliente",null,null,false,null,null)).getStatusCode().value());
    when(users.validate("suspendido")).thenReturn(new StatsUserClient.Identity("admin","admin",true));
    assertEquals(403,assertThrows(ResponseStatusException.class,()->service.get("Bearer suspendido",null,null,false,null,null)).getStatusCode().value());
    verifyNoInteractions(repo,catalog);
  }
}
