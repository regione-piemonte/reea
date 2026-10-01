package it.csi.registry.api.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.InailDTO;
import it.csi.registry.model.RegistroDTO;
import it.csi.registry.services.AnagraficaService;
import it.csi.registry.services.InailService;
import it.csi.registry.services.TracciaElaborazioneService;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;
import it.csi.registry.util.ErroreImportUtility;
import it.csi.registry.util.RegistroUtils;

import static java.lang.System.out;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/inail")
public class InailController {
	
	private final InailService inailService;
	private final AnagraficaService anagraficaService;
	private final TracciaElaborazioneService tracciaElaborazioneService;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(InailController.class);
	    

    public InailController(InailService inailService, AnagraficaService anagraficaService, TracciaElaborazioneService tracciaElaborazioneService) {
    	this.inailService = inailService;
        this.anagraficaService = anagraficaService;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
    }
	
    
    @PostMapping(value = "/import/inail/{tipologiaInail}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importExcelInail(
            @RequestPart("file") MultipartFile file,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest,
            @PathVariable("tipologiaInail") String tipologiaInail) {
        try {
            inailService.importInail(file, tipologiaInail, auditLogRequest);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            System.out.println(".error(Errore durante l'importazione del file Excel INAIL, e" + e);
            return ResponseEntity.badRequest().build();
        }
    }
//	@PostMapping("/import/inail/{tipologiaInail}")
//    public ResponseEntity<Void> importExcelInailA(@RequestParam("file") MultipartFile file, @PathVariable("tipologiaInail") String tipologiaInail) {
//        try {
//        	inailService.importInail(file, tipologiaInail);
//        	
//            return ResponseEntity.ok().build();
//        } catch (IOException e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }


	@GetMapping("/getRecordTabInail")
	public List<InailDTO> getRecordTabInail(
			@RequestParam(value = "tipologiaInail", required = false) String tipologiaInail,
			@RequestParam(name = "file_id", required = false) Integer fileId) {
		List<InailDTO> lista = null;
		try {
			lista = inailService.getRecordTabInail(tipologiaInail, fileId);
		} catch (IOException e) {
//			e.printStackTrace();
			LOGGER.error("Errore nel recupero dei record tab inail, fileId={}", fileId, e);
		}
		return lista;
	}



