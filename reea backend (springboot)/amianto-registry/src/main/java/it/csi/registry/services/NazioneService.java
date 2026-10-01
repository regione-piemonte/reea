package it.csi.registry.services;

import java.util.List;

import it.csi.registry.model.NazioneDTO;

public interface NazioneService {
	
	List<NazioneDTO> getListaStatoNascitaResidenza();

	String getDescByIstatCod(String istatCod);
}
