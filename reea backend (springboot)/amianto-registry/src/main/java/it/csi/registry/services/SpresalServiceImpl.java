package it.csi.registry.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import it.csi.registry.model.*;
import it.csi.registry.util.ExcelExportResult;
import org.apache.poi.xssf.usermodel.*;
import org.jooq.DSLContext;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import it.csi.registry.repositories.SpresalRepository;
import it.csi.registry.repositories.TracciaElaborazioneRepository;
import it.csi.registry.util.ExcelFileUtils;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;
import it.csi.registry.util.ExcelParser;

@Service
public class SpresalServiceImpl implements SpresalService {

	private final DSLContext dsl;
	private final SpresalRepository spresalRepository;
	private final TracciaElaborazioneRepository tracciaElaborazioneRepository;
    private final AuditService auditService;
    private final AuditPayloadMapper auditPayloadMapper;
	
    @Value("${spresalAnamnesi.upload.dir}")
    private String baseDirAnamnesi;
    @Value("${spresalEsiti.upload.dir}")
    private String baseDirEsiti;
    @Value("${spresal.export.base-dir}")
    private String baseDirExport;


    public SpresalServiceImpl(DSLContext dsl, SpresalRepository spresalRepository, 
    		TracciaElaborazioneRepository tracciaElaborazioneRepository,
    		AuditService auditService, AuditPayloadMapper auditPayloadMapper) {
        this.dsl = dsl;
    	this.spresalRepository = spresalRepository;
        this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
        this.auditService = auditService;
        this.auditPayloadMapper = auditPayloadMapper;
    }

    
    @Override
    public FileSalvato importSpresal(MultipartFile file, AuditLogRequest auditLogRequest) throws IOException {

        FileSalvato salvato = ExcelFileUtils.salvaExcelFileSystem(file, baseDirAnamnesi);

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

        auditLogRequest.setOperazione("import excel");
        auditLogRequest.setOggOper(JsonNullable.of("SPRESAL ANAMNESI"));
        auditLogRequest.setKeyOper(null);
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

            List<SpresalDTO> rows = ExcelParser.parseSpresal(is);

            Set<String> listaRecordRegistroIdIsNotNullAnamnesi = spresalRepository.getListaRecordRegistroIdIsNotNullSpresalAnamnesi();
            
            List<SpresalDTO> doppioni = rows == null ? List.of() : rows.stream()
                    .filter(r -> {
                        if (r.getCodiceFiscale() == null || r.getCodiceFiscale().isBlank()) {
                            return false;
                        }
                        if (r.getOccupazioneNum() == null) {
                            return false;
                        }

                        String key = r.getCodiceFiscale() + "_" + r.getOccupazioneNum();
                        return listaRecordRegistroIdIsNotNullAnamnesi.contains(key);
                    })
                    .toList();
            List<SpresalDTO> nuovi = rows == null ? List.of() : rows.stream()
                    .filter(r -> {
                        if (r.getCodiceFiscale() == null || r.getCodiceFiscale().isBlank()) {
                            return false;
                        }
                        if (r.getOccupazioneNum() == null) {
                            return false;
                        }

                        String key = r.getCodiceFiscale() + "_" + r.getOccupazioneNum();
                        return !listaRecordRegistroIdIsNotNullAnamnesi.contains(key);
                    })
                    .toList();
            for (SpresalDTO oggetto : doppioni) {
            	String codiceFiscale = oggetto.getCodiceFiscale();

                if (oggetto != null && oggetto.getCodiceFiscale().contains("_")) {
                    codiceFiscale = oggetto.getCodiceFiscale().substring(0, oggetto.getCodiceFiscale().indexOf("_"));
                }
            	tracciaElaborazioneRepository.inserisciScarto(
            			dsl,
                        fileId,
                        "ERR_ELABORAZIONE",
                        "Anamnesi già esistente",
                        null,
                        utenteCreazione,
                        null,
                        null,
                        null,
                        null,
                        codiceFiscale,
                        "REEA_T_REGISTRO_SPRESAL_AMIANTO",
                        0
                );
            }

            if (!nuovi.isEmpty()) {
                spresalRepository.scaricoDatiExcelInTRegistroSpresalAnamnesi(
                        nuovi,
                        fileId,
                        auditLogRequest.getUtente()
                );
            } else {
                tracciaElaborazioneRepository.aggiornaFile(
                        dsl,
                        fileId,
                        4,
                        auditLogRequest.getUtente()
                );
            }
        }
        // DE ORA POSSO FINALMENTE CHIUDERE LA T_FILE PERCHE' HO SAVATO TUTTE LE RIGHE CHE POTEVO
        tracciaElaborazioneRepository.aggiornaFineValidita(dsl, fileId);
        return salvato;
    }
    
    
    @Override
    public FileSalvato importSpresalEsiti(MultipartFile file, AuditLogRequest auditLogRequest) throws IOException {

        FileSalvato salvato = ExcelFileUtils.salvaExcelFileSystem(file, baseDirEsiti);

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

        auditLogRequest.setOperazione("import excel");
        auditLogRequest.setOggOper(JsonNullable.of("SPRESAL ESITI"));
        auditLogRequest.setKeyOper(null);
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

            List<SpresalEsitiDTO> rows = ExcelParser.parseSpresalEsiti(is);

            Set<String> listaRecordRegistroIdIsNotNullEsiti = spresalRepository.getListaRecordRegistroIdIsNotNullSpresalEsiti();

            List<SpresalEsitiDTO> doppioni = rows == null ? List.of() : rows.stream()
                    .filter(r -> {
                        if (r.getCodiceFiscale() == null || r.getCodiceFiscale().isBlank()) {
                            return false;
                        }
                        if (r.getDataVisita() == null) {
                            return false;
                        }

                        String key = r.getCodiceFiscale() + "_" + r.getDataVisita();
                        return listaRecordRegistroIdIsNotNullEsiti.contains(key);
                    })
                    .toList();
            List<SpresalEsitiDTO> nuovi = rows == null ? List.of() : rows.stream()
                    .filter(r -> {
                        if (r.getCodiceFiscale() == null || r.getCodiceFiscale().isBlank()) {
                            return false;
                        }
                        if (r.getDataVisita() == null) {
                            return false;
                        }

                        String key = r.getCodiceFiscale() + "_" + r.getDataVisita();
                        return !listaRecordRegistroIdIsNotNullEsiti.contains(key);
                    })
                    .toList();
            
            for (SpresalEsitiDTO oggetto : doppioni) {
            	String codiceFiscale = oggetto.getCodiceFiscale();

                if (oggetto != null && oggetto.getCodiceFiscale().contains("_")) {
                    codiceFiscale = oggetto.getCodiceFiscale().substring(0, oggetto.getCodiceFiscale().indexOf("_"));
                }
            	tracciaElaborazioneRepository.inserisciScarto(
            			dsl,
                        fileId,
                        "ERR_ELABORAZIONE",
                        "Esiti già esistente",
                        null,
                        utenteCreazione,
                        null,
                        null,
                        null,
                        null,
                        codiceFiscale,
                        "REEA_T_REGISTRO_SPRESAL_ESITI",
                        0
                );
            }

            if (!nuovi.isEmpty()) {
                spresalRepository.scaricoDatiExcelInTRegistroSpresalEsiti(
                        nuovi,
                        fileId,
                        auditLogRequest.getUtente()
                );
            } else {
                tracciaElaborazioneRepository.aggiornaFile(
                        dsl,
                        fileId,
                        4,
                        auditLogRequest.getUtente()
                );
            }
        }
        // DE ORA POSSO FINALMENTE CHIUDERE LA T_FILE PERCHE' HO SAVATO TUTTE LE RIGHE CHE POTEVO
        tracciaElaborazioneRepository.aggiornaFineValidita(dsl, fileId);
        return salvato;
    }



    @Override
	public List<SpresalDTO> getRecordTabSpresalAnamnesi(Integer fileId) throws IOException {
		List<SpresalDTO> listaSpresalAnamnesi = spresalRepository.listaRecordSpresalAnamnesi(fileId);
		return listaSpresalAnamnesi;
	}
    
    
    @Override
	public List<SpresalEsitiDTO> getRecordTabSpresalEsiti(Integer fileId) throws IOException {
		List<SpresalEsitiDTO> listaSpresalEsiti = spresalRepository.listaRecordSpresalEsiti(fileId);
		return listaSpresalEsiti;
	}


	public boolean controlloObbligatorietaDatiFile(SpresalEsitiDTO spresalEsitiDTO) {
		boolean risultato = false;
				
			if(spresalEsitiDTO.getCodiceFiscale() != null && spresalEsitiDTO.getDataVisita() != null 
				&& spresalEsitiDTO.getVisita() != null && spresalEsitiDTO.getLivelloVisita() != null && spresalEsitiDTO.getRiceveIndennizzo() != null)				
					
				risultato = true;

		return risultato;
	}
    
    
    public boolean stessiValoriSpresalEsiti(SpresalEsitiDTO a, SpresalEsitiDTO b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;

        return Objects.equals(a.getRegSpresalEsitiId(),              b.getRegSpresalEsitiId())
            && Objects.equals(a.getRegistroId(),                     b.getRegistroId())
            && Objects.equals(a.getCodiceFiscale(),                  b.getCodiceFiscale())
            && Objects.equals(a.getIdAura(),                         b.getIdAura())
            && Objects.equals(a.getDataVisita(),                     b.getDataVisita())
            && Objects.equals(a.getVisita(),                         b.getVisita())
            && Objects.equals(a.getLivelloVisita(),                  b.getLivelloVisita())
            && Objects.equals(a.getRiceveIndennizzo(),               b.getRiceveIndennizzo())
            && Objects.equals(a.getMalattiaIndennizzo(),             b.getMalattiaIndennizzo())

            && Objects.equals(a.getAccertamentiRx(),                 b.getAccertamentiRx())
            && Objects.equals(a.getAccertamentiRxData(),             b.getAccertamentiRxData())
            && Objects.equals(a.getAccertamentiRxRefertoNormale(),   b.getAccertamentiRxRefertoNormale())

            && Objects.equals(a.getAccertamentiTc(),                 b.getAccertamentiTc())
            && Objects.equals(a.getAccertamentiTcData(),             b.getAccertamentiTcData())
            && Objects.equals(a.getAccertamentiTcRefertoNormale(),   b.getAccertamentiTcRefertoNormale())

            && Objects.equals(a.getAccertamentiSpirometriaSemplice(),              b.getAccertamentiSpirometriaSemplice())
            && Objects.equals(a.getAccertamentiSpirometriaSempliceData(),          b.getAccertamentiSpirometriaSempliceData())
            && Objects.equals(a.getAccertamentiSpirometriaSempliceRefertoNorm(),   b.getAccertamentiSpirometriaSempliceRefertoNorm())

            && Objects.equals(a.getAccertamentiSpirometriaGlobale(),               b.getAccertamentiSpirometriaGlobale())
            && Objects.equals(a.getAccertamentiSpirometriaGlobaleData(),           b.getAccertamentiSpirometriaGlobaleData())
            && Objects.equals(a.getAccertamentiSpirometriaGlobaleRefertoNorm(),    b.getAccertamentiSpirometriaGlobaleRefertoNorm())

            && Objects.equals(a.getAccertamentiDlco(),               b.getAccertamentiDlco())
            && Objects.equals(a.getAccertamentiDlcoData(),           b.getAccertamentiDlcoData())
            && Objects.equals(a.getAccertamentiDlcoRefertoNormale(), b.getAccertamentiDlcoRefertoNormale())

            && Objects.equals(a.getAccertamentiPet(),                b.getAccertamentiPet())
            && Objects.equals(a.getAccertamentiPetData(),            b.getAccertamentiPetData())
            && Objects.equals(a.getAccertamentiPetRefertoNormale(),  b.getAccertamentiPetRefertoNormale())

            && Objects.equals(a.getAccertamentiVisitaPneumologica(),           b.getAccertamentiVisitaPneumologica())
            && Objects.equals(a.getAccertamentiVisitaPneumologicaData(),       b.getAccertamentiVisitaPneumologicaData())
            && Objects.equals(a.getAccertamentiVisitaPneumologicaReferto(),    b.getAccertamentiVisitaPneumologicaReferto())

            && Objects.equals(a.getAccertamentiVisitaRadiologica(),            b.getAccertamentiVisitaRadiologica())
            && Objects.equals(a.getAccertamentiVisitaRadiologicaData(),        b.getAccertamentiVisitaRadiologicaData())
            && Objects.equals(a.getAccertamentiVisitaRadiologicaReferto(),     b.getAccertamentiVisitaRadiologicaReferto())

            && Objects.equals(a.getAccertamentiVisitaOncologica(),             b.getAccertamentiVisitaOncologica())
            && Objects.equals(a.getAccertamentiVisitaOncologicaData(),         b.getAccertamentiVisitaOncologicaData())
            && Objects.equals(a.getAccertamentiVisitaOncologicaReferto(),      b.getAccertamentiVisitaOncologicaReferto())

            && Objects.equals(a.getAccertamentiAltro(),                        b.getAccertamentiAltro())
            && Objects.equals(a.getAccertamentiAltroDescrizione(),             b.getAccertamentiAltroDescrizione())
            && Objects.equals(a.getAccertamentiAltroData(),                    b.getAccertamentiAltroData())
            && Objects.equals(a.getAccertamentiAltroRefertoNormale(),          b.getAccertamentiAltroRefertoNormale())

            && Objects.equals(a.getRisultatoNegativo(),                        b.getRisultatoNegativo())

            && Objects.equals(a.getPpmPlacchePleuricheMonolaterali(),          b.getPpmPlacchePleuricheMonolaterali())
            && Objects.equals(a.getPpmPrimoCertificatoEDenuncia(),             b.getPpmPrimoCertificatoEDenuncia())
            && Objects.equals(a.getPpmPrimoCertificatoEDenunciaData(),         b.getPpmPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getPpmAggravamentoEDenuncia(),                 b.getPpmAggravamentoEDenuncia())
            && Objects.equals(a.getPpmAggravamentoEDenunciaData(),             b.getPpmAggravamentoEDenunciaData())
            && Objects.equals(a.getPpmPercentualeDiRiconoscimento(),           b.getPpmPercentualeDiRiconoscimento())
            && Objects.equals(a.getPpmReferto(),                               b.getPpmReferto())
            && Objects.equals(a.getPpmRefertoData(),                           b.getPpmRefertoData())

            && Objects.equals(a.getPpbPlacchePleuricheBilaterali(),            b.getPpbPlacchePleuricheBilaterali())
            && Objects.equals(a.getPpbPrimoCertificatoEDenuncia(),             b.getPpbPrimoCertificatoEDenuncia())
            && Objects.equals(a.getPpbPrimoCertificatoEDenunciaData(),         b.getPpbPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getPpbAggravamentoEDenuncia(),                 b.getPpbAggravamentoEDenuncia())
            && Objects.equals(a.getPpbAggravamentoEDenunciaData(),             b.getPpbAggravamentoEDenunciaData())
            && Objects.equals(a.getPpbPercentualeDiRiconoscimento(),           b.getPpbPercentualeDiRiconoscimento())
            && Objects.equals(a.getPpbReferto(),                               b.getPpbReferto())
            && Objects.equals(a.getPpbRefertoData(),                           b.getPpbRefertoData())

            && Objects.equals(a.getApAsbestosiPolmonare(),                     b.getApAsbestosiPolmonare())
            && Objects.equals(a.getApPrimoCertificatoEDenuncia(),              b.getApPrimoCertificatoEDenuncia())
            && Objects.equals(a.getApPrimoCertificatoEDenunciaData(),          b.getApPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getApAggravamentoEDenuncia(),                  b.getApAggravamentoEDenuncia())
            && Objects.equals(a.getApAggravamentoEDenunciaData(),              b.getApAggravamentoEDenunciaData())
            && Objects.equals(a.getApPercentualeDiRiconoscimento(),            b.getApPercentualeDiRiconoscimento())
            && Objects.equals(a.getApReferto(),                                b.getApReferto())
            && Objects.equals(a.getApRefertoData(),                            b.getApRefertoData())

            && Objects.equals(a.getFpdFibrosiPleuricaDiffusa(),                b.getFpdFibrosiPleuricaDiffusa())
            && Objects.equals(a.getFpdPrimoCertificatoEDenuncia(),             b.getFpdPrimoCertificatoEDenuncia())
            && Objects.equals(a.getFpdPrimoCertificatoEDenunciaData(),         b.getFpdPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getFpdAggravamentoEDenuncia(),                 b.getFpdAggravamentoEDenuncia())
            && Objects.equals(a.getFpdAggravamentoEDenunciaData(),             b.getFpdAggravamentoEDenunciaData())
            && Objects.equals(a.getFpdPercentualeDiRiconoscimento(),           b.getFpdPercentualeDiRiconoscimento())
            && Objects.equals(a.getFpdReferto(),                               b.getFpdReferto())
            && Objects.equals(a.getFpdRefertoData(),                           b.getFpdRefertoData())

            && Objects.equals(a.getMpMesoteliomaPleurico(),                    b.getMpMesoteliomaPleurico())
            && Objects.equals(a.getMpPrimoCertificatoEDenuncia(),              b.getMpPrimoCertificatoEDenuncia())
            && Objects.equals(a.getMpPrimoCertificatoEDenunciaData(),          b.getMpPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getMpAggravamentoEDenuncia(),                  b.getMpAggravamentoEDenuncia())
            && Objects.equals(a.getMpAggravamentoEDenunciaData(),              b.getMpAggravamentoEDenunciaData())
            && Objects.equals(a.getMpPercentualeDiRiconoscimento(),            b.getMpPercentualeDiRiconoscimento())
            && Objects.equals(a.getMpReferto(),                                b.getMpReferto())
            && Objects.equals(a.getMpRefertoData(),                            b.getMpRefertoData())
            && Objects.equals(a.getMpComunicazioneAlCor(),                     b.getMpComunicazioneAlCor())
            && Objects.equals(a.getMpComunicazioneAlCorData(),                 b.getMpComunicazioneAlCorData())

            && Objects.equals(a.getAmAltroMesotelioma(),                       b.getAmAltroMesotelioma())
            && Objects.equals(a.getAmPrimoCertificatoEDenuncia(),              b.getAmPrimoCertificatoEDenuncia())
            && Objects.equals(a.getAmPrimoCertificatoEDenunciaData(),          b.getAmPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getAmAggravamentoEDenuncia(),                  b.getAmAggravamentoEDenuncia())
            && Objects.equals(a.getAmAggravamentoEDenunciaData(),              b.getAmAggravamentoEDenunciaData())
            && Objects.equals(a.getAmPercentualeDiRiconoscimento(),            b.getAmPercentualeDiRiconoscimento())
            && Objects.equals(a.getAmReferto(),                                b.getAmReferto())
            && Objects.equals(a.getAmRefertoData(),                            b.getAmRefertoData())
            && Objects.equals(a.getAmComunicazioneAlCor(),                     b.getAmComunicazioneAlCor())
            && Objects.equals(a.getAmComunicazioneAlCorData(),                 b.getAmComunicazioneAlCorData())

            && Objects.equals(a.getNlNeoplasiaLaringe(),                       b.getNlNeoplasiaLaringe())
            && Objects.equals(a.getNlPrimoCertificatoEDenuncia(),              b.getNlPrimoCertificatoEDenuncia())
            && Objects.equals(a.getNlPrimoCertificatoEDenunciaData(),          b.getNlPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getNlAggravamentoEDenuncia(),                  b.getNlAggravamentoEDenuncia())
            && Objects.equals(a.getNlAggravamentoEDenunciaData(),              b.getNlAggravamentoEDenunciaData())
            && Objects.equals(a.getNlPercentualeDiRiconoscimento(),            b.getNlPercentualeDiRiconoscimento())
            && Objects.equals(a.getNlReferto(),                                b.getNlReferto())
            && Objects.equals(a.getNlRefertoData(),                            b.getNlRefertoData())

            && Objects.equals(a.getNoNeoplasiaOvarica(),                       b.getNoNeoplasiaOvarica())
            && Objects.equals(a.getNoPrimoCertificatoEDenuncia(),              b.getNoPrimoCertificatoEDenuncia())
            && Objects.equals(a.getNoPrimoCertificatoEDenunciaData(),          b.getNoPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getNoAggravamentoEDenuncia(),                  b.getNoAggravamentoEDenuncia())
            && Objects.equals(a.getNoAggravamentoEDenunciaData(),              b.getNoAggravamentoEDenunciaData())
            && Objects.equals(a.getNoPercentualeDiRiconoscimento(),            b.getNoPercentualeDiRiconoscimento())
            && Objects.equals(a.getNoReferto(),                                b.getNoReferto())
            && Objects.equals(a.getNoRefertoData(),                            b.getNoRefertoData())

            && Objects.equals(a.getTpTumoreDelPolmone(),                       b.getTpTumoreDelPolmone())
            && Objects.equals(a.getTpPrimoCertificatoEDenuncia(),              b.getTpPrimoCertificatoEDenuncia())
            && Objects.equals(a.getTpPrimoCertificatoEDenunciaData(),          b.getTpPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getTpAggravamentoEDenuncia(),                  b.getTpAggravamentoEDenuncia())
            && Objects.equals(a.getTpAggravamentoEDenunciaData(),              b.getTpAggravamentoEDenunciaData())
            && Objects.equals(a.getTpPercentualeDiRiconoscimento(),            b.getTpPercentualeDiRiconoscimento())
            && Objects.equals(a.getTpReferto(),                                b.getTpReferto())
            && Objects.equals(a.getTpRefertoData(),                            b.getTpRefertoData())
            && Objects.equals(a.getTpComunicazioneAlCor(),                     b.getTpComunicazioneAlCor())
            && Objects.equals(a.getTpComunicazioneAlCorData(),                 b.getTpComunicazioneAlCorData())

            && Objects.equals(a.getBpcoEnfisemaPolmonare(),                    b.getBpcoEnfisemaPolmonare())
            && Objects.equals(a.getBpcoPrimoCertificatoEDenuncia(),            b.getBpcoPrimoCertificatoEDenuncia())
            && Objects.equals(a.getBpcoPrimoCertificatoEDenunciaData(),        b.getBpcoPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getBpcoAggravamentoEDenuncia(),                b.getBpcoAggravamentoEDenuncia())
            && Objects.equals(a.getBpcoAggravamentoEDenunciaData(),            b.getBpcoAggravamentoEDenunciaData())
            && Objects.equals(a.getBpcoPercentualeDiRiconoscimento(),          b.getBpcoPercentualeDiRiconoscimento())
            && Objects.equals(a.getBpcoReferto(),                              b.getBpcoReferto())
            && Objects.equals(a.getBpcoRefertoData(),                          b.getBpcoRefertoData())

            && Objects.equals(a.getAltraDiagnosi(),                            b.getAltraDiagnosi())
            && Objects.equals(a.getAltraDiagnosiDescrizione(),                 b.getAltraDiagnosiDescrizione())
            && Objects.equals(a.getAltraPrimoCertificatoEDenuncia(),           b.getAltraPrimoCertificatoEDenuncia())
            && Objects.equals(a.getAltraPrimoCertificatoEDenunciaData(),       b.getAltraPrimoCertificatoEDenunciaData())
            && Objects.equals(a.getAltraAggravamentoEDenuncia(),               b.getAltraAggravamentoEDenuncia())
            && Objects.equals(a.getAltraAggravamentoEDenunciaData(),           b.getAltraAggravamentoEDenunciaData())
            && Objects.equals(a.getAltraPercentualeDiRiconoscimento(),         b.getAltraPercentualeDiRiconoscimento())
            && Objects.equals(a.getAltraReferto(),                             b.getAltraReferto())
            && Objects.equals(a.getAltraRefertoData(),                         b.getAltraRefertoData())

            && Objects.equals(a.getFollowUpPrevisto(),                         b.getFollowUpPrevisto())
            && Objects.equals(a.getAnnoPresuntoProssimaVisita(),               b.getAnnoPresuntoProssimaVisita())
            && Objects.equals(a.getAnnoUltimaVisita(),                         b.getAnnoUltimaVisita())
            && Objects.equals(a.getInvioSintesiAMmg(),                         b.getInvioSintesiAMmg())
            && Objects.equals(a.getIdSpresal(),                                b.getIdSpresal())
            && Objects.equals(a.getValiditaInizio(),                           b.getValiditaInizio())
            && Objects.equals(a.getValiditaFine(),                             b.getValiditaFine())
            && Objects.equals(a.getDataCreazione(),                            b.getDataCreazione())
            && Objects.equals(a.getDataModifica(),                             b.getDataModifica())
            && Objects.equals(a.getDataCancellazione(),                        b.getDataCancellazione())
            && Objects.equals(a.getUtenteCreazione(),                          b.getUtenteCreazione())
            && Objects.equals(a.getUtenteModifica(),                           b.getUtenteModifica())
            && Objects.equals(a.getUtenteCancellazione(),                      b.getUtenteCancellazione());
    }

	@Override
	public List<SpresalEsitiDTO> getEsitiByRegistroId(Integer registroId) {
		return spresalRepository.getEsitiByRegistroId(registroId);
	}

	@Override
	public void eliminaSpresalEsitiSenzaRegistroId() {
		spresalRepository.eliminaSpresalEsitiSenzaRegistroId();
	}

	@Override
	public void aggiornaCrpt(Integer regSpresalAnamnesiId, Map<String, Object> body, AuditLogRequest auditLogRequest) {
		spresalRepository.aggiornaCrpt(regSpresalAnamnesiId, body, auditLogRequest);
		
		//
		if(auditLogRequest == null)
        	auditLogRequest = new AuditLogRequest();

        auditLogRequest.setOperazione("Dettaglio assistito");
        auditLogRequest.setOggOper(JsonNullable.of("Registro – aggiorna dati occupazione"));
        auditLogRequest.setKeyOper(null);
        auditLogRequest.setRequestPayload(null);
        auditLogRequest.setEsitoChiamata(200);

        auditLogRequest.setResponsePayload(null);

        auditService.salvaAudit(auditLogRequest);
		//
	}

    @Override
    public void aggiornaCounseling(Integer regSpresalAnamnesiId, Map<String, Object> body, AuditLogRequest auditLogRequest) {
        spresalRepository.aggiornaCounseling(regSpresalAnamnesiId, body, auditLogRequest);
        
        //
		if(auditLogRequest == null)
        	auditLogRequest = new AuditLogRequest();

        auditLogRequest.setOperazione("Dettaglio assistito");
        auditLogRequest.setOggOper(JsonNullable.of("Registro – aggiorna counseling"));
        auditLogRequest.setKeyOper(null);
        auditLogRequest.setRequestPayload(null);
        auditLogRequest.setEsitoChiamata(200);

        auditLogRequest.setResponsePayload(null);

        auditService.salvaAudit(auditLogRequest);
		//
    }



    //	@Transactional(propagation = Propagation.REQUIRES_NEW)
    public Integer aggiornaSpresalAnamnesiFlagElaboratoIndipendente(AnagraficaDTO dto) {
        return spresalRepository.aggiornaSpresalAnamnesiFlagElaborato(dto);
    }
	
