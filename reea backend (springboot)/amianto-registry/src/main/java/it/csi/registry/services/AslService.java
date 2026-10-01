package it.csi.registry.services;

import java.util.List;

import it.csi.registry.model.AslDTO;

public interface AslService {
	
	List<AslDTO> getListaAslCompetenza();
	
	Integer getAslIdByAslCod(String aslCod);

	List<AslDTO> getListaFiltroAssistiti();

}
