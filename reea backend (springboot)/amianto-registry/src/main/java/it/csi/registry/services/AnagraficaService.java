package it.csi.registry.services;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.ListaAnagraficheResponse;
import it.csi.registry.model.SpresalEsitiDTO;
import it.csi.registry.model.StoriaStatoDTO;
import it.csi.registry.util.ExcelExportResult;

public interface AnagraficaService {

//	List<AnagraficaDTO> getLista(List<Integer> filtroFonteId, List<String> descrizioniStato, List<String> sezione, 
//											Boolean insInSorveglianza, String tipoElencoInail, List<Integer> assistenzaAslId,
//	                                        AuditLogRequest auditLogRequest );
	
	ListaAnagraficheResponse getLista(List<Integer> filtroFonteId,
            List<String> descrizioniStato,
            List<String> sezione,
            Boolean insInSorveglianza,
            String tipoElencoInail,
            List<Integer> assistenzaAslId,
                                      String codiceFiscale,
                                      String cognome,
                                      String nome,
                                      String cognomeLettDa,
                                      String cognomeLettA,
                                      LocalDate nascitaData,
                                      Boolean azzeraContatoreProssimoStep,
                                      String profiloUtente,
            AuditLogRequest auditLogRequest);
	
	Integer creazioneAnagrafica(AnagraficaDTO dto, Boolean inserimnetoDafile, Integer elaborazioneId, AuditLogRequest auditLogRequest);
	
	Integer creazioneAnagraficaInailA(AnagraficaDTO dto, Integer elaborazioneId) throws IOException;
	
	Integer creazioneAnagraficaInailB(AnagraficaDTO dto, Integer elaborazioneId) throws IOException;
	
	Integer creazioneAnagraficaSpresalAnamnesi(AnagraficaDTO dto, Integer elaborazioneId);
	
	Integer creazioneAnagraficaSpresalEsiti(SpresalEsitiDTO dto, Integer elaborazioneId);
	
	AnagraficaDTO getById(Long soggettoId);
	
	Map<String, String> getByIdCampiAnonimizzati(Long soggettoId, Boolean flagDatiAnonimizzati, String profiloUtente);
	
	AnagraficaDTO getByCodiceFiscale(String codiceFiscale);
	
	Integer getRegistroIdBySoggettoId(Integer soggettoId);
	
	AnagraficaDTO aggiornaSoggetto(Long soggettoId, AnagraficaDTO dto, String cfOperatore, AuditLogRequest auditLogRequest);
	
	List<AnagraficaDTO> getByCForNomeOrCognomeOrDataNascita(String filtroCodiceFiscale, String filtroNome, String filtroCognome, LocalDate filtroNascitaData);

	void aggiornaStato(Long soggettoId, AnagraficaDTO dto, AuditLogRequest auditLogRequest);

	List<StoriaStatoDTO> getStoriaStati(Long soggettoId);
	
	Map<String, Integer> scaricaDatiExcelTotaliAsincroni(Boolean flagDatiAnonimizzati, Integer assistenzaAslId, String profiloUtente, String cfOperatore);
	
	Integer recuperaScarti(Integer fileId);
	
	void chiudiTuttiRecordTabAdesioneByFile(Integer fileId);
}
