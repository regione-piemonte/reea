package it.csi.registry.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.csi.registry.model.ParametroDTO;
import it.csi.registry.repositories.ParametroRepository;

@Service
public class ParametroServiceImpl implements ParametroService {

    private final ParametroRepository parametroRepository;

    public ParametroServiceImpl(ParametroRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametroDTO> getListaParametri() {
        return parametroRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public String getValoreByCod(String cod) {
        return parametroRepository.getValoreByCod(cod);
    }
}

