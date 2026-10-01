package it.csi.registry.services;

import java.util.List;

import it.csi.registry.model.ComuneDTO;

public interface ComuneService {
	
	List<ComuneDTO> getListaComuneNascitaResidenzaAzienda(Integer provinciaId);

}
