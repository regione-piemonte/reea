package it.csi.registry.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.model.ComuneDTO;
import it.csi.registry.services.ComuneService;

@RestController
@RequestMapping("/api/comuni")
public class ComuneController {

    private final ComuneService comuneService;

    public ComuneController(ComuneService comuneService) {
        this.comuneService = comuneService;
    }

    @GetMapping("/getListaComuneNascitaResidenzaAzienda")
    public ResponseEntity<List<ComuneDTO>> getListaComuneNascitaResidenza(
            @RequestParam(name = "provincia_id", required = false) Integer provinciaId) {

        List<ComuneDTO> lista =
                comuneService.getListaComuneNascitaResidenzaAzienda(provinciaId);

        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(lista);
    }
}
