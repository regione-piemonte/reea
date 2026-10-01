package it.csi.registry.services;

import org.jooq.DSLContext;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.TracciaFileDTO;
import it.csi.registry.repositories.TracciaElaborazioneRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class TracciaElaborazioneServiceImpl implements TracciaElaborazioneService {

    @Autowired
    private TracciaElaborazioneRepository tracciaElaborazioneRepository;

    @Autowired
    private DSLContext dsl;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private AuditPayloadMapper auditPayloadMapper;


	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Integer inserisciErroreRiga(Integer elaborazioneId,
									   String codiceErrore, String descrizioneErrore, String contenutoRigaRaw,
									   String utenteCreazione, String cognome, String nome,
									   LocalDate dataNascita, String sesso, String codiceFiscale, String targetTabellaNome, Integer targetRecordId) {

		return tracciaElaborazioneRepository.inserisciErroreRiga(
				dsl, elaborazioneId, codiceErrore,
				descrizioneErrore, contenutoRigaRaw, utenteCreazione,
				cognome, nome, dataNascita, sesso, codiceFiscale, targetTabellaNome,                                    // ‚Üê passa diretto, senza fallback
				targetRecordId != null ? targetRecordId : 0
		);
	}
	
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Integer inserisciScarto(Integer fileId,
									   String codiceErrore, String descrizioneErrore, String contenutoRigaRaw,
									   String utenteCreazione, String cognome, String nome,
									   LocalDate dataNascita, String sesso, String codiceFiscale, String targetTabellaNome, Integer targetRecordId) {

		return tracciaElaborazioneRepository.inserisciScarto(
				dsl, fileId, codiceErrore,
				descrizioneErrore, contenutoRigaRaw, utenteCreazione,
				cognome, nome, dataNascita, sesso, codiceFiscale, targetTabellaNome,                                    // ‚Üê passa diretto, senza fallback
				targetRecordId != null ? targetRecordId : 0
		);
	}


	@Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Integer inserisciStatoElaborazione(Integer fileId, String utente, Integer statoFile) {
        return tracciaElaborazioneRepository.inserisciTracciaElaborazione(
                dsl,
                fileId,
                statoFile,
                utente
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Integer aggiornaFineOk(Integer elaborazioneId,
    		                      Integer fileStatoId,
                                  Integer righeTotali,
                                  Integer righeElaborate,
                                  Integer righeScartate,
                                  Integer righeModificanti,
                                  Integer righeErrore,
                                  String messaggio,
                                  String utente) {
        return tracciaElaborazioneRepository.aggiornaTracciaElaborazioneFineOk(
                dsl,
                elaborazioneId,
                fileStatoId,
                righeTotali,
                righeElaborate,
                righeScartate,
                righeModificanti,
                righeErrore,
                messaggio,
                utente
        );
    }

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Integer inserisciFile(String fileName, String filePath, String fileHashChecksum,
			String fileMimeType, long fileDimensioneBytes, Integer fileStatoId, String utente) {
		return tracciaElaborazioneRepository.inserisciFile(dsl, fileName, filePath, fileHashChecksum, fileMimeType, fileDimensioneBytes, fileStatoId, utente);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Integer aggiornaFile(Integer fileId, Integer fileStatoId, String utenteCreazione) {
		return tracciaElaborazioneRepository.aggiornaFile(dsl, fileId, fileStatoId, utenteCreazione);
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Integer aggiornaFileExcel(Integer fileId, Integer fileStatoId, String checksum, String contentType, Long size, String utenteCreazione) {
		return tracciaElaborazioneRepository.aggiornaFileExcel(dsl, fileId, fileStatoId, checksum, contentType, size, utenteCreazione);
	}
	
	
	@Override
	public void inserisciFileImpatto(
	        Integer elaborazioneId,
	        String targetTabellaNome,
	        Integer targetRecordId,
	        Integer operazioneTipoId,
	        String utente) {

	    tracciaElaborazioneRepository.inserisciFileImpatto(
	    		dsl,
	            elaborazioneId,
	            targetTabellaNome,
	            targetRecordId,
	            operazioneTipoId,
	            utente
	    );
	}


	@Override
	public TracciaFileDTO recuperaFileByName(String fileName, AuditLogRequest auditLogRequest) {
		
		TracciaFileDTO tracciaFileDTO = null;
		
        tracciaFileDTO = tracciaElaborazioneRepository.recuperaFileByName(dsl,fileName);
        
        if(auditLogRequest == null)
        	auditLogRequest = new AuditLogRequest();

        auditLogRequest.setOperazione("Export asincrono"); // dovr‡ essere passato dal FE
        auditLogRequest.setOggOper(JsonNullable.of("Dati assistito")); // dovr‡ essere passato dal FE
        auditLogRequest.setKeyOper(null);
        auditLogRequest.setRequestPayload(null);
        auditLogRequest.setEsitoChiamata(200);

        Map<String, Object> auditPayload = new HashMap<>();
        auditPayload.put("fileName", tracciaFileDTO != null ? tracciaFileDTO.getFileName() : null);
//        auditPayload.put("contentType", contentType);
        auditPayload.put("size", tracciaFileDTO != null ? tracciaFileDTO.getFileSize() : null);
        
        auditLogRequest.setResponsePayload(JsonNullable.of(auditPayloadMapper.toBytes(auditPayload)));

        auditService.salvaAudit(auditLogRequest);
        
        return tracciaFileDTO;
	}

}
