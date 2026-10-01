package it.csi.registry.services;

import java.util.List;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.NotaDTO;

public interface NoteService {
	List<NotaDTO> getNote(Long soggettoId);
	NotaDTO inserisciNota(Long soggettoId, String descrizione, AuditLogRequest auditLogRequest);
	NotaDTO modificaNota(Long notaId, String descrizione, AuditLogRequest auditLogRequest);
	void eliminaNota(Long notaId);
}
