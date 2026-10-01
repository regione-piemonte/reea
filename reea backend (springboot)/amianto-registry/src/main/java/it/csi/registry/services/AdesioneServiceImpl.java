package it.csi.registry.services;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jooq.DSLContext;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.csi.registry.model.AdesioneDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.ExportDTO;
import it.csi.registry.repositories.AdesioneRepository;
import it.csi.registry.repositories.TracciaElaborazioneRepository;
import it.csi.registry.util.ExcelFileUtils;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;
import it.csi.registry.util.ExcelParser;
import it.csi.registry.util.NumberUtils;

@Service
public class AdesioneServiceImpl implements AdesioneService {

	private final DSLContext dsl;
    private final AdesioneRepository adesioneRepository;
    private final TracciaElaborazioneRepository elaborazioneRepository;
    private final AuditService auditService;
    private final AuditPayloadMapper auditPayloadMapper;
    private final TracciaElaborazioneService tracciaElaborazioneService;
    private final TracciaElaborazioneRepository tracciaElaborazioneRepository;
    
    @Value("${preadesione.upload.dir}")
    private String baseDir;

    public AdesioneServiceImpl(DSLContext dsl, AdesioneRepository adesioneRepository, TracciaElaborazioneRepository elaborazioneRepository,
    		AuditService auditService, AuditPayloadMapper auditPayloadMapper,
    		TracciaElaborazioneService tracciaElaborazioneService, TracciaElaborazioneRepository tracciaElaborazioneRepository) {
    	this.dsl = dsl;
    	this.adesioneRepository = adesioneRepository;
        this.elaborazioneRepository = elaborazioneRepository;
        this.auditService = auditService;
        this.auditPayloadMapper = auditPayloadMapper;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
		this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
    }
    
    
    @Override
    public FileSalvato importAdesioni(MultipartFile file, AuditLogRequest auditLogRequest) throws IOException {

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

//        auditLogRequest.setOperazione("import excel");
//        auditLogRequest.setOggOper(JsonNullable.of("ADESIONI"));
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

        
        // DE VERIFICARE CON CHRISTIAN MA QUESTA PARTE LA DOVRA' FARE IL BATCH perch� se sono molte righe potrebbe andare in timeout
    
        try (InputStream is = new FileInputStream(salvato.filePath())) {
//            adesioneRepository.deleteRecordNonProcessati();

            List<AdesioneDTO> rows = ExcelParser.parseAdesioni(is);
            Set<String> giaProcessati = adesioneRepository.getProcessedAdesioneCods();

            List<AdesioneDTO> nuovi = rows.stream()
                    .filter(r -> r.getAdesioneCod() == null || !giaProcessati.contains(r.getAdesioneCod()))
                    .toList();

            List<AdesioneDTO> duplicati = rows.stream()
                    .filter(r -> r.getAdesioneCod() != null && giaProcessati.contains(r.getAdesioneCod()))
                    .toList();

            for (AdesioneDTO dup : duplicati) {
                tracciaElaborazioneService.inserisciScarto(
                    fileId,
                    "DUPLICATO",
                    "Record già presente nel sistema (codice adesione: " + dup.getAdesioneCod() + ")",
                    dup.toString(),
                    auditLogRequest.getUtente(),
                    dup.getCognome(),
                    dup.getNome(),
                    dup.getNascitaData(),
                    dup.getSesso(),
                    dup.getCodiceFiscale(),
                    "REEA_T_ADESIONE",
                    null
                );
            }

            if (!nuovi.isEmpty()) {
                adesioneRepository.scaricoDatiExcelInTAdesione(nuovi, fileId, auditLogRequest.getUtente());
            }
            else
            	tracciaElaborazioneService.aggiornaFile(fileId, 4, auditLogRequest.getUtente());
        }
        // DE ORA POSSO FINALMENTE CHIUDERE LA T_FILE PERCHE' HO SAVATO TUTTE LE RIGHE CHE POTEVO
        tracciaElaborazioneRepository.aggiornaFineValidita(dsl, fileId);
        return salvato;
    }
//    @Override
//    public FileSalvato importAdesioni(MultipartFile file) throws IOException {
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
//            adesioneRepository.deleteRecordNonProcessati();
//
//            List<AdesioneDTO> rows = ExcelParser.parseAdesioni(is);
//            Set<String> giaProcessati = adesioneRepository.getProcessedAdesioneCods();
//
//            // 3. Escludi record gia'� importati (soggetto_id valorizzato)
//            List<AdesioneDTO> nuovi = rows.stream()
//                .filter(r -> r.getAdesioneCod() == null || !giaProcessati.contains(r.getAdesioneCod()))
//                .toList();
//
//            if (!nuovi.isEmpty()) {
//                adesioneRepository.scaricoDatiExcelInTAdesione(nuovi, fileId);
//            }
//        }
//
//        return salvato;
//    }