//	@Transactional(propagation = Propagation.REQUIRES_NEW)
    public Integer aggiornaSpresalEsitiFlagElaboratoIndipendente(SpresalEsitiDTO dto) {
        return spresalRepository.aggiornaSpresalEsitiFlagElaborato(dto);
    }

    private static final Set<String> ALLOWED_ACQUISITA_FIELDS = Set.of(
            "accertamenti_rx_acquisita",
            "accertamenti_tc_acquisita",
            "accertamenti_spirometria_semplice_acquisita",
            "accertamenti_spirometria_globale_acquisita",
            "accertamenti_dlco_acquisita",
            "accertamenti_pet_acquisita",
            "accertamenti_altro_referto_acquisita",
            "accertamenti_visita_pneumologica_acquisita",
            "accertamenti_visita_radiologica_acquisita",
            "accertamenti_visita_oncologica_acquisita"
    );

    @Override
    public void aggiornaPrestazioneAcquisita(Integer esitoId, Map<String, Object> body, AuditLogRequest auditLogRequest) {
        spresalRepository.aggiornaPrestazioneAcquisita(esitoId, body, auditLogRequest);
        
        //
		if(auditLogRequest == null)
        	auditLogRequest = new AuditLogRequest();

        auditLogRequest.setOperazione("Dettaglio assistito");
        auditLogRequest.setOggOper(JsonNullable.of("Registro – aggiorna accertamenti - flag acquisito"));
        auditLogRequest.setKeyOper(null);
        auditLogRequest.setRequestPayload(null);
        auditLogRequest.setEsitoChiamata(200);

        auditLogRequest.setResponsePayload(null);

        auditService.salvaAudit(auditLogRequest);
		//
    }

    @Override
    public void esportaAllegato4(int anno, String utente) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String fileName = "export_PIEMONTE_all4_" + anno + "_" + timestamp + ".xlsx";
        Integer fileId = null;
        Integer elaborazioneId = null;
        try {
            List<SpresalEsitiDTO> tutti = spresalRepository.queryAllegato4Tutti(anno);
            System.out.println(">>> queryAllegato4Tutti(" + anno + ") → " + tutti.size() + " record");


            if (tutti.isEmpty()) {
                throw new RuntimeException("Nessun dato disponibile per l'anno " + anno);
            }

            byte[] excelBytes;
            try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {

                // N_Pazienti
                creaFoglio(wb, "N_Pazienti",
                        tutti.stream().filter(r -> inAnno(r.getDataVisita(), anno)).toList(), anno,
                        new String[]{},
                        r -> new String[]{});

// Accertamenti RX
                creaFoglio(wb, "Accert_RX_Torace",
                        "ALLEGATO 4 - ACCERTAMENTI RX TORACE - " + anno,
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiRxData(), anno)).toList(), anno,
                        new String[]{"accertamenti_rx","accertamenti_rx_data","accertamenti_rx_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiRx()), str(r.getAccertamentiRxData()),
                                str(r.getAccertamentiRxAcquisita())});

