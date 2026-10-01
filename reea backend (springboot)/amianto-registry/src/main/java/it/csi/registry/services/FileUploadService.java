package it.csi.registry.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Interfaccia per il servizio di upload e gestione file PDF.
 */
public interface FileUploadService {

    /**
     * Salva un file PDF nel filesystem configurato, nella cartella root di upload.
     *
     * @param file il file MultipartFile ricevuto dalla richiesta
     * @return il path assoluto del file salvato
     * @throws IOException se il salvataggio fallisce
     * @throws IllegalArgumentException se il file non è valido o non è un PDF
     */
    String salvaPdf(MultipartFile file) throws IOException;

    /**
     * Salva un file PDF nel filesystem configurato, in una sottocartella specifica.
     *
     * @param file il file MultipartFile ricevuto dalla richiesta
     * @param cartella la sottocartella di destinazione (es: "anagrafiche", "sorveglianza")
     * @return il path assoluto del file salvato
     * @throws IOException se il salvataggio fallisce
     * @throws IllegalArgumentException se il file non è valido o non è un PDF
     */
    String salvaPdf(MultipartFile file, String cartella) throws IOException;

    /**
     * Elimina un file dato il suo path assoluto.
     *
     * @param percorsoFile il path assoluto del file da eliminare
     * @throws IOException se l'eliminazione fallisce
     */
    void eliminaPdf(String percorsoFile) throws IOException;
}
