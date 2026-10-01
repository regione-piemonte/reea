package it.csi.registry.repositories;

import it.csi.registry.model.NplaDTO;
import java.util.List;
import java.util.Set;

public interface NplaRepository {
    void deleteRecordNonProcessati();
    void scaricaNpla(List<NplaDTO> rows, Integer fileId, String utenteLogin);
    List<NplaDTO> listaRecordNpla(Integer fileId);
    Set<String> getListaRecordApertiNpla();
    void updateRegistroId(Integer regPdlAmiantoId, Integer registroId);
    void deleteById(Integer regPdlAmiantoId);
    
    void chiudiTuttiRecordTabRegistroPdlAmiantoByFile(Integer fileId);
	Set<String> getListaRecordRegistroIdIsNotNullNpla();

}
