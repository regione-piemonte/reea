package it.csi.registry.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.csi.registry.model.FonteConDataDTO;
import it.csi.registry.repositories.FonteRepository;

@Service
public class FonteServiceImpl implements FonteService{
	
	private final FonteRepository fonteRepository;

    public FonteServiceImpl(FonteRepository fonteRepository) {
        this.fonteRepository = fonteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FonteConDataDTO> getListaFonteDescByIdSoggetto(Long soggettoId) {
        return fonteRepository.findListaFonteDescByIdSoggetto(soggettoId);
    }

}
