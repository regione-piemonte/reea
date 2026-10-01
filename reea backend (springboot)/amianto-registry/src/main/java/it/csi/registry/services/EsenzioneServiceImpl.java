package it.csi.registry.services;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import it.csi.registry.model.EsenzioneDTO;
import it.csi.registry.repositories.EsenzioneRepository;

@Service
public class EsenzioneServiceImpl implements EsenzioneService{
	
	private final EsenzioneRepository esenzioneRepository;
	
	
	public EsenzioneServiceImpl(EsenzioneRepository esenzioneRepository) {
		super();
		this.esenzioneRepository = esenzioneRepository;
	}

	
	@Override
	public List<EsenzioneDTO> getRecordTabEsenzione(Long soggettoId) throws IOException {
		List<EsenzioneDTO> listaEsenzioni = esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId);
		return listaEsenzioni;
	}

}
