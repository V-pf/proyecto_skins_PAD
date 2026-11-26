package cl.skinspad.usuario.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Datos de la cuenta, su acceso a la tienda y el saldo de su billetera.
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    private String motivoSuspension;
    private java.time.LocalDateTime fechaSuspension;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String passwordHash;

    private LocalDate fechaNacimiento;

    @jakarta.persistence.Convert(converter = RolPersistido.class)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Rol rol = Rol.cliente;

    @Column(nullable = false)
    private boolean suspendido = false;

    @Column(nullable = false)
    private boolean verificado = false;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;
}
