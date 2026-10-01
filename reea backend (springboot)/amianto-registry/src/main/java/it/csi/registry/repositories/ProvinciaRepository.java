package it.csi.registry.repositories;

import java.util.List;

import it.csi.registry.model.ProvinciaDTO;

public interface ProvinciaRepository {
	
	List<ProvinciaDTO> findAllProvinciaNascitaResidenza();
	
	ProvinciaDTO findProvinciaWithComune(String comuneCod);
	
	String findProvinciaCodByProvinciaDesc(String provinciaDesc);

}
