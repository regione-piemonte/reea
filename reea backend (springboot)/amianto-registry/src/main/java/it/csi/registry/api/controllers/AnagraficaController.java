package it.csi.registry.api.controllers;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import it.csi.registry.configuratoreDTO.UserDTO;
import it.csi.registry.model.StoriaStatoDTO;

import jakarta.servlet.http.HttpSession;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.ExportDTO;
import it.csi.registry.model.GetListaAnagraficheRequest;
import it.csi.registry.model.ListaAnagraficheResponse;
import it.csi.registry.model.PdfListResponse;
import it.csi.registry.services.AnagraficaService;
import it.csi.registry.services.PdfService;
import it.csi.registry.util.ExcelExportResult;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/anagrafica")
public class AnagraficaController {
    
    private final AnagraficaService anagraficaService;
    private final PdfService pdfService;
    
    public AnagraficaController(AnagraficaService anagraficaService, PdfService pdfService) {
        this.anagraficaService = anagraficaService;
        this.pdfService = pdfService;
    }


//    @PostMapping("/getListaAnagrafiche")
//    public List<AnagraficaDTO> getLista(
//            @RequestParam(name = "fonte_id", required = false) List<Integer> filtroFonteId,
//            @RequestParam(name = "descrizione_stato", required = false) List<String> descrizioniStato,
//            @RequestParam(name = "sezione", required = false) List<String> sezione,
//            @RequestParam(name = "inserito_in_sorveglianza", required = false) Boolean insInSorveglianza,
//            @RequestParam(name = "tipo_elenco_inail", required = false) String tipoElencoInail,
//            @RequestParam(name = "assistenza_asl_id", required = false) List<Integer> assistenzaAslId){
//
//        return anagraficaService.getLista(filtroFonteId, descrizioniStato, sezione, insInSorveglianza, tipoElencoInail, assistenzaAslId);
//    }
    
//    @PostMapping("/getListaAnagrafiche")
//    public List<AnagraficaDTO> getLista(@RequestBody GetListaAnagraficheRequest request) {
//        return anagraficaService.getLista(
//                request.getFiltroFonteId(),
//                request.getDescrizioniStato(),
//                request.getSezione(),
//                request.getInsInSorveglianza() != null
//                        ? request.getInsInSorveglianza().orElse(null)
//                        : null,
//                request.getTipoElencoInail() != null
//                        ? request.getTipoElencoInail().orElse(null)
//                        : null,
//                request.getAssistenzaAslId(),
//                request.getAuditLogRequest()
//        );
//    }
    
    @PostMapping("/getListaAnagrafiche")
    public ListaAnagraficheResponse getLista(@RequestBody GetListaAnagraficheRequest request) {
        return anagraficaService.getLista(
                request.getFiltroFonteId(),
                request.getDescrizioniStato(),
                request.getSezione(),
                request.getInsInSorveglianza() != null
                        ? request.getInsInSorveglianza().orElse(null)
                        : null,
                request.getTipoElencoInail() != null
                        ? request.getTipoElencoInail().orElse(null)
                        : null,
                request.getAssistenzaAslId(),
                request.getCodiceFiscale() != null ? request.getCodiceFiscale().orElse(null) : null,
                request.getCognome() != null ? request.getCognome().orElse(null) : null,
                request.getNome() != null ? request.getNome().orElse(null) : null,
                request.getCognomeLettDa() != null ? request.getCognomeLettDa().orElse(null) : null,
                request.getCognomeLettA() != null ? request.getCognomeLettA().orElse(null) : null,
                request.getNascitaData() != null ? request.getNascitaData().orElse(null) : null,
                request.getAzzeraContatoreProssimoStep(),
                request.getProfiloUtente(),
                request.getAuditLogRequest()
        );
    }

    
    
