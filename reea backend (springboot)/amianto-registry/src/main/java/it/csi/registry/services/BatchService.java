package it.csi.registry.services;


import it.csi.registry.model.AnagraficaDTO;
import java.util.List;


public interface BatchService {

	List<String[]> findAnagraficheConAura();
}
