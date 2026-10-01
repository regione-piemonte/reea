package it.csi.registry.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.csi.registry.model.*;

public interface SpresalRepository {
	
	void scaricoDatiExcelInTRegistroSpresalAnamnesi(List<SpresalDTO> rows, Integer fileId, String utenteLogin);
	
	void scaricoDatiExcelInTRegistroSpresalEsiti(List<SpresalEsitiDTO> rows, Integer fileId, String utenteLogin);

    void deleteRecordNonProcessati();

	Set<String> getListaRecordApertiSpresalAnamnesi();
	
	Set<String> getListaRecordRegistroIdIsNotNullSpresalAnamnesi();
	
	Set<String> getListaRecordApertiSpresalEsiti();
	
	Set<String> getListaRecordRegistroIdIsNotNullSpresalEsiti();

	List<SpresalDTO> listaRecordSpresalAnamnesi(Integer fileId);
	
	List<SpresalEsitiDTO>  listaRecordSpresalEsiti(Integer fileId);

	Integer aggiornaSpresalRegistroId(AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);

	Integer chiudiRecordDuplicatoSpresal(SpresalDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);
	
//	Integer chiudiRecordDuplicatoSpresalEsiti(SpresalEsitiDTO dto);
	Integer chiudiRecordDuplicatoSpresalEsiti(SpresalEsitiDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);
	
	Integer aggiornaSpresalEsitiRegistroId(AnagraficaDTO dto, Integer regSpresalEsitiId, Integer elaborazioneId, String utenteModifica, Integer tipoOperazione);
	
	SpresalDTO recuperaOggettoTRegistroSpresalAnamnesiByCF(String codiceFiscale, boolean registroIdGiaAssegnato, String occupazioneNum, boolean elaborato);
	
	SpresalEsitiDTO recuperaOggettoTRegistroSpresalEsitiByCF(String codiceFiscale, boolean registroIdGiaAssegnato, LocalDate dataVisita, boolean elaborato);

	List<SpresalEsitiDTO> getEsitiByRegistroId(Integer registroId);
	
	Integer aggiornaSpresalAnamnesiFlagElaborato(AnagraficaDTO dto);
	
	Integer aggiornaSpresalEsitiFlagElaborato(SpresalEsitiDTO dto);

	void eliminaSpresalEsitiSenzaRegistroId();

	void aggiornaCrpt(Integer regSpresalAnamnesiId, Map<String, Object> body, AuditLogRequest auditLogRequest);

	void aggiornaCounseling(Integer regSpresalAnamnesiId, Map<String, Object> body, AuditLogRequest auditLogRequest);

	void aggiornaPrestazioneAcquisita(Integer esitoId, Map<String, Object> body, AuditLogRequest auditLogRequest);


	List<SpresalEsitiDTO> queryAllegato4Tutti(int anno);
	List<Allegato4ElaborazioneDTO> listaElaborazioniAllegato4();
	String getFilePathAllegato4(Integer elaborazioneId);
	
	void chiudiTuttiRecordTabRegistroSpresalAnamnesiByFile(Integer fileId);
	
	void chiudiTuttiRecordTabRegistroSpresalEsitiByFile(Integer fileId);

}
