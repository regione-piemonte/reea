package it.csi.registry.repositories;

import java.util.List;

import it.csi.registry.model.ComuneDTO;

public interface ComuneRepository {
	
	List<ComuneDTO> findComuneNascitaResidenzaAzienda(Integer provinciaId);
	
	String findComuneDescByComuneCod(String comuneCod);
	
	String findComuneCodByComuneDesc(String comuneDesc);

}