    @PostMapping(value = "/creazioneAnagrafica",
    		consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Integer> creazioneAnagrafica(@RequestPart("anagraficaDTO") AnagraficaDTO dto, 
    		@RequestPart(name = "flagCaricamentoDaFile", required = true) Boolean flagCaricamentoDaFile,  
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest) {
        Integer risultato = anagraficaService.creazioneAnagrafica(dto, flagCaricamentoDaFile, null, auditLogRequest);
        
        if (risultato > 0) {
        	return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
        
    }
    
    
    @GetMapping("/getById/{soggettoId}")
    public ResponseEntity<AnagraficaDTO> getById(@PathVariable("soggettoId") Long soggettoId) {

        // 1. RECUPERA L'ANAGRAFICA
        AnagraficaDTO dto = anagraficaService.getById(soggettoId);

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. RECUPERA LA LISTA PDF (prefisso = soggettoId)
        // Il prefisso ï¿½ l'id del soggetto come intero (es: 200, 201)
        if(dto.getListaAnamnesiSpresal() != null && dto.getListaAnamnesiSpresal().size() > 0) {
        	int prefisso = dto.getListaAnamnesiSpresal().get(0).getIdSpresal();
        	PdfListResponse pdfResponse = pdfService.listaPdfPerPrefisso(prefisso);
        	dto.setListaPdf(JsonNullable.of(pdfResponse.getPdfList() != null ? pdfResponse.getPdfList() : Collections.emptyList()));
        }

        // 3. RITORNA IL DTO COMPLETO (anagrafica + listaPdf)
        return ResponseEntity.ok(dto);
    }
    
    
//    @GetMapping("/getById/{soggettoId}")
//    public ResponseEntity<AnagraficaDTO> getById(@PathVariable("soggettoId") Long soggettoId) {
//
//        AnagraficaDTO dto = anagraficaService.getById(soggettoId);
//
//        if (dto == null) {
//            return ResponseEntity.notFound().build();
//        }
//        return ResponseEntity.ok(dto);
//    }
    
    
    @GetMapping("/getByIdCampiAnonimizzati/{soggettoId}")
    public ResponseEntity<Map<String, String>> getByIdCampiAnonimizzati(@PathVariable("soggettoId") Long soggettoId, @RequestParam(name = "flagCampiAnonimi", required = true) Boolean flagCampiAnonimi ) {

    	Map<String, String> mappaRitorno = anagraficaService.getByIdCampiAnonimizzati(soggettoId, flagCampiAnonimi, null);

        if (mappaRitorno == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mappaRitorno);
    }
    
    
    @GetMapping("/getByCodiceFiscale/{codiceFiscale}")
    public ResponseEntity<AnagraficaDTO> getByCodiceFiscale(@PathVariable("codiceFiscale") String codiceFiscale) {

        AnagraficaDTO dto = anagraficaService.getByCodiceFiscale(codiceFiscale);

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    //Dettaglio assistito – modifica e-mail o telefono (dettaglio della modifica)
    @PostMapping(value = "/modifica/{soggettoId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnagraficaDTO> aggiornaSoggetto(
            @PathVariable("soggettoId") Long id,
            @RequestPart("anagraficaDTO") AnagraficaDTO anagraficaDTO,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest,
            HttpSession session) {

        UserDTO user = (UserDTO) session.getAttribute("currentUser");
        String cfOperatore = (user != null && user.getCodiceFiscale() != null)
                ? user.getCodiceFiscale() : "SCONOSCIUTO";

        AnagraficaDTO updated = anagraficaService.aggiornaSoggetto(id, anagraficaDTO, auditLogRequest.getUtente() != null ? auditLogRequest.getUtente() : cfOperatore, auditLogRequest);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }



    @GetMapping("/getByCForNomeorCognomeorDataNascita")
    public List<AnagraficaDTO> getByCForNomeorCognomeorDataNascita(
            @RequestParam(name = "codice_fiscale", required = false) String filtroCodiceFiscale,
            @RequestParam(name = "nome",             required = false) String filtroNome,
            @RequestParam(name = "cognome",          required = false) String filtroCognome,
            @RequestParam(name = "nascita_data",     required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate filtroNascitaData) {

        return anagraficaService.getByCForNomeOrCognomeOrDataNascita(filtroCodiceFiscale, filtroNome, filtroCognome, filtroNascitaData);
    }

//    @PostMapping("/aggiornaStato/{soggettoId}")
//    public ResponseEntity<Void> aggiornaStato(
//            @PathVariable("soggettoId") Long soggettoId,
//            @RequestBody AnagraficaDTO dto) {
//        anagraficaService.aggiornaStato(soggettoId, dto);
//        return ResponseEntity.ok().build();
//    }

    @PostMapping(value = "/aggiornaStato/{soggettoId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> aggiornaStato(
            @PathVariable("soggettoId") Long soggettoId,
            @RequestPart("anagraficaDTO") AnagraficaDTO anagraficaDTO,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest,
            HttpSession session) {
//        UserDTO user = (UserDTO) session.getAttribute("currentUser");
//        String cfOperatore = (user != null && user.getCodiceFiscale() != null)
//                ? user.getCodiceFiscale() : "SCONOSCIUTO";
        anagraficaService.aggiornaStato(soggettoId, anagraficaDTO, auditLogRequest);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/storiaStati/{soggettoId}")
    public ResponseEntity<List<StoriaStatoDTO>> storiaStati(@PathVariable("soggettoId") Long soggettoId) {
        return ResponseEntity.ok(anagraficaService.getStoriaStati(soggettoId));
    }
    

    @PostMapping(value = "/scaricaDatiExcelTotaliAsincroni",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Integer>> scaricaDatiExcelTotaliAsincroni(
            @RequestParam(name = "flagDatiAnonimizzati", required = true) Boolean flagDatiAnonimizzati,
            @RequestParam(name = "assistenza_asl_id", required = true) Integer assistenzaAslId,
            @RequestParam(name = "profiloUtente",          required = true) String profiloUtente,
            @RequestPart("auditLogRequest") AuditLogRequest auditLogRequest,
            HttpSession session
            ) {

        Map<String, Integer> ids = anagraficaService.scaricaDatiExcelTotaliAsincroni(
                flagDatiAnonimizzati, assistenzaAslId, profiloUtente, auditLogRequest.getUtente());

        return ResponseEntity.accepted().body(ids);
    }
} 

