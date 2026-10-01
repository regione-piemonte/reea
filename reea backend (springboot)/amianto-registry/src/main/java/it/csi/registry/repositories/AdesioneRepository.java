package it.csi.registry.repositories;

import java.util.List;
import java.util.Set;

import it.csi.registry.model.AdesioneDTO;
import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.ExportDTO;

public interface AdesioneRepository {
	
	void scaricoDatiExcelInTAdesione(List<AdesioneDTO> rows, Integer fileId, String utenteLogin);

	void deleteRecordNonProcessati();

	List<AdesioneDTO> listaRecordAdesioni(Integer fileId);

	Set<String> getProcessedAdesioneCods();

	Integer aggiornaAdesioniIdSoggetto(AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);

    List<AdesioneDTO> findBySoggettoId(Long soggettoId);
    
    List<ExportDTO> recuperaPreadesioniPerExport(List<Long> soggettoIds, Integer assistenzaAslId);
}
