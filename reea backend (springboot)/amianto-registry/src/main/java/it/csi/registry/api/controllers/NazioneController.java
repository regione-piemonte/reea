package it.csi.registry.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.model.NazioneDTO;
import it.csi.registry.services.NazioneService;

@RestController
@RequestMapping("/api/nazioni")
public class NazioneController {

    private final NazioneService nazioneService;

    public NazioneController(NazioneService nazioneService) {
        this.nazioneService = nazioneService;
    }

    @GetMapping("/getListaStatoNascitaResidenza")
    public ResponseEntity<List<NazioneDTO>> getListaStatoNascitaResidenza() {

        List<NazioneDTO> lista = nazioneService.getListaStatoNascitaResidenza();

        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(lista);
    }
}

