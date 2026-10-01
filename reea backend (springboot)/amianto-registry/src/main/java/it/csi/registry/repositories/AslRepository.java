package it.csi.registry.repositories;

import java.util.List;

import it.csi.registry.model.AslDTO;

public interface AslRepository {
	
	List<AslDTO> findAllAslCompetenza();
	
	Integer getAslIdByAslCod(String domicilioAslCod);

	AslDTO findByCod(String aslCod);

	Integer getAslIdByAslDesc(String aslDesc);

	List<AslDTO> findListaFiltroAssistiti();

	AslDTO findById(Integer aslId);

}
