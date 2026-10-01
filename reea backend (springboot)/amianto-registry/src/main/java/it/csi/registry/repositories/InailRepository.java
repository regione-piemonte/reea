package it.csi.registry.repositories;

import java.util.List;
import java.util.Set;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.InailDTO;

public interface InailRepository {
	
	void scaricoDatiExcelInTRegistroInail(List<InailDTO> rows, String tipologiaInail, Integer fileId, String utenteCreazione);

    void deleteRecordNonProcessati(String tipologiaInail);

	Set<String> getProcessedCodiciFiscali(String tipologiaInail);

	List<InailDTO> listaRecordInail(String tipologiaInail, Integer fileId);

	Integer aggiornaInailRegistroId(AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione);
	
	void chiudiTuttiRecordTabRegistroInailByFile(Integer fileId);

}