// Accertamenti TC
                creaFoglio(wb, "Accert_TC_Torace",
                        "ALLEGATO 4 - ACCERTAMENTI TC TORACE - " + anno,
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiTcData(), anno)).toList(), anno,
                        new String[]{"accertamenti_tc","accertamenti_tc_data","accertamenti_tc_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiTc()), str(r.getAccertamentiTcData()),
                                str(r.getAccertamentiTcAcquisita())});

// Accertamenti Spirometria Semplice
                creaFoglio(wb, "Accert_Spiro_Semplice",
                        "ALLEGATO 4 - ACCERTAMENTI SPIROMETRIA SEMPLICE - " + anno,
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiSpirometriaSempliceData(), anno)).toList(), anno,
                        new String[]{"accertamenti_spirometria_semplice","accertamenti_spirometria_semplice_data","accertamenti_spirometria_semplice_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiSpirometriaSemplice()), str(r.getAccertamentiSpirometriaSempliceData()),
                                str(r.getAccertamentiSpirometriaSempliceAcquisita())});

// Accertamenti Spirometria Globale
                creaFoglio(wb, "Accert_Spiro_Globale",
                        "ALLEGATO 4 - ACCERTAMENTI SPIROMETRIA GLOBALE - " + anno,
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiSpirometriaGlobaleData(), anno)).toList(), anno,
                        new String[]{"accertamenti_spirometria_globale","accertamenti_spirometria_globale_data","accertamenti_spirometria_globale_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiSpirometriaGlobale()), str(r.getAccertamentiSpirometriaGlobaleData()),
                                str(r.getAccertamentiSpirometriaGlobaleAcquisita())});

