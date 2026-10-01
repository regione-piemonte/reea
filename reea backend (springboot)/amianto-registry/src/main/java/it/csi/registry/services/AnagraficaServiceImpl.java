package it.csi.registry.services;

import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalAnamnesi.REEA_T_REGISTRO_SPRESAL_ANAMNESI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;

import it.csi.registry.configuratoreDTO.UserDTO;
import it.csi.registry.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jooq.DSLContext;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.csi.registry.repositories.AdesioneRepository;
import it.csi.registry.repositories.AnagraficaRepository;
import it.csi.registry.repositories.AslRepository;
import it.csi.registry.repositories.ComuneRepository;
import it.csi.registry.repositories.EsenzioneRepository;
import it.csi.registry.repositories.FonteRepository;
import it.csi.registry.repositories.InailRepository;
import it.csi.registry.repositories.ProvinciaRepository;
import it.csi.registry.repositories.SpresalRepository;
import it.csi.registry.repositories.SpresalRepositoryImpl;
import it.csi.registry.repositories.TracciaElaborazioneRepository;
import it.csi.registry.soap.AnagrafeFind.DatiAnagraficiMsg;
import it.csi.registry.soap.AnagrafeSanitaria.SoggettoAuraMsg;
import it.csi.registry.soap.anagrafe.clients.AnagrafeFindClient;
import it.csi.registry.soap.anagrafe.clients.AnagrafeGetClient;
import it.csi.registry.util.DateConversionUtils;
import it.csi.registry.util.ExcelExportResult;
import it.csi.registry.util.ExcelFileUtils;
import it.csi.registry.util.ExportField;
import it.csi.registry.util.RegistroUtils;
import it.csi.registry.util.SafeAccess;
import it.csi.registry.util.SpresalUtils;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AnagraficaServiceImpl implements AnagraficaService {

    private final DSLContext dsl;
    private final AnagraficaRepository anagraficaRepository;
    private final AnagrafeFindClient anagrafeFindClient;
    private final AnagrafeGetClient anagrafeGetClient;
    private final FonteRepository fonteRepository;
    private final AdesioneRepository adesioneRepository;
    private final AslRepository aslRepository;
    private final EsenzioneRepository esenzioneRepository;
    private final InailRepository inailRepository;
    private final SpresalRepository spresalRepository;
    private final ProvinciaRepository provinciaRepository;
    private final ComuneRepository comuneRepository;
    private final TracciaElaborazioneRepository tracciaElaborazioneRepository;
    private final SpresalService spresalService;
    private final TracciaElaborazioneService tracciaElaborazioneService;
    private final AuditService auditService;
    private final AuditPayloadMapper auditPayloadMapper;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AnagraficaServiceImpl.class);

    @Autowired
    private HttpServletRequest request;

    private String getCfOperatore() {
        HttpSession session = request.getSession(false);
        if (session == null) return "SISTEMA";
        UserDTO user = (UserDTO) session.getAttribute("currentUser");
        return user != null ? user.getCodiceFiscale() : "SISTEMA";
    }


    @Value("${export.download.dir}")
    private String baseDir;
    

    public AnagraficaServiceImpl(DSLContext dsl, AnagraficaRepository anagraficaRepository,
                                 AnagrafeFindClient anagrafeFindClient, AnagrafeGetClient anagrafeGetClient,
                                 FonteRepository fonteRepository, AdesioneRepository adesioneRepository,
                                 AslRepository aslRepository, EsenzioneRepository esenzioneRepository,
                                 InailRepository inailRepository, SpresalRepository spresalRepository,
                                 ProvinciaRepository provinciaRepository, ComuneRepository comuneRepository,
                                 TracciaElaborazioneRepository tracciaElaborazioneRepository,
                                 SpresalService spresalService,
                                 TracciaElaborazioneService tracciaElaborazioneService,
                                 AuditService auditService,
                                 AuditPayloadMapper auditPayloadMapper
                                ) {
        this.dsl = dsl;
        this.anagraficaRepository = anagraficaRepository;
        this.anagrafeFindClient = anagrafeFindClient;
        this.anagrafeGetClient = anagrafeGetClient;
        this.fonteRepository = fonteRepository;
        this.adesioneRepository = adesioneRepository;
        this.aslRepository = aslRepository;
        this.esenzioneRepository = esenzioneRepository;
        this.inailRepository = inailRepository;
        this.spresalRepository = spresalRepository;
        this.provinciaRepository = provinciaRepository;
        this.comuneRepository = comuneRepository;
        this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
        this.spresalService = spresalService;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
        this.auditService = auditService;
        this.auditPayloadMapper = auditPayloadMapper;
    }
    
