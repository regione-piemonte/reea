package it.csi.registry.repositories;

import java.util.List;

import it.csi.registry.model.FonteConDataDTO;

public interface FonteRepository {
	
	List<FonteConDataDTO> findListaFonteDescByIdSoggetto(Long soggettoId);
	
	List<FonteConDataDTO> findListaFonteDescByIdSoggettoInail(Long soggettoId);

}
