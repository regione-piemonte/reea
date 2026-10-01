package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.CsiLogAudit.CSI_LOG_AUDIT;

import java.time.LocalDateTime;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.AuditLogRequest;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepositoryImpl implements AuditRepository {

    private final DSLContext dsl;

    public AuditRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    // metodi repository

    public void inserisciLog(AuditLogRequest request) {
        dsl.insertInto(CSI_LOG_AUDIT)
                .set(CSI_LOG_AUDIT.ID_APP, "REEAFE")
            .set(CSI_LOG_AUDIT.DATA_ORA, LocalDateTime.now())
            .set(CSI_LOG_AUDIT.UTENTE, request.getUtente())
            .set(CSI_LOG_AUDIT.IP_ADDRESS, request.getIpAddress())
            .set(CSI_LOG_AUDIT.OPERAZIONE, request.getOperazione())
            .set(CSI_LOG_AUDIT.ESITO_CHIAMATA, request.getEsitoChiamata())
            .set(CSI_LOG_AUDIT.OGG_OPER, 
                 request.getOggOper() != null 
                    ? request.getOggOper().toString() 
                    : null)
            .set(CSI_LOG_AUDIT.KEY_OPER, 
                 request.getKeyOper() != null 
                    ? request.getKeyOper().toString() 
                    : null)
            .execute();
    }
    

    @Override
    public Long insertAudit(AuditLogRequest request) {
        Long id = dsl
                .insertInto(CSI_LOG_AUDIT)
                .set(CSI_LOG_AUDIT.DATA_ORA, LocalDateTime.now())
                .set(CSI_LOG_AUDIT.ID_APP, request.getIdApp())
                .set(CSI_LOG_AUDIT.IP_ADDRESS, request.getIpAddress())
                .set(CSI_LOG_AUDIT.UTENTE, request.getUtente())
                .set(CSI_LOG_AUDIT.OPERAZIONE, request.getOperazione())
                .set(CSI_LOG_AUDIT.OGG_OPER, unwrap(request.getOggOper()))
                .set(CSI_LOG_AUDIT.KEY_OPER, unwrap(request.getKeyOper()))
                .set(CSI_LOG_AUDIT.UUID, unwrap(request.getUuid()))
                .set(CSI_LOG_AUDIT.REQUEST_PAYLOAD, unwrap(request.getRequestPayload()))
                .set(CSI_LOG_AUDIT.RESPONSE_PAYLOAD, unwrap(request.getResponsePayload()))
                .set(CSI_LOG_AUDIT.ESITO_CHIAMATA, request.getEsitoChiamata())
                .returning(CSI_LOG_AUDIT.AUDIT_ID)
                .fetchOne(CSI_LOG_AUDIT.AUDIT_ID);

        return id;
    }


    private <T> T unwrap(JsonNullable<T> value) {
        return value != null ? value.orElse(null) : null;
    }
}