//    @Override
//    public List<AnagraficaDTO> getLista(List<Integer> filtroFonteId,
//                                        List<String> descrizioniStato,
//                                        List<String> sezione,
//                                        Boolean insInSorveglianza,
//                                        String tipoElencoInail,
//                                        List<Integer> assistenzaAslId,
//                                        AuditLogRequest auditLogRequest) {
//    	
//      List<AnagraficaDTO> listaRitorno = anagraficaRepository.findListaAnagrafica(
//              filtroFonteId,
//              descrizioniStato,
//              sezione,
//              insInSorveglianza,
//              tipoElencoInail,
//              assistenzaAslId
//      );
//
//      if (auditLogRequest == null) {
//          auditLogRequest = new AuditLogRequest();
//      }
//
//      auditLogRequest.setIdApp(auditLogRequest.getIdApp());
//      auditLogRequest.setOperazione("read");
////      auditLogRequest.setOggOper("ANAGRAFICA");
//      auditLogRequest.setOggOper(JsonNullable.of("ANAGRAFICA"));
//
//      // valorizzati dal contesto chiamante / FE
//      auditLogRequest.setUtente(auditLogRequest.getUtente());
//      auditLogRequest.setUuid(auditLogRequest.getUuid());
//
//      // scegli tu la logica: null oppure chiave sintetica della ricerca
//      auditLogRequest.setKeyOper(null);
//
//      auditLogRequest.setRequestPayload(null);
//      auditLogRequest.setResponsePayload(JsonNullable.of(auditPayloadMapper.toBytes(listaRitorno)));
//      auditLogRequest.setEsitoChiamata(200);
//
//      auditService.salvaAudit(auditLogRequest);
//
//      return listaRitorno;
//    }


    @Override
    public ListaAnagraficheResponse getLista(List<Integer> filtroFonteId,
                                             List<String> descrizioniStato,
                                             List<String> sezione,
                                             Boolean insInSorveglianza,
                                             String tipoElencoInail,
                                             List<Integer> assistenzaAslId,
                                             String codiceFiscale,
                                             String cognome,
                                             String nome,
                                             String cognomeLettDa,
                                             String cognomeLettA,
                                             LocalDate nascitaData,
                                             Boolean azzeraContatoreProssimoStep,
                                             String profiloUtente,
                                             AuditLogRequest auditLogRequest) {

        Integer totalRecords = anagraficaRepository.countListaAnagrafica(
                filtroFonteId,
                descrizioniStato,
                sezione,
                insInSorveglianza,
                tipoElencoInail,
                assistenzaAslId, codiceFiscale, cognome, nome, cognomeLettDa, cognomeLettA, nascitaData, profiloUtente
        );

        List<AnagraficaDTO> listaRitorno = anagraficaRepository.findListaAnagrafica(
                filtroFonteId,
                descrizioniStato,
                sezione,
                insInSorveglianza,
                tipoElencoInail,
                assistenzaAslId, codiceFiscale, cognome, nome, cognomeLettDa, cognomeLettA, nascitaData, azzeraContatoreProssimoStep, profiloUtente, auditLogRequest


        );

        if (auditLogRequest == null) {
            auditLogRequest = new AuditLogRequest();
        }

        auditLogRequest.setIdApp(auditLogRequest.getIdApp());
        auditLogRequest.setOperazione("read");
        auditLogRequest.setOggOper(JsonNullable.of("ANAGRAFICA"));
        auditLogRequest.setUtente(auditLogRequest.getUtente());
        auditLogRequest.setUuid(auditLogRequest.getUuid());
        auditLogRequest.setKeyOper(null);
        auditLogRequest.setRequestPayload(null);
        auditLogRequest.setResponsePayload(JsonNullable.of(auditPayloadMapper.toBytes(listaRitorno)));
        auditLogRequest.setEsitoChiamata(200);

        auditService.salvaAudit(auditLogRequest);

        ListaAnagraficheResponse response = new ListaAnagraficheResponse();
        response.setRecords(listaRitorno);
        response.setTotalRecords(totalRecords);

        return response;
    }
    
    
    @Override
    @Transactional
    public Integer creazioneAnagraficaSpresalEsiti(SpresalEsitiDTO dtos, Integer elaborazioneId) {

        try {
            boolean uguali = false;
            AnagraficaDTO anagraficaDTO = anagraficaRepository.findByCodiceFiscale(dtos.getCodiceFiscale());
            Integer variabileFinale = null;
            
            //
            List<StoriaStatoDTO> statoDTOs = null;
            if (anagraficaDTO != null)
            	statoDTOs = anagraficaRepository.findStoriaStati(anagraficaDTO.getSoggettoId().intValue());
            StoriaStatoDTO corrente = anagraficaRepository.findStatoCorrente(anagraficaDTO.getSoggettoId().intValue());
            if(statoDTOs == null)
            	statoDTOs = new ArrayList<StoriaStatoDTO>();
            statoDTOs.add(corrente);
            Set<String> statiDaSbloccare = Set.of("4");
            Set<Integer> statiDaSbloccareInteger = Set.of(4);

            if (anagraficaDTO != null
                  && statoDTOs != null &&
                    (statoDTOs.stream()
                            .map(StoriaStatoDTO::getSoggettoStatoId)
                            .anyMatch(statiDaSbloccare::contains) || (anagraficaDTO.getSoggettoStatoId() != null && statiDaSbloccareInteger.contains(anagraficaDTO.getSoggettoStatoId())))) {
            //
//            if (anagraficaDTO != null
//                    && anagraficaDTO.getSoggettoStatoId() != null
//                    && List.of(5, 8, 9).contains(anagraficaDTO.getSoggettoStatoId())) {

                boolean verifica = controlloObbligatorietaDatiFile(dtos);

                if (verifica) {
                    SpresalEsitiDTO oggettoConRegistroId = spresalRepository
                            .recuperaOggettoTRegistroSpresalEsitiByCF(
                                    dtos.getCodiceFiscale(),
                                    true,
                                    dtos.getDataVisita(),
                                    true
                            );

                    SpresalEsitiDTO oggettoSenzaRegistroId = spresalRepository
                            .recuperaOggettoTRegistroSpresalEsitiByCF(
                                    dtos.getCodiceFiscale(),
                                    false,
                                    dtos.getDataVisita(),
                                    false
                            );

                    if (oggettoConRegistroId != null && oggettoSenzaRegistroId != null) {
                        uguali = stessiValoriSpresalEsiti(
                                oggettoConRegistroId,
                                oggettoSenzaRegistroId
                        );
                    }

                    spresalService.aggiornaSpresalEsitiFlagElaboratoIndipendente(dtos);

                    // boolean deveProcessare;
                    //
                    // if (oggettoConRegistroId == null) {
                    //     // Prima inserzione: nessun record confermato esiste ancora
                    //     deveProcessare = true;
                    // } else {
                    //     // Aggiornamento: esiste giï¿½ un confermato, processa solo se dati cambiati
                    //     boolean uguali = stessiValoriSpresalEsiti(oggettoConRegistroId, oggettoSenzaRegistroId);
                    //     deveProcessare = !uguali && oggettoConRegistroId.getValiditaFine() == null;
                    // }

                    if (!uguali) {
                        // SEMPRE aggiorna registro_id nella staging
                        variabileFinale = spresalRepository.aggiornaSpresalEsitiRegistroId(
                                anagraficaDTO,
                                dtos.getRegSpresalEsitiId().intValue(),
                                elaborazioneId,
                                dtos.getUtenteCreazione(),
                                2
                        );

                        if (oggettoConRegistroId != null) {
                            spresalRepository.chiudiRecordDuplicatoSpresalEsiti(
                                    oggettoConRegistroId,
                                    elaborazioneId,
                                    dtos.getUtenteCreazione(),
                                    2
                            );
                        }

                        // Cambia stato a CONCLUSA_SORV (7) solo se follow-up NON previsto
                        if (Boolean.FALSE.equals(dtos.getFollowUpPrevisto())) {
                            anagraficaDTO.setSoggettoStatoId(7);
                            aggiornaSoggetto(anagraficaDTO.getSoggettoId(), anagraficaDTO, dtos.getUtenteCreazione(), null);

                            tracciaElaborazioneRepository.inserisciFileImpatto(
                                    dsl,
                                    elaborazioneId,
                                    "REEA_S_SOGGETTO",
                                    dtos.getRegSpresalEsitiId().intValue(),
                                    1,
                                    dtos.getUtenteCreazione()
                            );
                            tracciaElaborazioneRepository.inserisciFileImpatto(
                                    dsl,
                                    elaborazioneId,
                                    "REEA_T_SOGGETTO",
                                    dtos.getRegSpresalEsitiId().intValue(),
                                    2,
                                    dtos.getUtenteCreazione()
                            );
                        }
                    }
                }
            }

            return variabileFinale;

        } catch (Exception e) {
            System.err.println("Errore in creazioneAnagraficaSpresalEsiti per CF "
                    + dtos.getCodiceFiscale() + ": " + e.getMessage());
//            e.printStackTrace();
            LOGGER.error("Errore in creazioneAnagraficaSpresalEsiti per CF: ", dtos.getCodiceFiscale(), e);
            throw new RuntimeException("Errore in creazioneAnagraficaSpresalEsiti", e);
        }
    }
    
    
    @Override
    @Transactional
    public Integer creazioneAnagraficaSpresalAnamnesi(AnagraficaDTO dtos, Integer elaborazioneId) {

        try {
            boolean uguali = false;
            DSLContext ctx = dsl;
            AnagraficaDTO anagraficaDTO = getByCodiceFiscale(dtos.getCodiceFiscale());

            System.out.println(">> SPRESAL ANAMNESI CF=" + dtos.getCodiceFiscale()
                    + " | statoId=" + (anagraficaDTO != null ? anagraficaDTO.getSoggettoStatoId() : "NULL"));

            Integer registroId = null;

            // inizializza SEMPRE i nested DTO sul dto in ingresso
            RegistroDTO registro = RegistroUtils.ensureRegistro(dtos);
            SpresalDTO spresal  = SpresalUtils.ensureSpresal(dtos);

            //
            List<StoriaStatoDTO> statoDTOs = null;
            if (anagraficaDTO != null)
            	statoDTOs = anagraficaRepository.findStoriaStati(anagraficaDTO.getSoggettoId().intValue());
            StoriaStatoDTO corrente = anagraficaRepository.findStatoCorrente(anagraficaDTO.getSoggettoId().intValue());
            if(statoDTOs == null)
            	statoDTOs = new ArrayList<StoriaStatoDTO>();
            statoDTOs.add(corrente);
            Set<String> statiDaSbloccare = Set.of("4", "5", "6");
            Set<Integer> statiDaSbloccareInteger = Set.of(4, 5, 6);

            if (anagraficaDTO != null
                  && statoDTOs != null &&
                    (statoDTOs.stream()
                            .map(StoriaStatoDTO::getSoggettoStatoId)
                            .anyMatch(statiDaSbloccare::contains) || (anagraficaDTO.getSoggettoStatoId() != null && statiDaSbloccareInteger.contains(anagraficaDTO.getSoggettoStatoId())))) {
            //
            
            // & controllare se stato PRESO_IN_CARICO o AVVIATO_SORV o ESCLUSO_SORV
//            if (anagraficaDTO != null
//                    && anagraficaDTO.getSoggettoStatoId() != null
//                    && List.of(4, 5, 6).contains(anagraficaDTO.getSoggettoStatoId())) {

                // CASO 1: soggetto presente nel REEA
                boolean verifica = controlloObbligatorietaDatiFile(dtos);
                System.out.println(">> verifica=" + verifica + " | CF=" + dtos.getCodiceFiscale());

                if (verifica) {

                    // -----------------------------
                    // Calcolo stato sanitario / sezione
                    // -----------------------------
                    if (Boolean.TRUE.equals(spresal.getAnamnesiEsposizioneAmianto())
                            && spresal.getEsposizioneProfessionale() != null
                            && List.of("CERTA", "POSSIBILE", "PROBABILE")
                                    .contains(spresal.getEsposizioneProfessionale())) {

                        registro.setAttSanitariaSpresal(true);

                        if (Boolean.TRUE.equals(spresal.getInserimentoInSorveglianza())) {
                            dtos.setSoggettoStatoId(5);
                        } else {
                            dtos.setSoggettoStatoId(6);
                        }

                    } else if (Boolean.FALSE.equals(spresal.getAnamnesiEsposizioneAmianto())) {
                        dtos.setSoggettoStatoId(6);
                    }

                    dtos.setFonteId(5);

                    if (Boolean.TRUE.equals(registro.getAttSanitariaSpresal())
                            || Boolean.TRUE.equals(registro.getAttSanitariaInail())) {
                        registro.setAttSanitariaStato(true);
                    } else {
                        registro.setAttSanitariaStato(false);
                    }

                    if (registro.getAttSanitariaSpresalData() == null
                            && Boolean.TRUE.equals(registro.getAttSanitariaSpresal())) {

                        registro.setAttSanitariaSpresalData(
                                registro.getDataCreazione() != null
                                        ? registro.getDataCreazione().toLocalDate()
                                        : LocalDate.now()
                        );
                    }

                    if (Boolean.TRUE.equals(registro.getAttSanitariaSpresal())) {
                        registro.setSezione(1);
                    } else if (!Boolean.TRUE.equals(registro.getAttSanitariaSpresal())
                            && Boolean.TRUE.equals(registro.getAttSanitariaInail())) {
                        registro.setSezione(2);
                    } else {
                        registro.setSezione(3);
                    }

                    // versione/registroId a partire dall'anagrafica esistente
                    RegistroDTO registroAnagrafica = RegistroUtils.ensureRegistro(anagraficaDTO);
                    registro.setVersioneNumero(registroAnagrafica.getVersioneNumero() + 1);
                    registro.setRegistroId(registroAnagrafica.getRegistroId());

                    // -----------------------------
                    // Aggiornamenti in base allo stato
                    // -----------------------------
                    if (Integer.valueOf(4).equals(anagraficaDTO.getSoggettoStatoId())) {

                        aggiornaSoggetto(anagraficaDTO.getSoggettoId(), dtos, dtos.getUtenteCreazione(), null);
                        tracciaElaborazioneRepository.inserisciFileImpatto(
                                ctx,
                                elaborazioneId,
                                "REEA_S_SOGGETTO",
                                dtos.getSpresal().getRegSpresalAnamnesiId().intValue(),
                                1,
                                dtos.getUtenteCreazione()
                        );
                        tracciaElaborazioneRepository.inserisciFileImpatto(
                                ctx,
                                elaborazioneId,
                                "REEA_T_SOGGETTO",
                                dtos.getSpresal().getRegSpresalAnamnesiId().intValue(),
                                2,
                                dtos.getUtenteCreazione()
                        );
                        registroId = anagraficaRepository.updateRegistro(
                                ctx,
                                anagraficaDTO.getSoggettoId().intValue(),
                                dtos,
                                elaborazioneId,
                                dtos.getUtenteCreazione(),
                                2
                        );

                        spresalRepository.aggiornaSpresalRegistroId(dtos, elaborazioneId, dtos.getUtenteCreazione(), 2);
                        spresalService.aggiornaSpresalAnamnesiFlagElaboratoIndipendente(dtos);

                    } else if (Integer.valueOf(5).equals(anagraficaDTO.getSoggettoStatoId())
                            || Integer.valueOf(6).equals(anagraficaDTO.getSoggettoStatoId())) {

                        SpresalDTO oggettoRecuperatoIdRegistroNotNull =
                                spresalRepository.recuperaOggettoTRegistroSpresalAnamnesiByCF(
                                        dtos.getCodiceFiscale(),
                                        true,
                                        dtos.getSpresal().getOccupazioneNum(),
                                        true
                                );

                        SpresalDTO oggettoRecuperatoIdRegistroNull =
                                spresalRepository.recuperaOggettoTRegistroSpresalAnamnesiByCF(
                                        dtos.getCodiceFiscale(),
                                        false,
                                        dtos.getSpresal().getOccupazioneNum(),
                                        false
                                );

                        if (oggettoRecuperatoIdRegistroNotNull != null
                                && oggettoRecuperatoIdRegistroNull != null) {
                            uguali = stessiValoriSpresal(
                                    oggettoRecuperatoIdRegistroNotNull,
                                    oggettoRecuperatoIdRegistroNull
                            );
                        }

                        spresalService.aggiornaSpresalAnamnesiFlagElaboratoIndipendente(dtos);

                        if (!uguali) {

                            aggiornaSoggetto(anagraficaDTO.getSoggettoId(), dtos, dtos.getUtenteCreazione(), null);
                            tracciaElaborazioneRepository.inserisciFileImpatto(
                                    ctx,
                                    elaborazioneId,
                                    "REEA_S_SOGGETTO",
                                    dtos.getSpresal().getRegSpresalAnamnesiId().intValue(),
                                    1,
                                    dtos.getUtenteCreazione()
                            );
                            tracciaElaborazioneRepository.inserisciFileImpatto(
                                    ctx,
                                    elaborazioneId,
                                    "REEA_T_SOGGETTO",
                                    dtos.getSpresal().getRegSpresalAnamnesiId().intValue(),
                                    2,
                                    dtos.getUtenteCreazione()
                            );
                            registroId = anagraficaRepository.updateRegistro(
                                    ctx,
                                    anagraficaDTO.getSoggettoId().intValue(),
                                    dtos,
                                    elaborazioneId,
                                    dtos.getUtenteCreazione(),
                                    2
                            );
                            spresalRepository.aggiornaSpresalRegistroId(dtos, elaborazioneId, dtos.getUtenteCreazione(), 2);
                            if (oggettoRecuperatoIdRegistroNotNull != null
                                    && oggettoRecuperatoIdRegistroNotNull.getValiditaFine() == null) {
                                spresalRepository.chiudiRecordDuplicatoSpresal(
                                        oggettoRecuperatoIdRegistroNotNull,
                                        elaborazioneId,
                                        dtos.getUtenteCreazione(),
                                        2
                                );
                            }
                        }
                    }
                }
            }

            return registroId;

        } catch (Exception e) {
            System.err.println("Errore in creazioneAnagraficaSpresalAnamnesi per CF "
                    + dtos.getCodiceFiscale() + ": " + e.getMessage());
//            e.printStackTrace();
            LOGGER.error("Errore in creazioneAnagraficaSpresalAnamnesi per CF: ", dtos.getCodiceFiscale(), e);
            // rilancio RuntimeException per permettere il rollback di @Transactional
            throw new RuntimeException("Errore in creazioneAnagraficaSpresalAnamnesi", e);
        }
    }
    
    
    private boolean controlloObbligatorietaDatiFile(AnagraficaDTO anagraficaDTO) {
        SpresalDTO spresal = SpresalUtils.ensureSpresal(anagraficaDTO);

        if (spresal.getDataIntervista() == null
                || spresal.getAnamnesiEsposizioneAmianto() == null
                || spresal.getInserimentoInSorveglianza() == null
                || spresal.getIdSpresal() == null) {
            return false;
        }

        if (Boolean.TRUE.equals(spresal.getAnamnesiEsposizioneAmianto())) {
            return spresal.getEsposizioneProfessionale() != null
                    && spresal.getAnnoFineEsposizione() != null
                    && spresal.getLivelloEsposizione() != null;
        }

        return true;
    }
    
    
    @Override
    @Transactional
    public Integer creazioneAnagraficaInailA(AnagraficaDTO dtos, Integer elaborazioneId) {

        try {
            DSLContext ctx = dsl;
            AnagraficaDTO anagraficaDTO = getByCodiceFiscale(dtos.getCodiceFiscale());
            Integer soggettoId = null;
            AnagraficaDTO anagraficaDTOIntegrazioneAura = null;

            if (anagraficaDTO == null) {
                // CASO 1: soggetto NON presente nel REEA

                // AURA lookup (una sola volta)
                SoggettoAuraMsg sAuraMsg = null;
                String flagFind = "1";

                DatiAnagraficiMsg dAuraMsg =
                        anagrafeFindClient.find(flagFind, dtos.getCodiceFiscale(), null, null);

                var elencoProfili = SafeAccess.safeGet(() -> dAuraMsg.getBody().getElencoProfili());
                var primoProfilo = SafeAccess.safeGet(() -> elencoProfili.getDatianagrafici().get(0));

                if (primoProfilo != null && primoProfilo.getIdProfiloAnagrafico() != null) {
                    sAuraMsg = anagrafeGetClient.get(
                            primoProfilo.getIdProfiloAnagrafico().toString());
                }

                if (sAuraMsg != null) {
                    anagraficaDTOIntegrazioneAura = popolaAnagraficaDTObyAura(dtos, sAuraMsg);
                }

                if (anagraficaDTOIntegrazioneAura == null
                        || anagraficaDTOIntegrazioneAura.getDomicilioAslCod() == null || aslRepository.getAslIdByAslCod(anagraficaDTOIntegrazioneAura.getDomicilioAslCod()) == null) {
                    dtos.setAssistenzaAslCod("999999");
                    dtos.setDomicilioAslCod(dtos.getAssistenzaAslCod());
                    dtos.setResidenzaAslCod(dtos.getAssistenzaAslCod());
                }

                AnagraficaDTO nuovoDtoFinale =
                        anagraficaDTOIntegrazioneAura != null ? anagraficaDTOIntegrazioneAura : dtos;

                // inizializza sempre il RegistroDTO sul DTO finale
                RegistroDTO registroFinale = RegistroUtils.ensureRegistro(nuovoDtoFinale);

                // Tipo inserimento, fonte e stato
                nuovoDtoFinale.setInserimentoTipoId(2);
                nuovoDtoFinale.setFonteId(3);
                if(nuovoDtoFinale.getSoggettoStatoId() == null)
                	nuovoDtoFinale.setSoggettoStatoId(10);

                registroFinale.setSezione(2);
                registroFinale.setAttSanitariaStato(true);
                nuovoDtoFinale.setAttSantariaInail(true);
                registroFinale.setTipoElencoInail("A");
                nuovoDtoFinale.setRegInailId(dtos.getRegInailId());

                // Risoluzione ASL
                if (nuovoDtoFinale.getDomicilioAslId() == null) {
                    nuovoDtoFinale.setDomicilioAslId(
                            aslRepository.getAslIdByAslCod(nuovoDtoFinale.getDomicilioAslCod()));
                }
                if (nuovoDtoFinale.getDomicilioAslId() == null) {
                	nuovoDtoFinale.setDomicilioAslId(1174);
                }

                nuovoDtoFinale.setResidenzaAslId(
                        aslRepository.getAslIdByAslCod(nuovoDtoFinale.getResidenzaAslCod()));
                
                if(nuovoDtoFinale.getResidenzaAslId() == null)
                	nuovoDtoFinale.setResidenzaAslId(1174);
                
                if (anagraficaDTOIntegrazioneAura == null) {
                    nuovoDtoFinale.setAssistenzaAslId(
                            nuovoDtoFinale.getDomicilioAslId().toString());
                } else {
                    String assistenzaCod = nuovoDtoFinale.getAssistenzaAslId();
                    Integer assistenzaId =
                            (assistenzaCod != null && !assistenzaCod.isBlank())
                                    ? aslRepository.getAslIdByAslCod(assistenzaCod)
                                    : null;
                    nuovoDtoFinale.setAssistenzaAslId(
                            assistenzaId != null
                                    ? String.format("%06d", assistenzaId)
                                    : nuovoDtoFinale.getDomicilioAslId().toString());
                }

                // Inserimenti
                soggettoId = anagraficaRepository.inserisciSoggetto(
                        ctx, nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 1);
                if (soggettoId == null) {
                    throw new IllegalStateException("Inserimento soggetto fallito");
                }

                nuovoDtoFinale.setSoggettoId(Long.valueOf(soggettoId));

                Integer registroId =
                        anagraficaRepository.inserisciRegistroInail(
                                ctx, soggettoId, nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 1);
                if (registroId == null) {
                    throw new IllegalStateException("Inserimento registro fallito");
                }

                registroFinale.setRegistroId(registroId);

                inailRepository.aggiornaInailRegistroId(nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 2);

            } else {
                // CASO 2: soggetto gia PRESENTE nel REEA

                RegistroDTO registroEsistente = RegistroUtils.ensureRegistro(anagraficaDTO);

                anagraficaDTO.setFonteId(3);
                if (registroEsistente.getSezione() != 1) {
                    registroEsistente.setSezione(2);
                }
                registroEsistente.setAttSanitariaStato(true);
                anagraficaDTO.setAttSantariaInail(true);
                registroEsistente.setTipoElencoInail("A");
                anagraficaDTO.setRegInailId(dtos.getRegInailId());
                registroEsistente.setVersioneNumero(
                        registroEsistente.getVersioneNumero() + 1);

                List<FonteConDataDTO> listaFontiStorico =
                        fonteRepository.findListaFonteDescByIdSoggettoInail(
                                anagraficaDTO.getSoggettoId());

                boolean presente = listaFontiStorico.stream()
                        .anyMatch(f -> f.getFonteDesc() != null
                                && f.getFonteDesc().toUpperCase().contains("INAIL"));

                if (!presente) {
                    OffsetDateTime now = OffsetDateTime.now();
                    registroEsistente.setDataModifica(now);

                    aggiornaSoggetto(anagraficaDTO.getSoggettoId(), anagraficaDTO, dtos.getUtenteCreazione(), null);
                    tracciaElaborazioneRepository.inserisciFileImpatto(
                            ctx,
                            elaborazioneId,
                            "REEA_S_SOGGETTO",
                            dtos.getRegInailId(),
                            1,
                            dtos.getUtenteCreazione()
                    );
                    tracciaElaborazioneRepository.inserisciFileImpatto(
                            ctx,
                            elaborazioneId,
                            "REEA_T_SOGGETTO",
                            dtos.getRegInailId(),
                            2,
                            dtos.getUtenteCreazione()
                    );

                    Integer registroId = anagraficaRepository.updateRegistro(
                            ctx,
                            anagraficaDTO.getSoggettoId().intValue(),
                            anagraficaDTO,
                            elaborazioneId,
                            dtos.getUtenteCreazione(),
                            2);
                    if (registroId == null) {
                        throw new IllegalStateException("Inserimento registro fallito");
                    }

                    registroEsistente.setRegistroId(registroId);
                    inailRepository.aggiornaInailRegistroId(anagraficaDTO, elaborazioneId, dtos.getUtenteCreazione(), 2);
                    soggettoId = anagraficaDTO.getSoggettoId().intValue();
                }
                else
                	//condizione per cui l'utente esiste ma e' duplicato e quindi viene scartato
                	soggettoId = 0;

//                soggettoId = anagraficaDTO.getSoggettoId().intValue();
            }

            return soggettoId;

        } catch (Exception e) {
            System.err.println("Errore in creazioneAnagraficaInailA per CF "
                    + dtos.getCodiceFiscale() + ": " + e.getMessage());
//            e.printStackTrace();
            LOGGER.error("Errore in creazioneAnagraficaInailA per CF: ", dtos.getCodiceFiscale(), e);
            throw new RuntimeException("Errore in creazioneAnagraficaInailA", e);
        }
    }
  
    
    @Override
    @Transactional
    public Integer creazioneAnagraficaInailB(AnagraficaDTO dtos, Integer elaborazioneId) {

        try {
            DSLContext ctx = dsl;
            AnagraficaDTO anagraficaDTO = getByCodiceFiscale(dtos.getCodiceFiscale());
            Integer soggettoId = null;
            AnagraficaDTO anagraficaDTOIntegrazioneAura = null;

            if (anagraficaDTO == null) {
                // CASO 1: soggetto NON presente nel REEA

                // AURA lookup (una sola volta)
                SoggettoAuraMsg sAuraMsg = null;
                String flagFind = "1";

                DatiAnagraficiMsg dAuraMsg =
                        anagrafeFindClient.find(flagFind, dtos.getCodiceFiscale(), null, null);

                var elencoProfili = SafeAccess.safeGet(() -> dAuraMsg.getBody().getElencoProfili());
                var primoProfilo = SafeAccess.safeGet(() -> elencoProfili.getDatianagrafici().get(0));

                if (primoProfilo != null && primoProfilo.getIdProfiloAnagrafico() != null) {
                    sAuraMsg = anagrafeGetClient.get(
                            primoProfilo.getIdProfiloAnagrafico().toString()
                    );
                }

                if (sAuraMsg != null) {
                    anagraficaDTOIntegrazioneAura = popolaAnagraficaDTObyAura(dtos, sAuraMsg);
                }

                if (anagraficaDTOIntegrazioneAura == null
                        || anagraficaDTOIntegrazioneAura.getDomicilioAslCod() == null || aslRepository.getAslIdByAslCod(anagraficaDTOIntegrazioneAura.getDomicilioAslCod()) == null) {
                    dtos.setAssistenzaAslCod("999999");
                    dtos.setDomicilioAslCod(dtos.getAssistenzaAslCod());
                    dtos.setResidenzaAslCod(dtos.getAssistenzaAslCod());
                }

                AnagraficaDTO nuovoDtoFinale =
                        anagraficaDTOIntegrazioneAura != null ? anagraficaDTOIntegrazioneAura : dtos;

                // inizializza sempre il RegistroDTO sul DTO finale
                RegistroDTO registroFinale = RegistroUtils.ensureRegistro(nuovoDtoFinale);

                // Tipo inserimento, fonte e stato
                nuovoDtoFinale.setInserimentoTipoId(2);
                nuovoDtoFinale.setFonteId(3);
                if(nuovoDtoFinale.getSoggettoStatoId() == null)
                	nuovoDtoFinale.setSoggettoStatoId(10);

                registroFinale.setSezione(3);
                registroFinale.setAttSanitariaStato(null);
                nuovoDtoFinale.setAttSantariaInail(null);
                registroFinale.setTipoElencoInail("B");
                nuovoDtoFinale.setRegInailId(dtos.getRegInailId());

                // Risoluzione ASL
                if (nuovoDtoFinale.getDomicilioAslId() == null) {
                    nuovoDtoFinale.setDomicilioAslId(
                            aslRepository.getAslIdByAslCod(nuovoDtoFinale.getDomicilioAslCod())
                    );
                }
                if (nuovoDtoFinale.getDomicilioAslId() == null) {
                	nuovoDtoFinale.setDomicilioAslId(1174);
                }

                nuovoDtoFinale.setResidenzaAslId(
                        aslRepository.getAslIdByAslCod(nuovoDtoFinale.getResidenzaAslCod())
                );
                
                if(nuovoDtoFinale.getResidenzaAslId() == null) {
                	nuovoDtoFinale.setResidenzaAslId(1174);
                }

                if (anagraficaDTOIntegrazioneAura == null) {
                    nuovoDtoFinale.setAssistenzaAslId(
                            nuovoDtoFinale.getDomicilioAslId().toString()
                    );
                } else {
                    String assistenzaCod = nuovoDtoFinale.getAssistenzaAslId();
                    Integer assistenzaId =
                            (assistenzaCod != null && !assistenzaCod.isBlank())
                                    ? aslRepository.getAslIdByAslCod(assistenzaCod)
                                    : null;

                    nuovoDtoFinale.setAssistenzaAslId(
                            assistenzaId != null
                                    ? String.format("%06d", assistenzaId)
                                    : nuovoDtoFinale.getDomicilioAslId().toString()
                    );
                }

                // Inserimenti
                soggettoId = anagraficaRepository.inserisciSoggetto(
                        ctx, nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 1);
                if (soggettoId == null) {
                    throw new IllegalStateException("Inserimento soggetto fallito");
                }

                nuovoDtoFinale.setSoggettoId(Long.valueOf(soggettoId));

                Integer registroId = anagraficaRepository.inserisciRegistroInail(
                        ctx, soggettoId, nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 1);
                if (registroId == null) {
                    throw new IllegalStateException("Inserimento registro fallito");
                }

                registroFinale.setRegistroId(registroId);

                inailRepository.aggiornaInailRegistroId(nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 2);

            } else {
                // CASO 2: soggetto gia PRESENTE nel REEA

                RegistroDTO registroEsistente = RegistroUtils.ensureRegistro(anagraficaDTO);

                anagraficaDTO.setFonteId(3);
                registroEsistente.setTipoElencoInail("B");
                anagraficaDTO.setRegInailId(dtos.getRegInailId());
                registroEsistente.setVersioneNumero(
                        registroEsistente.getVersioneNumero() + 1
                );

                List<FonteConDataDTO> listaFontiStorico =
                        fonteRepository.findListaFonteDescByIdSoggettoInail(
                                anagraficaDTO.getSoggettoId()
                        );

                boolean presente = listaFontiStorico.stream()
                        .anyMatch(f -> f.getFonteDesc() != null
                                && f.getFonteDesc().toUpperCase().contains("INAIL"));

                if (!presente) {
                    OffsetDateTime now = OffsetDateTime.now();
                    registroEsistente.setDataModifica(now);

                    aggiornaSoggetto(anagraficaDTO.getSoggettoId(), anagraficaDTO, dtos.getUtenteCreazione(), null);
                    tracciaElaborazioneRepository.inserisciFileImpatto(
                            ctx,
                            elaborazioneId,
                            "REEA_S_SOGGETTO",
                            dtos.getRegInailId(),
                            1,
                            dtos.getUtenteCreazione()
                    );
                    tracciaElaborazioneRepository.inserisciFileImpatto(
                            ctx,
                            elaborazioneId,
                            "REEA_T_SOGGETTO",
                            dtos.getRegInailId(),
                            2,
                            dtos.getUtenteCreazione()
                    );

                    Integer registroId = anagraficaRepository.updateRegistro(
                            ctx,
                            anagraficaDTO.getSoggettoId().intValue(),
                            anagraficaDTO,
                            elaborazioneId,
                            dtos.getUtenteCreazione(),
                            2
                    );
                    if (registroId == null) {
                        throw new IllegalStateException("Inserimento registro fallito");
                    }

                    registroEsistente.setRegistroId(registroId);
                    inailRepository.aggiornaInailRegistroId(anagraficaDTO, elaborazioneId, dtos.getUtenteCreazione(), 2);

                }
                else
                	//condizione per cui l'utente esiste ma e' duplicato e quindi viene scartato
                	soggettoId = 0;

//                soggettoId = anagraficaDTO.getSoggettoId().intValue();
            }

            return soggettoId;

        } catch (Exception e) {
            System.err.println("Errore in creazioneAnagraficaInailB per CF "
                    + dtos.getCodiceFiscale() + ": " + e.getMessage());
//            e.printStackTrace();
            LOGGER.error("Errore in creazioneAnagraficaInailB per CF: ", dtos.getCodiceFiscale(), e);
            throw new RuntimeException("Errore in creazioneAnagraficaInailB", e);
        }
    }
    
    
    @Override
    @Transactional
    public Integer creazioneAnagrafica(AnagraficaDTO dtos,
                                       Boolean flagInserimentoDaFile,
                                       Integer elaborazioneId, AuditLogRequest auditLogRequest) {

        DSLContext ctx = dsl;
        Integer soggettoId = null;

        try {
            AnagraficaDTO anagraficaDTO = getByCodiceFiscale(dtos.getCodiceFiscale());
            AnagraficaDTO anagraficaDTOIntegrazioneAura = null;

            if (anagraficaDTO == null) {
                // ============================
                // CASO 1: soggetto NON presente nel REEA
                // ============================

                // AURA lookup
                SoggettoAuraMsg sAuraMsg = null;
                String flagFind = "1";

                if (dtos.getIdAura() != null) {
                    sAuraMsg = anagrafeGetClient.get(dtos.getIdAura());
                    if (sAuraMsg != null) {
                        anagraficaDTOIntegrazioneAura = popolaAnagraficaDTObyAura(dtos, sAuraMsg);
                    }
                } else {
                    DatiAnagraficiMsg dAuraMsg =
                            anagrafeFindClient.find(flagFind, dtos.getCodiceFiscale(), null, null);
                    var elencoProfili =
                            SafeAccess.safeGet(() -> dAuraMsg.getBody().getElencoProfili());
                    var primoProfilo =
                            SafeAccess.safeGet(() -> elencoProfili.getDatianagrafici().get(0));

                    if (primoProfilo != null && primoProfilo.getIdProfiloAnagrafico() != null) {
                        sAuraMsg = anagrafeGetClient.get(
                                primoProfilo.getIdProfiloAnagrafico().toString());
                    }
                    if (sAuraMsg != null) {
                        anagraficaDTOIntegrazioneAura = popolaAnagraficaDTObyAura(dtos, sAuraMsg);
                    }
                }

                AnagraficaDTO nuovoDtoFinale =
                        (anagraficaDTOIntegrazioneAura != null) ? anagraficaDTOIntegrazioneAura : dtos;

                recuperaInfoProvinciaComune(nuovoDtoFinale);
                // inizializza sempre il RegistroDTO sul DTO finale
                RegistroDTO registroFinale = RegistroUtils.ensureRegistro(nuovoDtoFinale);

                // Tipo inserimento, fonte e stato
                if (Boolean.TRUE.equals(flagInserimentoDaFile)) {
                    nuovoDtoFinale.setInserimentoTipoId(2);

                    // Usa fonteId passato dal chiamante, default 2 (Preadesioni)
                    if (nuovoDtoFinale.getFonteId() == null) {
                        nuovoDtoFinale.setFonteId(2);
                    }

                    Integer fonteId = nuovoDtoFinale.getFonteId();

                    // NPLA (4): AURA obbligatoria
                    if (Integer.valueOf(4).equals(fonteId) && anagraficaDTOIntegrazioneAura == null) {
                        throw new IllegalStateException("CF non trovato in AURA");
                    }

                    // Stato in base alla fonte
                    if (Integer.valueOf(3).equals(fonteId) || Integer.valueOf(4).equals(fonteId)) {
                        // INAIL (3) e NPLA (4)  Caricato
                    	if(nuovoDtoFinale.getSoggettoStatoId() == null)
                    		nuovoDtoFinale.setSoggettoStatoId(10);
                    } else {
                        // Preadesioni e altri  logica AURA
//                        if (sAuraMsg != null
//                                && sAuraMsg.getBody().getInfoSan() != null
//                                && sAuraMsg.getBody().getInfoSan().getDataFineASL() != null
//                                && !sAuraMsg.getBody().getInfoSan().getDataFineASL().toString()
//                                        .contains("9999")) {
//                            nuovoDtoFinale.setSoggettoStatoId(9);
                    	if (sAuraMsg != null
                    	        && sAuraMsg.getBody() != null
                    	        && sAuraMsg.getBody().getInfoSan() != null
                    	        && sAuraMsg.getBody()
                    	                .getInfoSan()
                    	                .getDataFineASL() != null) {

                    	    XMLGregorianCalendar dataFineAslXml =
                    	            sAuraMsg.getBody()
                    	                    .getInfoSan()
                    	                    .getDataFineASL();

                    	    LocalDate dataFineAsl =
                    	            LocalDate.of(
                    	                    dataFineAslXml.getYear(),
                    	                    dataFineAslXml.getMonth(),
                    	                    dataFineAslXml.getDay()
                    	            );

                    	    boolean dataValida =
                    	            !dataFineAslXml.toString().contains("9999");

                    	    boolean dataScaduta =
                    	            !dataFineAsl.isAfter(LocalDate.now());

                    	    if (dataValida
                    	            && dataScaduta) {

                    	        nuovoDtoFinale.setSoggettoStatoId(
                    	                9
                    	        );
                    	    }
                    	    else {
                                nuovoDtoFinale.setSoggettoStatoId(1);
                    	    } 
                        }
                    	else {
                            nuovoDtoFinale.setSoggettoStatoId(1);
                	    } 
                    }

                } else {
                    nuovoDtoFinale.setInserimentoTipoId(1);
                    nuovoDtoFinale.setFonteId(1);
                    nuovoDtoFinale.setUtenteCreazione(auditLogRequest.getUtente());

                    if (sAuraMsg != null) {
                        XMLGregorianCalendar dataDecesso =
                                sAuraMsg.getBody().getInfoAnag().getDatiPrimari().getDataDecesso();
                        XMLGregorianCalendar dataFineAsl =
                                sAuraMsg.getBody().getInfoSan().getDataFineASL();

                        if (dataDecesso != null) {
                            nuovoDtoFinale.setSoggettoStatoId(8); // ESCLUSO_DECESSO
                        } 
//                        else if (dataFineAsl != null
//                                && !dataFineAsl.toString().contains("9999")) {
//                            nuovoDtoFinale.setSoggettoStatoId(9); // ESCLUSO_EMIGRAZIONE
                        LocalDate dataFineAslLocalDate =
                                LocalDate.of(
                                        dataFineAsl.getYear(),
                                        dataFineAsl.getMonth(),
                                        dataFineAsl.getDay()
                                );

                        boolean dataValida =
                                !dataFineAsl.toString().contains("9999");

                        boolean dataScaduta =
                                !dataFineAslLocalDate.isAfter(LocalDate.now());

                        if (dataValida
                                && dataScaduta) {

                            nuovoDtoFinale.setSoggettoStatoId(
                                    9
                            ); // EMIGRATA
                        } else {
                            nuovoDtoFinale.setSoggettoStatoId(2); // ELEGGIBILE
                        }
                    } else {
                        nuovoDtoFinale.setSoggettoStatoId(2);
                    }
                }

                // sezione default
                registroFinale.setSezione(3);

                // Risoluzione ASL
                if (nuovoDtoFinale.getDomicilioAslId() == null) {
                    String domicilioAslCod = nuovoDtoFinale.getDomicilioAslCod();

                    if (domicilioAslCod != null && !domicilioAslCod.isBlank()) {
                        String domicilioAslCodFormattato = String.format(
                                "%6s",
                                domicilioAslCod.trim()
                        ).replace(' ', '0');

                        nuovoDtoFinale.setDomicilioAslId(
                                aslRepository.getAslIdByAslCod(domicilioAslCodFormattato)
                        );
                    }
                }
//                if (nuovoDtoFinale.getDomicilioAslId() == null) {
//                    nuovoDtoFinale.setDomicilioAslId(
//                            aslRepository.getAslIdByAslCod(nuovoDtoFinale.getDomicilioAslCod()));
//                }
                
//                if(nuovoDtoFinale.getDomicilioAslId() == null)
//                	nuovoDtoFinale.setDomicilioAslId(1174);

                // Fallback ASL per deceduti/emigrati senza ASL da AURA
                if (nuovoDtoFinale.getDomicilioAslId() == null) {
//                        && (Integer.valueOf(8).equals(nuovoDtoFinale.getSoggettoStatoId())
//                        || Integer.valueOf(9).equals(nuovoDtoFinale.getSoggettoStatoId()))) {
                    Integer fallbackId = aslRepository.getAslIdByAslCod("999999");
                    nuovoDtoFinale.setDomicilioAslId(fallbackId);
//                    nuovoDtoFinale.setResidenzaAslId(fallbackId);
                }
                if (nuovoDtoFinale.getDomicilioAslId() == null) {
                    throw new IllegalStateException("Il domicilio_asl_id non puï¿½ essere null");
                }

//                nuovoDtoFinale.setResidenzaAslId(
//                        aslRepository.getAslIdByAslCod(nuovoDtoFinale.getResidenzaAslCod()));
                String residenzaAslCod = nuovoDtoFinale.getResidenzaAslCod();

                if (residenzaAslCod != null && !residenzaAslCod.isBlank()) {
                    String residenzaAslCodFormattato = String.format(
                            "%6s",
                            residenzaAslCod.trim()
                    ).replace(' ', '0');

                    nuovoDtoFinale.setResidenzaAslId(
                            aslRepository.getAslIdByAslCod(residenzaAslCodFormattato)
                    );
                }
                
                if(nuovoDtoFinale.getResidenzaAslId() == null) {
//                	nuovoDtoFinale.setResidenzaAslId(1174);
                	Integer fallbackId = aslRepository.getAslIdByAslCod("999999");
                	nuovoDtoFinale.setResidenzaAslId(fallbackId);
                }

                if (anagraficaDTOIntegrazioneAura == null) {
                    nuovoDtoFinale.setAssistenzaAslId(
                            nuovoDtoFinale.getDomicilioAslId().toString());
                } else {
                    String assistenzaCod = nuovoDtoFinale.getAssistenzaAslId();
                    Integer assistenzaId =
                            (assistenzaCod != null && !assistenzaCod.isBlank())
                                    ? aslRepository.getAslIdByAslCod(assistenzaCod)
                                    : null;
                    nuovoDtoFinale.setAssistenzaAslId(
                            assistenzaId != null
                                    ? String.format("%06d", assistenzaId)
                                    : nuovoDtoFinale.getDomicilioAslId().toString());
                }
                
                if(nuovoDtoFinale.getAssistenzaAslId() == null) {
                	Integer fallbackId = aslRepository.getAslIdByAslCod("999999");
                	nuovoDtoFinale.setAssistenzaAslId(fallbackId.toString());
                }

                if (Boolean.FALSE.equals(flagInserimentoDaFile)
                        && dtos.getAdesioneData() != null) {
                    nuovoDtoFinale.setPresentazioneIstanzaData(dtos.getAdesioneData());
                }
                
                
                // Inserimenti
                soggettoId = anagraficaRepository.inserisciSoggetto(
                        ctx, nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 1);
                if (soggettoId == null) {
                    throw new IllegalStateException("Inserimento soggetto fallito");
                }

                nuovoDtoFinale.setSoggettoId(Long.valueOf(soggettoId));
                nuovoDtoFinale.setAdesioneId(dtos.getAdesioneId());

                Integer registroId =
                        anagraficaRepository.inserisciRegistro(
                                ctx, soggettoId, nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 1);
                if (registroId == null) {
                    throw new IllegalStateException("Inserimento registro fallito");
                }

                Integer esposizioneId =
                        anagraficaRepository.inserisciEsposizioni(
                                ctx, soggettoId, nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 1);
                if (esposizioneId == null) {
                    throw new IllegalStateException("Inserimento esposizione fallito");
                }

                if (nuovoDtoFinale.getAdesioneId() != null) {
                    Integer aggiornamentoAdesione =
                            adesioneRepository.aggiornaAdesioniIdSoggetto(
                                    nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 2);
                    System.out.println("aggiornamentoAdesione: " + aggiornamentoAdesione);
                }

                if (nuovoDtoFinale.getListaEsenzione() != null
                        && !nuovoDtoFinale.getListaEsenzione().isEmpty()) {
                    Integer esenzioneInsId =
                            anagraficaRepository.inserisciEsenzioni(
                                    ctx, soggettoId, nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 1);
                    if (esenzioneInsId == null) {
                        throw new IllegalStateException("Inserimento esenzione fallito");
                    }
                }

//                if (Boolean.TRUE.equals(flagInserimentoDaFile)) {
//                    adesioneRepository.aggiornaAdesioniIdSoggetto(
//                            nuovoDtoFinale, elaborazioneId, "ADMIN", 2);
//                }
                adesioneRepository.aggiornaAdesioniIdSoggetto(
                        nuovoDtoFinale, elaborazioneId, dtos.getUtenteCreazione(), 2);
                
                
                //
        		if(auditLogRequest != null) {

	                auditLogRequest.setOperazione("Inserisci assistito");
	                auditLogRequest.setOggOper(JsonNullable.of("Inserimento manuale"));
	                auditLogRequest.setKeyOper(null);
	                auditLogRequest.setRequestPayload(null);
	                auditLogRequest.setEsitoChiamata(200);
	
	                auditLogRequest.setResponsePayload(null);
	
	                auditService.salvaAudit(auditLogRequest);
        		}
        		//

            } else {
                // ============================
                // CASO 2: soggetto gia PRESENTE nel REEA  aggiorna solo esposizioni
                // ============================

                RegistroDTO registroEsistente = RegistroUtils.ensureRegistro(anagraficaDTO);

                Integer oldFonteId = anagraficaDTO.getFonteId();
                anagraficaDTO.setFonteId(
                        dtos.getFonteId() != null ? dtos.getFonteId() : 2);

                Integer esposizioneId =
                        anagraficaRepository.inserisciEsposizioni(
                                ctx, anagraficaDTO.getSoggettoId().intValue(),
                                dtos, elaborazioneId, dtos.getUtenteCreazione(), 1);
                if (esposizioneId == null) {
                    throw new IllegalStateException("Inserimento esposizione fallito");
                }

                anagraficaDTO.setAdesioneId(dtos.getAdesioneId());
                anagraficaDTO.setAdesioneCod(dtos.getAdesioneCod());

                if (Integer.valueOf(2).equals(anagraficaDTO.getFonteId())) {
                    SoggettoAuraMsg sAuraCaso2 = null;
                    try {
                        DatiAnagraficiMsg dAuraCaso2 =
                                anagrafeFindClient.find(
                                        "1", anagraficaDTO.getCodiceFiscale(), null, null);
                        var elencoProfili2 =
                                SafeAccess.safeGet(() -> dAuraCaso2.getBody().getElencoProfili());
                        var primoProfilo2 =
                                SafeAccess.safeGet(() -> elencoProfili2.getDatianagrafici().get(0));
                        if (primoProfilo2 != null
                                && primoProfilo2.getIdProfiloAnagrafico() != null) {
                            sAuraCaso2 = anagrafeGetClient.get(
                                    primoProfilo2.getIdProfiloAnagrafico().toString());
                        }
                    } catch (Exception e) {
                        System.out.println("AURA non disponibile per CF="
                                + anagraficaDTO.getCodiceFiscale()
                                + ": " + e.getMessage());
                    }

//                    if (sAuraCaso2 != null
//                            && sAuraCaso2.getBody().getInfoSan().getDataFineASL() != null
//                            && !sAuraCaso2.getBody().getInfoSan().getDataFineASL().toString()
//                                    .contains("9999")) {
//                        anagraficaDTO.setSoggettoStatoId(9);
//                    } 
                    if (sAuraCaso2 != null
                	        && sAuraCaso2.getBody() != null
                	        && sAuraCaso2.getBody().getInfoSan() != null
                	        && sAuraCaso2.getBody()
                	                .getInfoSan()
                	                .getDataFineASL() != null) {

                	    XMLGregorianCalendar dataFineAslXml =
                	    		sAuraCaso2.getBody()
                	                    .getInfoSan()
                	                    .getDataFineASL();

                	    LocalDate dataFineAsl =
                	            LocalDate.of(
                	                    dataFineAslXml.getYear(),
                	                    dataFineAslXml.getMonth(),
                	                    dataFineAslXml.getDay()
                	            );

                	    boolean dataValida =
                	            !dataFineAslXml.toString().contains("9999");

                	    boolean dataScaduta =
                	            !dataFineAsl.isAfter(LocalDate.now());

                	    if (dataValida
                	            && dataScaduta) {

                	    	anagraficaDTO.setSoggettoStatoId(
                	                9
                	        );
                	    }
                	    else {
                            anagraficaDTO.setSoggettoStatoId(1);
                        }
                	}
                    else {
                        anagraficaDTO.setSoggettoStatoId(1);
                    }
                    
                }

                if (anagraficaDTO.getAdesioneId() != null) {
                    Integer aggiornamentoAdesione =
                            adesioneRepository.aggiornaAdesioniIdSoggetto(
                                    anagraficaDTO, elaborazioneId, dtos.getUtenteCreazione(), 2);
                    if (aggiornamentoAdesione == null) {
                        throw new IllegalStateException(
                                "Aggiornamento soggettoId tAdesione fallito");
                    }
                }

                // Se la fonte e' cambiata, storicizza e aggiorna il soggetto
                if (!oldFonteId.equals(anagraficaDTO.getFonteId())) {
                    registroEsistente.setDataModifica(OffsetDateTime.now());
                    aggiornaSoggetto(anagraficaDTO.getSoggettoId(), anagraficaDTO, auditLogRequest != null && auditLogRequest.getUtente() != null ? auditLogRequest.getUtente() : dtos.getUtenteCreazione(), null);
                    if (elaborazioneId != null && dtos.getAdesioneId() != null) {
                        tracciaElaborazioneRepository.inserisciFileImpatto(
                                ctx, elaborazioneId, "REEA_T_SOGGETTO",
                                dtos.getAdesioneId().intValue(), 2, dtos.getUtenteCreazione());
                        tracciaElaborazioneRepository.inserisciFileImpatto(
                                ctx, elaborazioneId, "REEA_S_SOGGETTO",
                                dtos.getAdesioneId().intValue(), 1, dtos.getUtenteCreazione());
                    }
                }

                soggettoId = anagraficaDTO.getSoggettoId().intValue();
            }

            return soggettoId;
        } catch (Exception e) {
            // Qui decidi la tua policy:
            // - log
            // - eventuale tracciatura
            // - rilancio per far scattare il rollback
            System.err.println("Errore in creazioneAnagrafica per CF " + dtos.getCodiceFiscale()
                    + ": " + e.getMessage());
//            e.printStackTrace();
            LOGGER.error("Errore in creazioneAnagrafica per CF: ", dtos.getCodiceFiscale(), e);

            // Se vuoi che la transazione venga rollbackata:
            throw new RuntimeException("Errore in creazioneAnagrafica", e);

            // Se invece vuoi **non** lanciare (sconsigliato in @Transactional),
            // potresti restituire null o un codice specifico:
            // return null;
        }
    }
    
    
    public AnagraficaDTO recuperaInfoProvinciaComune(AnagraficaDTO nuovoDTOdaInserire) {
        if (nuovoDTOdaInserire == null) {
            return null;
        }

        // NASCITA
        String nascitaComuneDesc = nuovoDTOdaInserire.getNascitaComuneDesc();
        if (nascitaComuneDesc != null && !nascitaComuneDesc.isBlank()) {
            String nascitaComuneCod = comuneRepository.findComuneCodByComuneDesc(nascitaComuneDesc);
            if(nuovoDTOdaInserire.getNascitaComuneCod() == null)
            	nuovoDTOdaInserire.setNascitaComuneCod(nascitaComuneCod);

            if (nascitaComuneCod != null && !nascitaComuneCod.isBlank()) {
                ProvinciaDTO provinciaNas = provinciaRepository.findProvinciaWithComune(nascitaComuneCod);
                if (provinciaNas != null) {
                	if(nuovoDTOdaInserire.getNascitaProvinciaCod() == null)
                		nuovoDTOdaInserire.setNascitaProvinciaCod(provinciaNas.getProvinciaCod());
                    nuovoDTOdaInserire.setNascitaProvinciaDesc(provinciaNas.getProvinciaDesc());
                }

                nuovoDTOdaInserire.setNascitaStatoCod("100");
                nuovoDTOdaInserire.setNascitaStatoDesc("ITALIA");
                nuovoDTOdaInserire.setCittadinanzaStatoCod("100");
                nuovoDTOdaInserire.setCittadinanzaStatoDesc("ITALIANA (ITALIA)");
            }
        }

        // DOMICILIO
        String domicilioProvinciaDesc = nuovoDTOdaInserire.getDomicilioProvinciaDesc();
        if (domicilioProvinciaDesc != null && !domicilioProvinciaDesc.isBlank()) {
            String domicilioProvinciaCod = provinciaRepository.findProvinciaCodByProvinciaDesc(domicilioProvinciaDesc);
            nuovoDTOdaInserire.setDomicilioProvinciaCod(domicilioProvinciaCod);

            if (domicilioProvinciaCod != null && !domicilioProvinciaCod.isBlank()) {
                nuovoDTOdaInserire.setDomicilioStatoCod("100");
                nuovoDTOdaInserire.setDomicilioStatoDesc("ITALIA");
            }
        }

        return nuovoDTOdaInserire;
    }
    
    
    public AnagraficaDTO popolaAnagraficaDTObyAura(AnagraficaDTO dtos, SoggettoAuraMsg sAuraMsg) {


        AnagraficaDTO nuovoDTOdaInserire = dtos;

        if (sAuraMsg == null) {
            return nuovoDTOdaInserire;
        }

        var body = sAuraMsg.getBody();
        var infoAnag = body != null ? body.getInfoAnag() : null;
        var datiPrimari = infoAnag != null ? infoAnag.getDatiPrimari() : null;
        var infoSan = body != null ? body.getInfoSan() : null;
        var domicilio = infoAnag != null ? infoAnag.getDomicilio() : null;
        var residenza = infoAnag != null ? infoAnag.getResidenza() : null;
        var altreInfo = body != null ? body.getAltreInfo() : null;

        // altreInfo
        String telefoni = null;
        String email = null;
        if (altreInfo != null && altreInfo.getInformazioni() != null) {
            telefoni = altreInfo.getInformazioni().stream()
                    .filter(info -> "1".equals(info.getCodInformazione()))
                    .map(info -> info.getValInformazione())
                    .filter(v -> v != null && !v.isBlank())
                    .collect(Collectors.joining(";"));

            email = altreInfo.getInformazioni().stream()
                    .filter(info -> "4".equals(info.getCodInformazione()))
                    .map(info -> info.getValInformazione())
                    .filter(v -> v != null && !v.isBlank())
                    .collect(Collectors.joining(";"));
        }

        // Sovrascrive solo se AURA ha effettivamente restituito un valore
        if (telefoni != null && !telefoni.isBlank()) {
            nuovoDTOdaInserire.setTelefonoAura(telefoni);
        }
        if (email != null && !email.isBlank()) {
            nuovoDTOdaInserire.setEmailAura(email);
        }
        
        if (sAuraMsg != null
                && sAuraMsg.getBody().getInfoSan() != null
                && sAuraMsg.getBody().getInfoSan().getDataFineASL() != null)
        	nuovoDTOdaInserire.setAssistenzaAslFine(sAuraMsg.getBody().getInfoSan().getDataFineASL().toString());


        // Codice fiscale
        String codiceFiscaleAura = datiPrimari != null ? datiPrimari.getCodiceFiscale() : null;
        nuovoDTOdaInserire.setCodiceFiscale(codiceFiscaleAura != null ? codiceFiscaleAura : dtos.getCodiceFiscale());

        // ASL residenza
        String aslResidenzaAura = infoSan != null ? infoSan.getAslResidenza() : null;
        String aslResidenzaStr = aslResidenzaAura != null
                ? aslResidenzaAura
                : (dtos.getResidenzaAslId() != null ? dtos.getResidenzaAslId().toString() : null);

        Integer residenzaAslId = (aslResidenzaStr != null && !aslResidenzaStr.isBlank())
                ? Integer.valueOf(aslResidenzaStr)
                : dtos.getResidenzaAslId();
        nuovoDTOdaInserire.setResidenzaAslId(residenzaAslId);

        // ASL domicilio
        String aslDomicilioAura = infoSan != null ? infoSan.getAslDomicilio() : null;
        nuovoDTOdaInserire.setDomicilioAslCod(aslDomicilioAura != null ? aslDomicilioAura : dtos.getDomicilioAslCod());
        nuovoDTOdaInserire.setDomicilioAslId(dtos.getDomicilioAslId());
        nuovoDTOdaInserire.setResidenzaAslCod(aslResidenzaAura != null ? aslResidenzaAura : dtos.getResidenzaAslCod());

        // Nome / Cognome
        String nomeAura = datiPrimari != null ? datiPrimari.getNome() : null;
        String cognomeAura = datiPrimari != null ? datiPrimari.getCognome() : null;
        nuovoDTOdaInserire.setNome(nomeAura != null ? nomeAura : dtos.getNome());
        nuovoDTOdaInserire.setCognome(cognomeAura != null ? cognomeAura : dtos.getCognome());

        // Data nascita
        XMLGregorianCalendar dataNascitaAura = datiPrimari != null ? datiPrimari.getDataNascita() : null;
        LocalDate dataNascita = dataNascitaAura != null
                ? dataNascitaAura.toGregorianCalendar().toZonedDateTime().toLocalDate()
                : dtos.getNascitaData();
        nuovoDTOdaInserire.setNascitaData(dataNascita);

        // Sesso
        String sessoAura = datiPrimari != null ? datiPrimari.getSesso() : null;
        nuovoDTOdaInserire.setSesso(sessoAura != null ? sessoAura : dtos.getSesso());

        // Campi fissi dal DTO di partenza
        nuovoDTOdaInserire.setFonteId(dtos.getFonteId());
        nuovoDTOdaInserire.setInserimentoTipoId(dtos.getInserimentoTipoId());
        nuovoDTOdaInserire.setSoggettoStatoId(dtos.getSoggettoStatoId());

        // ASL assistenza
        String aslAssistenzaAura = infoSan != null ? infoSan.getAslAssistenza() : null;
        nuovoDTOdaInserire.setAssistenzaAslId(aslAssistenzaAura);

        // Nascita
        if (datiPrimari != null) {
            String codComuneNascita = datiPrimari.getCodComuneNascita();
            nuovoDTOdaInserire.setNascitaComuneCod(codComuneNascita);
            nuovoDTOdaInserire.setNascitaComuneDesc(comuneRepository.findComuneDescByComuneCod(codComuneNascita));
            if(datiPrimari.getSiglaProvNascita() != null)
            	nuovoDTOdaInserire.setNascitaProvinciaCod(datiPrimari.getSiglaProvNascita());

            if (codComuneNascita != null && !codComuneNascita.isBlank()) {
                ProvinciaDTO provincia = provinciaRepository.findProvinciaWithComune(codComuneNascita);
                if (provincia != null) {                 
                    if(nuovoDTOdaInserire.getNascitaProvinciaCod() == null)
                    	nuovoDTOdaInserire.setNascitaProvinciaCod(provincia.getProvinciaCod());
                    nuovoDTOdaInserire.setNascitaProvinciaDesc(provincia.getProvinciaDesc());
                }
            }

            nuovoDTOdaInserire.setNascitaStatoCod(datiPrimari.getCodStatoNascita());
            nuovoDTOdaInserire.setNascitaStatoDesc(datiPrimari.getDescStatoNascita());
            nuovoDTOdaInserire.setCittadinanzaStatoCod(datiPrimari.getCodCittadinanza());
            nuovoDTOdaInserire.setCittadinanzaStatoDesc(datiPrimari.getDescCittadinanza());
        } else {
            nuovoDTOdaInserire.setNascitaComuneCod(null);
            nuovoDTOdaInserire.setNascitaComuneDesc(null);
            nuovoDTOdaInserire.setNascitaProvinciaCod(null);
            nuovoDTOdaInserire.setNascitaProvinciaDesc(null);
            nuovoDTOdaInserire.setNascitaStatoCod(null);
            nuovoDTOdaInserire.setNascitaStatoDesc(null);
            nuovoDTOdaInserire.setCittadinanzaStatoCod(null);
            nuovoDTOdaInserire.setCittadinanzaStatoDesc(null);
        }

        // Domicilio
        if (domicilio != null) {
            String codComuneDomicilio = domicilio.getCodComune();
            nuovoDTOdaInserire.setDomicilioComuneCod(codComuneDomicilio);
            nuovoDTOdaInserire.setDomicilioComuneDesc(domicilio.getDescComune());
            nuovoDTOdaInserire.setDomicilioStatoCod(domicilio.getCodStato());
            nuovoDTOdaInserire.setDomicilioStatoDesc(domicilio.getDescStato());
            nuovoDTOdaInserire.setDomicilioCap(domicilio.getCap());
            nuovoDTOdaInserire.setDomicilioNumeroCivico(domicilio.getNumCivico());
            nuovoDTOdaInserire.setDomicilioIndirizzo(domicilio.getIndirizzo());

            if (codComuneDomicilio != null && !codComuneDomicilio.isBlank()) {
                ProvinciaDTO provinciaDom = provinciaRepository.findProvinciaWithComune(codComuneDomicilio);
                if (provinciaDom != null) {
                    if (domicilio.getDescComune() == null) {
                        nuovoDTOdaInserire.setDomicilioComuneDesc(comuneRepository.findComuneDescByComuneCod(codComuneDomicilio));
                    }
                    nuovoDTOdaInserire.setDomicilioProvinciaCod(provinciaDom.getProvinciaCod());
                    nuovoDTOdaInserire.setDomicilioProvinciaDesc(provinciaDom.getProvinciaDesc());
                }
                else {
                    nuovoDTOdaInserire.setDomicilioProvinciaCod(null);
                    nuovoDTOdaInserire.setDomicilioProvinciaDesc(null);
                }
            }
        } else {
            nuovoDTOdaInserire.setDomicilioComuneCod(null);
            nuovoDTOdaInserire.setDomicilioComuneDesc(null);
            nuovoDTOdaInserire.setDomicilioProvinciaCod(null);
            nuovoDTOdaInserire.setDomicilioProvinciaDesc(null);
            nuovoDTOdaInserire.setDomicilioStatoCod(null);
            nuovoDTOdaInserire.setDomicilioStatoDesc(null);
            nuovoDTOdaInserire.setDomicilioCap(dtos.getDomicilioCap());
            nuovoDTOdaInserire.setDomicilioNumeroCivico(null);
            nuovoDTOdaInserire.setDomicilioIndirizzo(null);
        }

        // Residenza
        if (residenza != null) {
            String codComuneResidenza = residenza.getCodComune();
            nuovoDTOdaInserire.setResidenzaComuneCod(codComuneResidenza);
            nuovoDTOdaInserire.setResidenzaComuneDesc(residenza.getDescComune());

            if (codComuneResidenza != null && !codComuneResidenza.isBlank()) {
                ProvinciaDTO provinciaRes = provinciaRepository.findProvinciaWithComune(codComuneResidenza);
                if (provinciaRes != null) {
                    if (residenza.getDescComune() == null) {
                        nuovoDTOdaInserire.setResidenzaComuneDesc(comuneRepository.findComuneDescByComuneCod(codComuneResidenza));
                    }
                    nuovoDTOdaInserire.setResidenzaProvinciaCod(provinciaRes.getProvinciaCod());
                    nuovoDTOdaInserire.setResidenzaProvinciaDesc(provinciaRes.getProvinciaDesc());
                }
            }

            nuovoDTOdaInserire.setResidenzaStatoCod(residenza.getCodStato());
            nuovoDTOdaInserire.setResidenzaStatoDesc(residenza.getDescStato());
            nuovoDTOdaInserire.setResidenzaCap(residenza.getCap());
            nuovoDTOdaInserire.setResidenzaNumeroCivico(residenza.getNumCivico());
            nuovoDTOdaInserire.setResidenzaIndirizzo(residenza.getIndirizzo());
        } else {
            nuovoDTOdaInserire.setResidenzaComuneCod(null);
            nuovoDTOdaInserire.setResidenzaComuneDesc(null);
            nuovoDTOdaInserire.setResidenzaProvinciaCod(null);
            nuovoDTOdaInserire.setResidenzaProvinciaDesc(null);
            nuovoDTOdaInserire.setResidenzaStatoCod(null);
            nuovoDTOdaInserire.setResidenzaStatoDesc(null);
            nuovoDTOdaInserire.setResidenzaCap(null);
            nuovoDTOdaInserire.setResidenzaNumeroCivico(null);
            nuovoDTOdaInserire.setResidenzaIndirizzo(null);
        }

        // Tessera TEAM
        String tesseraTeamAura = infoSan != null ? infoSan.getCodiceTesseraTEAM() : null;
        nuovoDTOdaInserire.setTesseraTeam(tesseraTeamAura != null ? tesseraTeamAura : dtos.getTesseraTeam());

        // idAura
        String idAuraStr = body != null && body.getIdAura() != null ? body.getIdAura().toString() : null;
        nuovoDTOdaInserire.setIdAura(idAuraStr != null ? idAuraStr : dtos.getIdAura());

        // data decesso
        XMLGregorianCalendar dataDecesso = datiPrimari != null ? datiPrimari.getDataDecesso() : null;
        if (dataDecesso != null) {
            nuovoDTOdaInserire.setDataDecesso(dataDecesso.toGregorianCalendar().toZonedDateTime().toLocalDate().toString());
            nuovoDTOdaInserire.setSoggettoStatoId(8);       
        }
        else
        {
//            if (sAuraMsg != null
//                    && sAuraMsg.getBody().getInfoSan() != null
//                    && sAuraMsg.getBody().getInfoSan().getDataFineASL() != null
//                    && !sAuraMsg.getBody().getInfoSan().getDataFineASL().toString()
//                            .contains("9999")) {
//            	nuovoDTOdaInserire.setSoggettoStatoId(9);
//            }
        	if (sAuraMsg != null
        	        && sAuraMsg.getBody() != null
        	        && sAuraMsg.getBody().getInfoSan() != null
        	        && sAuraMsg.getBody()
        	                .getInfoSan()
        	                .getDataFineASL() != null) {

        	    XMLGregorianCalendar dataFineAslXml =
        	            sAuraMsg.getBody()
        	                    .getInfoSan()
        	                    .getDataFineASL();

        	    LocalDate dataFineAsl =
        	            LocalDate.of(
        	                    dataFineAslXml.getYear(),
        	                    dataFineAslXml.getMonth(),
        	                    dataFineAslXml.getDay()
        	            );

        	    boolean dataValida =
        	            !dataFineAslXml.toString().contains("9999");

        	    boolean dataScaduta =
        	            !dataFineAsl.isAfter(LocalDate.now());

        	    if (dataValida
        	            && dataScaduta) {

        	    	nuovoDTOdaInserire.setSoggettoStatoId(
        	                9
        	        );
        	    }
        	}
        }

        // residenza provincia dal form
        nuovoDTOdaInserire.setResidenzaProvinciaCod(dtos.getResidenzaProvinciaCod());
        nuovoDTOdaInserire.setResidenzaProvinciaDesc(dtos.getResidenzaProvinciaDesc());

        // Esenzioni
        List<EsenzioneDTO> listaEsenzione = new ArrayList<>();
        var infoEsenzioni = body != null ? body.getInfoEsenzioni() : null;
        if (infoEsenzioni != null && infoEsenzioni.getInfoesenzione() != null) {
            infoEsenzioni.getInfoesenzione().forEach(info -> {
                EsenzioneDTO dto = new EsenzioneDTO();
                dto.setEsenzioneCod(info.getCodEsenzione());
                dto.setDiagnosiCod(info.getCodDiagnosi());
                Integer esenzioneId = esenzioneRepository.getEsenzioneIdByEsenzioneCod(info.getCodEsenzione(), info.getCodDiagnosi());
                if (esenzioneId != null) {
                    dto.setEsenzioneId(esenzioneId.toString());
                    dto.setEsenzioneDataEmissione(DateConversionUtils.toLocalDate(info.getDataEmissione()));
                    dto.setEsenzioneDataScadenza(DateConversionUtils.toLocalDate(info.getDataScadenza()));
                    listaEsenzione.add(dto);
                }
            });
        }
        if (!listaEsenzione.isEmpty()) {
            // AURA ha restituito esenzioni con ID risolto â le usiamo
            nuovoDTOdaInserire.setListaEsenzione(listaEsenzione);
        } else if (dtos.getListaEsenzione() != null && !dtos.getListaEsenzione().isEmpty()) {
            // AURA non ha restituito esenzioni â risolviamo gli ID per quelle mandate dal frontend
            List<EsenzioneDTO> frontendEsenzioni = new ArrayList<>();
            dtos.getListaEsenzione().forEach(e -> {
                Integer esenzioneId = esenzioneRepository.getEsenzioneIdByEsenzioneCod(e.getEsenzioneCod(), e.getDiagnosiCod());
                if (esenzioneId != null) {
                    e.setEsenzioneId(esenzioneId.toString());
                    frontendEsenzioni.add(e);
                }
            });
            nuovoDTOdaInserire.setListaEsenzione(frontendEsenzioni);
        }

        return nuovoDTOdaInserire;
    }

    

    @Override
    @Transactional(readOnly = true)
    public AnagraficaDTO getById(Long soggettoId) {
        return anagraficaRepository.findById(soggettoId);
    }
    
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getByIdCampiAnonimizzati(Long soggettoId, Boolean flagDatiAnonimizzati, String profiloUtente) {
    	Map<String, String> mappaRitorno = null;
    	if(soggettoId == null)
    		return null;
    	List<ExportDTO> righe = anagraficaRepository.recuperaTotaleRecordPerExportAsincrono(null, soggettoId.intValue(), true, profiloUtente);
    	if(righe == null || righe.isEmpty())
    		return null;
    	ExportDTO dto = (righe.get(0));
    	// Carica la mappa dei campi mascherati
	    Map<String, List<String>> mapCampiMascherati = anagraficaRepository.buildCampiMascheratiMap();
	    mappaRitorno = buildExportMaskedResponse(dto, flagDatiAnonimizzati, mapCampiMascherati);
        	
	    return mappaRitorno;
    }
    

    @Override
    @Transactional(readOnly = true)
    public AnagraficaDTO getByCodiceFiscale(String codiceFiscale) {
        return anagraficaRepository.findByCodiceFiscale(codiceFiscale);
    }


    @Override
    // @Transactional
    public AnagraficaDTO aggiornaSoggetto(Long id, AnagraficaDTO dto, String cfOperatore, AuditLogRequest auditLogRequest) {
    	DSLContext ctx = dsl;
    	AnagraficaDTO existing = anagraficaRepository.findById(id);
    	if (existing == null) {
    		return null;
    	}

    	// garantisce che existing.getRegistro() non sia mai null
    	RegistroDTO registroExisting = RegistroUtils.ensureRegistro(existing);

    	Integer versioneCorrente = registroExisting.getVersioneNumero();

    	// Salva i valori VECCHI di tutti i campi tracciati PRIMA di mutare existing
    	Integer vecchioStatoId        = existing.getSoggettoStatoId();
    	Integer vecchiaFonteId        = existing.getFonteId();
    	String  vecchioTelefono       = existing.getTelefono();
    	String  vecchiaEmail          = existing.getEmail();
    	String  vecchiaAssistenzaAslId= existing.getAssistenzaAslId();
    	String  vecchiaResidenzaIndirizzo    = existing.getResidenzaIndirizzo();
    	String  vecchiaResidenzaNumeroCivico = existing.getResidenzaNumeroCivico();
    	String  vecchiaResidenzaCap          = existing.getResidenzaCap();
    	String  vecchiaResidenzaComuneCod    = existing.getResidenzaComuneCod();
    	String  vecchiaResidenzaProvinciaCod = existing.getResidenzaProvinciaCod();
    	String  vecchiaResidenzaStatoCod     = existing.getResidenzaStatoCod();
    	String  vecchioDomicilioIndirizzo    = existing.getDomicilioIndirizzo();
    	String  vecchioDomicilioNumeroCivico = existing.getDomicilioNumeroCivico();
    	String  vecchioDomicilioCap          = existing.getDomicilioCap();
    	String  vecchioDomicilioComuneCod    = existing.getDomicilioComuneCod();
    	String  vecchioDomicilioProvinciaCod = existing.getDomicilioProvinciaCod();
    	String  vecchioDomicilioStatoCod     = existing.getDomicilioStatoCod();

    	boolean fonteChanged     = dto.getFonteId() != null && !dto.getFonteId().equals(vecchiaFonteId);
    	boolean statoChanged     = dto.getSoggettoStatoId() != null && !dto.getSoggettoStatoId().equals(vecchioStatoId);
    	boolean telefonoChanged  = dto.getTelefono() != null && !dto.getTelefono().equals(vecchioTelefono);
    	boolean emailChanged     = dto.getEmail() != null && !dto.getEmail().equals(vecchiaEmail);
    	boolean aslChanged       = dto.getAssistenzaAslId() != null && !dto.getAssistenzaAslId().isBlank()
    			                   && !dto.getAssistenzaAslId().equals(vecchiaAssistenzaAslId);
    	boolean residenzaChanged = !java.util.Objects.equals(dto.getResidenzaIndirizzo(),   vecchiaResidenzaIndirizzo)
    			                || !java.util.Objects.equals(dto.getResidenzaComuneCod(),    vecchiaResidenzaComuneCod)
    			                || !java.util.Objects.equals(dto.getResidenzaProvinciaCod(), vecchiaResidenzaProvinciaCod)
    			                || !java.util.Objects.equals(dto.getResidenzaStatoCod(),     vecchiaResidenzaStatoCod);
    	boolean domicilioChanged = !java.util.Objects.equals(dto.getDomicilioIndirizzo(),   vecchioDomicilioIndirizzo)
    			                || !java.util.Objects.equals(dto.getDomicilioComuneCod(),    vecchioDomicilioComuneCod)
    			                || !java.util.Objects.equals(dto.getDomicilioProvinciaCod(), vecchioDomicilioProvinciaCod)
    			                || !java.util.Objects.equals(dto.getDomicilioStatoCod(),     vecchioDomicilioStatoCod);

    	boolean anyChanged = fonteChanged || statoChanged || telefonoChanged || emailChanged
    			          || aslChanged || residenzaChanged || domicilioChanged;

    	//        Integer nuovaVersione = !existing.getFonteId().equals(dto.getFonteId())
    	//             ? versioneCorrente + 1
    	//             : versioneCorrente;

    	Integer nuovaVersione = anyChanged ? versioneCorrente + 1 : versioneCorrente;
    	// Inserisce storico SOLO se fonte o stato sono cambiati
    	int updatedRows = 1;

    	//     updatedRows = anagraficaRepository.insertStoricoSoggetto(ctx, id, existing, nuovaVersione, cfOperatore);

    	existing.setCodiceFiscale(dto.getCodiceFiscale() != null ? dto.getCodiceFiscale() : existing.getCodiceFiscale());
    	existing.setResidenzaAslId(dto.getResidenzaAslId() != null ? dto.getResidenzaAslId() : existing.getResidenzaAslId());
    	existing.setDomicilioAslId(dto.getDomicilioAslId() != null ? dto.getDomicilioAslId() : existing.getDomicilioAslId());
    	existing.setNome(dto.getNome() != null ? dto.getNome() : existing.getNome());
    	existing.setCognome(dto.getCognome() != null ? dto.getCognome() : existing.getCognome());
    	existing.setNascitaData(dto.getNascitaData() != null ? dto.getNascitaData() : existing.getNascitaData());

    	//MM 2026-07-07 --- Inizio ---
    	existing.setSesso(dto.getSesso() != null ? dto.getSesso() : existing.getSesso());
    	existing.setNascitaComuneCod(dto.getNascitaComuneCod() != null ? dto.getNascitaComuneCod() : existing.getNascitaComuneCod());
    	existing.setNascitaComuneDesc(dto.getNascitaComuneDesc() != null ? dto.getNascitaComuneDesc() : existing.getNascitaComuneDesc());
    	existing.setNascitaProvinciaCod(dto.getNascitaProvinciaCod() != null ? dto.getNascitaProvinciaCod() : existing.getNascitaProvinciaCod());
    	existing.setNascitaProvinciaDesc(dto.getNascitaProvinciaDesc() != null ? dto.getNascitaProvinciaDesc() : existing.getNascitaProvinciaDesc());;
    	existing.setNascitaStatoCod(dto.getNascitaStatoCod() != null ? dto.getNascitaStatoCod() : existing.getNascitaStatoCod());
    	existing.setNascitaStatoDesc(dto.getNascitaStatoDesc() != null ? dto.getNascitaStatoDesc() : existing.getNascitaStatoDesc());
    	existing.setCittadinanzaStatoCod(dto.getCittadinanzaStatoCod() != null ? dto.getCittadinanzaStatoCod() : existing.getCittadinanzaStatoCod());
    	existing.setCittadinanzaStatoDesc(dto.getCittadinanzaStatoDesc() != null ? dto.getCittadinanzaStatoDesc() : existing.getCittadinanzaStatoDesc());
    	existing.setTesseraTeam(dto.getTesseraTeam() != null ? dto.getTesseraTeam() : existing.getTesseraTeam());
    	//MM 2026-07-07 ---  Fine  ---

    	existing.setFonteId(dto.getFonteId() != null ? dto.getFonteId() : existing.getFonteId());
    	//     existing.setSoggettoStatoId(dto.getSoggettoStatoId() != null ? dto.getSoggettoStatoId() : existing.getSoggettoStatoId());
    	existing.setSoggettoStatoId(dto.getSoggettoStatoId() != null ? dto.getSoggettoStatoId() : existing.getSoggettoStatoId());
    	existing.setTelefono(dto.getTelefono() != null ? dto.getTelefono() : existing.getTelefono());
    	existing.setEmail(dto.getEmail() != null ? dto.getEmail() : existing.getEmail());
    	existing.setInserimentoTipoId(dto.getInserimentoTipoId() != null ? dto.getInserimentoTipoId() : existing.getInserimentoTipoId());

    	existing.setResidenzaIndirizzo(dto.getResidenzaIndirizzo() != null ? dto.getResidenzaIndirizzo() : existing.getResidenzaIndirizzo());
    	existing.setResidenzaNumeroCivico(dto.getResidenzaNumeroCivico() != null ? dto.getResidenzaNumeroCivico() : existing.getResidenzaNumeroCivico());
    	existing.setResidenzaCap(dto.getResidenzaCap() != null ? dto.getResidenzaCap() : existing.getResidenzaCap());
    	existing.setResidenzaComuneCod(dto.getResidenzaComuneCod());   //MM 2026-07-07
    	existing.setResidenzaComuneDesc(dto.getResidenzaComuneDesc());
    	existing.setResidenzaProvinciaCod(dto.getResidenzaProvinciaCod());   //MM 2026-07-07
    	existing.setResidenzaProvinciaDesc(dto.getResidenzaProvinciaDesc());
    	existing.setResidenzaStatoCod(dto.getResidenzaStatoCod());   //MM 2026-07-07
    	existing.setResidenzaStatoDesc(dto.getResidenzaStatoDesc());

    	existing.setDomicilioIndirizzo(dto.getDomicilioIndirizzo() != null ? dto.getDomicilioIndirizzo() : existing.getDomicilioIndirizzo());
    	existing.setDomicilioNumeroCivico(dto.getDomicilioNumeroCivico() != null ? dto.getDomicilioNumeroCivico() : existing.getDomicilioNumeroCivico());
    	existing.setDomicilioCap(dto.getDomicilioCap() != null ? dto.getDomicilioCap() : existing.getDomicilioCap());
    	existing.setDomicilioComuneCod(dto.getDomicilioComuneCod() != null ? dto.getDomicilioComuneCod() : existing.getDomicilioComuneCod());  //MM 2026-07-07	     
    	existing.setDomicilioComuneDesc(dto.getDomicilioComuneDesc());
    	existing.setDomicilioProvinciaCod(dto.getDomicilioProvinciaCod());   //MM 2026-07-07
    	existing.setDomicilioProvinciaDesc(dto.getDomicilioProvinciaDesc());
    	existing.setDomicilioStatoCod(dto.getDomicilioStatoCod());   //MM 2026-07-07
    	existing.setDomicilioStatoDesc(dto.getDomicilioStatoDesc());

    	if (dto.getAssistenzaAslId() != null && !dto.getAssistenzaAslId().isBlank()) {
    		existing.setAssistenzaAslId(dto.getAssistenzaAslId());
    	}
    	if (updatedRows > 0) {
    		anagraficaRepository.updateSoggetto(ctx, id, existing, nuovaVersione, cfOperatore);
    	}

    	if (anyChanged) {
    		// Ripristina TUTTI i valori VECCHI prima di passarli alla storia
    		existing.setSoggettoStatoId(vecchioStatoId);
    		existing.setFonteId(vecchiaFonteId);
    		existing.setTelefono(vecchioTelefono);
    		existing.setEmail(vecchiaEmail);
    		existing.setAssistenzaAslId(vecchiaAssistenzaAslId);
    		existing.setResidenzaIndirizzo(vecchiaResidenzaIndirizzo);
    		existing.setResidenzaNumeroCivico(vecchiaResidenzaNumeroCivico);
    		existing.setResidenzaCap(vecchiaResidenzaCap);
    		existing.setResidenzaComuneCod(vecchiaResidenzaComuneCod);
    		existing.setResidenzaProvinciaCod(vecchiaResidenzaProvinciaCod);
    		existing.setResidenzaStatoCod(vecchiaResidenzaStatoCod);
    		existing.setDomicilioIndirizzo(vecchioDomicilioIndirizzo);
    		existing.setDomicilioNumeroCivico(vecchioDomicilioNumeroCivico);
    		existing.setDomicilioCap(vecchioDomicilioCap);
    		existing.setDomicilioComuneCod(vecchioDomicilioComuneCod);
    		existing.setDomicilioProvinciaCod(vecchioDomicilioProvinciaCod);
    		existing.setDomicilioStatoCod(vecchioDomicilioStatoCod);
    		anagraficaRepository.insertStoricoSoggetto(ctx, id, existing, nuovaVersione, existing.getUtenteCreazione(), existing.getUtenteModifica());
    	}

//    	if (fonteChanged || statoChanged) {
//    		anagraficaRepository.insertStoricoSoggetto(ctx, id, existing, nuovaVersione, cfOperatore);
//    	}

    	//
    	if (auditLogRequest != null) {
    		auditLogRequest.setIdApp(auditLogRequest.getIdApp());
    		auditLogRequest.setOperazione("Dettaglio assistito");
    		auditLogRequest.setOggOper(JsonNullable.of("Update - Telefono e/o Email"));

    		// valorizzati dal contesto chiamante / FE
    		auditLogRequest.setUtente(auditLogRequest.getUtente());
    		auditLogRequest.setUuid(auditLogRequest.getUuid());

    		// scegli tu la logica: null oppure chiave sintetica della ricerca
    		auditLogRequest.setKeyOper(null);

    		auditLogRequest.setRequestPayload(null);
    		auditLogRequest.setResponsePayload(null);
    		auditLogRequest.setEsitoChiamata(200);

    		auditService.salvaAudit(auditLogRequest);
    	}
    	
    	return anagraficaRepository.findById(id);
    }
    


    @Override
    @Transactional(readOnly = true)
    public List<AnagraficaDTO> getByCForNomeOrCognomeOrDataNascita(String filtroCodiceFiscale,
                                                                   String filtroNome, String filtroCognome, LocalDate filtroNascitaData) {
        return anagraficaRepository.findByCForNomeOrCognomeOrDataNascita(
                filtroCodiceFiscale, filtroNome, filtroCognome, filtroNascitaData);
    }

	@Override
	public Integer getRegistroIdBySoggettoId(Integer soggettoId) {
		return anagraficaRepository.findRegistroIdBySoggettoId(soggettoId);
	}


	public void aggiornaStato(Long soggettoId, AnagraficaDTO anagraficaDTO, AuditLogRequest auditLogRequest) {
        
		if(auditLogRequest == null)
			auditLogRequest = new AuditLogRequest();
		
		System.out.println(">>> aggiornaStato - statoId: " + anagraficaDTO.getSoggettoStatoId()
                + " | nota: " + anagraficaDTO.getSoggettoStatoNote());
        anagraficaRepository.aggiornaStato(soggettoId, anagraficaDTO, auditLogRequest.getUtente());

        auditLogRequest.setOperazione("Selezione assistito");
//        auditLogRequest.setOggOper(JsonNullable.of("Valuta assistito")); // dovrà essere passato dal FE
        auditLogRequest.setKeyOper(null);
        auditLogRequest.setRequestPayload(null);
        auditLogRequest.setEsitoChiamata(200);

        auditLogRequest.setResponsePayload(null);

        auditService.salvaAudit(auditLogRequest);
    }

    @Override
    public List<StoriaStatoDTO> getStoriaStati(Long soggettoId) {
        return anagraficaRepository.findStoriaStati(soggettoId.intValue());
    }
	
	//stessi valori
	public boolean stessiValoriSpresal(SpresalDTO a, SpresalDTO b) {
	    if (a == null && b == null) return true;
	    if (a == null || b == null) return false;

	    return Objects.equals(a.getIdAura(),                      b.getIdAura())
	        && Objects.equals(a.getDataIntervista(),              b.getDataIntervista())
	        && Objects.equals(a.getNominativoIntervistatore(),    b.getNominativoIntervistatore())
	        && Objects.equals(a.getFumatore(),                    b.getFumatore())
	        && Objects.equals(a.getSigarette(),                   b.getSigarette())
	        && Objects.equals(a.getSigaretteAnni(),               b.getSigaretteAnni())
	        && Objects.equals(a.getSigaretteEtaInizio(),          b.getSigaretteEtaInizio())
	        && Objects.equals(a.getSigaretteFumaAttualmente(),    b.getSigaretteFumaAttualmente())
	        && Objects.equals(a.getSigaretteEtaFine(),            b.getSigaretteEtaFine())
	        && Objects.equals(a.getSigaretteDie(),                b.getSigaretteDie())
	        && Objects.equals(a.getSigari(),                      b.getSigari())
	        && Objects.equals(a.getSigariAnni(),                  b.getSigariAnni())
	        && Objects.equals(a.getSigariEtaInizio(),             b.getSigariEtaInizio())
	        && Objects.equals(a.getSigariFumaAttualmente(),       b.getSigariFumaAttualmente())
	        && Objects.equals(a.getSigariEtaFine(),               b.getSigariEtaFine())
	        && Objects.equals(a.getSigariDie(),                   b.getSigariDie())
	        && Objects.equals(a.getPipa(),                        b.getPipa())
	        && Objects.equals(a.getPipaAnni(),                    b.getPipaAnni())
	        && Objects.equals(a.getPipaEtaInizio(),               b.getPipaEtaInizio())
	        && Objects.equals(a.getPipaFumaAttualmente(),         b.getPipaFumaAttualmente())
	        && Objects.equals(a.getPipaEtaFine(),                 b.getPipaEtaFine())
	        && Objects.equals(a.getPipaDie(),                     b.getPipaDie())
	        && Objects.equals(a.getOccupazioneNum(),              b.getOccupazioneNum())
	        && Objects.equals(a.getOccupazioneAnnoInizio(),       b.getOccupazioneAnnoInizio())
	        && Objects.equals(a.getOccupazioneAnnoFine(),         b.getOccupazioneAnnoFine())
	        && Objects.equals(a.getOccupazioneTipo(),             b.getOccupazioneTipo());
	}
	
	
	public boolean stessiValoriSpresalEsiti(SpresalEsitiDTO a, SpresalEsitiDTO b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;

        return Objects.equals(a.getCodiceFiscale(),                  b.getCodiceFiscale())
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
            && Objects.equals(a.getIdSpresal(),                                b.getIdSpresal());
    }
	
	
	public boolean controlloObbligatorietaDatiFile(SpresalEsitiDTO spresalEsitiDTO) {
		boolean risultato = false;
				
			if(spresalEsitiDTO.getCodiceFiscale() != null && spresalEsitiDTO.getDataVisita() != null 
				&& spresalEsitiDTO.getVisita() != null && spresalEsitiDTO.getLivelloVisita() != null)				
					
				risultato = true;

		return risultato;
	}
	
	
	@Override
	public Map<String, Integer> scaricaDatiExcelTotaliAsincroni(Boolean flagDatiAnonimizzati, Integer assistenzaAslId, String profiloUtente, String utenteLogin) {

	    // 1. Calcola aslCod (lookup veloce, sincrono)
	    String aslCod = null;
	    if (assistenzaAslId != null) {
	        AslDTO asl = aslRepository.findById(assistenzaAslId);
	        if (asl != null) {
	            aslCod = asl.getAslCod();
	            if (aslCod != null && aslCod.length() > 3) {
	                aslCod = aslCod.substring(aslCod.length() - 3);
	            }
	        }
	    }

	    // 2. Genera nome file univoco
	    String aslSuffix = (aslCod != null && !aslCod.isBlank())
	            ? aslCod
	            : (assistenzaAslId != null ? String.valueOf(assistenzaAslId) : null);
	    String baseName;
	    if (Boolean.TRUE.equals(flagDatiAnonimizzati) && aslSuffix != null) {
	        baseName = "export_totale_pseudo_" + aslSuffix;
	    } else if (Boolean.TRUE.equals(flagDatiAnonimizzati)) {
	        baseName = "export_totale_pseudo";
	    } else if (aslSuffix != null) {
	        baseName = "export_totale_chiaro_" + aslSuffix;
	    } else {
	        baseName = "export_totale_chiaro";
	    }
	    String uniqueName = baseName + "_"
	            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + ".xlsx";

	    // 3. Crea i record DB subito (sincrono, auto-commit)
	    Integer fileId = tracciaElaborazioneService.inserisciFile(uniqueName, baseDir, "", "", 0, 2, utenteLogin);
	    Integer elaborazioneId = tracciaElaborazioneService.inserisciStatoElaborazione(fileId, utenteLogin, 2);

	    // 4. Lancia il lavoro pesante in background (query + Excel + salvataggio)
	    final String fProfiloUtente   = profiloUtente;
	    final Integer fAslId          = assistenzaAslId;
	    final Boolean fFlag           = flagDatiAnonimizzati;
	    final String fLogin           = utenteLogin;
	    final Integer fFileId         = fileId;
	    final Integer fElabId         = elaborazioneId;
	    final String fUniqueName      = uniqueName;
	    final String fAslCod          = aslCod;

	    CompletableFuture.runAsync(() -> {
	        try {
	            List<ExportDTO> righe = new ArrayList<>();

	            if ("REEA_OP_SPRESAL_PSEUDO".equals(fProfiloUtente)
	                    || "REEA_OP_EPI_PSEUDO".equals(fProfiloUtente)
	                    || "REEA_OP_SPRESAL".equals(fProfiloUtente)
	                    || "REEA_OP_CSI_PSEUDO".equals(fProfiloUtente)) {
	                righe = new ArrayList<>(
	                        anagraficaRepository.recuperaTotaleRecordPerExportAsincronoStatiPrecedenti(fAslId, fProfiloUtente));
	            }

	            if ("REEA_OP_SPRESAL".equals(fProfiloUtente)
	                    || "REEA_OP_CRPT".equals(fProfiloUtente)
	                    || "REEA_OP_CSI".equals(fProfiloUtente)
	                    || "REEA_OP_CRPT_PSEUDO".equals(fProfiloUtente)
	                    || "REEA_OP_SPRESAL_PSEUDO".equals(fProfiloUtente)
	                    || "REEA_OP_EPI_PSEUDO".equals(fProfiloUtente)
	                    || "REEA_OP_CSI_PSEUDO".equals(fProfiloUtente)) {
//	            	boolean includiSenzaAdesione = "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente) || "REEA_OP_CRPT".equals(profiloUtente);
//	                List<ExportDTO> righe2 = anagraficaRepository.recuperaTotaleRecordPerExportAsincrono(fAslId, null, includiSenzaAdesione, fProfiloUtente);
	            	List<ExportDTO> righe2 = anagraficaRepository.recuperaTotaleRecordPerExportAsincrono(fAslId, null, false, fProfiloUtente);
	            	if (righe2 != null && !righe2.isEmpty()) {
	                    righe.addAll(righe2);
	                }
	            }

	            ExcelExportResult result = generaExcelTotaleAsincrono2(
	                    righe, fFlag, fAslId, fAslCod, fLogin, fFileId, fElabId, fUniqueName);

	            FileSalvato fileSalvato = null;
	            String contentType = null;
	            long fileSize = 0;
	            try {
	                fileSalvato = ExcelFileUtils.salvaExcelExportResult(result, baseDir);
	                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	                fileSize = result.getContent() != null ? result.getContent().length : 0;
	            } catch (Exception e) {
//	                e.printStackTrace();
	            	LOGGER.error("Errore in scaricaDatiExcelTotaliAsincroni ", null, e);
	            } finally {
	                try {
	                    if (result != null) {
	                        if (fileSalvato != null) {
	                            tracciaElaborazioneService.aggiornaFileExcel(
	                                    fFileId, 3, fileSalvato.checksum(), contentType, fileSize, fLogin);
	                        }
	                        tracciaElaborazioneService.aggiornaFineOk(
	                                fElabId, 3, 0, 0, 0, 0, 0, "Elaborazione completata", fLogin);
	                    }
	                } catch (Exception exFinally) {
	                    System.err.println("Errore nel finally durante aggiornaFineOk: " + exFinally.getMessage());
//	                    exFinally.printStackTrace();
	                    LOGGER.error("Errore nel finally durante aggiornaFineOk in scaricaDatiExcelTotaliAsincroni ", null, exFinally);
	                }
	            }
	        } catch (Exception e) {
	            System.err.println("Errore export background [" + fProfiloUtente + "]: " + e.getMessage());
//	            e.printStackTrace();
	            LOGGER.error("Errore export background [" + fProfiloUtente + "]: ", null, e);
	        }
	    });

	    // 5. Ritorna subito gli ID — il frontend fa polling sull'archivio
	    return Map.of("fileId", fileId, "elaborazioneId", elaborazioneId);
	}
    
    
	private Map<String, String> buildExportMaskedResponse(ExportDTO oggettoOriginale,
			Boolean flagDatiAnonimizzati,
			Map<String, List<String>> mapCampiMascherati) {

		Map<String, String> response = new LinkedHashMap<>();

		if (oggettoOriginale == null) {
			return response;
		}

		// ── ADESIONE ──────────────────────────────────────────────────────
		response.put("adesione_codice_adesione",
				valoreCellaString("adesione_codice_adesione", "ADESIONE",
						oggettoOriginale.getAdesioneCodiceAdesione(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_data_adesione",
				valoreCellaString("adesione_data_adesione", "ADESIONE",
						oggettoOriginale.getAdesioneDataAdesione(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_codice_fiscale",
				valoreCellaString("adesione_codice_fiscale", "ADESIONE",
						oggettoOriginale.getAdesioneCodiceFiscale(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_cognome",
				valoreCellaString("adesione_cognome", "ADESIONE",
						oggettoOriginale.getAdesioneCognome(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_nome",
				valoreCellaString("adesione_nome", "ADESIONE",
						oggettoOriginale.getAdesioneNome(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_data_di_nascita",
				valoreCellaString("adesione_data_di_nascita", "ADESIONE",
						oggettoOriginale.getAdesioneDataDiNascita(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_provincia_di_nascita",
				valoreCellaString("adesione_provincia_di_nascita", "ADESIONE",
						oggettoOriginale.getAdesioneProvinciaDiNascita(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_comune_di_nascita",
				valoreCellaString("adesione_comune_di_nascita", "ADESIONE",
						oggettoOriginale.getAdesioneComuneDiNascita(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_tessera_team",
				valoreCellaString("adesione_tessera_team", "ADESIONE",
						oggettoOriginale.getAdesioneTesseraTeam(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_id_aura",
				valoreCellaString("adesione_id_aura", "ADESIONE",
						oggettoOriginale.getAdesioneIdAura(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_provincia_di_domicilio",
				valoreCellaString("adesione_provincia_di_domicilio", "ADESIONE",
						oggettoOriginale.getAdesioneProvinciaDiDomicilio(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_comune_di_domicilio",
				valoreCellaString("adesione_comune_di_domicilio", "ADESIONE",
						oggettoOriginale.getAdesioneComuneDiDomicilio(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_codice_comune_istat_di_domicilio",
				valoreCellaString("adesione_codice_comune_istat_di_domicilio", "ADESIONE",
						oggettoOriginale.getAdesioneCodiceComuneIstatDiDomicilio(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_cap_di_domicilio",
				valoreCellaString("adesione_cap_di_domicilio", "ADESIONE",
						oggettoOriginale.getAdesioneCapDiDomicilio(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_email",
				valoreCellaString("adesione_email", "ADESIONE",
						oggettoOriginale.getAdesioneEmail(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_telefono",
				valoreCellaString("adesione_telefono", "ADESIONE",
						oggettoOriginale.getAdesioneTelefono(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_codice_asl_di_domicilio",
				valoreCellaString("adesione_codice_asl_di_domicilio", "ADESIONE",
						oggettoOriginale.getAdesioneCodiceAslDiDomicilio(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_asl_di_domicilio",
				valoreCellaString("adesione_asl_di_domicilio", "ADESIONE",
						oggettoOriginale.getAdesioneAslDiDomicilio(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_codazi",
				valoreCellaString("adesione_codazi", "ADESIONE",
						oggettoOriginale.getAdesioneCodazi(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_codice_asl_di_residenza",
				valoreCellaString("adesione_codice_asl_di_residenza", "ADESIONE",
						oggettoOriginale.getAdesioneCodiceAslDiResidenza(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_asl_di_residenza",
				valoreCellaString("adesione_asl_di_residenza", "ADESIONE",
						oggettoOriginale.getAdesioneAslDiResidenza(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_data_inizio_esposizione",
				valoreCellaString("adesione_data_inizio_esposizione", "ADESIONE",
						oggettoOriginale.getAdesioneDataInizioEsposizione(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_data_fine_esposizione",
				valoreCellaString("adesione_data_fine_esposizione", "ADESIONE",
						oggettoOriginale.getAdesioneDataFineEsposizione(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_azienda",
				valoreCellaString("adesione_azienda", "ADESIONE",
						oggettoOriginale.getAdesioneAzienda(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_codice_comune_istat_azienda",
				valoreCellaString("adesione_codice_comune_istat_azienda", "ADESIONE",
						oggettoOriginale.getAdesioneCodiceComuneIstatAzienda(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_comune_azienda",
				valoreCellaString("adesione_comune_azienda", "ADESIONE",
						oggettoOriginale.getAdesioneComuneAzienda(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_cap_azienda",
				valoreCellaString("adesione_cap_azienda", "ADESIONE",
						oggettoOriginale.getAdesioneCapAzienda(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_provincia_azienda",
				valoreCellaString("adesione_provincia_azienda", "ADESIONE",
						oggettoOriginale.getAdesioneProvinciaAzienda(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_mansione",
				valoreCellaString("adesione_mansione", "ADESIONE",
						oggettoOriginale.getAdesioneMansione(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_esposizione_azienda",
				valoreCellaString("adesione_azienda", "ADESIONE",
						oggettoOriginale.getAdesioneEsposizioneAzienda(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_esposizione_azienda_comune_cod",
				valoreCellaString("adesione_codice_comune_istat_azienda", "ADESIONE",
						oggettoOriginale.getAdesioneEsposizioneAziendaComuneCod(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_esposizione_azienda_comune_desc",
				valoreCellaString("adesione_comune_azienda", "ADESIONE",
						oggettoOriginale.getAdesioneEsposizioneAziendaComuneDesc(),
						flagDatiAnonimizzati, mapCampiMascherati));

		response.put("adesione_esposizione_azienda_cap",
				valoreCellaString("adesione_cap_azienda", "ADESIONE",
						oggettoOriginale.getAdesioneEsposizioneAziendaCap(),
						flagDatiAnonimizzati, mapCampiMascherati));
		
	    // ── AURA ──────────────────────────────────────────────────────────
	    response.put("aura_codice_fiscale",
	            valoreCellaString("aura_codice_fiscale", "AURA",
	                    oggettoOriginale.getAuraCodiceFiscale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_asl",
	            valoreCellaString("aura_domicilio_asl", "AURA",
	                    oggettoOriginale.getAuraDomicilioAsl(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_asl",
	            valoreCellaString("aura_residenza_asl", "AURA",
	                    oggettoOriginale.getAuraResidenzaAsl(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_assistenza_asl",
	            valoreCellaString("aura_assistenza_asl", "AURA",
	                    oggettoOriginale.getAuraAssistenzaAsl(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_nome",
	            valoreCellaString("aura_nome", "AURA",
	                    oggettoOriginale.getAuraNome(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_cognome",
	            valoreCellaString("aura_cognome", "AURA",
	                    oggettoOriginale.getAuraCognome(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_sesso",
	            valoreCellaString("aura_sesso", "AURA",
	                    oggettoOriginale.getAuraSesso(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_nascita_data",
	            valoreCellaString("aura_nascita_data", "AURA",
	                    oggettoOriginale.getAuraNascitaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_nascita_comune_cod",
	            valoreCellaString("aura_nascita_comune_cod", "AURA",
	                    oggettoOriginale.getAuraNascitaComuneCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_nascita_comune_desc",
	            valoreCellaString("aura_nascita_comune_desc", "AURA",
	                    oggettoOriginale.getAuraNascitaComuneDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_nascita_provincia_cod",
	            valoreCellaString("aura_nascita_provincia_cod", "AURA",
	                    oggettoOriginale.getAuraNascitaProvinciaCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_nascita_provincia_desc",
	            valoreCellaString("aura_nascita_provincia_desc", "AURA",
	                    oggettoOriginale.getAuraNascitaProvinciaDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_nascita_stato_cod",
	            valoreCellaString("aura_nascita_stato_cod", "AURA",
	                    oggettoOriginale.getAuraNascitaStatoCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_nascita_stato_desc",
	            valoreCellaString("aura_nascita_stato_desc", "AURA",
	                    oggettoOriginale.getAuraNascitaStatoDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_cittadinanza_stato_cod",
	            valoreCellaString("aura_cittadinanza_stato_cod", "AURA",
	                    oggettoOriginale.getAuraCittadinanzaStatoCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_cittadinanza_stato_desc",
	            valoreCellaString("aura_cittadinanza_stato_desc", "AURA",
	                    oggettoOriginale.getAuraCittadinanzaStatoDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_comune_cod",
	            valoreCellaString("aura_domicilio_comune_cod", "AURA",
	                    oggettoOriginale.getAuraDomicilioComuneCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_comune_desc",
	            valoreCellaString("aura_domicilio_comune_desc", "AURA",
	                    oggettoOriginale.getAuraDomicilioComuneDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_provincia_cod",
	            valoreCellaString("aura_domicilio_provincia_cod", "AURA",
	                    oggettoOriginale.getAuraDomicilioProvinciaCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_provincia_desc",
	            valoreCellaString("aura_domicilio_provincia_desc", "AURA",
	                    oggettoOriginale.getAuraDomicilioProvinciaDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_stato_cod",
	            valoreCellaString("aura_domicilio_stato_cod", "AURA",
	                    oggettoOriginale.getAuraDomicilioStatoCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_stato_desc",
	            valoreCellaString("aura_domicilio_stato_desc", "AURA",
	                    oggettoOriginale.getAuraDomicilioStatoDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_cap",
	            valoreCellaString("aura_domicilio_cap", "AURA",
	                    oggettoOriginale.getAuraDomicilioCap(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_indirizzo",
	            valoreCellaString("aura_domicilio_indirizzo", "AURA",
	                    oggettoOriginale.getAuraDomicilioIndirizzo(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_domicilio_numero_civico",
	            valoreCellaString("aura_domicilio_numero_civico", "AURA",
	                    oggettoOriginale.getAuraDomicilioNumeroCivico(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_comune_cod",
	            valoreCellaString("aura_residenza_comune_cod", "AURA",
	                    oggettoOriginale.getAuraResidenzaComuneCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_comune_desc",
	            valoreCellaString("aura_residenza_comune_desc", "AURA",
	                    oggettoOriginale.getAuraResidenzaComuneDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_provincia_cod",
	            valoreCellaString("aura_residenza_provincia_cod", "AURA",
	                    oggettoOriginale.getAuraResidenzaProvinciaCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_provincia_desc",
	            valoreCellaString("aura_residenza_provincia_desc", "AURA",
	                    oggettoOriginale.getAuraResidenzaProvinciaDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_stato_cod",
	            valoreCellaString("aura_residenza_stato_cod", "AURA",
	                    oggettoOriginale.getAuraResidenzaStatoCod(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_stato_desc",
	            valoreCellaString("aura_residenza_stato_desc", "AURA",
	                    oggettoOriginale.getAuraResidenzaStatoDesc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_cap",
	            valoreCellaString("aura_residenza_cap", "AURA",
	                    oggettoOriginale.getAuraResidenzaCap(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_indirizzo",
	            valoreCellaString("aura_residenza_indirizzo", "AURA",
	                    oggettoOriginale.getAuraResidenzaIndirizzo(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_residenza_numero_civico",
	            valoreCellaString("aura_residenza_numero_civico", "AURA",
	                    oggettoOriginale.getAuraResidenzaNumeroCivico(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_tessera_team",
	            valoreCellaString("aura_tessera_team", "AURA",
	                    oggettoOriginale.getAuraTesseraTeam(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_id_aura",
	            valoreCellaString("aura_id_aura", "AURA",
	                    oggettoOriginale.getAuraIdAura(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_email_aura",
	            valoreCellaString("aura_email_aura", "AURA",
	                    oggettoOriginale.getAuraEmailAura(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_telefono_aura",
	            valoreCellaString("aura_telefono_aura", "AURA",
	                    oggettoOriginale.getAuraTelefonoAura(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_data_decesso",
	            valoreCellaString("aura_data_decesso", "AURA",
	                    oggettoOriginale.getAuraDataDecesso(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("aura_assistenza_asl_fine",
	            valoreCellaString("aura_assistenza_asl_fine", "AURA",
	                    oggettoOriginale.getAuraAssistenzaAslFine(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    // ── SOGGETTO ──────────────────────────────────────────────────────
	    response.put("soggetto_email",
	            valoreCellaString("soggetto_email", "SOGGETTO",
	                    oggettoOriginale.getEmail(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("soggetto_telefono",
	            valoreCellaString("soggetto_telefono", "SOGGETTO",
	                    oggettoOriginale.getTelefono(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    // ── ANAMNESI ─────────────────────────────────────────────────────
	    response.put("anamnesi_codice_fiscale",
	            valoreCellaString("anamnesi_codice_fiscale", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiCodiceFiscale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_id_aura",
	            valoreCellaString("anamnesi_id_aura", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiIdAura(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_data_intervista",
	            valoreCellaString("anamnesi_data_intervista", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiDataIntervista(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_nominativo_intervistatore",
	            valoreCellaString("anamnesi_nominativo_intervistatore", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiNominativoIntervistatore(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_fumatore",
	            valoreCellaString("anamnesi_fumatore", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiFumatore(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigarette",
	            valoreCellaString("anamnesi_sigarette", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigarette(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigarette_anni",
	            valoreCellaString("anamnesi_sigarette_anni", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigaretteAnni(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigarette_eta_inizio",
	            valoreCellaString("anamnesi_sigarette_eta_inizio", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigaretteEtaInizio(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigarette_fuma_attualmente",
	            valoreCellaString("anamnesi_sigarette_fuma_attualmente", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigaretteFumaAttualmente(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigarette_eta_fine",
	            valoreCellaString("anamnesi_sigarette_eta_fine", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigaretteEtaFine(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigarette_die",
	            valoreCellaString("anamnesi_sigarette_die", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigaretteDie(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigari",
	            valoreCellaString("anamnesi_sigari", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigari(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigari_anni",
	            valoreCellaString("anamnesi_sigari_anni", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigariAnni(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigari_eta_inizio",
	            valoreCellaString("anamnesi_sigari_eta_inizio", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigariEtaInizio(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigari_fuma_attualmente",
	            valoreCellaString("anamnesi_sigari_fuma_attualmente", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigariFumaAttualmente(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigari_eta_fine",
	            valoreCellaString("anamnesi_sigari_eta_fine", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigariEtaFine(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_sigari_die",
	            valoreCellaString("anamnesi_sigari_die", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiSigariDie(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_pipa",
	            valoreCellaString("anamnesi_pipa", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiPipa(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_pipa_anni",
	            valoreCellaString("anamnesi_pipa_anni", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiPipaAnni(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_pipa_eta_inizio",
	            valoreCellaString("anamnesi_pipa_eta_inizio", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiPipaEtaInizio(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_pipa_fuma_attualmente",
	            valoreCellaString("anamnesi_pipa_fuma_attualmente", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiPipaFumaAttualmente(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_pipa_eta_fine",
	            valoreCellaString("anamnesi_pipa_eta_fine", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiPipaEtaFine(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_pipa_die",
	            valoreCellaString("anamnesi_pipa_die", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiPipaDie(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_num",
	            valoreCellaString("anamnesi_occupazione_num", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneNum(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_anno_inizio",
	            valoreCellaString("anamnesi_occupazione_anno_inizio", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneAnnoInizio(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_anno_fine",
	            valoreCellaString("anamnesi_occupazione_anno_fine", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneAnnoFine(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_tipo",
	            valoreCellaString("anamnesi_occupazione_tipo", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneTipo(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_descrizione_lavoro",
	            valoreCellaString("anamnesi_occupazione_descrizione_lavoro", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneDescrizioneLavoro(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_nome_e_indirizzo_ditta",
	            valoreCellaString("anamnesi_occupazione_nome_e_indirizzo_ditta", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneNomeEIndirizzoDitta(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_attivita_ditta",
	            valoreCellaString("anamnesi_occupazione_attivita_ditta", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneAttivitaDitta(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_nota_attivita_con_amianto",
	            valoreCellaString("anamnesi_nota_attivita_con_amianto", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiNotaAttivitaConAmianto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_anamnesi_esposizione_amianto",
	            valoreCellaString("anamnesi_anamnesi_esposizione_amianto", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiAnamnesiEsposizioneAmianto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_esposizione_professionale",
	            valoreCellaString("anamnesi_esposizione_professionale", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiEsposizioneProfessionale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_anno_fine_esposizione",
	            valoreCellaString("anamnesi_anno_fine_esposizione", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiAnnoFineEsposizione(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_livello_esposizione",
	            valoreCellaString("anamnesi_livello_esposizione", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiLivelloEsposizione(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_inserimento_in_sorveglianza",
	            valoreCellaString("anamnesi_inserimento_in_sorveglianza", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiInserimentoInSorveglianza(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_id_spresal",
	            valoreCellaString("anamnesi_id_spresal", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiIdSpresal(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_counseling",
	            valoreCellaString("anamnesi_counseling", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiCounseling(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_esposizione_crpt",
	            valoreCellaString("anamnesi_occupazione_esposizione_crpt", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneEsposizioneCrpt(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_settore_ditta_crpt",
	            valoreCellaString("anamnesi_occupazione_settore_ditta_crpt", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneSettoreDittaCrpt(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_mansione_crpt",
	            valoreCellaString("anamnesi_occupazione_mansione_crpt", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneMansioneCrpt(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_ragione_sociale_ditta_crpt",
	            valoreCellaString("anamnesi_occupazione_ragione_sociale_ditta_crpt", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneRagioneSocialeDittaCrpt(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_piva_ditta_crpt",
	            valoreCellaString("anamnesi_occupazione_piva_ditta_crpt", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazionePivaDittaCrpt(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("anamnesi_occupazione_codice_fiscale_ditta_crpt",
	            valoreCellaString("anamnesi_occupazione_codice_fiscale_ditta_crpt", "ANAMNESI",
	                    oggettoOriginale.getAnamnesiOccupazioneCodiceFiscaleDittaCrpt(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    response.put("esiti_vis_codice_fiscale",
	            valoreCellaString("esiti_vis_codice_fiscale", "ESITI",
	                    oggettoOriginale.getEsitiVisCodiceFiscale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_id_aura",
	            valoreCellaString("esiti_vis_id_aura", "ESITI",
	                    oggettoOriginale.getEsitiVisIdAura(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_data_visita",
	            valoreCellaString("esiti_vis_data_visita", "ESITI",
	                    oggettoOriginale.getEsitiVisDataVisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_visita",
	            valoreCellaString("esiti_vis_visita", "ESITI",
	                    oggettoOriginale.getEsitiVisVisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_livello_visita",
	            valoreCellaString("esiti_vis_livello_visita", "ESITI",
	                    oggettoOriginale.getEsitiVisLivelloVisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_riceve_indennizzo",
	            valoreCellaString("esiti_vis_riceve_indennizzo", "ESITI",
	                    oggettoOriginale.getEsitiVisRiceveIndennizzo(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_malattia_indennizzo",
	            valoreCellaString("esiti_vis_malattia_indennizzo", "ESITI",
	                    oggettoOriginale.getEsitiVisMalattiaIndennizzo(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    response.put("esiti_vis_accertamenti_rx",
	            valoreCellaString("esiti_vis_accertamenti_rx", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiRx(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_rx_data",
	            valoreCellaString("esiti_vis_accertamenti_rx_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiRxData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_rx_referto_normale",
	            valoreCellaString("esiti_vis_accertamenti_rx_referto_normale", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiRxRefertoNormale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_rx_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_rx_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiRxAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_tc",
	            valoreCellaString("esiti_vis_accertamenti_tc", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiTc(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_tc_data",
	            valoreCellaString("esiti_vis_accertamenti_tc_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiTcData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_tc_referto_normale",
	            valoreCellaString("esiti_vis_accertamenti_tc_referto_normale", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiTcRefertoNormale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_tc_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_tc_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiTcAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    response.put("esiti_vis_accertamenti_spirometria_semplice",
	            valoreCellaString("esiti_vis_accertamenti_spirometria_semplice", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiSpirometriaSemplice(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_spirometria_semplice_data",
	            valoreCellaString("esiti_vis_accertamenti_spirometria_semplice_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiSpirometriaSempliceData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_spirometria_semplice_referto_normale",
	            valoreCellaString("esiti_vis_accertamenti_spirometria_semplice_referto_normale", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiSpirometriaSempliceRefertoNormale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_spirometria_semplice_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_spirometria_semplice_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiSpirometriaSempliceAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_spirometria_globale",
	            valoreCellaString("esiti_vis_accertamenti_spirometria_globale", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiSpirometriaGlobale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_spirometria_globale_data",
	            valoreCellaString("esiti_vis_accertamenti_spirometria_globale_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiSpirometriaGlobaleData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_spirometria_globale_referto_normale",
	            valoreCellaString("esiti_vis_accertamenti_spirometria_globale_referto_normale", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiSpirometriaGlobaleRefertoNormale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_spirometria_globale_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_spirometria_globale_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiSpirometriaGlobaleAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_dlco",
	            valoreCellaString("esiti_vis_accertamenti_dlco", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiDlco(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_dlco_data",
	            valoreCellaString("esiti_vis_accertamenti_dlco_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiDlcoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_dlco_referto_normale",
	            valoreCellaString("esiti_vis_accertamenti_dlco_referto_normale", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiDlcoRefertoNormale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_dlco_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_dlco_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiDlcoAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_pet",
	            valoreCellaString("esiti_vis_accertamenti_pet", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiPet(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_pet_data",
	            valoreCellaString("esiti_vis_accertamenti_pet_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiPetData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_pet_referto_normale",
	            valoreCellaString("esiti_vis_accertamenti_pet_referto_normale", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiPetRefertoNormale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_pet_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_pet_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiPetAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    response.put("esiti_vis_accertamenti_visita_pneumologica",
	            valoreCellaString("esiti_vis_accertamenti_visita_pneumologica", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaPneumologica(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_pneumologica_data",
	            valoreCellaString("esiti_vis_accertamenti_visita_pneumologica_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaPneumologicaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_pneumologica_referto",
	            valoreCellaString("esiti_vis_accertamenti_visita_pneumologica_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaPneumologicaReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_pneumologica_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_visita_pneumologica_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaPneumologicaAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_radiologica",
	            valoreCellaString("esiti_vis_accertamenti_visita_radiologica", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaRadiologica(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_radiologica_data",
	            valoreCellaString("esiti_vis_accertamenti_visita_radiologica_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaRadiologicaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_radiologica_referto",
	            valoreCellaString("esiti_vis_accertamenti_visita_radiologica_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaRadiologicaReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_radiologica_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_visita_radiologica_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaRadiologicaAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_oncologica",
	            valoreCellaString("esiti_vis_accertamenti_visita_oncologica", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaOncologica(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_oncologica_data",
	            valoreCellaString("esiti_vis_accertamenti_visita_oncologica_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaOncologicaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_oncologica_referto",
	            valoreCellaString("esiti_vis_accertamenti_visita_oncologica_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaOncologicaReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_visita_oncologica_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_visita_oncologica_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiVisitaOncologicaAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_altro",
	            valoreCellaString("esiti_vis_accertamenti_altro", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiAltro(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_altro_descrizione",
	            valoreCellaString("esiti_vis_accertamenti_altro_descrizione", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiAltroDescrizione(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_altro_data",
	            valoreCellaString("esiti_vis_accertamenti_altro_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiAltroData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_altro_referto_normale",
	            valoreCellaString("esiti_vis_accertamenti_altro_referto_normale", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiAltroRefertoNormale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_accertamenti_altro_referto_acquisita",
	            valoreCellaString("esiti_vis_accertamenti_altro_referto_acquisita", "ESITI",
	                    oggettoOriginale.getEsitiVisAccertamentiAltroRefertoAcquisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    response.put("esiti_vis_risultato_negativo",
	            valoreCellaString("esiti_vis_risultato_negativo", "ESITI",
	                    oggettoOriginale.getEsitiVisRisultatoNegativo(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppm_placche_pleuriche_monolaterali",
	            valoreCellaString("esiti_vis_ppm_placche_pleuriche_monolaterali", "ESITI",
	                    oggettoOriginale.getEsitiVisPpmPlacchePleuricheMonolaterali(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppm_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_ppm_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisPpmPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppm_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_ppm_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisPpmPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppm_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_ppm_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisPpmAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppm_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_ppm_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisPpmAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppm_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_ppm_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisPpmPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppm_referto",
	            valoreCellaString("esiti_vis_ppm_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisPpmReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppm_referto_data",
	            valoreCellaString("esiti_vis_ppm_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisPpmRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppb_placche_pleuriche_bilaterali",
	            valoreCellaString("esiti_vis_ppb_placche_pleuriche_bilaterali", "ESITI",
	                    oggettoOriginale.getEsitiVisPpbPlacchePleuricheBilaterali(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppb_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_ppb_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisPpbPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppb_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_ppb_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisPpbPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppb_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_ppb_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisPpbAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppb_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_ppb_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisPpbAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppb_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_ppb_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisPpbPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppb_referto",
	            valoreCellaString("esiti_vis_ppb_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisPpbReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ppb_referto_data",
	            valoreCellaString("esiti_vis_ppb_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisPpbRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ap_asbestosi_polmonare",
	            valoreCellaString("esiti_vis_ap_asbestosi_polmonare", "ESITI",
	                    oggettoOriginale.getEsitiVisApAsbestosiPolmonare(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ap_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_ap_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisApPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ap_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_ap_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisApPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ap_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_ap_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisApAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ap_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_ap_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisApAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    response.put("esiti_vis_ap_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_ap_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisApPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ap_referto",
	            valoreCellaString("esiti_vis_ap_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisApReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_ap_referto_data",
	            valoreCellaString("esiti_vis_ap_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisApRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_fpd_fibrosi_pleurica_diffusa",
	            valoreCellaString("esiti_vis_fpd_fibrosi_pleurica_diffusa", "ESITI",
	                    oggettoOriginale.getEsitiVisFpdFibrosiPleuricaDiffusa(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_fpd_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_fpd_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisFpdPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_fpd_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_fpd_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisFpdPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_fpd_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_fpd_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisFpdAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_fpd_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_fpd_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisFpdAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_fpd_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_fpd_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisFpdPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_fpd_referto",
	            valoreCellaString("esiti_vis_fpd_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisFpdReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_fpd_referto_data",
	            valoreCellaString("esiti_vis_fpd_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisFpdRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_mp_mesotelioma_pleurico",
	            valoreCellaString("esiti_vis_mp_mesotelioma_pleurico", "ESITI",
	                    oggettoOriginale.getEsitiVisMpMesoteliomaPleurico(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_mp_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_mp_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisMpPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_mp_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_mp_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisMpPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_mp_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_mp_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisMpAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_mp_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_mp_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisMpAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    response.put("esiti_vis_mp_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_mp_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisMpPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_mp_referto",
	            valoreCellaString("esiti_vis_mp_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisMpReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_mp_referto_data",
	            valoreCellaString("esiti_vis_mp_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisMpRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_mp_comunicazione_al_cor",
	            valoreCellaString("esiti_vis_mp_comunicazione_al_cor", "ESITI",
	                    oggettoOriginale.getEsitiVisMpComunicazioneAlCor(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_mp_comunicazione_al_cor_data",
	            valoreCellaString("esiti_vis_mp_comunicazione_al_cor_data", "ESITI",
	                    oggettoOriginale.getEsitiVisMpComunicazioneAlCorData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_altro_mesotelioma",
	            valoreCellaString("esiti_vis_am_altro_mesotelioma", "ESITI",
	                    oggettoOriginale.getEsitiVisAmAltroMesotelioma(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_am_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisAmPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_am_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAmPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_am_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisAmAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_am_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAmAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_am_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisAmPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_referto",
	            valoreCellaString("esiti_vis_am_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisAmReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_referto_data",
	            valoreCellaString("esiti_vis_am_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAmRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_comunicazione_al_cor",
	            valoreCellaString("esiti_vis_am_comunicazione_al_cor", "ESITI",
	                    oggettoOriginale.getEsitiVisAmComunicazioneAlCor(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_am_comunicazione_al_cor_data",
	            valoreCellaString("esiti_vis_am_comunicazione_al_cor_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAmComunicazioneAlCorData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    response.put("esiti_vis_nl_neoplasia_laringe",
	            valoreCellaString("esiti_vis_nl_neoplasia_laringe", "ESITI",
	                    oggettoOriginale.getEsitiVisNlNeoplasiaLaringe(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_nl_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_nl_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisNlPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_nl_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_nl_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisNlPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_nl_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_nl_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisNlAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_nl_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_nl_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisNlAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_nl_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_nl_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisNlPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_nl_referto",
	            valoreCellaString("esiti_vis_nl_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisNlReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_nl_referto_data",
	            valoreCellaString("esiti_vis_nl_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisNlRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_no_neoplasia_ovarica",
	            valoreCellaString("esiti_vis_no_neoplasia_ovarica", "ESITI",
	                    oggettoOriginale.getEsitiVisNoNeoplasiaOvarica(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_no_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_no_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisNoPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_no_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_no_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisNoPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_no_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_no_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisNoAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_no_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_no_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisNoAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_no_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_no_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisNoPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_no_referto",
	            valoreCellaString("esiti_vis_no_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisNoReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_no_referto_data",
	            valoreCellaString("esiti_vis_no_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisNoRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_tp_tumore_del_polmone",
	            valoreCellaString("esiti_vis_tp_tumore_del_polmone", "ESITI",
	                    oggettoOriginale.getEsitiVisTpTumoreDelPolmone(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_tp_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_tp_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisTpPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_tp_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_tp_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisTpPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_tp_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_tp_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisTpAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_tp_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_tp_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisTpAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    response.put("esiti_vis_tp_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_tp_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisTpPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_tp_referto",
	            valoreCellaString("esiti_vis_tp_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisTpReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_tp_referto_data",
	            valoreCellaString("esiti_vis_tp_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisTpRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_tp_comunicazione_al_cor",
	            valoreCellaString("esiti_vis_tp_comunicazione_al_cor", "ESITI",
	                    oggettoOriginale.getEsitiVisTpComunicazioneAlCor(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_tp_comunicazione_al_cor_data",
	            valoreCellaString("esiti_vis_tp_comunicazione_al_cor_data", "ESITI",
	                    oggettoOriginale.getEsitiVisTpComunicazioneAlCorData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_bpco_enfisema_polmonare",
	            valoreCellaString("esiti_vis_bpco_enfisema_polmonare", "ESITI",
	                    oggettoOriginale.getEsitiVisBpcoEnfisemaPolmonare(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_bpco_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_bpco_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisBpcoPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_bpco_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_bpco_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisBpcoPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_bpco_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_bpco_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisBpcoAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_bpco_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_bpco_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisBpcoAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_bpco_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_bpco_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisBpcoPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_bpco_referto",
	            valoreCellaString("esiti_vis_bpco_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisBpcoReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_bpco_referto_data",
	            valoreCellaString("esiti_vis_bpco_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisBpcoRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_altra_diagnosi",
	            valoreCellaString("esiti_vis_altra_diagnosi", "ESITI",
	                    oggettoOriginale.getEsitiVisAltraDiagnosi(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_altra_diagnosi_descrizione",
	            valoreCellaString("esiti_vis_altra_diagnosi_descrizione", "ESITI",
	                    oggettoOriginale.getEsitiVisAltraDiagnosiDescrizione(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_altra_primo_certificato_e_denuncia",
	            valoreCellaString("esiti_vis_altra_primo_certificato_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisAltraPrimoCertificatoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_altra_primo_certificato_e_denuncia_data",
	            valoreCellaString("esiti_vis_altra_primo_certificato_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAltraPrimoCertificatoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_altra_aggravamento_e_denuncia",
	            valoreCellaString("esiti_vis_altra_aggravamento_e_denuncia", "ESITI",
	                    oggettoOriginale.getEsitiVisAltraAggravamentoEDenuncia(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_altra_aggravamento_e_denuncia_data",
	            valoreCellaString("esiti_vis_altra_aggravamento_e_denuncia_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAltraAggravamentoEDenunciaData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_altra_percentuale_di_riconoscimento",
	            valoreCellaString("esiti_vis_altra_percentuale_di_riconoscimento", "ESITI",
	                    oggettoOriginale.getEsitiVisAltraPercentualeDiRiconoscimento(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_altra_referto",
	            valoreCellaString("esiti_vis_altra_referto", "ESITI",
	                    oggettoOriginale.getEsitiVisAltraReferto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_altra_referto_data",
	            valoreCellaString("esiti_vis_altra_referto_data", "ESITI",
	                    oggettoOriginale.getEsitiVisAltraRefertoData(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_follow_up_previsto",
	            valoreCellaString("esiti_vis_follow_up_previsto", "ESITI",
	                    oggettoOriginale.getEsitiVisFollowUpPrevisto(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_anno_presunto_prossima_visita",
	            valoreCellaString("esiti_vis_anno_presunto_prossima_visita", "ESITI",
	                    oggettoOriginale.getEsitiVisAnnoPresuntoProssimaVisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_anno_ultima_visita",
	            valoreCellaString("esiti_vis_anno_ultima_visita", "ESITI",
	                    oggettoOriginale.getEsitiVisAnnoUltimaVisita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_invio_sintesi_a_mmg",
	            valoreCellaString("esiti_vis_invio_sintesi_a_mmg", "ESITI",
	                    oggettoOriginale.getEsitiVisInvioSintesiAMmg(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("esiti_vis_id_spresal",
	            valoreCellaString("esiti_vis_id_spresal", "ESITI",
	                    oggettoOriginale.getEsitiVisIdSpresal(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    
	    // ── INAIL
	    response.put("inail_domanda",
	            valoreCellaString("inail_domanda", "INAIL",
	                    oggettoOriginale.getInailDomanda(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_cognome",
	            valoreCellaString("inail_cognome", "INAIL",
	                    oggettoOriginale.getInailCognome(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_nome",
	            valoreCellaString("inail_nome", "INAIL",
	                    oggettoOriginale.getInailNome(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_codice_fiscale",
	            valoreCellaString("inail_codice_fiscale", "INAIL",
	                    oggettoOriginale.getInailCodiceFiscale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_sesso",
	            valoreCellaString("inail_sesso", "INAIL",
	                    oggettoOriginale.getInailSesso(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_data_nascita",
	            valoreCellaString("inail_data_nascita", "INAIL",
	                    oggettoOriginale.getInailDataNascita(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_indirizzo_residenza",
	            valoreCellaString("inail_indirizzo_residenza", "INAIL",
	                    oggettoOriginale.getInailIndirizzoResidenza(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_istat_residenza",
	            valoreCellaString("inail_istat_residenza", "INAIL",
	                    oggettoOriginale.getInailIstatResidenza(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_cap_residenza",
	            valoreCellaString("inail_cap_residenza", "INAIL",
	                    oggettoOriginale.getInailCapResidenza(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_regione_residenza",
	            valoreCellaString("inail_regione_residenza", "INAIL",
	                    oggettoOriginale.getInailRegioneResidenza(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_provincia_residenza",
	            valoreCellaString("inail_provincia_residenza", "INAIL",
	                    oggettoOriginale.getInailProvinciaResidenza(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("inail_comune_residenza",
	            valoreCellaString("inail_comune_residenza", "INAIL",
	                    oggettoOriginale.getInailComuneResidenza(),
	                    flagDatiAnonimizzati, mapCampiMascherati));
	    
	    // ── NPLA
	    response.put("npla_codice_fiscale",
	            valoreCellaString("npla_codice_fiscale", "NPLA",
	                    oggettoOriginale.getNplaCodiceFiscale(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_periodo",
	            valoreCellaString("npla_periodo", "NPLA",
	                    oggettoOriginale.getNplaPeriodo(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_id_cantiere",
	            valoreCellaString("npla_id_cantiere", "NPLA",
	                    oggettoOriginale.getNplaIdCantiere(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_azienda_piva",
	            valoreCellaString("npla_azienda_piva", "NPLA",
	                    oggettoOriginale.getNplaAziendaPiva(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_azienda_nome",
	            valoreCellaString("npla_azienda_nome", "NPLA",
	                    oggettoOriginale.getNplaAziendaNome(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_asl_cantiere",
	            valoreCellaString("npla_asl_cantiere", "NPLA",
	                    oggettoOriginale.getNplaAslCantiere(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_comune_cantiere",
	            valoreCellaString("npla_comune_cantiere", "NPLA",
	                    oggettoOriginale.getNplaComuneCantiere(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_anno",
	            valoreCellaString("npla_anno", "NPLA",
	                    oggettoOriginale.getNplaAnno(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_tipologia_piano",
	            valoreCellaString("npla_tipologia_piano", "NPLA",
	                    oggettoOriginale.getNplaTipologiaPiano(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_quantita_da_rimuovere",
	            valoreCellaString("npla_quantita_da_rimuovere", "NPLA",
	                    oggettoOriginale.getNplaQuantitaDaRimuovere(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

	    response.put("npla_quantita_rimossa",
	            valoreCellaString("npla_quantita_rimossa", "NPLA",
	                    oggettoOriginale.getNplaQuantitaRimossa(),
	                    flagDatiAnonimizzati, mapCampiMascherati));

		return response;
	}
//	private ExportDTO modificaValoriAnonimizzati(ExportDTO oggettoOriginale) {
//
//	    boolean flagDatiAnonimizzati = true;
//	    ExportDTO exportDTOModificato = new ExportDTO();
//
//	    if (oggettoOriginale == null) {
//	        return exportDTOModificato;
//	    }
//
//	    // Carica la mappa dei campi mascherati
//	    Map<String, List<String>> mapCampiMascherati = anagraficaRepository.buildCampiMascheratiMap();
//
//	    // ── ADESIONE ──────────────────────────────────────────────────
//
//	    exportDTOModificato.setAdesioneCodiceAdesione(
//	            valoreCella("adesione_codice_adesione", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCodiceAdesione(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneDataAdesione(
//	            valoreCella("adesione_data_adesione", "ADESIONE",
//	                    oggettoOriginale.getAdesioneDataAdesione(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneCodiceFiscale(
//	            valoreCella("adesione_codice_fiscale", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCodiceFiscale(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneCognome(
//	            valoreCella("adesione_cognome", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCognome(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneNome(
//	            valoreCella("adesione_nome", "ADESIONE",
//	                    oggettoOriginale.getAdesioneNome(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneDataDiNascita(
//	            valoreCella("adesione_data_di_nascita", "ADESIONE",
//	                    oggettoOriginale.getAdesioneDataDiNascita(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneProvinciaDiNascita(
//	            valoreCella("adesione_provincia_di_nascita", "ADESIONE",
//	                    oggettoOriginale.getAdesioneProvinciaDiNascita(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneComuneDiNascita(
//	            valoreCella("adesione_comune_di_nascita", "ADESIONE",
//	                    oggettoOriginale.getAdesioneComuneDiNascita(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneTesseraTeam(
//	            valoreCella("adesione_tessera_team", "ADESIONE",
//	                    oggettoOriginale.getAdesioneTesseraTeam(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneIdAura(
//	            valoreCella("adesione_id_aura", "ADESIONE",
//	                    oggettoOriginale.getAdesioneIdAura(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneProvinciaDiDomicilio(
//	            valoreCella("adesione_provincia_di_domicilio", "ADESIONE",
//	                    oggettoOriginale.getAdesioneProvinciaDiDomicilio(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneComuneDiDomicilio(
//	            valoreCella("adesione_comune_di_domicilio", "ADESIONE",
//	                    oggettoOriginale.getAdesioneComuneDiDomicilio(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneCodiceComuneIstatDiDomicilio(
//	            valoreCella("adesione_codice_comune_istat_di_domicilio", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCodiceComuneIstatDiDomicilio(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneCapDiDomicilio(
//	            valoreCella("adesione_cap_di_domicilio", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCapDiDomicilio(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneEmail(
//	            valoreCella("adesione_email", "ADESIONE",
//	                    oggettoOriginale.getAdesioneEmail(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneTelefono(
//	            valoreCella("adesione_telefono", "ADESIONE",
//	                    oggettoOriginale.getAdesioneTelefono(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneCodiceAslDiDomicilio(
//	            valoreCella("adesione_codice_asl_di_domicilio", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCodiceAslDiDomicilio(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneAslDiDomicilio(
//	            valoreCella("adesione_asl_di_domicilio", "ADESIONE",
//	                    oggettoOriginale.getAdesioneAslDiDomicilio(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneCodazi(
//	            valoreCella("adesione_codazi", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCodazi(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneCodiceAslDiResidenza(
//	            valoreCella("adesione_codice_asl_di_residenza", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCodiceAslDiResidenza(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneAslDiResidenza(
//	            valoreCella("adesione_asl_di_residenza", "ADESIONE",
//	                    oggettoOriginale.getAdesioneAslDiResidenza(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneDataInizioEsposizione(
//	            valoreCella("adesione_data_inizio_esposizione", "ADESIONE",
//	                    oggettoOriginale.getAdesioneDataInizioEsposizione(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneDataFineEsposizione(
//	            valoreCella("adesione_data_fine_esposizione", "ADESIONE",
//	                    oggettoOriginale.getAdesioneDataFineEsposizione(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneAzienda(
//	            valoreCella("adesione_azienda", "ADESIONE",
//	                    oggettoOriginale.getAdesioneAzienda(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneCodiceComuneIstatAzienda(
//	            valoreCella("adesione_codice_comune_istat_azienda", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCodiceComuneIstatAzienda(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneComuneAzienda(
//	            valoreCella("adesione_comune_azienda", "ADESIONE",
//	                    oggettoOriginale.getAdesioneComuneAzienda(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneCapAzienda(
//	            valoreCella("adesione_cap_azienda", "ADESIONE",
//	                    oggettoOriginale.getAdesioneCapAzienda(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneProvinciaAzienda(
//	            valoreCella("adesione_provincia_azienda", "ADESIONE",
//	                    oggettoOriginale.getAdesioneProvinciaAzienda(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//
//	    exportDTOModificato.setAdesioneMansione(
//	            valoreCella("adesione_mansione", "ADESIONE",
//	                    oggettoOriginale.getAdesioneMansione(),
//	                    flagDatiAnonimizzati, mapCampiMascherati)
//	    );
//	
//
//	        // ── AURA ─────────────────────────────────────────────────────
//
//	        exportDTOModificato.setAuraCodiceFiscale(
//	                valoreCella("aura_codice_fiscale", "AURA",
//	                        oggettoOriginale.getAuraCodiceFiscale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioAsl(
//	                valoreCella("aura_domicilio_asl", "AURA",
//	                        oggettoOriginale.getAuraDomicilioAsl(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaAsl(
//	                valoreCella("aura_residenza_asl", "AURA",
//	                        oggettoOriginale.getAuraResidenzaAsl(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraAssistenzaAsl(
//	                valoreCella("aura_assistenza_asl", "AURA",
//	                        oggettoOriginale.getAuraAssistenzaAsl(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraNome(
//	                valoreCella("aura_nome", "AURA",
//	                        oggettoOriginale.getAuraNome(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraCognome(
//	                valoreCella("aura_cognome", "AURA",
//	                        oggettoOriginale.getAuraCognome(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraSesso(
//	                valoreCella("aura_sesso", "AURA",
//	                        oggettoOriginale.getAuraSesso(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraNascitaData(
//	                valoreCella("aura_nascita_data", "AURA",
//	                        oggettoOriginale.getAuraNascitaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraNascitaComuneCod(
//	                valoreCella("aura_nascita_comune_cod", "AURA",
//	                        oggettoOriginale.getAuraNascitaComuneCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraNascitaComuneDesc(
//	                valoreCella("aura_nascita_comune_desc", "AURA",
//	                        oggettoOriginale.getAuraNascitaComuneDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraNascitaProvinciaCod(
//	                valoreCella("aura_nascita_provincia_cod", "AURA",
//	                        oggettoOriginale.getAuraNascitaProvinciaCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraNascitaProvinciaDesc(
//	                valoreCella("aura_nascita_provincia_desc", "AURA",
//	                        oggettoOriginale.getAuraNascitaProvinciaDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraNascitaStatoCod(
//	                valoreCella("aura_nascita_stato_cod", "AURA",
//	                        oggettoOriginale.getAuraNascitaStatoCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraNascitaStatoDesc(
//	                valoreCella("aura_nascita_stato_desc", "AURA",
//	                        oggettoOriginale.getAuraNascitaStatoDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraCittadinanzaStatoCod(
//	                valoreCella("aura_cittadinanza_stato_cod", "AURA",
//	                        oggettoOriginale.getAuraCittadinanzaStatoCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraCittadinanzaStatoDesc(
//	                valoreCella("aura_cittadinanza_stato_desc", "AURA",
//	                        oggettoOriginale.getAuraCittadinanzaStatoDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioComuneCod(
//	                valoreCella("aura_domicilio_comune_cod", "AURA",
//	                        oggettoOriginale.getAuraDomicilioComuneCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioComuneDesc(
//	                valoreCella("aura_domicilio_comune_desc", "AURA",
//	                        oggettoOriginale.getAuraDomicilioComuneDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioProvinciaCod(
//	                valoreCella("aura_domicilio_provincia_cod", "AURA",
//	                        oggettoOriginale.getAuraDomicilioProvinciaCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioProvinciaDesc(
//	                valoreCella("aura_domicilio_provincia_desc", "AURA",
//	                        oggettoOriginale.getAuraDomicilioProvinciaDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioStatoCod(
//	                valoreCella("aura_domicilio_stato_cod", "AURA",
//	                        oggettoOriginale.getAuraDomicilioStatoCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioStatoDesc(
//	                valoreCella("aura_domicilio_stato_desc", "AURA",
//	                        oggettoOriginale.getAuraDomicilioStatoDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioCap(
//	                valoreCella("aura_domicilio_cap", "AURA",
//	                        oggettoOriginale.getAuraDomicilioCap(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioIndirizzo(
//	                valoreCella("aura_domicilio_indirizzo", "AURA",
//	                        oggettoOriginale.getAuraDomicilioIndirizzo(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDomicilioNumeroCivico(
//	                valoreCella("aura_domicilio_numero_civico", "AURA",
//	                        oggettoOriginale.getAuraDomicilioNumeroCivico(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaComuneCod(
//	                valoreCella("aura_residenza_comune_cod", "AURA",
//	                        oggettoOriginale.getAuraResidenzaComuneCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaComuneDesc(
//	                valoreCella("aura_residenza_comune_desc", "AURA",
//	                        oggettoOriginale.getAuraResidenzaComuneDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaProvinciaCod(
//	                valoreCella("aura_residenza_provincia_cod", "AURA",
//	                        oggettoOriginale.getAuraResidenzaProvinciaCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaProvinciaDesc(
//	                valoreCella("aura_residenza_provincia_desc", "AURA",
//	                        oggettoOriginale.getAuraResidenzaProvinciaDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaStatoCod(
//	                valoreCella("aura_residenza_stato_cod", "AURA",
//	                        oggettoOriginale.getAuraResidenzaStatoCod(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaStatoDesc(
//	                valoreCella("aura_residenza_stato_desc", "AURA",
//	                        oggettoOriginale.getAuraResidenzaStatoDesc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaCap(
//	                valoreCella("aura_residenza_cap", "AURA",
//	                        oggettoOriginale.getAuraResidenzaCap(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaIndirizzo(
//	                valoreCella("aura_residenza_indirizzo", "AURA",
//	                        oggettoOriginale.getAuraResidenzaIndirizzo(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraResidenzaNumeroCivico(
//	                valoreCella("aura_residenza_numero_civico", "AURA",
//	                        oggettoOriginale.getAuraResidenzaNumeroCivico(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraTesseraTeam(
//	                valoreCella("aura_tessera_team", "AURA",
//	                        oggettoOriginale.getAuraTesseraTeam(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraIdAura(
//	                valoreCella("aura_id_aura", "AURA",
//	                        oggettoOriginale.getAuraIdAura(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraEmailAura(
//	                valoreCella("aura_email_aura", "AURA",
//	                        oggettoOriginale.getAuraEmailAura(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraTelefonoAura(
//	                valoreCella("aura_telefono_aura", "AURA",
//	                        oggettoOriginale.getAuraTelefonoAura(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraDataDecesso(
//	                valoreCella("aura_data_decesso", "AURA",
//	                        oggettoOriginale.getAuraDataDecesso(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAuraAssistenzaAslFine(
//	                valoreCella("aura_assistenza_asl_fine", "AURA",
//	                        oggettoOriginale.getAuraAssistenzaAslFine(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//	        
//	     // ── SOGGETTO ──────────────────────────────────────────────────
//	        exportDTOModificato.setEmail(
//	                valoreCella("soggetto_email", "SOGGETTO",
//	                        oggettoOriginale.getEmail(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setTelefono(
//	                valoreCella("soggetto_telefono", "SOGGETTO",
//	                        oggettoOriginale.getTelefono(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        // ── ANAMNESI ──────────────────────────────────────────────────
//	        exportDTOModificato.setAnamnesiCodiceFiscale(
//	                valoreCella("anamnesi_codice_fiscale", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiCodiceFiscale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiIdAura(
//	                valoreCella("anamnesi_id_aura", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiIdAura(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiDataIntervista(
//	                valoreCella("anamnesi_data_intervista", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiDataIntervista(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiNominativoIntervistatore(
//	                valoreCella("anamnesi_nominativo_intervistatore", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiNominativoIntervistatore(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiFumatore(
//	                valoreCella("anamnesi_fumatore", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiFumatore(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigarette(
//	                valoreCella("anamnesi_sigarette", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigarette(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigaretteAnni(
//	                valoreCella("anamnesi_sigarette_anni", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigaretteAnni(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigaretteEtaInizio(
//	                valoreCella("anamnesi_sigarette_eta_inizio", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigaretteEtaInizio(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigaretteFumaAttualmente(
//	                valoreCella("anamnesi_sigarette_fuma_attualmente", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigaretteFumaAttualmente(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigaretteEtaFine(
//	                valoreCella("anamnesi_sigarette_eta_fine", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigaretteEtaFine(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigaretteDie(
//	                valoreCella("anamnesi_sigarette_die", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigaretteDie(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigari(
//	                valoreCella("anamnesi_sigari", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigari(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigariAnni(
//	                valoreCella("anamnesi_sigari_anni", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigariAnni(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigariEtaInizio(
//	                valoreCella("anamnesi_sigari_eta_inizio", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigariEtaInizio(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigariFumaAttualmente(
//	                valoreCella("anamnesi_sigari_fuma_attualmente", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigariFumaAttualmente(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigariEtaFine(
//	                valoreCella("anamnesi_sigari_eta_fine", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigariEtaFine(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiSigariDie(
//	                valoreCella("anamnesi_sigari_die", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiSigariDie(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiPipa(
//	                valoreCella("anamnesi_pipa", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiPipa(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiPipaAnni(
//	                valoreCella("anamnesi_pipa_anni", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiPipaAnni(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiPipaEtaInizio(
//	                valoreCella("anamnesi_pipa_eta_inizio", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiPipaEtaInizio(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiPipaFumaAttualmente(
//	                valoreCella("anamnesi_pipa_fuma_attualmente", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiPipaFumaAttualmente(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiPipaEtaFine(
//	                valoreCella("anamnesi_pipa_eta_fine", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiPipaEtaFine(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiPipaDie(
//	                valoreCella("anamnesi_pipa_die", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiPipaDie(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneNum(
//	                valoreCella("anamnesi_occupazione_num", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneNum(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneAnnoInizio(
//	                valoreCella("anamnesi_occupazione_anno_inizio", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneAnnoInizio(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneAnnoFine(
//	                valoreCella("anamnesi_occupazione_anno_fine", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneAnnoFine(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneTipo(
//	                valoreCella("anamnesi_occupazione_tipo", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneTipo(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneDescrizioneLavoro(
//	                valoreCella("anamnesi_occupazione_descrizione_lavoro", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneDescrizioneLavoro(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneNomeEIndirizzoDitta(
//	                valoreCella("anamnesi_occupazione_nome_e_indirizzo_ditta", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneNomeEIndirizzoDitta(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneAttivitaDitta(
//	                valoreCella("anamnesi_occupazione_attivita_ditta", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneAttivitaDitta(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiNotaAttivitaConAmianto(
//	                valoreCella("anamnesi_nota_attivita_con_amianto", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiNotaAttivitaConAmianto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiAnamnesiEsposizioneAmianto(
//	                valoreCella("anamnesi_anamnesi_esposizione_amianto", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiAnamnesiEsposizioneAmianto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiEsposizioneProfessionale(
//	                valoreCella("anamnesi_esposizione_professionale", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiEsposizioneProfessionale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiAnnoFineEsposizione(
//	                valoreCella("anamnesi_anno_fine_esposizione", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiAnnoFineEsposizione(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiLivelloEsposizione(
//	                valoreCella("anamnesi_livello_esposizione", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiLivelloEsposizione(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiInserimentoInSorveglianza(
//	                valoreCella("anamnesi_inserimento_in_sorveglianza", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiInserimentoInSorveglianza(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiIdSpresal(
//	                valoreCella("anamnesi_id_spresal", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiIdSpresal(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiCounseling(
//	                valoreCella("anamnesi_counseling", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiCounseling(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneEsposizioneCrpt(
//	                valoreCella("anamnesi_occupazione_esposizione_crpt", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneEsposizioneCrpt(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneSettoreDittaCrpt(
//	                valoreCella("anamnesi_occupazione_settore_ditta_crpt", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneSettoreDittaCrpt(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneMansioneCrpt(
//	                valoreCella("anamnesi_occupazione_mansione_crpt", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneMansioneCrpt(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneRagioneSocialeDittaCrpt(
//	                valoreCella("anamnesi_occupazione_ragione_sociale_ditta_crpt", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneRagioneSocialeDittaCrpt(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazionePivaDittaCrpt(
//	                valoreCella("anamnesi_occupazione_piva_ditta_crpt", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazionePivaDittaCrpt(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setAnamnesiOccupazioneCodiceFiscaleDittaCrpt(
//	                valoreCella("anamnesi_occupazione_codice_fiscale_ditta_crpt", "ANAMNESI",
//	                        oggettoOriginale.getAnamnesiOccupazioneCodiceFiscaleDittaCrpt(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//	        
//	        exportDTOModificato.setEsitiVisCodiceFiscale(
//	                valoreCella("esiti_vis_codice_fiscale", "ESITI",
//	                        oggettoOriginale.getEsitiVisCodiceFiscale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisIdAura(
//	                valoreCella("esiti_vis_id_aura", "ESITI",
//	                        oggettoOriginale.getEsitiVisIdAura(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisDataVisita(
//	                valoreCella("esiti_vis_data_visita", "ESITI",
//	                        oggettoOriginale.getEsitiVisDataVisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisVisita(
//	                valoreCella("esiti_vis_visita", "ESITI",
//	                        oggettoOriginale.getEsitiVisVisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisLivelloVisita(
//	                valoreCella("esiti_vis_livello_visita", "ESITI",
//	                        oggettoOriginale.getEsitiVisLivelloVisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisRiceveIndennizzo(
//	                valoreCella("esiti_vis_riceve_indennizzo", "ESITI",
//	                        oggettoOriginale.getEsitiVisRiceveIndennizzo(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisMalattiaIndennizzo(
//	                valoreCella("esiti_vis_malattia_indennizzo", "ESITI",
//	                        oggettoOriginale.getEsitiVisMalattiaIndennizzo(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiRx(
//	                valoreCella("esiti_vis_accertamenti_rx", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiRx(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiRxData(
//	                valoreCella("esiti_vis_accertamenti_rx_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiRxData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiRxRefertoNormale(
//	                valoreCella("esiti_vis_accertamenti_rx_referto_normale", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiRxRefertoNormale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiRxAcquisita(
//	                valoreCella("esiti_vis_accertamenti_rx_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiRxAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiTc(
//	                valoreCella("esiti_vis_accertamenti_tc", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiTc(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiTcData(
//	                valoreCella("esiti_vis_accertamenti_tc_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiTcData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiTcRefertoNormale(
//	                valoreCella("esiti_vis_accertamenti_tc_referto_normale", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiTcRefertoNormale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiTcAcquisita(
//	                valoreCella("esiti_vis_accertamenti_tc_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiTcAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiSpirometriaSemplice(
//	                valoreCella("esiti_vis_accertamenti_spirometria_semplice", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiSpirometriaSemplice(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiSpirometriaSempliceData(
//	                valoreCella("esiti_vis_accertamenti_spirometria_semplice_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiSpirometriaSempliceData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiSpirometriaSempliceRefertoNormale(
//	                valoreCella("esiti_vis_accertamenti_spirometria_semplice_referto_normale", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiSpirometriaSempliceRefertoNormale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiSpirometriaSempliceAcquisita(
//	                valoreCella("esiti_vis_accertamenti_spirometria_semplice_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiSpirometriaSempliceAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiSpirometriaGlobale(
//	                valoreCella("esiti_vis_accertamenti_spirometria_globale", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiSpirometriaGlobale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiSpirometriaGlobaleData(
//	                valoreCella("esiti_vis_accertamenti_spirometria_globale_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiSpirometriaGlobaleData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiSpirometriaGlobaleRefertoNormale(
//	                valoreCella("esiti_vis_accertamenti_spirometria_globale_referto_normale", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiSpirometriaGlobaleRefertoNormale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiSpirometriaGlobaleAcquisita(
//	                valoreCella("esiti_vis_accertamenti_spirometria_globale_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiSpirometriaGlobaleAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiDlco(
//	                valoreCella("esiti_vis_accertamenti_dlco", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiDlco(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiDlcoData(
//	                valoreCella("esiti_vis_accertamenti_dlco_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiDlcoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiDlcoRefertoNormale(
//	                valoreCella("esiti_vis_accertamenti_dlco_referto_normale", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiDlcoRefertoNormale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiDlcoAcquisita(
//	                valoreCella("esiti_vis_accertamenti_dlco_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiDlcoAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiPet(
//	                valoreCella("esiti_vis_accertamenti_pet", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiPet(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiPetData(
//	                valoreCella("esiti_vis_accertamenti_pet_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiPetData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiPetRefertoNormale(
//	                valoreCella("esiti_vis_accertamenti_pet_referto_normale", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiPetRefertoNormale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiPetAcquisita(
//	                valoreCella("esiti_vis_accertamenti_pet_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiPetAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );xxx
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaPneumologica(
//	                valoreCella("esiti_vis_accertamenti_visita_pneumologica", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaPneumologica(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaPneumologicaData(
//	                valoreCella("esiti_vis_accertamenti_visita_pneumologica_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaPneumologicaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaPneumologicaReferto(
//	                valoreCella("esiti_vis_accertamenti_visita_pneumologica_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaPneumologicaReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaPneumologicaAcquisita(
//	                valoreCella("esiti_vis_accertamenti_visita_pneumologica_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaPneumologicaAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaRadiologica(
//	                valoreCella("esiti_vis_accertamenti_visita_radiologica", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaRadiologica(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaRadiologicaData(
//	                valoreCella("esiti_vis_accertamenti_visita_radiologica_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaRadiologicaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaRadiologicaReferto(
//	                valoreCella("esiti_vis_accertamenti_visita_radiologica_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaRadiologicaReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaRadiologicaAcquisita(
//	                valoreCella("esiti_vis_accertamenti_visita_radiologica_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaRadiologicaAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaOncologica(
//	                valoreCella("esiti_vis_accertamenti_visita_oncologica", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaOncologica(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaOncologicaData(
//	                valoreCella("esiti_vis_accertamenti_visita_oncologica_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaOncologicaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaOncologicaReferto(
//	                valoreCella("esiti_vis_accertamenti_visita_oncologica_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaOncologicaReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiVisitaOncologicaAcquisita(
//	                valoreCella("esiti_vis_accertamenti_visita_oncologica_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiVisitaOncologicaAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiAltro(
//	                valoreCella("esiti_vis_accertamenti_altro", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiAltro(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiAltroDescrizione(
//	                valoreCella("esiti_vis_accertamenti_altro_descrizione", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiAltroDescrizione(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiAltroData(
//	                valoreCella("esiti_vis_accertamenti_altro_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiAltroData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiAltroRefertoNormale(
//	                valoreCella("esiti_vis_accertamenti_altro_referto_normale", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiAltroRefertoNormale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAccertamentiAltroRefertoAcquisita(
//	                valoreCella("esiti_vis_accertamenti_altro_referto_acquisita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAccertamentiAltroRefertoAcquisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );xxx
//
//	        exportDTOModificato.setEsitiVisRisultatoNegativo(
//	                valoreCella("esiti_vis_risultato_negativo", "ESITI",
//	                        oggettoOriginale.getEsitiVisRisultatoNegativo(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//	        
//	        exportDTOModificato.setEsitiVisPpmPlacchePleuricheMonolaterali(
//	                valoreCella("esiti_vis_ppm_placche_pleuriche_monolaterali", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpmPlacchePleuricheMonolaterali(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpmPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_ppm_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpmPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpmPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_ppm_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpmPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpmAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_ppm_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpmAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpmAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_ppm_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpmAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpmPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_ppm_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpmPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpmReferto(
//	                valoreCella("esiti_vis_ppm_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpmReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpmRefertoData(
//	                valoreCella("esiti_vis_ppm_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpmRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpbPlacchePleuricheBilaterali(
//	                valoreCella("esiti_vis_ppb_placche_pleuriche_bilaterali", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpbPlacchePleuricheBilaterali(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpbPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_ppb_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpbPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpbPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_ppb_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpbPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpbAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_ppb_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpbAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpbAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_ppb_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpbAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpbPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_ppb_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpbPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpbReferto(
//	                valoreCella("esiti_vis_ppb_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpbReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisPpbRefertoData(
//	                valoreCella("esiti_vis_ppb_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisPpbRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisApAsbestosiPolmonare(
//	                valoreCella("esiti_vis_ap_asbestosi_polmonare", "ESITI",
//	                        oggettoOriginale.getEsitiVisApAsbestosiPolmonare(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisApPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_ap_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisApPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisApPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_ap_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisApPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisApAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_ap_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisApAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisApAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_ap_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisApAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );xxx
//
//	        exportDTOModificato.setEsitiVisApPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_ap_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisApPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisApReferto(
//	                valoreCella("esiti_vis_ap_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisApReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisApRefertoData(
//	                valoreCella("esiti_vis_ap_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisApRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisFpdFibrosiPleuricaDiffusa(
//	                valoreCella("esiti_vis_fpd_fibrosi_pleurica_diffusa", "ESITI",
//	                        oggettoOriginale.getEsitiVisFpdFibrosiPleuricaDiffusa(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisFpdPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_fpd_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisFpdPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisFpdPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_fpd_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisFpdPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisFpdAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_fpd_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisFpdAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisFpdAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_fpd_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisFpdAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisFpdPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_fpd_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisFpdPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisFpdReferto(
//	                valoreCella("esiti_vis_fpd_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisFpdReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisFpdRefertoData(
//	                valoreCella("esiti_vis_fpd_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisFpdRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//	        
//	        exportDTOModificato.setEsitiVisMpMesoteliomaPleurico(
//	                valoreCella("esiti_vis_mp_mesotelioma_pleurico", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpMesoteliomaPleurico(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisMpPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_mp_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisMpPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_mp_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisMpAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_mp_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisMpAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_mp_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );xxx
//
//	        exportDTOModificato.setEsitiVisMpPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_mp_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisMpReferto(
//	                valoreCella("esiti_vis_mp_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisMpRefertoData(
//	                valoreCella("esiti_vis_mp_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisMpComunicazioneAlCor(
//	                valoreCella("esiti_vis_mp_comunicazione_al_cor", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpComunicazioneAlCor(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisMpComunicazioneAlCorData(
//	                valoreCella("esiti_vis_mp_comunicazione_al_cor_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisMpComunicazioneAlCorData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmAltroMesotelioma(
//	                valoreCella("esiti_vis_am_altro_mesotelioma", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmAltroMesotelioma(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_am_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_am_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_am_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_am_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_am_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmReferto(
//	                valoreCella("esiti_vis_am_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmRefertoData(
//	                valoreCella("esiti_vis_am_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmComunicazioneAlCor(
//	                valoreCella("esiti_vis_am_comunicazione_al_cor", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmComunicazioneAlCor(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAmComunicazioneAlCorData(
//	                valoreCella("esiti_vis_am_comunicazione_al_cor_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAmComunicazioneAlCorData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );xxx
//
//	        exportDTOModificato.setEsitiVisNlNeoplasiaLaringe(
//	                valoreCella("esiti_vis_nl_neoplasia_laringe", "ESITI",
//	                        oggettoOriginale.getEsitiVisNlNeoplasiaLaringe(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNlPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_nl_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisNlPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNlPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_nl_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisNlPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNlAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_nl_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisNlAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNlAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_nl_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisNlAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNlPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_nl_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisNlPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//	        
//	        exportDTOModificato.setEsitiVisNlReferto(
//	                valoreCella("esiti_vis_nl_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisNlReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNlRefertoData(
//	                valoreCella("esiti_vis_nl_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisNlRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNoNeoplasiaOvarica(
//	                valoreCella("esiti_vis_no_neoplasia_ovarica", "ESITI",
//	                        oggettoOriginale.getEsitiVisNoNeoplasiaOvarica(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNoPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_no_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisNoPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNoPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_no_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisNoPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNoAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_no_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisNoAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNoAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_no_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisNoAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNoPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_no_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisNoPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNoReferto(
//	                valoreCella("esiti_vis_no_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisNoReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisNoRefertoData(
//	                valoreCella("esiti_vis_no_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisNoRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisTpTumoreDelPolmone(
//	                valoreCella("esiti_vis_tp_tumore_del_polmone", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpTumoreDelPolmone(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisTpPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_tp_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisTpPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_tp_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisTpAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_tp_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisTpAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_tp_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );xxx
//
//	        exportDTOModificato.setEsitiVisTpPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_tp_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisTpReferto(
//	                valoreCella("esiti_vis_tp_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//	        
//	        exportDTOModificato.setEsitiVisTpRefertoData(
//	                valoreCella("esiti_vis_tp_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisTpComunicazioneAlCor(
//	                valoreCella("esiti_vis_tp_comunicazione_al_cor", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpComunicazioneAlCor(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisTpComunicazioneAlCorData(
//	                valoreCella("esiti_vis_tp_comunicazione_al_cor_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisTpComunicazioneAlCorData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisBpcoEnfisemaPolmonare(
//	                valoreCella("esiti_vis_bpco_enfisema_polmonare", "ESITI",
//	                        oggettoOriginale.getEsitiVisBpcoEnfisemaPolmonare(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisBpcoPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_bpco_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisBpcoPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisBpcoPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_bpco_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisBpcoPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisBpcoAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_bpco_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisBpcoAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisBpcoAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_bpco_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisBpcoAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisBpcoPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_bpco_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisBpcoPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisBpcoReferto(
//	                valoreCella("esiti_vis_bpco_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisBpcoReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisBpcoRefertoData(
//	                valoreCella("esiti_vis_bpco_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisBpcoRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAltraDiagnosi(
//	                valoreCella("esiti_vis_altra_diagnosi", "ESITI",
//	                        oggettoOriginale.getEsitiVisAltraDiagnosi(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAltraDiagnosiDescrizione(
//	                valoreCella("esiti_vis_altra_diagnosi_descrizione", "ESITI",
//	                        oggettoOriginale.getEsitiVisAltraDiagnosiDescrizione(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAltraPrimoCertificatoEDenuncia(
//	                valoreCella("esiti_vis_altra_primo_certificato_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisAltraPrimoCertificatoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAltraPrimoCertificatoEDenunciaData(
//	                valoreCella("esiti_vis_altra_primo_certificato_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAltraPrimoCertificatoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAltraAggravamentoEDenuncia(
//	                valoreCella("esiti_vis_altra_aggravamento_e_denuncia", "ESITI",
//	                        oggettoOriginale.getEsitiVisAltraAggravamentoEDenuncia(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAltraAggravamentoEDenunciaData(
//	                valoreCella("esiti_vis_altra_aggravamento_e_denuncia_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAltraAggravamentoEDenunciaData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAltraPercentualeDiRiconoscimento(
//	                valoreCella("esiti_vis_altra_percentuale_di_riconoscimento", "ESITI",
//	                        oggettoOriginale.getEsitiVisAltraPercentualeDiRiconoscimento(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAltraReferto(
//	                valoreCella("esiti_vis_altra_referto", "ESITI",
//	                        oggettoOriginale.getEsitiVisAltraReferto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAltraRefertoData(
//	                valoreCella("esiti_vis_altra_referto_data", "ESITI",
//	                        oggettoOriginale.getEsitiVisAltraRefertoData(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisFollowUpPrevisto(
//	                valoreCella("esiti_vis_follow_up_previsto", "ESITI",
//	                        oggettoOriginale.getEsitiVisFollowUpPrevisto(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAnnoPresuntoProssimaVisita(
//	                valoreCella("esiti_vis_anno_presunto_prossima_visita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAnnoPresuntoProssimaVisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisAnnoUltimaVisita(
//	                valoreCella("esiti_vis_anno_ultima_visita", "ESITI",
//	                        oggettoOriginale.getEsitiVisAnnoUltimaVisita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisInvioSintesiAMmg(
//	                valoreCella("esiti_vis_invio_sintesi_a_mmg", "ESITI",
//	                        oggettoOriginale.getEsitiVisInvioSintesiAMmg(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setEsitiVisIdSpresal(
//	                valoreCella("esiti_vis_id_spresal", "ESITI",
//	                        oggettoOriginale.getEsitiVisIdSpresal(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//	        
//	     // ── INAIL ──────────────────────────────────────────────────────
//	        exportDTOModificato.setInailDomanda(
//	                valoreCella("inail_domanda", "INAIL",
//	                        oggettoOriginale.getInailDomanda(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailCognome(
//	                valoreCella("inail_cognome", "INAIL",
//	                        oggettoOriginale.getInailCognome(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailNome(
//	                valoreCella("inail_nome", "INAIL",
//	                        oggettoOriginale.getInailNome(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailCodiceFiscale(
//	                valoreCella("inail_codice_fiscale", "INAIL",
//	                        oggettoOriginale.getInailCodiceFiscale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailSesso(
//	                valoreCella("inail_sesso", "INAIL",
//	                        oggettoOriginale.getInailSesso(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailDataNascita(
//	                valoreCella("inail_data_nascita", "INAIL",
//	                        oggettoOriginale.getInailDataNascita(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailIndirizzoResidenza(
//	                valoreCella("inail_indirizzo_residenza", "INAIL",
//	                        oggettoOriginale.getInailIndirizzoResidenza(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailIstatResidenza(
//	                valoreCella("inail_istat_residenza", "INAIL",
//	                        oggettoOriginale.getInailIstatResidenza(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailCapResidenza(
//	                valoreCella("inail_cap_residenza", "INAIL",
//	                        oggettoOriginale.getInailCapResidenza(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailRegioneResidenza(
//	                valoreCella("inail_regione_residenza", "INAIL",
//	                        oggettoOriginale.getInailRegioneResidenza(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailProvinciaResidenza(
//	                valoreCella("inail_provincia_residenza", "INAIL",
//	                        oggettoOriginale.getInailProvinciaResidenza(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setInailComuneResidenza(
//	                valoreCella("inail_comune_residenza", "INAIL",
//	                        oggettoOriginale.getInailComuneResidenza(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//	        
//	     // ── NPLA ──────────────────────────────────────────────────────
//	        exportDTOModificato.setNplaCodiceFiscale(
//	                valoreCella("npla_codice_fiscale", "NPLA",
//	                        oggettoOriginale.getNplaCodiceFiscale(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaPeriodo(
//	                valoreCella("npla_periodo", "NPLA",
//	                        oggettoOriginale.getNplaPeriodo(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaIdCantiere(
//	                valoreCella("npla_id_cantiere", "NPLA",
//	                        oggettoOriginale.getNplaIdCantiere(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaAziendaPiva(
//	                valoreCella("npla_azienda_piva", "NPLA",
//	                        oggettoOriginale.getNplaAziendaPiva(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaAziendaNome(
//	                valoreCella("npla_azienda_nome", "NPLA",
//	                        oggettoOriginale.getNplaAziendaNome(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaAslCantiere(
//	                valoreCella("npla_asl_cantiere", "NPLA",
//	                        oggettoOriginale.getNplaAslCantiere(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaComuneCantiere(
//	                valoreCella("npla_comune_cantiere", "NPLA",
//	                        oggettoOriginale.getNplaComuneCantiere(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaAnno(
//	                valoreCella("npla_anno", "NPLA",
//	                        oggettoOriginale.getNplaAnno(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaTipologiaPiano(
//	                valoreCella("npla_tipologia_piano", "NPLA",
//	                        oggettoOriginale.getNplaTipologiaPiano(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaQuantitaDaRimuovere(
//	                valoreCella("npla_quantita_da_rimuovere", "NPLA",
//	                        oggettoOriginale.getNplaQuantitaDaRimuovere(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//	        exportDTOModificato.setNplaQuantitaRimossa(
//	                valoreCella("npla_quantita_rimossa", "NPLA",
//	                        oggettoOriginale.getNplaQuantitaRimossa(),
//	                        flagDatiAnonimizzati, mapCampiMascherati)
//	        );
//
//
//	    return exportDTOModificato;
//	}
	
	private static final List<ExportField> EXPORT_FIELDS = List.of(

	        // ========== ADESIONE ==========
	        new ExportField("adesionecodiceadesione", "ADESIONE", "Adesione - Codice Adesione"),
	        new ExportField("adesionedataadesione", "ADESIONE", "Adesione - Data Adesione"),
	        new ExportField("adesionecodicefiscale", "ADESIONE", "Adesione - Codice Fiscale"),
	        new ExportField("adesionecognome", "ADESIONE", "Adesione - Cognome"),
	        new ExportField("adesionenome", "ADESIONE", "Adesione - Nome"),
	        new ExportField("adesionedatadinascita", "ADESIONE", "Adesione - Data di nascita"),
	        new ExportField("adesioneprovinciadinascita", "ADESIONE", "Adesione - Provincia di nascita"),
	        new ExportField("adesionecomunedinascita", "ADESIONE", "Adesione - Comune di nascita"),
	        new ExportField("adesionetesserateam", "ADESIONE", "Adesione - Tessera Team"),
	        new ExportField("adesioneidaura", "ADESIONE", "Adesione - Id Aura"),
	        new ExportField("adesioneprovinciadidomicilio", "ADESIONE", "Adesione - Provincia di domicilio"),
	        new ExportField("adesionecomunedidomicilio", "ADESIONE", "Adesione - Comune di domicilio"),
	        new ExportField("adesionecodicecomuneistatdidomicilio", "ADESIONE", "Adesione - Codice comune ISTAT di domicilio"),
	        new ExportField("adesionecapdidomicilio", "ADESIONE", "Adesione - CAP di domicilio"),
	        new ExportField("adesioneemail", "ADESIONE", "Adesione - Email"),
	        new ExportField("adesionetelefono", "ADESIONE", "Adesione - Telefono"),
	        new ExportField("adesionecodiceasldidomicilio", "ADESIONE", "Adesione - Codice ASL di domicilio"),
	        new ExportField("adesioneasldidomicilio", "ADESIONE", "Adesione - ASL di domicilio"),
	        new ExportField("adesionecodazi", "ADESIONE", "Adesione - Codazi"),
	        new ExportField("adesionecodiceasldiresidenza", "ADESIONE", "Adesione - Codice ASL di residenza"),
	        new ExportField("adesioneasldiresidenza", "ADESIONE", "Adesione - ASL di residenza"),
	        new ExportField("adesionedatainizioesposizione", "ADESIONE", "Adesione - Data inizio esposizione"),
	        new ExportField("adesionedatafineesposizione", "ADESIONE", "Adesione - Data fine esposizione"),
	        new ExportField("adesioneazienda", "ADESIONE", "Adesione - Azienda"),
	        new ExportField("adesionecodicecomuneistatazienda", "ADESIONE", "Adesione - Codice comune ISTAT azienda"),
	        new ExportField("adesionecomuneazienda", "ADESIONE", "Adesione - Comune azienda"),
	        new ExportField("adesionecapazienda", "ADESIONE", "Adesione - CAP azienda"),
	        new ExportField("adesioneprovinciaazienda", "ADESIONE", "Adesione - Provincia azienda"),
	        new ExportField("adesionemansione", "ADESIONE", "Adesione - Mansione"),

	        // ========== AURA ==========
	        new ExportField("auracodicefiscale", "AURA", "Aura - Codice Fiscale"),
	        new ExportField("auradomicilioasl", "AURA", "Aura - Domicilio ASL"),
	        new ExportField("auraresidenzaasl", "AURA", "Aura - Residenza ASL"),
	        new ExportField("auraassistenzaasl", "AURA", "Aura - Assistenza ASL"),
	        new ExportField("auranome", "AURA", "Aura - Nome"),
	        new ExportField("auracognome", "AURA", "Aura - Cognome"),
	        new ExportField("aurasesso", "AURA", "Aura - Sesso"),
	        new ExportField("auranascitadata", "AURA", "Aura - Data nascita"),
	        new ExportField("auranascitacomunecod", "AURA", "Aura - Comune nascita cod"),
	        new ExportField("auranascitacomunedesc", "AURA", "Aura - Comune nascita desc"),
	        new ExportField("auranascitaprovinciacod", "AURA", "Aura - Provincia nascita cod"),
	        new ExportField("auranascitaprovinciadesc", "AURA", "Aura - Provincia nascita desc"),
	        new ExportField("auranascitastatocod", "AURA", "Aura - Stato nascita cod"),
	        new ExportField("auranascitastatodesc", "AURA", "Aura - Stato nascita desc"),
	        new ExportField("auracittadinanzastatocod", "AURA", "Aura - Cittadinanza stato cod"),
	        new ExportField("auracittadinanzastatodesc", "AURA", "Aura - Cittadinanza stato desc"),
	        new ExportField("auradomiciliocomunecod", "AURA", "Aura - Domicilio comune cod"),
	        new ExportField("auradomiciliocomunedesc", "AURA", "Aura - Domicilio comune desc"),
	        new ExportField("auradomicilioprovinciacod", "AURA", "Aura - Domicilio provincia cod"),
	        new ExportField("auradomicilioprovinciadesc", "AURA", "Aura - Domicilio provincia desc"),
	        new ExportField("auradomiciliostatocod", "AURA", "Aura - Domicilio stato cod"),
	        new ExportField("auradomiciliostatodesc", "AURA", "Aura - Domicilio stato desc"),
	        new ExportField("auradomiciliocap", "AURA", "Aura - Domicilio CAP"),
	        new ExportField("auradomicilioindirizzo", "AURA", "Aura - Domicilio indirizzo"),
	        new ExportField("auradomicilionumerocivico", "AURA", "Aura - Domicilio numero civico"),
	        new ExportField("auraresidenzacomunecod", "AURA", "Aura - Residenza comune cod"),
	        new ExportField("auraresidenzacomunedesc", "AURA", "Aura - Residenza comune desc"),
	        new ExportField("auraresidenzaprovinciacod", "AURA", "Aura - Residenza provincia cod"),
	        new ExportField("auraresidenzaprovinciadesc", "AURA", "Aura - Residenza provincia desc"),
	        new ExportField("auraresidenzastatocod", "AURA", "Aura - Residenza stato cod"),
	        new ExportField("auraresidenzastatodesc", "AURA", "Aura - Residenza stato desc"),
	        new ExportField("auraresidenzacap", "AURA", "Aura - Residenza CAP"),
	        new ExportField("auraresidenzaindirizzo", "AURA", "Aura - Residenza indirizzo"),
	        new ExportField("auraresidenzanumerocivico", "AURA", "Aura - Residenza numero civico"),
	        new ExportField("auratesserateam", "AURA", "Aura - Tessera team"),
	        new ExportField("auraidaura", "AURA", "Aura - Id Aura"),
	        new ExportField("auraemailaura", "AURA", "Aura - Email Aura"),
	        new ExportField("auratelefonoaura", "AURA", "Aura - Telefono Aura"),
	        new ExportField("auradatadecesso", "AURA", "Aura - Data decesso"),
	        new ExportField("auraassistenzaaslfine", "AURA", "Aura - Assistenza ASL fine"),
	        
	        //SOGGETTO ED ESENZIONI
	        new ExportField("soggettoemail", "SOGGETTO", "Soggetto - Email"),
	        new ExportField("soggettotelefono", "SOGGETTO", "Soggetto - Telefono"),
	        new ExportField("codesenzione",      "ESENZIONI", "Aura - Codice esenzione"),
	        new ExportField("descesenzione",     "ESENZIONI", "Aura - Descrizione esenzione"),
	        new ExportField("coddignosi",        "ESENZIONI", "Aura - Codice diagnosi"),
	        new ExportField("descdignosi",       "ESENZIONI", "Aura - Descrizione diagnosi"),
	        new ExportField("dataemissione",     "ESENZIONI", "Aura - Data emissione esenzione"),
	        new ExportField("datascadenza",      "ESENZIONI", "Aura - Data scadenza esenzione"),

	        // ========== ANAMNESI ==========
	        new ExportField("anamnesicodicefiscale", "ANAMNESI", "Anamnesi - Codice Fiscale"),
	        new ExportField("anamnesiidaura", "ANAMNESI", "Anamnesi - Id Aura"),
	        new ExportField("anamnesidataintervista", "ANAMNESI", "Anamnesi - Data intervista"),
	        new ExportField("anamnesinominativointervistatore", "ANAMNESI", "Anamnesi - Nominativo intervistatore"),
	        new ExportField("anamnesifumatore", "ANAMNESI", "Anamnesi - Fumatore"),
	        new ExportField("anamnesisigarette", "ANAMNESI", "Anamnesi - Sigarette"),
	        new ExportField("anamnesisigaretteanni", "ANAMNESI", "Anamnesi - Sigarette anni"),
	        new ExportField("anamnesisigaretteetainizio", "ANAMNESI", "Anamnesi - Sigarette età inizio"),
	        new ExportField("anamnesisigarettefumaattualmente", "ANAMNESI", "Anamnesi - Sigarette fuma attualmente"),
	        new ExportField("anamnesisigaretteetafine", "ANAMNESI", "Anamnesi - Sigarette età fine"),
	        new ExportField("anamnesisigarettedie", "ANAMNESI", "Anamnesi - Sigarette die"),
	        new ExportField("anamnesisigari", "ANAMNESI", "Anamnesi - Sigari"),
	        new ExportField("anamnesisigarianni", "ANAMNESI", "Anamnesi - Sigari anni"),
	        new ExportField("anamnesisigarietainizio", "ANAMNESI", "Anamnesi - Sigari età inizio"),
	        new ExportField("anamnesisigarifumaattualmente", "ANAMNESI", "Anamnesi - Sigari fuma attualmente"),
	        new ExportField("anamnesisigarietafine", "ANAMNESI", "Anamnesi - Sigari età fine"),
	        new ExportField("anamnesisigaridie", "ANAMNESI", "Anamnesi - Sigari die"),
	        new ExportField("anamnesipipa", "ANAMNESI", "Anamnesi - Pipa"),
	        new ExportField("anamnesipipaanni", "ANAMNESI", "Anamnesi - Pipa anni"),
	        new ExportField("anamnesipipaetainizio", "ANAMNESI", "Anamnesi - Pipa età inizio"),
	        new ExportField("anamnesipipafumaattualmente", "ANAMNESI", "Anamnesi - Pipa fuma attualmente"),
	        new ExportField("anamnesipipaetafine", "ANAMNESI", "Anamnesi - Pipa età fine"),
	        new ExportField("anamnesipipadie", "ANAMNESI", "Anamnesi - Pipa die"),
	        new ExportField("anamnesioccupazionenum", "ANAMNESI", "Anamnesi - Occupazione num"),
	        new ExportField("anamnesioccupazioneannoinizio", "ANAMNESI", "Anamnesi - Occupazione anno inizio"),
	        new ExportField("anamnesioccupazioneannofine", "ANAMNESI", "Anamnesi - Occupazione anno fine"),
	        new ExportField("anamnesioccupazionetipo", "ANAMNESI", "Anamnesi - Occupazione tipo"),
	        new ExportField("anamnesioccupazionedescrizionelavoro", "ANAMNESI", "Anamnesi - Occupazione descrizione lavoro"),
	        new ExportField("anamnesioccupazionenomeeindirizzoditta", "ANAMNESI", "Anamnesi - Occupazione nome e indirizzo ditta"),
	        new ExportField("anamnesioccupazioneattivitaditta", "ANAMNESI", "Anamnesi - Occupazione attività ditta"),
	        new ExportField("anamnesinotaattivitaconamianto", "ANAMNESI", "Anamnesi - Nota attività con amianto"),
	        new ExportField("anamnesianamnesiesposizioneamianto", "ANAMNESI", "Anamnesi - Anamnesi esposizione amianto"),
	        new ExportField("anamnesiesposizioneprofessionale", "ANAMNESI", "Anamnesi - Esposizione professionale"),
	        new ExportField("anamnesiannofineesposizione", "ANAMNESI", "Anamnesi - Anno fine esposizione"),
	        new ExportField("anamnesilivelloesposizione", "ANAMNESI", "Anamnesi - Livello esposizione"),
	        new ExportField("anamnesiinserimentoinsorveglianza", "ANAMNESI", "Anamnesi - Inserimento in sorveglianza"),
	        new ExportField("anamnesiidspresal", "ANAMNESI", "Anamnesi - Id SPRESAL"),
	        new ExportField("anamnesicounseling", "ANAMNESI", "Anamnesi - Counseling"),
	        new ExportField("anamnesioccupazioneesposizionecrpt", "ANAMNESI", "Anamnesi - Occupazione Esposizione CRPT"),
	        new ExportField("anamnesioccupazionesettoredittacrpt", "ANAMNESI", "Anamnesi - Occupazione Settore Ditta CRPT"),
	        new ExportField("anamnesioccupazionemansionecrpt", "ANAMNESI", "Anamnesi - Occupazione Mansione CRPT"),
	        new ExportField("anamnesioccupazioneragionesocialedittacrpt", "ANAMNESI", "Anamnesi - Occupazione Ragione Sociale Ditta CRPT"),
	        new ExportField("anamnesioccupazionepivadittacrpt", "ANAMNESI", "Anamnesi - Occupazione Piva Ditta CRPT"),
	        new ExportField("anamnesioccupazionecodicefiscaledittacrpt", "ANAMNESI", "Anamnesi - Occupazione Codice Fiscale Ditta CRPT"),

	        // ========== ESITI VISITA & ACCERTAMENTI ==========
	        new ExportField("esitiviscodicefiscale", "ESITI", "Esiti - Codice Fiscale"),
	        new ExportField("esitivisidaura", "ESITI", "Esiti - Id Aura"),
	        new ExportField("esitivisdatavisita", "ESITI", "Esiti - Data visita"),
	        new ExportField("esitivisvisita", "ESITI", "Esiti - Visita"),
	        new ExportField("esitivislivellovisita", "ESITI", "Esiti - Livello visita"),
	        new ExportField("esitivisriceveindennizzo", "ESITI", "Esiti - Riceve indennizzo"),
	        new ExportField("esitivismalattiaindennizzo", "ESITI", "Esiti - Malattia indennizzo"),

	        // RX
	        new ExportField("esitivisaccertamentirx", "ESITI", "Esiti - Accertamenti RX"),
	        new ExportField("esitivisaccertamentirxdata", "ESITI", "Esiti - Accertamenti RX data"),
	        new ExportField("esitivisaccertamentirxrefertonormale", "ESITI", "Esiti - Accertamenti RX referto normale"),
	        new ExportField("esitivisaccertamentirxacquisita", "ESITI", "Esiti - Accertamenti RX acquisita"),

	        // TC
	        new ExportField("esitivisaccertamentitc", "ESITI", "Esiti - Accertamenti TC"),
	        new ExportField("esitivisaccertamentitcdata", "ESITI", "Esiti - Accertamenti TC data"),
	        new ExportField("esitivisaccertamentitcrefertonormale", "ESITI", "Esiti - Accertamenti TC referto normale"),
	        new ExportField("esitivisaccertamentitcacquisita", "ESITI", "Esiti - Accertamenti TC acquisita"),

	        // Spirometria semplice
	        new ExportField("esitivisaccertamentispirometriasemplice", "ESITI", "Esiti - Accertamenti spirometria semplice"),
	        new ExportField("esitivisaccertamentispirometriasemplicedata", "ESITI", "Esiti - Accertamenti spirometria semplice data"),
	        new ExportField("esitivisaccertamentispirometriasemplicerefertonormale", "ESITI", "Esiti - Accertamenti spirometria semplice referto normale"),
	        new ExportField("esitivisaccertamentispirometriasempliceacquisita", "ESITI", "Esiti - Accertamenti spirometria semplice acquisita"),

	        // Spirometria globale
	        new ExportField("esitivisaccertamentispirometriaglobale", "ESITI", "Esiti - Accertamenti spirometria globale"),
	        new ExportField("esitivisaccertamentispirometriaglobaledata", "ESITI", "Esiti - Accertamenti spirometria globale data"),
	        new ExportField("esitivisaccertamentispirometriaglobalerefertonormale", "ESITI", "Esiti - Accertamenti spirometria globale referto normale"),
	        new ExportField("esitivisaccertamentispirometriaglobaleacquisita", "ESITI", "Esiti - Accertamenti spirometria globale acquisita"),

	        // DLCO
	        new ExportField("esitivisaccertamentidlco", "ESITI", "Esiti - Accertamenti DLCO"),
	        new ExportField("esitivisaccertamentidlcodata", "ESITI", "Esiti - Accertamenti DLCO data"),
	        new ExportField("esitivisaccertamentidlcorefertonormale", "ESITI", "Esiti - Accertamenti DLCO referto normale"),
	        new ExportField("esitivisaccertamentidlcoacquisita", "ESITI", "Esiti - Accertamenti DLCO acquisita"),

	        // PET
	        new ExportField("esitivisaccertamentipet", "ESITI", "Esiti - Accertamenti PET"),
	        new ExportField("esitivisaccertamentipetdata", "ESITI", "Esiti - Accertamenti PET data"),
	        new ExportField("esitivisaccertamentipetrefertonormale", "ESITI", "Esiti - Accertamenti PET referto normale"),
	        new ExportField("esitivisaccertamentipetacquisita", "ESITI", "Esiti - Accertamenti PET acquisita"),

	        // Visita pneumologica
	        new ExportField("esitivisaccertamentivisitapneumologica", "ESITI", "Esiti - Accertamenti visita pneumologica"),
	        new ExportField("esitivisaccertamentivisitapneumologicadata", "ESITI", "Esiti - Accertamenti visita pneumologica data"),
	        new ExportField("esitivisaccertamentivisitapneumologicareferto", "ESITI", "Esiti - Accertamenti visita pneumologica referto"),
	        new ExportField("esitivisaccertamentivisitapneumologicaacquisita", "ESITI", "Esiti - Accertamenti visita pneumologica acquisita"),

	        // Visita radiologica
	        new ExportField("esitivisaccertamentivisitaradiologica", "ESITI", "Esiti - Accertamenti visita radiologica"),
	        new ExportField("esitivisaccertamentivisitaradiologicadata", "ESITI", "Esiti - Accertamenti visita radiologica data"),
	        new ExportField("esitivisaccertamentivisitaradiologicareferto", "ESITI", "Esiti - Accertamenti visita radiologica referto"),
	        new ExportField("esitivisaccertamentivisitaradiologicaacquisita", "ESITI", "Esiti - Accertamenti visita radiologica acquisita"),

	        // Visita oncologica
	        new ExportField("esitivisaccertamentivisitaoncologica", "ESITI", "Esiti - Accertamenti visita oncologica"),
	        new ExportField("esitivisaccertamentivisitaoncologicadata", "ESITI", "Esiti - Accertamenti visita oncologica data"),
	        new ExportField("esitivisaccertamentivisitaoncologicareferto", "ESITI", "Esiti - Accertamenti visita oncologica referto"),
	        new ExportField("esitivisaccertamentivisitaoncologicaacquisita", "ESITI", "Esiti - Accertamenti visita oncologica acquisita"),

	        // Altro accertamento
	        new ExportField("esitivisaccertamentialtro", "ESITI", "Esiti - Accertamenti altro"),
	        new ExportField("esitivisaccertamentialtrodescrizione", "ESITI", "Esiti - Accertamenti altro descrizione"),
	        new ExportField("esitivisaccertamentialtrodata", "ESITI", "Esiti - Accertamenti altro data"),
	        new ExportField("esitivisaccertamentialtrorefertonormale", "ESITI", "Esiti - Accertamenti altro referto normale"),
	        new ExportField("esitivisaccertamentialtrorefertoacquisita", "ESITI", "Esiti - Accertamenti altro referto acquisita"),

	        // Risultato negativo
	        new ExportField("esitivisrisultatonegativo", "ESITI", "Esiti - Risultato negativo"),

	        // Da qui in poi, tutte le diagnosi PPM, PPB, AP, FPD, MP, AM, NL, NO, TP, BPCO, Altra diagnosi,
	        // follow-up ecc. seguono lo stesso schema che hai già:
	        new ExportField("esitivisppmplacchepleurichemonolaterali", "ESITI", "Esiti - PPM placche pleuriche monolaterali"),
	        new ExportField("esitivisppmprimocertificatoedenuncia", "ESITI", "Esiti - PPM primo certificato e denuncia"),
	        new ExportField("esitivisppmprimocertificatoedenunciadata", "ESITI", "Esiti - PPM primo certificato e denuncia data"),
	        new ExportField("esitivisppmaggravamentoedenuncia", "ESITI", "Esiti - PPM aggravamento e denuncia"),
	        new ExportField("esitivisppmaggravamentoedenunciadata", "ESITI", "Esiti - PPM aggravamento e denuncia data"),
	        new ExportField("esitivisppmpercentualediriconoscimento", "ESITI", "Esiti - PPM percentuale di riconoscimento"),
	        new ExportField("esitivisppmreferto", "ESITI", "Esiti - PPM referto"),
	        new ExportField("esitivisppmrefertodata", "ESITI", "Esiti - PPM referto data"),

	        new ExportField("esitivisppbplacchepleurichebilaterali", "ESITI", "Esiti - PPB placche pleuriche bilaterali"),
	        new ExportField("esitivisppbprimocertificatoedenuncia", "ESITI", "Esiti - PPB primo certificato e denuncia"),
	        new ExportField("esitivisppbprimocertificatoedenunciadata", "ESITI", "Esiti - PPB primo certificato e denuncia data"),
	        new ExportField("esitivisppbaggravamentoedenuncia", "ESITI", "Esiti - PPB aggravamento e denuncia"),
	        new ExportField("esitivisppbaggravamentoedenunciadata", "ESITI", "Esiti - PPB aggravamento e denuncia data"),
	        new ExportField("esitivisppbpercentualediriconoscimento", "ESITI", "Esiti - PPB percentuale di riconoscimento"),
	        new ExportField("esitivisppbreferto", "ESITI", "Esiti - PPB referto"),
	        new ExportField("esitivisppbrefertodata", "ESITI", "Esiti - PPB referto data"),

	        new ExportField("esitivisapasbestosipolmonare", "ESITI", "Esiti - AP asbestosi polmonare"),
	        new ExportField("esitivisapprimocertificatoedenuncia", "ESITI", "Esiti - AP primo certificato e denuncia"),
	        new ExportField("esitivisapprimocertificatoedenunciadata", "ESITI", "Esiti - AP primo certificato e denuncia data"),
	        new ExportField("esitivisapaggravamentoedenuncia", "ESITI", "Esiti - AP aggravamento e denuncia"),
	        new ExportField("esitivisapaggravamentoedenunciadata", "ESITI", "Esiti - AP aggravamento e denuncia data"),
	        new ExportField("esitivisappercentualediriconoscimento", "ESITI", "Esiti - AP percentuale di riconoscimento"),
	        new ExportField("esitivisapreferto", "ESITI", "Esiti - AP referto"),
	        new ExportField("esitivisaprefertodata", "ESITI", "Esiti - AP referto data"),

	        new ExportField("esitivisfpdfibrosipleuricadiffusa", "ESITI", "Esiti - FPD fibrosi pleurica diffusa"),
	        new ExportField("esitivisfpdprimocertificatoedenuncia", "ESITI", "Esiti - FPD primo certificato e denuncia"),
	        new ExportField("esitivisfpdprimocertificatoedenunciadata", "ESITI", "Esiti - FPD primo certificato e denuncia data"),
	        new ExportField("esitivisfpdaggravamentoedenuncia", "ESITI", "Esiti - FPD aggravamento e denuncia"),
	        new ExportField("esitivisfpdaggravamentoedenunciadata", "ESITI", "Esiti - FPD aggravamento e denuncia data"),
	        new ExportField("esitivisfpdpercentualediriconoscimento", "ESITI", "Esiti - FPD percentuale di riconoscimento"),
	        new ExportField("esitivisfpdreferto", "ESITI", "Esiti - FPD referto"),
	        new ExportField("esitivisfpdrefertodata", "ESITI", "Esiti - FPD referto data"),

	        new ExportField("esitivismpmesoteliomapleurico", "ESITI", "Esiti - MP mesotelioma pleurico"),
	        new ExportField("esitivismpprimocertificatoedenuncia", "ESITI", "Esiti - MP primo certificato e denuncia"),
	        new ExportField("esitivismpprimocertificatoedenunciadata", "ESITI", "Esiti - MP primo certificato e denuncia data"),
	        new ExportField("esitivismpaggravamentoedenuncia", "ESITI", "Esiti - MP aggravamento e denuncia"),
	        new ExportField("esitivismpaggravamentoedenunciadata", "ESITI", "Esiti - MP aggravamento e denuncia data"),
	        new ExportField("esitivismppercentualediriconoscimento", "ESITI", "Esiti - MP percentuale di riconoscimento"),
	        new ExportField("esitivismpreferto", "ESITI", "Esiti - MP referto"),
	        new ExportField("esitivismprefertodata", "ESITI", "Esiti - MP referto data"),
	        new ExportField("esitivismpcomunicazionealcor", "ESITI", "Esiti - MP comunicazione al COR"),
	        new ExportField("esitivismpcomunicazionealcordata", "ESITI", "Esiti - MP comunicazione al COR data"),

	        new ExportField("esitivisamaltromesotelioma", "ESITI", "Esiti - AM altro mesotelioma"),
	        new ExportField("esitivisamprimocertificatoedenuncia", "ESITI", "Esiti - AM primo certificato e denuncia"),
	        new ExportField("esitivisamprimocertificatoedenunciadata", "ESITI", "Esiti - AM primo certificato e denuncia data"),
	        new ExportField("esitivisamaggravamentoedenuncia", "ESITI", "Esiti - AM aggravamento e denuncia"),
	        new ExportField("esitivisamaggravamentoedenunciadata", "ESITI", "Esiti - AM aggravamento e denuncia data"),
	        new ExportField("esitivisampercentualediriconoscimento", "ESITI", "Esiti - AM percentuale di riconoscimento"),
	        new ExportField("esitivisamreferto", "ESITI", "Esiti - AM referto"),
	        new ExportField("esitivisamrefertodata", "ESITI", "Esiti - AM referto data"),
	        new ExportField("esitivisamcomunicazionealcor", "ESITI", "Esiti - AM comunicazione al COR"),
	        new ExportField("esitivisamcomunicazionealcordata", "ESITI", "Esiti - AM comunicazione al COR data"),

	        new ExportField("esitivisnlneoplasialaringe", "ESITI", "Esiti - NL neoplasia laringe"),
	        new ExportField("esitivisnlprimocertificatoedenuncia", "ESITI", "Esiti - NL primo certificato e denuncia"),
	        new ExportField("esitivisnlprimocertificatoedenunciadata", "ESITI", "Esiti - NL primo certificato e denuncia data"),
	        new ExportField("esitivisnlaggravamentoedenuncia", "ESITI", "Esiti - NL aggravamento e denuncia"),
	        new ExportField("esitivisnlaggravamentoedenunciadata", "ESITI", "Esiti - NL aggravamento e denuncia data"),
	        new ExportField("esitivisnlpercentualediriconoscimento", "ESITI", "Esiti - NL percentuale di riconoscimento"),
	        new ExportField("esitivisnlreferto", "ESITI", "Esiti - NL referto"),
	        new ExportField("esitivisnlrefertodata", "ESITI", "Esiti - NL referto data"),

	        new ExportField("esitivisnoneoplasiaovarica", "ESITI", "Esiti - NO neoplasia ovarica"),
	        new ExportField("esitivisnoprimocertificatoedenuncia", "ESITI", "Esiti - NO primo certificato e denuncia"),
	        new ExportField("esitivisnoprimocertificatoedenunciadata", "ESITI", "Esiti - NO primo certificato e denuncia data"),
	        new ExportField("esitivisnoaggravamentoedenuncia", "ESITI", "Esiti - NO aggravamento e denuncia"),
	        new ExportField("esitivisnoaggravamentoedenunciadata", "ESITI", "Esiti - NO aggravamento e denuncia data"),
	        new ExportField("esitivisnopercentualediriconoscimento", "ESITI", "Esiti - NO percentuale di riconoscimento"),
	        new ExportField("esitivisnoreferto", "ESITI", "Esiti - NO referto"),
	        new ExportField("esitivisnorefertodata", "ESITI", "Esiti - NO referto data"),

	        new ExportField("esitivistptumoredelpolmone", "ESITI", "Esiti - TP tumore del polmone"),
	        new ExportField("esitivistpprimocertificatoedenuncia", "ESITI", "Esiti - TP primo certificato e denuncia"),
	        new ExportField("esitivistpprimocertificatoedenunciadata", "ESITI", "Esiti - TP primo certificato e denuncia data"),
	        new ExportField("esitivistpaggravamentoedenuncia", "ESITI", "Esiti - TP aggravamento e denuncia"),
	        new ExportField("esitivistpaggravamentoedenunciadata", "ESITI", "Esiti - TP aggravamento e denuncia data"),
	        new ExportField("esitivistppercentualediriconoscimento", "ESITI", "Esiti - TP percentuale di riconoscimento"),
	        new ExportField("esitivistpreferto", "ESITI", "Esiti - TP referto"),
	        new ExportField("esitivistprefertodata", "ESITI", "Esiti - TP referto data"),
	        new ExportField("esitivistpcomunicazionealcor", "ESITI", "Esiti - TP comunicazione al COR"),
	        new ExportField("esitivistpcomunicazionealcordata", "ESITI", "Esiti - TP comunicazione al COR data"),

	        new ExportField("esitivisbpcoenfisemapolmonare", "ESITI", "Esiti - BPCO enfisema polmonare"),
	        new ExportField("esitivisbpcoprimocertificatoedenuncia", "ESITI", "Esiti - BPCO primo certificato e denuncia"),
	        new ExportField("esitivisbpcoprimocertificatoedenunciadata", "ESITI", "Esiti - BPCO primo certificato e denuncia data"),
	        new ExportField("esitivisbpcoaggravamentoedenuncia", "ESITI", "Esiti - BPCO aggravamento e denuncia"),
	        new ExportField("esitivisbpcoaggravamentoedenunciadata", "ESITI", "Esiti - BPCO aggravamento e denuncia data"),
	        new ExportField("esitivisbpcopercentualediriconoscimento", "ESITI", "Esiti - BPCO percentuale di riconoscimento"),
	        new ExportField("esitivisbpcoreferto", "ESITI", "Esiti - BPCO referto"),
	        new ExportField("esitivisbpcorefertodata", "ESITI", "Esiti - BPCO referto data"),

	        new ExportField("esitivisaltradiagnosi", "ESITI", "Esiti - Altra diagnosi"),
	        new ExportField("esitivisaltradiagnosidescrizione", "ESITI", "Esiti - Altra diagnosi descrizione"),
	        new ExportField("esitivisaltraprimocertificatoedenuncia", "ESITI", "Esiti - Altra primo certificato e denuncia"),
	        new ExportField("esitivisaltraprimocertificatoedenunciadata", "ESITI", "Esiti - Altra primo certificato e denuncia data"),
	        new ExportField("esitivisaltraaggravamentoedenuncia", "ESITI", "Esiti - Altra aggravamento e denuncia"),
	        new ExportField("esitivisaltraaggravamentoedenunciadata", "ESITI", "Esiti - Altra aggravamento e denuncia data"),
	        new ExportField("esitivisaltrapercentualediriconoscimento", "ESITI", "Esiti - Altra percentuale di riconoscimento"),
	        new ExportField("esitivisaltrareferto", "ESITI", "Esiti - Altra referto"),
	        new ExportField("esitivisaltrarefertodata", "ESITI", "Esiti - Altra referto data"),

	        new ExportField("esitivisfollowupprevisto", "ESITI", "Esiti - Follow up previsto"),
	        new ExportField("esitivisannopresuntoprossimavisita", "ESITI", "Esiti - Anno presunto prossima visita"),
	        new ExportField("esitivisannoultimavisita", "ESITI", "Esiti - Anno ultima visita"),
	        new ExportField("esitivisinviosintesiammg", "ESITI", "Esiti - Invio sintesi a MMG"),
	        new ExportField("esitivisidspresal", "ESITI", "Esiti - Id SPRESAL"),
	        
	        
	        new ExportField("attsanitariastato", "DATODERIVATO", "Att. Sanitaria Stato"),
	        new ExportField("attsanitariaspresal", "DATODERIVATO", "Att. Sanitaria Spresal"),
	        new ExportField("attsanitariaspresaldata", "DATODERIVATO", "Att. Sanitaria Spresal Data"),
	        new ExportField("attsanitariainail", "DATODERIVATO", "Att. Sanitaria Inail"),
	        new ExportField("sezione", "DATODERIVATO", "Sezione"),
	        new ExportField("inseritosorveglianza", "DATODERIVATO", "Inserito Sorveglianza"),
	        new ExportField("tipoelencoinail", "DATODERIVATO", "Tipo Elenco Inail"),
	        
	        new ExportField("inaildomanda", "INAIL", "Inail - Domanda"),
	        new ExportField("inailcognome", "INAIL", "Inail - Cognome"),
	        new ExportField("inailnome", "INAIL", "Inail - Nome"),
	        new ExportField("inailcodicefiscale", "INAIL", "Inail - Codice Fiscale"),
	        new ExportField("inailsesso", "INAIL", "Inail - Sesso"),
	        new ExportField("inaildatanascita", "INAIL", "Inail - Data Nascita"),
	        new ExportField("inailindirizzoresidenza", "INAIL", "Inail - Indirizzo Residenza"),
	        new ExportField("inailistatresidenza", "INAIL", "Inail - Istat Residenza"),
	        new ExportField("inailcapresidenza", "INAIL", "Inail - CAP Residenza"),
	        new ExportField("inailregioneresidenza", "INAIL", "Inail - Regione Residenza"),
	        new ExportField("inailprovinciaresidenza", "INAIL", "Inail - Provincia Residenza"),
	        new ExportField("inailcomuneresidenza", "INAIL", "Inail - Comune Residenza"),
            
	        
	        new ExportField("nplacodicefiscale", "NPLA", "Npla - Codice Fiscale"),
	        new ExportField("nplaperiodo", "NPLA", "Npla - Periodo"),
	        new ExportField("nplaidcantiere", "NPLA", "Npla - Id Cantiere"),
	        new ExportField("nplaaziendapiva", "NPLA", "Npla - Azienda Piva"),
	        new ExportField("nplaaziendanome", "NPLA", "Npla - Azienda Nome"),
	        new ExportField("nplaaslcantiere", "NPLA", "Npla - Asl Cantiere"),
	        new ExportField("nplacomunecantiere", "NPLA", "Npla - Comune Cantiere"),
	        new ExportField("nplaanno", "NPLA", "Npla - Anno"),
	        new ExportField("nplatipologiapiano", "NPLA", "Npla - Tipologia Piano"),
	        new ExportField("nplaquantitadarimuovere", "NPLA", "Npla - Quantita da Rimuovere"),
	        new ExportField("nplaquantitarimossa", "NPLA", "Npla - Quantita Rimossa"),
	        new ExportField("soggettostatodesc", "STATO_SOGGETTO", "Stato Soggetto"),
	        new ExportField("soggettoid", "SOGGETTO_ID", "Soggetto Id")
	);
	
	private String normalizzaNomeCampo(String s) {
	    if (s == null) return "";
	    return s.toLowerCase()
	            .replace("_", "")
	            .replace("-", "")
	            .trim();
	}
	
	private boolean isCampoMascherato2(String nomeCampo,
			String chiaveMappa,
			Boolean flagDatiAnonimizzati,
			Map<String, List<String>> mapCampiMascherati) {
		// se NON è richiesta l'anonimizzazione, nessun campo è mascherato
		if (!Boolean.TRUE.equals(flagDatiAnonimizzati)) {
			return false;
		}

		List<String> campiChiave = mapCampiMascherati.getOrDefault(chiaveMappa, List.of());
		if (campiChiave.isEmpty()) {
			return false;
		}

		String normCampo = normalizzaNomeCampo(nomeCampo);

		return campiChiave.stream()
				.map(this::normalizzaNomeCampo) // es: "adesione_codice_adesione" -> "adesionecodiceadesione"
				.anyMatch(normCampo::equals);
	}
	
	private String[] costruisciHeaders(Boolean flagDatiAnonimizzati,
			Map<String, List<String>> mapCampiMascherati) {
		return EXPORT_FIELDS.stream()
				.filter(f -> !isCampoMascherato2(
						f.getNomeCampo(),
						f.getChiaveMappa(),
						flagDatiAnonimizzati,
						mapCampiMascherati))
				.map(ExportField::getHeader)
				.toArray(String[]::new);
	}

    private ExcelExportResult generaExcelTotaleAsincrono2(List<ExportDTO> righe,
                                                          Boolean flagDatiAnonimizzati,
                                                          Integer assistenzaAslId, String aslCod, String utenteLogin,
                                                          Integer fileId, Integer elaborazioneId, String uniqueName) {

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			Map<String, List<String>> mapCampiMascherati = anagraficaRepository.buildCampiMascheratiMap();

			Sheet sheet = workbook.createSheet("Export Totale Asincrono");

			String[] headers = costruisciHeaders(flagDatiAnonimizzati, mapCampiMascherati);
			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < headers.length; i++) {
			    headerRow.createCell(i).setCellValue(headers[i]);
			}
			int rowNum = 1;

			for (ExportDTO dto : righe) {
				Row row = sheet.createRow(rowNum++);
				int[] col = {0};

				// ===================== ADESIONE =====================
				scriviCella(row, col, "adesione_codice_adesione", "ADESIONE", nvl(dto.getAdesioneCodiceAdesione()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_data_adesione", "ADESIONE", nvl(dto.getAdesioneDataAdesione()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_codice_fiscale", "ADESIONE", nvl(dto.getAdesioneCodiceFiscale()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_cognome", "ADESIONE", nvl(dto.getAdesioneCognome()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_nome", "ADESIONE", nvl(dto.getAdesioneNome()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_data_di_nascita", "ADESIONE", nvl(dto.getAdesioneDataDiNascita()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_provincia_di_nascita", "ADESIONE", nvl(dto.getAdesioneProvinciaDiNascita()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_comune_di_nascita", "ADESIONE", nvl(dto.getAdesioneComuneDiNascita()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_tessera_team", "ADESIONE", nvl(dto.getAdesioneTesseraTeam()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_id_aura", "ADESIONE", nvl(dto.getAdesioneIdAura()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_provincia_di_domicilio", "ADESIONE", nvl(dto.getAdesioneProvinciaDiDomicilio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_comune_di_domicilio", "ADESIONE", nvl(dto.getAdesioneComuneDiDomicilio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_codice_comune_istat_di_domicilio", "ADESIONE", nvl(dto.getAdesioneCodiceComuneIstatDiDomicilio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_cap_di_domicilio", "ADESIONE", nvl(dto.getAdesioneCapDiDomicilio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_email", "ADESIONE", nvl(dto.getAdesioneEmail()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_telefono", "ADESIONE", nvl(dto.getAdesioneTelefono()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_codice_asl_di_domicilio", "ADESIONE", nvl(dto.getAdesioneCodiceAslDiDomicilio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_asl_di_domicilio", "ADESIONE", nvl(dto.getAdesioneAslDiDomicilio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_codazi", "ADESIONE", nvl(dto.getAdesioneCodazi()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_codice_asl_di_residenza", "ADESIONE", nvl(dto.getAdesioneCodiceAslDiResidenza()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_asl_di_residenza", "ADESIONE", nvl(dto.getAdesioneAslDiResidenza()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_data_inizio_esposizione", "ADESIONE", nvl(dto.getAdesioneDataInizioEsposizione()) != null && !(nvl(dto.getAdesioneDataInizioEsposizione()).trim().isEmpty()) ? nvl(dto.getAdesioneDataInizioEsposizione()) : nvl(dto.getAdesioneEsposizioneDataInizioEsposizione()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_data_fine_esposizione", "ADESIONE", nvl(dto.getAdesioneDataFineEsposizione()) != null && !(nvl(dto.getAdesioneDataFineEsposizione()).trim().isEmpty()) ? nvl(dto.getAdesioneDataFineEsposizione()) : nvl(dto.getAdesioneEsposizioneDataFineEsposizione()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_azienda", "ADESIONE", StringUtils.isNotBlank(dto.getAdesioneAzienda()) ? dto.getAdesioneAzienda() : nvl(dto.getAdesioneEsposizioneAzienda()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_codice_comune_istat_azienda", "ADESIONE", StringUtils.isNotBlank(dto.getAdesioneCodiceComuneIstatAzienda()) ? nvl(dto.getAdesioneCodiceComuneIstatAzienda()) : nvl(dto.getAdesioneEsposizioneAziendaComuneCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_comune_azienda", "ADESIONE", StringUtils.isNotBlank(dto.getAdesioneComuneAzienda()) ? nvl(dto.getAdesioneComuneAzienda()) : nvl(dto.getAdesioneEsposizioneAziendaComuneDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_cap_azienda", "ADESIONE", StringUtils.isNotBlank(dto.getAdesioneCapAzienda()) ? nvl(dto.getAdesioneCapAzienda()) : nvl(dto.getAdesioneEsposizioneAziendaCap()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_provincia_azienda", "ADESIONE", StringUtils.isNotBlank(dto.getAdesioneProvinciaAzienda()) ? nvl(dto.getAdesioneProvinciaAzienda()) : nvl(dto.getAdesioneEsposizioneProvinciaAzienda()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "adesione_mansione", "ADESIONE", StringUtils.isNotBlank(dto.getAdesioneMansione()) ? nvl(dto.getAdesioneMansione()) : nvl(dto.getAdesioneEsposizioneMansione()), flagDatiAnonimizzati, mapCampiMascherati);

				// ===================== AURA =====================
				scriviCella(row, col, "aura_codice_fiscale", "AURA", nvl(dto.getAuraCodiceFiscale()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_asl", "AURA", nvl(dto.getAslAziendaDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_asl", "AURA", nvl(dto.getAslAziendaDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_assistenza_asl", "AURA", nvl(dto.getAslAziendaDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_nome", "AURA", nvl(dto.getAuraNome()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_cognome", "AURA", nvl(dto.getAuraCognome()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_sesso", "AURA", nvl(dto.getAuraSesso()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_nascita_data", "AURA", nvl(dto.getAuraNascitaData()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_nascita_comune_cod", "AURA", nvl(dto.getAuraNascitaComuneCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_nascita_comune_desc", "AURA", nvl(dto.getAuraNascitaComuneDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_nascita_provincia_cod", "AURA", nvl(dto.getAuraNascitaProvinciaCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_nascita_provincia_desc", "AURA", nvl(dto.getAuraNascitaProvinciaDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_nascita_stato_cod", "AURA", nvl(dto.getAuraNascitaStatoCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_nascita_stato_desc", "AURA", nvl(dto.getAuraNascitaStatoDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_cittadinanza_stato_cod", "AURA", nvl(dto.getAuraCittadinanzaStatoCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_cittadinanza_stato_desc", "AURA", nvl(dto.getAuraCittadinanzaStatoDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_comune_cod", "AURA", nvl(dto.getAuraDomicilioComuneCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_comune_desc", "AURA", nvl(dto.getAuraDomicilioComuneDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_provincia_cod", "AURA", nvl(dto.getAuraDomicilioProvinciaCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_provincia_desc", "AURA", nvl(dto.getAuraDomicilioProvinciaDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_stato_cod", "AURA", nvl(dto.getAuraDomicilioStatoCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_stato_desc", "AURA", nvl(dto.getAuraDomicilioStatoDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_cap", "AURA", nvl(dto.getAuraDomicilioCap()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_indirizzo", "AURA", nvl(dto.getAuraDomicilioIndirizzo()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_domicilio_numero_civico", "AURA", nvl(dto.getAuraDomicilioNumeroCivico()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_comune_cod", "AURA", nvl(dto.getAuraResidenzaComuneCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_comune_desc", "AURA", nvl(dto.getAuraResidenzaComuneDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_provincia_cod", "AURA", nvl(dto.getAuraResidenzaProvinciaCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_provincia_desc", "AURA", nvl(dto.getAuraResidenzaProvinciaDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_stato_cod", "AURA", nvl(dto.getAuraResidenzaStatoCod()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_stato_desc", "AURA", nvl(dto.getAuraResidenzaStatoDesc()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_cap", "AURA", nvl(dto.getAuraResidenzaCap()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_indirizzo", "AURA", nvl(dto.getAuraResidenzaIndirizzo()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_residenza_numero_civico", "AURA", nvl(dto.getAuraResidenzaNumeroCivico()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_tessera_team", "AURA", nvl(dto.getAuraTesseraTeam()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_id_aura", "AURA", nvl(dto.getAuraIdAura()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_email_aura", "AURA", nvl(dto.getAuraEmailAura()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_telefono_aura", "AURA", nvl(dto.getAuraTelefonoAura()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_data_decesso", "AURA", nvl(dto.getAuraDataDecesso()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "aura_assistenza_asl_fine", "AURA", nvl(formatDataConSentinella(dto.getAuraAssistenzaAslFine())), flagDatiAnonimizzati, mapCampiMascherati);
				//				scriviCella(row, col, "aura_assistenza_asl_fine", "AURA", nvl(dto.getAuraAssistenzaAslFine()), flagDatiAnonimizzati, mapCampiMascherati);

				// ===================== SOGGETTO =====================
				scriviCella(row, col, "soggettoemail", "SOGGETTO", nvl(dto.getEmail()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "soggettotelefono", "SOGGETTO", nvl(dto.getTelefono()), flagDatiAnonimizzati, mapCampiMascherati);

				// ===================== ESENZIONE - sempre visibili =====================
				scriviCella(row, col, "codesenzione",  "ESENZIONI", nvl(dto.getCodEsenzione()),  flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "descesenzione", "ESENZIONI", nvl(dto.getDescEsenzione()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "coddignosi",    "ESENZIONI", nvl(dto.getCodDiagnosi()),   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "descdignosi",   "ESENZIONI", nvl(dto.getDescDiagnosi()),  flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "dataemissione", "ESENZIONI", nvl(dto.getDataEmissione()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "datascadenza",  "ESENZIONI", nvl(dto.getDataScadenza()),  flagDatiAnonimizzati, mapCampiMascherati);

				// ===================== ANAMNESI =====================
				scriviCella(row, col, "anamnesi_codice_fiscale", "ANAMNESI", nvl(dto.getAnamnesiCodiceFiscale()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_id_aura", "ANAMNESI", nvl(dto.getAnamnesiIdAura()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_data_intervista", "ANAMNESI", nvl(dto.getAnamnesiDataIntervista()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_nominativo_intervistatore", "ANAMNESI", nvl(dto.getAnamnesiNominativoIntervistatore()), flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "anamnesi_fumatore", "ANAMNESI", nvl(dto.getAnamnesiFumatore()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigarette", "ANAMNESI", nvl(dto.getAnamnesiSigarette()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigarette_anni", "ANAMNESI", nvl(dto.getAnamnesiSigaretteAnni()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigarette_eta_inizio", "ANAMNESI", nvl(dto.getAnamnesiSigaretteEtaInizio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigarette_fuma_attualmente", "ANAMNESI", nvl(dto.getAnamnesiSigaretteFumaAttualmente()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigarette_eta_fine", "ANAMNESI", nvl(dto.getAnamnesiSigaretteEtaFine()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigarette_die", "ANAMNESI", nvl(dto.getAnamnesiSigaretteDie()), flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "anamnesi_sigari", "ANAMNESI", nvl(dto.getAnamnesiSigari()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigari_anni", "ANAMNESI", nvl(dto.getAnamnesiSigariAnni()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigari_eta_inizio", "ANAMNESI", nvl(dto.getAnamnesiSigariEtaInizio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigari_fuma_attualmente", "ANAMNESI", nvl(dto.getAnamnesiSigariFumaAttualmente()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigari_eta_fine", "ANAMNESI", nvl(dto.getAnamnesiSigariEtaFine()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_sigari_die", "ANAMNESI", nvl(dto.getAnamnesiSigariDie()), flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "anamnesi_pipa", "ANAMNESI", nvl(dto.getAnamnesiPipa()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_pipa_anni", "ANAMNESI", nvl(dto.getAnamnesiPipaAnni()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_pipa_eta_inizio", "ANAMNESI", nvl(dto.getAnamnesiPipaEtaInizio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_pipa_fuma_attualmente", "ANAMNESI", nvl(dto.getAnamnesiPipaFumaAttualmente()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_pipa_eta_fine", "ANAMNESI", nvl(dto.getAnamnesiPipaEtaFine()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_pipa_die", "ANAMNESI", nvl(dto.getAnamnesiPipaDie()), flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "anamnesi_occupazione_num", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneNum()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_anno_inizio", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneAnnoInizio()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_anno_fine", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneAnnoFine()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_tipo", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneTipo()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_descrizione_lavoro", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneDescrizioneLavoro()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_nome_e_indirizzo_ditta", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneNomeEIndirizzoDitta()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_attivita_ditta", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneAttivitaDitta()), flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "anamnesi_nota_attivita_con_amianto", "ANAMNESI", nvl(dto.getAnamnesiNotaAttivitaConAmianto()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_anamnesi_esposizione_amianto", "ANAMNESI", nvl(dto.getAnamnesiAnamnesiEsposizioneAmianto()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_esposizione_professionale", "ANAMNESI", nvl(dto.getAnamnesiEsposizioneProfessionale()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_anno_fine_esposizione", "ANAMNESI", nvl(dto.getAnamnesiAnnoFineEsposizione()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_livello_esposizione", "ANAMNESI", nvl(dto.getAnamnesiLivelloEsposizione()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_inserimento_in_sorveglianza", "ANAMNESI", nvl(dto.getAnamnesiInserimentoInSorveglianza()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_id_spresal", "ANAMNESI", nvl(dto.getAnamnesiIdSpresal()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_counseling", "ANAMNESI", nvl(dto.getAnamnesiCounseling()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_esposizione_crpt", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneEsposizioneCrpt()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_settore_ditta_crpt", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneSettoreDittaCrpt()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_mansione_crpt", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneMansioneCrpt()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_ragione_sociale_ditta_crpt", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneRagioneSocialeDittaCrpt()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_piva_ditta_crpt", "ANAMNESI", nvl(dto.getAnamnesiOccupazionePivaDittaCrpt()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "anamnesi_occupazione_codice_fiscale_ditta_crpt", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneCodiceFiscaleDittaCrpt()), flagDatiAnonimizzati, mapCampiMascherati);
                

                // ── ESITI VISITA ──────────────────────────────────────────────
				scriviCella(row, col, "esiti_vis_codice_fiscale",                                      "ESITI", nvl(dto.getEsitiVisCodiceFiscale()),                                      flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_id_aura",                                             "ESITI", nvl(dto.getEsitiVisIdAura()),                                             flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_data_visita",                                         "ESITI", nvl(dto.getEsitiVisDataVisita()),                                         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_visita",                                              "ESITI", nvl(dto.getEsitiVisVisita()),                                              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_livello_visita",                                      "ESITI", nvl(dto.getEsitiVisLivelloVisita()),                                      flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_riceve_indennizzo",                                   "ESITI", nvl(dto.getEsitiVisRiceveIndennizzo()),                                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_malattia_indennizzo",                                 "ESITI", nvl(dto.getEsitiVisMalattiaIndennizzo()),                                 flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_rx",                                     "ESITI", nvl(dto.getEsitiVisAccertamentiRx()),                                     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_rx_data",                                "ESITI", nvl(dto.getEsitiVisAccertamentiRxData()),                                flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_rx_referto_normale",                     "ESITI", nvl(dto.getEsitiVisAccertamentiRxRefertoNormale()),                     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_rx_acquisita",                           "ESITI", nvl(dto.getEsitiVisAccertamentiRxAcquisita()),                           flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_tc",                                     "ESITI", nvl(dto.getEsitiVisAccertamentiTc()),                                     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_tc_data",                                "ESITI", nvl(dto.getEsitiVisAccertamentiTcData()),                                flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_tc_referto_normale",                     "ESITI", nvl(dto.getEsitiVisAccertamentiTcRefertoNormale()),                     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_tc_acquisita",                           "ESITI", nvl(dto.getEsitiVisAccertamentiTcAcquisita()),                           flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_spirometria_semplice",                   "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaSemplice()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_spirometria_semplice_data",              "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaSempliceData()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_spirometria_semplice_referto_normale",   "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaSempliceRefertoNormale()),   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_spirometria_semplice_acquisita",         "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaSempliceAcquisita()),         flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_spirometria_globale",                    "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaGlobale()),                    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_spirometria_globale_data",               "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaGlobaleData()),               flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_spirometria_globale_referto_normale",    "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaGlobaleRefertoNormale()),    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_spirometria_globale_acquisita",          "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaGlobaleAcquisita()),          flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_dlco",                                   "ESITI", nvl(dto.getEsitiVisAccertamentiDlco()),                                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_dlco_data",                              "ESITI", nvl(dto.getEsitiVisAccertamentiDlcoData()),                              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_dlco_referto_normale",                   "ESITI", nvl(dto.getEsitiVisAccertamentiDlcoRefertoNormale()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_dlco_acquisita",                         "ESITI", nvl(dto.getEsitiVisAccertamentiDlcoAcquisita()),                         flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_pet",                                    "ESITI", nvl(dto.getEsitiVisAccertamentiPet()),                                    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_pet_data",                               "ESITI", nvl(dto.getEsitiVisAccertamentiPetData()),                               flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_pet_referto_normale",                    "ESITI", nvl(dto.getEsitiVisAccertamentiPetRefertoNormale()),                    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_pet_acquisita",                          "ESITI", nvl(dto.getEsitiVisAccertamentiPetAcquisita()),                          flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_visita_pneumologica",                    "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaPneumologica()),                    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_visita_pneumologica_data",               "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaPneumologicaData()),               flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_visita_pneumologica_referto",            "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaPneumologicaReferto()),            flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_visita_pneumologica_acquisita",          "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaPneumologicaAcquisita()),          flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_visita_radiologica",                     "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaRadiologica()),                     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_visita_radiologica_data",                "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaRadiologicaData()),                flagDatiAnonimizzati, mapCampiMascherati);
				
				scriviCella(row, col, "esiti_vis_accertamenti_visita_radiologica_referto",             "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaRadiologicaReferto()),             flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_visita_radiologica_acquisita",          "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaRadiologicaAcquisita()),          flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_visita_oncologica",                     "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaOncologica()),                     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_visita_oncologica_data",                "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaOncologicaData()),                flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_visita_oncologica_referto",             "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaOncologicaReferto()),             flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_visita_oncologica_acquisita",           "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaOncologicaAcquisita()),           flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_accertamenti_altro",                                 "ESITI", nvl(dto.getEsitiVisAccertamentiAltro()),                                 flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_altro_descrizione",                     "ESITI", nvl(dto.getEsitiVisAccertamentiAltroDescrizione()),                     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_altro_data",                            "ESITI", nvl(dto.getEsitiVisAccertamentiAltroData()),                            flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_altro_referto_normale",                 "ESITI", nvl(dto.getEsitiVisAccertamentiAltroRefertoNormale()),                 flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_accertamenti_altro_referto_acquisita",               "ESITI", nvl(dto.getEsitiVisAccertamentiAltroRefertoAcquisita()),               flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_risultato_negativo",                                 "ESITI", nvl(dto.getEsitiVisRisultatoNegativo()),                                 flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppm_placche_pleuriche_monolaterali",                 "ESITI", nvl(dto.getEsitiVisPpmPlacchePleuricheMonolaterali()),                 flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppm_primo_certificato_e_denuncia",                   "ESITI", nvl(dto.getEsitiVisPpmPrimoCertificatoEDenuncia()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppm_primo_certificato_e_denuncia_data",              "ESITI", nvl(dto.getEsitiVisPpmPrimoCertificatoEDenunciaData()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppm_aggravamento_e_denuncia",                        "ESITI", nvl(dto.getEsitiVisPpmAggravamentoEDenuncia()),                        flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppm_aggravamento_e_denuncia_data",                   "ESITI", nvl(dto.getEsitiVisPpmAggravamentoEDenunciaData()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppm_percentuale_di_riconoscimento",                  "ESITI", nvl(dto.getEsitiVisPpmPercentualeDiRiconoscimento()),                  flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppm_referto",                                        "ESITI", nvl(dto.getEsitiVisPpmReferto()),                                        flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppm_referto_data",                                   "ESITI", nvl(dto.getEsitiVisPpmRefertoData()),                                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppb_placche_pleuriche_bilaterali",                   "ESITI", nvl(dto.getEsitiVisPpbPlacchePleuricheBilaterali()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppb_primo_certificato_e_denuncia",                   "ESITI", nvl(dto.getEsitiVisPpbPrimoCertificatoEDenuncia()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppb_primo_certificato_e_denuncia_data",              "ESITI", nvl(dto.getEsitiVisPpbPrimoCertificatoEDenunciaData()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppb_aggravamento_e_denuncia",                        "ESITI", nvl(dto.getEsitiVisPpbAggravamentoEDenuncia()),                        flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppb_aggravamento_e_denuncia_data",                   "ESITI", nvl(dto.getEsitiVisPpbAggravamentoEDenunciaData()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppb_percentuale_di_riconoscimento",                  "ESITI", nvl(dto.getEsitiVisPpbPercentualeDiRiconoscimento()),                  flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppb_referto",                                        "ESITI", nvl(dto.getEsitiVisPpbReferto()),                                        flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ppb_referto_data",                                   "ESITI", nvl(dto.getEsitiVisPpbRefertoData()),                                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ap_asbestosi_polmonare",                             "ESITI", nvl(dto.getEsitiVisApAsbestosiPolmonare()),                             flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ap_primo_certificato_e_denuncia",                    "ESITI", nvl(dto.getEsitiVisApPrimoCertificatoEDenuncia()),                    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ap_primo_certificato_e_denuncia_data",               "ESITI", nvl(dto.getEsitiVisApPrimoCertificatoEDenunciaData()),               flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ap_aggravamento_e_denuncia",                         "ESITI", nvl(dto.getEsitiVisApAggravamentoEDenuncia()),                         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ap_aggravamento_e_denuncia_data",                    "ESITI", nvl(dto.getEsitiVisApAggravamentoEDenunciaData()),                    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ap_percentuale_di_riconoscimento",                   "ESITI", nvl(dto.getEsitiVisApPercentualeDiRiconoscimento()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ap_referto",                                         "ESITI", nvl(dto.getEsitiVisApReferto()),                                         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_ap_referto_data",                                    "ESITI", nvl(dto.getEsitiVisApRefertoData()),                                    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_fpd_fibrosi_pleurica_diffusa",                       "ESITI", nvl(dto.getEsitiVisFpdFibrosiPleuricaDiffusa()),                       flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_fpd_primo_certificato_e_denuncia",                   "ESITI", nvl(dto.getEsitiVisFpdPrimoCertificatoEDenuncia()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_fpd_primo_certificato_e_denuncia_data",              "ESITI", nvl(dto.getEsitiVisFpdPrimoCertificatoEDenunciaData()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_fpd_aggravamento_e_denuncia",              "ESITI", nvl(dto.getEsitiVisFpdAggravamentoEDenuncia()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_fpd_aggravamento_e_denuncia_data",         "ESITI", nvl(dto.getEsitiVisFpdAggravamentoEDenunciaData()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_fpd_percentuale_di_riconoscimento",        "ESITI", nvl(dto.getEsitiVisFpdPercentualeDiRiconoscimento()),        flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_fpd_referto",                              "ESITI", nvl(dto.getEsitiVisFpdReferto()),                              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_fpd_referto_data",                         "ESITI", nvl(dto.getEsitiVisFpdRefertoData()),                         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_mesotelioma_pleurico",                  "ESITI", nvl(dto.getEsitiVisMpMesoteliomaPleurico()),                  flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_primo_certificato_e_denuncia",          "ESITI", nvl(dto.getEsitiVisMpPrimoCertificatoEDenuncia()),          flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_primo_certificato_e_denuncia_data",     "ESITI", nvl(dto.getEsitiVisMpPrimoCertificatoEDenunciaData()),     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_aggravamento_e_denuncia",               "ESITI", nvl(dto.getEsitiVisMpAggravamentoEDenuncia()),               flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_aggravamento_e_denuncia_data",          "ESITI", nvl(dto.getEsitiVisMpAggravamentoEDenunciaData()),          flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_percentuale_di_riconoscimento",         "ESITI", nvl(dto.getEsitiVisMpPercentualeDiRiconoscimento()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_referto",                               "ESITI", nvl(dto.getEsitiVisMpReferto()),                               flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_referto_data",                          "ESITI", nvl(dto.getEsitiVisMpRefertoData()),                          flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_comunicazione_al_cor",                  "ESITI", nvl(dto.getEsitiVisMpComunicazioneAlCor()),                  flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_mp_comunicazione_al_cor_data",             "ESITI", nvl(dto.getEsitiVisMpComunicazioneAlCorData()),             flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_altro_mesotelioma",                     "ESITI", nvl(dto.getEsitiVisAmAltroMesotelioma()),                     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_primo_certificato_e_denuncia",         "ESITI", nvl(dto.getEsitiVisAmPrimoCertificatoEDenuncia()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_primo_certificato_e_denuncia_data",    "ESITI", nvl(dto.getEsitiVisAmPrimoCertificatoEDenunciaData()),    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_aggravamento_e_denuncia",              "ESITI", nvl(dto.getEsitiVisAmAggravamentoEDenuncia()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_aggravamento_e_denuncia_data",         "ESITI", nvl(dto.getEsitiVisAmAggravamentoEDenunciaData()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_percentuale_di_riconoscimento",        "ESITI", nvl(dto.getEsitiVisAmPercentualeDiRiconoscimento()),        flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_referto",                              "ESITI", nvl(dto.getEsitiVisAmReferto()),                              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_referto_data",                         "ESITI", nvl(dto.getEsitiVisAmRefertoData()),                         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_comunicazione_al_cor",                 "ESITI", nvl(dto.getEsitiVisAmComunicazioneAlCor()),                 flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_am_comunicazione_al_cor_data",            "ESITI", nvl(dto.getEsitiVisAmComunicazioneAlCorData()),            flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_nl_neoplasia_laringe",                    "ESITI", nvl(dto.getEsitiVisNlNeoplasiaLaringe()),                    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_nl_primo_certificato_e_denuncia",         "ESITI", nvl(dto.getEsitiVisNlPrimoCertificatoEDenuncia()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_nl_primo_certificato_e_denuncia_data",    "ESITI", nvl(dto.getEsitiVisNlPrimoCertificatoEDenunciaData()),    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_nl_aggravamento_e_denuncia",              "ESITI", nvl(dto.getEsitiVisNlAggravamentoEDenuncia()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_nl_aggravamento_e_denuncia_data",         "ESITI", nvl(dto.getEsitiVisNlAggravamentoEDenunciaData()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_nl_percentuale_di_riconoscimento",        "ESITI", nvl(dto.getEsitiVisNlPercentualeDiRiconoscimento()),        flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_nl_referto",                              "ESITI", nvl(dto.getEsitiVisNlReferto()),                              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_nl_referto_data",                         "ESITI", nvl(dto.getEsitiVisNlRefertoData()),                         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_no_neoplasia_ovarica",                    "ESITI", nvl(dto.getEsitiVisNoNeoplasiaOvarica()),                    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_no_primo_certificato_e_denuncia",         "ESITI", nvl(dto.getEsitiVisNoPrimoCertificatoEDenuncia()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_no_primo_certificato_e_denuncia_data",    "ESITI", nvl(dto.getEsitiVisNoPrimoCertificatoEDenunciaData()),    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_no_aggravamento_e_denuncia",              "ESITI", nvl(dto.getEsitiVisNoAggravamentoEDenuncia()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_no_aggravamento_e_denuncia_data",         "ESITI", nvl(dto.getEsitiVisNoAggravamentoEDenunciaData()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_no_percentuale_di_riconoscimento",        "ESITI", nvl(dto.getEsitiVisNoPercentualeDiRiconoscimento()),        flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_no_referto",                              "ESITI", nvl(dto.getEsitiVisNoReferto()),                              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_no_referto_data",                         "ESITI", nvl(dto.getEsitiVisNoRefertoData()),                         flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_tp_tumore_del_polmone",                   "ESITI", nvl(dto.getEsitiVisTpTumoreDelPolmone()),                   flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_tp_primo_certificato_e_denuncia",         "ESITI", nvl(dto.getEsitiVisTpPrimoCertificatoEDenuncia()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_tp_primo_certificato_e_denuncia_data",    "ESITI", nvl(dto.getEsitiVisTpPrimoCertificatoEDenunciaData()),    flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_tp_aggravamento_e_denuncia",              "ESITI", nvl(dto.getEsitiVisTpAggravamentoEDenuncia()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_tp_aggravamento_e_denuncia_data",         "ESITI", nvl(dto.getEsitiVisTpAggravamentoEDenunciaData()),         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_tp_percentuale_di_riconoscimento",        "ESITI", nvl(dto.getEsitiVisTpPercentualeDiRiconoscimento()),        flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_tp_referto",                              "ESITI", nvl(dto.getEsitiVisTpReferto()),                              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_tp_referto_data",                         "ESITI", nvl(dto.getEsitiVisTpRefertoData()),                         flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_tp_comunicazione_al_cor",                 "ESITI", nvl(dto.getEsitiVisTpComunicazioneAlCor()),                 flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_tp_comunicazione_al_cor_data",            "ESITI", nvl(dto.getEsitiVisTpComunicazioneAlCorData()),            flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_bpco_enfisema_polmonare",                 "ESITI", nvl(dto.getEsitiVisBpcoEnfisemaPolmonare()),                 flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_bpco_primo_certificato_e_denuncia",       "ESITI", nvl(dto.getEsitiVisBpcoPrimoCertificatoEDenuncia()),       flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_bpco_primo_certificato_e_denuncia_data",  "ESITI", nvl(dto.getEsitiVisBpcoPrimoCertificatoEDenunciaData()),  flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_bpco_aggravamento_e_denuncia",            "ESITI", nvl(dto.getEsitiVisBpcoAggravamentoEDenuncia()),            flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_bpco_aggravamento_e_denuncia_data",       "ESITI", nvl(dto.getEsitiVisBpcoAggravamentoEDenunciaData()),       flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_bpco_percentuale_di_riconoscimento",      "ESITI", nvl(dto.getEsitiVisBpcoPercentualeDiRiconoscimento()),      flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_bpco_referto",                            "ESITI", nvl(dto.getEsitiVisBpcoReferto()),                            flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_bpco_referto_data",                       "ESITI", nvl(dto.getEsitiVisBpcoRefertoData()),                       flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_altra_diagnosi",                          "ESITI", nvl(dto.getEsitiVisAltraDiagnosi()),                          flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_altra_diagnosi_descrizione",              "ESITI", nvl(dto.getEsitiVisAltraDiagnosiDescrizione()),              flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_altra_primo_certificato_e_denuncia",      "ESITI", nvl(dto.getEsitiVisAltraPrimoCertificatoEDenuncia()),      flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_altra_primo_certificato_e_denuncia_data", "ESITI", nvl(dto.getEsitiVisAltraPrimoCertificatoEDenunciaData()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_altra_aggravamento_e_denuncia",           "ESITI", nvl(dto.getEsitiVisAltraAggravamentoEDenuncia()),           flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_altra_aggravamento_e_denuncia_data",      "ESITI", nvl(dto.getEsitiVisAltraAggravamentoEDenunciaData()),      flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_altra_percentuale_di_riconoscimento",     "ESITI", nvl(dto.getEsitiVisAltraPercentualeDiRiconoscimento()),     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_altra_referto",                           "ESITI", nvl(dto.getEsitiVisAltraReferto()),                           flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_altra_referto_data",                      "ESITI", nvl(dto.getEsitiVisAltraRefertoData()),                      flagDatiAnonimizzati, mapCampiMascherati);

				scriviCella(row, col, "esiti_vis_follow_up_previsto",                      "ESITI", nvl(dto.getEsitiVisFollowUpPrevisto()),                      flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_anno_presunto_prossima_visita",           "ESITI", nvl(dto.getEsitiVisAnnoPresuntoProssimaVisita()),           flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_anno_ultima_visita",                      "ESITI", nvl(dto.getEsitiVisAnnoUltimaVisita()),                      flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_invio_sintesi_a_mmg",                     "ESITI", nvl(dto.getEsitiVisInvioSintesiAMmg()),                     flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "esiti_vis_id_spresal",                              "ESITI", nvl(dto.getEsitiVisIdSpresal()),                              flagDatiAnonimizzati, mapCampiMascherati);

//				===================== ESEMPI FINALI =====================
				scriviCella(row, col, "att_sanitaria_stato",          "DATODERIVATO", nvl(dto.getRegAttSanitariaStato()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "att_sanitaria_spresal",        "DATODERIVATO", nvl(dto.getRegAttSanitariaSpresal()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "att_sanitaria_spresal_data",   "DATODERIVATO", nvl(dto.getRegAttSanitariaSpresalData()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "att_sanitaria_inail",          "DATODERIVATO", nvl(dto.getRegAttSanitariaInail()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "sezione",                      "DATODERIVATO", nvl(dto.getRegSezione()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "inserito_sorveglianza",        "DATODERIVATO", nvl(dto.getRegInseritoInSorveglianza()), flagDatiAnonimizzati, mapCampiMascherati);
				scriviCella(row, col, "tipo_elenco_inail",            "DATODERIVATO", nvl(dto.getRegTipoElencoInail()), flagDatiAnonimizzati, mapCampiMascherati);

				
				// ── INAIL ──────────────────────────────────────────────────────
                scriviCella(row, col, "inail_domanda",             "INAIL", nvl(dto.getInailDomanda()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_cognome",             "INAIL", nvl(dto.getInailCognome()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_nome",                "INAIL", nvl(dto.getInailNome()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_codice_fiscale",      "INAIL", nvl(dto.getInailCodiceFiscale()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_sesso",               "INAIL", nvl(dto.getInailSesso()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_data_nascita",        "INAIL", nvl(dto.getInailDataNascita()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_indirizzo_residenza", "INAIL", nvl(dto.getInailIndirizzoResidenza()), flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_istat_residenza",     "INAIL", nvl(dto.getInailIstatResidenza()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_cap_residenza",       "INAIL", nvl(dto.getInailCapResidenza()),       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_regione_residenza",   "INAIL", nvl(dto.getInailRegioneResidenza()),   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_provincia_residenza", "INAIL", nvl(dto.getInailProvinciaResidenza()), flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_comune_residenza",    "INAIL", nvl(dto.getInailComuneResidenza()),    flagDatiAnonimizzati, mapCampiMascherati);
				
                
                // ── NPLA ──────────────────────────────────────────────────────
                scriviCella(row, col, "npla_codice_fiscale",      "NPLA", nvl(dto.getNplaCodiceFiscale()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_periodo",             "NPLA", nvl(dto.getNplaPeriodo()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_id_cantiere",         "NPLA", nvl(dto.getNplaIdCantiere()),         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_azienda_piva",        "NPLA", nvl(dto.getNplaAziendaPiva()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_azienda_nome",        "NPLA", nvl(dto.getNplaAziendaNome()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_asl_cantiere",        "NPLA", nvl(dto.getNplaAslCantiere()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_comune_cantiere",     "NPLA", nvl(dto.getNplaComuneCantiere()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_anno",                "NPLA", nvl(dto.getNplaAnno()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_tipologia_piano",     "NPLA", nvl(dto.getNplaTipologiaPiano()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_quantita_da_rimuovere","NPLA", nvl(dto.getNplaQuantitaDaRimuovere()),flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_quantita_rimossa",    "NPLA", nvl(dto.getNplaQuantitaRimossa()),    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "soggetto_stato_desc", "STATO_SOGGETTO", nvl(dto.getSoggettoStatoDesc()), flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "soggetto_id", "SOGGETTO ID", nvl(dto.getSoggettoId()), flagDatiAnonimizzati, mapCampiMascherati);
			}

			for (int i = 0; i < headers.length; i++) {
				sheet.setColumnWidth(i, 256 * 22);
			}

			workbook.write(out);
				workbook.dispose();

			return new ExcelExportResult(out.toByteArray(), uniqueName, fileId, elaborazioneId);

		} catch (Exception e) {
			throw new IllegalStateException("Errore durante la generazione del file Excel", e);
		}
	}

	

	private String nvl(String s) {
	    return s != null ? s : "";
	}

   
	//zzz
    private ExcelExportResult generaExcelTotaleAsincrono(List<ExportDTO> righe, Boolean flagDatiAnonimizzati, Integer assistenzaAslId, String cfOperatore) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

        	Integer elaborazioneId = null;
            // Carica la mappa dei campi mascherati
            Map<String, List<String>> mapCampiMascherati = anagraficaRepository.buildCampiMascheratiMap();

            String fileName;

            if (Boolean.TRUE.equals(flagDatiAnonimizzati) && assistenzaAslId != null) {
                // pseudo con filtro ASL
                fileName = "export_totale_pseudo_" + assistenzaAslId;
            } else if (Boolean.TRUE.equals(flagDatiAnonimizzati) && assistenzaAslId == null) {
                // pseudo senza filtro ASL
                fileName = "export_totale_pseudo";
            } else if (Boolean.FALSE.equals(flagDatiAnonimizzati) && assistenzaAslId != null) {
                // chiaro con filtro ASL
                fileName = "export_totale_chiaro_"+ assistenzaAslId;
            }
            else {
            	// chiaro senza filtro ASL
            	fileName = "export_totale_chiaro";
            }
            
            Integer fileId = tracciaElaborazioneService.inserisciFile(
                	fileName,
                	baseDir,
                	"",
                	"",
                	0,
                    2,
                    cfOperatore
                );
            
            elaborazioneId = tracciaElaborazioneService.inserisciStatoElaborazione(
                    fileId,
                    cfOperatore,
                    2
            );

            Sheet sheet = workbook.createSheet("Export Totale Asincrono");

            String[] headers = costruisciHeaders(flagDatiAnonimizzati);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (ExportDTO dto : righe) {
                Row row = sheet.createRow(rowNum++);
                int[] col = {0};

                // ── ADESIONE ──────────────────────────────────────────────────
                scriviCella(row, col, "adesione_codice_adesione",                  "ADESIONE", nvl(dto.getAdesioneCodiceAdesione()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_data_adesione",                    "ADESIONE", nvl(dto.getAdesioneDataAdesione()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_codice_fiscale",                   "ADESIONE", nvl(dto.getAdesioneCodiceFiscale()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_cognome",                          "ADESIONE", nvl(dto.getAdesioneCognome()),                          flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_nome",                             "ADESIONE", nvl(dto.getAdesioneNome()),                             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_data_di_nascita",                  "ADESIONE", nvl(dto.getAdesioneDataDiNascita()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_provincia_di_nascita",             "ADESIONE", nvl(dto.getAdesioneProvinciaDiNascita()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_comune_di_nascita",                "ADESIONE", nvl(dto.getAdesioneComuneDiNascita()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_tessera_team",                     "ADESIONE", nvl(dto.getAdesioneTesseraTeam()),                      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_id_aura",                          "ADESIONE", nvl(dto.getAdesioneIdAura()),                           flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_provincia_di_domicilio",           "ADESIONE", nvl(dto.getAdesioneProvinciaDiDomicilio()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_comune_di_domicilio",              "ADESIONE", nvl(dto.getAdesioneComuneDiDomicilio()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_codice_comune_istat_di_domicilio", "ADESIONE", nvl(dto.getAdesioneCodiceComuneIstatDiDomicilio()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_cap_di_domicilio",                 "ADESIONE", nvl(dto.getAdesioneCapDiDomicilio()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_email",                            "ADESIONE", nvl(dto.getAdesioneEmail()),                            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_telefono",                         "ADESIONE", nvl(dto.getAdesioneTelefono()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_codice_asl_di_domicilio",          "ADESIONE", nvl(dto.getAdesioneCodiceAslDiDomicilio()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_asl_di_domicilio",                 "ADESIONE", nvl(dto.getAdesioneAslDiDomicilio()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_codazi",                           "ADESIONE", nvl(dto.getAdesioneCodazi()),                           flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_codice_asl_di_residenza",          "ADESIONE", nvl(dto.getAdesioneCodiceAslDiResidenza()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_asl_di_residenza",                 "ADESIONE", nvl(dto.getAdesioneAslDiResidenza()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_data_inizio_esposizione",          "ADESIONE", nvl(dto.getAdesioneDataInizioEsposizione()),            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_data_fine_esposizione",            "ADESIONE", nvl(dto.getAdesioneDataFineEsposizione()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_azienda",                          "ADESIONE", nvl(dto.getAdesioneAzienda()),                          flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_codice_comune_istat_azienda",      "ADESIONE", nvl(dto.getAdesioneCodiceComuneIstatAzienda()),         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_comune_azienda",                   "ADESIONE", nvl(dto.getAdesioneComuneAzienda()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_cap_azienda",                      "ADESIONE", nvl(dto.getAdesioneCapAzienda()),                       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_provincia_azienda",                "ADESIONE", nvl(dto.getAdesioneProvinciaAzienda()),                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "adesione_mansione",                         "ADESIONE", nvl(dto.getAdesioneMansione()),                         flagDatiAnonimizzati, mapCampiMascherati);

                // ── AURA ──────────────────────────────────────────────────────
                scriviCella(row, col, "aura_codice_fiscale",           "AURA", nvl(dto.getAuraCodiceFiscale()),           flagDatiAnonimizzati, mapCampiMascherati);
//                scriviCella(row, col, "aura_domicilio_asl",            "AURA", nvl(dto.getAuraDomicilioAsl()),            flagDatiAnonimizzati, mapCampiMascherati);
//                scriviCella(row, col, "aura_residenza_asl",            "AURA", nvl(dto.getAuraResidenzaAsl()),            flagDatiAnonimizzati, mapCampiMascherati);
//                scriviCella(row, col, "aura_assistenza_asl",           "AURA", nvl(dto.getAuraAssistenzaAsl()),           flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_asl",            "AURA", nvl(dto.getAslAziendaDesc()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_asl",            "AURA", nvl(dto.getAslAziendaDesc()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_assistenza_asl",           "AURA", nvl(dto.getAslAziendaDesc()),              flagDatiAnonimizzati, mapCampiMascherati);
                
                scriviCella(row, col, "aura_nome",                     "AURA", nvl(dto.getAuraNome()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_cognome",                  "AURA", nvl(dto.getAuraCognome()),                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_sesso",                    "AURA", nvl(dto.getAuraSesso()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_nascita_data",             "AURA", nvl(dto.getAuraNascitaData()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_nascita_comune_cod",       "AURA", nvl(dto.getAuraNascitaComuneCod()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_nascita_comune_desc",      "AURA", nvl(dto.getAuraNascitaComuneDesc()),       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_nascita_provincia_cod",    "AURA", nvl(dto.getAuraNascitaProvinciaCod()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_nascita_provincia_desc",   "AURA", nvl(dto.getAuraNascitaProvinciaDesc()),    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_nascita_stato_cod",        "AURA", nvl(dto.getAuraNascitaStatoCod()),         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_nascita_stato_desc",       "AURA", nvl(dto.getAuraNascitaStatoDesc()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_cittadinanza_stato_cod",   "AURA", nvl(dto.getAuraCittadinanzaStatoCod()),    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_cittadinanza_stato_desc",  "AURA", nvl(dto.getAuraCittadinanzaStatoDesc()),   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_comune_cod",     "AURA", nvl(dto.getAuraDomicilioComuneCod()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_comune_desc",    "AURA", nvl(dto.getAuraDomicilioComuneDesc()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_provincia_cod",  "AURA", nvl(dto.getAuraDomicilioProvinciaCod()),   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_provincia_desc", "AURA", nvl(dto.getAuraDomicilioProvinciaDesc()),  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_stato_cod",      "AURA", nvl(dto.getAuraDomicilioStatoCod()),       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_stato_desc",     "AURA", nvl(dto.getAuraDomicilioStatoDesc()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_cap",            "AURA", nvl(dto.getAuraDomicilioCap()),            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_indirizzo",      "AURA", nvl(dto.getAuraDomicilioIndirizzo()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_domicilio_numero_civico",  "AURA", nvl(dto.getAuraDomicilioNumeroCivico()),   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_comune_cod",     "AURA", nvl(dto.getAuraResidenzaComuneCod()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_comune_desc",    "AURA", nvl(dto.getAuraResidenzaComuneDesc()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_provincia_cod",  "AURA", nvl(dto.getAuraResidenzaProvinciaCod()),   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_provincia_desc", "AURA", nvl(dto.getAuraResidenzaProvinciaDesc()),  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_stato_cod",      "AURA", nvl(dto.getAuraResidenzaStatoCod()),       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_stato_desc",     "AURA", nvl(dto.getAuraResidenzaStatoDesc()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_cap",            "AURA", nvl(dto.getAuraResidenzaCap()),            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_indirizzo",      "AURA", nvl(dto.getAuraResidenzaIndirizzo()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_residenza_numero_civico",  "AURA", nvl(dto.getAuraResidenzaNumeroCivico()),   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_tessera_team",             "AURA", nvl(dto.getAuraTesseraTeam()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_id_aura",                  "AURA", nvl(dto.getAuraIdAura()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_email_aura",               "AURA", nvl(dto.getAuraEmailAura()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_telefono_aura",            "AURA", nvl(dto.getAuraTelefonoAura()),            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_data_decesso",             "AURA", nvl(dto.getAuraDataDecesso()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "aura_assistenza_asl_fine", "AURA", nvl(formatDataConSentinella(dto.getAuraAssistenzaAslFine())), flagDatiAnonimizzati, mapCampiMascherati);
                //               xxx scriviCella(row, col, "aura_assistenza_asl_fine",      "AURA", nvl(dto.getAuraAssistenzaAslFine()),       flagDatiAnonimizzati, mapCampiMascherati);

                // ── SOGGETTO ──────────────────────────────────────────────────
                scriviCella(row, col, "soggetto_email",    "SOGGETTO", nvl(dto.getEmail()),    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "soggetto_telefono", "SOGGETTO", nvl(dto.getTelefono()), flagDatiAnonimizzati, mapCampiMascherati);

                // ── ESENZIONE (sempre visibili, non soggetti a mascheramento) ─
                row.createCell(col[0]++).setCellValue(nvl(dto.getCodEsenzione()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getDescEsenzione()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getCodDiagnosi()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getDescDiagnosi()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getDataEmissione()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getDataScadenza()));

                // ── ANAMNESI ──────────────────────────────────────────────────
                scriviCella(row, col, "anamnesi_codice_fiscale",                         "ANAMNESI", nvl(dto.getAnamnesiCodiceFiscale()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_id_aura",                                "ANAMNESI", nvl(dto.getAnamnesiIdAura()),                                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_data_intervista",                        "ANAMNESI", nvl(dto.getAnamnesiDataIntervista()),                        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_nominativo_intervistatore",              "ANAMNESI", nvl(dto.getAnamnesiNominativoIntervistatore()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_fumatore",                               "ANAMNESI", nvl(dto.getAnamnesiFumatore()),                               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigarette",                              "ANAMNESI", nvl(dto.getAnamnesiSigarette()),                              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigarette_anni",                         "ANAMNESI", nvl(dto.getAnamnesiSigaretteAnni()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigarette_eta_inizio",                   "ANAMNESI", nvl(dto.getAnamnesiSigaretteEtaInizio()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigarette_fuma_attualmente",             "ANAMNESI", nvl(dto.getAnamnesiSigaretteFumaAttualmente()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigarette_eta_fine",                     "ANAMNESI", nvl(dto.getAnamnesiSigaretteEtaFine()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigarette_die",                          "ANAMNESI", nvl(dto.getAnamnesiSigaretteDie()),                          flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigari",                                 "ANAMNESI", nvl(dto.getAnamnesiSigari()),                                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigari_anni",                            "ANAMNESI", nvl(dto.getAnamnesiSigariAnni()),                            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigari_eta_inizio",                      "ANAMNESI", nvl(dto.getAnamnesiSigariEtaInizio()),                      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigari_fuma_attualmente",                "ANAMNESI", nvl(dto.getAnamnesiSigariFumaAttualmente()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigari_eta_fine",                        "ANAMNESI", nvl(dto.getAnamnesiSigariEtaFine()),                        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_sigari_die",                             "ANAMNESI", nvl(dto.getAnamnesiSigariDie()),                             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_pipa",                                   "ANAMNESI", nvl(dto.getAnamnesiPipa()),                                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_pipa_anni",                              "ANAMNESI", nvl(dto.getAnamnesiPipaAnni()),                              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_pipa_eta_inizio",                        "ANAMNESI", nvl(dto.getAnamnesiPipaEtaInizio()),                        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_pipa_fuma_attualmente",                  "ANAMNESI", nvl(dto.getAnamnesiPipaFumaAttualmente()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_pipa_eta_fine",                          "ANAMNESI", nvl(dto.getAnamnesiPipaEtaFine()),                          flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_pipa_die",                               "ANAMNESI", nvl(dto.getAnamnesiPipaDie()),                               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_num",                        "ANAMNESI", nvl(dto.getAnamnesiOccupazioneNum()),                        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_anno_inizio",                "ANAMNESI", nvl(dto.getAnamnesiOccupazioneAnnoInizio()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_anno_fine",                  "ANAMNESI", nvl(dto.getAnamnesiOccupazioneAnnoFine()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_tipo",                       "ANAMNESI", nvl(dto.getAnamnesiOccupazioneTipo()),                       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_descrizione_lavoro",         "ANAMNESI", nvl(dto.getAnamnesiOccupazioneDescrizioneLavoro()),         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_nome_e_indirizzo_ditta",     "ANAMNESI", nvl(dto.getAnamnesiOccupazioneNomeEIndirizzoDitta()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_attivita_ditta",             "ANAMNESI", nvl(dto.getAnamnesiOccupazioneAttivitaDitta()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_nota_attivita_con_amianto",              "ANAMNESI", nvl(dto.getAnamnesiNotaAttivitaConAmianto()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_anamnesi_esposizione_amianto",           "ANAMNESI", nvl(dto.getAnamnesiAnamnesiEsposizioneAmianto()),           flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_esposizione_professionale",              "ANAMNESI", nvl(dto.getAnamnesiEsposizioneProfessionale()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_anno_fine_esposizione",                  "ANAMNESI", nvl(dto.getAnamnesiAnnoFineEsposizione()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_livello_esposizione",                    "ANAMNESI", nvl(dto.getAnamnesiLivelloEsposizione()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_inserimento_in_sorveglianza",            "ANAMNESI", nvl(dto.getAnamnesiInserimentoInSorveglianza()),            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_id_spresal",                             "ANAMNESI", nvl(dto.getAnamnesiIdSpresal()),                             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_counseling",            				 "ANAMNESI", nvl(dto.getAnamnesiCounseling()),            				flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_esposizione_crpt",           "ANAMNESI", nvl(dto.getAnamnesiOccupazioneEsposizioneCrpt()),           flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_settore_ditta_crpt",         "ANAMNESI", nvl(dto.getAnamnesiOccupazioneSettoreDittaCrpt()),         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_mansione_crpt",              "ANAMNESI", nvl(dto.getAnamnesiOccupazioneMansioneCrpt()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_ragione_sociale_ditta_crpt", "ANAMNESI", nvl(dto.getAnamnesiOccupazioneRagioneSocialeDittaCrpt()), flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_piva_ditta_crpt",            "ANAMNESI", nvl(dto.getAnamnesiOccupazionePivaDittaCrpt()),            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "anamnesi_occupazione_codice_fiscale_ditta_crpt",  "ANAMNESI", nvl(dto.getAnamnesiOccupazioneCodiceFiscaleDittaCrpt()),   flagDatiAnonimizzati, mapCampiMascherati);
                
                // ── ESITI VISITA ──────────────────────────────────────────────
                scriviCella(row, col, "esiti_vis_codice_fiscale",                                      "ESITI", nvl(dto.getEsitiVisCodiceFiscale()),                                      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_id_aura",                                             "ESITI", nvl(dto.getEsitiVisIdAura()),                                             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_data_visita",                                         "ESITI", nvl(dto.getEsitiVisDataVisita()),                                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_visita",                                              "ESITI", nvl(dto.getEsitiVisVisita()),                                              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_livello_visita",                                      "ESITI", nvl(dto.getEsitiVisLivelloVisita()),                                      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_riceve_indennizzo",                                   "ESITI", nvl(dto.getEsitiVisRiceveIndennizzo()),                                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_malattia_indennizzo",                                 "ESITI", nvl(dto.getEsitiVisMalattiaIndennizzo()),                                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_rx",                                     "ESITI", nvl(dto.getEsitiVisAccertamentiRx()),                                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_rx_data",                                "ESITI", nvl(dto.getEsitiVisAccertamentiRxData()),                                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_rx_referto_normale",                     "ESITI", nvl(dto.getEsitiVisAccertamentiRxRefertoNormale()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_rx_acquisita",                     "ESITI", nvl(dto.getEsitiVisAccertamentiRxAcquisita()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_tc",                                     "ESITI", nvl(dto.getEsitiVisAccertamentiTc()),                                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_tc_data",                                "ESITI", nvl(dto.getEsitiVisAccertamentiTcData()),                                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_tc_referto_normale",                     "ESITI", nvl(dto.getEsitiVisAccertamentiTcRefertoNormale()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_tc_acquisita",                     "ESITI", nvl(dto.getEsitiVisAccertamentiTcAcquisita()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_spirometria_semplice",                   "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaSemplice()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_spirometria_semplice_data",              "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaSempliceData()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_spirometria_semplice_referto_normale",   "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaSempliceRefertoNormale()),   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_spirometria_semplice_acquisita",   "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaSempliceAcquisita()),   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_spirometria_globale",                    "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaGlobale()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_spirometria_globale_data",               "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaGlobaleData()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_spirometria_globale_referto_normale",    "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaGlobaleRefertoNormale()),    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_spirometria_globale_acquisita",    "ESITI", nvl(dto.getEsitiVisAccertamentiSpirometriaGlobaleAcquisita()),    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_dlco",                                   "ESITI", nvl(dto.getEsitiVisAccertamentiDlco()),                                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_dlco_data",                              "ESITI", nvl(dto.getEsitiVisAccertamentiDlcoData()),                              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_dlco_referto_normale",                   "ESITI", nvl(dto.getEsitiVisAccertamentiDlcoRefertoNormale()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_dlco_acquisita",                   "ESITI", nvl(dto.getEsitiVisAccertamentiDlcoAcquisita()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_pet",                                    "ESITI", nvl(dto.getEsitiVisAccertamentiPet()),                                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_pet_data",                               "ESITI", nvl(dto.getEsitiVisAccertamentiPetData()),                               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_pet_referto_normale",                    "ESITI", nvl(dto.getEsitiVisAccertamentiPetRefertoNormale()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_pet_acquisita",                    "ESITI", nvl(dto.getEsitiVisAccertamentiPetAcquisita()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_pneumologica",                    "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaPneumologica()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_pneumologica_data",               "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaPneumologicaData()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_pneumologica_referto",            "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaPneumologicaReferto()),            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_pneumologica_acquisita",            "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaPneumologicaAcquisita()),            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_radiologica",                     "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaRadiologica()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_radiologica_data",                "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaRadiologicaData()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_radiologica_referto",             "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaRadiologicaReferto()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_radiologica_acquisita",             "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaRadiologicaAcquisita()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_oncologica",                      "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaOncologica()),                      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_oncologica_data",                 "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaOncologicaData()),                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_oncologica_referto",              "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaOncologicaReferto()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_visita_oncologica_acquisita",              "ESITI", nvl(dto.getEsitiVisAccertamentiVisitaOncologicaAcquisita()),              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_altro",                                  "ESITI", nvl(dto.getEsitiVisAccertamentiAltro()),                                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_altro_descrizione",                      "ESITI", nvl(dto.getEsitiVisAccertamentiAltroDescrizione()),                      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_altro_data",                             "ESITI", nvl(dto.getEsitiVisAccertamentiAltroData()),                             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_altro_referto_normale",                  "ESITI", nvl(dto.getEsitiVisAccertamentiAltroRefertoNormale()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_accertamenti_altro_referto_acquisita",                  "ESITI", nvl(dto.getEsitiVisAccertamentiAltroRefertoAcquisita()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_risultato_negativo",                                  "ESITI", nvl(dto.getEsitiVisRisultatoNegativo()),                                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppm_placche_pleuriche_monolaterali",                  "ESITI", nvl(dto.getEsitiVisPpmPlacchePleuricheMonolaterali()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppm_primo_certificato_e_denuncia",                    "ESITI", nvl(dto.getEsitiVisPpmPrimoCertificatoEDenuncia()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppm_primo_certificato_e_denuncia_data",               "ESITI", nvl(dto.getEsitiVisPpmPrimoCertificatoEDenunciaData()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppm_aggravamento_e_denuncia",                         "ESITI", nvl(dto.getEsitiVisPpmAggravamentoEDenuncia()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppm_aggravamento_e_denuncia_data",                    "ESITI", nvl(dto.getEsitiVisPpmAggravamentoEDenunciaData()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppm_percentuale_di_riconoscimento",                   "ESITI", nvl(dto.getEsitiVisPpmPercentualeDiRiconoscimento()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppm_referto",                                         "ESITI", nvl(dto.getEsitiVisPpmReferto()),                                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppm_referto_data",                                    "ESITI", nvl(dto.getEsitiVisPpmRefertoData()),                                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppb_placche_pleuriche_bilaterali",                    "ESITI", nvl(dto.getEsitiVisPpbPlacchePleuricheBilaterali()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppb_primo_certificato_e_denuncia",                    "ESITI", nvl(dto.getEsitiVisPpbPrimoCertificatoEDenuncia()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppb_primo_certificato_e_denuncia_data",               "ESITI", nvl(dto.getEsitiVisPpbPrimoCertificatoEDenunciaData()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppb_aggravamento_e_denuncia",                         "ESITI", nvl(dto.getEsitiVisPpbAggravamentoEDenuncia()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppb_aggravamento_e_denuncia_data",                    "ESITI", nvl(dto.getEsitiVisPpbAggravamentoEDenunciaData()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppb_percentuale_di_riconoscimento",                   "ESITI", nvl(dto.getEsitiVisPpbPercentualeDiRiconoscimento()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppb_referto",                                         "ESITI", nvl(dto.getEsitiVisPpbReferto()),                                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ppb_referto_data",                                    "ESITI", nvl(dto.getEsitiVisPpbRefertoData()),                                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ap_asbestosi_polmonare",                              "ESITI", nvl(dto.getEsitiVisApAsbestosiPolmonare()),                              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ap_primo_certificato_e_denuncia",                     "ESITI", nvl(dto.getEsitiVisApPrimoCertificatoEDenuncia()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ap_primo_certificato_e_denuncia_data",                "ESITI", nvl(dto.getEsitiVisApPrimoCertificatoEDenunciaData()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ap_aggravamento_e_denuncia",                          "ESITI", nvl(dto.getEsitiVisApAggravamentoEDenuncia()),                          flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ap_aggravamento_e_denuncia_data",                     "ESITI", nvl(dto.getEsitiVisApAggravamentoEDenunciaData()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ap_percentuale_di_riconoscimento",                    "ESITI", nvl(dto.getEsitiVisApPercentualeDiRiconoscimento()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ap_referto",                                          "ESITI", nvl(dto.getEsitiVisApReferto()),                                          flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_ap_referto_data",                                     "ESITI", nvl(dto.getEsitiVisApRefertoData()),                                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_fpd_fibrosi_pleurica_diffusa",                        "ESITI", nvl(dto.getEsitiVisFpdFibrosiPleuricaDiffusa()),                        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_fpd_primo_certificato_e_denuncia",                    "ESITI", nvl(dto.getEsitiVisFpdPrimoCertificatoEDenuncia()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_fpd_primo_certificato_e_denuncia_data",               "ESITI", nvl(dto.getEsitiVisFpdPrimoCertificatoEDenunciaData()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_fpd_aggravamento_e_denuncia",                         "ESITI", nvl(dto.getEsitiVisFpdAggravamentoEDenuncia()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_fpd_aggravamento_e_denuncia_data",                    "ESITI", nvl(dto.getEsitiVisFpdAggravamentoEDenunciaData()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_fpd_percentuale_di_riconoscimento",                   "ESITI", nvl(dto.getEsitiVisFpdPercentualeDiRiconoscimento()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_fpd_referto",                                         "ESITI", nvl(dto.getEsitiVisFpdReferto()),                                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_fpd_referto_data",                                    "ESITI", nvl(dto.getEsitiVisFpdRefertoData()),                                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_mesotelioma_pleurico",                             "ESITI", nvl(dto.getEsitiVisMpMesoteliomaPleurico()),                             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_primo_certificato_e_denuncia",                     "ESITI", nvl(dto.getEsitiVisMpPrimoCertificatoEDenuncia()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_primo_certificato_e_denuncia_data",                "ESITI", nvl(dto.getEsitiVisMpPrimoCertificatoEDenunciaData()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_aggravamento_e_denuncia",                          "ESITI", nvl(dto.getEsitiVisMpAggravamentoEDenuncia()),                          flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_aggravamento_e_denuncia_data",                     "ESITI", nvl(dto.getEsitiVisMpAggravamentoEDenunciaData()),                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_percentuale_di_riconoscimento",                    "ESITI", nvl(dto.getEsitiVisMpPercentualeDiRiconoscimento()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_referto",                                          "ESITI", nvl(dto.getEsitiVisMpReferto()),                                          flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_referto_data",                                    "ESITI", nvl(dto.getEsitiVisMpRefertoData()),                                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_comunicazione_al_cor",                            "ESITI", nvl(dto.getEsitiVisMpComunicazioneAlCor()),                            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_mp_comunicazione_al_cor_data",                       "ESITI", nvl(dto.getEsitiVisMpComunicazioneAlCorData()),                       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_altro_mesotelioma",                               "ESITI", nvl(dto.getEsitiVisAmAltroMesotelioma()),                               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_primo_certificato_e_denuncia",                    "ESITI", nvl(dto.getEsitiVisAmPrimoCertificatoEDenuncia()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_primo_certificato_e_denuncia_data",               "ESITI", nvl(dto.getEsitiVisAmPrimoCertificatoEDenunciaData()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_aggravamento_e_denuncia",                         "ESITI", nvl(dto.getEsitiVisAmAggravamentoEDenuncia()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_aggravamento_e_denuncia_data",                    "ESITI", nvl(dto.getEsitiVisAmAggravamentoEDenunciaData()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_percentuale_di_riconoscimento",                   "ESITI", nvl(dto.getEsitiVisAmPercentualeDiRiconoscimento()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_referto",                                         "ESITI", nvl(dto.getEsitiVisAmReferto()),                                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_referto_data",                                    "ESITI", nvl(dto.getEsitiVisAmRefertoData()),                                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_comunicazione_al_cor",                            "ESITI", nvl(dto.getEsitiVisAmComunicazioneAlCor()),                            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_am_comunicazione_al_cor_data",                       "ESITI", nvl(dto.getEsitiVisAmComunicazioneAlCorData()),                       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_nl_neoplasia_laringe",                               "ESITI", nvl(dto.getEsitiVisNlNeoplasiaLaringe()),                               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_nl_primo_certificato_e_denuncia",                    "ESITI", nvl(dto.getEsitiVisNlPrimoCertificatoEDenuncia()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_nl_primo_certificato_e_denuncia_data",               "ESITI", nvl(dto.getEsitiVisNlPrimoCertificatoEDenunciaData()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_nl_aggravamento_e_denuncia",                         "ESITI", nvl(dto.getEsitiVisNlAggravamentoEDenuncia()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_nl_aggravamento_e_denuncia_data",                    "ESITI", nvl(dto.getEsitiVisNlAggravamentoEDenunciaData()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_nl_percentuale_di_riconoscimento",                   "ESITI", nvl(dto.getEsitiVisNlPercentualeDiRiconoscimento()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_nl_referto",                                         "ESITI", nvl(dto.getEsitiVisNlReferto()),                                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_nl_referto_data",                                    "ESITI", nvl(dto.getEsitiVisNlRefertoData()),                                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_no_neoplasia_ovarica",                               "ESITI", nvl(dto.getEsitiVisNoNeoplasiaOvarica()),                               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_no_primo_certificato_e_denuncia",                    "ESITI", nvl(dto.getEsitiVisNoPrimoCertificatoEDenuncia()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_no_primo_certificato_e_denuncia_data",               "ESITI", nvl(dto.getEsitiVisNoPrimoCertificatoEDenunciaData()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_no_aggravamento_e_denuncia",                         "ESITI", nvl(dto.getEsitiVisNoAggravamentoEDenuncia()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_no_aggravamento_e_denuncia_data",                    "ESITI", nvl(dto.getEsitiVisNoAggravamentoEDenunciaData()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_no_percentuale_di_riconoscimento",                   "ESITI", nvl(dto.getEsitiVisNoPercentualeDiRiconoscimento()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_no_referto",                                         "ESITI", nvl(dto.getEsitiVisNoReferto()),                                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_no_referto_data",                                    "ESITI", nvl(dto.getEsitiVisNoRefertoData()),                                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_tumore_del_polmone",                              "ESITI", nvl(dto.getEsitiVisTpTumoreDelPolmone()),                              flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_primo_certificato_e_denuncia",                    "ESITI", nvl(dto.getEsitiVisTpPrimoCertificatoEDenuncia()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_primo_certificato_e_denuncia_data",               "ESITI", nvl(dto.getEsitiVisTpPrimoCertificatoEDenunciaData()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_aggravamento_e_denuncia",                         "ESITI", nvl(dto.getEsitiVisTpAggravamentoEDenuncia()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_aggravamento_e_denuncia_data",                    "ESITI", nvl(dto.getEsitiVisTpAggravamentoEDenunciaData()),                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_percentuale_di_riconoscimento",                   "ESITI", nvl(dto.getEsitiVisTpPercentualeDiRiconoscimento()),                   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_referto",                                         "ESITI", nvl(dto.getEsitiVisTpReferto()),                                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_referto_data",                                    "ESITI", nvl(dto.getEsitiVisTpRefertoData()),                                    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_comunicazione_al_cor",                            "ESITI", nvl(dto.getEsitiVisTpComunicazioneAlCor()),                            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_tp_comunicazione_al_cor_data",                       "ESITI", nvl(dto.getEsitiVisTpComunicazioneAlCorData()),                       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_bpco_enfisema_polmonare",                            "ESITI", nvl(dto.getEsitiVisBpcoEnfisemaPolmonare()),                            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_bpco_primo_certificato_e_denuncia",                  "ESITI", nvl(dto.getEsitiVisBpcoPrimoCertificatoEDenuncia()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_bpco_primo_certificato_e_denuncia_data",             "ESITI", nvl(dto.getEsitiVisBpcoPrimoCertificatoEDenunciaData()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_bpco_aggravamento_e_denuncia",                       "ESITI", nvl(dto.getEsitiVisBpcoAggravamentoEDenuncia()),                       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_bpco_aggravamento_e_denuncia_data",                  "ESITI", nvl(dto.getEsitiVisBpcoAggravamentoEDenunciaData()),                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_bpco_percentuale_di_riconoscimento",                 "ESITI", nvl(dto.getEsitiVisBpcoPercentualeDiRiconoscimento()),                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_bpco_referto",                                       "ESITI", nvl(dto.getEsitiVisBpcoReferto()),                                       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_bpco_referto_data",                                  "ESITI", nvl(dto.getEsitiVisBpcoRefertoData()),                                  flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_altra_diagnosi",                                     "ESITI", nvl(dto.getEsitiVisAltraDiagnosi()),                                     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_altra_diagnosi_descrizione",                         "ESITI", nvl(dto.getEsitiVisAltraDiagnosiDescrizione()),                         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_altra_primo_certificato_e_denuncia",                 "ESITI", nvl(dto.getEsitiVisAltraPrimoCertificatoEDenuncia()),                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_altra_primo_certificato_e_denuncia_data",            "ESITI", nvl(dto.getEsitiVisAltraPrimoCertificatoEDenunciaData()),            flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_altra_aggravamento_e_denuncia",                      "ESITI", nvl(dto.getEsitiVisAltraAggravamentoEDenuncia()),                      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_altra_aggravamento_e_denuncia_data",                 "ESITI", nvl(dto.getEsitiVisAltraAggravamentoEDenunciaData()),                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_altra_percentuale_di_riconoscimento",                "ESITI", nvl(dto.getEsitiVisAltraPercentualeDiRiconoscimento()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_altra_referto",                                      "ESITI", nvl(dto.getEsitiVisAltraReferto()),                                      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_altra_referto_data",                                 "ESITI", nvl(dto.getEsitiVisAltraRefertoData()),                                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_follow_up_previsto",                                 "ESITI", nvl(dto.getEsitiVisFollowUpPrevisto()),                                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_anno_presunto_prossima_visita",                      "ESITI", nvl(dto.getEsitiVisAnnoPresuntoProssimaVisita()),                      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_anno_ultima_visita",                                 "ESITI", nvl(dto.getEsitiVisAnnoUltimaVisita()),                                 flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_invio_sintesi_a_mmg",                               "ESITI", nvl(dto.getEsitiVisInvioSintesiAMmg()),                               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "esiti_vis_id_spresal",                                         "ESITI", nvl(dto.getEsitiVisIdSpresal()),                                         flagDatiAnonimizzati, mapCampiMascherati);

                row.createCell(col[0]++).setCellValue(nvl(dto.getRegAttSanitariaStato()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getRegAttSanitariaSpresal()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getRegAttSanitariaSpresalData()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getRegAttSanitariaInail()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getRegSezione()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getRegInseritoInSorveglianza()));
                row.createCell(col[0]++).setCellValue(nvl(dto.getRegTipoElencoInail()));
                
                // ── INAIL ──────────────────────────────────────────────────────
                scriviCella(row, col, "inail_domanda",             "INAIL", nvl(dto.getInailDomanda()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_cognome",             "INAIL", nvl(dto.getInailCognome()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_nome",                "INAIL", nvl(dto.getInailNome()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_codice_fiscale",      "INAIL", nvl(dto.getInailCodiceFiscale()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_sesso",               "INAIL", nvl(dto.getInailSesso()),               flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_data_nascita",        "INAIL", nvl(dto.getInailDataNascita()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_indirizzo_residenza", "INAIL", nvl(dto.getInailIndirizzoResidenza()), flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_istat_residenza",     "INAIL", nvl(dto.getInailIstatResidenza()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_cap_residenza",       "INAIL", nvl(dto.getInailCapResidenza()),       flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_regione_residenza",   "INAIL", nvl(dto.getInailRegioneResidenza()),   flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_provincia_residenza", "INAIL", nvl(dto.getInailProvinciaResidenza()), flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "inail_comune_residenza",    "INAIL", nvl(dto.getInailComuneResidenza()),    flagDatiAnonimizzati, mapCampiMascherati);
            
                // ── NPLA ──────────────────────────────────────────────────────
                scriviCella(row, col, "npla_codice_fiscale",      "NPLA", nvl(dto.getNplaCodiceFiscale()),      flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_periodo",             "NPLA", nvl(dto.getNplaPeriodo()),             flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_id_cantiere",         "NPLA", nvl(dto.getNplaIdCantiere()),         flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_azienda_piva",        "NPLA", nvl(dto.getNplaAziendaPiva()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_azienda_nome",        "NPLA", nvl(dto.getNplaAziendaNome()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_asl_cantiere",        "NPLA", nvl(dto.getNplaAslCantiere()),        flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_comune_cantiere",     "NPLA", nvl(dto.getNplaComuneCantiere()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_anno",                "NPLA", nvl(dto.getNplaAnno()),                flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_tipologia_piano",     "NPLA", nvl(dto.getNplaTipologiaPiano()),     flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_quantita_da_rimuovere","NPLA", nvl(dto.getNplaQuantitaDaRimuovere()),flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "npla_quantita_rimossa",    "NPLA", nvl(dto.getNplaQuantitaRimossa()),    flagDatiAnonimizzati, mapCampiMascherati);
                scriviCella(row, col, "soggetto_stato_desc", "STATO_SOGGETTO", nvl(dto.getSoggettoStatoDesc()), flagDatiAnonimizzati, mapCampiMascherati);

            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
//            return out.toByteArray();
            
            return new ExcelExportResult(out.toByteArray(), fileName, fileId, elaborazioneId);

        } catch (Exception e) {
            throw new IllegalStateException("Errore durante la generazione del file Excel", e);
        }
    }
    


    private String[] costruisciHeaders(Boolean flagDatiAnonimizzati) {
		
    	String[] headers = new String[0];
    	
    	if (Boolean.FALSE.equals(flagDatiAnonimizzati)) {
    		headers = new String[]{
            		"Adesione - Codice Adesione",
                    "Adesione - Data Adesione",
                    "Adesione - Codice Fiscale",
                    "Adesione - Cognome",
                    "Adesione - Nome",
                    "Adesione - Data di nascita",
                    "Adesione - Provincia di nascita",
                    "Adesione - Comune di nascita",
                    "Adesione - Tessera Team",
                    "Adesione - Id Aura",
                    "Adesione - Provincia di domicilio",
                    "Adesione - Comune di domicilio",
                    "Adesione - Codice comune ISTAT di domicilio",
                    "Adesione - CAP di domicilio",
                    "Adesione - Email",
                    "Adesione - Telefono",
                    "Adesione - Codice ASL di domicilio",
                    "Adesione - ASL di domicilio",
                    "Adesione - Codazi",
                    "Adesione - Codice ASL di residenza",
                    "Adesione - ASL di residenza",
                    "Adesione - Data inizio esposizione",
                    "Adesione - Data fine esposizione",
                    "Adesione - Azienda",
                    "Adesione - Codice comune ISTAT azienda",
                    "Adesione - Comune azienda",
                    "Adesione - CAP azienda",
                    "Adesione - Provincia azienda",
                    "Adesione - Mansione",
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
                    "Soggetto - Email",
                    "Soggetto - Telefono",
                    "Aura - Codice esenzione",
                    "Aura - Descrizione esenzione",
                    "Aura - Codice diagnosi",
                    "Aura - Descrizione diagnosi",
                    "Aura - Data emissione esenzione",
                    "Aura - Data scadenza esenzione",
                    "Anamnesi - Codice Fiscale",
                    "Anamnesi - Id Aura",
                    "Anamnesi - Data intervista",
                    "Anamnesi - Nominativo intervistatore",
                    "Anamnesi - Fumatore",
                    "Anamnesi - Sigarette",
                    "Anamnesi - Sigarette anni",
                    "Anamnesi - Sigarette età inizio",
                    "Anamnesi - Sigarette fuma attualmente",
                    "Anamnesi - Sigarette età fine",
                    "Anamnesi - Sigarette die",
                    "Anamnesi - Sigari",
                    "Anamnesi - Sigari anni",
                    "Anamnesi - Sigari età inizio",
                    "Anamnesi - Sigari fuma attualmente",
                    "Anamnesi - Sigari età fine",
                    "Anamnesi - Sigari die",
                    "Anamnesi - Pipa",
                    "Anamnesi - Pipa anni",
                    "Anamnesi - Pipa età inizio",
                    "Anamnesi - Pipa fuma attualmente",
                    "Anamnesi - Pipa età fine",
                    "Anamnesi - Pipa die",
                    "Anamnesi - Occupazione num",
                    "Anamnesi - Occupazione anno inizio",
                    "Anamnesi - Occupazione anno fine",
                    "Anamnesi - Occupazione tipo",
                    "Anamnesi - Occupazione descrizione lavoro",
                    "Anamnesi - Occupazione nome e indirizzo ditta",
                    "Anamnesi - Occupazione attività ditta",
                    "Anamnesi - Nota attività con amianto",
                    "Anamnesi - Anamnesi esposizione amianto",
                    "Anamnesi - Esposizione professionale",
                    "Anamnesi - Anno fine esposizione",
                    "Anamnesi - Livello esposizione",
                    "Anamnesi - Inserimento in sorveglianza",
                    "Anamnesi - Id SPRESAL",
                    "Anamnesi - Counseling",
                    "Anamnesi - Occupazione Esposizione CRPT",
                    "Anamnesi - Occupazione Settore Ditta CRPT",
                    "Anamnesi - Occupazione Mansione CRPT",
                    "Anamnesi - Occupazione Ragione Sociale Ditta CRPT",
                    "Anamnesi - Occupazione Piva Ditta CRPT",
                    "Anamnesi - Occupazione Codice Fiscale Ditta CRPT",
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
                     "Att. Sanitaria Stato",
                     "Att. Sanitaria Spresal",
                     "Att. Sanitaria Spresal Data",
                     "Att. Sanitaria Inail",
                     "Sezione",
                     "Inserito Sorveglianza",
                     "Tipo Elenco Inail",
                    "Inail - Domanda",
                    "Inail - Cognome",
                    "Inail - Nome",
                    "Inail - Codice Fiscale",
                    "Inail - Sesso",
                    "Inail - Data Nascita",
                    "Inail - Indirizzo Residenza",
                    "Inail - Istat Residenza",
                    "Inail - CAP Residenza",
                    "Inail - Regione Residenza",
                    "Inail - Provincia Residenza",
                    "Inail - Comune Residenza",
                    "Npla - Codice Fiscale",
                    "Npla - Periodo",
                    "Npla - Id Cantiere",
                    "Npla - Azienda Piva",
                    "Npla - Azienda Nome",
                    "Npla - Asl Cantiere",
                    "Npla - Comune Cantiere",
                    "Npla - Anno",
                    "Npla - Tipologia Piano",
                    "Npla - Quantita da Rimuovere",
                    "Npla - Quantita Rimossa"
            };
    	}
    	else {
    		headers = new String[]{
                    "Adesione - Data Adesione",
                    "Adesione - Provincia di nascita",
                    "Adesione - Comune di nascita",
                    "Adesione - Provincia di domicilio",
                    "Adesione - Codice comune ISTAT di domicilio",
                    "Adesione - Codice ASL di domicilio",
                    "Adesione - ASL di domicilio",
                    "Adesione - Codazi",
                    "Adesione - Codice ASL di residenza",
                    "Adesione - ASL di residenza",
                    "Adesione - Data inizio esposizione",
                    "Adesione - Data fine esposizione",
                    "Adesione - Azienda",
                    "Adesione - Codice comune ISTAT azienda",
                    "Adesione - Comune azienda",
                    "Adesione - CAP azienda",
                    "Adesione - Provincia azienda",
                    "Adesione - Mansione",
                    "Aura - Domicilio ASL",
                    "Aura - Residenza ASL",
                    "Aura - Assistenza ASL",
                    "Aura - Sesso",
                    "Aura - Provincia nascita cod",
                    "Aura - Provincia nascita desc",
                    "Aura - Stato nascita cod",
                    "Aura - Stato nascita desc",
                    "Aura - Cittadinanza stato cod",
                    "Aura - Cittadinanza stato desc",
                    "Aura - Domicilio provincia cod",
                    "Aura - Domicilio provincia desc",
                    "Aura - Domicilio stato cod",
                    "Aura - Domicilio stato desc",
                    "Aura - Residenza provincia cod",
                    "Aura - Residenza provincia desc",
                    "Aura - Residenza stato cod",
                    "Aura - Residenza stato desc",
                    "Aura - Data decesso",
                    "Aura - Assistenza ASL fine",
                    "Aura - Codice esenzione",
                    "Aura - Descrizione esenzione",
                    "Aura - Codice diagnosi",
                    "Aura - Descrizione diagnosi",
                    "Aura - Data emissione esenzione",
                    "Aura - Data scadenza esenzione",
                    "Anamnesi - Data intervista",
                    "Anamnesi - Nominativo intervistatore",
                    "Anamnesi - Fumatore",
                    "Anamnesi - Sigarette",
                    "Anamnesi - Sigarette anni",
                    "Anamnesi - Sigarette età inizio",
                    "Anamnesi - Sigarette fuma attualmente",
                    "Anamnesi - Sigarette età fine",
                    "Anamnesi - Sigarette die",
                    "Anamnesi - Sigari",
                    "Anamnesi - Sigari anni",
                    "Anamnesi - Sigari età inizio",
                    "Anamnesi - Sigari fuma attualmente",
                    "Anamnesi - Sigari età fine",
                    "Anamnesi - Sigari die",
                    "Anamnesi - Pipa",
                    "Anamnesi - Pipa anni",
                    "Anamnesi - Pipa età inizio",
                    "Anamnesi - Pipa fuma attualmente",
                    "Anamnesi - Pipa età fine",
                    "Anamnesi - Pipa die",
                    "Anamnesi - Occupazione num",
                    "Anamnesi - Occupazione anno inizio",
                    "Anamnesi - Occupazione anno fine",
                    "Anamnesi - Occupazione tipo",
                    "Anamnesi - Occupazione descrizione lavoro",
                    "Anamnesi - Occupazione attività ditta",
                    "Anamnesi - Nota attività con amianto",
                    "Anamnesi - Anamnesi esposizione amianto",
                    "Anamnesi - Esposizione professionale",
                    "Anamnesi - Anno fine esposizione",
                    "Anamnesi - Livello esposizione",
                    "Anamnesi - Inserimento in sorveglianza",
                    "Anamnesi - Id SPRESAL",
                    "Anamnesi - Counseling",
                    "Anamnesi - Occupazione Esposizione CRPT",
                    "Anamnesi - Occupazione Settore Ditta CRPT",
                    "Anamnesi - Occupazione Mansione CRPT",
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
                     "Att. Sanitaria Stato",
                     "Att. Sanitaria Spresal",
                     "Att. Sanitaria Spresal Data",
                     "Att. Sanitaria Inail",
                     "Sezione",
                     "Inserito Sorveglianza",
                     "Tipo Elenco Inail",
                    "Inail - Sesso",
                    "Inail - Regione Residenza",
                    "Inail - Provincia Residenza",
                    "Npla - Periodo",
                    "Npla - Id Cantiere",
                    "Npla - Asl Cantiere",
                    "Npla - Anno",
                    "Npla - Tipologia Piano",
                    "Npla - Quantita da Rimuovere",
                    "Npla - Quantita Rimossa"
            };
    	}
    	
		return headers;
	}

	/**
     * Restituisce il valore da scrivere in cella.
     *
     * Se flagDatiAnonimizzati è TRUE e il campo è presente nella mappa
     * dei campi mascherati → restituisce stringa vuota (campo oscurato).
     * Altrimenti → restituisce il valore originale.
     *
     * @param nomeCampo           nome del campo (es. "inail_cognome")
     * @param chiaveMappa         chiave della mappa (es. "INAIL")
     * @param valore              valore originale del campo
     * @param flagDatiAnonimizzati se true attiva il mascheramento
     * @param mapCampiMascherati  mappa dei campi da mascherare
     */ 
    private boolean isCampoVisibile(String campoCod,
    		Boolean flagDatiAnonimizzati,
    		Map<String, List<String>> mapCampiMascherati) {

    	// Se NON siamo in modalità anonima, tutti i campi sono visibili
    	if (Boolean.FALSE.equals(flagDatiAnonimizzati)) {
    		return true;
    	}

    	List<String> sezioni = mapCampiMascherati.get(campoCod);
    	return sezioni == null || sezioni.isEmpty();
    }
    
    
    /**
     * Restituisce il valore originale se il campo è visibile,
     * altrimenti null (il tipo T resta quello del getter).
     */
    private String valoreCellaString(String campoCod,
    		String sezione,
    		Object valoreOriginale,
    		Boolean flagDatiAnonimizzati,
    		Map<String, List<String>> mapCampiMascherati) {

    	if (Boolean.TRUE.equals(flagDatiAnonimizzati)) {
    		List<String> campiMascherati = mapCampiMascherati.getOrDefault(sezione, Collections.emptyList());
    		if (valoreOriginale != null && campiMascherati.contains(campoCod)) {
    			return "*****";
    		}
    	}

    	return valoreOriginale != null ? String.valueOf(valoreOriginale) : null;
    }
    
    
    /**
     * Verifica se il campo deve essere mascherato.
     * Restituisce true se flagDatiAnonimizzati=true
     * e il campo è contenuto nella mappa.
     */
    private boolean isCampoMascherato(String nomeCampo,
                                       String chiaveMappa,
                                       Boolean flagDatiAnonimizzati,
                                       Map<String, List<String>> mapCampiMascherati) {
        if (Boolean.TRUE.equals(flagDatiAnonimizzati)) {
            List<String> campiMascherati = mapCampiMascherati.getOrDefault(chiaveMappa, Collections.emptyList());
            return campiMascherati.contains(nomeCampo);
        }
        return false;
    }
    
    private void scriviCella(Row row, int[] colRef,
    		String nomeCampo, String chiaveMappa,
    		String valore,
    		Boolean flagDatiAnonimizzati,
    		Map<String, List<String>> mapCampiMascherati) {
    	if (!isCampoMascherato2(nomeCampo, chiaveMappa, flagDatiAnonimizzati, mapCampiMascherati)) {
    		row.createCell(colRef[0]).setCellValue(valore != null ? valore : "");
    		colRef[0]++;
    	}
    	
    }
    
    
    private String nvl(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
 
    private String formatDataConSentinella(LocalDate data) {
        LocalDate sentinella = LocalDate.of(9999, 12, 31);

        if (data == null || sentinella.equals(data)) {
            return "";
        }

        return data.toString();
    }

	@Override
	public Integer recuperaScarti(Integer fileId) {
		return anagraficaRepository.recuperaScarti(fileId);
	}

	@Override
	public void chiudiTuttiRecordTabAdesioneByFile(Integer fileId) {
		anagraficaRepository.chiudiTuttiRecordTabAdesioneByFile(fileId);
	}

}
