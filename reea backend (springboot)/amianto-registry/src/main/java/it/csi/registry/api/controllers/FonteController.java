package it.csi.registry.api.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.model.FonteConDataDTO;
import it.csi.registry.services.FonteService;

@RestController
@RequestMapping("/api/fonte")
public class FonteController {
	
	private final FonteService fonteService;

    public FonteController(FonteService fonteService) {
        this.fonteService = fonteService;
    }

    @GetMapping("/getListaFonteDescByIdSoggetto/{soggettoId}")
    public List<FonteConDataDTO> getListaFonteDescByIdSoggetto(@PathVariable("soggettoId") Long soggettoId) {

        return fonteService.getListaFonteDescByIdSoggetto(soggettoId);
    }

}
