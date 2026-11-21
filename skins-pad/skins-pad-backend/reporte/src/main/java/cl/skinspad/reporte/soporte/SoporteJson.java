package cl.skinspad.reporte.soporte;
@org.springframework.stereotype.Component @lombok.RequiredArgsConstructor public class SoporteJson {
 private final com.fasterxml.jackson.databind.ObjectMapper mapper;
 public <T>T leer(com.fasterxml.jackson.databind.JsonNode b,Class<T> tipo){if(b==null||!b.isObject())throw new IllegalArgumentException("Envía un objeto JSON");try{return mapper.readerFor(tipo).with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).without(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT).readValue(b);}catch(java.io.IOException e){throw new IllegalArgumentException("Datos inválidos o campos no permitidos");}}
}
