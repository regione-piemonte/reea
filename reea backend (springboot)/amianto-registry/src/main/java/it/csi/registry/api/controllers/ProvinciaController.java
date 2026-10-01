package it.csi.registry.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.model.ProvinciaDTO;
import it.csi.registry.services.ProvinciaService;

@RestController
@RequestMapping("/api/province")
public class ProvinciaController {

    private final ProvinciaService provinciaService;

    public ProvinciaController(ProvinciaService provinciaService) {
        this.provinciaService = provinciaService;
    }

    @GetMapping("/getListaProvinciaNascitaResidenza")
    public ResponseEntity<List<ProvinciaDTO>> getListaProvinciaNascitaResidenza() {

        List<ProvinciaDTO> lista = provinciaService.getListaProvinciaNascitaResidenza();

        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(lista);
    }
}
