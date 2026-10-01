package it.csi.registry.services;

import java.time.LocalDate;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.TracciaFileDTO;

public interface TracciaElaborazioneService {
	
	
	//inserisci file in t_file
	Integer inserisciFile(String fileName, String filePath, String fileHashChecksum, String fileMimeType, long fileDimensioneBytes, Integer fileStatoId, String utente);
	Integer aggiornaFile(Integer fileId, Integer fileStatoId, String utenteCreazione);
	Integer aggiornaFileExcel(Integer fileId, Integer fileStatoId, String checksum, String contentType, Long size, String utenteCreazione);
	
	// Inserimento traccia
	Integer inserisciStatoElaborazione(Integer fileId, String utenteCreazione, Integer statoFile);

	// Aggiornamento stato traccia
	Integer aggiornaFineOk(Integer elaborazioneId, Integer fileStatoId, Integer righeTotali, Integer righeElaborate, Integer righeScartate,
										Integer righeModificanti,Integer righeErrore, String messaggioSistema, String utenteModifica);

	Integer inserisciErroreRiga(Integer elaborazioneId,
								String codiceErrore, String descrizioneErrore, String contenutoRigaRaw,
								String utenteCreazione, String cognome, String nome,
								LocalDate dataNascita, String sesso, String codiceFiscale, String targetTabellaNome, Integer targetRecordId);
	
	Integer inserisciScarto(Integer fileId,
			String codiceErrore, String descrizioneErrore, String contenutoRigaRaw,
			String utenteCreazione, String cognome, String nome,
			LocalDate dataNascita, String sesso, String codiceFiscale, String targetTabellaNome, Integer targetRecordId);


	public void inserisciFileImpatto(
	        Integer elaborazioneId,
	        String targetTabellaNome,
	        Integer targetRecordId,
	        Integer operazioneTipoId,
	        String utente);

	TracciaFileDTO recuperaFileByName(String name, AuditLogRequest auditLogRequest);
}
