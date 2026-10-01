package it.csi.registry.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.repositories.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;


@Service
public class AuditServiceImpl implements AuditService {

    private final AuditRepository auditRepository;
    private final HttpServletRequest httpServletRequest;

    public AuditServiceImpl(AuditRepository auditRepository, HttpServletRequest httpServletRequest) {
        this.auditRepository = auditRepository;
        this.httpServletRequest = httpServletRequest;
    }


    @Override
    public Long salvaAudit(AuditLogRequest request) {
        valida(request);

        normalizza(request);

        return auditRepository.insertAudit(request);
    }
    
    
    public void logLogout(String username, String ipClient) {
        AuditLogRequest auditRequest = new AuditLogRequest();
        auditRequest.setUtente(username);
        auditRequest.setIpAddress(ipClient);
        auditRequest.setOperazione("LOGOUT");
        auditRequest.setEsitoChiamata(200);
        // NON valorizzare oggOper e keyOper per login/logout
        // (conforme doc REEA-CDU-032)
        auditRepository.inserisciLog(auditRequest);
    }
    
    public void logLogin(String username, String ipClient) {
        AuditLogRequest auditRequest = new AuditLogRequest();
        auditRequest.setUtente(username);
        auditRequest.setIpAddress(ipClient);
        auditRequest.setOperazione("LOGIN");
        auditRequest.setEsitoChiamata(200);
        // NON valorizzare oggOper/keyOper per login/logout
        auditRepository.inserisciLog(auditRequest);
    }
    
    public void logLoginFailed(String username, String ipClient) {
        AuditLogRequest auditRequest = new AuditLogRequest();
        auditRequest.setUtente(username);
        auditRequest.setIpAddress(ipClient);
        auditRequest.setOperazione("LOGIN");
        auditRequest.setEsitoChiamata(401);
        // NON impostare oggOper e keyOper (conforme doc REEA-CDU-032)
        auditRepository.inserisciLog(auditRequest);
    }

    @Override
    public Long salvaAuditLoginLogout(String idApp, String ipAddress, String utente, String operazione, Integer esitoChiamata) {
        AuditLogRequest request = new AuditLogRequest();
        request.setIdApp(idApp);
        request.setIpAddress(ipAddress);
        request.setUtente(utente);
        request.setOperazione(operazione);
        request.setEsitoChiamata(esitoChiamata);
        request.setLoginLogout(true);
        request.setSkipPayload(true);

        return salvaAudit(request);
    }

    private void valida(AuditLogRequest request) {
        
    	
    	if (request == null) {
            throw new IllegalArgumentException("request audit non valorizzata");
        }
        if (!StringUtils.hasText(request.getIdApp())) {
            throw new IllegalArgumentException("idApp obbligatorio");
        }
        if (!StringUtils.hasText(request.getIpAddress())) {
            request.setIpAddress(httpServletRequest.getRemoteAddr()); // ← sostituisce il throw
        }
        
        if (!StringUtils.hasText(request.getIpAddress())) {
            throw new IllegalArgumentException("ipAddress obbligatorio");
        }
        if (!StringUtils.hasText(request.getUtente())) {
            throw new IllegalArgumentException("utente obbligatorio");
        }
        if (!StringUtils.hasText(request.getOperazione())) {
            throw new IllegalArgumentException("operazione obbligatoria");
        }
        if (request.getEsitoChiamata() == null) {
            throw new IllegalArgumentException("esitoChiamata obbligatorio");
        }
    }

    private void normalizza(AuditLogRequest request) {
        // Genera UUID se assente (uuid è JsonNullable<String>)
        if (!StringUtils.hasText(unwrap(request.getUuid()))) {
            request.setUuid(JsonNullable.of(UUID.randomUUID().toString()));
        }

        // Login/logout: oggOper e keyOper non devono essere valorizzati
        // loginLogout è Boolean puro, NON JsonNullable
        Boolean loginLogout = request.getLoginLogout();
        if (loginLogout != null && loginLogout) {
            request.setOggOper(null);
            request.setKeyOper(null);
        }

        // Skip payload: non tracciare request e response payload
        // skipPayload è Boolean puro, NON JsonNullable
        Boolean skipPayload = request.getSkipPayload();
        if (skipPayload != null && skipPayload) {
            request.setRequestPayload(null);
            request.setResponsePayload(null);
        }
    }

    private <T> T unwrap(JsonNullable<T> value) {
        return value != null ? value.orElse(null) : null;
    }
    
   
    
    @Override
    public void preparaAuditRequest(
            AuditLogRequest auditLogRequest,
            String operazione,
            String fileName,
            String filePath,
            String checksum,
            String contentType,
            Long size,
            Integer fileId,
            AuditPayloadMapper auditPayloadMapper) {

        if (auditLogRequest == null) {
            return;
        }

        if (operazione != null && !operazione.isBlank()) {
            auditLogRequest.setOperazione(operazione);
        }

        String currentOgg = unwrap(auditLogRequest.getOggOper());
        if (currentOgg != null && !currentOgg.isBlank()) {
            auditLogRequest.setOggOper(JsonNullable.of(currentOgg));
        }

        auditLogRequest.setKeyOper(null);
        auditLogRequest.setResponsePayload(null);
        auditLogRequest.setEsitoChiamata(200);

        if (auditPayloadMapper != null && fileId != null) {
            Map<String, Object> auditPayload = new HashMap<>();
            auditPayload.put("fileName", fileName);
            auditPayload.put("filePath", filePath);
            auditPayload.put("checksum", checksum);
            auditPayload.put("contentType", contentType);
            auditPayload.put("size", size);
            auditPayload.put("fileId", fileId);

            auditLogRequest.setRequestPayload(
                    JsonNullable.of(auditPayloadMapper.toBytes(auditPayload)));
        }
    }

}