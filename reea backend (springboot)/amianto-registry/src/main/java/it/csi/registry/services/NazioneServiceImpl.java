package it.csi.registry.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.csi.registry.model.NazioneDTO;
import it.csi.registry.repositories.NazioneRepository;

@Service
public class NazioneServiceImpl implements NazioneService {

    private final NazioneRepository nazioneRepository;

    public NazioneServiceImpl(NazioneRepository nazioneRepository) {
        this.nazioneRepository = nazioneRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NazioneDTO> getListaStatoNascitaResidenza() {
        return nazioneRepository.findAllStatoNascitaResidenza();
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getDescByIstatCod(String istatCod) {
        return nazioneRepository.findDescByIstatCod(istatCod);
    }

}

