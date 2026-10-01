package it.csi.registry.repositories;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FilenameFilter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import it.csi.registry.model.PdfInfo;

/**
 * Repository per la ricerca e recupero di file PDF dal filesystem.
 * I file devono avere lo schema: {prefisso}_*.pdf
 * Esempio: 200_Scheda_S03_...Nominativo-1.pdf, 200_Scheda_S05_...Nominativo-1.pdf
 */
@Repository
public class PdfRepositoryImpl implements PdfRepository{

    private final String baseDir;

    // Costruttore manuale
    public PdfRepositoryImpl(@Value("${pdf.upload.dir}") String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Cerca tutti i file PDF che iniziano con il prefisso dato.
     * Esempio: prefisso=200 trova 200_Scheda_S03_*.pdf e 200_Scheda_S05_*.pdf
     *
     * @param prefisso il prefisso numerico (es: 200, 201)
     * @return lista di PdfInfo metadata
     */
    public List<PdfInfo> cercaPerPrefisso(int prefisso) {
        List<PdfInfo> risultato = new ArrayList<>();

        File directory = new File(baseDir);
        if (!directory.exists() || !directory.isDirectory()) {
            return risultato;
        }

        String prefix = prefisso + "_";
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefix) && name.toLowerCase().endsWith(".pdf");
            }
        });

        if (files == null || files.length == 0) {
            return risultato;
        }

        for (File file : files) {
            OffsetDateTime dataModifica = Instant.ofEpochMilli(file.lastModified())
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();

            PdfInfo pdfInfo = new PdfInfo();
            pdfInfo.setNomeFile(file.getName());
            pdfInfo.setPercorsoAssoluto(file.getAbsolutePath());
            pdfInfo.setDimensione(file.length());
            pdfInfo.setDataModifica(dataModifica);

            risultato.add(pdfInfo);
        }

        return risultato;
    }

    /**
     * Ottiene un file PDF dato il nome, se esiste nella baseDir.
     *
     * @param nomeFile il nome del file (es: 200_Scheda_S03_...pdf)
     * @return il File se esiste, null altrimenti
     */
    public File getFileSync(String nomeFile) {
        if (nomeFile == null || nomeFile.isEmpty()) {
            return null;
        }
        // Security: prevent path traversal
        if (nomeFile.contains("..") || nomeFile.contains("/")) {
            return null;
        }
        File file = new File(baseDir, nomeFile);
        if (file.exists() && file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
            return file;
        }
        return null;
    }
}