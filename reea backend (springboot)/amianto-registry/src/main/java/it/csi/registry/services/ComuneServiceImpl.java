package it.csi.registry.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.csi.registry.model.ComuneDTO;
import it.csi.registry.repositories.ComuneRepository;

@Service
public class ComuneServiceImpl implements ComuneService {

    private final ComuneRepository comuneRepository;

    public ComuneServiceImpl(ComuneRepository comuneRepository) {
        this.comuneRepository = comuneRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComuneDTO> getListaComuneNascitaResidenzaAzienda(Integer provinciaId) {
        return comuneRepository.findComuneNascitaResidenzaAzienda(provinciaId);
    }
}
