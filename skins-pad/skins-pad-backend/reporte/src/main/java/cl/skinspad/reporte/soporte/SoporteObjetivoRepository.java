package cl.skinspad.reporte.soporte;
public interface SoporteObjetivoRepository extends org.springframework.data.jpa.repository.JpaRepository<SoporteObjetivo,String>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select o from SoporteObjetivo o where o.clave=:id") java.util.Optional<SoporteObjetivo> bloquear(@org.springframework.data.repository.query.Param("id") String id);
}