	@GetMapping("/inserimentiMassiviFileInail/{tipologiaInail}")
    public void ciclaListaInail(@PathVariable("tipologiaInail") String tipologiaInail, FileSalvato fileSalvato) {
		Integer recordInseriti = 0;
    	Integer recordErrore = 0;
    	Integer recordScartati = 0;
    	Integer fileId = null;
    	Integer elaborazioneId = null;
    	String utenteCreazione  = "ADMIN";
        
    	List<InailDTO> inailDTOs = getRecordTabInail(tipologiaInail, fileId);
    	
    	if (inailDTOs == null || inailDTOs.isEmpty()) {
            System.out.println("/inserimentiMassiviFileInail --> lista vuota");
            return;
        }
    	
    	utenteCreazione = inailDTOs.get(0).getUtenteCreazione();
    	
    	elaborazioneId = tracciaElaborazioneService.inserisciStatoElaborazione(inailDTOs.get(0).getFileId(), utenteCreazione, 2);
    	fileId = inailDTOs.get(0).getFileId();
        
        List<AnagraficaDTO> result = inailDTOs.stream()
        	    .map(dto -> {
        	        AnagraficaDTO out = new AnagraficaDTO();
        	        out.setRegInailId(dto.getRegInailId().intValue());
        	        out.setDomanda(dto.getDomanda().toString());
        	        out.setCognome(dto.getCognome());
        	        out.setNome(dto.getNome());
        	        out.setCodiceFiscale(dto.getCodiceFiscale());
        	        out.setSesso(dto.getSesso());
        	        out.setNascitaData(dto.getDataNascita());
        	        out.setResidenzaIndirizzo(dto.getIndirizzoResidenza());
        	        out.setResidenzaCap(dto.getCapResidenza() != null ? dto.getCapResidenza().toString() : null);
        	        out.setResidenzaIstat(dto.getIstatResidenza() != null ? dto.getIstatResidenza().toString() : null);
        	        out.setResidenzaComuneDesc(dto.getComuneResidenza());
        	        out.setResidenzaProvinciaDesc(dto.getProvinciaResidenza());
        	        out.setResidenzaRegione(dto.getRegioneResidenza());
        	        out.setUtenteCreazione(dto.getUtenteCreazione());

        	        RegistroDTO registro = RegistroUtils.ensureRegistro(out);
        	        registro.setValiditaInizio(dto.getValiditaInizio());
        	        registro.setValiditaFine(dto.getValiditaFine());
        	        registro.setDataCreazione(dto.getDataCreazione());
        	        registro.setDataModifica(dto.getDataModifica());
        	        registro.setDataCancellazione(dto.getDataCancellazione());
        	        registro.setUtenteCreazione(dto.getUtenteCreazione());
        	        registro.setUtenteModifica(dto.getUtenteModifica());
        	        registro.setUtenteCancellazione(dto.getUtenteCancellazione());

        	        out.setTipologiaInail(tipologiaInail);
        	        return out;
        	    })
        	    .collect(Collectors.toList());
    	
    	try {
    	    for (AnagraficaDTO dto : result) {
    	        try {
    	            Integer risultato = null;

    	            if ("A".equals(dto.getTipologiaInail())) {
    	                risultato = anagraficaService.creazioneAnagraficaInailA(dto, elaborazioneId);
    	            } else {
    	                risultato = anagraficaService.creazioneAnagraficaInailB(dto, elaborazioneId);
    	            }

    	            if (risultato != null && risultato > 0) {
    	                recordInseriti++;
    	                System.out.println("recordInseriti: " + recordInseriti);
    	            } 
    	            else if(risultato != null && risultato == 0){
                    	//devo notificare il mancato inserimento
    	            	recordErrore++;
    	            	tracciaElaborazioneService.inserisciErroreRiga(
								elaborazioneId, "Record duplicato", "Assistito duplicato nel file INAIL", null,
								utenteCreazione, dto.getCognome(), dto.getNome(),
								dto.getNascitaData(), dto.getSesso(), dto.getCodiceFiscale(),
								"REEA_T_REGISTRO_INAIL",                     // ← era "REEA_T_SOGGETTO"
								dto.getRegInailId() != null ? dto.getRegInailId() : 0
						);
    	            }

    	            out.println("/inserimentiMassiviFileInail --> creazioneAnagraficaInail : risultato " + risultato);

    	        } catch (Exception e) {
    	            recordErrore++;
    	            System.out.println("recordErrore: " + recordErrore);

    	            System.err.println("Errore in creazioneAnagraficaInail per CF " + dto.getCodiceFiscale()
    	                    + " : " + e.getMessage());
//    	            e.printStackTrace();
    	            LOGGER.error("Errore in creazioneAnagraficaInail per CF:", dto.getCodiceFiscale(), e);
					try {
						String descrizioneErrore = ErroreImportUtility.buildErroreDescrizione(e);
						tracciaElaborazioneService.inserisciErroreRiga(
								elaborazioneId, "ERR_ELABORAZIONE", e.getMessage()+ " :" + descrizioneErrore, null,
								utenteCreazione, dto.getCognome(), dto.getNome(),
								dto.getNascitaData(), dto.getSesso(), dto.getCodiceFiscale(),
								"REEA_T_REGISTRO_INAIL",                     // ← era "REEA_T_SOGGETTO"
								dto.getRegInailId() != null ? dto.getRegInailId() : 0
						);
					} catch (Exception ex) { ex.printStackTrace(); }

				}
    	    }
    	} finally {
    	    if (elaborazioneId != null) {
    	    	recordScartati = anagraficaService.recuperaScarti(inailDTOs.get(0).getFileId());
    	    	if(fileId != null)
    	    		tracciaElaborazioneService.aggiornaFile(fileId, 3, utenteCreazione);
    	        tracciaElaborazioneService.aggiornaFineOk(
    	            elaborazioneId,
    	            3,
    	            result.size() + recordScartati,
    	            result.size(),
    	            recordScartati,
    	            recordInseriti,
    	            recordErrore,
    	            "Elaborazione completata",
    	            utenteCreazione
    	        );
    	        inailService.chiudiTuttiRecordTabRegistroInailByFile(fileId);
    	    }
    	}
    }

}
