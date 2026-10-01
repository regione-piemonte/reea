package it.csi.registry.services;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.NplaDTO;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface NplaService {

	FileSalvato importNpla(MultipartFile file, AuditLogRequest auditLogRequest) throws IOException;

    List<NplaDTO> getRecordTabNpla(Integer fileId);

    Integer processaSingoloNpla(Integer regPdlAmiantoId, AnagraficaDTO out, Integer elaborazioneId);
    
    void chiudiTuttiRecordTabRegistroPdlAmiantoByFile(Integer fileId);

}
