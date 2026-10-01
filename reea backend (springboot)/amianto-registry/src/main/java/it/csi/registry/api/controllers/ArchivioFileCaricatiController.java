package it.csi.registry.api.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import it.csi.registry.model.ScartoFileDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.csi.registry.model.ArchivioFileCaricatiDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.ConcorrenzaCheckDTO;
import it.csi.registry.services.ArchivioFileCaricatiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/archivioFileCaricati")
public class ArchivioFileCaricatiController {
	
	private final ArchivioFileCaricatiService archivioFileCaricatiService;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ArchivioFileCaricatiController.class);

    public ArchivioFileCaricatiController(ArchivioFileCaricatiService archivioFileCaricatiService) {
    	this.archivioFileCaricatiService = archivioFileCaricatiService;
    }

    @GetMapping(value = "/checkNomeFile")
    public ResponseEntity<Boolean> checkNomeFile(@RequestParam("nomeFile") String nomeFile) {
        return ResponseEntity.ok(archivioFileCaricatiService.existsFileByName(nomeFile));
    }

    @GetMapping(value = "/checkConcorrenza")
    public ResponseEntity<ConcorrenzaCheckDTO> checkConcorrenza(@RequestParam("tipo") String tipo) {
        return ResponseEntity.ok(archivioFileCaricatiService.checkConcorrenza(tipo));
    }

    @PostMapping(value = "/getArchivioFileCaricati",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ArchivioFileCaricatiDTO> getArchivioFileCaricati(
    		@RequestPart(name = "filtro_fonte", required = false) String filtroFonte,
            @RequestPart(name = "data_da", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataDa,
            @RequestPart(name = "data_a", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataA,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
        List<ArchivioFileCaricatiDTO> lista = null;
        try {
            lista = archivioFileCaricatiService.getArchivioFileCaricati(filtroFonte, dataDa, dataA, auditLogRequest);
        } catch (IOException e) {
//            e.printStackTrace();
        	LOGGER.error("Errore in getArchivioFileCaricati: " , null, e);
        }
        return lista;
    }



    @PostMapping(value = "/getElaborazioneErroreFile/{elaborazioneId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ScartoFileDTO>> getElaborazioneErroreFile(@PathVariable("elaborazioneId") Integer elaborazioneId,
    		@RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
        return ResponseEntity.ok(archivioFileCaricatiService.getElaborazioneErroreFile(elaborazioneId, auditLogRequest));
    }


    @PostMapping(value = "/getScaricoErroreFile/{fileId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ScartoFileDTO>> getScaricoErroreFile(@PathVariable("fileId") Integer fileId,
    		@RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
        return ResponseEntity.ok(archivioFileCaricatiService.getScaricoErroreFile(fileId, auditLogRequest));
    }

    
    

}