// Accertamenti DLCO
                creaFoglio(wb, "Accertamenti_DLCO",
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiDlcoData(), anno)).toList(), anno,
                        new String[]{"accertamenti_dlco","accertamenti_dlco_data","accertamenti_dlco_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiDlco()), str(r.getAccertamentiDlcoData()),
                                str(r.getAccertamentiDlcoAcquisita())});

// Accertamenti PET
                creaFoglio(wb, "Accertamenti_PET",
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiPetData(), anno)).toList(), anno,
                        new String[]{"accertamenti_pet","accertamenti_pet_data","accertamenti_pet_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiPet()), str(r.getAccertamentiPetData()),
                                str(r.getAccertamentiPetAcquisita())});

// Visita Pneumologica
                creaFoglio(wb, "Visita_Pneumologica",
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiVisitaPneumologicaData(), anno)).toList(), anno,
                        new String[]{"accertamenti_visita_pneumologica","accertamenti_visita_pneumologica_data","accertamenti_visita_pneumologica_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiVisitaPneumologica()), str(r.getAccertamentiVisitaPneumologicaData()),
                                str(r.getAccertamentiVisitaPneumologicaAcquisita())});

// Visita Radiologica
                creaFoglio(wb, "Visita_Radiologica",
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiVisitaRadiologicaData(), anno)).toList(), anno,
                        new String[]{"accertamenti_visita_radiologica","accertamenti_visita_radiologica_data","accertamenti_visita_radiologica_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiVisitaRadiologica()), str(r.getAccertamentiVisitaRadiologicaData()),
                                str(r.getAccertamentiVisitaRadiologicaAcquisita())});

