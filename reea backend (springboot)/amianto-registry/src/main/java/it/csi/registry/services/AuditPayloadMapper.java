package it.csi.registry.services;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AuditPayloadMapper {

    private final ObjectMapper objectMapper;

    public AuditPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public byte[] toBytes(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Errore serializzazione payload audit", e);
        }
    }
}