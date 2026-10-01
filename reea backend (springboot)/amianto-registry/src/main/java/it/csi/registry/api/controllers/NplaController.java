package it.csi.registry.api.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.NplaDTO;
import it.csi.registry.repositories.NplaRepository;
import it.csi.registry.services.AnagraficaService;
import it.csi.registry.services.NplaService;
import it.csi.registry.services.TracciaElaborazioneService;
import it.csi.registry.util.ErroreImportUtility;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/npla")
public class NplaController {

    private final NplaService nplaService;
    private final NplaRepository nplaRepository;
    private final TracciaElaborazioneService tracciaElaborazioneService;
    private final AnagraficaService anagraficaService;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NplaController.class);

    public NplaController(DSLContext dsl, NplaService nplaService, NplaRepository nplaRepository, 
    		TracciaElaborazioneService tracciaElaborazioneService, AnagraficaService anagraficaService) {
        this.nplaService = nplaService;
        this.nplaRepository = nplaRepository;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
        this.anagraficaService = anagraficaService;
    }

    
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importNpla(
            @RequestPart("file") MultipartFile file,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
        try {
            nplaService.importNpla(file, auditLogRequest);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            System.out.println(".error(Errore durante l'importazione del file NPLA, e" + e);
            return ResponseEntity.badRequest().build();
        }
    }


    @GetMapping("/getRecordTabNpla")
    public List<NplaDTO> getRecordTabNpla( @RequestParam(name = "file_id", required = false) Integer fileId) {
        List<NplaDTO> lista = null;
        try {
            lista = nplaService.getRecordTabNpla(fileId);
        } catch (Exception e) {
//            e.printStackTrace();
        	LOGGER.error("Errore nel recupero dei record tab npla, fileId={}", fileId, e);
        }
        return lista;
    }


    @GetMapping("/inserimentiMassiviNpla")
    public ResponseEntity<Map<String, Object>> inserimentiMassiviNpla(FileSalvato fileSalvato) {
    	int recordInseriti = 0;
    	int recordErrore = 0;
    	int recordScartati = 0;

    	Integer fileId = null;
    	Integer elaborazioneId = null;
    	String utenteCreazione = "ADMIN";

    	List<String> cfNonTrovatiInAura = new ArrayList<>();
    	Map<String, Object> response = new HashMap<>();

    	List<NplaDTO> nplaDTOs = getRecordTabNpla(null);
    	
    	LOGGER.info("---Numero record NPLA validi da elaborare post modifica nplaDTOs.size()={} ", nplaDTOs.size());

    	if (nplaDTOs == null || nplaDTOs.isEmpty()) {
    		LOGGER.info("/inserimentiMassiviNpla --> lista vuota");
    		response.put("cfNonTrovatiInAura", cfNonTrovatiInAura);
    		response.put("recordInseriti", recordInseriti);
    		response.put("recordErrore", recordErrore);
    		response.put("recordScartati", recordScartati);
    		return ResponseEntity.ok(response);
    	}
    	
    	final int totaleRecordIniziali = getRecordTabNpla(null).size();
    	LOGGER.info("---totaleRecordIniziali={}: ", totaleRecordIniziali);

    	NplaDTO primo = nplaDTOs.get(0);
    	fileId = primo.getFileId();
    	utenteCreazione = primo.getUtenteCreazione() != null ? primo.getUtenteCreazione() : "ADMIN";

    	try {
    		elaborazioneId = tracciaElaborazioneService.inserisciStatoElaborazione(
    				fileId,
    				utenteCreazione,
    				2
    				);

    		for (NplaDTO dto : nplaDTOs) {
    			try {
    				validaDtoNpla(dto);

    				AnagraficaDTO out = buildAnagraficaDto(dto);

    				Integer risultato =  nplaService.processaSingoloNpla(
    						dto.getRegPdlAmiantoId(),
    						out,
    						elaborazioneId
    						);
    				
    				LOGGER.info("---Numero record NPLA validi da elaborare post modifica nplaDTOs.size()={} ", nplaDTOs.size());

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
    							dto.getRegPdlAmiantoId() != null ? dto.getRegPdlAmiantoId().intValue() : null
    							);
    				}

    				System.out.println("/inserimentiMassiviNpla --> processaSingoloNpla : risultato " + risultato);

    			}
    				catch (IllegalStateException e) {
                        String message = e.getMessage() != null ? e.getMessage() : "";

                        if (message.contains("non trovato in AURA")) {
                            cfNonTrovatiInAura.add(dto.getCodiceFiscale());
                            recordErrore++;

                            try {
                                if (dto.getRegPdlAmiantoId() != null) {
                                    nplaRepository.deleteById(dto.getRegPdlAmiantoId());
                                    
                                }
                            } catch (Exception ex) {
                                LOGGER.error("Errore cancellando staging NPLA id={}", dto.getRegPdlAmiantoId(), ex);
                            }

                            salvaErroreRigaNpla(
                                    elaborazioneId,
                                    "CF_NON_IN_AURA",
                                    e,
                                    utenteCreazione,
                                    dto,
                                    "REEA_T_REGISTRO_PDL_AMIANTO",
                                    dto.getRegPdlAmiantoId() != null ? dto.getRegPdlAmiantoId() : 0
                            );
                        } else {
                            recordErrore++;
                            salvaErroreRigaNpla(
                                    elaborazioneId,
                                    "ERR_STATO",
                                    e,
                                    utenteCreazione,
                                    dto,
                                    "REEA_T_SOGGETTO",
                                    null
                            );
                        }

                    	

    			}  catch (Exception e) {
    				recordErrore++;
    				salvaErroreRigaNpla(
    						elaborazioneId,
    						"ERR_ELABORAZIONE",
    						e,
    						utenteCreazione,
    						dto,
    						"REEA_T_SOGGETTO",
    						null
    						);
    			}
    		}

    		return ResponseEntity.ok(buildResponse(cfNonTrovatiInAura, recordInseriti, recordErrore, recordScartati));

    	}  finally {
    		if (elaborazioneId != null) {

    			recordScartati = anagraficaService.recuperaScarti(fileId);

    			LOGGER.info("---Numero record NPLA validi da elaborare post modifica ={} ", totaleRecordIniziali);
    			LOGGER.info(
    			        "---Numero record ancora presenti nella lista al termine nplaDTOs.size()={} ",
    			        nplaDTOs.size()
    			);
    			LOGGER.info("---Numero record scartati={} ", recordScartati);

    			if (fileId != null) 
    				tracciaElaborazioneService.aggiornaFile(fileId, 3, utenteCreazione);



    			tracciaElaborazioneService.aggiornaFineOk(
    					elaborazioneId,
    					3,
    					totaleRecordIniziali + recordScartati,
    					nplaDTOs.size(),
    					recordScartati,
    					recordInseriti,
    					recordErrore,
    					"Elaborazione completata",
    					utenteCreazione
    					);

    			nplaService.chiudiTuttiRecordTabRegistroPdlAmiantoByFile(fileId);
    			
    		}
    	}
    }

    private AnagraficaDTO buildAnagraficaDto(NplaDTO dto) {
        AnagraficaDTO out = new AnagraficaDTO();
        out.setCodiceFiscale(dto.getCodiceFiscale());
        out.setFonteId(4);
        out.setInserimentoTipoId(2);
        out.setDomicilioAslDesc(dto.getAslCantiere());
        out.setAziendaCod(dto.getAziendaPiva());
        out.setAdesioneId(dto.getRegPdlAmiantoId() != null ? dto.getRegPdlAmiantoId().longValue() : null);
        out.setUtenteCreazione(dto.getUtenteCreazione());
        return out;
    }
    
    private Map<String, Object> buildResponse(
            List<String> cfNonTrovatiInAura,
            int recordInseriti,
            int recordErrore,
            int recordScartati
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("cfNonTrovatiInAura", cfNonTrovatiInAura);
        response.put("recordInseriti", recordInseriti);
        response.put("recordErrore", recordErrore);
        response.put("recordScartati", recordScartati);
        return response;
    }
    
    private void validaDtoNpla(NplaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO NPLA nullo");
        }
        if (dto.getRegPdlAmiantoId() == null) {
            throw new IllegalArgumentException("regPdlAmiantoId nullo");
        }
        if (dto.getCodiceFiscale() == null || dto.getCodiceFiscale().isBlank()) {
            throw new IllegalArgumentException("Codice fiscale nullo o vuoto");
        }
    }
    
    private void salvaErroreRigaNpla(
            Integer elaborazioneId,
            String codiceErrore,
            Exception e,
            String utenteCreazione,
            NplaDTO dto,
            String targetTabella,
            Integer targetRecordId
    ) {
        try {
            String descrizioneErrore = ErroreImportUtility.buildErroreDescrizione(e);
            String messaggio = (e.getMessage() != null ? e.getMessage() : "Errore") + " : " + descrizioneErrore;

            tracciaElaborazioneService.inserisciErroreRiga(
                    elaborazioneId,
                    codiceErrore,
                    messaggio,
                    null,
                    utenteCreazione,
                    null,
                    null,
                    null,
                    null,
                    dto != null ? dto.getCodiceFiscale() : null,
                    targetTabella,
                    targetRecordId
            );
        } catch (Exception ex) {
            LOGGER.error(
                    "Errore salvando riga di errore NPLA, elaborazioneId={}, cf={}",
                    elaborazioneId,
                    dto != null ? dto.getCodiceFiscale() : null,
                    ex
            );
        }
    }
}
