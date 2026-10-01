package it.csi.registry.services;

import java.io.File;
import it.csi.registry.model.PdfListResponse;

/**
 * Interfaccia per il servizio di gestione PDF su filesystem.
 */
public interface PdfService {

    /**
     * Recupera la lista di tutti i PDF che iniziano con il prefisso dato.
     *
     * @param prefisso il prefisso numerico (es: 200, 201)
     * @return PdfListResponse con la lista dei file trovati
     */
    PdfListResponse listaPdfPerPrefisso(int prefisso);

    File getFile(String nomeFile);
}
