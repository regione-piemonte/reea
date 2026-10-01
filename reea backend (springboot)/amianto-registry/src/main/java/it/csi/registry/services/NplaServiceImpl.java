package it.csi.registry.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jooq.DSLContext;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.NplaDTO;
import it.csi.registry.model.SpresalEsitiDTO;
import it.csi.registry.repositories.AnagraficaRepository;
import it.csi.registry.repositories.NplaRepository;
import it.csi.registry.repositories.TracciaElaborazioneRepository;
import it.csi.registry.util.ExcelFileUtils;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;
import it.csi.registry.util.ExcelNplaParser;

@Service
public class NplaServiceImpl implements NplaService {

	private final DSLContext dsl;
    private final NplaRepository nplaRepository;
    private final AnagraficaService anagraficaService;
    private final AnagraficaRepository anagraficaRepository;
    private final TracciaElaborazioneService elaborazioneService;
    private final TracciaElaborazioneRepository tracciaElaborazioneRepository;
    private final AuditService auditService;
    private final AuditPayloadMapper auditPayloadMapper;
    private final ExcelNplaParser excelNplaParser;

    
    
    @Value("${npla.upload.dir}")
    private String baseDir;

    public NplaServiceImpl(DSLContext dsl, NplaRepository nplaRepository, AnagraficaService anagraficaService, AnagraficaRepository anagraficaRepository,
    						TracciaElaborazioneService elaborazioneService, TracciaElaborazioneRepository tracciaElaborazioneRepository, 
    						AuditService auditService, AuditPayloadMapper auditPayloadMapper, ExcelNplaParser excelNplaParser) {
        this.dsl = dsl;
    	this.nplaRepository = nplaRepository;
        this.anagraficaService = anagraficaService;
        this.anagraficaRepository = anagraficaRepository;
        this.elaborazioneService = elaborazioneService;
        this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
        this.auditService = auditService;
        this.auditPayloadMapper = auditPayloadMapper;
        this.excelNplaParser = excelNplaParser;
    }

