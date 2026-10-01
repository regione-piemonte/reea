package it.csi.registry.api.controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.model.EsenzioneDTO;
import it.csi.registry.services.EsenzioneService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/esenzioni")
public class EsenzioneController {
	
    private final EsenzioneService esenzioneService;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EsenzioneController.class);

    public EsenzioneController(EsenzioneService esenzioneService) {
        this.esenzioneService = esenzioneService;
    }
    
    
    @GetMapping("/getRecordTabEsenzione/{soggettoId}")
    public List<EsenzioneDTO> getRecordTabEsenzione(@PathVariable("soggettoId") Long soggettoId) {
    	List<EsenzioneDTO> lista = null;
        try {
			lista = esenzioneService.getRecordTabEsenzione(soggettoId);
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			LOGGER.error("Errore nel recupero dei record tab esenzione, soggettoId={}", soggettoId, e);
		}
        return lista;
    }

}
