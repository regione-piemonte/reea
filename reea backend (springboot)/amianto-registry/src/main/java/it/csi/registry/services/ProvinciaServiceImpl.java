package it.csi.registry.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.csi.registry.model.ProvinciaDTO;
import it.csi.registry.repositories.ProvinciaRepository;

@Service
public class ProvinciaServiceImpl implements ProvinciaService {

    private final ProvinciaRepository provinciaRepository;

    public ProvinciaServiceImpl(ProvinciaRepository provinciaRepository) {
        this.provinciaRepository = provinciaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvinciaDTO> getListaProvinciaNascitaResidenza() {
        return provinciaRepository.findAllProvinciaNascitaResidenza();
    }
}