    @Override
    public List<AdesioneDTO> getAdesioneBySoggettoId(Long soggettoId) {
        return adesioneRepository.findBySoggettoId(soggettoId);
    }



    @Override
	public List<AdesioneDTO> getRecordTabAdesioni(Integer fileId) throws IOException {
		List<AdesioneDTO> listaAdesioni = adesioneRepository.listaRecordAdesioni(fileId);
		return listaAdesioni;
	}
    
    
    @Override
//    @Transactional(readOnly = true)
    public byte[] scaricaDatiExcelPreadesioniFiltrate(List<Long> soggettoIds, Integer assistenzaAslId, AuditLogRequest auditLogRequest) {
        if (soggettoIds == null || soggettoIds.isEmpty()) {
            throw new IllegalArgumentException("La lista dei soggettoId non pu� essere vuota");
        }

        List<ExportDTO> righe =
                adesioneRepository.recuperaPreadesioniPerExport(soggettoIds, assistenzaAslId);
        
        return generaExcelPreadesioni(righe, auditLogRequest);
    }
    
    
    private byte[] generaExcelPreadesioni(List<ExportDTO> righe, AuditLogRequest auditLogRequest) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Preadesioni");

            String[] headers = {
//            		"Inail - Domanda",
//            		"Inail - Cognome",
//            		"Inail - Nome",
//            		"Inail - Codice Fiscale",
//            		"Inail - Sesso",
//            		"Inail - Data Nascita",
//            		"Inail - Indirizzo Residenza",
//            		"Inail - Istat Residenza",
//            		"Inail - Cap Residenza",
//            		"Inail - Regione Residenza",
//            		"Inail - Provincia Residenza",
//            		"Inail - Comune Residenza",
                    "Pre - Codice Adesione",
                    "Pre - Data Adesione",
                    "Pre - Codice Fiscale",
                    "Pre - Cognome",
                    "Pre - Nome",
                    "Pre - Data di nascita",
                    "Pre - Provincia di nascita",
                    "Pre - Comune di nascita",
                    "Pre - Tessera Team",
                    "Pre - Id Aura",
                    "Pre - Provincia di domicilio",
                    "Pre - Comune di domicilio",
                    "Pre - Codice comune ISTAT di domicilio",
                    "Pre - CAP di domicilio",
                    "Pre - Email",
                    "Pre - Telefono",
                    "Pre - Codice ASL di domicilio",
                    "Pre - ASL di domicilio",
                    "Pre - Codazi",
                    "Pre - Codice ASL di residenza",
                    "Pre - ASL di residenza",
                    "Pre - Data inizio esposizione",
                    "Pre - Data fine esposizione",
                    "Pre - Azienda",
                    "Pre - Codice comune ISTAT azienda",
                    "Pre - Comune azienda",
                    "Pre - CAP azienda",
                    "Pre - Provincia azienda",
                    "Pre - Mansione",
                    "Aura - Codice Fiscale",
                    "Aura - Domicilio ASL",
                    "Aura - Residenza ASL",
                    "Aura - Assistenza ASL",
                    "Aura - Nome",
                    "Aura - Cognome",
                    "Aura - Sesso",
                    "Aura - Data nascita",
                    "Aura - Comune nascita cod",
                    "Aura - Comune nascita desc",
                    "Aura - Provincia nascita cod",
                    "Aura - Provincia nascita desc",
                    "Aura - Stato nascita cod",
                    "Aura - Stato nascita desc",
                    "Aura - Cittadinanza stato cod",
                    "Aura - Cittadinanza stato desc",
                    "Aura - Domicilio comune cod",
                    "Aura - Domicilio comune desc",
                    "Aura - Domicilio provincia cod",
                    "Aura - Domicilio provincia desc",
                    "Aura - Domicilio stato cod",
                    "Aura - Domicilio stato desc",
                    "Aura - Domicilio CAP",
                    "Aura - Domicilio indirizzo",
                    "Aura - Domicilio numero civico",
                    "Aura - Residenza comune cod",
                    "Aura - Residenza comune desc",
                    "Aura - Residenza provincia cod",
                    "Aura - Residenza provincia desc",
                    "Aura - Residenza stato cod",
                    "Aura - Residenza stato desc",
                    "Aura - Residenza CAP",
                    "Aura - Residenza indirizzo",
                    "Aura - Residenza numero civico",
                    "Aura - Tessera team",
                    "Aura - Id Aura",
                    "Aura - Email Aura",
                    "Aura - Telefono Aura",
                    "Aura - Data decesso",
                    "Aura - Assistenza ASL fine",
                    "Aura - Codice esenzione",
                    "Aura - Descrizione esenzione",
                    "Aura - Codice diagnosi",
                    "Aura - Descrizione diagnosi",
                    "Aura - Data emissione esenzione",
                    "Aura - Data scadenza esenzione",
                    "Email soggetto",
                    "Telefono soggetto",
                    "Anam - Codice Fiscale",
                    "Anam - Id Aura",
                    "Anam - Data intervista",
                    "Anam - Nominativo intervistatore",
                    "Anam - Fumatore",
                    "Anam - Sigarette",
                    "Anam - Sigarette anni",
                    "Anam - Sigarette et� inizio",
                    "Anam - Sigarette fuma attualmente",
                    "Anam - Sigarette et� fine",
                    "Anam - Sigarette die",
                    "Anam - Sigari",
                    "Anam - Sigari anni",
                    "Anam - Sigari et� inizio",
                    "Anam - Sigari fuma attualmente",
                    "Anam - Sigari et� fine",
                    "Anam - Sigari die",
                    "Anam - Pipa",
                    "Anam - Pipa anni",
                    "Anam - Pipa et� inizio",
                    "Anam - Pipa fuma attualmente",
                    "Anam - Pipa et� fine",
                    "Anam - Pipa die",
                    "Anam - Occupazione num",
                    "Anam - Occupazione anno inizio",
                    "Anam - Occupazione anno fine",
                    "Anam - Occupazione tipo",
                    "Anam - Occupazione descrizione lavoro",
                    "Anam - Occupazione nome e indirizzo ditta",
                    "Anam - Occupazione attivit� ditta",
                    "Anam - Nota attivit� con amianto",
                    "Anam - Anamnesi esposizione amianto",
                    "Anam - Esposizione professionale",
                    "Anam - Anno fine esposizione",
                    "Anam - Livello esposizione",
                    "Anam - Inserimento in sorveglianza",
                    "Anam - Id SPRESAL",
                    "Anam - Counseling",
                    "Anam - Occupazione Esposizione CRPT",
                    "Anam - Occupazione Settore Ditta CRPT",
                    "Anam - Occupazione Mansione CRPT",
                    "Anam - Occupazione Ragione Sociale Ditta CRPT",
                    "Anam - Occupazione Piva Ditta CRPT",
                    "Anam - Occupazione Codice Fiscale Ditta CRPT",
                    "Esiti - Codice Fiscale",
                    "Esiti - Id Aura",
                    "Esiti - Data visita",
                    "Esiti - Visita",
                    "Esiti - Livello visita",
                    "Esiti - Riceve indennizzo",
                    "Esiti - Malattia indennizzo",
                    "Esiti - Accertamenti RX",
                    "Esiti - Accertamenti RX data",
                    "Esiti - Accertamenti RX referto normale",
                    "Esiti - Accertamenti RX acquisita",
                    "Esiti - Accertamenti TC",
                    "Esiti - Accertamenti TC data",
                    "Esiti - Accertamenti TC referto normale",
                    "Esiti - Accertamenti TC acquisita",
                    "Esiti - Accertamenti spirometria semplice",
                    "Esiti - Accertamenti spirometria semplice data",
                    "Esiti - Accertamenti spirometria semplice referto normale",
                    "Esiti - Accertamenti spirometria semplice acquisita",
                    "Esiti - Accertamenti spirometria globale",
                    "Esiti - Accertamenti spirometria globale data",
                    "Esiti - Accertamenti spirometria globale referto normale",
                    "Esiti - Accertamenti spirometria globale acquisita",
                    "Esiti - Accertamenti DLCO",
                    "Esiti - Accertamenti DLCO data",
                    "Esiti - Accertamenti DLCO referto normale",
                    "Esiti - Accertamenti DLCO acquisita",
                    "Esiti - Accertamenti PET",
                    "Esiti - Accertamenti PET data",
                    "Esiti - Accertamenti PET referto normale",
                    "Esiti - Accertamenti PET acquisita",
                    "Esiti - Accertamenti visita pneumologica",
                    "Esiti - Accertamenti visita pneumologica data",
                    "Esiti - Accertamenti visita pneumologica referto",
                    "Esiti - Accertamenti visita pneumologica acquisita",
                    "Esiti - Accertamenti visita radiologica",
                    "Esiti - Accertamenti visita radiologica data",
                    "Esiti - Accertamenti visita radiologica referto",
                    "Esiti - Accertamenti visita radiologica acquisita",
                    "Esiti - Accertamenti visita oncologica",
                    "Esiti - Accertamenti visita oncologica data",
                    "Esiti - Accertamenti visita oncologica referto",
                    "Esiti - Accertamenti visita oncologica acquisita",
                    "Esiti - Accertamenti altro",
                    "Esiti - Accertamenti altro descrizione",
                    "Esiti - Accertamenti altro data",
                    "Esiti - Accertamenti altro referto normale",
                    "Esiti - Accertamenti altro referto acquisita",
                    "Esiti - Risultato negativo",
                    "Esiti - PPM placche pleuriche monolaterali",
                    "Esiti - PPM primo certificato e denuncia",
                    "Esiti - PPM primo certificato e denuncia data",
                    "Esiti - PPM aggravamento e denuncia",
                    "Esiti - PPM aggravamento e denuncia data",
                    "Esiti - PPM percentuale di riconoscimento",
                    "Esiti - PPM referto",
                    "Esiti - PPM referto data",
                    "Esiti - PPB placche pleuriche bilaterali",
                    "Esiti - PPB primo certificato e denuncia",
                    "Esiti - PPB primo certificato e denuncia data",
                    "Esiti - PPB aggravamento e denuncia",
                    "Esiti - PPB aggravamento e denuncia data",
                    "Esiti - PPB percentuale di riconoscimento",
                    "Esiti - PPB referto",
                    "Esiti - PPB referto data",
                    "Esiti - AP asbestosi polmonare",
                    "Esiti - AP primo certificato e denuncia",
                    "Esiti - AP primo certificato e denuncia data",
                    "Esiti - AP aggravamento e denuncia",
                    "Esiti - AP aggravamento e denuncia data",
                    "Esiti - AP percentuale di riconoscimento",
                    "Esiti - AP referto",
                    "Esiti - AP referto data",
                    "Esiti - FPD fibrosi pleurica diffusa",
                    "Esiti - FPD primo certificato e denuncia",
                    "Esiti - FPD primo certificato e denuncia data",
                    "Esiti - FPD aggravamento e denuncia",
                    "Esiti - FPD aggravamento e denuncia data",
                    "Esiti - FPD percentuale di riconoscimento",
                    "Esiti - FPD referto",
                    "Esiti - FPD referto data",
                    "Esiti - MP mesotelioma pleurico",
                    "Esiti - MP primo certificato e denuncia",
                    "Esiti - MP primo certificato e denuncia data",
                    "Esiti - MP aggravamento e denuncia",
                    "Esiti - MP aggravamento e denuncia data",
                    "Esiti - MP percentuale di riconoscimento",
                    "Esiti - MP referto",
                    "Esiti - MP referto data",
                    "Esiti - MP comunicazione al COR",
                    "Esiti - MP comunicazione al COR data",
                    "Esiti - AM altro mesotelioma",
                    "Esiti - AM primo certificato e denuncia",
                    "Esiti - AM primo certificato e denuncia data",
                    "Esiti - AM aggravamento e denuncia",
                    "Esiti - AM aggravamento e denuncia data",
                    "Esiti - AM percentuale di riconoscimento",
                    "Esiti - AM referto",
                    "Esiti - AM referto data",
                    "Esiti - AM comunicazione al COR",
                    "Esiti - AM comunicazione al COR data",
                    "Esiti - NL neoplasia laringe",
                    "Esiti - NL primo certificato e denuncia",
                    "Esiti - NL primo certificato e denuncia data",
                    "Esiti - NL aggravamento e denuncia",
                    "Esiti - NL aggravamento e denuncia data",
                    "Esiti - NL percentuale di riconoscimento",
                    "Esiti - NL referto",
                    "Esiti - NL referto data",
                    "Esiti - NO neoplasia ovarica",
                    "Esiti - NO primo certificato e denuncia",
                    "Esiti - NO primo certificato e denuncia data",
                    "Esiti - NO aggravamento e denuncia",
                    "Esiti - NO aggravamento e denuncia data",
                    "Esiti - NO percentuale di riconoscimento",
                    "Esiti - NO referto",
                    "Esiti - NO referto data",
                    "Esiti - TP tumore del polmone",
                    "Esiti - TP primo certificato e denuncia",
                    "Esiti - TP primo certificato e denuncia data",
                    "Esiti - TP aggravamento e denuncia",
                    "Esiti - TP aggravamento e denuncia data",
                    "Esiti - TP percentuale di riconoscimento",
                    "Esiti - TP referto",
                    "Esiti - TP referto data",
                    "Esiti - TP comunicazione al COR",
                    "Esiti - TP comunicazione al COR data",
                    "Esiti - BPCO enfisema polmonare",
                    "Esiti - BPCO primo certificato e denuncia",
                    "Esiti - BPCO primo certificato e denuncia data",
                    "Esiti - BPCO aggravamento e denuncia",
                    "Esiti - BPCO aggravamento e denuncia data",
                    "Esiti - BPCO percentuale di riconoscimento",
                    "Esiti - BPCO referto",
                    "Esiti - BPCO referto data",
                    "Esiti - Altra diagnosi",
                    "Esiti - Altra diagnosi descrizione",
                    "Esiti - Altra primo certificato e denuncia",
                    "Esiti - Altra primo certificato e denuncia data",
                    "Esiti - Altra aggravamento e denuncia",
                    "Esiti - Altra aggravamento e denuncia data",
                    "Esiti - Altra percentuale di riconoscimento",
                    "Esiti - Altra referto",
                    "Esiti - Altra referto data",
                    "Esiti - Follow up previsto",
                    "Esiti - Anno presunto prossima visita",
                    "Esiti - Anno ultima visita",
                    "Esiti - Invio sintesi a MMG",
                    "Esiti - Id SPRESAL",
//                    "Npla - Codice Fiscale",
//                    "Npla - Periodo",
//                    "Npla - Id Cantiere",
//                    "Npla - Azienda Piva",
//                    "Npla - Azienda Nome",
//                    "Npla - Asl Cantiere",
//                    "Npla - Comune Cantiere",
//                    "Npla - Anno",
//                    "Npla - Tipologia Paino",
//                    "Npla - Quantita da Rimuovere",
//                    "Npla - Quantita Rimossa",
                    "Att. Sanitaria Stato",
                    "Att. Sanitaria Spresal",
                    "Att. Sanitaria Spresal Data",
                    "Att. Sanitaria Inail",
                    "Sezione",
                    "Inserito Sorveglianza",
                    "Tipo Elenco Inail",
                    "Stato Soggetto"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (ExportDTO dto : righe) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;

                //Inail
//                row.createCell(col++).setCellValue(nvl(dto.getInailDomanda()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailCognome()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailNome()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailCodiceFiscale()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailSesso()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailDataNascita()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailIndirizzoResidenza()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailIstatResidenza()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailCapResidenza()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailRegioneResidenza()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailProvinciaResidenza()));
//                row.createCell(col++).setCellValue(nvl(dto.getInailComuneResidenza()));
                
                // campi PRE da REEA_T_ADESIONE
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCodiceAdesione()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneDataAdesione()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCodiceFiscale()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCognome()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneNome()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneDataDiNascita()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneProvinciaDiNascita()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneComuneDiNascita()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneTesseraTeam()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneIdAura()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneProvinciaDiDomicilio()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneComuneDiDomicilio()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCodiceComuneIstatDiDomicilio()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCapDiDomicilio()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneEmail()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneTelefono()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCodiceAslDiDomicilio()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneAslDiDomicilio()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCodazi()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCodiceAslDiResidenza()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneAslDiResidenza()));
//                row.createCell(col++).setCellValue(nvl(dto.getAdesioneDataInizioEsposizione()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneDataInizioEsposizione()) != null && !(nvl(dto.getAdesioneDataInizioEsposizione()).trim().isEmpty()) ? nvl(dto.getAdesioneDataInizioEsposizione()) : nvl(dto.getAdesioneEsposizioneDataInizioEsposizione()));
//                row.createCell(col++).setCellValue(nvl(dto.getAdesioneDataFineEsposizione()));
                row.createCell(col++).setCellValue(nvl(dto.getAdesioneDataFineEsposizione()) != null && !(nvl(dto.getAdesioneDataFineEsposizione()).trim().isEmpty()) ? nvl(dto.getAdesioneDataFineEsposizione()) : nvl(dto.getAdesioneEsposizioneDataFineEsposizione()));
