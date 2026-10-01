package it.csi.registry.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.model.AslDTO;
import it.csi.registry.services.AslService;

@RestController
@RequestMapping("/api/asl")
public class AslController {

    private final AslService aslService;

    public AslController(AslService aslService) {
        this.aslService = aslService;
    }

    @GetMapping("/getListaASLCompetenza")
    public ResponseEntity<List<AslDTO>> getListaAslCompetenza() {

        List<AslDTO> lista = aslService.getListaAslCompetenza();

        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/getListaFiltroAssistiti")
    public ResponseEntity<List<AslDTO>> getListaFiltroAssistiti() {
        List<AslDTO> lista = aslService.getListaFiltroAssistiti();
        return ResponseEntity.ok(lista);
    }


}

