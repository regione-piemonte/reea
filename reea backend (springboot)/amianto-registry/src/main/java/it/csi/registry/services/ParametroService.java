package it.csi.registry.services;

import java.util.List;

import it.csi.registry.model.ParametroDTO;

public interface ParametroService {
	
	List<ParametroDTO> getListaParametri();

	String getValoreByCod(String cod);

}
