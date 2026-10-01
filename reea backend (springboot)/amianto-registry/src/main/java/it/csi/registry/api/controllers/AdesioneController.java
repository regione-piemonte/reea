package it.csi.registry.api.controllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import it.csi.registry.model.AdesioneDTO;
import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.EsposizioneDTO;
import it.csi.registry.services.AdesioneService;
import it.csi.registry.services.AnagraficaService;
import it.csi.registry.services.TracciaElaborazioneService;
import it.csi.registry.util.ErroreImportUtility;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/adesioni")
public class AdesioneController {

    private final AdesioneService adesioneService;
    private final AnagraficaService anagraficaService;
    private final TracciaElaborazioneService tracciaElaborazioneService;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdesioneController.class);

    public AdesioneController(AdesioneService adesioneService, AnagraficaService anagraficaService, 
    							 TracciaElaborazioneService tracciaElaborazioneService) {
    	this.adesioneService = adesioneService;
        this.anagraficaService = anagraficaService;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importExcel(
            @RequestPart("file") MultipartFile file,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
        try {
            adesioneService.importAdesioni(file, auditLogRequest);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            System.out.println(".error(Errore durante l'importazione del file Excel adesioni, e" + e);
            return ResponseEntity.badRequest().build();
        }
    }


    @GetMapping("/getRecordTabAdesioni")
    public List<AdesioneDTO> getRecordTabAdesioni(
            @RequestParam(name = "file_id", required = false) Integer fileId) {
        List<AdesioneDTO> lista = null;
        try {
            lista = adesioneService.getRecordTabAdesioni(fileId);
        } catch (IOException e) {
//            e.printStackTrace();
        	LOGGER.error("Errore nel recupero dei record tab adesioni, fileId={}", fileId, e);
        }
        return lista;
    }



    @GetMapping("/inserimentiMassiviFileAdesioni")
    public void ciclaListaAdesioni() {
        Integer recordInseriti = 0;
        Integer recordErrore = 0;
        Integer recordScartati = 0;
        Integer fileId = null;
        Integer elaborazioneId = null;
        String utenteCreazione  = "ADMIN";

        List<AdesioneDTO> adesioneDTOs = getRecordTabAdesioni(fileId);

        if (adesioneDTOs == null || adesioneDTOs.isEmpty()) {
            System.out.println("/inserimentiMassiviFileAdesioni --> lista vuota");
            return;
        }
        
        utenteCreazione = adesioneDTOs.get(0).getUtenteCreazione();

        elaborazioneId = tracciaElaborazioneService.inserisciStatoElaborazione(adesioneDTOs.get(0).getFileId(), utenteCreazione, 2);
        fileId = adesioneDTOs.get(0).getFileId();

        try {
            for (AdesioneDTO dto : adesioneDTOs) {

                AnagraficaDTO out = new AnagraficaDTO();
                out.setAdesioneId(dto.getAdesioneId());
                out.setAdesioneCod(dto.getAdesioneCod());
                out.setCodiceFiscale(dto.getCodiceFiscale());
                out.setNome(dto.getNome());
                out.setCognome(dto.getCognome());
                out.setNascitaData(dto.getNascitaData());
                out.setSesso(dto.getSesso());
                out.setNascitaProvinciaDesc(dto.getNascitaProvinciaDesc());
                out.setNascitaComuneDesc(dto.getNascitaComuneDesc());
                out.setTesseraTeam(dto.getTesseraTeam());
                out.setIdAura(dto.getIdAura());
                out.setDomicilioProvinciaDesc(dto.getDomicilioProvinciaDesc());
                out.setDomicilioComuneDesc(dto.getDomicilioComuneDesc());
                out.setDomicilioComuneCod(dto.getDomicilioComuneCod());
                out.setDomicilioCap(dto.getDomicilioCap());
                out.setEmail(dto.getEmail());
                out.setTelefono(dto.getTelefono());
                out.setDomicilioAslCod(dto.getDomicilioAslCod());
                out.setDomicilioAslDesc(dto.getDomicilioAslDesc());
                out.setAziendaCod(dto.getAziendaCod());
                out.setResidenzaAslDesc(dto.getResidenzaAslDesc());
                out.setResidenzaAslCod(dto.getResidenzaAslCod());
                out.setFileId(dto.getFileId());
                out.setFonteId(dto.getFonteId());
                out.setUtenteCreazione(dto.getUtenteCreazione());

                // Una sola esposizione per questa riga
                EsposizioneDTO e = new EsposizioneDTO();
                e.setEsposizioneAzienda(dto.getEsposizioneAzienda());
                e.setEsposizioneAziendaComuneCod(dto.getEsposizioneAziendaComuneCod());
                e.setEsposizioneAziendaComuneDesc(dto.getEsposizioneAziendaComuneDesc());
                e.setEsposizioneAziendaCap(dto.getEsposizioneAziendaCap());
                e.setEsposizioneAziendaProvincia(dto.getEsposizioneAziendaProvincia());
                e.setEsposizioneMansione(dto.getEsposizioneMansione());
                e.setEsposizioneInizio(dto.getEsposizioneInizio());
                e.setEsposizioneFine(dto.getEsposizioneFine());

                out.setListaEsposizione(Collections.singletonList(e));

                try {
                    Integer risultato = anagraficaService.creazioneAnagrafica(out, true, elaborazioneId, null);

                    if (risultato != null && risultato > 0) {
                        recordInseriti++;
                        System.out.println("recordInseriti: " + recordInseriti);
                    }
//                    else {
//                    	recordScartati++;
//                    	System.out.println("recordScartati: " + recordScartati);
//                    }

                } catch (Exception e1) {
                    recordErrore++;
                    System.out.println("recordErrore: " + recordErrore);
                    System.err.println("Errore in creazioneAnagrafica per CF " + out.getCodiceFiscale()
                            + " : " + e1.getMessage());
//                    e1.printStackTrace();
                    LOGGER.error("Errore in creazioneAnagrafica per CF " + out.getCodiceFiscale(), fileId, e);
                    try {  
                    	String descrizioneErrore = ErroreImportUtility.buildErroreDescrizione(e1);
                        tracciaElaborazioneService.inserisciErroreRiga(
                                elaborazioneId, "ERR_ELABORAZIONE", e1.getMessage() + " :" + descrizioneErrore , null,
                                utenteCreazione, out.getCognome(), out.getNome(),
                                out.getNascitaData(), out.getSesso(), out.getCodiceFiscale(),
                                "REEA_T_ADESIONE",                                               // ← era "REEA_T_SOGGETTO"
                                out.getAdesioneId() != null ? out.getAdesioneId().intValue() : 0 // ← era null → 0
                        );
                    } catch (Exception ex) { 
//                    	ex.printStackTrace(); 
                    	LOGGER.error("Errore in creazioneAnagrafica per CF " + out.getCodiceFiscale(), fileId, e);
                    	}
                }
            }

        } finally {
            if (elaborazioneId != null) {
                try {
                	recordScartati = anagraficaService.recuperaScarti(adesioneDTOs.get(0).getFileId());
                    if (fileId != null)
                        tracciaElaborazioneService.aggiornaFile(fileId, 3, utenteCreazione);

                    tracciaElaborazioneService.aggiornaFineOk(
                            elaborazioneId, 3, adesioneDTOs.size() + recordScartati,
                            adesioneDTOs.size() , recordScartati, recordInseriti, recordErrore,
                            "Elaborazione completata", utenteCreazione
                    );
                    anagraficaService.chiudiTuttiRecordTabAdesioneByFile(fileId);
                } catch (Exception exFinally) {
                    System.err.println("Errore nel finally durante aggiornaFineOk: " + exFinally.getMessage());
//                    exFinally.printStackTrace();
                    LOGGER.error("Errore in creazioneAnagrafica: " , fileId, exFinally);
                }
            }
        }
    }


	@GetMapping("/getBySoggettoId/{soggettoId}")
	public List<AdesioneDTO> getBySoggettoId(@PathVariable("soggettoId") Long soggettoId) {
		return adesioneService.getAdesioneBySoggettoId(soggettoId);
	}


//	@PostMapping("/scaricaDatiExcelPreadesioniFiltrate")
//    public ResponseEntity<ByteArrayResource> scaricaDatiExcelPreadesioniFiltrate(
//            @RequestBody List<Long> soggettoIds) {
//
//        byte[] file = adesioneService.scaricaDatiExcelPreadesioniFiltrate(soggettoIds);
//
//        ByteArrayResource resource = new ByteArrayResource(file);
//        
//        String timestamp = LocalDateTime.now()
//                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
//        
//        String fileName = "export_preadesioni_filtrate_" + timestamp + ".xlsx";
//
////        String fileName = Boolean.TRUE.equals(flagDatiAnonimizzati)
////                ? "export_totale_pseudo_" + timestamp + ".xlsx"
////                : "export_totale_chiaro_" + timestamp + ".xlsx";
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .contentLength(file.length)
//                .body(resource);
//    }
	
	@PostMapping("/scaricaDatiExcelPreadesioniFiltrate")
	public ResponseEntity<ByteArrayResource> scaricaDatiExcelPreadesioniFiltrate(
			@RequestPart("soggettoIds") List<Long> soggettoIds,
	        @RequestPart(name = "assistenza_asl_id", required = false) Integer assistenzaAslId,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {

	    byte[] file = adesioneService.scaricaDatiExcelPreadesioniFiltrate(soggettoIds, assistenzaAslId, auditLogRequest);

	    if (file == null) {
	        return ResponseEntity.noContent().build();
	    }

	    ByteArrayResource resource = new ByteArrayResource(file);

	    String timestamp = LocalDateTime.now()
	            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
	    
	    String fileName = assistenzaAslId == null
                ? "export_preadesioni_filtrate_" + timestamp + ".xlsx"
                : "export_preadesioni_filtrate_" + assistenzaAslId + "_" + timestamp + ".xlsx";

	    MediaType mediaType = Objects.requireNonNull(
	            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	    );

	    return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
	            .contentType(mediaType)
	            .contentLength(file.length)
	            .body(resource);
	}

}

