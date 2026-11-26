package cl.skinspad.pago.stats;

import java.util.List;

public interface StatsUserClient {
    record Identity(String id, String rol, boolean suspendido) {}
    record PublicUser(String id, String rol) {}
    Identity validate(String token);
    List<PublicUser> users();
}
