package it.csi.registry.services;

import java.util.List;
import it.csi.registry.model.FonteConDataDTO;

public interface FonteService {
	
	List<FonteConDataDTO> getListaFonteDescByIdSoggetto(Long soggettoId);

}