//                row.createCell(col++).setCellValue(nvl(dto.getAdesioneAzienda()));
                row.createCell(col++).setCellValue(StringUtils.isNotBlank(dto.getAdesioneAzienda()) ? dto.getAdesioneAzienda() : nvl(dto.getAdesioneEsposizioneAzienda()));
//                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCodiceComuneIstatAzienda()));
                row.createCell(col++).setCellValue(StringUtils.isNotBlank(dto.getAdesioneCodiceComuneIstatAzienda()) ? nvl(dto.getAdesioneCodiceComuneIstatAzienda()) : nvl(dto.getAdesioneEsposizioneAziendaComuneCod()));
//                row.createCell(col++).setCellValue(nvl(dto.getAdesioneComuneAzienda()));
                row.createCell(col++).setCellValue(StringUtils.isNotBlank(dto.getAdesioneComuneAzienda()) ? nvl(dto.getAdesioneComuneAzienda()) : nvl(dto.getAdesioneEsposizioneAziendaComuneDesc()));
//                row.createCell(col++).setCellValue(nvl(dto.getAdesioneCapAzienda()));
                row.createCell(col++).setCellValue(StringUtils.isNotBlank(dto.getAdesioneCapAzienda()) ? nvl(dto.getAdesioneCapAzienda()) : nvl(dto.getAdesioneEsposizioneAziendaCap()));
