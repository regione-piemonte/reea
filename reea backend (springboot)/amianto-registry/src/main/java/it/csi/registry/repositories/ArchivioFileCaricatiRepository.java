package it.csi.registry.repositories;

import java.time.LocalDate;
import java.util.List;
import it.csi.registry.model.ArchivioFileCaricatiDTO;
import it.csi.registry.model.ScartoFileDTO;

public interface ArchivioFileCaricatiRepository {

	List<ArchivioFileCaricatiDTO> listaArchivioFileCaricati(String filtroFonte, LocalDate dataDa, LocalDate dataA);

	boolean hasPendingElaborazioni(String fonteFolder);

	boolean existsFileByName(String nomeFile);

	List<ScartoFileDTO> getElaborazioneErroreFile(Integer elaborazioneId);

	List<ScartoFileDTO> getScaricoErroreFile(Integer fileId);

	boolean hasElaborazioneTerminata(String tipoFonte);

}
