package it.csi.registry.repositories;

import it.csi.registry.model.AuditLogRequest;

public interface AuditRepository {

    Long insertAudit(AuditLogRequest request);
    
    void inserisciLog(AuditLogRequest request);
}