    @Override
    @Transactional
    public Integer processaSingoloNpla(Integer regPdlAmiantoId, AnagraficaDTO out, Integer elaborazioneId) {
        Integer soggettoId = anagraficaService.creazioneAnagrafica(out, true, elaborazioneId, null);
        Integer registroId = anagraficaRepository.findRegistroIdBySoggettoId(soggettoId);
        nplaRepository.updateRegistroId(regPdlAmiantoId, registroId);
        tracciaElaborazioneRepository.inserisciFileImpatto(
        		dsl,
                elaborazioneId,
                "REEA_T_REGISTRO_PDL_AMIANTO",
                out.getAdesioneId().intValue(),
                2,
                out.getUtenteCreazione()
        );
        
        return registroId;
    }
    
    
    @Override
    public FileSalvato importNpla(MultipartFile file, AuditLogRequest auditLogRequest) throws IOException {

        FileSalvato salvato = ExcelFileUtils.salvaExcelFileSystem(file, baseDir);

        Integer fileId = tracciaElaborazioneRepository.inserisciFile(
        		dsl,
                salvato.fileName(),
                salvato.filePath(),
                salvato.checksum(),
                file.getContentType(),
                file.getSize(),
                1,
                auditLogRequest != null && auditLogRequest.getUtente() != null ? auditLogRequest.getUtente() : "ADMIN"
        );

        if (auditLogRequest == null) {
            auditLogRequest = new AuditLogRequest();
        }

//        auditLogRequest.setOperazione("import excel");
//        auditLogRequest.setOggOper(JsonNullable.of("NPLA"));
//        auditLogRequest.setKeyOper(null);
        auditLogRequest.setResponsePayload(null);
        auditLogRequest.setEsitoChiamata(200);

        Map<String, Object> auditPayload = new HashMap<>();
        auditPayload.put("fileName", salvato.fileName());
        auditPayload.put("filePath", salvato.filePath());
        auditPayload.put("checksum", salvato.checksum());
        auditPayload.put("contentType", file.getContentType());
        auditPayload.put("size", file.getSize());
        auditPayload.put("fileId", fileId);

        auditLogRequest.setRequestPayload(JsonNullable.of(auditPayloadMapper.toBytes(auditPayload)));

        auditService.salvaAudit(auditLogRequest);
        
        String utenteCreazione = auditLogRequest.getUtente() != null ? auditLogRequest.getUtente() : "ADMIN";

        try (InputStream is = new FileInputStream(salvato.filePath())) {
//            nplaRepository.deleteRecordNonProcessati();

            List<NplaDTO> rows = excelNplaParser.parseNpla(is, dsl, fileId, utenteCreazione);

//            Set<String> listaRecordApertiNpla = nplaRepository.getListaRecordApertiNpla();

            Set<String> listaRecordRegistroIdIsNotNullNpla = nplaRepository.getListaRecordRegistroIdIsNotNullNpla();
            
            List<NplaDTO> doppioni = rows == null ? List.of() : rows.stream()
                    .filter(r -> {
                        if (r.getCodiceFiscale() == null || r.getCodiceFiscale().isBlank()) {
                            return false;
                        }
//                        if (r.getPeriodo() == null) {
//                            return false;
//                        }
                        if (r.getIdCantiere() == null) {
                            return false;
                        }
                        if (r.getAslCantiere() == null || r.getAslCantiere().isBlank()) {
                            return false;
                        }

                        String key = r.getCodiceFiscale() + "_" + r.getIdCantiere();
                        return listaRecordRegistroIdIsNotNullNpla.contains(key);
                    })
                    .toList();
            
            List<NplaDTO>  nuovi = rows == null ? List.of() : rows.stream()
                    .filter(r -> {
                        if (r.getCodiceFiscale() == null || r.getCodiceFiscale().isBlank()) {
                            return false;
                        }
//                        if (r.getPeriodo() == null) {
//                            return false;
//                        }
                        if (r.getIdCantiere() == null) {
                            return false;
                        }
                        if (r.getAslCantiere() == null || r.getAslCantiere().isBlank()) {
                            return false;
                        }

                        String key = r.getCodiceFiscale() + "_" + r.getIdCantiere();
                        return !listaRecordRegistroIdIsNotNullNpla.contains(key);
                    })
                    .toList();
            
            
            for (NplaDTO oggetto : doppioni) {
            	String codiceFiscale = oggetto.getCodiceFiscale();

                if (oggetto != null && oggetto.getCodiceFiscale().contains("_")) {
                    codiceFiscale = oggetto.getCodiceFiscale().substring(0, oggetto.getCodiceFiscale().indexOf("_"));
                }
            	tracciaElaborazioneRepository.inserisciScarto(
            			dsl,
                        fileId,
                        "ERR_ELABORAZIONE",
                        "Npla gia' esistente",
                        null,
                        utenteCreazione,
                        null,
                        null,
                        null,
                        null,
                        codiceFiscale,
                        "REEA_T_REGISTRO_PDL_AMIANTO",
                        0
                );
            }

            if (!nuovi.isEmpty()) {
                nplaRepository.scaricaNpla(nuovi, fileId, auditLogRequest.getUtente());
            }
            else
            	tracciaElaborazioneRepository.aggiornaFile(dsl,fileId, 4, auditLogRequest.getUtente());
        }
        // DE ORA POSSO FINALMENTE CHIUDERE LA T_FILE PERCHE' HO SAVATO TUTTE LE RIGHE CHE POTEVO
        tracciaElaborazioneRepository.aggiornaFineValidita(dsl, fileId);
        return salvato;
    }
//    @Override
//    public FileSalvato importNpla(MultipartFile file) throws IOException {
//
//        // 1. Salva subito il file su filesystem
//        FileSalvato salvato = ExcelFileUtils.salvaExcelFileSystem(file, baseDir);
//
//        // 2. Inserisci il record su reea_t_file
//        Integer fileId = elaborazioneService.inserisciFile(
//            salvato.fileName(),
//            salvato.filePath(),
//            salvato.checksum(),
//            file.getContentType(),
//            file.getSize(),
//            1,
//            "ADMIN"
//        );
//
//        // 3. Leggi il file DAL PATH SALVATO, non pi� dal MultipartFile
//        try (InputStream is = new FileInputStream(salvato.filePath())) {
//            // 1. Cancella staging non ancora processati
//            nplaRepository.deleteRecordNonProcessati();
//
//            // 2. Parsa il file NPLA
//            List<NplaDTO> rows = ExcelNplaParser.parseNpla(is);
//
//            // 3. Escludi campi obbligatori null e CF+cantiere gi� processati
//            Set<String> giaProcessati = nplaRepository.getProcessedCantiereCods();
//
//            List<NplaDTO> nuovi = rows.stream()
//                    .filter(r -> {
//                        if (r.getCodiceFiscale() == null || r.getCodiceFiscale().isBlank()) return false;
//                        if (r.getPeriodo() == null) return false;
//                        if (r.getIdCantiere() == null) return false;
//                        if (r.getAslCantiere() == null || r.getAslCantiere().isBlank()) return false;
//
//                        String key = r.getCodiceFiscale() + "_" + r.getIdCantiere();
//                        return !giaProcessati.contains(key);
//                    })
//                    .toList();
//
//            // 4. Inserisci solo i nuovi
//            if (!nuovi.isEmpty()) {
//                nplaRepository.scaricaNpla(nuovi, fileId);
//            }
//        }
//
//        return salvato;
//    }


    @Override
    public List<NplaDTO> getRecordTabNpla(Integer fileId) {
//    	// DE CANCELLO TUTTI I RECORD NON ELABORATI NELLE VOLTE PRECEDENTI
//    	System.out.println("cancello le righe spurie....");
//    	nplaRepository.deleteRecordNonProcessati();
        return nplaRepository.listaRecordNpla(fileId);
    }
    
    
    @Override
	public void chiudiTuttiRecordTabRegistroPdlAmiantoByFile(Integer fileId) {
    	nplaRepository.chiudiTuttiRecordTabRegistroPdlAmiantoByFile(fileId);
	}
}
