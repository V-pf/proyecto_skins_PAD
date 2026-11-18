package cl.skinspad.usuario.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RolPersistido implements AttributeConverter<Rol, String> {
  public String convertToDatabaseColumn(Rol rol) {
    return rol.name();
  }

  // Las cuentas antiguas de comprador o vendedor se leen como clientes.
  public Rol convertToEntityAttribute(String valor) {
    return switch (valor) {
      case "comprador", "vendedor", "cliente" -> Rol.cliente;
      default -> Rol.valueOf(valor);
    };
  }
}
