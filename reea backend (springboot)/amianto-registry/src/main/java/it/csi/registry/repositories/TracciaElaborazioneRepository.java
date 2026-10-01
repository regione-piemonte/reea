package it.csi.registry.repositories;

import org.jooq.DSLContext;

import it.csi.registry.model.TracciaFileDTO;

import java.time.LocalDate;

public interface TracciaElaborazioneRepository {
	
	//inserisci file in t_file
	Integer inserisciFile(DSLContext ctx, String fileName, String filePath, String fileHashChecksum, String fileMimeType, long fileDimensioneBytes, Integer fileStatoId, String utente);
	Integer aggiornaFile(DSLContext ctx, Integer fileId, Integer fileStatoId, String utenteCreazione);
	Integer aggiornaFileExcel(DSLContext ctx, Integer fileId, Integer fileStatoId, String checksum, String contentType, Long size, String utenteCreazione);
	
	// Inserimento traccia
	Integer inserisciTracciaElaborazione(DSLContext ctx, Integer fileId, Integer fileStatoId, String utenteCreazione);

	// Aggiornamento stato traccia
	Integer aggiornaTracciaElaborazioneFineOk(DSLContext ctx, Integer elaborazioneId, Integer fileStatoId, Integer righeTotali, Integer righeElaborate, Integer righeScartate,
										Integer righeModificanti,Integer righeErrore, String messaggioSistema, String utenteModifica);

	//Recupera il fileId 
	Integer findFileIdByFileHashChecksum(DSLContext ctx, String fileChecksum);
	
	Integer inserisciErroreRiga(DSLContext ctx,
								Integer elaborazioneId,
								String codiceErrore,
								String descrizioneErrore,
								String contenutoRigaRaw,
								String utenteCreazione,
	                            String cognome,
								String nome,
								LocalDate dataNascita, String sesso, String codiceFiscale,
								String targetNomeTabella, Integer targetRecordId);
	
	Integer inserisciScarto(DSLContext ctx,
			Integer fileId,
			String codiceErrore,
			String descrizioneErrore,
			String contenutoRigaRaw,
			String utenteCreazione,
            String cognome,
			String nome,
			LocalDate dataNascita, String sesso, String codiceFiscale,
			String targetNomeTabella, Integer targetRecordId);

	public Integer inserisciFileImpatto(
			DSLContext ctx,
	        Integer elaborazioneId,
	        String targetTabellaNome,
	        Integer targetRecordId,
	        Integer operazioneTipoId,
	        String utente);
	
	TracciaFileDTO recuperaFileByName(DSLContext dsl, String fileName);
	// DE
	Integer aggiornaFineValidita(DSLContext ctx, Integer fileId);
}
