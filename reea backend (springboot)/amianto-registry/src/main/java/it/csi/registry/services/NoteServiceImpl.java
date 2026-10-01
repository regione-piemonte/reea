package it.csi.registry.services;

import java.util.List;
import org.springframework.stereotype.Service;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.NotaDTO;
import it.csi.registry.repositories.NoteRepository;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;

    public NoteServiceImpl(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Override
    public List<NotaDTO> getNote(Long soggettoId) {
        return noteRepository.findBySoggettoId(soggettoId);
    }

    @Override
    public NotaDTO inserisciNota(Long soggettoId, String descrizione, AuditLogRequest auditLogRequest) {
        return noteRepository.inserisci(soggettoId, descrizione, auditLogRequest);
    }

    @Override
    public NotaDTO modificaNota(Long notaId, String descrizione, AuditLogRequest auditLogRequest) {
        return noteRepository.modifica(notaId, descrizione, auditLogRequest);
    }

    @Override
    public void eliminaNota(Long notaId) {
        noteRepository.elimina(notaId);
    }
}
