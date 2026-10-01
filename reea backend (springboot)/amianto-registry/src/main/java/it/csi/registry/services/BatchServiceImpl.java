package it.csi.registry.services;


import it.csi.registry.model.AnagraficaDTO;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import it.csi.registry.services.BatchService;
import java.util.List;


import it.csi.registry.repositories.AnagraficaRepository;
import it.csi.registry.repositories.AslRepository;

@Service
public class BatchServiceImpl implements BatchService {
	
	private AnagraficaRepository anagraficaRepository = null;
	
	public BatchServiceImpl(AnagraficaRepository anagraficaRepository) {
        this.anagraficaRepository = anagraficaRepository;
    }
	@Override
	public List<String[]> findAnagraficheConAura() {
		
		
		return anagraficaRepository.findAnagraficheConAura();
		//return null;
	}

	public AnagraficaRepository getAnagraficaRepository() {
		return anagraficaRepository;
	}

}
