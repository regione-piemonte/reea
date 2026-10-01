package it.csi.registry.repositories;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.NotaDTO;

import java.util.List;

public interface NoteRepository {
	List<NotaDTO> findBySoggettoId(Long soggettoId);
	NotaDTO inserisci(Long soggettoId, String descrizione, AuditLogRequest auditLogRequest);
	NotaDTO modifica(Long notaId, String descrizione, AuditLogRequest auditLogRequest);
	void elimina(Long notaId);
}

