package it.csi.registry.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import it.csi.registry.model.AslDTO;
import it.csi.registry.model.StoriaStatoDTO;
import org.jooq.DSLContext;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.ExportDTO;

public interface AnagraficaRepository {
	
	List<AnagraficaDTO> findListaAnagrafica(List<Integer> filtroFonteId,
            List<String> descrizioniStato,
            List<String> sezione,
            Boolean insInSorveglianza,
            String tipoElencoInail,
            List<Integer> assistenzaAslId,  String codiceFiscale,
                                            String cognome,
                                            String nome,
                                            String cognomeLettDa,
                                            String cognomeLettA,
                                            LocalDate nascitaData,
                                            Boolean azzeraContatoreProssimoStep,
                                            String profiloUtente,
                                            AuditLogRequest auditLogRequest);

	Integer countListaAnagrafica(List<Integer> filtroFonteId,
			List<String> descrizioniStato,
			List<String> sezione,
			Boolean insInSorveglianza,
			String tipoElencoInail,
			List<Integer> assistenzaAslId,  String codiceFiscale,
                                 String cognome,
                                 String nome,
                                 String cognomeLettDa,
                                 String cognomeLettA,
                                 LocalDate nascitaData,
                                 String profiloUtente);

	
	Integer inserisciSoggetto(DSLContext ctx, AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);

    Integer inserisciRegistro(DSLContext ctx, Integer soggettoId, AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);
    
    Integer updateRegistro(DSLContext ctx, Integer soggettoId, AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);
    
    Integer inserisciRegistroInail(DSLContext ctx, Integer soggettoId, AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);

    Integer inserisciEsposizioni(DSLContext ctx, Integer soggettoId, AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);
    
    Integer inserisciEsenzioni(DSLContext ctx, Integer soggettoId, AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);
    
    AnagraficaDTO findById(Long soggettoId);
    
    AnagraficaDTO findByCodiceFiscale(String codicefiscale);
    
    Integer findRegistroIdBySoggettoId(Integer soggettoId);
    
    AnagraficaDTO updateSoggetto(DSLContext ctx, Long soggettoId, AnagraficaDTO dto, Integer nuovaVersione, String cfOperatore);
    
    List<AnagraficaDTO> findByCForNomeOrCognomeOrDataNascita(String filtroCodiceFiscale, String filtroNome, String filtroCognome, LocalDate filtroNascitaData);

    void aggiornaStato(Long soggettoId, AnagraficaDTO dto, String cfOperatore);

    int insertStoricoSoggetto(DSLContext ctx, Long soggettoId, AnagraficaDTO dto, Integer nuovaVersione, String utenteCreazione, String utenteModifica);

    List<StoriaStatoDTO> findStoriaStati(Integer soggettoId);
    
    List<ExportDTO> recuperaTotaleRecordPerExportAsincrono(Integer assistenzaAslId, Integer soggettoId, boolean includiSenzaAdesione, String profiloUtente);

    List<ExportDTO> recuperaTotaleRecordPerExportAsincronoStatiPrecedenti(Integer assistenzaAslId, String profiloUtente);
    
    Map<String, List<String>> buildCampiMascheratiMap();
    
    Set<String> findCampiMascheratiCod();

    List<String[]> findAnagraficheConAura();
    
    Integer recuperaScarti(Integer fileId);
    
    void chiudiTuttiRecordTabAdesioneByFile(Integer fileId);

	Integer countListaAnagraficaFast(List<Integer> filtroFonteId, List<String> descrizioniStato, List<String> sezione,
			Boolean insInSorveglianza, String tipoElencoInail, List<Integer> assistenzaAslId, String codiceFiscale,
			String cognome, String nome, String cognomeLettDa, String cognomeLettA, LocalDate nascitaData, String profiloUtente);

	StoriaStatoDTO findStatoCorrente(Integer soggettoId);
	
}
