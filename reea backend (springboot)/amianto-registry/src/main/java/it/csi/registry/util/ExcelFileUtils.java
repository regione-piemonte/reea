package it.csi.registry.util;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.web.multipart.MultipartFile;

public class ExcelFileUtils {

    private ExcelFileUtils() {
        // utility class
    }
    
    public record FileSalvato(String fileName, String filePath, String checksum) {}

    public static FileSalvato salvaExcelFileSystem(MultipartFile file, String baseDir) throws IOException {
        File dir = new File(baseDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Impossibile creare directory " + baseDir);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "upload.xlsx";
        }

        int dotIndex = originalName.lastIndexOf('.');
        String baseName = (dotIndex > 0) ? originalName.substring(0, dotIndex) : originalName;
        String ext      = (dotIndex > 0) ? originalName.substring(dotIndex)    : "";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);

        String uniqueName = baseName + "_" + timestamp + ext;
        File dest = new File(dir, uniqueName);

        file.transferTo(dest);

        if (!dest.exists() || dest.length() == 0L) {
            throw new IOException("File non trovato o vuoto dopo transferTo: " + dest.getAbsolutePath());
        }
        
        String checksum = calcolaSha256(dest);

        System.out.println("File preadesione salvato in " + dest.getAbsolutePath());
        return new FileSalvato(uniqueName, dest.getAbsolutePath(), checksum);
    }
    
    
    private static String calcolaSha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 non disponibile", e);
        }
    }
    
    
    public static FileSalvato salvaExcelExportResult(ExcelExportResult exportResult, String baseDir) throws IOException {

        // Crea la directory se non esiste
        File dir = new File(baseDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Impossibile creare directory " + baseDir);
        }

        // Ricava nome e estensione dal fileName dell'ExcelExportResult
        String originalName = exportResult.getFileName();
        if (originalName == null || originalName.isBlank()) {
            originalName = "export.xlsx";
        }

        int dotIndex = originalName.lastIndexOf('.');
        String baseName = (dotIndex > 0) ? originalName.substring(0, dotIndex) : originalName;
        String ext      = (dotIndex > 0) ? originalName.substring(dotIndex)    : ".xlsx";

//        // Aggiunge timestamp per unicità
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
//        String timestamp = LocalDateTime.now().format(formatter);
//
        String uniqueName = baseName + ext;
        File dest = new File(dir, uniqueName);

        // Scrive il byte[] sul filesystem
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(exportResult.getContent());
            fos.flush();
        }

        // Verifica che il file sia stato creato correttamente
        if (!dest.exists() || dest.length() == 0L) {
            throw new IOException("File non trovato o vuoto dopo la scrittura: " + dest.getAbsolutePath());
        }

        // Calcola il checksum SHA-256
        String checksum = calcolaSha256(dest);

        System.out.println("File export salvato in " + dest.getAbsolutePath());
        return new FileSalvato(uniqueName, dest.getAbsolutePath(), checksum);
    }
    

//    public static String salvaExcelFileSystem(MultipartFile file, String baseDir) throws IOException {
//        // crea la directory se non esiste
//        File dir = new File(baseDir);
//        if (!dir.exists() && !dir.mkdirs()) {
//            throw new IOException("Impossibile creare directory " + baseDir);
//        }
//
//        String originalName = file.getOriginalFilename();
//        if (originalName == null || originalName.isBlank()) {
//            originalName = "upload.xlsx"; // fallback
//        }
//
//        int dotIndex = originalName.lastIndexOf('.');
//        String baseName = (dotIndex > 0) ? originalName.substring(0, dotIndex) : originalName;
//        String ext      = (dotIndex > 0) ? originalName.substring(dotIndex)    : "";
//
//        // timestamp: es. 20260331_173945
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
//        String timestamp = LocalDateTime.now().format(formatter);
//
//        String uniqueName = baseName + "_" + timestamp + ext;
//        File dest = new File(dir, uniqueName);
//
//        file.transferTo(dest);
//
//        if (!dest.exists() || dest.length() == 0L) {
//            throw new IOException("File non trovato o vuoto dopo transferTo: " + dest.getAbsolutePath());
//        }
//
//        System.out.println("File preadesione salvato in " + dest.getAbsolutePath());
//        return dest.getAbsolutePath(); // oppure solo uniqueName, se preferisci
//    }
}