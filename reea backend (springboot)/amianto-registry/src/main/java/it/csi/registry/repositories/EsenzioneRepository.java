package it.csi.registry.repositories;

import java.util.List;

import it.csi.registry.model.EsenzioneDTO;

public interface EsenzioneRepository {

	Integer getEsenzioneIdByEsenzioneCod(String esenzioneCod, String diagnosiCod);
	
	List<EsenzioneDTO> findListaEsenzioniByIdSoggetto(Long soggettoId);

	EsenzioneDTO findDescrizioniByCods(String esenzioneCod, String diagnosiCod);

}
