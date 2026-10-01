package it.csi.registry.repositories;

import java.util.List;

import it.csi.registry.model.NazioneDTO;

public interface NazioneRepository {
	
	List<NazioneDTO> findAllStatoNascitaResidenza();

	String findDescByIstatCod(String istatCod);

}
