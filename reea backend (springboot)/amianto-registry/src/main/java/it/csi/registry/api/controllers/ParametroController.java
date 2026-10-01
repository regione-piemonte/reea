package it.csi.registry.api.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.model.ParametroDTO;
import it.csi.registry.services.ParametroService;

@RestController
@RequestMapping("/api/parametri")
public class ParametroController {

    private final ParametroService parametroService;

    public ParametroController(ParametroService parametroService) {
        this.parametroService = parametroService;
    }

    @GetMapping("/getListaParametri")
    public List<ParametroDTO> getListaParametri() {
        return parametroService.getListaParametri();
    }

    @GetMapping("/getValore")
    public String getValore(@RequestParam("cod") String cod) {
        return parametroService.getValoreByCod(cod);
    }
}