//                row.createCell(col++).setCellValue(nvl(dto.getAdesioneProvinciaAzienda()));
                row.createCell(col++).setCellValue(StringUtils.isNotBlank(dto.getAdesioneProvinciaAzienda()) ? nvl(dto.getAdesioneProvinciaAzienda()) : nvl(dto.getAdesioneEsposizioneProvinciaAzienda()));
//                row.createCell(col++).setCellValue(nvl(dto.getAdesioneMansione()));
                row.createCell(col++).setCellValue(StringUtils.isNotBlank(dto.getAdesioneMansione()) ? nvl(dto.getAdesioneMansione()) : nvl(dto.getAdesioneEsposizioneMansione()));

                // campi AURA da REEA_T_SOGGETTO
                row.createCell(col++).setCellValue(nvl(dto.getAuraCodiceFiscale()));
//                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioAsl()));
//                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaAsl()));
//                row.createCell(col++).setCellValue(nvl(dto.getAuraAssistenzaAsl()));
                row.createCell(col++).setCellValue(nvl(dto.getAslAziendaDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAslAziendaDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAslAziendaDesc()));
                
                
                row.createCell(col++).setCellValue(nvl(dto.getAuraNome()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraCognome()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraSesso()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraNascitaData()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraNascitaComuneCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraNascitaComuneDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraNascitaProvinciaCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraNascitaProvinciaDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraNascitaStatoCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraNascitaStatoDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraCittadinanzaStatoCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraCittadinanzaStatoDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioComuneCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioComuneDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioProvinciaCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioProvinciaDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioStatoCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioStatoDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioCap()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioIndirizzo()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDomicilioNumeroCivico()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaComuneCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaComuneDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaProvinciaCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaProvinciaDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaStatoCod()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaStatoDesc()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaCap()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaIndirizzo()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraResidenzaNumeroCivico()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraTesseraTeam()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraIdAura()));
