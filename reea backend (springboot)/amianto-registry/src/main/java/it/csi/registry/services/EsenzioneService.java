package it.csi.registry.services;

import java.io.IOException;
import java.util.List;
import it.csi.registry.model.EsenzioneDTO;

public interface EsenzioneService {

	List<EsenzioneDTO> getRecordTabEsenzione(Long soggettoId) throws IOException;

}
