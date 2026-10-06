package it.csi.registry.api.controllers;

import java.util.List;

import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.api.BatchApi;
import it.csi.registry.aura.services.AuraService;
import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.RegistroDTO;
import it.csi.registry.repositories.AnagraficaRepository;
import it.csi.registry.repositories.InailRepositoryImpl;
import it.csi.registry.services.AnagraficaService;
import it.csi.registry.services.ArchivioFileCaricatiService;
import it.csi.registry.services.NazioneService;
import it.csi.registry.soap.AnagrafeSanitaria.SoggettoAuraMsg;
import it.csi.registry.soap.anagrafe.clients.AnagrafeGetClient;
import it.csi.registry.util.RegistroUtils;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;
import it.csi.registry.services.AnagraficaServiceImpl;  //MMAA
import it.csi.registry.repositories.AslRepository;      //MMAA
import com.fasterxml.jackson.databind.ObjectMapper;		//MMAA


@RestController
public class BatchApiController implements BatchApi {

    private final AdesioneController adesioneController;
    private final InailController inailController;
    private final NplaController nplaController;
    private final SpresalController spresalController;
	private final AnagraficaRepository anagraficaRepository;
    private final AuraService auraService;
    private final AnagraficaService anagraficaService;
    private final NazioneService nazioneService;
    private final InailRepositoryImpl inailRepository;
    private final ArchivioFileCaricatiService archivioFileCaricatiService;
    private final AnagrafeGetClient anagrafeGetClient;          //MMAA
    private final AnagraficaServiceImpl anagraficaServiceImpl;  //MMAA
    private final AslRepository aslRepository;					//MMAA
    private final DSLContext dsl;								//MMAA
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdesioneController.class);
    
    
    // injection diretta
    public BatchApiController(AnagraficaRepository anagraficaRepository,
                              AuraService auraService, AdesioneController adesioneController, AnagraficaService anagraficaService, NazioneService nazioneService, InailController inailController, NplaController nplaController, SpresalController spresalController, InailRepositoryImpl inailRepository, ArchivioFileCaricatiService archivioFileCaricatiService, 
                              AnagraficaServiceImpl anagraficaServiceImpl, 
                              AnagrafeGetClient anagrafeGetClient,
                              AslRepository aslRepository,
                              DSLContext dsl) {
        this.inailController = inailController;
		this.nplaController = nplaController;
		this.spresalController = spresalController;
		this.anagraficaRepository = anagraficaRepository;
        this.auraService = auraService;
		this.adesioneController = adesioneController;
		this.anagraficaService = anagraficaService;
		this.nazioneService = nazioneService;
		this.inailRepository = inailRepository;
		this.archivioFileCaricatiService = archivioFileCaricatiService;
		this.anagrafeGetClient = anagrafeGetClient;           //MMAA
		this.anagraficaServiceImpl = anagraficaServiceImpl;   //MMAA
		this.aslRepository = aslRepository;                   //MMAA
		this.dsl = dsl;										  //MMAA
    }

    @Override
    public ResponseEntity<String> triggerBatchAnagrafiche() {

        // 1. ora hai 2 valori per riga (id soggetto e id aura decifrato)
        //List<String[]> lista = anagraficaRepository.findAnagraficheConAura(); --- disabilito per prove

        // FORZATURA PER PROVE
        // FORZATURA PER PROVE
        // FORZATURA PER PROVE
    	System.out.println("##### INIZIO triggerBatchAnagrafiche ---");
        List<String[]> lista = List.of(
        	    //new String[] { "764631", "255802" },
        	    //new String[] { "764681", "1667309" }
        	    new String[] { "782326", "1617133" },
        	    new String[] { "776470", "2790476" }
        	);
        
        System.out.println("##### dopo forzature");
    	
        // 2. loop
        for (String[] riga : lista) {

            String soggettoId = riga[0];
            String idAura = riga[1];

            try {
            	
                System.out.println("##### Elabora idAura=" + idAura + " soggettoId=" + soggettoId);

           	    // 1 - Per il soggettoId in elaborazione estraggo i dati attuali da REEA_T_SOGGETTO.
           	    Long id = Long.valueOf(soggettoId);
           	    AnagraficaDTO dtoSoggetto = anagraficaRepository.findById(id);

           	    System.out.println("##### Estratto dtoSoggetto");
           	    
           	    // 2 - I dati attuali li salvo anche in un altro dto per averne una copia "congelata"
           	    AnagraficaDTO dtoSoggettoCopia = anagraficaRepository.findById(id);   //prova1=rieseguo il metodo
           	    //Duplico il valore di dtoSoggetto nella variabile dtoSoggettoCopia     //prova2=faccio una deep copy
           	    //ObjectMapper mapper = new ObjectMapper();
           	    //AnagraficaDTO dtoSoggettoCopia =
           	    //    mapper.readValue(mapper.writeValueAsString(dtoSoggetto), AnagraficaDTO.class);

           	    System.out.println("##### Creato dtoSoggettoCopia");
           	    
           	    // 3 - Estraggo i dati da Aura
           	    SoggettoAuraMsg sAuraMsg = null;
           	    sAuraMsg = anagrafeGetClient.get(idAura);

           	    System.out.println("##### Estratto dati da Aura sAuraMsg");
           	    
           	    if (sAuraMsg != null) {

           	    	System.out.println("##### ----Aura valorizzato---");
           	    	
            	    // 4 - Popola i dati del DTO della REEA_T_SOGGETTO con i dati di AURA
                	AnagraficaDTO dtoSoggettoConModificheAura = null;
                	dtoSoggettoConModificheAura = anagraficaServiceImpl.popolaAnagraficaDTObyAura(dtoSoggetto, sAuraMsg);
                    
                	System.out.println("##### Finito popolaAnagraficaDTObyAura");
                	
                	// 4a - Valorizza altre colonne del DTO della REEA_T_SOGGETTO
                	anagraficaServiceImpl.recuperaInfoProvinciaComune(dtoSoggettoConModificheAura);

                	System.out.println("##### Finito recuperaInfoProvinciaComune");
                	
                    // 4b - Domicilio ASL
                    if (dtoSoggettoConModificheAura.getDomicilioAslId() == null) {
                        String domicilioAslCod = dtoSoggettoConModificheAura.getDomicilioAslCod();

                        if (domicilioAslCod != null && !domicilioAslCod.isBlank()) {
                            String domicilioAslCodFormattato = String.format(
                                    "%6s",
                                    domicilioAslCod.trim()
                            ).replace(' ', '0');

                            dtoSoggettoConModificheAura.setDomicilioAslId(
                            		aslRepository.getAslIdByAslCod(domicilioAslCodFormattato)
                            );
                        }
                    }

                    System.out.println("##### Finito 4b - Domicilio ASL");
                    
                    // 4c - Fallback ASL per deceduti/emigrati senza ASL da AURA
                    if (dtoSoggettoConModificheAura.getDomicilioAslId() == null) {
                        Integer fallbackId = aslRepository.getAslIdByAslCod("999999");
                        dtoSoggettoConModificheAura.setDomicilioAslId(fallbackId);
                    }
                    //if (dtoSoggettoConModificheAura.getDomicilioAslId() == null) {
                    //    throw new IllegalStateException("Il domicilio_asl_id non puo essere null");
                    //}

                    System.out.println("##### Finito 4c");
                    
                    // 4d - Residenza ASL
                    String residenzaAslCod = dtoSoggettoConModificheAura.getResidenzaAslCod();
                    if (residenzaAslCod != null && !residenzaAslCod.isBlank()) {
                        String residenzaAslCodFormattato = String.format(
                                "%6s",
                                residenzaAslCod.trim()
                        ).replace(' ', '0');

                        dtoSoggettoConModificheAura.setResidenzaAslId(
                                aslRepository.getAslIdByAslCod(residenzaAslCodFormattato)
                        );
                    }

                    System.out.println("##### Finito 4d");
                    
                    if(dtoSoggettoConModificheAura.getResidenzaAslId() == null) {
                    	Integer fallbackId = aslRepository.getAslIdByAslCod("999999");
                    	dtoSoggettoConModificheAura.setResidenzaAslId(fallbackId);
                        }

                    //if (anagraficaDTOIntegrazioneAura == null) {
                    //	dtoSoggettoConModificheAura.setAssistenzaAslId(
                    //			dtoSoggettoConModificheAura.getDomicilioAslId().toString());
                    //} else {

                    // 4e - AssistenzaAslId
                    String assistenzaCod = dtoSoggettoConModificheAura.getAssistenzaAslId();
                    Integer assistenzaId = (assistenzaCod != null && !assistenzaCod.isBlank())
                                           ? aslRepository.getAslIdByAslCod(assistenzaCod)
                                           : null;
                    dtoSoggettoConModificheAura.setAssistenzaAslId(assistenzaId != null
                                            			  ? String.format("%06d", assistenzaId)
                                            		      : dtoSoggettoConModificheAura.getDomicilioAslId().toString());
                        //}

                    System.out.println("##### Finito 4e");
                    
                    if(dtoSoggettoConModificheAura.getAssistenzaAslId() == null) {
                      	Integer fallbackId = aslRepository.getAslIdByAslCod("999999");
                       	dtoSoggettoConModificheAura.setAssistenzaAslId(fallbackId.toString());
                    }
                    
                    // 5 - Controllo se dopo avere aggiornato con i dati AURA ci sono della variazioni sui dati
                    //String jsonCopia = mapper.writeValueAsString(dtoSoggettoCopia);                  // dati originali
                    //String jsonModificato = mapper.writeValueAsString(dtoSoggettoConModificheAura);  // dati modificati con Aura
                    //boolean sonoDiversi = !jsonCopia.equals(jsonModificato);
                    // PROVA CONFRONTO CAMPO PER CAMPO
                    
                    System.out.println("##### CONFONTI");
                    System.out.println("##### Nome originale    = " + dtoSoggettoCopia.getNome() + " - Nome con modif = " + dtoSoggettoConModificheAura.getNome());
                    System.out.println("##### Cognome originale = " + dtoSoggettoCopia.getCognome() + " - Cognome con modif = " + dtoSoggettoConModificheAura.getCognome());
                    System.out.println("##### DomicilioAslId originale = " + dtoSoggettoCopia.getDomicilioAslId() + " - DomicilioAslId con modif = " + dtoSoggettoConModificheAura.getDomicilioAslId());
                    System.out.println("##### ResidenzaComuneDesc originale = " + dtoSoggettoCopia.getResidenzaComuneDesc() + " - ResidenzaComuneDesc con modif = " + dtoSoggettoConModificheAura.getResidenzaComuneDesc());

                    
                    boolean ciSonoDifferenze =
                    /*		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioAslId(), dtoSoggettoConModificheAura.getDomicilioAslId())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaAslId(), dtoSoggettoConModificheAura.getResidenzaAslId())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getAssistenzaAslId(), dtoSoggettoConModificheAura.getAssistenzaAslId())||*/
                    		!java.util.Objects.equals(dtoSoggettoCopia.getNome(), dtoSoggettoConModificheAura.getNome())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getCognome(), dtoSoggettoConModificheAura.getCognome()); //||
                    /*		!java.util.Objects.equals(dtoSoggettoCopia.getSesso(), dtoSoggettoConModificheAura.getSesso())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getNascitaData(), dtoSoggettoConModificheAura.getNascitaData())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getNascitaComuneCod(), dtoSoggettoConModificheAura.getNascitaComuneCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getNascitaComuneDesc(), dtoSoggettoConModificheAura.getNascitaComuneDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getNascitaProvinciaCod(), dtoSoggettoConModificheAura.getNascitaProvinciaCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getNascitaProvinciaDesc(), dtoSoggettoConModificheAura.getNascitaProvinciaDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getNascitaStatoCod(), dtoSoggettoConModificheAura.getNascitaStatoCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getNascitaStatoDesc(), dtoSoggettoConModificheAura.getNascitaStatoDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getCittadinanzaStatoCod(), dtoSoggettoConModificheAura.getCittadinanzaStatoCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getCittadinanzaStatoDesc(), dtoSoggettoConModificheAura.getCittadinanzaStatoDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioComuneCod(), dtoSoggettoConModificheAura.getDomicilioComuneCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioComuneDesc(), dtoSoggettoConModificheAura.getDomicilioComuneDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioProvinciaCod(), dtoSoggettoConModificheAura.getDomicilioProvinciaCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioProvinciaDesc(), dtoSoggettoConModificheAura.getDomicilioProvinciaDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioStatoCod(), dtoSoggettoConModificheAura.getDomicilioStatoCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioStatoDesc(), dtoSoggettoConModificheAura.getDomicilioStatoDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioCap(), dtoSoggettoConModificheAura.getDomicilioCap())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioIndirizzo(), dtoSoggettoConModificheAura.getDomicilioIndirizzo())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaComuneCod(), dtoSoggettoConModificheAura.getResidenzaComuneCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaComuneDesc(), dtoSoggettoConModificheAura.getResidenzaComuneDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaProvinciaCod(), dtoSoggettoConModificheAura.getResidenzaProvinciaCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaProvinciaDesc(), dtoSoggettoConModificheAura.getResidenzaProvinciaDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaStatoCod(), dtoSoggettoConModificheAura.getResidenzaStatoCod())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaStatoDesc(), dtoSoggettoConModificheAura.getResidenzaStatoDesc())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaCap(), dtoSoggettoConModificheAura.getResidenzaCap())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getEmail(), dtoSoggettoConModificheAura.getEmail())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getTelefono(), dtoSoggettoConModificheAura.getTelefono())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getTesseraTeam(), dtoSoggettoConModificheAura.getTesseraTeam())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getEmailAura(), dtoSoggettoConModificheAura.getEmailAura())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getTelefonoAura(), dtoSoggettoConModificheAura.getTelefonoAura())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDataDecesso(), dtoSoggettoConModificheAura.getDataDecesso())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getAssistenzaAslFine(), dtoSoggettoConModificheAura.getAssistenzaAslFine())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getSoggettoStatoId(), dtoSoggettoConModificheAura.getSoggettoStatoId())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaIndirizzo(), dtoSoggettoConModificheAura.getResidenzaIndirizzo())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getDomicilioNumeroCivico(), dtoSoggettoConModificheAura.getDomicilioNumeroCivico())||
                    		!java.util.Objects.equals(dtoSoggettoCopia.getResidenzaNumeroCivico(), dtoSoggettoConModificheAura.getResidenzaNumeroCivico());
						*/

                    System.out.println("##### Finito confronto campo per campo");
                    
                    //if (sonoDiversi) {
                    if (ciSonoDifferenze) {
                        // ci sono modifiche → storicizza + aggiorna DB
                        System.out.println("##### SI FATTE VARIAZIONI");
                        
                        // Imposto il nuovo valore della versione
                    	RegistroDTO registroCurrent = RegistroUtils.ensureRegistro(dtoSoggetto);
                    	Integer nuovaVersione = (registroCurrent.getVersioneNumero() != null ? registroCurrent.getVersioneNumero() : 1) + 1;

                    	System.out.println("##### Finito nuovo valore della versione");
                    	
                        // 6 - Storicizzo il record originale su REEA_S_SOGGETTO (...dentro fa nuovaversione-1)
                    	DSLContext ctx = dsl;
                    	anagraficaRepository.insertStoricoSoggetto(ctx, id, dtoSoggettoCopia, nuovaVersione, dtoSoggettoCopia.getUtenteCreazione(), dtoSoggettoCopia.getUtenteModifica());

                    	System.out.println("##### Finito insertStoricoSoggetto");
                    	
                    	// 7 - Aggiorno REEA_T_SOGGETTO con i dati aggiornati da Aura
                    	anagraficaRepository.updateSoggetto(ctx, id, dtoSoggettoConModificheAura, nuovaVersione, dtoSoggettoConModificheAura.getUtenteModifica());

                    	System.out.println("##### Finito updateSoggetto ----------------");
                    	
                    } else {
                        // nessuna modifica → non fare nulla
                    }

                	//MMAA ---  fine  ------------------------------------------------------------------
                                        
                    // STORICIZZARE IL RECORD VECCHIO
                    //anagraficaService.aggiornaSoggetto(
                    //    Long.valueOf(soggettoId),
                    //    dtoSoggettoConModificheAura,
                    //    idAura,
                    //    null
                    //);
                    // MANCANO I DATI DELLE ESENZIONI
                } else {
                    System.out.println("NOT FOUND idAura=" + idAura);
                }

            } catch (Exception e) {
                System.err.println("ERRORE idAura=" + idAura);
//                e.printStackTrace();
                LOGGER.error("Errore nel triggerBatchAnagrafiche", null, e);
            }
        }

        return ResponseEntity.ok("Batch completato");
    }

	@Override
	public ResponseEntity<String> triggerBatchTracciati() {

	    new Thread(() -> {
	    	
	        // Oggetto fittizio (da rimuovere in futuro)
	        FileSalvato fileSalvato = new FileSalvato("A", "A", "A");
	    	
	        // PREADESIONI (controllo fatto una sola volta)
	        boolean adesioniInCorso = archivioFileCaricatiService.hasElaborazioneTerminata("preadesione");
	        if (adesioniInCorso) 
	        {
	        	System.out.println("NON POSSO ESEGUIRE ELEBORAZIONI PREADESIONI");
	        }	
	        else
	        {
	        	System.out.println("POSSO ESEGUIRE ELEBORAZIONI PREADESIONI");
	        	this.adesioneController.ciclaListaAdesioni();
	        }
	        


	        // INAIL (controllo fatto una sola volta)
	        boolean inailInCorso = archivioFileCaricatiService.hasElaborazioneTerminata("inail");
	        if (inailInCorso) {
	        	System.out.println("NON POSSO ESEGUIRE ELEBORAZIONI INAIL");
	        }
	        else {
	        	System.out.println("POSSO ESEGUIRE ELEBORAZIONI INAIL");
	            this.inailController.ciclaListaInail("A", fileSalvato);
	            this.inailController.ciclaListaInail("B", fileSalvato);
	        }
	        
	        // NPLA
	        boolean nplaInCorso = archivioFileCaricatiService.hasElaborazioneTerminata("npla");
	        if (nplaInCorso) {
	        	System.out.println("NON POSSO ESEGUIRE ELEBORAZIONI NPLA");
	        } 
	        else {	
	        	System.out.println("POSSO ESEGUIRE ELEBORAZIONI NPLA");
	            this.nplaController.inserimentiMassiviNpla(fileSalvato);
	        }

	        // SPRESAL (controllo fatto una sola volta)
	        boolean spresalInCorso = archivioFileCaricatiService.hasElaborazioneTerminata("spresal");
	        if (spresalInCorso) {
	        	System.out.println("NON POSSO ESEGUIRE ELEBORAZIONI SPRESAL");
	        }
	        else {
	        	System.out.println("POSSO ESEGUIRE ELEBORAZIONI SPRESAL");
	            this.spresalController.ciclaListaSpresalAnamnesi(fileSalvato);
	            this.spresalController.ciclaListaSpresalEsiti(fileSalvato);
	        }

	    }).start();

	    return ResponseEntity.ok("Operazione batch import massivo avviata!");
	}
	
}