//                row.createCell(col++).setCellValue(nvl(dto.getAuraEmailAura()));
                row.createCell(col++).setCellValue(StringUtils.isNotBlank(dto.getAuraEmailAura()) ? dto.getAuraEmailAura() : nvl(dto.getEmail()));
//                row.createCell(col++).setCellValue(nvl(dto.getAuraTelefonoAura()));
                row.createCell(col++).setCellValue(StringUtils.isNotBlank(dto.getAuraTelefonoAura()) ? dto.getAuraTelefonoAura() : nvl(dto.getTelefono()));
                row.createCell(col++).setCellValue(nvl(dto.getAuraDataDecesso()));
                row.createCell(col++).setCellValue(nvl(formatDataConSentinella(dto.getAuraAssistenzaAslFine())));
//              row.createCell(col++).setCellValue(nvl(dto.getAuraAssistenzaAslFine()));

                // campi esenzione
                row.createCell(col++).setCellValue(nvl(dto.getCodEsenzione()));
                row.createCell(col++).setCellValue(nvl(dto.getDescEsenzione()));
                row.createCell(col++).setCellValue(nvl(dto.getCodDiagnosi()));
                row.createCell(col++).setCellValue(nvl(dto.getDescDiagnosi()));
                row.createCell(col++).setCellValue(nvl(dto.getDataEmissione()));
                row.createCell(col++).setCellValue(nvl(dto.getDataScadenza()));
                
                //
                row.createCell(col++).setCellValue(nvl(dto.getEmail()));
                row.createCell(col++).setCellValue(nvl(dto.getTelefono()));

                // campi ANAM da REEA_REG_SPRESAL_ANAMNESI
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiCodiceFiscale()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiIdAura()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiDataIntervista()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiNominativoIntervistatore()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiFumatore()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigarette()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigaretteAnni()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigaretteEtaInizio()));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiSigaretteAnni())));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiSigaretteEtaInizio())));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigaretteFumaAttualmente()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigaretteEtaFine()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigaretteDie()));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiSigaretteEtaFine())));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiSigaretteDie())));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigari()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigariAnni()));
                // NUOVO
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiSigariAnni())));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigariEtaInizio()));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiSigariEtaInizio())));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigariFumaAttualmente()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigariEtaFine()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiSigariDie()));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiSigariEtaFine())));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiSigariDie())));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiPipa()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiPipaAnni()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiPipaEtaInizio()));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiPipaAnni())));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiPipaEtaInizio())));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiPipaFumaAttualmente()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiPipaEtaFine()));
//                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiPipaDie()));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiPipaEtaFine())));
                row.createCell(col++).setCellValue(nvl(NumberUtils.formatBigDecimal(dto.getAnamnesiPipaDie())));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneNum()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneAnnoInizio()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneAnnoFine()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneTipo()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneDescrizioneLavoro()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneNomeEIndirizzoDitta()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneAttivitaDitta()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiNotaAttivitaConAmianto()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiAnamnesiEsposizioneAmianto()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiEsposizioneProfessionale()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiAnnoFineEsposizione()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiLivelloEsposizione()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiInserimentoInSorveglianza()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiIdSpresal()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiCounseling()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneEsposizioneCrpt()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneSettoreDittaCrpt()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneMansioneCrpt()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneRagioneSocialeDittaCrpt()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazionePivaDittaCrpt()));
                row.createCell(col++).setCellValue(nvl(dto.getAnamnesiOccupazioneCodiceFiscaleDittaCrpt()));

                // campi ESITI da REEA_T_REGISTRO_SPRESAL_ESITI
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisCodiceFiscale()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisIdAura()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisDataVisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisVisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisLivelloVisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisRiceveIndennizzo()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMalattiaIndennizzo()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiRx()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiRxData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiRxRefertoNormale()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiRxAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiTc()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiTcData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiTcRefertoNormale()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiTcAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiSpirometriaSemplice()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiSpirometriaSempliceData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiSpirometriaSempliceRefertoNormale()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiSpirometriaSempliceAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiSpirometriaGlobale()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiSpirometriaGlobaleData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiSpirometriaGlobaleRefertoNormale()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiSpirometriaGlobaleAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiDlco()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiDlcoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiDlcoRefertoNormale()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiDlcoAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiPet()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiPetData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiPetRefertoNormale()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiPetAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaPneumologica()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaPneumologicaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaPneumologicaReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaPneumologicaAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaRadiologica()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaRadiologicaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaRadiologicaReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaRadiologicaAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaOncologica()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaOncologicaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaOncologicaReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiVisitaOncologicaAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiAltro()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiAltroDescrizione()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiAltroData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiAltroRefertoNormale()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAccertamentiAltroRefertoAcquisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisRisultatoNegativo()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpmPlacchePleuricheMonolaterali()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpmPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpmPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpmAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpmAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpmPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpmReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpmRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpbPlacchePleuricheBilaterali()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpbPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpbPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpbAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpbAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpbPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpbReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisPpbRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisApAsbestosiPolmonare()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisApPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisApPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisApAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisApAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisApPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisApReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisApRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisFpdFibrosiPleuricaDiffusa()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisFpdPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisFpdPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisFpdAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisFpdAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisFpdPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisFpdReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisFpdRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpMesoteliomaPleurico()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpComunicazioneAlCor()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisMpComunicazioneAlCorData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmAltroMesotelioma()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmComunicazioneAlCor()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAmComunicazioneAlCorData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNlNeoplasiaLaringe()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNlPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNlPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNlAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNlAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNlPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNlReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNlRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNoNeoplasiaOvarica()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNoPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNoPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNoAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNoAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNoPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNoReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisNoRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpTumoreDelPolmone()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpComunicazioneAlCor()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisTpComunicazioneAlCorData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisBpcoEnfisemaPolmonare()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisBpcoPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisBpcoPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisBpcoAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisBpcoAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisBpcoPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisBpcoReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisBpcoRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAltraDiagnosi()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAltraDiagnosiDescrizione()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAltraPrimoCertificatoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAltraPrimoCertificatoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAltraAggravamentoEDenuncia()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAltraAggravamentoEDenunciaData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAltraPercentualeDiRiconoscimento()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAltraReferto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAltraRefertoData()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisFollowUpPrevisto()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAnnoPresuntoProssimaVisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisAnnoUltimaVisita()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisInvioSintesiAMmg()));
                row.createCell(col++).setCellValue(nvl(dto.getEsitiVisIdSpresal()));
                
                //Npla
