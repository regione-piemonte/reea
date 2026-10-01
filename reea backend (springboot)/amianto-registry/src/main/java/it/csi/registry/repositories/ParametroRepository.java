package it.csi.registry.repositories;

import java.util.List;

import it.csi.registry.model.ParametroDTO;

public interface ParametroRepository {
	
	List<ParametroDTO> findAll();

	String getValoreByCod(String cod);

}
