package it.csi.registry.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.services.AuditService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }


    @PostMapping(value = "/salva", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> salvaAudit(
            @RequestPart("auditLogRequest") AuditLogRequest request) {
        Long id = auditService.salvaAudit(request);
        return ResponseEntity.ok(id);
    }

    @PostMapping("/login")
    public ResponseEntity<Long> salvaAuditLogin(@RequestParam String idApp,
                                                @RequestParam String ipAddress,
                                                @RequestParam String utente,
                                                @RequestParam(defaultValue = "login") String operazione,
                                                @RequestParam(defaultValue = "200") Integer esitoChiamata) {
        Long id = auditService.salvaAuditLoginLogout(idApp, ipAddress, utente, operazione, esitoChiamata);
        return ResponseEntity.ok(id);
    }
}