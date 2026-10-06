package it.csi.registry.services;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import it.csi.registry.model.ScartoFileDTO;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import it.csi.registry.model.ArchivioFileCaricatiDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.ConcorrenzaCheckDTO;
import it.csi.registry.repositories.ArchivioFileCaricatiRepository;
import it.csi.registry.repositories.ParametroRepository;

@Service
public class ArchivioFileCaricatiServiceImpl implements ArchivioFileCaricatiService {

    private final ArchivioFileCaricatiRepository archivioFileCaricatiRepository;
    private final ParametroRepository parametroRepository;
    private final AuditService auditService;

    public ArchivioFileCaricatiServiceImpl(ArchivioFileCaricatiRepository archivioFileCaricatiRepository,
                                           ParametroRepository parametroRepository,
                                           AuditService auditService) {
    	this.archivioFileCaricatiRepository = archivioFileCaricatiRepository;
    	this.parametroRepository = parametroRepository;
    	this.auditService = auditService;
    }


	@Override
	public boolean existsFileByName(String nomeFile) {
		return archivioFileCaricatiRepository.existsFileByName(nomeFile);
	}

	@Override
	public ConcorrenzaCheckDTO checkConcorrenza(String tipo) {
		String folder = switch (tipo) {
			case "npla"          -> "npla";
			case "inail-a", "inail-b" -> "inail";
			case "spresal", "spresal-esiti" -> "spresal";
			default              -> "preadesione";
		};
		boolean pending = archivioFileCaricatiRepository.hasPendingElaborazioni(folder);
		ConcorrenzaCheckDTO dto = new ConcorrenzaCheckDTO();
		dto.setBloccato(pending);
		if (pending) {
			String msg = parametroRepository.getValoreByCod("CONCORRENZA_IMPORT");
			dto.setMessaggio(msg);
		}
		return dto;
	}

	@Override
	public List<ArchivioFileCaricatiDTO> getArchivioFileCaricati(String filtroFonte, LocalDate dataDa, LocalDate dataA, 
			AuditLogRequest auditLogRequest) throws IOException {
		List<ArchivioFileCaricatiDTO> listaRitorno = null;
		listaRitorno = archivioFileCaricatiRepository.listaArchivioFileCaricati(filtroFonte, dataDa, dataA);
		
		salvaAuditArchivioFile(
	            auditLogRequest,
	            "Ricerca file"
	    );
		 
		return listaRitorno;
	}


	@Override
	public List<ScartoFileDTO> getElaborazioneErroreFile(Integer elaborazioneId, AuditLogRequest auditLogRequest) {
		
		List<ScartoFileDTO> listaRitorno = null;
		listaRitorno = archivioFileCaricatiRepository.getElaborazioneErroreFile(elaborazioneId);
		
		salvaAuditArchivioFile(
	            auditLogRequest,
	            "Visualizzazione degli errori"
	    );
        
        return listaRitorno;
	}
	
	
	@Override
	public List<ScartoFileDTO> getScaricoErroreFile(Integer fileId, AuditLogRequest auditLogRequest) {
		
		List<ScartoFileDTO> listaRitorno = null;
		listaRitorno = archivioFileCaricatiRepository.getScaricoErroreFile(fileId);
		
		salvaAuditArchivioFile(
	            auditLogRequest,
	            "Visualizzazione degli scarti"
	    );
        
        return listaRitorno;
	}
	
	@Override
	public boolean hasElaborazioneTerminata(String tipo) {
	    return archivioFileCaricatiRepository.hasElaborazioneTerminata(tipo);
	}

	
	private void salvaAuditArchivioFile(
	        AuditLogRequest auditLogRequest,
	        String oggettoOperazione) {

	    AuditLogRequest audit =
	            auditLogRequest != null
	                    ? auditLogRequest
	                    : new AuditLogRequest();

	    audit.setOperazione("Archivio file");
	    audit.setOggOper(
	            JsonNullable.of(oggettoOperazione)
	    );
	    audit.setKeyOper(null);
	    audit.setRequestPayload(null);
	    audit.setEsitoChiamata(200);
	    audit.setResponsePayload(null);

	    auditService.salvaAudit(audit);
	}

}

