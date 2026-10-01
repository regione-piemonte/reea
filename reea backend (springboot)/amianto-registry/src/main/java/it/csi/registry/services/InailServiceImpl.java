package it.csi.registry.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jooq.DSLContext;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.InailDTO;
import it.csi.registry.repositories.InailRepository;
import it.csi.registry.repositories.TracciaElaborazioneRepository;
import it.csi.registry.util.ErroreImportUtility;
import it.csi.registry.util.ExcelFileUtils;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;
import it.csi.registry.util.ExcelParser;

@Service
public class InailServiceImpl implements InailService {

	private final DSLContext dsl;
    private final InailRepository inailRepository;
    private final TracciaElaborazioneRepository elaborazioneRepository;
    private final AuditService auditService;
    private final AuditPayloadMapper auditPayloadMapper;
    private final TracciaElaborazioneRepository tracciaElaborazioneRepository;
    
    
    @Value("${inail.upload.dir}")
    private String baseDir;

    public InailServiceImpl(DSLContext dsl, InailRepository inailRepository, TracciaElaborazioneRepository elaborazioneRepository,
    		AuditService auditService, AuditPayloadMapper auditPayloadMapper, TracciaElaborazioneRepository tracciaElaborazioneRepository) {
        this.dsl = dsl;
    	this.inailRepository = inailRepository;
        this.elaborazioneRepository = elaborazioneRepository;
        this.auditService = auditService;
        this.auditPayloadMapper = auditPayloadMapper;
		this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
    }
    
    
//    @Override
//    public FileSalvato importInail(MultipartFile file, String tipologiaInail, AuditLogRequest auditLogRequest) throws IOException {
//
//        FileSalvato salvato = ExcelFileUtils.salvaExcelFileSystem(file, baseDir);
//
//        Integer fileId = elaborazioneRepository.inserisciFile(
//                dsl,
//                salvato.fileName(),
//                salvato.filePath(),
//                salvato.checksum(),
//                file.getContentType(),
//                file.getSize(),
//                1,
//                auditLogRequest != null && auditLogRequest.getUtente() != null ? auditLogRequest.getUtente() : "ADMIN"
//        );
//
//        if (auditLogRequest == null) {
//            auditLogRequest = new AuditLogRequest();
//        }
//        String currentOgg = unwrap(auditLogRequest.getOggOper());  // o auditLogRequest.getOggOper().orElse(null)
//
//        if (currentOgg != null && !currentOgg.isBlank()) {
//            auditLogRequest.setOggOper(JsonNullable.of(currentOgg + " " + tipologiaInail));
//        } else {
//            auditLogRequest.setOggOper(JsonNullable.of(tipologiaInail));
//        }
////        auditLogRequest.setOperazione("import excel");
////        auditLogRequest.setOggOper(JsonNullable.of(auditLogRequest.getOggOper() + " " + tipologiaInail));
////        auditLogRequest.setKeyOper(null);
//        auditLogRequest.setResponsePayload(null);
//        auditLogRequest.setEsitoChiamata(200);
//
//        Map<String, Object> auditPayload = new HashMap<>();
//        auditPayload.put("fileName", salvato.fileName());
//        auditPayload.put("filePath", salvato.filePath());
//        auditPayload.put("checksum", salvato.checksum());
//        auditPayload.put("contentType", file.getContentType());
//        auditPayload.put("size", file.getSize());
//        auditPayload.put("fileId", fileId);
//        auditPayload.put("tipologiaInail", tipologiaInail);
//
//        auditLogRequest.setRequestPayload(JsonNullable.of(auditPayloadMapper.toBytes(auditPayload)));
//
//        auditService.salvaAudit(auditLogRequest);
//
//        try (InputStream is = new FileInputStream(salvato.filePath())) {
//            List<InailDTO> rows = ExcelParser.parseInail(is);
//
//            Set<String> giaProcessati = inailRepository.getProcessedCodiciFiscali(tipologiaInail);
//
//            List<InailDTO> nuovi = rows.stream()
//                    .filter(r -> r.getCodiceFiscale() != null && !r.getCodiceFiscale().isBlank()
//                              && !giaProcessati.contains(r.getCodiceFiscale()))
//                    .toList();
//
//            System.out.println("[INAIL] giaProcessati=" + giaProcessati.size() + " nuovi=" + nuovi.size() + " fileId=" + fileId);
//
//            if (!nuovi.isEmpty()) {
//                inailRepository.scaricoDatiExcelInTRegistroInail(nuovi, tipologiaInail, fileId, auditLogRequest.getUtente());
//            } else {
//                elaborazioneRepository.aggiornaFile(dsl, fileId, 4, auditLogRequest.getUtente());
//            }
//        } catch (Exception e) {
//            try { elaborazioneRepository.aggiornaFile(dsl, fileId, 4, auditLogRequest.getUtente()); } catch (Exception ignored) {}
//            throw new IOException("Errore elaborazione INAIL", e);
//        }
//
//        return salvato;
//    }
    @Override
    public FileSalvato importInail(MultipartFile file, String tipologiaInail, AuditLogRequest auditLogRequest) throws IOException {

        FileSalvato salvato = ExcelFileUtils.salvaExcelFileSystem(file, baseDir);

        Integer fileId = elaborazioneRepository.inserisciFile(
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

        String currentOgg = unwrap(auditLogRequest.getOggOper());

        if (currentOgg != null && !currentOgg.isBlank()) {
            auditLogRequest.setOggOper(JsonNullable.of(currentOgg + " " + tipologiaInail));
        } else {
            auditLogRequest.setOggOper(JsonNullable.of(tipologiaInail));
        }

        auditLogRequest.setResponsePayload(null);
        auditLogRequest.setEsitoChiamata(200);

        Map<String, Object> auditPayload = new HashMap<>();
        auditPayload.put("fileName", salvato.fileName());
        auditPayload.put("filePath", salvato.filePath());
        auditPayload.put("checksum", salvato.checksum());
        auditPayload.put("contentType", file.getContentType());
        auditPayload.put("size", file.getSize());
        auditPayload.put("fileId", fileId);
        auditPayload.put("tipologiaInail", tipologiaInail);

        auditLogRequest.setRequestPayload(JsonNullable.of(auditPayloadMapper.toBytes(auditPayload)));

        auditService.salvaAudit(auditLogRequest);

        try (InputStream is = new FileInputStream(salvato.filePath())) {

            List<InailDTO> rows = ExcelParser.parseInail(is);
            Set<String> giaProcessati = inailRepository.getProcessedCodiciFiscali(tipologiaInail);

            String utenteCreazione = auditLogRequest.getUtente() != null ? auditLogRequest.getUtente() : "ADMIN";

            List<InailDTO> nuovi = new ArrayList<>();

            if (rows != null) {
                for (InailDTO dto : rows) {
                    try {
                        String cf = validaCodiceFiscalePerImport(dto.getCodiceFiscale());

                        if (giaProcessati.contains(cf)) {
                        	elaborazioneRepository.inserisciScarto(
                        			dsl,
                                    fileId,
                                    "ERR_ELABORAZIONE",
                                    "Codice Fiscale gia' presente nel registro inail",
                                    null,
                                    utenteCreazione,
                                    dto.getCognome(),
                                    dto.getNome(),
                                    dto.getDataNascita(),
                                    dto.getSesso(),
                                    dto.getCodiceFiscale(),
                                    "REEA_T_REGISTRO_INAIL",
                                    dto.getFileId() != null ? dto.getFileId() : 0
                            );
                        }
                        else {
                        	dto.setCodiceFiscale(cf);
                        	nuovi.add(dto);
                        }

                    } catch (Exception e) {
                        String descrizioneErrore = ErroreImportUtility.buildErroreDescrizione(e);

                        try {
                        	elaborazioneRepository.inserisciScarto(
                        			dsl,
                                    fileId,
                                    "ERR_ELABORAZIONE",
                                    descrizioneErrore,
                                    null,
                                    utenteCreazione,
                                    dto.getCognome(),
                                    dto.getNome(),
                                    dto.getDataNascita(),
                                    dto.getSesso(),
                                    dto.getCodiceFiscale(),
                                    "REEA_T_REGISTRO_INAIL",
                                    dto.getFileId() != null ? dto.getFileId() : 0
                            );
                        } catch (Exception ex) {
                        	System.out.println("Errore inserendo scarto per CF: " + dto.getCodiceFiscale() + " Errore: " + ex);
                        }
                    }
                }
            }

            System.out.println("[INAIL] giaProcessati=" + giaProcessati.size() + " nuovi=" + nuovi.size() + " fileId=" + fileId);

            if (!nuovi.isEmpty()) {
                inailRepository.scaricoDatiExcelInTRegistroInail(
                        nuovi,
                        tipologiaInail,
                        fileId,
                        utenteCreazione
                );
            } else {
                elaborazioneRepository.aggiornaFile(dsl, fileId, 4, utenteCreazione);
            }

        } catch (Exception e) {
            try {
                elaborazioneRepository.aggiornaFile(dsl, fileId, 4, auditLogRequest.getUtente());
            } catch (Exception ignored) {
            }
            throw new IOException("Errore elaborazione INAIL", e);
        }
        
        
        // DE ORA POSSO FINALMENTE CHIUDERE LA T_FILE PERCHE' HO SAVATO TUTTE LE RIGHE CHE POTEVO
        tracciaElaborazioneRepository.aggiornaFineValidita(dsl, fileId);
        return salvato;
    }
    
    private String validaCodiceFiscalePerImport(String codiceFiscale) {
        if (codiceFiscale == null) {
            throw new IllegalArgumentException("Codice fiscale nullo");
        }

        if (!codiceFiscale.equals(codiceFiscale.trim())) {
            throw new IllegalArgumentException("Codice fiscale con spazi iniziali o finali");
        }

        if (codiceFiscale.contains(" ") || codiceFiscale.contains("\t")) {
            throw new IllegalArgumentException("Codice fiscale con spazi o tab non ammessi");
        }

        String cf = codiceFiscale.trim().toUpperCase();

        if (!cf.matches("^[A-Z0-9]{16}$")) {
            throw new IllegalArgumentException("Codice fiscale con caratteri non validi o lunghezza diversa da 16");
        }

        return cf;
    }
    
    
    private String unwrap(JsonNullable<String> jsonNullable) {
        if (jsonNullable == null || !jsonNullable.isPresent()) {
            return null;
        }
        return jsonNullable.get();
    }
    
    
//    @Override
//    public FileSalvato importInail(MultipartFile file, String tipologiaInail) throws IOException {
//
//        // 1. Salva subito il file su filesystem
//        FileSalvato salvato = ExcelFileUtils.salvaExcelFileSystem(file, baseDir);
//
//        // 2. Inserisci il record su reea_t_file
//        Integer fileId = elaborazioneRepository.inserisciFile(
//        	dsl,
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
//            inailRepository.deleteRecordNonProcessati(tipologiaInail);
//
//            List<InailDTO> rows = ExcelParser.parseInail(is);
//
//            Set<String> giaProcessati = inailRepository.getProcessedCodiciFiscali(tipologiaInail);
//
//            List<InailDTO> nuovi = rows.stream()
//                    .filter(r -> r.getCodiceFiscale() == null || !giaProcessati.contains(r.getCodiceFiscale()))
//                    .toList();
//
//            if (!nuovi.isEmpty()) {
//                inailRepository.scaricoDatiExcelInTRegistroInail(nuovi, tipologiaInail, fileId);
//            }
//        }
//
//        return salvato;
//    }

//    @Override
//    public FileSalvato importInail(MultipartFile file, String tipologiaInail) throws IOException {
//        try (InputStream is = file.getInputStream()) {
//            // 1. Cancella record non ancora processati (soggetto_id null)
//            inailRepository.deleteRecordNonProcessati(tipologiaInail);
//            // 2. Parsa il file
//            List<InailDTO> rows = ExcelParser.parseInail(is);
//            // 3. Escludi record gia'� importati (soggetto_id valorizzato)
//            Set<String> giaProcessati = inailRepository.getProcessedCodiciFiscali(tipologiaInail);
//            List<InailDTO> nuovi = rows.stream()
//                    .filter(r -> r.getCodiceFiscale() == null || !giaProcessati.contains(r.getCodiceFiscale()))
//                    .toList();
//            // 4. Inserisci solo i record nuovi
//            if (!nuovi.isEmpty()) {
//                inailRepository.scaricoDatiExcelInTRegistroInail(nuovi, tipologiaInail);
//            }
////            ExcelFileUtils.salvaExcelFileSystem(file, baseDir);
//            FileSalvato salvato = ExcelFileUtils.salvaExcelFileSystem(file, baseDir);
//            Integer fileId =  elaborazioneService.inserisciFile(
////            Integer fileId = elaborazioneRepository.inserisciFile(
//                salvato.fileName(),              // reea_t_file.file_name
//                salvato.filePath(),              // reea_t_file.file_path
//                salvato.checksum(),
//                file.getContentType(),           // reea_t_file.file_mime_type
//                file.getSize(),                  // reea_t_file.file_dimensione_bytes
//                1,                               // file_stato_id (es. 1 = caricato)
//                "ADMIN"                          // utente_creazione/modifica
//            );
//            return salvato;
//        }
//    }


    @Override
	public List<InailDTO> getRecordTabInail(String tipologiaInail, Integer fileId) throws IOException {
//     //DE RIPULISCI LA TABALLA DAGLI SCARTI PRECEDENTI
//     System.out.println("cancello le righe spurie....");
//    inailRepository.deleteRecordNonProcessati(tipologiaInail);
    List<InailDTO> listaInail = inailRepository.listaRecordInail(tipologiaInail, fileId);
		return listaInail;
	}
    
    
    @Override
	public void chiudiTuttiRecordTabRegistroInailByFile(Integer fileId) {
    	inailRepository.chiudiTuttiRecordTabRegistroInailByFile(fileId);
	}

}

