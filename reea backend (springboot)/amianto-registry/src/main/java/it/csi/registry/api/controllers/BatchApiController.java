package it.csi.registry.api.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.api.BatchApi;
import it.csi.registry.aura.services.AuraService;
import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.repositories.AnagraficaRepository;
import it.csi.registry.repositories.InailRepositoryImpl;
import it.csi.registry.services.AnagraficaService;
import it.csi.registry.services.ArchivioFileCaricatiService;
import it.csi.registry.services.NazioneService;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;


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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdesioneController.class);
    
    
    // injection diretta
    public BatchApiController(AnagraficaRepository anagraficaRepository,
                              AuraService auraService, AdesioneController adesioneController, AnagraficaService anagraficaService, NazioneService nazioneService, InailController inailController, NplaController nplaController, SpresalController spresalController, InailRepositoryImpl inailRepository,ArchivioFileCaricatiService archivioFileCaricatiService ) {
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
    }

    @Override
    public ResponseEntity<String> triggerBatchAnagrafiche() {

        // 1. ora hai 2 valori per riga (id soggetto e id aura decifrato)
        List<String[]> lista = anagraficaRepository.findAnagraficheConAura();

        // 2. loop
        for (String[] riga : lista) {

            String soggettoId = riga[0];
            String idAura = riga[1];

            try {
                var auraServicedto = auraService.getByIdAura(idAura);

                if (auraServicedto != null) {
                    System.out.println("OK idAura=" + idAura + " soggettoId=" + soggettoId);

                    // DTO da aggiornare
                    AnagraficaDTO anagraficadto = new AnagraficaDTO();
                    
                    // --- Valorizzo i singoli campi --- //

                    anagraficadto.setCodiceFiscale(auraServicedto.getCodiceFiscale());	// codice_fiscale
                    
                    // domicilio_asl_id : impostare reea_d_asl.asl_id con reea_d_asl.asl_cod = AslDomicilio di Aura
                    //ASL domicilio di aura= NON E' PRESENTE NEL DTO
                    anagraficadto.setDomicilioAslId(null); //...al posto del null devo recuperare il valore dell'id asl.. 
                    
                    // residenza_asl_id : impostare reea_d_asl.asl_id con reea_d_asl.asl_cod = AslResidenza di Aura
                    // assistenza_asl_id : impostare reea_d_asl.asl_id con reea_d_asl.asl_cod = AslAssistenza di Aura
                    
                    anagraficadto.setNome(auraServicedto.getNome());							// nome_cifrato
                    //System.out.println("nome=" + anagraficadto.getNome());
                    anagraficadto.setCognome(auraServicedto.getCognome());						// cognome_cifrato
                    anagraficadto.setSesso(auraServicedto.getSesso());							// sesso
                    anagraficadto.setNascitaData(auraServicedto.getDataNascita());				// nascita_data
                    //System.out.println("data di nascita="+anagraficadto.getNascitaData());
                    anagraficadto.setNascitaComuneCod(auraServicedto.getComuneNascitaCod());	// nascita_comune_cod
                    anagraficadto.setNascitaComuneDesc(auraServicedto.getComuneNascitaDesc());	// nascita_comune_desc
                    
                    // nascita_provincia_cod --- sigla della provincia
                    anagraficadto.setNascitaProvinciaCod(auraServicedto.getProvinciaNascitaCod());
                    
                    // nascita_provincia_desc ---- VERIFICARE SE OK
                    anagraficadto.setNascitaProvinciaDesc(auraServicedto.getProvinciaNascitaDesc());
                    
                    // nascita_stato_cod
                    anagraficadto.setNascitaStatoCod(auraServicedto.getStatoNascita());

                    // nascita_stato_desc --- denominazione stato di nascita: da recuperare, non c'Ã¨ su DTO aura
                    String descrizionestatonascita = nazioneService.getDescByIstatCod(auraServicedto.getStatoNascita());
                    anagraficadto.setNascitaStatoDesc(descrizionestatonascita);
                    
                    //cittadinanza_stato_cod --- codice stato cittadinanza : non trovo su service aura
                    //anagraficadto.setCittadinanzaStatoCod(...);
                    
                    //cittadinanza_stato_desc --- descrizione stato cittadinanza ; da costruire
                    //anagraficadto.setCittadinanzaStatoDesc(..);
                    
                    //domicilio_comune_cod
                    anagraficadto.setDomicilioComuneCod(auraServicedto.getComuneDomicilioCod());
                    
                    //domicilio_comune_desc
                    anagraficadto.setDomicilioComuneDesc(auraServicedto.getComuneDomicilioDesc());
                    

                    //domicilio_provincia_cod --- impostare la sigla o il codice della provincia (preso da reea_d_provincia)?

                    //domicilio_provincia_cod --- impostare il codice della provincia (preso da reea_d_provincia)
					//VERIFICARE  anagraficadto.setDomicilioProvinciaCod(auraServicedto.getProvinciaDomicilioCod());--> qui c'Ã¨ la sigla
					//VERIFICARE prendo il codice della provincia in base alla SIGLA della provincia
					System.out.println("------ aura ProvinciaDomicilioCod=" + auraServicedto.getProvinciaDomicilioCod());

//                    anagraficadto.setDomicilioProvinciaCod(provinciaRepository.findProvinciaCodByProvinciaDesc(auraServicedto.getProvinciaDomicilioCod()));

                    //anagraficadto.setDomicilioProvinciaCod(provinciaRepository.findProvinciaCodByProvinciaDesc(auraServicedto.getProvinciaDomicilioCod()));


                    
                    //domicilio_provincia_desc --- VERIFICARE
                    anagraficadto.setDomicilioProvinciaDesc(auraServicedto.getProvinciaDomicilioDesc());
                    System.out.println("------ aura ProvinciaDomicilioDesc=" + auraServicedto.getProvinciaDomicilioDesc());
                    
                    //domicilio_stato_cod --- VERIFICARE 
                    anagraficadto.setDomicilioStatoCod(auraServicedto.getStatoDomicilio());
                    
                    //domicilio_stato_desc--- VERIFICARE su dto aura non c'Ã¨ la descriz stato domicilio; da costruire
                    //anagraficadto.setDomicilioStatoDesc(...);
                    
                    anagraficadto.setDomicilioCap(auraServicedto.getCapDomicilio());				//domicilio_cap
                    anagraficadto.setDomicilioIndirizzo(auraServicedto.getIndirizzoDomicilio());	//domicilio_indirizzo_cifrato
                    anagraficadto.setResidenzaComuneCod(auraServicedto.getComuneResidenzaCod());	//residenza_comune_cod
                    anagraficadto.setResidenzaComuneDesc(auraServicedto.getComuneResidenzaDesc());	//residenza_comune_desc

                    
                    //residenza_provincia_cod --- impostare la sigla o il codice della provincia (preso da reea_d_provincia)?

                    //residenza_provincia_cod --- impostare il codice della provincia (preso da reea_d_provincia)
                    //VERIFICARE prendo il codice della provincia in base alla SIGLA della provincia
//                    anagraficadto.setResidenzaProvinciaCod(provinciaRepository.findProvinciaCodByProvinciaDesc(auraServicedto.getProvinciaResidenzaCod()));
                    //anagraficadto.setResidenzaProvinciaCod(provinciaRepository.findProvinciaCodByProvinciaDesc(auraServicedto.getProvinciaResidenzaCod()));

                    
                    //residenza_provincia_desc
                    anagraficadto.setResidenzaProvinciaDesc(auraServicedto.getProvinciaResidenzaDesc());
                    
                    //residenza_stato_cod --- VERIFICARE
                    anagraficadto.setResidenzaStatoCod(auraServicedto.getStatoResidenza());
                    
                    //residenza_stato_desc ---VERIFICARE su dto aura non c'Ã¨ la descriz stato residenza; da costruire
                    //anagraficadto.setResidenzaStatoDesc(---);
                    
                    //residenza_cap
                    anagraficadto.setResidenzaCap(auraServicedto.getCapResidenza());
                    
                    //email_cifrata --- DA COSTRUIRE
                    
                    //telefono_cifrato --- DA COSTRUIRE
                    
                    //tessera_team_cifrata  --- VERIFICARE: non trovo il campo su Aura
                    //anagraficadto.setTesseraTeam(auraServicedto.???);
                    
                    //id_aura_cifrato --- NON AGGIORNARE!!!
                    
                    //email_aura_cifrata	: da costruire
                    
                    //telefono_aura_cifrato : da costruire
                    
                    //data_aggiornamento_aura : IMPOSTARE LA DATA DI SISTEMA
                    
                    //data_decesso
                    //anagraficadto.setDataDecesso(auraServicedto.getDataDecesso());
                    
                    //assistenza_asl_fine --- VERIFICARE i due campi sono di tipi diversi
                    //anagraficadto.setAssistenzaAslFine(auraServicedto.getDataFineAsl());
                    
                    //inserimento_tipo_id
                    //validita_inizio
                    //validita_fine
                    //data_creazione
                    //data_modifica
                    //data_cancellazione
                    //utente_creazione
                    //utente_modifica
                    //utente_cancellazione
                    //fonte_id
                    //soggetto_stato_id
                    //versione_numero
                    
                    anagraficadto.setResidenzaIndirizzo(auraServicedto.getIndirizzoResidenza());	//residenza_indirizzo_cifrato
                    anagraficadto.setDomicilioNumeroCivico(auraServicedto.getCivicoDomicilio());	//domicilio_numero_civico
                    anagraficadto.setResidenzaNumeroCivico(auraServicedto.getCivicoResidenza());	//residenza_numero_civico
                    
                    //presentazione_istanza_data
                    //soggetto_stato_note

                    
                    // âš ï¸� QUI CAMBIA: ora puoi usare soggettoId!
                    
                    
                    //7 STORICIZZARE IL RECORD VECCHIO
                    anagraficaService.aggiornaSoggetto(
                        Long.valueOf(soggettoId),  // âœ… ora passi l'id corretto
                        anagraficadto,
                        idAura,
                        null
                    );
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