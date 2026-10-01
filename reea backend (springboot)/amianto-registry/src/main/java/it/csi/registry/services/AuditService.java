package it.csi.registry.services;

import it.csi.registry.model.AuditLogRequest;

public interface AuditService {

    Long salvaAudit(AuditLogRequest request);

    Long salvaAuditLoginLogout(String idApp, String ipAddress, String utente, String operazione, Integer esitoChiamata);
    
    void logLoginFailed(String username, String ipClient);
    
    void logLogin(String username, String ipClient);
    
    void logLogout(String username, String ipClient);
    
    void preparaAuditRequest(
            AuditLogRequest auditLogRequest,
            String operazione,
            String fileName,
            String filePath,
            String checksum,
            String contentType,
            Long size,
            Integer fileId,
            AuditPayloadMapper auditPayloadMapper);
}