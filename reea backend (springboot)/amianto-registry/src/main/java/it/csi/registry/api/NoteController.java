package it.csi.registry.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.NotaDTO;
import it.csi.registry.services.NoteService;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/note")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/getByRegistroId/{soggettoId}")
    public ResponseEntity<List<NotaDTO>> getNote(@PathVariable("soggettoId") Long soggettoId) {
        return ResponseEntity.ok(noteService.getNote(soggettoId));
    }

//    @PostMapping("/inserisci")
//    public ResponseEntity<NotaDTO> inserisciNota(
//            @RequestParam("soggettoId") Long soggettoId,
//            @RequestParam("descrizione") String descrizione) {
//        return ResponseEntity.ok(noteService.inserisciNota(soggettoId, descrizione));
//    }
    
    @PostMapping(value = "/inserisci", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NotaDTO> inserisciNota(
            @RequestParam("soggettoId") Long soggettoId,
            @RequestParam("descrizione") String descrizione,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
        try {
            return ResponseEntity.ok(noteService.inserisciNota(soggettoId, descrizione, auditLogRequest));
        } catch (Exception e) {
            System.out.println("Errore durante l'inserimento della nota: " + e);
            return ResponseEntity.badRequest().build();
        }
    }

//    @PutMapping("/modifica/{notaId}")
//    public ResponseEntity<NotaDTO> modificaNota(
//            @PathVariable("notaId") Long notaId,
//            @RequestParam("descrizione") String descrizione) {
//        return ResponseEntity.ok(noteService.modificaNota(notaId, descrizione));
//    }
    @PutMapping(value = "/modifica/{notaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NotaDTO> modificaNota(
            @PathVariable("notaId") Long notaId,
            @RequestParam("descrizione") String descrizione,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
    	try {
            return ResponseEntity.ok(noteService.modificaNota(notaId, descrizione, auditLogRequest));
        } catch (Exception e) {
            System.out.println("Errore durante l'aggiornamento della nota: " + e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/elimina/{notaId}")
    public ResponseEntity<Void> eliminaNota(@PathVariable("notaId") Long notaId) {
        noteService.eliminaNota(notaId);
        return ResponseEntity.noContent().build();
    }
}
