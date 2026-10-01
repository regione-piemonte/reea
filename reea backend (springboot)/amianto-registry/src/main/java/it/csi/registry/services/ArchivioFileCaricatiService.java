package it.csi.registry.services;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import it.csi.registry.model.ArchivioFileCaricatiDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.ConcorrenzaCheckDTO;
import it.csi.registry.model.ScartoFileDTO;

public interface ArchivioFileCaricatiService {

	List<ArchivioFileCaricatiDTO> getArchivioFileCaricati(String filtroFonte, LocalDate dataDa, LocalDate dataA, AuditLogRequest auditLogRequest) throws IOException;

	ConcorrenzaCheckDTO checkConcorrenza(String tipo);

	boolean existsFileByName(String nomeFile);

	List<ScartoFileDTO> getElaborazioneErroreFile(Integer elaborazioneId, AuditLogRequest auditLogRequest);

	List<ScartoFileDTO> getScaricoErroreFile(Integer fileId, AuditLogRequest auditLogRequest);

	boolean hasElaborazioneTerminata(String tipo);
}
