package it.csi.registry.api.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.ExportFileListDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import it.csi.registry.model.TracciaFileDTO;
import it.csi.registry.services.AuditService;
import it.csi.registry.services.FileUploadService;
import it.csi.registry.services.TracciaElaborazioneService;

import static it.csi.registry.jooq.tables.ReeaDFileStato.REEA_D_FILE_STATO;
import static it.csi.registry.jooq.tables.ReeaLFileElaborazione.REEA_L_FILE_ELABORAZIONE;
import static it.csi.registry.jooq.tables.ReeaTFile.REEA_T_FILE;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final DSLContext dsl;
    private final TracciaElaborazioneService tracciaElaborazioneService;
    private final FileUploadService fileUploadService;
    private final AuditService auditService;
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public FileController(DSLContext dsl, TracciaElaborazioneService tracciaElaborazioneService, FileUploadService fileUploadService,
    		AuditService auditService) {
        this.dsl = dsl;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
        this.fileUploadService = fileUploadService;
        this.auditService = auditService;
    }
    
    @PostMapping(value = "/download/{fileName}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> downloadFile(@PathVariable("fileName") String fileName, 
    		@RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
        try {
            TracciaFileDTO fileDto = tracciaElaborazioneService.recuperaFileByName(fileName, auditLogRequest);

            if (fileDto == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File non trovato per fileName=" + fileName
                );
            }

            String baseDir = fileDto.getBaseDir();
            String contentType = fileDto.getContentType();

            if (baseDir == null || baseDir.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Base directory non valorizzata per fileName=" + fileName
                );
            }

            String physicalFileName = fileName.endsWith(".xlsx") ? fileName : fileName + ".xlsx";
            Path filePath = Paths.get(baseDir, physicalFileName).normalize();

            if (!Files.exists(filePath)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File non presente sul filesystem: " + filePath
                );
            }

            if (Files.isDirectory(filePath)) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Il path individuato è una directory e non un file: " + filePath
                );
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "File non leggibile: " + filePath
                );
            }

            String resolvedContentType = contentType;
            if (resolvedContentType == null || resolvedContentType.isBlank()) {
                resolvedContentType = Files.probeContentType(filePath);
            }
            if (resolvedContentType == null || resolvedContentType.isBlank()) {
                resolvedContentType = "application/octet-stream";
            }

            String downloadFileName = physicalFileName;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resolvedContentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadFileName + "\"")
                    .contentLength(Files.size(filePath))
                    .body(resource);

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore nella costruzione della risorsa file",
                    ex
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il download del file",
                    ex
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore imprevisto durante il download",
                    ex
            );
        }
    }

    @GetMapping("/getExportList")
    public List<ExportFileListDTO> getExportList(
            @RequestParam(name = "assistenza_asl_id", required = false) Integer assistenzaAslId) {

        Condition cond = REEA_T_FILE.FILE_NAME.like("export_totale_%");
        if (assistenzaAslId != null) {
            cond = cond.and(REEA_T_FILE.FILE_NAME.like("export_totale_%_" + assistenzaAslId + "_%"));
        }

        return dsl
                .select(
                        REEA_T_FILE.FILE_ID,
                        REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID,
                        REEA_T_FILE.FILE_NAME,
                        REEA_D_FILE_STATO.FILE_STATO_DESC,
                        REEA_T_FILE.DATA_CREAZIONE,
                        REEA_T_FILE.DATA_MODIFICA,
                        REEA_T_FILE.UTENTE_CREAZIONE
                )
                .from(REEA_T_FILE)
                .join(REEA_L_FILE_ELABORAZIONE).on(REEA_L_FILE_ELABORAZIONE.FILE_ID.eq(REEA_T_FILE.FILE_ID))
                .join(REEA_D_FILE_STATO).on(REEA_D_FILE_STATO.FILE_STATO_ID.eq(REEA_T_FILE.FILE_STATO_ID))
                .where(cond)
                .orderBy(REEA_T_FILE.DATA_CREAZIONE.desc())
                .stream().map(r -> {
                    ExportFileListDTO row = new ExportFileListDTO();
                    String nome = r.get(REEA_T_FILE.FILE_NAME);
                    row.setFileId(r.get(REEA_T_FILE.FILE_ID) != null ? r.get(REEA_T_FILE.FILE_ID).intValue() : null);
                    row.setElaborazioneId(r.get(REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID) != null
                            ? r.get(REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID).intValue() : null);
                    row.setNome(nome);
                    row.setTipo(nome != null && nome.contains("_pseudo_") ? "pseudo" : "chiaro");
                    String statoDesc = r.get(REEA_D_FILE_STATO.FILE_STATO_DESC);
                    row.setStato(statoDesc);
                    row.setDataRichiesta(r.get(REEA_T_FILE.DATA_CREAZIONE) != null
                            ? r.get(REEA_T_FILE.DATA_CREAZIONE).format(fmt) : null);
                    boolean terminata = "ELABORAZIONE TERMINATA".equals(statoDesc);
                    row.setDataGenerazione(terminata && r.get(REEA_T_FILE.DATA_MODIFICA) != null
                            ? r.get(REEA_T_FILE.DATA_MODIFICA).format(fmt) : null);
                    row.setOperatore(r.get(REEA_T_FILE.UTENTE_CREAZIONE));
                    return row;
                }).collect(Collectors.toList());
    }


    @PostMapping(value = "/downloadImport/{fileId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> downloadImport(@PathVariable("fileId") Long fileId,
    		@RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {


        org.jooq.Record row = dsl.fetchOne(
                "SELECT file_name, file_path FROM reea.reea_t_file WHERE file_id = ?", fileId
        );

        if (row == null) return ResponseEntity.notFound().build();

        String nomeFile = row.get("file_name", String.class);
        String filePath = row.get("file_path", String.class);

        if (filePath == null) return ResponseEntity.notFound().build();

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return ResponseEntity.notFound().build();

        try {
            byte[] content = Files.readAllBytes(path);
            
            //
            auditService.salvaAudit(auditLogRequest);
            //
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + nomeFile + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

//    @GetMapping("/download/{fileName}")
//    public ResponseEntity<Resource> download(@PathVariable("fileName") String fileName) {
//        // Recupera il path dal DB
//        String filePath = dsl
//                .select(REEA_T_FILE.FILE_PATH)
//                .from(REEA_T_FILE)
//                .where(REEA_T_FILE.FILE_NAME.eq(fileName))
//                .fetchOne(REEA_T_FILE.FILE_PATH);
//
//        if (filePath == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        Path path = Paths.get(filePath);
//        Resource resource = new FileSystemResource(path);
//
//        if (!resource.exists()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION,
//                        "attachment; filename=\"" + path.getFileName().toString() + "\"")
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(resource);
//    }
    
    
    /**
     * Upload file PDF.
     *
     * Parametri form-data:
     * - file: il file PDF (obbligatorio)
     * - cartella: sottocartella di destinazione (opzionale)
     *
     * Esempio Postman:
     * POST http://localhost:8080/api/upload-pdf
     * Body → form-data → file (seleziona file PDF), cartella (testo opzionale)
     */
    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "cartella", required = false) String cartella) {

        Map<String, Object> response = new HashMap<>();

        try {
            String pathAssoluto = fileUploadService.salvaPdf(file, cartella);
            
            response.put("successo", true);
            response.put("percorso", pathAssoluto);
            response.put("nomeFile", file.getOriginalFilename());
            response.put("dimensione", file.getSize());
            response.put("tipo", file.getContentType());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("successo", false);
            response.put("errore", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            response.put("successo", false);
            response.put("errore", "Errore durante il salvataggio del file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Download file PDF per path.
     *
     * Esempio:
     * GET http://localhost:8080/api/download-pdf?percorso=C%3A%5Creeabe%5Cuploads%5Cpdf%5Cfile.pdf
     */
    @GetMapping(value = "/download-pdf", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@RequestParam("percorso") String percorso) {
        try {
            String decodedPath = URLDecoder.decode(percorso, StandardCharsets.UTF_8);
            Path filePath = Paths.get(decodedPath);

            if (!Files.exists(filePath) || !filePath.toString().toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            String fileName = filePath.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", 
                            "attachment; filename=\"" + fileName + "\"")
                    .body(fileContent);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     *Elimina file PDF.
     */
    @DeleteMapping("/elimina-pdf")
    public ResponseEntity<Map<String, Object>> eliminaPdf(@RequestParam("percorso") String percorso) {
        Map<String, Object> response = new HashMap<>();

        try {
            String decodedPath = URLDecoder.decode(percorso, StandardCharsets.UTF_8);
            fileUploadService.eliminaPdf(decodedPath);
            
            response.put("successo", true);
            response.put("messaggio", "File eliminato con successo");
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("successo", false);
            response.put("errore", "Errore durante l'eliminazione: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

