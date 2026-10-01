package it.csi.registry.services;

import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import it.csi.registry.model.AdesioneDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;

public interface AdesioneService {
	
	FileSalvato importAdesioni(MultipartFile file, AuditLogRequest auditLogRequest) throws IOException;
	
	List<AdesioneDTO> getRecordTabAdesioni(Integer fileId) throws IOException;

	List<AdesioneDTO> getAdesioneBySoggettoId(Long soggettoId);

//	Integer aggiornaAdesioniIdSoggetto(AnagraficaDTO dto) throws IOException;
	
	byte[] scaricaDatiExcelPreadesioniFiltrate(List<Long> soggettoIds, Integer assistenzaAslId, AuditLogRequest auditLogRequest);

}
