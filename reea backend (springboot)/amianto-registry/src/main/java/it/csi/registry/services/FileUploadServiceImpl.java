package it.csi.registry.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileUploadServiceImpl implements FileUploadService{

//    private final String uploadDir;
	@Value("${pdf.upload.dir}")
    private String baseDir;


    /**
     * Salva un file PDF nel filesystem configurato.
     * Il nome del file viene rinominato con timestamp per evitare sovrascritture.
     *
     * @param file il file MultipartFile ricevuto
     * @param cartellaolicitata la sottocartella (es: "anagrafiche", "sorveglianza")
     * @return il path assoluto del file salvato
     * @throws IOException se il salvataggio fallisce
     */
    public String salvaPdf(MultipartFile file, String cartellaolicitata) throws IOException {
        // Validazione PDF
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Il file non può essere vuoto");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || 
        	    !(fileName.toLowerCase().endsWith(".pdf") || fileName.toLowerCase().endsWith(".xlsx"))) {
            throw new IllegalArgumentException("Estensione non valida. Consente solo file PDF e XLSX");
        }

        // Genera nome file univoco con timestamp
//        String timestamp = LocalDateTime.now()
//                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_"));
        String cleanFileName = sanitizeFileName(fileName);
        String newFileName = cleanFileName;

        // Build path: uploadDir + sottocartella + nomefile
        Path targetPath = Paths.get(baseDir, 
                                     cartellaolicitata != null ? cartellaolicitata : "", 
                                     newFileName);

        // Crea le directory se non esistono
        Files.createDirectories(targetPath.getParent());

        // Salva il file
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return targetPath.toAbsolutePath().toString();
    }

    /**
     * Salva un file PDF nella cartella root di upload (senza sottocartella).
     */
    public String salvaPdf(MultipartFile file) throws IOException {
        return salvaPdf(file, null);
    }

    /**
     * Pulisce il nome del file per evitare path traversal e caratteri pericolosi.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "file_sconosciuto";
        }
        // Rimuovi caratteri speciali e spazi
        String clean = fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        // Evita che il nome inizi con "."
        if (clean.startsWith(".")) {
            clean = "file" + clean;
        }
        return clean;
    }

    /**
     * Elimina un file dato il suo path assoluto.
     */
    public void eliminaPdf(String percorsoFile) throws IOException {
        Path filePath = Paths.get(percorsoFile);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }
}