//                row.createCell(col++).setCellValue(nvl(dto.getNplaCodiceFiscale()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaPeriodo()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaIdCantiere()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaAziendaPiva()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaAziendaNome()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaAslCantiere()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaComuneCantiere()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaAnno()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaTipologiaPiano()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaQuantitaDaRimuovere()));
//                row.createCell(col++).setCellValue(nvl(dto.getNplaQuantitaRimossa()));
                
                row.createCell(col++).setCellValue(nvl(dto.getRegAttSanitariaStato()));
                row.createCell(col++).setCellValue(nvl(dto.getRegAttSanitariaSpresal()));
                row.createCell(col++).setCellValue(nvl(dto.getRegAttSanitariaSpresalData()));
                row.createCell(col++).setCellValue(nvl(dto.getRegAttSanitariaInail()));
                row.createCell(col++).setCellValue(nvl(dto.getRegSezione()));
                row.createCell(col++).setCellValue(nvl(dto.getRegInseritoInSorveglianza()));
                row.createCell(col++).setCellValue(nvl(dto.getRegTipoElencoInail()));
                row.createCell(col++).setCellValue(nvl(dto.getSoggettoStatoDesc()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            
            //
            byte[] excelBytes = out.toByteArray();

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            String fileName = "export_preadesioni_" + timestamp + ".xlsx";
//            String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            
            if(auditLogRequest == null)
            	auditLogRequest = new AuditLogRequest();

            auditLogRequest.setOperazione("export");
            auditLogRequest.setOggOper(JsonNullable.of("excel preadesioni"));
            auditLogRequest.setKeyOper(null);
            auditLogRequest.setRequestPayload(null);
            auditLogRequest.setEsitoChiamata(200);

            Map<String, Object> auditPayload = new HashMap<>();
            auditPayload.put("fileName", fileName);
//            auditPayload.put("contentType", contentType);
            auditPayload.put("size", excelBytes.length);
            auditPayload.put("sheetName", sheet.getSheetName());
            auditPayload.put("recordCount", righe != null ? righe.size() : 0);

            auditLogRequest.setResponsePayload(
                    JsonNullable.of(auditPayloadMapper.toBytes(auditPayload))
            );

            auditService.salvaAudit(auditLogRequest);
            //
            return excelBytes;

        } catch (Exception e) {
            throw new IllegalStateException("Errore durante la generazione del file Excel", e);
        }
    }


    private String nvl(Object value) {
        return value == null ? "" : value.toString();
    }

    private String formatDataConSentinella(LocalDate data) {
        LocalDate sentinella = LocalDate.of(9999, 12, 31);

        if (data == null || sentinella.equals(data)) {
            return "";
        }

        return data.toString();
    }
}

