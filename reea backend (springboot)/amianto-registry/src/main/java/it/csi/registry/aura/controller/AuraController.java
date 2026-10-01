package it.csi.registry.aura.controller;

import it.csi.registry.aura.dto.AuraAssistitoDto;
import it.csi.registry.aura.services.AuraService;
import it.csi.registry.repositories.ParametroRepository;
import it.csi.registry.soap.anagrafe.clients.AnagrafeFindClient;
import it.csi.registry.soap.anagrafe.clients.AnagrafeGetClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aura")
public class AuraController {

    private final AuraService auraService;
    private final AnagrafeFindClient anagrafeFindClient;
    private final AnagrafeGetClient anagrafeGetClient;
    private final ParametroRepository parametroRepository;

    public AuraController(AuraService auraService,
                           AnagrafeFindClient anagrafeFindClient,
                           AnagrafeGetClient anagrafeGetClient,
                           ParametroRepository parametroRepository) {
        this.auraService = auraService;
        this.anagrafeFindClient = anagrafeFindClient;
        this.anagrafeGetClient = anagrafeGetClient;
        this.parametroRepository = parametroRepository;
    }

    /**
     * GET /api/aura/{idAura}
     */
    @GetMapping("/{idAura}")
    public ResponseEntity<AuraAssistitoDto> getByIdAura(
            @PathVariable("idAura") String idAura) {

        AuraAssistitoDto dto = auraService.getByIdAura(idAura);

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/aura/find?cf=RSSMRA80A01H501X
     */
    @GetMapping("/find")
    public ResponseEntity<AuraAssistitoDto> findByCodiceFiscale(
            @RequestParam("cf") String cf) {

        AuraAssistitoDto dto = auraService.findByCodiceFiscale(cf);

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(dto);
    }


    /**
     * GET /api/aura/findByAnagrafica?cognome=...&nome=...&data_nascita=yyyy-MM-dd
     */
    @GetMapping("/findByAnagrafica")
    public ResponseEntity<List<AuraAssistitoDto>> findByAnagrafica(
            @RequestParam("cognome") String cognome,
            @RequestParam("nome") String nome,
            @RequestParam("data_nascita") String dataNascita) {

        List<AuraAssistitoDto> results = auraService.findByAnagrafica(cognome, nome, dataNascita);

        if (results == null || results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(results != null ? results : List.of());

    }

    /**
     * POST /api/aura/admin/refresh
     * Rilegge AURA_ENDPOINT_FIND, AURA_ENDPOINT_GET, AURA_USER, AURA_PASSWORD da
     * reea_c_parametro e li riapplica ai client SOAP già attivi, senza necessità
     * di riavviare il pod.
     */
    @PostMapping("/admin/refresh")
    public ResponseEntity<Map<String, String>> refreshConfigurazioneAura() {
        String endpointFind = parametroRepository.getValoreByCod("AURA_ENDPOINT_FIND");
        String endpointGet = parametroRepository.getValoreByCod("AURA_ENDPOINT_GET");
        String user = parametroRepository.getValoreByCod("AURA_USER");
        String password = parametroRepository.getValoreByCod("AURA_PASSWORD");

        anagrafeFindClient.reinizializza(endpointFind, user, password);
        anagrafeGetClient.reinizializza(endpointGet, user, password);

        return ResponseEntity.ok(Map.of(
                "endpointFind", endpointFind != null ? endpointFind : "(non configurato)",
                "endpointGet", endpointGet != null ? endpointGet : "(non configurato)",
                "credenzialiConfigurate", String.valueOf(user != null && password != null)
        ));
    }

}