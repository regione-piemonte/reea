package it.csi.registry.api.controllers;

import org.springframework.http.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.csi.registry.model.*;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import it.csi.registry.services.AnagraficaService;
import it.csi.registry.services.SpresalService;
import it.csi.registry.services.TracciaElaborazioneService;
import it.csi.registry.util.ErroreImportUtility;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;

import static it.csi.registry.jooq.tables.ReeaDRegistroSpresalAnamnesiSettore.REEA_D_REGISTRO_SPRESAL_ANAMNESI_SETTORE;
import static it.csi.registry.jooq.tables.ReeaDRegistroSpresalAnamnesiMansione.REEA_D_REGISTRO_SPRESAL_ANAMNESI_MANSIONE;
import static it.csi.registry.jooq.tables.ReeaDRegistroSpresalAnamnesiRagionesociale.REEA_D_REGISTRO_SPRESAL_ANAMNESI_RAGIONESOCIALE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@RestController
@RequestMapping("/api/spresal")
public class SpresalController {
	
	private final SpresalService spresalService;
	private final AnagraficaService anagraficaService;
	private final TracciaElaborazioneService tracciaElaborazioneService;
	private final DSLContext dsl;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SpresalController.class);


    public SpresalController(DSLContext dsl, SpresalService spresalService, AnagraficaService anagraficaService, TracciaElaborazioneService tracciaElaborazioneService) {
		this.dsl = dsl;
		this.spresalService = spresalService;
        this.anagraficaService = anagraficaService;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
    }
    
    
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importExcelSpresal(
            @RequestPart("file") MultipartFile file, 
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {  // ✅ Entrambi @RequestPart
        try {
            spresalService.importSpresal(file, auditLogRequest);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
        	System.out.println(".error(Errore durante l'importazione del file Excel, e" + e);
            return ResponseEntity.badRequest().build();
        }
    }
//    @PostMapping("/import")
//    public ResponseEntity<Void> importExcelSpresal(@RequestParam("file") MultipartFile file, @RequestBody GetListaAnagraficheRequest request) {
//        try {
//        	spresalService.importSpresal(file,
//                    request.getAuditLogRequest());
//            
//            return ResponseEntity.ok().build();
//        } catch (IOException e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
    
    @PostMapping(value = "/import/esiti", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importExcelSpresalEsiti(
            @RequestPart("file") MultipartFile file,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
        try {
            spresalService.importSpresalEsiti(file, auditLogRequest);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            System.out.println(".error(Errore durante l'importazione del file Excel esiti, e" + e);
            return ResponseEntity.badRequest().build();
        }
    }
//    @PostMapping("/import/esiti")
//    public ResponseEntity<Void> importExcelSpresalEsiti(@RequestParam("file") MultipartFile file) {
//        try {
//        	spresalService.importSpresalEsiti(file);
//            
//            return ResponseEntity.ok().build();
//        } catch (IOException e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }


	@GetMapping("/getRecordTabSpresal")
	public List<SpresalDTO> getRecordTabSpresalAnamnesi(
			@RequestParam(name = "file_id", required = false) Integer fileId) {
		List<SpresalDTO> lista = null;
		try {
			lista = spresalService.getRecordTabSpresalAnamnesi(fileId);
		} catch (IOException e) {
//			e.printStackTrace();
			LOGGER.error("Errore nel recupero dei record tab Spresal, fileId={}", fileId, e);
		}
		return lista;
	}



	@GetMapping("/getRecordTabSpresalEsiti")
	public List<SpresalEsitiDTO> getRecordTabSpresalEsiti(
			@RequestParam(name = "file_id", required = false) Integer fileId) {
		List<SpresalEsitiDTO> lista = null;
		try {
			lista = spresalService.getRecordTabSpresalEsiti(fileId);
		} catch (IOException e) {
//			e.printStackTrace();
			LOGGER.error("Errore nel recupero dei record tab Esiti, fileId={}", fileId, e);
		}
		return lista;
	}



	@GetMapping("/inserimentiMassiviFileSpresalAnamnesi")
    public void ciclaListaSpresalAnamnesi(FileSalvato fileSalvato) {
		Integer recordInseriti = 0;
    	Integer recordErrore = 0;
    	Integer recordScartati = 0;
    	Integer fileId = null;
    	Integer elaborazioneId = null;
    	String utenteCreazione  = "ADMIN";
    	
        List<SpresalDTO> spresalDTOs = getRecordTabSpresalAnamnesi(fileId);
        
        if (spresalDTOs == null || spresalDTOs.isEmpty()) {
            System.out.println("/inserimentiMassiviFileSpresalAnamnesi --> lista vuota");
            return;
        }
        
        utenteCreazione = spresalDTOs.get(0).getUtenteCreazione();
        
    	elaborazioneId = tracciaElaborazioneService.inserisciStatoElaborazione(spresalDTOs.get(0).getFileId(), utenteCreazione, 2); 
    	fileId = spresalDTOs.get(0).getFileId();
        
        List<AnagraficaDTO> result = spresalDTOs.stream()
        	    .map(dto -> {
        	    	AnagraficaDTO out = new AnagraficaDTO();
        	    	SpresalDTO spresalDTO = new SpresalDTO();
        	    	spresalDTO.setRegSpresalAnamnesiId(dto.getRegSpresalAnamnesiId());
        	        out.setCodiceFiscale(dto.getCodiceFiscale());
					out.setCognome(dto.getCognome());
					out.setNome(dto.getNome());
					out.setNascitaData(dto.getDataNascita());
					out.setSesso(dto.getSesso());
					out.setUtenteCreazione(dto.getUtenteCreazione());
					spresalDTO.setDataIntervista(dto.getDataIntervista());
        	        spresalDTO.setNominativoIntervistatore(dto.getNominativoIntervistatore());
        	        spresalDTO.setFumatore(dto.getFumatore());
        	        spresalDTO.setSigarette(dto.getSigarette());
        	        spresalDTO.setSigaretteAnni(dto.getSigaretteAnni());
        	        spresalDTO.setSigaretteEtaInizio(dto.getSigaretteEtaInizio());
        	        spresalDTO.setSigaretteFumaAttualmente(dto.getSigaretteFumaAttualmente());
        	        spresalDTO.setSigaretteEtaFine(dto.getSigaretteEtaFine());
        	        spresalDTO.setSigaretteDie(dto.getSigaretteDie());
        	        spresalDTO.sigari(dto.getSigari());
        	        spresalDTO.sigariAnni(dto.getSigariAnni());
        	        spresalDTO.setSigariEtaInizio(dto.getSigariEtaInizio());
        	        spresalDTO.setSigariFumaAttualmente(dto.getSigariFumaAttualmente());
        	        spresalDTO.setSigariEtaFine(dto.getSigariEtaFine());
        	        spresalDTO.setSigariDie(dto.getSigariDie());
        	        spresalDTO.setPipa(dto.getPipa());
        	        spresalDTO.setPipaAnni(dto.getPipaAnni());
        	        spresalDTO.setPipaEtaInizio(dto.getPipaEtaInizio());
        	        spresalDTO.setPipaFumaAttualmente(dto.getPipaFumaAttualmente());
        	        spresalDTO.setPipaEtaFine(dto.getPipaEtaFine());
        	        spresalDTO.setPipaDie(dto.getPipaDie());
        	        spresalDTO.setOccupazioneNum(dto.getOccupazioneNum());
        	        spresalDTO.setOccupazioneAnnoInizio(dto.getOccupazioneAnnoInizio());
        	        spresalDTO.setOccupazioneAnnoFine(dto.getOccupazioneAnnoFine());
        	        spresalDTO.setOccupazioneTipo(dto.getOccupazioneTipo());
        	        spresalDTO.setOccupazioneDescrizioneLavoro(dto.getOccupazioneDescrizioneLavoro());
        	        spresalDTO.setOccupazioneNomeEIndirizzoDitta(dto.getOccupazioneNomeEIndirizzoDitta());
        	        spresalDTO.setOccupazioneAttivitaDitta(dto.getOccupazioneAttivitaDitta());
        	        spresalDTO.setNotaAttivitaConAmianto(dto.getNotaAttivitaConAmianto());
        	        spresalDTO.setAnamnesiEsposizioneAmianto(dto.getAnamnesiEsposizioneAmianto());
        	        spresalDTO.setEsposizioneProfessionale(dto.getEsposizioneProfessionale());
        	        spresalDTO.setAnnoFineEsposizione(dto.getAnnoFineEsposizione());
        	        spresalDTO.setLivelloEsposizione(dto.getLivelloEsposizione());    
        	        spresalDTO.setInserimentoInSorveglianza(dto.getInserimentoInSorveglianza());
        	        spresalDTO.setIdSpresal(dto.getIdSpresal());
        	        out.setSpresal(spresalDTO);

        	    	
        	        return out;
        	    })
        	    .collect(Collectors.toList());
    	
    	try {
    	    for (AnagraficaDTO dto : result) {
    	        try {
    	            Integer risultato = anagraficaService.creazioneAnagraficaSpresalAnamnesi(dto, elaborazioneId);

    	            if (risultato != null && risultato > 0) {
    	                recordInseriti++;
    	                System.out.println("recordInseriti: " + recordInseriti);
    	            }
    	            else {
    	            	recordErrore++;
                    	tracciaElaborazioneService.inserisciErroreRiga(
            					elaborazioneId,
            					"Errore Logico",
            					"Non e' stato possibile associare un Registro",
            					null,
            					dto.getUtenteCreazione(),
            					dto.getCognome(),
            					dto.getNome(),
            					dto.getNascitaData(),
            					dto.getSesso(),
            					dto.getCodiceFiscale(),
            					"REEA_T_REGISTRO",
            					dto.getRegInailId() != null ? dto.getRegInailId().intValue() : dto.getSpresal().getRegSpresalAnamnesiId().intValue()
            					);
    	            }

    	            System.out.println("/inserimentiMassiviFileSpresalAnamnesi --> creazioneAnagraficaSpresalAnamnesi : risultato " + risultato);

    	        } catch (Exception e) {
    	            recordErrore++;
    	            System.out.println("recordErrore: " + recordErrore);

    	            System.err.println("Errore in creazioneAnagraficaSpresalAnamnesi per CF " + dto.getCodiceFiscale()
    	                    + " : " + e.getMessage());
//    	            e.printStackTrace();
    	            LOGGER.error("Errore nel inserimentiMassiviFileSpresalAnamnesi, fileId={}", fileId, e);
					try {
						String descrizioneErrore = ErroreImportUtility.buildErroreDescrizione(e);
						tracciaElaborazioneService.inserisciErroreRiga(
								elaborazioneId, "ERR_ELABORAZIONE", e.getMessage()+ " :" + descrizioneErrore, null,
								utenteCreazione, dto.getCognome(), dto.getNome(),
								dto.getNascitaData(), dto.getSesso(), dto.getCodiceFiscale(),
								"REEA_T_REGISTRO_SPRESAL_ANAMNESI",          // â era "REEA_T_SOGGETTO"
								dto.getSpresal() != null && dto.getSpresal().getRegSpresalAnamnesiId() != null
										? dto.getSpresal().getRegSpresalAnamnesiId().intValue() : 0

						);

					} catch (Exception ex) { 
//						ex.printStackTrace(); 
						LOGGER.error("Errore nel inserimentiMassiviFileSpresalAnamnesi, fileId={}", fileId, ex);
						}

				}
    	    }
    	} finally {
    	    if (elaborazioneId != null) {
    	    	recordScartati = anagraficaService.recuperaScarti(spresalDTOs.get(0).getFileId());
    	    	System.out.println("spresalDTOs.get(0).getFileId(): " + spresalDTOs.get(0).getFileId());
    	    	if(fileId != null)
    	    		tracciaElaborazioneService.aggiornaFile(fileId, 3, utenteCreazione);
    	        tracciaElaborazioneService.aggiornaFineOk(
    	            elaborazioneId,
    	            3,
    	            result.size() + recordScartati,
    	            result.size() ,
    	            recordScartati,
    	            recordInseriti,
    	            recordErrore,
    	            "Elaborazione completata",
    	            utenteCreazione
    	        );
    	        spresalService.chiudiTuttiRecordTabSpresalAnamnesiByFile(fileId);
    	    }
    	}
    }

	@GetMapping("/inserimentiMassiviFileSpresalEsiti")
	public void ciclaListaSpresalEsiti(FileSalvato fileSalvato) {

		Integer recordInseriti = 0;
		Integer recordErrore = 0;
		Integer recordScartati = 0;
		Integer fileId = null;
		Integer elaborazioneId = null;
		String utenteCreazione  = "ADMIN";

		List<SpresalEsitiDTO> spresalEsitiDTOs = getRecordTabSpresalEsiti(fileId);

		if (spresalEsitiDTOs == null || spresalEsitiDTOs.isEmpty()) {
			System.out.println("/inserimentiMassiviFileSpresalEsiti --> lista vuota");
			return;
		}
		
		utenteCreazione = spresalEsitiDTOs.get(0).getUtenteCreazione();

		elaborazioneId = tracciaElaborazioneService.inserisciStatoElaborazione(spresalEsitiDTOs.get(0).getFileId(), utenteCreazione, 2);
		fileId = spresalEsitiDTOs.get(0).getFileId();

		try {
			for (SpresalEsitiDTO dto : spresalEsitiDTOs) {
				try {
					Integer risultato = anagraficaService.creazioneAnagraficaSpresalEsiti(dto, elaborazioneId);

					if (risultato != null && risultato > 0) {
						recordInseriti++;
						System.out.println("recordInseriti: " + recordInseriti);
					} 
					else {
    	            	recordErrore++;
	                	tracciaElaborazioneService.inserisciErroreRiga(
	        					elaborazioneId,
	        					"Errore Logico",
	        					"Non e' stato possibile associare un Registro",
	        					null,
	        					dto.getUtenteCreazione(),
	        					dto.getCognome(),
	        					dto.getNome(),
	        					dto.getDataNascita(),
	        					dto.getSesso(),
	        					dto.getCodiceFiscale(),
	        					"REEA_T_REGISTRO",
	        					dto.getRegSpresalEsitiId() != null ? dto.getRegSpresalEsitiId().intValue() : null
	        					);
					}
					System.out.println("/inserimentiMassiviFileSpresalEsiti --> creazioneAnagraficaSpresalEsiti : risultato " + risultato);

				} catch (Exception e) {
					recordErrore++;
					System.out.println("recordErrore: " + recordErrore);
					
					System.err.println("Errore in creazioneAnagraficaSpresalEsiti per CF " + dto.getCodiceFiscale()
							+ " : " + e.getMessage());
//					e.printStackTrace();
					LOGGER.error("Errore nel inserimentiMassiviFileSpresalEsiti, fileId={}", fileId, e);
					try {
						String descrizioneErrore = ErroreImportUtility.buildErroreDescrizione(e);
						tracciaElaborazioneService.inserisciErroreRiga(
								elaborazioneId, "ERR_ELABORAZIONE", e.getMessage()+ " :" + descrizioneErrore, null,
								utenteCreazione, dto.getCognome(), dto.getNome(),
								dto.getDataNascita(), dto.getSesso(), dto.getCodiceFiscale(),
								"REEA_T_REGISTRO_SPRESAL_ESITI",
								dto.getRegSpresalEsitiId() != null ? dto.getRegSpresalEsitiId().intValue() : 0

						);

					} catch (Exception ex) { 
//						ex.printStackTrace();
						LOGGER.error("Errore nel inserimentiMassiviFileSpresalEsiti, fileId={}", fileId, ex);
						}


				}
			}
		} finally {
			if (elaborazioneId != null) {
				recordScartati = anagraficaService.recuperaScarti(spresalEsitiDTOs.get(0).getFileId());
				if (fileId != null)
					tracciaElaborazioneService.aggiornaFile(fileId, 3, utenteCreazione);
				tracciaElaborazioneService.aggiornaFineOk(
						elaborazioneId, 3,
						spresalEsitiDTOs.size() + recordScartati,
						spresalEsitiDTOs.size() ,
						recordScartati, recordInseriti, recordErrore,
						"Elaborazione completata", utenteCreazione
				);
				spresalService.chiudiTuttiRecordTabSpresalEsitiByFile(fileId);
			}
		}
	}
	

	@GetMapping("/getEsitiByRegistroId/{registroId}")
	public ResponseEntity<List<SpresalEsitiDTO>> getEsitiByRegistroId(
			@PathVariable("registroId") Integer registroId) {
		List<SpresalEsitiDTO> esiti = spresalService.getEsitiByRegistroId(registroId);
		return ResponseEntity.ok(esiti);
	}

	//Dettaglio assistito – registro – aggiorna dati occupazione
	@PutMapping(value = "/aggiornaCrpt/{regSpresalAnamnesiId}", 
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> aggiornaCrpt(
			@PathVariable("regSpresalAnamnesiId") Integer regSpresalAnamnesiId,
			@RequestPart("body") Map<String, Object> body,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
		spresalService.aggiornaCrpt(regSpresalAnamnesiId, body, auditLogRequest);
		return ResponseEntity.ok().build();
	}


	@GetMapping("/dizionari/settore")
	public ResponseEntity<List<CrptDizionarioDTO>> getDizionarioSettore() {
		List<CrptDizionarioDTO> lista = dsl
				.select(
						REEA_D_REGISTRO_SPRESAL_ANAMNESI_SETTORE.SETTORE_COD,
						REEA_D_REGISTRO_SPRESAL_ANAMNESI_SETTORE.SETTORE_DESC)
				.from(REEA_D_REGISTRO_SPRESAL_ANAMNESI_SETTORE)
				.orderBy(REEA_D_REGISTRO_SPRESAL_ANAMNESI_SETTORE.SETTORE_COD)
				.fetch(r -> {
					CrptDizionarioDTO dto = new CrptDizionarioDTO();
					dto.setCod(r.get(REEA_D_REGISTRO_SPRESAL_ANAMNESI_SETTORE.SETTORE_COD));
					dto.setDesc(r.get(REEA_D_REGISTRO_SPRESAL_ANAMNESI_SETTORE.SETTORE_DESC));
					return dto;
				});
		return ResponseEntity.ok(lista);
	}

	@GetMapping("/dizionari/mansione")
	public ResponseEntity<List<CrptDizionarioDTO>> getDizionarioMansione() {
		List<CrptDizionarioDTO> lista = dsl
				.select(
						REEA_D_REGISTRO_SPRESAL_ANAMNESI_MANSIONE.MANSIONE_COD,
						REEA_D_REGISTRO_SPRESAL_ANAMNESI_MANSIONE.MANSIONE_DESC)
				.from(REEA_D_REGISTRO_SPRESAL_ANAMNESI_MANSIONE)
				.orderBy(REEA_D_REGISTRO_SPRESAL_ANAMNESI_MANSIONE.MANSIONE_COD)
				.fetch(r -> {
					CrptDizionarioDTO dto = new CrptDizionarioDTO();
					dto.setCod(r.get(REEA_D_REGISTRO_SPRESAL_ANAMNESI_MANSIONE.MANSIONE_COD));
					dto.setDesc(r.get(REEA_D_REGISTRO_SPRESAL_ANAMNESI_MANSIONE.MANSIONE_DESC));
					return dto;
				});
		return ResponseEntity.ok(lista);
	}

	@GetMapping("/dizionari/ragionesociale")
	public ResponseEntity<List<CrptDizionarioDTO>> getDizionarioRagioneSociale() {
		List<CrptDizionarioDTO> lista = dsl
				.select(
						DSL.field("ragionesociale", String.class),
						DSL.field("ditta_codice_fiscale", String.class),
						DSL.field("piva", String.class))
				.from(REEA_D_REGISTRO_SPRESAL_ANAMNESI_RAGIONESOCIALE)
				.where(DSL.field("ragionesociale").isNotNull())
				.orderBy(DSL.field("ragionesociale"))
				.fetch(r -> {
					CrptDizionarioDTO dto = new CrptDizionarioDTO();
					dto.setCod(r.get("ragionesociale", String.class));  // cod = ragionesociale
					dto.setDesc(r.get("piva", String.class));
					dto.setDittaCodiceFiscale(r.get("ditta_codice_fiscale", String.class));// desc = piva associata
					return dto;
				});
		return ResponseEntity.ok(lista);
	}

	//Dettaglio assistito – registro – aggiorna accertamenti - flag acquisito
	@PutMapping(value = "/aggiornaPrestazioneAcquisita/{regSpresalEsitiId}", 
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> aggiornaPrestazioneAcquisita(
			@PathVariable("regSpresalEsitiId") Integer regSpresalEsitiId,
			@RequestPart("body") Map<String, Object> body,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
		spresalService.aggiornaPrestazioneAcquisita(regSpresalEsitiId, body, auditLogRequest);
		return ResponseEntity.ok().build();
	}

	//Dettaglio assistito – registro – aggiorna counseling
	@PutMapping(value = "/aggiornaCounseling/{regSpresalAnamnesiId}", 
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> aggiornaCounseling(
			@PathVariable("regSpresalAnamnesiId") Integer regSpresalAnamnesiId,
			@RequestPart("body") Map<String, Object> body,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
		spresalService.aggiornaCounseling(regSpresalAnamnesiId, body, auditLogRequest);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/export/allegato4")
	public ResponseEntity<Void> esportaAllegato4(
			@RequestParam("anno") int anno,
			@RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
		spresalService.esportaAllegato4(anno, auditLogRequest.getUtente());
		return ResponseEntity.ok().build();
	}

	@GetMapping("/export/allegato4")
	public ResponseEntity<List<Allegato4ElaborazioneDTO>> listaExportAllegato4() {
		return ResponseEntity.ok(spresalService.listaElaborazioniAllegato4());
	}

	@GetMapping("/export/allegato4/download/{elaborazioneId}")
	public ResponseEntity<org.springframework.core.io.Resource> downloadAllegato4(
			@PathVariable("elaborazioneId") Integer elaborazioneId) {
		org.springframework.core.io.Resource resource = spresalService.getFileAllegato4(elaborazioneId);
		return ResponseEntity.ok()
				.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + resource.getFilename() + "\"")
				.contentType(org.springframework.http.MediaType.parseMediaType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(resource);
	}

}
