package it.csi.registry.repositories;

import java.io.File;
import java.util.List;

import it.csi.registry.model.PdfInfo;

/**
 * Interfaccia repository per la ricerca e il recupero di file PDF dal filesystem.
 */
public interface PdfRepository {

    /**
     * Cerca tutti i file PDF che iniziano con il prefisso dato.
     *
     * @param prefisso il prefisso numerico del file
     * @return lista di metadati dei PDF trovati
     */
    List<PdfInfo> cercaPerPrefisso(int prefisso);

    /**
     * Restituisce il file PDF dato il nome del file, se presente nella directory base.
     *
     * @param nomeFile nome del file PDF
     * @return il file se esiste, null altrimenti
     */
    File getFileSync(String nomeFile);
}