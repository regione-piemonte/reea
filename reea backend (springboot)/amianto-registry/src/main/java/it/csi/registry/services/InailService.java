package it.csi.registry.services;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.InailDTO;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;

public interface InailService {
	
	FileSalvato importInail(MultipartFile file, String tipologiaInail, AuditLogRequest auditLogRequest) throws IOException;
	
	List<InailDTO> getRecordTabInail(String tipologiaInail, Integer fileId) throws IOException;
	
	void chiudiTuttiRecordTabRegistroInailByFile(Integer fileId);

}