// Visita Oncologica
                creaFoglio(wb, "Visita_Oncologica",
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiVisitaOncologicaData(), anno)).toList(), anno,
                        new String[]{"accertamenti_visita_oncologica","accertamenti_visita_oncologica_data","accertamenti_visita_oncologica_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiVisitaOncologica()), str(r.getAccertamentiVisitaOncologicaData()),
                                str(r.getAccertamentiVisitaOncologicaAcquisita())});

// Accertamenti Altro
                creaFoglio(wb, "Accertamenti_Altro",
                        tutti.stream().filter(r -> inAnno(r.getAccertamentiAltroData(), anno)).toList(), anno,
                        new String[]{"accertamenti_altro","accertamenti_altro_descrizione","accertamenti_altro_data","accertamenti_altro_acquisita"},
                        r -> new String[]{bool(r.getAccertamentiAltro()), str(r.getAccertamentiAltroDescrizione()),
                                str(r.getAccertamentiAltroData()), str(r.getAccertamentiAltroRefertoAcquisita())});

// PPM
                creaFoglio(wb, "PPM_Placche_Monolaterali",
                        "ALLEGATO 4 - PPM - PLACCHE PLEURICHE MONOLATERALI - " + anno,
                        tutti.stream().filter(r -> inAnno(r.getPpmPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getPpmAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"ppm_placche_pleuriche_monolaterali","ppm_primo_certificato_e_denuncia","ppm_primo_certificato_e_denuncia_data","ppm_aggravamento_e_denuncia","ppm_aggravamento_e_denuncia_data","ppm_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getPpmPlacchePleuricheMonolaterali()),
                                bool(r.getPpmPrimoCertificatoEDenuncia()), str(r.getPpmPrimoCertificatoEDenunciaData()),
                                bool(r.getPpmAggravamentoEDenuncia()), str(r.getPpmAggravamentoEDenunciaData()),
                                str(r.getPpmPercentualeDiRiconoscimento())});

// PPB
                creaFoglio(wb, "PPB_Placche_Bilaterali",
                        "ALLEGATO 4 - PPB - PLACCHE PLEURICHE BILATERALI - " + anno,
                        tutti.stream().filter(r -> inAnno(r.getPpbPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getPpbAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"ppb_placche_pleuriche_bilaterali","ppb_primo_certificato_e_denuncia","ppb_primo_certificato_e_denuncia_data","ppb_aggravamento_e_denuncia","ppb_aggravamento_e_denuncia_data","ppb_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getPpbPlacchePleuricheBilaterali()),
                                bool(r.getPpbPrimoCertificatoEDenuncia()), str(r.getPpbPrimoCertificatoEDenunciaData()),
                                bool(r.getPpbAggravamentoEDenuncia()), str(r.getPpbAggravamentoEDenunciaData()),
                                str(r.getPpbPercentualeDiRiconoscimento())});

// AP
                creaFoglio(wb, "AP_Asbestosi_Polmonare",
                        tutti.stream().filter(r -> inAnno(r.getApPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getApAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"ap_asbestosi_polmonare","ap_primo_certificato_e_denuncia","ap_primo_certificato_e_denuncia_data","ap_aggravamento_e_denuncia","ap_aggravamento_e_denuncia_data","ap_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getApAsbestosiPolmonare()),
                                bool(r.getApPrimoCertificatoEDenuncia()), str(r.getApPrimoCertificatoEDenunciaData()),
                                bool(r.getApAggravamentoEDenuncia()), str(r.getApAggravamentoEDenunciaData()),
                                str(r.getApPercentualeDiRiconoscimento())});

// FPD
                creaFoglio(wb, "FPD_Fibrosi_Pleurica_Diffusa",
                        tutti.stream().filter(r -> inAnno(r.getFpdPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getFpdAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"fpd_fibrosi_pleurica_diffusa","fpd_primo_certificato_e_denuncia","fpd_primo_certificato_e_denuncia_data","fpd_aggravamento_e_denuncia","fpd_aggravamento_e_denuncia_data","fpd_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getFpdFibrosiPleuricaDiffusa()),
                                bool(r.getFpdPrimoCertificatoEDenuncia()), str(r.getFpdPrimoCertificatoEDenunciaData()),
                                bool(r.getFpdAggravamentoEDenuncia()), str(r.getFpdAggravamentoEDenunciaData()),
                                str(r.getFpdPercentualeDiRiconoscimento())});

// MP
                creaFoglio(wb, "MP_Mesotelioma_Pleurico",
                        tutti.stream().filter(r -> inAnno(r.getMpPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getMpAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"mp_mesotelioma_pleurico","mp_primo_certificato_e_denuncia","mp_primo_certificato_e_denuncia_data","mp_aggravamento_e_denuncia","mp_aggravamento_e_denuncia_data","mp_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getMpMesoteliomaPleurico()),
                                bool(r.getMpPrimoCertificatoEDenuncia()), str(r.getMpPrimoCertificatoEDenunciaData()),
                                bool(r.getMpAggravamentoEDenuncia()), str(r.getMpAggravamentoEDenunciaData()),
                                str(r.getMpPercentualeDiRiconoscimento())});

// AM
                creaFoglio(wb, "AM_Altro_Mesotelioma",
                        tutti.stream().filter(r -> inAnno(r.getAmPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getAmAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"am_altro_mesotelioma","am_primo_certificato_e_denuncia","am_primo_certificato_e_denuncia_data","am_aggravamento_e_denuncia","am_aggravamento_e_denuncia_data","am_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getAmAltroMesotelioma()),
                                bool(r.getAmPrimoCertificatoEDenuncia()), str(r.getAmPrimoCertificatoEDenunciaData()),
                                bool(r.getAmAggravamentoEDenuncia()), str(r.getAmAggravamentoEDenunciaData()),
                                str(r.getAmPercentualeDiRiconoscimento())});

// NL
                creaFoglio(wb, "NL_Neoplasia_Laringe",
                        tutti.stream().filter(r -> inAnno(r.getNlPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getNlAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"nl_neoplasia_laringe","nl_primo_certificato_e_denuncia","nl_primo_certificato_e_denuncia_data","nl_aggravamento_e_denuncia","nl_aggravamento_e_denuncia_data","nl_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getNlNeoplasiaLaringe()),
                                bool(r.getNlPrimoCertificatoEDenuncia()), str(r.getNlPrimoCertificatoEDenunciaData()),
                                bool(r.getNlAggravamentoEDenuncia()), str(r.getNlAggravamentoEDenunciaData()),
                                str(r.getNlPercentualeDiRiconoscimento())});

// NO
                creaFoglio(wb, "NO_Neoplasia_Ovarica",
                        tutti.stream().filter(r -> inAnno(r.getNoPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getNoAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"no_neoplasia_ovarica","no_primo_certificato_e_denuncia","no_primo_certificato_e_denuncia_data","no_aggravamento_e_denuncia","no_aggravamento_e_denuncia_data","no_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getNoNeoplasiaOvarica()),
                                bool(r.getNoPrimoCertificatoEDenuncia()), str(r.getNoPrimoCertificatoEDenunciaData()),
                                bool(r.getNoAggravamentoEDenuncia()), str(r.getNoAggravamentoEDenunciaData()),
                                str(r.getNoPercentualeDiRiconoscimento())});

// TP
                creaFoglio(wb, "TP_Tumore_Polmone",
                        tutti.stream().filter(r -> inAnno(r.getTpPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getTpAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"tp_tumore_del_polmone","tp_primo_certificato_e_denuncia","tp_primo_certificato_e_denuncia_data","tp_aggravamento_e_denuncia","tp_aggravamento_e_denuncia_data","tp_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getTpTumoreDelPolmone()),
                                bool(r.getTpPrimoCertificatoEDenuncia()), str(r.getTpPrimoCertificatoEDenunciaData()),
                                bool(r.getTpAggravamentoEDenuncia()), str(r.getTpAggravamentoEDenunciaData()),
                                str(r.getTpPercentualeDiRiconoscimento())});

// BPCO
                creaFoglio(wb, "BPCO_Enfisema_Polmonare",
                        "ALLEGATO 4 - BPCO/ENFISEMA POLMONARE - " + anno,
                        tutti.stream().filter(r -> inAnno(r.getBpcoPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getBpcoAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"bpco_enfisema_polmonare","bpco_primo_certificato_e_denuncia","bpco_primo_certificato_e_denuncia_data","bpco_aggravamento_e_denuncia","bpco_aggravamento_e_denuncia_data","bpco_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getBpcoEnfisemaPolmonare()),
                                bool(r.getBpcoPrimoCertificatoEDenuncia()), str(r.getBpcoPrimoCertificatoEDenunciaData()),
                                bool(r.getBpcoAggravamentoEDenuncia()), str(r.getBpcoAggravamentoEDenunciaData()),
                                str(r.getBpcoPercentualeDiRiconoscimento())});

// Altra Diagnosi
                creaFoglio(wb, "Altra_Diagnosi",
                        tutti.stream().filter(r -> inAnno(r.getAltraPrimoCertificatoEDenunciaData(), anno)
                                || inAnno(r.getAltraAggravamentoEDenunciaData(), anno)).toList(), anno,
                        new String[]{"altra_diagnosi","altra_diagnosi_descrizione","altra_primo_certificato_e_denuncia","altra_primo_certificato_e_denuncia_data","altra_aggravamento_e_denuncia","altra_aggravamento_e_denuncia_data","altra_percentuale_di_riconoscimento"},
                        r -> new String[]{bool(r.getAltraDiagnosi()), str(r.getAltraDiagnosiDescrizione()),
                                bool(r.getAltraPrimoCertificatoEDenuncia()), str(r.getAltraPrimoCertificatoEDenunciaData()),
                                bool(r.getAltraAggravamentoEDenuncia()), str(r.getAltraAggravamentoEDenunciaData()),
                                str(r.getAltraPercentualeDiRiconoscimento())});


                if (wb.getNumberOfSheets() == 0) {
                    wb.createSheet("N_Pazienti"); // foglio vuoto di sicurezza
                }

                wb.write(baos);
                excelBytes = baos.toByteArray();
            }


            ExcelFileUtils.FileSalvato salvato = ExcelFileUtils.salvaExcelExportResult(
                    new ExcelExportResult(excelBytes, fileName, null, null), baseDirExport);

            long fileSize = java.nio.file.Files.size(java.nio.file.Paths.get(salvato.filePath()));

            fileId = tracciaElaborazioneRepository.inserisciFile(
                    dsl,
                    salvato.fileName(),
                    salvato.filePath(),
                    salvato.checksum(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fileSize,
                    1,
                    utente);
            elaborazioneId = tracciaElaborazioneRepository.inserisciTracciaElaborazione(dsl, fileId, 2, utente);
            tracciaElaborazioneRepository.aggiornaTracciaElaborazioneFineOk(
                    dsl, elaborazioneId, 3,
                    tutti.size(), tutti.size(), 0, tutti.size(), 0, "OK", utente);
            tracciaElaborazioneRepository.aggiornaFile(dsl, fileId, 3, utente);


        } catch (Exception e) {
            if (fileId != null) {
                try { tracciaElaborazioneRepository.aggiornaFile(dsl, fileId, 4, utente); } catch (Exception ignored) {}
            }
            throw new RuntimeException("Errore export allegato 4: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Allegato4ElaborazioneDTO> listaElaborazioniAllegato4() {
        return spresalRepository.listaElaborazioniAllegato4();
    }

    @Override
    public org.springframework.core.io.Resource getFileAllegato4(Integer elaborazioneId) {
        String filePath = spresalRepository.getFilePathAllegato4(elaborazioneId);
        if (filePath == null) throw new RuntimeException("Elaborazione non trovata: " + elaborazioneId);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(filePath);
        if (!resource.exists()) throw new RuntimeException("File non trovato: " + filePath);
        return resource;
    }

    private String str(Object val) {
        if (val == null) return "";
        if (val instanceof java.time.LocalDate) return fmtData((java.time.LocalDate) val);
        return val.toString();
    }


    private String bool(Boolean val) {
        return val == null ? "" : val ? "SI" : "NO";
    }

    private boolean inAnno(java.time.LocalDate d, int anno) {
        return d != null && d.getYear() == anno;
    }

    private void creaFoglio(XSSFWorkbook wb, String nome, List<SpresalEsitiDTO> righe, int anno,
                            String[] intestazioniExtra,
                            java.util.function.Function<SpresalEsitiDTO, String[]> mapper) {
        creaFoglio(wb, nome, null, righe, anno, intestazioniExtra, mapper);
    }

    private void creaFoglio(XSSFWorkbook wb, String nome, String titoloOverride, List<SpresalEsitiDTO> righe, int anno,
                            String[] intestazioniExtra,
                            java.util.function.Function<SpresalEsitiDTO, String[]> mapper) {
        if (righe.isEmpty()) return;
        XSSFSheet sheet = wb.createSheet(nome);

        String[] base = {"codice_fiscale","id_aura","asl_assistenza","data_visita","visita","livello_visita"};
        String[] tutte = concat(base, intestazioniExtra);

        // Stile titolo giallo
        XSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.YELLOW.getIndex());
        titleStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        org.apache.poi.xssf.usermodel.XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        titleStyle.setFont(titleFont);

        // Stile intestazioni
        XSSFCellStyle headerStyle = wb.createCellStyle();
        org.apache.poi.xssf.usermodel.XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Riga 0: titolo giallo
        XSSFRow titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(22);
        XSSFCell titleCell = titleRow.createCell(0);
        String titoloHeader = titoloOverride != null
            ? titoloOverride
            : "ALLEGATO 4 - " + nome.replace("_", " ").toUpperCase() + " - " + anno;
        titleCell.setCellValue(titoloHeader.toUpperCase());
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, tutte.length - 1));

        // Riga 1: intestazioni
        XSSFRow hdr = sheet.createRow(1);
        for (int i = 0; i < tutte.length; i++) {
            XSSFCell cell = hdr.createCell(i);
            cell.setCellValue(tutte[i]);
            cell.setCellStyle(headerStyle);
        }

        // Righe 2+: dati
        int rn = 2;
        for (SpresalEsitiDTO r : righe) {
            XSSFRow row = sheet.createRow(rn++);
            String[] valBase = {
                    str(r.getCodiceFiscale()), str(r.getIdAura()), str(r.getAslAssistenza()),
                    fmtData(r.getDataVisita()), str(r.getVisita()), str(r.getLivelloVisita())
            };
            String[] tutti2 = concat(valBase, mapper.apply(r));
            for (int i = 0; i < tutti2.length; i++) row.createCell(i).setCellValue(tutti2[i]);
        }

        // Auto-dimensiona colonne
        for (int i = 0; i < tutte.length; i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i) + 512;
            sheet.setColumnWidth(i, Math.min(w, 20000));
        }
    }


    private static String[] concat(String[] a, String[] b) {
        String[] r = new String[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private String fmtData(java.time.LocalDate d) {
        return d != null ? d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }
    
	@Override
	public void chiudiTuttiRecordTabSpresalAnamnesiByFile(Integer fileId) {
		spresalRepository.chiudiTuttiRecordTabRegistroSpresalAnamnesiByFile(fileId);
	}
	
	@Override
	public void chiudiTuttiRecordTabSpresalEsitiByFile(Integer fileId) {
		spresalRepository.chiudiTuttiRecordTabRegistroSpresalEsitiByFile(fileId);
	}


}
