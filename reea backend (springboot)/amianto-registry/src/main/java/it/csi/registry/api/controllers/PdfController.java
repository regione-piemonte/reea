package it.csi.registry.api.controllers;

import java.io.File;
import java.util.Collections;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.model.PdfListResponse;
import it.csi.registry.services.PdfService;

@RestController
@RequestMapping("/api")
public class PdfController {

    private final PdfService pdfService;

    // Costruttore manuale
    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    /**
     * Recupera la lista dei PDF che iniziano con il prefisso dato.
     *
     * Esempio Postman:
     * GET http://localhost:8080/reeabe/api/pdf/lista/200
     *
     * @param prefisso il prefisso numerico (es: 200, 201)
     * @return PdfListResponse con la lista dei file
     */
    @GetMapping("/pdf/lista/{prefisso}")
    public ResponseEntity<PdfListResponse> listaPdf(@PathVariable("prefisso") int prefisso) {
        try {
            PdfListResponse response = pdfService.listaPdfPerPrefisso(prefisso);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            PdfListResponse response = new PdfListResponse();
            response.setPrefisso(prefisso);
            response.setTotaleTrovati(0);
            response.setPdfList(Collections.emptyList());

            return ResponseEntity.badRequest().body(response);
        }
    }
    
    
    @GetMapping("/pdf/download/{nomeFile:.+}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable("nomeFile") String nomeFile) {
        File file = pdfService.getFile(nomeFile);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentLength(file.length())
                .body(resource);
    }
}