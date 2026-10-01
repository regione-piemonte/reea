package it.csi.registry.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import it.csi.registry.model.*;
import org.springframework.web.multipart.MultipartFile;

import it.csi.registry.util.ExcelFileUtils.FileSalvato;

public interface SpresalService {
	
	FileSalvato importSpresal(MultipartFile file,
            AuditLogRequest auditLogRequest) throws IOException;
	
	FileSalvato importSpresalEsiti(MultipartFile file, AuditLogRequest auditLogRequest) throws IOException;
	
	List<SpresalDTO> getRecordTabSpresalAnamnesi(Integer fileId) throws IOException;
	
	List<SpresalEsitiDTO> getRecordTabSpresalEsiti(Integer fileId) throws IOException;

	List<SpresalEsitiDTO> getEsitiByRegistroId(Integer registroId);

	Integer aggiornaSpresalAnamnesiFlagElaboratoIndipendente(AnagraficaDTO dto);
	
	Integer aggiornaSpresalEsitiFlagElaboratoIndipendente(SpresalEsitiDTO dto);

	void eliminaSpresalEsitiSenzaRegistroId();

	void aggiornaCrpt(Integer regSpresalAnamnesiId, Map<String, Object> body, AuditLogRequest auditLogRequest);

	void aggiornaPrestazioneAcquisita(Integer esitoId, Map<String, Object> body, AuditLogRequest auditLogRequest);

	void aggiornaCounseling(Integer regSpresalAnamnesiId, Map<String, Object> body, AuditLogRequest auditLogRequest);
	void esportaAllegato4(int anno, String utente);
	List<Allegato4ElaborazioneDTO> listaElaborazioniAllegato4();
	org.springframework.core.io.Resource getFileAllegato4(Integer elaborazioneId);
	
	void chiudiTuttiRecordTabSpresalAnamnesiByFile(Integer fileId);
	
	void chiudiTuttiRecordTabSpresalEsitiByFile(Integer fileId);

}
