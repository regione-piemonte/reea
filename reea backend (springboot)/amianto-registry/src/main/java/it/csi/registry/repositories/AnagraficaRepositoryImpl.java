package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaDAsl.REEA_D_ASL;
import static it.csi.registry.jooq.tables.ReeaDEsenzione.REEA_D_ESENZIONE;
import static it.csi.registry.jooq.tables.ReeaDFonte.REEA_D_FONTE;
import static it.csi.registry.jooq.tables.ReeaDSoggettoStato.REEA_D_SOGGETTO_STATO;
import static it.csi.registry.jooq.tables.ReeaLFileScaricoErrore.REEA_L_FILE_SCARICO_ERRORE;
import static it.csi.registry.jooq.tables.ReeaRSoggettoEsenzione.REEA_R_SOGGETTO_ESENZIONE;
import static it.csi.registry.jooq.tables.ReeaSSoggetto.REEA_S_SOGGETTO;
import static it.csi.registry.jooq.tables.ReeaTAdesione.REEA_T_ADESIONE;
import static it.csi.registry.jooq.tables.ReeaTEsposizione.REEA_T_ESPOSIZIONE;
import static it.csi.registry.jooq.tables.ReeaTRegistro.REEA_T_REGISTRO;
import static it.csi.registry.jooq.tables.ReeaTRegistroInail.REEA_T_REGISTRO_INAIL;
import static it.csi.registry.jooq.tables.ReeaTRegistroPdlAmianto.REEA_T_REGISTRO_PDL_AMIANTO;
import static it.csi.registry.jooq.tables.ReeaTSoggetto.REEA_T_SOGGETTO;
import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalAnamnesi.REEA_T_REGISTRO_SPRESAL_ANAMNESI;
import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalEsiti.REEA_T_REGISTRO_SPRESAL_ESITI;
import static it.csi.registry.jooq.tables.ReeaDCampoMascherato.REEA_D_CAMPO_MASCHERATO;
import static it.csi.registry.jooq.tables.ReeaDNazione.REEA_D_NAZIONE;
import static it.csi.registry.jooq.tables.ReeaDComune.REEA_D_COMUNE;
import static it.csi.registry.jooq.tables.ReeaDProvincia.REEA_D_PROVINCIA;
import static it.csi.registry.jooq.tables.ReeaCParametro.REEA_C_PARAMETRO;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.val;

import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.Table;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Record9;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Repository;


import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.EsenzioneDTO;
import it.csi.registry.model.EsposizioneDTO;
import it.csi.registry.model.ExportDTO;
import it.csi.registry.model.InailDTO;
import it.csi.registry.model.NplaDTO;
import it.csi.registry.model.RegistroDTO;
import it.csi.registry.model.ScartoFileDTO;
import it.csi.registry.model.SpresalDTO;
import it.csi.registry.model.StoriaStatoDTO;
import it.csi.registry.services.TracciaElaborazioneService;
import it.csi.registry.util.CodiceFiscale;
import it.csi.registry.util.DateConversionUtils;
import it.csi.registry.util.RegistroUtils;
import it.csi.registry.util.SpresalUtils;


@Repository
public class AnagraficaRepositoryImpl implements AnagraficaRepository{

    private final DSLContext dsl;
    private final FonteRepository fonteRepository;
    private final EsenzioneRepository esenzioneRepository;
    private final TracciaElaborazioneRepository tracciaElaborazioneRepository;
    private final TracciaElaborazioneService tracciaElaborazioneService;

    public AnagraficaRepositoryImpl(DSLContext dsl, FonteRepository fonteRepository, EsenzioneRepository esenzioneRepository, 
    		TracciaElaborazioneService tracciaElaborazioneService, TracciaElaborazioneRepository tracciaElaborazioneRepository) {
        this.dsl = dsl;
        this.fonteRepository = fonteRepository;
        this.esenzioneRepository = esenzioneRepository;
        this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
    }


//    @Override
//    public List<AnagraficaDTO> findListaAnagrafica(List<Integer> filtroFonteId,
//                                                   List<String> descrizioniStato,
//                                                   List<String> sezione,
//                                                   Boolean insInSorveglianza,
//                                                   String tipoElencoInail,
//                                                   List<Integer> assistenzaAslId) {
//
//        // 1) condizioni dinamiche
//        Condition condDescrizioneStato = noCondition();
//        if (descrizioniStato != null && !descrizioniStato.isEmpty()) {
//            condDescrizioneStato = REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.in(descrizioniStato);
//        }
//
//        Condition condSezione = noCondition();
//        if (sezione != null && !sezione.isEmpty()) {
//            condSezione = REEA_T_REGISTRO.SEZIONE.in(sezione);
//        }
//
//        Condition condInseritoInSorveglianza = noCondition();
//        if (insInSorveglianza != null) {
//            condInseritoInSorveglianza = REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.eq(insInSorveglianza);
//        }
//
//        Condition condTipoElencoInail = noCondition();
//        if (tipoElencoInail != null && !tipoElencoInail.isEmpty()) {
//            condTipoElencoInail = REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(tipoElencoInail.toUpperCase());
//        }
//
//        // Filtro assistenza_asl_id â due condizioni separate per le due tabelle
//        Condition condAssistenzaAsl = noCondition();
//        Condition condAssistenzaAslStorico = noCondition();
//        if (assistenzaAslId != null && !assistenzaAslId.isEmpty()) {
//            condAssistenzaAsl        = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//            condAssistenzaAslStorico = REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//        }
//
//        Condition condGlobale = condDescrizioneStato
//                .and(condSezione)
//                .and(condInseritoInSorveglianza)
//                .and(condTipoElencoInail)
//                .and(condAssistenzaAsl);
//
//        Condition condGlobaleStorico = condDescrizioneStato
//                .and(condSezione)
//                .and(condInseritoInSorveglianza)
//                .and(condTipoElencoInail)
//                .and(condAssistenzaAslStorico);
//
//        // 2) fonte
//        Condition condFonteGlobale = noCondition();
//        if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
//            condFonteGlobale = REEA_T_SOGGETTO.FONTE_ID.in(filtroFonteId)
//                .or(REEA_T_SOGGETTO.SOGGETTO_ID.in(
//                    dsl.selectDistinct(REEA_S_SOGGETTO.SOGGETTO_ID)
//                        .from(REEA_S_SOGGETTO)
//                        .where(REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId))
//                ));
//        }
//
//        // 3) query con UNION ALL
//        var query = dsl
//            .selectDistinct(
//                REEA_D_ASL.ASL_AZIENDA_DESC,
//                REEA_T_SOGGETTO.FONTE_ID,
//                REEA_D_FONTE.FONTE_DESC,
//                REEA_T_SOGGETTO.DATA_MODIFICA,
//                REEA_T_SOGGETTO.DATA_CREAZIONE,
//                REEA_T_SOGGETTO.NASCITA_DATA,
//                REEA_T_SOGGETTO.SOGGETTO_ID,
//                REEA_T_SOGGETTO.VERSIONE_NUMERO,
//                REEA_T_ADESIONE.ADESIONE_DATA,
//                REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA,
//
//                DSL.when(REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
//                        field("pgp_sym_decrypt({0}, {1})", String.class,
//                            REEA_T_SOGGETTO.NOME_CIFRATO,
//                            val("16<odcc8!"))
//                    ).otherwise((String) null).as("NOME_CHIARO"),
//                DSL.when(REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//                        field("pgp_sym_decrypt({0}, {1})", String.class,
//                            REEA_T_SOGGETTO.COGNOME_CIFRATO,
//                            val("16<odcc8!"))
//                    ).otherwise((String) null).as("COGNOME_CHIARO"),
//
//                REEA_T_SOGGETTO.SESSO,
//                REEA_T_SOGGETTO.CODICE_FISCALE,
//                REEA_T_SOGGETTO.DOMICILIO_ASL_ID,
//                REEA_T_SOGGETTO.RESIDENZA_ASL_ID,
//                REEA_T_SOGGETTO.ASSISTENZA_ASL_ID,
//                REEA_T_REGISTRO.SEZIONE,
//                REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA,
//                REEA_T_REGISTRO.TIPO_ELENCO_INAIL,
//                REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE,
//                REEA_T_REGISTRO.REGISTRO_ID,
//                REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC,
//                	DSL.when(REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
//                        field("pgp_sym_decrypt({0}, {1})", String.class,
//                            REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO,
//                            val("16<odcc8!"))
//                    ).otherwise((String) null).as("TELEFONO_AURA"),
//                    DSL.when(REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
//                            field("pgp_sym_decrypt({0}, {1})", String.class,
//                                REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA,
//                                val("16<odcc8!"))
//                        ).otherwise((String) null).as("EMAIL_AURA"),
//                    DSL.field(
//                            DSL.select(REEA_S_SOGGETTO.SOGGETTO_STATO_ID)
//                                    .from(REEA_S_SOGGETTO)
//                                    .where(REEA_S_SOGGETTO.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
//                                    .and(REEA_S_SOGGETTO.DATA_CANCELLAZIONE.isNull())
//                                    .orderBy(REEA_S_SOGGETTO.DATA_MODIFICA.desc())
//                                    .limit(1)
//                    ).as("SOGGETTO_STATO_PRECEDENTE_ID")
//            )
//            .from(REEA_T_SOGGETTO)
//            .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//            .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                field("reea.hmac_soggetto_id({0}, {1})",
//                    String.class,
//                    REEA_T_SOGGETTO.SOGGETTO_ID,
//                    val("16<odcc8!"))))
//            .join(REEA_D_SOGGETTO_STATO)
//            .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//            .leftJoin(REEA_T_ADESIONE)
//            .on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID)
//                .and(REEA_T_ADESIONE.SOGGETTO_ID.isNotNull()))
//            .where(condGlobale.and(condFonteGlobale))
//
//            .unionAll(
//
//                dsl.selectDistinct(
//                    REEA_D_ASL.ASL_AZIENDA_DESC,
//                    REEA_S_SOGGETTO.FONTE_ID,
//                    REEA_D_FONTE.FONTE_DESC,
//                    REEA_S_SOGGETTO.DATA_MODIFICA,
//                    REEA_S_SOGGETTO.DATA_CREAZIONE,
//                    REEA_S_SOGGETTO.NASCITA_DATA,
//                    REEA_S_SOGGETTO.SOGGETTO_ID,
//                    REEA_S_SOGGETTO.VERSIONE_NUMERO,
//
//
//                    DSL.val((LocalDateTime) null).as("ADESIONE_DATA"),
//                    DSL.val((LocalDateTime) null).as("PRESENTAZIONE_ISTANZA_DATA"),
//
//                    DSL.when(
//                    	    REEA_S_SOGGETTO.NOME_CIFRATO.isNotNull(),
//                    	    DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                    	        REEA_S_SOGGETTO.NOME_CIFRATO,
//                    	        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//                    	).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("NOME_CHIARO"),
//
//                    DSL.when(
//                    	    REEA_S_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//                    	    DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                    	        REEA_S_SOGGETTO.COGNOME_CIFRATO,
//                    	        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//                    	).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("COGNOME_CHIARO"),
//
//                    REEA_S_SOGGETTO.SESSO,
//                    REEA_S_SOGGETTO.CODICE_FISCALE,
//                    REEA_S_SOGGETTO.DOMICILIO_ASL_ID,
//                    REEA_S_SOGGETTO.RESIDENZA_ASL_ID,
//                    REEA_S_SOGGETTO.ASSISTENZA_ASL_ID,
//                    REEA_T_REGISTRO.SEZIONE,
//                    REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA,
//                    REEA_T_REGISTRO.TIPO_ELENCO_INAIL,
//                    REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE,
//                    REEA_T_REGISTRO.REGISTRO_ID,
//                    REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC,
//                                DSL.val((String) null).as("TELEFONO_AURA"),
//                                DSL.val((String) null).as("EMAIL_AURA"),
//                                DSL.val((Integer) null).as("SOGGETTO_STATO_PRECEDENTE_ID")
//                )
//                .from(REEA_S_SOGGETTO)
//                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
//                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID))
//                .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                    field("reea.hmac_soggetto_id({0}, {1})",
//                        String.class,
//                        REEA_S_SOGGETTO.SOGGETTO_ID,
//                        val("16<odcc8!"))))
//                .join(REEA_D_SOGGETTO_STATO)
//                .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_S_SOGGETTO.SOGGETTO_STATO_ID))
//                .where(condGlobaleStorico)
//                .and(REEA_S_SOGGETTO.SOGGETTO_ID.notIn(
//                                dsl.selectDistinct(REEA_T_SOGGETTO.SOGGETTO_ID)
//
//                                        .from(REEA_T_SOGGETTO)
//                        ))
//            );
//
//        // 4) fetch e mapping null safe
//        return query
//            .stream()
//            .map(r -> {
//
//                AnagraficaDTO row = new AnagraficaDTO();
//
//                // SOGGETTO_ID da T o da S
//                Integer soggettoIdInt = r.get(REEA_T_SOGGETTO.SOGGETTO_ID);
//                if (soggettoIdInt == null) {
//                    soggettoIdInt = r.get(REEA_S_SOGGETTO.SOGGETTO_ID);
//                }
//                Long soggettoId = soggettoIdInt != null ? soggettoIdInt.longValue() : null;
//                row.setSoggettoId(soggettoId);
//
//                // FONTE_ID da T o da S
//                Integer fonteId = r.get(REEA_T_SOGGETTO.FONTE_ID);
//                if (fonteId == null) {
//                    fonteId = r.get(REEA_S_SOGGETTO.FONTE_ID);
//                }
//                row.setFonteId(fonteId);
//
//                // Descrizione fonte
//                String fonteDesc = r.get(REEA_D_FONTE.FONTE_DESC);
//                row.setDescrizioneFonte(
//                    List.of(fonteDesc != null ? fonteDesc : "PROVA")
//                );
//
//                // RegistroDTO sempre presente
//                RegistroDTO registro = RegistroUtils.ensureRegistro(row);
//
//                // DATA_MODIFICA
//                LocalDateTime dataModifica = r.get(REEA_T_SOGGETTO.DATA_MODIFICA);
//                if (dataModifica == null) {
//                    dataModifica = r.get(REEA_S_SOGGETTO.DATA_MODIFICA);
//                }
//                registro.setDataModifica(
//                    DateConversionUtils.toOffsetDateTime(dataModifica)
//                );
//
//                // DATA_CREAZIONE
//                LocalDateTime dataCreazione = r.get(REEA_T_SOGGETTO.DATA_CREAZIONE);
//                if (dataCreazione == null) {
//                    dataCreazione = r.get(REEA_S_SOGGETTO.DATA_CREAZIONE);
//                }
//                registro.setDataCreazione(
//                    DateConversionUtils.toOffsetDateTime(dataCreazione)
//                );
//
//                // VERSIONE_NUMERO
//                Integer versioneNumero = r.get(REEA_T_SOGGETTO.VERSIONE_NUMERO);
//                if (versioneNumero == null) {
//                    versioneNumero = r.get(REEA_S_SOGGETTO.VERSIONE_NUMERO);
//                }
//                registro.setVersioneNumero(versioneNumero);
//
//                // REGISTRO_ID / SEZIONE / SORVEGLIANZA / ELENCO INAIL
//                registro.setRegistroId(r.get(REEA_T_REGISTRO.REGISTRO_ID));
//                registro.setSezione(r.get(REEA_T_REGISTRO.SEZIONE));
//                registro.setInseritoInSorveglianza(r.get(REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA));
//                registro.setTipoElencoInail(r.get(REEA_T_REGISTRO.TIPO_ELENCO_INAIL));
//
//                // Nascita
//                LocalDate nascitaData = r.get(REEA_T_SOGGETTO.NASCITA_DATA);
//                if (nascitaData == null) {
//                    nascitaData = r.get(REEA_S_SOGGETTO.NASCITA_DATA);
//                }
//                row.setNascitaData(nascitaData);
//
//                // Nome/cognome decrypt null safe
//                String nomeChiaro = r.get("NOME_CHIARO", String.class);
//                row.setNome(nomeChiaro != null ? nomeChiaro.toUpperCase() : "" );
//
//                String cognomeChiaro = r.get("COGNOME_CHIARO", String.class);
//                row.setCognome(cognomeChiaro != null ? cognomeChiaro.toUpperCase() : "" );
//
//                // Sesso / CF
//                String sesso = r.get(REEA_T_SOGGETTO.SESSO);
//                if (sesso == null) {
//                    sesso = r.get(REEA_S_SOGGETTO.SESSO);
//                }
//                row.setSesso(sesso);
//
//                String codiceFiscale = r.get(REEA_T_SOGGETTO.CODICE_FISCALE);
//                if (codiceFiscale == null) {
//                    codiceFiscale = r.get(REEA_S_SOGGETTO.CODICE_FISCALE);
//                }
//                row.setCodiceFiscale(codiceFiscale);
//
//                // ASL residenza/domicilio/assistenza
//                Integer resAsl = r.get(REEA_T_SOGGETTO.RESIDENZA_ASL_ID);
//                if (resAsl == null) {
//                    resAsl = r.get(REEA_S_SOGGETTO.RESIDENZA_ASL_ID);
//                }
//                row.setResidenzaAslId(resAsl);
//
//                Integer domAsl = r.get(REEA_T_SOGGETTO.DOMICILIO_ASL_ID);
//                if (domAsl == null) {
//                    domAsl = r.get(REEA_S_SOGGETTO.DOMICILIO_ASL_ID);
//                }
//                row.setDomicilioAslId(domAsl);
//
//                Integer assistenzaId = r.get(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID);
//                if (assistenzaId == null) {
//                    assistenzaId = r.get(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID);
//                }
//                row.setAssistenzaAslId(
//                    assistenzaId != null ? String.format("%06d", assistenzaId) : null
//                );
//
//                row.setDescrizioneAslCompetenza(r.get(REEA_D_ASL.ASL_AZIENDA_DESC));
//
//                row.setTelefonoAura(r.get("TELEFONO_AURA", String.class));
//                row.setEmailAura(r.get("EMAIL_AURA", String.class));
//
//                // Stato / note
//                row.setDescrizioneStato(r.get(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC));
//                String statoNote = r.get(REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE);
//                if (statoNote == null) {
//                    statoNote = r.get(REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE);
//                }
//                row.setSoggettoStatoNote(statoNote);
//
//                // Stato precedente (per filtro ruolo SPRESAL/EPI)
//                Integer statoPrecedenteId = r.get("SOGGETTO_STATO_PRECEDENTE_ID", Integer.class);
//                row.setSoggettoStatoPrecedenteId(statoPrecedenteId);
//
//
//                // Liste fonte/esenzioni (solo se ho l'id)
//                if (soggettoId != null) {
//                    row.setListaFonteByIdSoggetto(
//                        fonteRepository != null
//                            ? fonteRepository.findListaFonteDescByIdSoggetto(soggettoId)
//                            : Collections.emptyList()
//                    );
//
//                    row.setListaEsenzione(
//                        esenzioneRepository != null
//                            ? esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId)
//                            : Collections.emptyList()
//                    );
//                } else {
//                    row.setListaFonteByIdSoggetto(Collections.emptyList());
//                    row.setListaEsenzione(Collections.emptyList());
//                }
//
//                // Adesione (DTO non nullo)
////                if (row.getAdesione() == null) {
////                    row.setAdesione(new AdesioneDTO());
////                }
//                LocalDateTime adesioneData = r.get(REEA_T_ADESIONE.ADESIONE_DATA);
//                row.setAdesioneData(adesioneData != null ? adesioneData.toString() : null);
//
//                // Presentazione istanza
//                LocalDateTime presentazione = r.get(REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA);
//                row.setPresentazioneIstanzaData(
//                    presentazione != null ? presentazione.toLocalDate().toString() : null
//                );
//
//                return row;
//            })
//            .collect(Collectors.toList());
//    }
    
    private Field<byte[]> buildHmacField(String value) {
        return DSL.field(
                "hmac({0}, {1}, {2})",
                SQLDataType.BLOB,
                DSL.val(value, SQLDataType.VARCHAR),
                DSL.val(
                        "Porcatrota2000!",
                        SQLDataType.VARCHAR
                ),
                DSL.val(
                        "sha256",
                        SQLDataType.VARCHAR
                )
        );
    }
    
    //versione 17/09/2026 ore 12:20
    private List<AnagraficaDTO> findListaAnagraficaFastConNominativo(
            List<Integer> filtroFonteId,
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

        /*
         * Normalizzazione filtro fonte.
         */
        if (filtroFonteId == null || filtroFonteId.isEmpty()) {
            filtroFonteId = List.of(1, 2, 3, 4, 5);
        } else {
            filtroFonteId = new ArrayList<>(filtroFonteId);
        }

//        /*
//         * Regola applicativa esistente.
//         */
//        if (filtroFonteId.contains(2)) {
//            azzeraContatoreProssimoStep = Boolean.TRUE;
//        }

        Set<String> profiliCorrente = Set.of(
                "REEA_OP_SPRESAL",
                "REEA_OP_CRPT",
                "REEA_OP_CSI",
                "REEA_OP_CRPT_PSEUDO",
                "REEA_OP_SPRESAL_PSEUDO",
                "REEA_OP_EPI_PSEUDO",
                "REEA_OP_CSI_PSEUDO"
        );

        Set<String> profiliStorico = Set.of(
                "REEA_OP_SPRESAL_PSEUDO",
                "REEA_OP_EPI_PSEUDO",
                "REEA_OP_SPRESAL",
                "REEA_OP_CSI_PSEUDO"
        );

        boolean includeCorrente =
                profiliCorrente.contains(profiloUtente);

        boolean includeStorico =
                profiliStorico.contains(profiloUtente);

        if (!includeCorrente) {
            return Collections.emptyList();
        }

        /*
         * Condizioni comuni.
         */
        Condition condDescrizioneStato = noCondition();

        if (descrizioniStato != null && !descrizioniStato.isEmpty()) {
            condDescrizioneStato =
                    REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
                            .in(descrizioniStato);
        }

        Condition condSezione = noCondition();

        if (sezione != null && !sezione.isEmpty()) {
            condSezione =
                    REEA_T_REGISTRO.SEZIONE.in(sezione);
        }

        Condition condInseritoInSorveglianza = noCondition();

        if (insInSorveglianza != null) {
            condInseritoInSorveglianza =
                    REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA
                            .eq(insInSorveglianza);
        }

        Condition condTipoElencoInail = noCondition();

        if (tipoElencoInail != null && !tipoElencoInail.isBlank()) {
            condTipoElencoInail =
                    REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(
                            tipoElencoInail
                                    .trim()
                                    .toUpperCase(Locale.ROOT)
                    );
        }

        Condition condAssistenzaAsl = noCondition();
        Condition condAssistenzaAslStorico = noCondition();

        if (assistenzaAslId != null && !assistenzaAslId.isEmpty()) {
            condAssistenzaAsl =
                    REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);

            condAssistenzaAslStorico =
                    REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
        }

        Condition condCodiceFiscaleT = noCondition();
        Condition condCodiceFiscaleS = noCondition();

        if (codiceFiscale != null && !codiceFiscale.isBlank()) {
            String codiceFiscaleNormalizzato =
                    codiceFiscale.trim();

            condCodiceFiscaleT =
                    REEA_T_SOGGETTO.CODICE_FISCALE
                            .equalIgnoreCase(codiceFiscaleNormalizzato);

            condCodiceFiscaleS =
                    REEA_S_SOGGETTO.CODICE_FISCALE
                            .equalIgnoreCase(codiceFiscaleNormalizzato);
        }

        Condition condNascitaDataT = noCondition();
        Condition condNascitaDataS = noCondition();

        if (nascitaData != null) {
            condNascitaDataT =
                    REEA_T_SOGGETTO.NASCITA_DATA.eq(nascitaData);

            condNascitaDataS =
                    REEA_S_SOGGETTO.NASCITA_DATA.eq(nascitaData);
        }

        Condition condRegistroIdPaging = noCondition();

        if (Boolean.FALSE.equals(azzeraContatoreProssimoStep)) {
            Integer lastRegistroId =
                    getUltimoRegistroIdProcessato(auditLogRequest);

            if (lastRegistroId != null) {
                condRegistroIdPaging =
                        REEA_T_REGISTRO.REGISTRO_ID.gt(lastRegistroId);
            }
        }

        Condition condGlobale =
                condDescrizioneStato
                        .and(condSezione)
                        .and(condInseritoInSorveglianza)
                        .and(condTipoElencoInail)
                        .and(condAssistenzaAsl)
                        .and(condCodiceFiscaleT)
                        .and(condNascitaDataT)
                        .and(condRegistroIdPaging);

        Condition condGlobaleStorico =
                condDescrizioneStato
                        .and(condSezione)
                        .and(condInseritoInSorveglianza)
                        .and(condTipoElencoInail)
                        .and(condAssistenzaAslStorico)
                        .and(condCodiceFiscaleS)
                        .and(condNascitaDataS);

        /*
         * Condizione fonte corrente.
         *
         * EXISTS evita di moltiplicare le righe correnti.
         */
        Condition condFonteGlobaleT =
                REEA_T_SOGGETTO.FONTE_ID.in(filtroFonteId)
                        .orExists(
                                dsl.selectOne()
                                        .from(REEA_S_SOGGETTO)
                                        .where(
                                                REEA_S_SOGGETTO.SOGGETTO_ID
                                                        .eq(
                                                                REEA_T_SOGGETTO
                                                                        .SOGGETTO_ID
                                                        )
                                        )
                                        .and(
                                                REEA_S_SOGGETTO.FONTE_ID
                                                        .in(filtroFonteId)
                                        )
                        );

        Condition condFonteGlobaleS =
                REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId);

        /*
         * Query ID corrente.
         */
        Select<Record1<Integer>> selectIdsCorrente =
                dsl.selectDistinct(
                        REEA_T_SOGGETTO.SOGGETTO_ID
                                .as("SOGGETTO_ID")
                )
                .from(REEA_T_SOGGETTO)
                .join(REEA_D_FONTE)
                    .on(
                            REEA_D_FONTE.FONTE_ID.eq(
                                    REEA_T_SOGGETTO.FONTE_ID
                            )
                    )
                .leftJoin(REEA_D_ASL)
                    .on(
                            REEA_D_ASL.ASL_ID.eq(
                                    REEA_T_SOGGETTO.ASSISTENZA_ASL_ID
                            )
                    )
                .join(REEA_T_REGISTRO)
                    .on(
                            REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                                    field(
                                            "reea.hmac_soggetto_id({0}, {1})",
                                            String.class,
                                            REEA_T_SOGGETTO.SOGGETTO_ID,
                                            val("16<odcc8!")
                                    )
                            )
                    )
                .join(REEA_D_SOGGETTO_STATO)
                    .on(
                            REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(
                                    REEA_T_SOGGETTO.SOGGETTO_STATO_ID
                            )
                    )
                .where(
                        condGlobale
                                .and(condFonteGlobaleT)
                );

        /*
         * Query ID storico.
         */
        Select<Record1<Integer>> selectIdsStorico = null;

        if (includeStorico) {
            selectIdsStorico =
                    dsl.selectDistinct(
                            REEA_S_SOGGETTO.SOGGETTO_ID
                                    .as("SOGGETTO_ID")
                    )
                    .from(REEA_S_SOGGETTO)
                    .join(REEA_D_FONTE)
                        .on(
                                REEA_D_FONTE.FONTE_ID.eq(
                                        REEA_S_SOGGETTO.FONTE_ID
                                )
                        )
                    .leftJoin(REEA_D_ASL)
                        .on(
                                REEA_D_ASL.ASL_ID.eq(
                                        REEA_S_SOGGETTO.ASSISTENZA_ASL_ID
                                )
                        )
                    .join(REEA_T_REGISTRO)
                        .on(
                                REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                                        field(
                                                "reea.hmac_soggetto_id({0}, {1})",
                                                String.class,
                                                REEA_S_SOGGETTO.SOGGETTO_ID,
                                                val("16<odcc8!")
                                        )
                                )
                        )
                    .join(REEA_D_SOGGETTO_STATO)
                        .on(
                                REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(
                                        REEA_S_SOGGETTO.SOGGETTO_STATO_ID
                                )
                        )
                    .where(
                            condGlobaleStorico
                                    .and(condFonteGlobaleS)
                                    .and(
                                            REEA_S_SOGGETTO.SOGGETTO_ID
                                                    .notIn(
                                                            dsl.selectDistinct(
                                                                    REEA_T_SOGGETTO
                                                                            .SOGGETTO_ID
                                                            )
                                                            .from(
                                                                    REEA_T_SOGGETTO
                                                            )
                                                    )
                                    )
                    );
        }

        /*
         * Unione degli ID.
         *
         * UNION garantisce un solo ID per soggetto.
         */
        Table<?> idsTable;

        if (selectIdsStorico != null) {
            idsTable =
                    selectIdsCorrente
                            .union(selectIdsStorico)
                            .asTable("ids");
        } else {
            idsTable =
                    selectIdsCorrente.asTable("ids");
        }

        Field<Integer> idField =
                idsTable.field(
                        "SOGGETTO_ID",
                        Integer.class
                );

        if (idField == null) {
            return Collections.emptyList();
        }

        List<Integer> ids =
                dsl.select(idField)
                        .from(idsTable)
                        .where(idField.isNotNull())
                        .fetch(idField)
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(
                                id -> isValidoPerExport(
                                        id,
                                        profiloUtente
                                )
                        )
                        .distinct()
                        .toList();

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        /*
         * Query dettaglio corrente.
         *
         * Gli hash vengono restituiti per il filtro nominativo esterno.
         */
        Select<Record> selectCorrente =
                dsl.select(
                        REEA_D_ASL.ASL_AZIENDA_DESC
                                .as("ASL_AZIENDA_DESC"),
                        REEA_T_SOGGETTO.FONTE_ID
                                .as("FONTE_ID"),
                        REEA_D_FONTE.FONTE_DESC
                                .as("FONTE_DESC"),
                        REEA_T_SOGGETTO.DATA_MODIFICA
                                .as("DATA_MODIFICA"),
                        REEA_T_SOGGETTO.DATA_CREAZIONE
                                .as("DATA_CREAZIONE"),
                        REEA_T_SOGGETTO.NASCITA_DATA
                                .as("NASCITA_DATA"),
                        REEA_T_SOGGETTO.SOGGETTO_ID
                                .as("SOGGETTO_ID"),
                        REEA_T_SOGGETTO.VERSIONE_NUMERO
                                .as("VERSIONE_NUMERO"),
                        REEA_T_ADESIONE.ADESIONE_DATA
                                .as("ADESIONE_DATA"),
                        REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA
                                .as("PRESENTAZIONE_ISTANZA_DATA"),

                        field(
                                "pgp_sym_decrypt({0}, {1})",
                                String.class,
                                REEA_T_SOGGETTO.NOME_CIFRATO,
                                val("16<odcc8!")
                        ).as("NOME_CHIARO"),

                        field(
                                "pgp_sym_decrypt({0}, {1})",
                                String.class,
                                REEA_T_SOGGETTO.COGNOME_CIFRATO,
                                val("16<odcc8!")
                        ).as("COGNOME_CHIARO"),

                        REEA_T_SOGGETTO.SESSO
                                .as("SESSO"),
                        REEA_T_SOGGETTO.CODICE_FISCALE
                                .as("CODICE_FISCALE"),
                        REEA_T_SOGGETTO.DOMICILIO_ASL_ID
                                .as("DOMICILIO_ASL_ID"),
                        REEA_T_SOGGETTO.RESIDENZA_ASL_ID
                                .as("RESIDENZA_ASL_ID"),
                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID
                                .as("ASSISTENZA_ASL_ID"),
                        REEA_T_REGISTRO.SEZIONE
                                .as("SEZIONE"),
                        REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA
                                .as("INSERITO_IN_SORVEGLIANZA"),
                        REEA_T_REGISTRO.TIPO_ELENCO_INAIL
                                .as("TIPO_ELENCO_INAIL"),
                        REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE
                                .as("SOGGETTO_STATO_NOTE"),
                        REEA_T_REGISTRO.REGISTRO_ID
                                .as("REGISTRO_ID"),
                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
                                .as("SOGGETTO_STATO_DESC"),
                        DSL.inline((String) null)
                                .as("TELEFONO_AURA"),
                        DSL.inline((String) null)
                                .as("EMAIL_AURA"),
                        DSL.field(
                                DSL.select(
                                        REEA_S_SOGGETTO
                                                .SOGGETTO_STATO_ID
                                )
                                .from(REEA_S_SOGGETTO)
                                .where(
                                        REEA_S_SOGGETTO.SOGGETTO_ID.eq(
                                                REEA_T_SOGGETTO.SOGGETTO_ID
                                        )
                                )
                                .and(
                                        REEA_S_SOGGETTO.DATA_CANCELLAZIONE
                                                .isNull()
                                )
                                .orderBy(
                                        REEA_S_SOGGETTO.DATA_MODIFICA
                                                .desc()
                                )
                                .limit(1)
                        ).as("SOGGETTO_STATO_PRECEDENTE_ID"),

                        REEA_T_SOGGETTO.COGNOME_HASH_1CHAR
                                .as("COGNOME_HASH_1CHAR"),
                        REEA_T_SOGGETTO.COGNOME_HASH_2CHAR
                                .as("COGNOME_HASH_2CHAR"),
                        REEA_T_SOGGETTO.COGNOME_HASH
                                .as("COGNOME_HASH"),
                        REEA_T_SOGGETTO.NOME_HASH_1CHAR
                                .as("NOME_HASH_1CHAR"),
                        REEA_T_SOGGETTO.NOME_HASH_2CHAR
                                .as("NOME_HASH_2CHAR"),
                        REEA_T_SOGGETTO.NOME_HASH
                                .as("NOME_HASH")
                )
                .from(REEA_T_SOGGETTO)
                .join(idsTable)
                    .on(
                            idField.eq(
                                    REEA_T_SOGGETTO.SOGGETTO_ID
                            )
                    )
                .leftJoin(REEA_D_FONTE)
                    .on(
                            REEA_D_FONTE.FONTE_ID.eq(
                                    REEA_T_SOGGETTO.FONTE_ID
                            )
                    )
                .leftJoin(REEA_D_ASL)
                    .on(
                            REEA_D_ASL.ASL_ID.eq(
                                    REEA_T_SOGGETTO.ASSISTENZA_ASL_ID
                            )
                    )
                .join(REEA_T_REGISTRO)
                    .on(
                            REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                                    field(
                                            "reea.hmac_soggetto_id({0}, {1})",
                                            String.class,
                                            REEA_T_SOGGETTO.SOGGETTO_ID,
                                            val("16<odcc8!")
                                    )
                            )
                    )
                .leftJoin(REEA_D_SOGGETTO_STATO)
                    .on(
                            REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(
                                    REEA_T_SOGGETTO.SOGGETTO_STATO_ID
                            )
                    )
                .leftJoin(REEA_T_ADESIONE)
                    .on(
                            REEA_T_ADESIONE.SOGGETTO_ID.eq(
                                    REEA_T_SOGGETTO.SOGGETTO_ID
                            )
                    )
                .where(
                        REEA_T_SOGGETTO.SOGGETTO_ID.in(ids)
                );

        /*
         * Query dettaglio storico.
         */
        Select<Record> selectStorico = null;

        if (includeStorico) {
            selectStorico =
                    dsl.select(
                            DSL.castNull(
                                    REEA_D_ASL.ASL_AZIENDA_DESC
                                            .getDataType()
                            ).as("ASL_AZIENDA_DESC"),
                            REEA_S_SOGGETTO.FONTE_ID
                                    .as("FONTE_ID"),
                            REEA_D_FONTE.FONTE_DESC
                                    .as("FONTE_DESC"),
                            REEA_S_SOGGETTO.DATA_MODIFICA
                                    .as("DATA_MODIFICA"),
                            REEA_S_SOGGETTO.DATA_CREAZIONE
                                    .as("DATA_CREAZIONE"),
                            REEA_S_SOGGETTO.NASCITA_DATA
                                    .as("NASCITA_DATA"),
                            REEA_S_SOGGETTO.SOGGETTO_ID
                                    .as("SOGGETTO_ID"),
                            REEA_S_SOGGETTO.VERSIONE_NUMERO
                                    .as("VERSIONE_NUMERO"),
                            DSL.val((LocalDateTime) null)
                                    .as("ADESIONE_DATA"),
                            DSL.val((LocalDateTime) null)
                                    .as("PRESENTAZIONE_ISTANZA_DATA"),
                            DSL.val((String) null)
                                    .as("NOME_CHIARO"),
                            DSL.val((String) null)
                                    .as("COGNOME_CHIARO"),
                            DSL.val((String) null)
                                    .as("SESSO"),
                            REEA_S_SOGGETTO.CODICE_FISCALE
                                    .as("CODICE_FISCALE"),
                            REEA_S_SOGGETTO.DOMICILIO_ASL_ID
                                    .as("DOMICILIO_ASL_ID"),
                            REEA_S_SOGGETTO.RESIDENZA_ASL_ID
                                    .as("RESIDENZA_ASL_ID"),
                            REEA_S_SOGGETTO.ASSISTENZA_ASL_ID
                                    .as("ASSISTENZA_ASL_ID"),
                            REEA_T_REGISTRO.SEZIONE
                                    .as("SEZIONE"),
                            REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA
                                    .as("INSERITO_IN_SORVEGLIANZA"),
                            REEA_T_REGISTRO.TIPO_ELENCO_INAIL
                                    .as("TIPO_ELENCO_INAIL"),
                            REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE
                                    .as("SOGGETTO_STATO_NOTE"),
                            REEA_T_REGISTRO.REGISTRO_ID
                                    .as("REGISTRO_ID"),
                            REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
                                    .as("SOGGETTO_STATO_DESC"),
                            DSL.val((String) null)
                                    .as("TELEFONO_AURA"),
                            DSL.val((String) null)
                                    .as("EMAIL_AURA"),
                            DSL.val((Integer) null)
                                    .as("SOGGETTO_STATO_PRECEDENTE_ID"),

                            REEA_S_SOGGETTO.COGNOME_HASH_1CHAR
                                    .as("COGNOME_HASH_1CHAR"),
                            REEA_S_SOGGETTO.COGNOME_HASH_2CHAR
                                    .as("COGNOME_HASH_2CHAR"),
                            REEA_S_SOGGETTO.COGNOME_HASH
                                    .as("COGNOME_HASH"),
                            REEA_S_SOGGETTO.NOME_HASH_1CHAR
                                    .as("NOME_HASH_1CHAR"),
                            REEA_S_SOGGETTO.NOME_HASH_2CHAR
                                    .as("NOME_HASH_2CHAR"),
                            REEA_S_SOGGETTO.NOME_HASH
                                    .as("NOME_HASH")
                    )
                    .from(REEA_S_SOGGETTO)
                    .join(idsTable)
                        .on(
                                idField.eq(
                                        REEA_S_SOGGETTO.SOGGETTO_ID
                                )
                        )
                    .join(REEA_D_FONTE)
                        .on(
                                REEA_D_FONTE.FONTE_ID.eq(
                                        REEA_S_SOGGETTO.FONTE_ID
                                )
                        )
                    .leftJoin(REEA_D_ASL)
                        .on(
                                REEA_D_ASL.ASL_ID.eq(
                                        REEA_S_SOGGETTO.ASSISTENZA_ASL_ID
                                )
                        )
                    .join(REEA_T_REGISTRO)
                        .on(
                                REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                                        field(
                                                "reea.hmac_soggetto_id({0}, {1})",
                                                String.class,
                                                REEA_S_SOGGETTO.SOGGETTO_ID,
                                                val("16<odcc8!")
                                        )
                                )
                        )
                    .join(REEA_D_SOGGETTO_STATO)
                        .on(
                                REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(
                                        REEA_S_SOGGETTO.SOGGETTO_STATO_ID
                                )
                        )
                    .where(
                            REEA_S_SOGGETTO.SOGGETTO_ID.in(ids)
                    );
        }

        /*
         * UNION dei dettagli.
         *
         * UNION garantisce un solo risultato per righe identiche.
         */
        Table<?> unionTable;

        if (selectStorico != null) {
            unionTable =
                    selectCorrente
                            .union(selectStorico)
                            .asTable("q");
        } else {
            unionTable =
                    selectCorrente.asTable("q");
        }

        Condition condOuterNome = noCondition();

        if (nome != null && !nome.isBlank()) {
            String valoreNome =
                    nome.trim().toUpperCase(Locale.ROOT);

            Field<byte[]> nomeHmac =
                    buildHmacField(valoreNome);

            Field<byte[]> nomeHashField;

            if (valoreNome.length() == 1) {
                nomeHashField =
                        DSL.field(
                                DSL.name(
                                        "q",
                                        "NOME_HASH_1CHAR"
                                ),
                                byte[].class
                        );
            } else if (valoreNome.length() == 2) {
                nomeHashField =
                        DSL.field(
                                DSL.name(
                                        "q",
                                        "NOME_HASH_2CHAR"
                                ),
                                byte[].class
                        );
            } else {
                nomeHashField =
                        DSL.field(
                                DSL.name(
                                        "q",
                                        "NOME_HASH"
                                ),
                                byte[].class
                        );
            }

            condOuterNome =
                    nomeHashField.eq(nomeHmac);
        }

        Condition condOuterCognome = noCondition();

        if (cognome != null && !cognome.isBlank()) {
            String valoreCognome =
                    cognome.trim().toUpperCase(Locale.ROOT);

            Field<byte[]> cognomeHmac =
                    buildHmacField(valoreCognome);

            Field<byte[]> cognomeHashField;

            if (valoreCognome.length() == 1) {
                cognomeHashField =
                        DSL.field(
                                DSL.name(
                                        "q",
                                        "COGNOME_HASH_1CHAR"
                                ),
                                byte[].class
                        );
            } else if (valoreCognome.length() == 2) {
                cognomeHashField =
                        DSL.field(
                                DSL.name(
                                        "q",
                                        "COGNOME_HASH_2CHAR"
                                ),
                                byte[].class
                        );
            } else {
                cognomeHashField =
                        DSL.field(
                                DSL.name(
                                        "q",
                                        "COGNOME_HASH"
                                ),
                                byte[].class
                        );
            }

            condOuterCognome =
                    cognomeHashField.eq(cognomeHmac);
        }

        /*
         * Filtro intervallo alfabetico.
         *
         * Il filtro viene applicato sui dati decrittografati, perché
         * il range non corrisponde a un singolo HMAC.
         */
        Condition condOuterLettDa = noCondition();

        if (cognomeLettDa != null
                && !cognomeLettDa.isBlank()) {

            condOuterLettDa =
                    DSL.upper(
                            DSL.left(
                                    DSL.field(
                                            DSL.name(
                                                    "q",
                                                    "COGNOME_CHIARO"
                                            ),
                                            String.class
                                    ),
                                    1
                            )
                    ).greaterOrEqual(
                            cognomeLettDa
                                    .trim()
                                    .substring(0, 1)
                                    .toUpperCase(Locale.ROOT)
                    );
        }

        Condition condOuterLettA = noCondition();

        if (cognomeLettA != null
                && !cognomeLettA.isBlank()) {

            condOuterLettA =
                    DSL.upper(
                            DSL.left(
                                    DSL.field(
                                            DSL.name(
                                                    "q",
                                                    "COGNOME_CHIARO"
                                            ),
                                            String.class
                                    ),
                                    1
                            )
                    ).lessOrEqual(
                            cognomeLettA
                                    .trim()
                                    .substring(0, 1)
                                    .toUpperCase(Locale.ROOT)
                    );
        }

        /*
         * Query finale.
         *
         * Il filtro HMAC viene applicato prima del LIMIT.
         */
        SelectConditionStep<?> finalQuery =
                dsl.selectFrom(unionTable)
                        .where(
                                condOuterNome
                                        .and(condOuterCognome)
                                        .and(condOuterLettDa)
                                        .and(condOuterLettA)
                        );

        Integer limiteRecordStepCaricamentoAdesioni =
                Optional.ofNullable(
                        dsl.select(
                                REEA_C_PARAMETRO.PARAMETRO_VALORE
                        )
                        .from(REEA_C_PARAMETRO)
                        .where(
                                REEA_C_PARAMETRO.PARAMETRO_COD.eq(
                                        "NumeroRecordRicercaAdesioni"
                                )
                        )
                        .fetchOneInto(Integer.class)
                ).orElse(1000);

        Integer limiteRecord =
                Optional.ofNullable(
                        dsl.select(
                                REEA_C_PARAMETRO.PARAMETRO_VALORE
                        )
                        .from(REEA_C_PARAMETRO)
                        .where(
                                REEA_C_PARAMETRO.PARAMETRO_COD.eq(
                                        "NumeroRecordRicercaGenerica"
                                )
                        )
                        .fetchOneInto(Integer.class)
                ).orElse(1000);

        int limite =
                azzeraContatoreProssimoStep != null
                        ? limiteRecordStepCaricamentoAdesioni
                        : limiteRecord;

        Result<?> result =
                finalQuery
                        .orderBy(
                                DSL.field(
                                        DSL.name(
                                                "q",
                                                "COGNOME_CHIARO"
                                        ),
                                        String.class
                                ).asc(),
                                DSL.field(
                                        DSL.name(
                                                "q",
                                                "NOME_CHIARO"
                                        ),
                                        String.class
                                ).asc()
                        )
                        .limit(limite)
                        .fetch();

        /*
         * Una sola riga per SOGGETTO_ID.
         */
        Map<Integer, Record> recordUnivoci =
                result.stream()
                        .filter(
                                r -> r.get(
                                        "SOGGETTO_ID",
                                        Integer.class
                                ) != null
                        )
                        .collect(
                                Collectors.toMap(
                                        r -> r.get(
                                                "SOGGETTO_ID",
                                                Integer.class
                                        ),
                                        r -> r,
                                        (record1, record2) -> record1,
                                        LinkedHashMap::new
                                )
                        );

        /*
         * Mapping DTO.
         */
        return recordUnivoci.values()
                .stream()
                .map(r -> {
                    AnagraficaDTO row =
                            new AnagraficaDTO();

                    Integer soggettoIdInt =
                            r.get(
                                    "SOGGETTO_ID",
                                    Integer.class
                            );

                    Long soggettoId =
                            soggettoIdInt != null
                                    ? soggettoIdInt.longValue()
                                    : null;

                    row.setSoggettoId(soggettoId);

                    row.setFonteId(
                            r.get(
                                    "FONTE_ID",
                                    Integer.class
                            )
                    );

                    String fonteDesc =
                            r.get(
                                    "FONTE_DESC",
                                    String.class
                            );

                    row.setDescrizioneFonte(
                            List.of(
                                    fonteDesc != null
                                            ? fonteDesc
                                            : "PROVA"
                            )
                    );

                    RegistroDTO registro =
                            RegistroUtils.ensureRegistro(row);

                    registro.setDataModifica(
                            DateConversionUtils.toOffsetDateTime(
                                    r.get(
                                            "DATA_MODIFICA",
                                            LocalDateTime.class
                                    )
                            )
                    );

                    registro.setDataCreazione(
                            DateConversionUtils.toOffsetDateTime(
                                    r.get(
                                            "DATA_CREAZIONE",
                                            LocalDateTime.class
                                    )
                            )
                    );

                    registro.setVersioneNumero(
                            r.get(
                                    "VERSIONE_NUMERO",
                                    Integer.class
                            )
                    );

                    registro.setRegistroId(
                            r.get(
                                    "REGISTRO_ID",
                                    Integer.class
                            )
                    );

                    registro.setSezione(
                            r.get(
                                    "SEZIONE",
                                    Integer.class
                            )
                    );

                    registro.setInseritoInSorveglianza(
                            r.get(
                                    "INSERITO_IN_SORVEGLIANZA",
                                    Boolean.class
                            )
                    );

                    registro.setTipoElencoInail(
                            r.get(
                                    "TIPO_ELENCO_INAIL",
                                    String.class
                            )
                    );

                    row.setNascitaData(
                            r.get(
                                    "NASCITA_DATA",
                                    LocalDate.class
                            )
                    );

                    String nomeChiaro =
                            r.get(
                                    "NOME_CHIARO",
                                    String.class
                            );

                    row.setNome(
                            nomeChiaro != null
                                    ? nomeChiaro.toUpperCase(
                                            Locale.ROOT
                                    )
                                    : ""
                    );

                    String cognomeChiaro =
                            r.get(
                                    "COGNOME_CHIARO",
                                    String.class
                            );

                    row.setCognome(
                            cognomeChiaro != null
                                    ? cognomeChiaro.toUpperCase(
                                            Locale.ROOT
                                    )
                                    : ""
                    );

                    row.setSesso(
                            r.get(
                                    "SESSO",
                                    String.class
                            )
                    );

                    row.setCodiceFiscale(
                            r.get(
                                    "CODICE_FISCALE",
                                    String.class
                            )
                    );

                    row.setDomicilioAslId(
                            r.get(
                                    "DOMICILIO_ASL_ID",
                                    Integer.class
                            )
                    );

                    row.setResidenzaAslId(
                            r.get(
                                    "RESIDENZA_ASL_ID",
                                    Integer.class
                            )
                    );

                    Integer assistenzaId =
                            r.get(
                                    "ASSISTENZA_ASL_ID",
                                    Integer.class
                            );

                    row.setAssistenzaAslId(
                            assistenzaId != null
                                    ? String.format(
                                            "%06d",
                                            assistenzaId
                                    )
                                    : null
                    );

                    row.setDescrizioneAslCompetenza(
                            r.get(
                                    "ASL_AZIENDA_DESC",
                                    String.class
                            )
                    );

                    row.setTelefonoAura(
                            r.get(
                                    "TELEFONO_AURA",
                                    String.class
                            )
                    );

                    row.setEmailAura(
                            r.get(
                                    "EMAIL_AURA",
                                    String.class
                            )
                    );

                    row.setDescrizioneStato(
                            r.get(
                                    "SOGGETTO_STATO_DESC",
                                    String.class
                            )
                    );

                    row.setSoggettoStatoNote(
                            r.get(
                                    "SOGGETTO_STATO_NOTE",
                                    String.class
                            )
                    );

                    row.setSoggettoStatoPrecedenteId(
                            r.get(
                                    "SOGGETTO_STATO_PRECEDENTE_ID",
                                    Integer.class
                            )
                    );

                    row.setListaEsenzione(
                            Collections.emptyList()
                    );

                    if (soggettoId != null) {
                        row.setListaFonteByIdSoggetto(
                                fonteRepository != null
                                        ? fonteRepository
                                                .findListaFonteDescByIdSoggetto(
                                                        soggettoId
                                                )
                                        : Collections.emptyList()
                        );
                    } else {
                        row.setListaFonteByIdSoggetto(
                                Collections.emptyList()
                        );
                    }

                    LocalDateTime adesioneData =
                            r.get(
                                    "ADESIONE_DATA",
                                    LocalDateTime.class
                            );

                    row.setAdesioneData(
                            adesioneData != null
                                    ? adesioneData.toString()
                                    : null
                    );

                    LocalDateTime presentazione =
                            r.get(
                                    "PRESENTAZIONE_ISTANZA_DATA",
                                    LocalDateTime.class
                            );

                    row.setPresentazioneIstanzaData(
                            presentazione != null
                                    ? presentazione
                                            .toLocalDate()
                                            .toString()
                                    : null
                    );

                    return row;
                })
                .filter(
                        row -> row.getSoggettoId() != null
                )
                .toList();
    }
    
    
    @Override
    public List<AnagraficaDTO> findListaAnagrafica(
            List<Integer> filtroFonteId,
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

        boolean stepAttivo =
                azzeraContatoreProssimoStep != null;

        /*
         * true:
         * nuovo caricamento, il precedente valore del contatore
         * non deve essere utilizzato.
         *
         * false:
         * caricamento successivo, il metodo delegato deve recuperare
         * il valore salvato tramite getUltimoRegistroIdProcessato().
         *
         * null:
         * ricerca normale, senza paging progressivo.
         */
        if (Boolean.TRUE.equals(azzeraContatoreProssimoStep)) {
            /*
             * Non viene chiamato getUltimoRegistroIdProcessato().
             *
             * Il metodo delegato riceve true e quindi non applica
             * la condizione REGISTRO_ID > lastRegistroId.
             */
        }

        boolean ricercaPerNominativo =
                (nome != null && !nome.isBlank())
                || (cognome != null && !cognome.isBlank())
                || (cognomeLettDa != null && !cognomeLettDa.isBlank())
                || (cognomeLettA != null && !cognomeLettA.isBlank());

        /*
         * Ricerca veloce.
         */
        if (!ricercaPerNominativo) {
            return findListaAnagraficaFast(
                    filtroFonteId,
                    descrizioniStato,
                    sezione,
                    insInSorveglianza,
                    tipoElencoInail,
                    assistenzaAslId,
                    codiceFiscale,
                    null,
                    null,
                    nascitaData,
                    azzeraContatoreProssimoStep,
                    profiloUtente,
                    auditLogRequest
            );
        }

        /*
         * Ricerca nominativa.
         */
        return findListaAnagraficaFastConNominativo(
                filtroFonteId,
                descrizioniStato,
                sezione,
                insInSorveglianza,
                tipoElencoInail,
                assistenzaAslId,
                codiceFiscale,
                cognome,
                nome,
                cognomeLettDa,
                cognomeLettA,
                nascitaData,
                azzeraContatoreProssimoStep,
                profiloUtente,
                auditLogRequest
        );
    }
//    Versione ultima del 17/09/2026    
//    public List<AnagraficaDTO> findListaAnagrafica(
//            List<Integer> filtroFonteId,
//            List<String> descrizioniStato,
//            List<String> sezione,
//            Boolean insInSorveglianza,
//            String tipoElencoInail,
//            List<Integer> assistenzaAslId,
//            String codiceFiscale,
//            String cognome,
//            String nome,
//            String cognomeLettDa,
//            String cognomeLettA,
//            LocalDate nascitaData,
//            Boolean azzeraContatoreProssimoStep,
//            String profiloUtente,
//            AuditLogRequest auditLogRequest) {
//
//        boolean ricercaPerNominativo =
//                (nome != null && !nome.isBlank())
//                || (cognome != null && !cognome.isBlank())
//                || (cognomeLettA != null && !cognomeLettA.isBlank())
//                || (cognomeLettDa != null && !cognomeLettDa.isBlank());
//
//        if (!ricercaPerNominativo) {
//            return findListaAnagraficaFast(
//                    filtroFonteId,
//                    descrizioniStato,
//                    sezione,
//                    insInSorveglianza,
//                    tipoElencoInail,
//                    assistenzaAslId,
//                    codiceFiscale,
//                    cognomeLettDa,
//                    cognomeLettA,
//                    nascitaData,
//                    azzeraContatoreProssimoStep,
//                    profiloUtente,
//                    auditLogRequest
//            );
//        }
//
//        boolean stepAttivo = azzeraContatoreProssimoStep != null;
//
//        Condition condDescrizioneStato = noCondition();
//        if (descrizioniStato != null && !descrizioniStato.isEmpty()) {
//            condDescrizioneStato =
//                    REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.in(descrizioniStato);
//        }
//
//        Condition condSezione = noCondition();
//        if (sezione != null && !sezione.isEmpty()) {
//            condSezione = REEA_T_REGISTRO.SEZIONE.in(sezione);
//        }
//
//        Condition condInseritoInSorveglianza = noCondition();
//        if (insInSorveglianza != null) {
//            condInseritoInSorveglianza =
//                    REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.eq(insInSorveglianza);
//        }
//
//        Condition condTipoElencoInail = noCondition();
//        if (tipoElencoInail != null && !tipoElencoInail.isEmpty()) {
//            condTipoElencoInail =
//                    REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(tipoElencoInail.toUpperCase());
//        }
//
//        Condition condAssistenzaAsl = noCondition();
//        Condition condAssistenzaAslStorico = noCondition();
//
//        if (assistenzaAslId != null && !assistenzaAslId.isEmpty()) {
//            condAssistenzaAsl =
//                    REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//
//            condAssistenzaAslStorico =
//                    REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//        }
//
//        Condition condCodiceFiscaleT = noCondition();
//        Condition condCodiceFiscaleS = noCondition();
//
//        if (codiceFiscale != null && !codiceFiscale.isEmpty()) {
//            condCodiceFiscaleT =
//                    REEA_T_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
//
//            condCodiceFiscaleS =
//                    REEA_S_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
//        }
//
//        Condition condNascitaDataT = noCondition();
//        Condition condNascitaDataS = noCondition();
//
//        if (nascitaData != null) {
//            condNascitaDataT =
//                    REEA_T_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//
//            condNascitaDataS =
//                    REEA_S_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//        }
//
//        Condition condRegistroIdPaging = noCondition();
//
//        if (Boolean.FALSE.equals(azzeraContatoreProssimoStep)) {
//            Integer lastRegistroId =
//                    getUltimoRegistroIdProcessato(auditLogRequest);
//
//            if (lastRegistroId != null) {
//                condRegistroIdPaging =
//                        REEA_T_REGISTRO.REGISTRO_ID.gt(lastRegistroId);
//            }
//        }
//
//        Condition condGlobale = condDescrizioneStato
//                .and(condSezione)
//                .and(condInseritoInSorveglianza)
//                .and(condTipoElencoInail)
//                .and(condAssistenzaAsl)
//                .and(condCodiceFiscaleT)
//                .and(condNascitaDataT)
//                .and(condRegistroIdPaging);
//
//        Condition condGlobaleStorico = condDescrizioneStato
//                .and(condSezione)
//                .and(condInseritoInSorveglianza)
//                .and(condTipoElencoInail)
//                .and(condAssistenzaAslStorico)
//                .and(condCodiceFiscaleS)
//                .and(condNascitaDataS);
//
//        Condition condFonteGlobaleT = noCondition();
//
//        if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
//            condFonteGlobaleT =
//                    REEA_T_SOGGETTO.FONTE_ID.in(filtroFonteId)
//                    .or(
//                            REEA_T_SOGGETTO.SOGGETTO_ID.in(
//                                    dsl.selectDistinct(REEA_S_SOGGETTO.SOGGETTO_ID)
//                                            .from(REEA_S_SOGGETTO)
//                                            .where(REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId))
//                            )
//                    );
//        }
//
//        Condition condFonteGlobaleS = noCondition();
//
//        if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
//            condFonteGlobaleS =
//                    REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId);
//        }
//
//        Integer limiteRecordStepCaricamentoAdesioni = Optional.ofNullable(
//                dsl.select(REEA_C_PARAMETRO.PARAMETRO_VALORE)
//                        .from(REEA_C_PARAMETRO)
//                        .where(
//                                REEA_C_PARAMETRO.PARAMETRO_COD
//                                        .eq("NumeroRecordRicercaAdesioni")
//                        )
//                        .fetchOneInto(Integer.class)
//        ).orElse(1000);
//
//        Integer limiteRecord = Optional.ofNullable(
//                dsl.select(REEA_C_PARAMETRO.PARAMETRO_VALORE)
//                        .from(REEA_C_PARAMETRO)
//                        .where(
//                                REEA_C_PARAMETRO.PARAMETRO_COD
//                                        .eq("NumeroRecordRicercaGenerica")
//                        )
//                        .fetchOneInto(Integer.class)
//        ).orElse(1000);
//
//        /*
//         * Query preliminare: viene mantenuta per rispettare la logica
//         * esistente di paginazione e aggiornamento dell'ultimo registro.
//         */
//        var basePageQuery = dsl
//                .select(
//                        REEA_T_SOGGETTO.SOGGETTO_ID,
//                        REEA_T_REGISTRO.REGISTRO_ID
//                )
//                .from(REEA_T_SOGGETTO)
//                .join(REEA_T_REGISTRO)
//                .on(
//                        REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                                field(
//                                        "reea.hmac_soggetto_id({0}, {1})",
//                                        String.class,
//                                        REEA_T_SOGGETTO.SOGGETTO_ID,
//                                        val("16<odcc8!")
//                                )
//                        )
//                )
//                .join(REEA_D_SOGGETTO_STATO)
//                .on(
//                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID
//                                .eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID)
//                )
//                .join(REEA_D_FONTE)
//                .on(
//                        REEA_D_FONTE.FONTE_ID
//                                .eq(REEA_T_SOGGETTO.FONTE_ID)
//                )
//                .leftJoin(REEA_D_ASL)
//                .on(
//                        REEA_D_ASL.ASL_ID
//                                .eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID)
//                )
//                .where(
//                        condGlobale
//                                .and(condFonteGlobaleT)
//                )
//                .orderBy(REEA_T_REGISTRO.REGISTRO_ID.asc());
//
//        var page = stepAttivo
//                ? basePageQuery.limit(limiteRecordStepCaricamentoAdesioni).fetch()
//                : basePageQuery.limit(limiteRecord).fetch();
//
//        List<Integer> ids = page.getValues(REEA_T_SOGGETTO.SOGGETTO_ID);
//
//        Integer nuovoLastRegistroId = page.isEmpty()
//                ? null
//                : page.get(page.size() - 1).get(REEA_T_REGISTRO.REGISTRO_ID);
//
//        if (nuovoLastRegistroId != null) {
//            salvaUltimoRegistroIdProcessato(
//                    nuovoLastRegistroId,
//                    auditLogRequest
//            );
//        }
//
//        /*
//         * Questa è la modifica sostanziale:
//         *
//         * - In step mode: limitiamo ai soggetti selezionati dalla pagina.
//         * - Fuori dallo step: non applichiamo il filtro IN(ids), altrimenti
//         *   i filtri HMAC possono trovare solo i match presenti nella prima pagina.
//         */
//        Condition condPaginaCorrente = stepAttivo
//                ? REEA_T_SOGGETTO.SOGGETTO_ID.in(ids)
//                : noCondition();
//
//        Condition condPaginaStorico = stepAttivo
//                ? REEA_S_SOGGETTO.SOGGETTO_ID.in(ids)
//                : noCondition();
//
//        Select<Record> selectCorrente = dsl
//                .select(
//                        DSL.val((String) null).as("ASL_AZIENDA_DESC"),
//                        DSL.val((Integer) null).as("FONTE_ID"),
//                        DSL.val((String) null).as("FONTE_DESC"),
//                        DSL.val((LocalDateTime) null).as("DATA_MODIFICA"),
//                        DSL.val((LocalDateTime) null).as("DATA_CREAZIONE"),
//                        DSL.val((LocalDate) null).as("NASCITA_DATA"),
//                        DSL.val((Integer) null).as("SOGGETTO_ID"),
//                        DSL.val((Integer) null).as("VERSIONE_NUMERO"),
//                        DSL.val((LocalDateTime) null).as("ADESIONE_DATA"),
//                        DSL.val((LocalDateTime) null).as("PRESENTAZIONE_ISTANZA_DATA"),
//                        DSL.val((String) null).as("NOME_CHIARO"),
//                        DSL.val((String) null).as("COGNOME_CHIARO"),
//                        DSL.val((String) null).as("SESSO"),
//                        DSL.val((String) null).as("CODICE_FISCALE"),
//                        DSL.val((Integer) null).as("DOMICILIO_ASL_ID"),
//                        DSL.val((Integer) null).as("RESIDENZA_ASL_ID"),
//                        DSL.val((Integer) null).as("ASSISTENZA_ASL_ID"),
//                        DSL.val((Integer) null).as("SEZIONE"),
//                        DSL.val((Boolean) null).as("INSERITO_IN_SORVEGLIANZA"),
//                        DSL.val((String) null).as("TIPO_ELENCO_INAIL"),
//                        DSL.val((String) null).as("SOGGETTO_STATO_NOTE"),
//                        DSL.val((Integer) null).as("REGISTRO_ID"),
//                        DSL.val((String) null).as("SOGGETTO_STATO_DESC"),
//                        DSL.val((String) null).as("TELEFONO_AURA"),
//                        DSL.val((String) null).as("EMAIL_AURA"),
//                        DSL.val((Integer) null).as("SOGGETTO_STATO_PRECEDENTE_ID"),
//                        DSL.val((byte[]) null).as("COGNOME_HASH_1CHAR"),
//                        DSL.val((byte[]) null).as("COGNOME_HASH_2CHAR"),
//                        DSL.val((byte[]) null).as("COGNOME_HASH"),
//                        DSL.val((byte[]) null).as("NOME_HASH_1CHAR"),
//                        DSL.val((byte[]) null).as("NOME_HASH_2CHAR"),
//                        DSL.val((byte[]) null).as("NOME_HASH")
//                )
//                .where(DSL.falseCondition());
//
//        boolean includeCorrente =
//                "REEA_OP_SPRESAL".equals(profiloUtente)
//                || "REEA_OP_CRPT".equals(profiloUtente)
//                || "REEA_OP_CSI".equals(profiloUtente)
//                || "REEA_OP_CRPT_PSEUDO".equals(profiloUtente)
//                || "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
//                || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
//                || "REEA_OP_CSI_PSEUDO".equals(profiloUtente);
//
//        if (includeCorrente) {
//            selectCorrente = dsl
//                    .selectDistinct(
//                            REEA_D_ASL.ASL_AZIENDA_DESC.as("ASL_AZIENDA_DESC"),
//                            REEA_T_SOGGETTO.FONTE_ID.as("FONTE_ID"),
//                            REEA_D_FONTE.FONTE_DESC.as("FONTE_DESC"),
//                            REEA_T_SOGGETTO.DATA_MODIFICA.as("DATA_MODIFICA"),
//                            REEA_T_SOGGETTO.DATA_CREAZIONE.as("DATA_CREAZIONE"),
//                            REEA_T_SOGGETTO.NASCITA_DATA.as("NASCITA_DATA"),
//                            REEA_T_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
//                            REEA_T_SOGGETTO.VERSIONE_NUMERO.as("VERSIONE_NUMERO"),
//                            REEA_T_ADESIONE.ADESIONE_DATA.as("ADESIONE_DATA"),
//                            REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA
//                                    .as("PRESENTAZIONE_ISTANZA_DATA"),
//
//                            DSL.when(
//                                    REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
//                                    field(
//                                            "pgp_sym_decrypt({0}, {1})",
//                                            String.class,
//                                            REEA_T_SOGGETTO.NOME_CIFRATO,
//                                            val("16<odcc8!")
//                                    )
//                            )
//                            .otherwise((String) null)
//                            .as("NOME_CHIARO"),
//
//                            DSL.when(
//                                    REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//                                    field(
//                                            "pgp_sym_decrypt({0}, {1})",
//                                            String.class,
//                                            REEA_T_SOGGETTO.COGNOME_CIFRATO,
//                                            val("16<odcc8!")
//                                    )
//                            )
//                            .otherwise((String) null)
//                            .as("COGNOME_CHIARO"),
//
//                            REEA_T_SOGGETTO.SESSO.as("SESSO"),
//                            REEA_T_SOGGETTO.CODICE_FISCALE.as("CODICE_FISCALE"),
//                            REEA_T_SOGGETTO.DOMICILIO_ASL_ID.as("DOMICILIO_ASL_ID"),
//                            REEA_T_SOGGETTO.RESIDENZA_ASL_ID.as("RESIDENZA_ASL_ID"),
//                            REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("ASSISTENZA_ASL_ID"),
//                            REEA_T_REGISTRO.SEZIONE.as("SEZIONE"),
//                            REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA
//                                    .as("INSERITO_IN_SORVEGLIANZA"),
//                            REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("TIPO_ELENCO_INAIL"),
//                            REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE
//                                    .as("SOGGETTO_STATO_NOTE"),
//                            REEA_T_REGISTRO.REGISTRO_ID.as("REGISTRO_ID"),
//                            REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
//                                    .as("SOGGETTO_STATO_DESC"),
//
//                            DSL.when(
//                                    REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
//                                    field(
//                                            "pgp_sym_decrypt({0}, {1})",
//                                            String.class,
//                                            REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO,
//                                            val("16<odcc8!")
//                                    )
//                            )
//                            .otherwise((String) null)
//                            .as("TELEFONO_AURA"),
//
//                            DSL.when(
//                                    REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
//                                    field(
//                                            "pgp_sym_decrypt({0}, {1})",
//                                            String.class,
//                                            REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA,
//                                            val("16<odcc8!")
//                                    )
//                            )
//                            .otherwise((String) null)
//                            .as("EMAIL_AURA"),
//
//                            DSL.field(
//                                    DSL.select(REEA_S_SOGGETTO.SOGGETTO_STATO_ID)
//                                            .from(REEA_S_SOGGETTO)
//                                            .where(
//                                                    REEA_S_SOGGETTO.SOGGETTO_ID
//                                                            .eq(REEA_T_SOGGETTO.SOGGETTO_ID)
//                                            )
//                                            .and(REEA_S_SOGGETTO.DATA_CANCELLAZIONE.isNull())
//                                            .orderBy(REEA_S_SOGGETTO.DATA_MODIFICA.desc())
//                                            .limit(1)
//                            ).as("SOGGETTO_STATO_PRECEDENTE_ID"),
//
//                            REEA_T_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
//                            REEA_T_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
//                            REEA_T_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
//                            REEA_T_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
//                            REEA_T_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
//                            REEA_T_SOGGETTO.NOME_HASH.as("NOME_HASH")
//                    )
//                    .from(REEA_T_SOGGETTO)
//                    .join(REEA_D_FONTE)
//                    .on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//                    .leftJoin(REEA_D_ASL)
//                    .on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//                    .join(REEA_T_REGISTRO)
//                    .on(
//                            REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                                    field(
//                                            "reea.hmac_soggetto_id({0}, {1})",
//                                            String.class,
//                                            REEA_T_SOGGETTO.SOGGETTO_ID,
//                                            val("16<odcc8!")
//                                    )
//                            )
//                    )
//                    .join(REEA_D_SOGGETTO_STATO)
//                    .on(
//                            REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID
//                                    .eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID)
//                    )
//                    .leftJoin(REEA_T_ADESIONE)
//                    .on(
//                            REEA_T_ADESIONE.SOGGETTO_ID
//                                    .eq(REEA_T_SOGGETTO.SOGGETTO_ID)
//                                    .and(REEA_T_ADESIONE.SOGGETTO_ID.isNotNull())
//                    )
//                    .where(
//                            condGlobale
//                                    .and(condFonteGlobaleT)
//                                    .and(condPaginaCorrente)
//                    );
//        }
//
//        Select<Record> selectStorico = null;
//
//        boolean includeStorico =
//                "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
//                || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
//                || "REEA_OP_SPRESAL".equals(profiloUtente)
//                || "REEA_OP_CSI_PSEUDO".equals(profiloUtente);
//
//        if (includeStorico) {
//            selectStorico = dsl
//                    .selectDistinct(
//                            REEA_D_ASL.ASL_AZIENDA_DESC.as("ASL_AZIENDA_DESC"),
//                            REEA_S_SOGGETTO.FONTE_ID.as("FONTE_ID"),
//                            REEA_D_FONTE.FONTE_DESC.as("FONTE_DESC"),
//                            REEA_S_SOGGETTO.DATA_MODIFICA.as("DATA_MODIFICA"),
//                            REEA_S_SOGGETTO.DATA_CREAZIONE.as("DATA_CREAZIONE"),
//                            REEA_S_SOGGETTO.NASCITA_DATA.as("NASCITA_DATA"),
//                            REEA_S_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
//                            REEA_S_SOGGETTO.VERSIONE_NUMERO.as("VERSIONE_NUMERO"),
//                            DSL.val((LocalDateTime) null).as("ADESIONE_DATA"),
//                            DSL.val((LocalDateTime) null).as("PRESENTAZIONE_ISTANZA_DATA"),
//
//                            DSL.when(
//                                    REEA_S_SOGGETTO.NOME_CIFRATO.isNotNull(),
//                                    field(
//                                            "pgp_sym_decrypt({0}, {1})",
//                                            String.class,
//                                            REEA_S_SOGGETTO.NOME_CIFRATO,
//                                            val("16<odcc8!", SQLDataType.VARCHAR)
//                                    )
//                            )
//                            .otherwise(DSL.val((String) null, SQLDataType.VARCHAR))
//                            .as("NOME_CHIARO"),
//
//                            DSL.when(
//                                    REEA_S_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//                                    field(
//                                            "pgp_sym_decrypt({0}, {1})",
//                                            String.class,
//                                            REEA_S_SOGGETTO.COGNOME_CIFRATO,
//                                            val("16<odcc8!", SQLDataType.VARCHAR)
//                                    )
//                            )
//                            .otherwise(DSL.val((String) null, SQLDataType.VARCHAR))
//                            .as("COGNOME_CHIARO"),
//
//                            REEA_S_SOGGETTO.SESSO.as("SESSO"),
//                            REEA_S_SOGGETTO.CODICE_FISCALE.as("CODICE_FISCALE"),
//                            REEA_S_SOGGETTO.DOMICILIO_ASL_ID.as("DOMICILIO_ASL_ID"),
//                            REEA_S_SOGGETTO.RESIDENZA_ASL_ID.as("RESIDENZA_ASL_ID"),
//                            REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.as("ASSISTENZA_ASL_ID"),
//                            REEA_T_REGISTRO.SEZIONE.as("SEZIONE"),
//                            REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA
//                                    .as("INSERITO_IN_SORVEGLIANZA"),
//                            REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("TIPO_ELENCO_INAIL"),
//                            REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE
//                                    .as("SOGGETTO_STATO_NOTE"),
//                            REEA_T_REGISTRO.REGISTRO_ID.as("REGISTRO_ID"),
//                            REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
//                                    .as("SOGGETTO_STATO_DESC"),
//                            DSL.val((String) null).as("TELEFONO_AURA"),
//                            DSL.val((String) null).as("EMAIL_AURA"),
//                            DSL.val((Integer) null).as("SOGGETTO_STATO_PRECEDENTE_ID"),
//
//                            REEA_S_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
//                            REEA_S_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
//                            REEA_S_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
//                            REEA_S_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
//                            REEA_S_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
//                            REEA_S_SOGGETTO.NOME_HASH.as("NOME_HASH")
//                    )
//                    .from(REEA_S_SOGGETTO)
//                    .join(REEA_D_FONTE)
//                    .on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
//                    .leftJoin(REEA_D_ASL)
//                    .on(REEA_D_ASL.ASL_ID.eq(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID))
//                    .join(REEA_T_REGISTRO)
//                    .on(
//                            REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                                    field(
//                                            "reea.hmac_soggetto_id({0}, {1})",
//                                            String.class,
//                                            REEA_S_SOGGETTO.SOGGETTO_ID,
//                                            val("16<odcc8!")
//                                    )
//                            )
//                    )
//                    .join(REEA_D_SOGGETTO_STATO)
//                    .on(
//                            REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID
//                                    .eq(REEA_S_SOGGETTO.SOGGETTO_STATO_ID)
//                    )
//                    .where(
//                            condGlobaleStorico
//                                    .and(condFonteGlobaleS)
//                                    .and(
//                                            REEA_S_SOGGETTO.SOGGETTO_ID.notIn(
//                                                    dsl.selectDistinct(
//                                                            REEA_T_SOGGETTO.SOGGETTO_ID
//                                                    )
//                                                    .from(REEA_T_SOGGETTO)
//                                            )
//                                    )
//                                    .and(condPaginaStorico)
//                    );
//        }
//
//        Table<?> unionTable;
//
//        if (includeStorico && selectStorico != null) {
//            unionTable = selectCorrente.unionAll(selectStorico).asTable("q");
//        } else {
//            unionTable = selectCorrente.asTable("q");
//        }
//
//        Condition condOuterCognome = noCondition();
//
//        if (cognome != null && !cognome.isBlank()) {
//            String value = cognome.trim().toUpperCase();
//            int len = value.length();
//
//            Field<byte[]> hmacValue = DSL.function(
//                    "hmac",
//                    SQLDataType.BLOB,
//                    DSL.val(value, SQLDataType.VARCHAR),
//                    DSL.inline("Porcatrota2000!"),
//                    DSL.inline("sha256")
//            );
//
//            if (len == 1) {
//                condOuterCognome = DSL.field(
//                        DSL.name("q", "COGNOME_HASH_1CHAR"),
//                        byte[].class
//                ).eq(hmacValue);
//            } else if (len == 2) {
//                condOuterCognome = DSL.field(
//                        DSL.name("q", "COGNOME_HASH_2CHAR"),
//                        byte[].class
//                ).eq(hmacValue);
//            } else {
//                condOuterCognome = DSL.field(
//                        DSL.name("q", "COGNOME_HASH"),
//                        byte[].class
//                ).eq(hmacValue);
//            }
//        }
//
//        Condition condOuterNome = noCondition();
//
//        if (nome != null && !nome.isBlank()) {
//            String value = nome.trim().toUpperCase();
//            int len = value.length();
//
//            Field<byte[]> hmacValue = DSL.function(
//                    "hmac",
//                    SQLDataType.BLOB,
//                    DSL.val(value, SQLDataType.VARCHAR),
//                    DSL.inline("Porcatrota2000!"),
//                    DSL.inline("sha256")
//            );
//
//            if (len == 1) {
//                condOuterNome = DSL.field(
//                        DSL.name("q", "NOME_HASH_1CHAR"),
//                        byte[].class
//                ).eq(hmacValue);
//            } else if (len == 2) {
//                condOuterNome = DSL.field(
//                        DSL.name("q", "NOME_HASH_2CHAR"),
//                        byte[].class
//                ).eq(hmacValue);
//            } else {
//                condOuterNome = DSL.field(
//                        DSL.name("q", "NOME_HASH"),
//                        byte[].class
//                ).eq(hmacValue);
//            }
//        }
//
//        Condition condCognomeRange = noCondition();
//
//        if (cognomeLettDa != null && !cognomeLettDa.isBlank()
//                && cognomeLettA != null && !cognomeLettA.isBlank()) {
//
//            char da = Character.toUpperCase(cognomeLettDa.trim().charAt(0));
//            char a = Character.toUpperCase(cognomeLettA.trim().charAt(0));
//
//            List<Field<byte[]>> hmacList = new ArrayList<>();
//
//            for (char c = da; c <= a; c++) {
//                hmacList.add(
//                        DSL.function(
//                                "hmac",
//                                SQLDataType.BLOB,
//                                DSL.val(String.valueOf(c), SQLDataType.VARCHAR),
//                                DSL.inline("Porcatrota2000!"),
//                                DSL.inline("sha256")
//                        )
//                );
//            }
//
//            condCognomeRange = DSL.field(
//                    DSL.name("q", "COGNOME_HASH_1CHAR"),
//                    byte[].class
//            ).in(hmacList);
//        }
//
//        var finalQuery = dsl
//                .selectFrom(unionTable)
//                .where(
//                        condOuterCognome
//                                .and(condOuterNome)
//                                .and(condCognomeRange)
//                )
//                .orderBy(
//                        DSL.field(DSL.name("q", "COGNOME_CHIARO")).asc(),
//                        DSL.field(DSL.name("q", "NOME_CHIARO")).asc()
//                );
//
//        /*
//         * Il LIMIT viene applicato dopo i filtri HMAC:
//         * in questo modo i cinque soggetti individuati dal cognome/nome
//         * non vengono tagliati dalla pagina preliminare.
//         */
//        var finalRecords = stepAttivo
//                ? finalQuery.limit(limiteRecordStepCaricamentoAdesioni).fetch()
//                : finalQuery.limit(limiteRecord).fetch();
//
//        return finalRecords.stream()
//                .map(r -> {
//                    AnagraficaDTO row = new AnagraficaDTO();
//
//                    Integer soggettoIdInt = r.get("SOGGETTO_ID", Integer.class);
//                    Long soggettoId = soggettoIdInt != null
//                            ? soggettoIdInt.longValue()
//                            : null;
//
//                    row.setSoggettoId(soggettoId);
//                    row.setFonteId(r.get("FONTE_ID", Integer.class));
//
//                    String fonteDesc = r.get("FONTE_DESC", String.class);
//                    row.setDescrizioneFonte(
//                            List.of(fonteDesc != null ? fonteDesc : "PROVA")
//                    );
//
//                    RegistroDTO registro = RegistroUtils.ensureRegistro(row);
//
//                    registro.setDataModifica(
//                            DateConversionUtils.toOffsetDateTime(
//                                    r.get("DATA_MODIFICA", LocalDateTime.class)
//                            )
//                    );
//
//                    registro.setDataCreazione(
//                            DateConversionUtils.toOffsetDateTime(
//                                    r.get("DATA_CREAZIONE", LocalDateTime.class)
//                            )
//                    );
//
//                    registro.setVersioneNumero(
//                            r.get("VERSIONE_NUMERO", Integer.class)
//                    );
//
//                    registro.setRegistroId(
//                            r.get("REGISTRO_ID", Integer.class)
//                    );
//
//                    registro.setSezione(
//                            r.get("SEZIONE", Integer.class)
//                    );
//
//                    registro.setInseritoInSorveglianza(
//                            r.get("INSERITO_IN_SORVEGLIANZA", Boolean.class)
//                    );
//
//                    registro.setTipoElencoInail(
//                            r.get("TIPO_ELENCO_INAIL", String.class)
//                    );
//
//                    row.setNascitaData(
//                            r.get("NASCITA_DATA", LocalDate.class)
//                    );
//
//                    String nomeChiaro = r.get("NOME_CHIARO", String.class);
//                    row.setNome(
//                            nomeChiaro != null
//                                    ? nomeChiaro.toUpperCase()
//                                    : ""
//                    );
//
//                    String cognomeChiaro = r.get("COGNOME_CHIARO", String.class);
//                    row.setCognome(
//                            cognomeChiaro != null
//                                    ? cognomeChiaro.toUpperCase()
//                                    : ""
//                    );
//
//                    row.setSesso(r.get("SESSO", String.class));
//                    row.setCodiceFiscale(r.get("CODICE_FISCALE", String.class));
//                    row.setDomicilioAslId(
//                            r.get("DOMICILIO_ASL_ID", Integer.class)
//                    );
//                    row.setResidenzaAslId(
//                            r.get("RESIDENZA_ASL_ID", Integer.class)
//                    );
//
//                    Integer assistenzaId =
//                            r.get("ASSISTENZA_ASL_ID", Integer.class);
//
//                    row.setAssistenzaAslId(
//                            assistenzaId != null
//                                    ? String.format("%06d", assistenzaId)
//                                    : null
//                    );
//
//                    row.setDescrizioneAslCompetenza(
//                            r.get("ASL_AZIENDA_DESC", String.class)
//                    );
//
//                    row.setTelefonoAura(
//                            r.get("TELEFONO_AURA", String.class)
//                    );
//
//                    row.setEmailAura(
//                            r.get("EMAIL_AURA", String.class)
//                    );
//
//                    row.setDescrizioneStato(
//                            r.get("SOGGETTO_STATO_DESC", String.class)
//                    );
//
//                    row.setSoggettoStatoNote(
//                            r.get("SOGGETTO_STATO_NOTE", String.class)
//                    );
//
//                    row.setSoggettoStatoPrecedenteId(
//                            r.get(
//                                    "SOGGETTO_STATO_PRECEDENTE_ID",
//                                    Integer.class
//                            )
//                    );
//
//                    if (soggettoId != null) {
//                        row.setListaFonteByIdSoggetto(
//                                fonteRepository != null
//                                        ? fonteRepository
//                                                .findListaFonteDescByIdSoggetto(soggettoId)
//                                        : Collections.emptyList()
//                        );
//                    } else {
//                        row.setListaFonteByIdSoggetto(Collections.emptyList());
//                        row.setListaEsenzione(Collections.emptyList());
//                    }
//
//                    LocalDateTime adesioneData =
//                            r.get("ADESIONE_DATA", LocalDateTime.class);
//
//                    row.setAdesioneData(
//                            adesioneData != null
//                                    ? adesioneData.toString()
//                                    : null
//                    );
//
//                    LocalDateTime presentazione =
//                            r.get(
//                                    "PRESENTAZIONE_ISTANZA_DATA",
//                                    LocalDateTime.class
//                            );
//
//                    row.setPresentazioneIstanzaData(
//                            presentazione != null
//                                    ? presentazione.toLocalDate().toString()
//                                    : null
//                    );
//
//                    return row;
//                })
//                .filter(dto ->
//                        dto.getSoggettoId() != null
//                        && isValidoPerExport(
//                                dto.getSoggettoId().intValue(),
//                                profiloUtente
//                        )
//                )
//                .toList();
//    }
    //ORIGINALE ULTIMA VERSIONE
//    public List<AnagraficaDTO> findListaAnagrafica(List<Integer> filtroFonteId,
//    		List<String> descrizioniStato,
//    		List<String> sezione,
//    		Boolean insInSorveglianza,
//    		String tipoElencoInail,
//    		List<Integer> assistenzaAslId,
//    		String codiceFiscale,
//    		String cognome,
//    		String nome,
//    		String cognomeLettDa,
//    		String cognomeLettA,
//    		LocalDate nascitaData,
//    		Boolean azzeraContatoreProssimoStep,
//    		String profiloUtente,
//    		AuditLogRequest auditLogRequest)  {
//
//    	// DE Aggiungo questo boolean che fa da switch sulla decrypt in line di nome e cognome cifrato
//    	boolean ricercaPerNominativo = (nome != null && !nome.isBlank())  
//    			|| (cognome != null && !cognome.isBlank() || cognomeLettA != null && !cognomeLettA.isBlank())  
//    			|| (cognomeLettDa != null && !cognomeLettDa.isBlank());
//
//    	// non serve (in questo caso) eseguire la query con filtri su campi criptati quindi utilizzo quella fast
//    	if (!ricercaPerNominativo) {
//    		return findListaAnagraficaFast(
//    				filtroFonteId,
//    				descrizioniStato,
//    				sezione,
//    				insInSorveglianza,
//    				tipoElencoInail,
//    				assistenzaAslId,
//    				codiceFiscale,
//    				cognomeLettDa,
//    				cognomeLettA,
//    				nascitaData,
//    				azzeraContatoreProssimoStep,
//    				profiloUtente,
//    				auditLogRequest
//    				);
//    	}
//    	// DE FINE
//
//    	Condition condDescrizioneStato = noCondition();
//    	if (descrizioniStato != null && !descrizioniStato.isEmpty()) {
//    		condDescrizioneStato = REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.in(descrizioniStato);
//    	}
//
//    	Condition condSezione = noCondition();
//    	if (sezione != null && !sezione.isEmpty()) {
//    		condSezione = REEA_T_REGISTRO.SEZIONE.in(sezione);
//    	}
//
//    	Condition condInseritoInSorveglianza = noCondition();
//    	if (insInSorveglianza != null) {
//    		condInseritoInSorveglianza = REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.eq(insInSorveglianza);
//    	}
//
//    	Condition condTipoElencoInail = noCondition();
//    	if (tipoElencoInail != null && !tipoElencoInail.isEmpty()) {
//    		condTipoElencoInail = REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(tipoElencoInail.toUpperCase());
//    	}
//
//    	Condition condAssistenzaAsl = noCondition();
//    	Condition condAssistenzaAslStorico = noCondition();
//    	if (assistenzaAslId != null && !assistenzaAslId.isEmpty()) {
//    		condAssistenzaAsl = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//    		condAssistenzaAslStorico = REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//    	}
//
//    	Condition condCodiceFiscaleT = noCondition();
//    	Condition condCodiceFiscaleS = noCondition();
//    	if (codiceFiscale != null && !codiceFiscale.isEmpty()) {
//    		condCodiceFiscaleT = REEA_T_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
//    		condCodiceFiscaleS = REEA_S_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
//    	}
//
//    	Condition condNascitaDataT = noCondition();
//    	Condition condNascitaDataS = noCondition();
//    	if (nascitaData != null) {
//    		condNascitaDataT = REEA_T_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//    		condNascitaDataS = REEA_S_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//    	}
//
//    	// Paging condition
//    	Condition condRegistroIdPaging = noCondition();
//    	if (Boolean.FALSE.equals(azzeraContatoreProssimoStep)) {
//    		Integer lastRegistroId = getUltimoRegistroIdProcessato(auditLogRequest);
//    		if (lastRegistroId != null) {
//    			condRegistroIdPaging = REEA_T_REGISTRO.REGISTRO_ID.gt(lastRegistroId);
//    		}
//    	}
//
//    	Condition condGlobale = condDescrizioneStato
//    			.and(condSezione)
//    			.and(condInseritoInSorveglianza)
//    			.and(condTipoElencoInail)
//    			.and(condAssistenzaAsl)
//    			.and(condCodiceFiscaleT)
//    			.and(condNascitaDataT)
//    			.and(condRegistroIdPaging);
//
//    	Condition condGlobaleStorico = condDescrizioneStato
//    			.and(condSezione)
//    			.and(condInseritoInSorveglianza)
//    			.and(condTipoElencoInail)
//    			.and(condAssistenzaAslStorico)
//    			.and(condCodiceFiscaleS)
//    			.and(condNascitaDataS);
//
//    	Condition condFonteGlobaleT = noCondition();
//    	if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
//    		condFonteGlobaleT = REEA_T_SOGGETTO.FONTE_ID.in(filtroFonteId)
//    				.or(REEA_T_SOGGETTO.SOGGETTO_ID.in(
//    						dsl.selectDistinct(REEA_S_SOGGETTO.SOGGETTO_ID)
//    						.from(REEA_S_SOGGETTO)
//    						.where(REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId))
//    						));
//    	}
//
//    	Condition condFonteGlobaleS = noCondition();
//    	if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
//    		condFonteGlobaleS = REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId);
//    	}
//
//    	// Recupero limite step
//    	Integer limiteRecordStepCaricamentoAdesioni = Optional.ofNullable(
//    			dsl.select(REEA_C_PARAMETRO.PARAMETRO_VALORE)
//    			.from(REEA_C_PARAMETRO)
//    			.where(REEA_C_PARAMETRO.PARAMETRO_COD.eq("NumeroRecordRicercaAdesioni"))
//    			.fetchOneInto(Integer.class)
//    			).orElse(1000);
//    	
//    	Integer limiteRecord = Optional.ofNullable(
//    		    dsl.select(REEA_C_PARAMETRO.PARAMETRO_VALORE)
//    		       .from(REEA_C_PARAMETRO)
//    		       .where(REEA_C_PARAMETRO.PARAMETRO_COD.eq("NumeroRecordRicercaGenerica"))
//    		       .fetchOneInto(Integer.class)
//    		).orElse(1000);
//
//    	// QUERY BASE PER PAGINAZIONE (solo corrente, per ottenere IDs)
//    	var basePageQuery = dsl
//    			.select(REEA_T_SOGGETTO.SOGGETTO_ID, REEA_T_REGISTRO.REGISTRO_ID)
//    			.from(REEA_T_SOGGETTO)
//    			.join(REEA_T_REGISTRO)
//    			.on(
//    					REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//    							field("reea.hmac_soggetto_id({0}, {1})",
//    									String.class,
//    									REEA_T_SOGGETTO.SOGGETTO_ID,
//    									val("16<odcc8!"))
//    							)
//    					)
//    			.join(REEA_D_SOGGETTO_STATO)
//    			.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//    			.join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//    			.leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//    			.where(condGlobale
//    					.and(condFonteGlobaleT))
//    			.orderBy(REEA_T_REGISTRO.REGISTRO_ID.asc());
//
//    	// Applica limit solo se step attivo
//    	var page = (azzeraContatoreProssimoStep != null)
//    			? basePageQuery.limit(limiteRecordStepCaricamentoAdesioni).fetch()
//    					: basePageQuery.limit(limiteRecord).fetch();
//
//    	List<Integer> ids = page.getValues(REEA_T_SOGGETTO.SOGGETTO_ID);
//
//    	// Salva ultimo registro processato
//    	Integer nuovoLastRegistroId = page.isEmpty()
//    			? null
//    					: page.get(page.size() - 1).get(REEA_T_REGISTRO.REGISTRO_ID);
//
//    	if (nuovoLastRegistroId != null) {
//    		salvaUltimoRegistroIdProcessato(nuovoLastRegistroId, auditLogRequest);
//    	}
//
//    	Select<Record> selectCorrente = dsl
//    			.select(
//    					DSL.val((String) null).as("ASL_AZIENDA_DESC"),
//    					DSL.val((Integer) null).as("FONTE_ID"),
//    					DSL.val((String) null).as("FONTE_DESC"),
//    					DSL.val((LocalDateTime) null).as("DATA_MODIFICA"),
//    					DSL.val((LocalDateTime) null).as("DATA_CREAZIONE"),
//    					DSL.val((LocalDate) null).as("NASCITA_DATA"),
//    					DSL.val((Integer) null).as("SOGGETTO_ID"),
//    					DSL.val((Integer) null).as("VERSIONE_NUMERO"),
//    					DSL.val((LocalDateTime) null).as("ADESIONE_DATA"),
//    					DSL.val((LocalDateTime) null).as("PRESENTAZIONE_ISTANZA_DATA"),
//    					DSL.val((String) null).as("NOME_CHIARO"),
//    					DSL.val((String) null).as("COGNOME_CHIARO"),
//    					DSL.val((String) null).as("SESSO"),
//    					DSL.val((String) null).as("CODICE_FISCALE"),
//    					DSL.val((Integer) null).as("DOMICILIO_ASL_ID"),
//    					DSL.val((Integer) null).as("RESIDENZA_ASL_ID"),
//    					DSL.val((Integer) null).as("ASSISTENZA_ASL_ID"),
//    					DSL.val((Integer) null).as("SEZIONE"),
//    					DSL.val((Boolean) null).as("INSERITO_IN_SORVEGLIANZA"),
//    					DSL.val((String) null).as("TIPO_ELENCO_INAIL"),
//    					DSL.val((String) null).as("SOGGETTO_STATO_NOTE"),
//    					DSL.val((Integer) null).as("REGISTRO_ID"),
//    					DSL.val((String) null).as("SOGGETTO_STATO_DESC"),
//    					DSL.val((String) null).as("TELEFONO_AURA"),
//    					DSL.val((String) null).as("EMAIL_AURA"),
//    					DSL.val((Integer) null).as("SOGGETTO_STATO_PRECEDENTE_ID")
//    					)
//    			.where(DSL.falseCondition());
//
//    	if ("REEA_OP_SPRESAL".equals(profiloUtente)
//    			|| "REEA_OP_CRPT".equals(profiloUtente)
//    			|| "REEA_OP_CSI".equals(profiloUtente)
//    			|| "REEA_OP_CRPT_PSEUDO".equals(profiloUtente)
//    			|| "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
//    			|| "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
//    			|| "REEA_OP_CSI_PSEUDO".equals(profiloUtente)) {
//
////    		boolean includiSenzaAdesione = "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente) || "REEA_OP_CRPT".equals(profiloUtente);
//
//    		selectCorrente = dsl
//    				.selectDistinct(
//    						REEA_D_ASL.ASL_AZIENDA_DESC.as("ASL_AZIENDA_DESC"),
//    						REEA_T_SOGGETTO.FONTE_ID.as("FONTE_ID"),
//    						REEA_D_FONTE.FONTE_DESC.as("FONTE_DESC"),
//    						REEA_T_SOGGETTO.DATA_MODIFICA.as("DATA_MODIFICA"),
//    						REEA_T_SOGGETTO.DATA_CREAZIONE.as("DATA_CREAZIONE"),
//    						REEA_T_SOGGETTO.NASCITA_DATA.as("NASCITA_DATA"),
//    						REEA_T_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
//    						REEA_T_SOGGETTO.VERSIONE_NUMERO.as("VERSIONE_NUMERO"),
//    						REEA_T_ADESIONE.ADESIONE_DATA.as("ADESIONE_DATA"),
//    						REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA.as("PRESENTAZIONE_ISTANZA_DATA"),
//
//    						DSL.when(REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
//    								field("pgp_sym_decrypt({0}, {1})", String.class,
//    										REEA_T_SOGGETTO.NOME_CIFRATO,
//    										val("16<odcc8!"))
//    								).otherwise((String) null).as("NOME_CHIARO"),
//
//    						DSL.when(REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//    								field("pgp_sym_decrypt({0}, {1})", String.class,
//    										REEA_T_SOGGETTO.COGNOME_CIFRATO,
//    										val("16<odcc8!"))
//    								).otherwise((String) null).as("COGNOME_CHIARO"),
//
//    						REEA_T_SOGGETTO.SESSO.as("SESSO"),
//    						REEA_T_SOGGETTO.CODICE_FISCALE.as("CODICE_FISCALE"),
//    						REEA_T_SOGGETTO.DOMICILIO_ASL_ID.as("DOMICILIO_ASL_ID"),
//    						REEA_T_SOGGETTO.RESIDENZA_ASL_ID.as("RESIDENZA_ASL_ID"),
//    						REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("ASSISTENZA_ASL_ID"),
//    						REEA_T_REGISTRO.SEZIONE.as("SEZIONE"),
//    						REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("INSERITO_IN_SORVEGLIANZA"),
//    						REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("TIPO_ELENCO_INAIL"),
//    						REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE.as("SOGGETTO_STATO_NOTE"),
//    						REEA_T_REGISTRO.REGISTRO_ID.as("REGISTRO_ID"),
//    						REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("SOGGETTO_STATO_DESC"),
//
//    						DSL.when(REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
//    								field("pgp_sym_decrypt({0}, {1})", String.class,
//    										REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO,
//    										val("16<odcc8!"))
//    								).otherwise((String) null).as("TELEFONO_AURA"),
//
//    						DSL.when(REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
//    								field("pgp_sym_decrypt({0}, {1})", String.class,
//    										REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA,
//    										val("16<odcc8!"))
//    								).otherwise((String) null).as("EMAIL_AURA"),
//
//    						DSL.field(
//    								DSL.select(REEA_S_SOGGETTO.SOGGETTO_STATO_ID)
//    								.from(REEA_S_SOGGETTO)
//    								.where(REEA_S_SOGGETTO.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
//    								.and(REEA_S_SOGGETTO.DATA_CANCELLAZIONE.isNull())
//    								.orderBy(REEA_S_SOGGETTO.DATA_MODIFICA.desc())
//    								.limit(1)
//    								).as("SOGGETTO_STATO_PRECEDENTE_ID"),
//
//    						// MM AGGIUNTA DELLE COLONNE HASH
//    						REEA_T_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
//    						REEA_T_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
//    						REEA_T_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
//    						REEA_T_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
//    						REEA_T_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
//    						REEA_T_SOGGETTO.NOME_HASH.as("NOME_HASH")
//    						)
//    				.from(REEA_T_SOGGETTO)
//    				.join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//    				.leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//    				.join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//    						field("reea.hmac_soggetto_id({0}, {1})",
//    								String.class,
//    								REEA_T_SOGGETTO.SOGGETTO_ID,
//    								val("16<odcc8!"))))
//    				.join(REEA_D_SOGGETTO_STATO)
//    				.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//    				.leftJoin(REEA_T_ADESIONE)
//    				.on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID)
//    						.and(REEA_T_ADESIONE.SOGGETTO_ID.isNotNull()))
//    				.where(condGlobale.and(condFonteGlobaleT))
//    				.and(azzeraContatoreProssimoStep != null
//    				? REEA_T_SOGGETTO.SOGGETTO_ID.in(ids)
//    				: REEA_T_SOGGETTO.SOGGETTO_ID.in(ids));
////    						: noCondition());
////    				.and(includiSenzaAdesione ? noCondition() : REEA_T_ADESIONE.SOGGETTO_ID.isNotNull());
//    	}
//
//    	Select<Record> selectStorico = null;
//
//    	boolean includeStorico =
//    			"REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
//    			|| "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
//    			|| "REEA_OP_SPRESAL".equals(profiloUtente)
//    			|| "REEA_OP_CSI_PSEUDO".equals(profiloUtente);
//
//    	if (includeStorico) {
//    		selectStorico = dsl
//    				.selectDistinct(
//    						REEA_D_ASL.ASL_AZIENDA_DESC.as("ASL_AZIENDA_DESC"),
//    						REEA_S_SOGGETTO.FONTE_ID.as("FONTE_ID"),
//    						REEA_D_FONTE.FONTE_DESC.as("FONTE_DESC"),
//    						REEA_S_SOGGETTO.DATA_MODIFICA.as("DATA_MODIFICA"),
//    						REEA_S_SOGGETTO.DATA_CREAZIONE.as("DATA_CREAZIONE"),
//    						REEA_S_SOGGETTO.NASCITA_DATA.as("NASCITA_DATA"),
//    						REEA_S_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
//    						REEA_S_SOGGETTO.VERSIONE_NUMERO.as("VERSIONE_NUMERO"),
//    						DSL.val((LocalDateTime) null).as("ADESIONE_DATA"),
//    						DSL.val((LocalDateTime) null).as("PRESENTAZIONE_ISTANZA_DATA"),
//
//    						DSL.when(
//    								REEA_S_SOGGETTO.NOME_CIFRATO.isNotNull(),
//    								DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//    										REEA_S_SOGGETTO.NOME_CIFRATO,
//    										DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//    								).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("NOME_CHIARO"),
//
//    						DSL.when(
//    								REEA_S_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//    								DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//    										REEA_S_SOGGETTO.COGNOME_CIFRATO,
//    										DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//    								).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("COGNOME_CHIARO"),
//
//    						REEA_S_SOGGETTO.SESSO.as("SESSO"),
//    						REEA_S_SOGGETTO.CODICE_FISCALE.as("CODICE_FISCALE"),
//    						REEA_S_SOGGETTO.DOMICILIO_ASL_ID.as("DOMICILIO_ASL_ID"),
//    						REEA_S_SOGGETTO.RESIDENZA_ASL_ID.as("RESIDENZA_ASL_ID"),
//    						REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.as("ASSISTENZA_ASL_ID"),
//    						REEA_T_REGISTRO.SEZIONE.as("SEZIONE"),
//    						REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("INSERITO_IN_SORVEGLIANZA"),
//    						REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("TIPO_ELENCO_INAIL"),
//    						REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE.as("SOGGETTO_STATO_NOTE"),
//    						REEA_T_REGISTRO.REGISTRO_ID.as("REGISTRO_ID"),
//    						REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("SOGGETTO_STATO_DESC"),
//    						DSL.val((String) null).as("TELEFONO_AURA"),
//    						DSL.val((String) null).as("EMAIL_AURA"),
//    						DSL.val((Integer) null).as("SOGGETTO_STATO_PRECEDENTE_ID"),
//
//    						// MM AGGIUNTA DELLE COLONNE HASH
//    						REEA_S_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
//    						REEA_S_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
//    						REEA_S_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
//    						REEA_S_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
//    						REEA_S_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
//    						REEA_S_SOGGETTO.NOME_HASH.as("NOME_HASH")
//    						)
//    				.from(REEA_S_SOGGETTO)
//    				.join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
//    				.leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID))
//    				.join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//    						field("reea.hmac_soggetto_id({0}, {1})",
//    								String.class,
//    								REEA_S_SOGGETTO.SOGGETTO_ID,
//    								val("16<odcc8!"))))
//    				.join(REEA_D_SOGGETTO_STATO)
//    				.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_S_SOGGETTO.SOGGETTO_STATO_ID))
//    				.where(condGlobaleStorico
//    						.and(condFonteGlobaleS)
//    						.and(REEA_S_SOGGETTO.SOGGETTO_ID.notIn(
//    								dsl.selectDistinct(REEA_T_SOGGETTO.SOGGETTO_ID).from(REEA_T_SOGGETTO)
//    								))
//    						.and(azzeraContatoreProssimoStep != null
//    						? REEA_S_SOGGETTO.SOGGETTO_ID.in(ids)
//    								: noCondition()));
//    	}
//
//    	Table<?> unionTable;
//    	if (includeStorico && selectStorico != null) {
//    		unionTable = selectCorrente.unionAll(selectStorico).asTable("q");
//    	} else {
//    		unionTable = selectCorrente.asTable("q");
//    	}
//
//    	// Filtro COGNOME (HMAC)
//    	Condition condOuterCognome = noCondition();
//    	if (cognome != null && !cognome.isEmpty()) {
//    		int len = cognome.length();
//    		String value = cognome.trim().toUpperCase();
//
//    		Field<byte[]> hmacValue = DSL.function(
//    				"hmac",
//    				SQLDataType.BLOB,
//    				DSL.val(value, SQLDataType.VARCHAR),
//    				DSL.inline("Porcatrota2000!"),
//    				DSL.inline("sha256")
//    				);
//
//    		if (len == 1) {
//    			condOuterCognome = DSL.field(DSL.name("q", "COGNOME_HASH_1CHAR"), byte[].class)
//    					.eq(hmacValue);
//    		} else if (len == 2) {
//    			condOuterCognome = DSL.field(DSL.name("q", "COGNOME_HASH_2CHAR"), byte[].class)
//    					.eq(hmacValue);
//    		} else {
//    			condOuterCognome = DSL.field(DSL.name("q", "COGNOME_HASH"), byte[].class)
//    					.eq(hmacValue);
//    		}
//    	}
//
//    	// Filtro NOME (HMAC)
//    	Condition condOuterNome = noCondition();
//    	if (nome != null && !nome.isEmpty()) {
//    		int len = nome.length();
//    		String value = nome.trim().toUpperCase();
//
//    		Field<byte[]> hmacValue = DSL.function(
//    				"hmac",
//    				SQLDataType.BLOB,
//    				DSL.val(value, SQLDataType.VARCHAR),
//    				DSL.inline("Porcatrota2000!"),
//    				DSL.inline("sha256")
//    				);
//
//    		if (len == 1) {
//    			condOuterNome = DSL.field(DSL.name("q", "NOME_HASH_1CHAR"), byte[].class)
//    					.eq(hmacValue);
//    		} else if (len == 2) {
//    			condOuterNome = DSL.field(DSL.name("q", "NOME_HASH_2CHAR"), byte[].class)
//    					.eq(hmacValue);
//    		} else {
//    			condOuterNome = DSL.field(DSL.name("q", "NOME_HASH"), byte[].class)
//    					.eq(hmacValue);
//    		}
//    	}
//
//    	// Filtro alfabetico COGNOME DA - A (range di lettere)
//    	Condition condCognomeRange = noCondition();
//    	if (cognomeLettDa != null && !cognomeLettDa.isEmpty()
//    			&& cognomeLettA != null && !cognomeLettA.isEmpty()) {
//
//    		char da = cognomeLettDa.trim().charAt(0);
//    		char a  = cognomeLettA.trim().charAt(0);
//
//    		List<String> lettere = new ArrayList<>();
//    		for (char c = da; c <= a; c++) {
//    			lettere.add(String.valueOf(c));
//    		}
//
//    		List<Field<byte[]>> hmacList = new ArrayList<>();
//    		for (String letter : lettere) {
//    			Field<byte[]> hmacLetter = DSL.function(
//    					"hmac",
//    					SQLDataType.BLOB,
//    					DSL.val(letter, SQLDataType.VARCHAR),
//    					DSL.inline("Porcatrota2000!"),
//    					DSL.inline("sha256")
//    					);
//    			hmacList.add(hmacLetter);
//    		}
//
//    		condCognomeRange = DSL.field(DSL.name("q", "COGNOME_HASH_1CHAR"), byte[].class)
//    				.in(hmacList);
//    	}
//
//    	// ESECUZIONE QUERY FINALE CON FILTRI HASH
//    	var finalQuery = dsl.selectFrom(unionTable)
//    			.where(
//    					condOuterCognome
//    					.and(condOuterNome)
//    					.and(condCognomeRange)
//    					)
//    			.orderBy(
//    					field(DSL.name("q", "COGNOME_CHIARO")).asc(),
//    					field(DSL.name("q", "NOME_CHIARO")).asc()
//    					);
//
//    	// Applica limit solo se step attivo
//    	var finalRecords = (azzeraContatoreProssimoStep != null)
//    			? finalQuery.limit(limiteRecordStepCaricamentoAdesioni).fetch()
//    					: finalQuery.fetch();
//
//    	return finalRecords
//    			.stream()
//    			.map(r -> {
//    				AnagraficaDTO row = new AnagraficaDTO();
//
//    				Integer soggettoIdInt = r.get("SOGGETTO_ID", Integer.class);
//    				Long soggettoId = soggettoIdInt != null ? soggettoIdInt.longValue() : null;
//    				row.setSoggettoId(soggettoId);
//
//    				row.setFonteId(r.get("FONTE_ID", Integer.class));
//
//    				String fonteDesc = r.get("FONTE_DESC", String.class);
//    				row.setDescrizioneFonte(List.of(fonteDesc != null ? fonteDesc : "PROVA"));
//
//    				RegistroDTO registro = RegistroUtils.ensureRegistro(row);
//    				registro.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get("DATA_MODIFICA", LocalDateTime.class)));
//    				registro.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get("DATA_CREAZIONE", LocalDateTime.class)));
//    				registro.setVersioneNumero(r.get("VERSIONE_NUMERO", Integer.class));
//    				registro.setRegistroId(r.get("REGISTRO_ID", Integer.class));
//    				registro.setSezione(r.get("SEZIONE", Integer.class));
//    				registro.setInseritoInSorveglianza(r.get("INSERITO_IN_SORVEGLIANZA", Boolean.class));
//    				registro.setTipoElencoInail(r.get("TIPO_ELENCO_INAIL", String.class));
//
//    				row.setNascitaData(r.get("NASCITA_DATA", LocalDate.class));
//
//    				String nomeChiaro = r.get("NOME_CHIARO", String.class);
//    				row.setNome(nomeChiaro != null ? nomeChiaro.toUpperCase() : "");
//
//    				String cognomeChiaro = r.get("COGNOME_CHIARO", String.class);
//    				row.setCognome(cognomeChiaro != null ? cognomeChiaro.toUpperCase() : "");
//
//    				row.setSesso(r.get("SESSO", String.class));
//    				row.setCodiceFiscale(r.get("CODICE_FISCALE", String.class));
//    				row.setDomicilioAslId(r.get("DOMICILIO_ASL_ID", Integer.class));
//    				row.setResidenzaAslId(r.get("RESIDENZA_ASL_ID", Integer.class));
//
//    				Integer assistenzaId = r.get("ASSISTENZA_ASL_ID", Integer.class);
//    				row.setAssistenzaAslId(assistenzaId != null ? String.format("%06d", assistenzaId) : null);
//
//    				row.setDescrizioneAslCompetenza(r.get("ASL_AZIENDA_DESC", String.class));
//    				row.setTelefonoAura(r.get("TELEFONO_AURA", String.class));
//    				row.setEmailAura(r.get("EMAIL_AURA", String.class));
//    				row.setDescrizioneStato(r.get("SOGGETTO_STATO_DESC", String.class));
//    				row.setSoggettoStatoNote(r.get("SOGGETTO_STATO_NOTE", String.class));
//    				row.setSoggettoStatoPrecedenteId(r.get("SOGGETTO_STATO_PRECEDENTE_ID", Integer.class));
//
//    				if (soggettoId != null) {
//    					row.setListaFonteByIdSoggetto(
//    							fonteRepository != null
//    							? fonteRepository.findListaFonteDescByIdSoggetto(soggettoId)
//    									: Collections.emptyList()
//    							);
//
////    					row.setListaEsenzione(
////    							esenzioneRepository != null
////    							? esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId)
////    									: Collections.emptyList()
////    							);
//    				} else {
//    					row.setListaFonteByIdSoggetto(Collections.emptyList());
//    					row.setListaEsenzione(Collections.emptyList());
//    				}
//
//    				LocalDateTime adesioneData = r.get("ADESIONE_DATA", LocalDateTime.class);
//    				row.setAdesioneData(adesioneData != null ? adesioneData.toString() : null);
//
//    				LocalDateTime presentazione = r.get("PRESENTAZIONE_ISTANZA_DATA", LocalDateTime.class);
//    				row.setPresentazioneIstanzaData(
//    						presentazione != null ? presentazione.toLocalDate().toString() : null
//    						);
//
//    				return row;
//    			})
//    			.filter(dto -> dto.getSoggettoId() != null && isValidoPerExport(dto.getSoggettoId().intValue(), profiloUtente))
//    			.toList();
//    }
    //ORIGINALE
//    public List<AnagraficaDTO> findListaAnagrafica(List<Integer> filtroFonteId,
//                                                   List<String> descrizioniStato,
//                                                   List<String> sezione,
//                                                   Boolean insInSorveglianza,
//                                                   String tipoElencoInail,
//                                                   List<Integer> assistenzaAslId,
//                                                   String codiceFiscale,
//                                                   String cognome,
//                                                   String nome,
//                                                   String cognomeLettDa,
//                                                   String cognomeLettA,
//                                                   LocalDate nascitaData,
//                                                   Boolean azzeraContatoreProssimoStep,
//                                                   String profiloUtente,
//                                                   AuditLogRequest auditLogRequest)  {
//
//
//    	//DE Aggiungo questo boolean che fa da switch sulla decrypt  in line di nome e cognome cifrato
//    	boolean ricercaPerNominativo = (nome != null && !nome.isBlank())  || (cognome != null && !cognome.isBlank()||cognomeLettA != null && !cognomeLettA.isBlank())  || (cognomeLettDa != null && !cognomeLettDa.isBlank());
//
//        // non serve (in questo caso) eseguire la query con filtri su campi criptati quindi utilizzo quella fast
//		if (!ricercaPerNominativo) {
//		    return findListaAnagraficaFast(
//		            filtroFonteId,
//		            descrizioniStato,
//		            sezione,
//		            insInSorveglianza,
//		            tipoElencoInail,
//		            assistenzaAslId,
//		            codiceFiscale,
//		            cognomeLettDa,
//		            cognomeLettA,
//		            nascitaData,
//		            azzeraContatoreProssimoStep,
//		            profiloUtente,
//		            auditLogRequest
//		    );
//		}
//        //DE FINE
// 
// 
//		
//		
//        Condition condDescrizioneStato = noCondition();
//        if (descrizioniStato != null && !descrizioniStato.isEmpty()) {
//            condDescrizioneStato = REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.in(descrizioniStato);
//        }
//
//        Condition condSezione = noCondition();
//        if (sezione != null && !sezione.isEmpty()) {
//            condSezione = REEA_T_REGISTRO.SEZIONE.in(sezione);
//        }
//
//        Condition condInseritoInSorveglianza = noCondition();
//        if (insInSorveglianza != null) {
//            condInseritoInSorveglianza = REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.eq(insInSorveglianza);
//        }
//
//        Condition condTipoElencoInail = noCondition();
//        if (tipoElencoInail != null && !tipoElencoInail.isEmpty()) {
//            condTipoElencoInail = REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(tipoElencoInail.toUpperCase());
//        }
//
//        Condition condAssistenzaAsl = noCondition();
//        Condition condAssistenzaAslStorico = noCondition();
//        if (assistenzaAslId != null && !assistenzaAslId.isEmpty()) {
//            condAssistenzaAsl = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//            condAssistenzaAslStorico = REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//        }
//
//        Condition condCodiceFiscaleT = noCondition();
//        Condition condCodiceFiscaleS = noCondition();
//        if (codiceFiscale != null && !codiceFiscale.isEmpty()) {
//            condCodiceFiscaleT = REEA_T_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
//            condCodiceFiscaleS = REEA_S_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
//        }
//        
//        Condition condNascitaDataT = noCondition();
//        Condition condNascitaDataS = noCondition();
//        if (nascitaData != null) {
//            condNascitaDataT = REEA_T_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//            condNascitaDataS = REEA_S_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//        }
//
//        Condition condGlobale = condDescrizioneStato
//                .and(condSezione)
//                .and(condInseritoInSorveglianza)
//                .and(condTipoElencoInail)
//                .and(condAssistenzaAsl)
//                .and(condCodiceFiscaleT)
//                .and(condNascitaDataT);
//
//        Condition condGlobaleStorico = condDescrizioneStato
//                .and(condSezione)
//                .and(condInseritoInSorveglianza)
//                .and(condTipoElencoInail)
//                .and(condAssistenzaAslStorico)
//                .and(condCodiceFiscaleS)
//                .and(condNascitaDataS);
//
//        Condition condFonteGlobaleT = noCondition();
//        if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
//            condFonteGlobaleT = REEA_T_SOGGETTO.FONTE_ID.in(filtroFonteId)
//                    .or(REEA_T_SOGGETTO.SOGGETTO_ID.in(
//                            dsl.selectDistinct(REEA_S_SOGGETTO.SOGGETTO_ID)
//                               .from(REEA_S_SOGGETTO)
//                               .where(REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId))
//                    ));
//        }
//
//        Condition condFonteGlobaleS = noCondition();
//        if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
//            condFonteGlobaleS = REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId);
//        }
//        
//        Select<Record> selectCorrente = dsl
//    	        .select(
//    	                DSL.val((String) null).as("ASL_AZIENDA_DESC"),
//    	                DSL.val((Integer) null).as("FONTE_ID"),
//    	                DSL.val((String) null).as("FONTE_DESC"),
//    	                DSL.val((LocalDateTime) null).as("DATA_MODIFICA"),
//    	                DSL.val((LocalDateTime) null).as("DATA_CREAZIONE"),
//    	                DSL.val((LocalDate) null).as("NASCITA_DATA"),
//    	                DSL.val((Integer) null).as("SOGGETTO_ID"),
//    	                DSL.val((Integer) null).as("VERSIONE_NUMERO"),
//    	                DSL.val((LocalDateTime) null).as("ADESIONE_DATA"),
//    	                DSL.val((LocalDateTime) null).as("PRESENTAZIONE_ISTANZA_DATA"),
//    	                DSL.val((String) null).as("NOME_CHIARO"),
//    	                DSL.val((String) null).as("COGNOME_CHIARO"),
//    	                DSL.val((String) null).as("SESSO"),
//    	                DSL.val((String) null).as("CODICE_FISCALE"),
//    	                DSL.val((Integer) null).as("DOMICILIO_ASL_ID"),
//    	                DSL.val((Integer) null).as("RESIDENZA_ASL_ID"),
//    	                DSL.val((Integer) null).as("ASSISTENZA_ASL_ID"),
//    	                DSL.val((Integer) null).as("SEZIONE"),
//    	                DSL.val((Boolean) null).as("INSERITO_IN_SORVEGLIANZA"),
//    	                DSL.val((String) null).as("TIPO_ELENCO_INAIL"),
//    	                DSL.val((String) null).as("SOGGETTO_STATO_NOTE"),
//    	                DSL.val((Integer) null).as("REGISTRO_ID"),
//    	                DSL.val((String) null).as("SOGGETTO_STATO_DESC"),
//    	                DSL.val((String) null).as("TELEFONO_AURA"),
//    	                DSL.val((String) null).as("EMAIL_AURA"),
//    	                DSL.val((Integer) null).as("SOGGETTO_STATO_PRECEDENTE_ID")
//    	        )
//    	        .where(DSL.falseCondition());
//    	
//    	if ("REEA_OP_SPRESAL".equals(profiloUtente)
//    	        || "REEA_OP_CRPT".equals(profiloUtente)
//    	        || "REEA_OP_CSI".equals(profiloUtente)
//    	        || "REEA_OP_CRPT_PSEUDO".equals(profiloUtente)
//    	        || "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
//    	        || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
//    	        || "REEA_OP_CSI_PSEUDO".equals(profiloUtente)) {
//
//    		boolean includiSenzaAdesione = "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente) || "REEA_OP_CRPT".equals(profiloUtente);
//
//        selectCorrente = dsl
//                .selectDistinct(
//                        REEA_D_ASL.ASL_AZIENDA_DESC.as("ASL_AZIENDA_DESC"),
//                        REEA_T_SOGGETTO.FONTE_ID.as("FONTE_ID"),
//                        REEA_D_FONTE.FONTE_DESC.as("FONTE_DESC"),
//                        REEA_T_SOGGETTO.DATA_MODIFICA.as("DATA_MODIFICA"),
//                        REEA_T_SOGGETTO.DATA_CREAZIONE.as("DATA_CREAZIONE"),
//                        REEA_T_SOGGETTO.NASCITA_DATA.as("NASCITA_DATA"),
//                        REEA_T_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
//                        REEA_T_SOGGETTO.VERSIONE_NUMERO.as("VERSIONE_NUMERO"),
//                        REEA_T_ADESIONE.ADESIONE_DATA.as("ADESIONE_DATA"),
//                        REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA.as("PRESENTAZIONE_ISTANZA_DATA"),
//
//                  
//                        DSL.when(REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
//                                field("pgp_sym_decrypt({0}, {1})", String.class,
//                                        REEA_T_SOGGETTO.NOME_CIFRATO,
//                                        val("16<odcc8!"))
//                        ).otherwise((String) null).as("NOME_CHIARO"),
//                        
//
//
//
//                        DSL.when(REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//                                field("pgp_sym_decrypt({0}, {1})", String.class,
//                                        REEA_T_SOGGETTO.COGNOME_CIFRATO,
//                                        val("16<odcc8!"))
//                        ).otherwise((String) null).as("COGNOME_CHIARO"),
//
//                        REEA_T_SOGGETTO.SESSO.as("SESSO"),
//                        REEA_T_SOGGETTO.CODICE_FISCALE.as("CODICE_FISCALE"),
//                        REEA_T_SOGGETTO.DOMICILIO_ASL_ID.as("DOMICILIO_ASL_ID"),
//                        REEA_T_SOGGETTO.RESIDENZA_ASL_ID.as("RESIDENZA_ASL_ID"),
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("ASSISTENZA_ASL_ID"),
//                        REEA_T_REGISTRO.SEZIONE.as("SEZIONE"),
//                        REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("INSERITO_IN_SORVEGLIANZA"),
//                        REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("TIPO_ELENCO_INAIL"),
//                        REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE.as("SOGGETTO_STATO_NOTE"),
//                        REEA_T_REGISTRO.REGISTRO_ID.as("REGISTRO_ID"),
//                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("SOGGETTO_STATO_DESC"),
//
//                        DSL.when(REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
//                                field("pgp_sym_decrypt({0}, {1})", String.class,
//                                        REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO,
//                                        val("16<odcc8!"))
//                        ).otherwise((String) null).as("TELEFONO_AURA"),
//
//                        DSL.when(REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
//                                field("pgp_sym_decrypt({0}, {1})", String.class,
//                                        REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA,
//                                        val("16<odcc8!"))
//                        ).otherwise((String) null).as("EMAIL_AURA"),
//
//                        DSL.field(
//                                DSL.select(REEA_S_SOGGETTO.SOGGETTO_STATO_ID)
//                                        .from(REEA_S_SOGGETTO)
//                                        .where(REEA_S_SOGGETTO.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
//                                        .and(REEA_S_SOGGETTO.DATA_CANCELLAZIONE.isNull())
//                                        .orderBy(REEA_S_SOGGETTO.DATA_MODIFICA.desc())
//                                        .limit(1)
//                        ).as("SOGGETTO_STATO_PRECEDENTE_ID"),
//                        
//                        // MM AGGIUNTA DELLE COLONNE HASH
//        				REEA_T_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
//        				REEA_T_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
//        				REEA_T_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
//        				REEA_T_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
//        				REEA_T_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
//        				REEA_T_SOGGETTO.NOME_HASH.as("NOME_HASH")
//                )
//                .from(REEA_T_SOGGETTO)
//                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//                .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                        field("reea.hmac_soggetto_id({0}, {1})",
//                                String.class,
//                                REEA_T_SOGGETTO.SOGGETTO_ID,
//                                val("16<odcc8!"))))
//                .join(REEA_D_SOGGETTO_STATO)
//                .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//                .leftJoin(REEA_T_ADESIONE)
//                .on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID)
//                        .and(REEA_T_ADESIONE.SOGGETTO_ID.isNotNull()))
//                .where(condGlobale.and(condFonteGlobaleT))
//                .and(includiSenzaAdesione ? noCondition() : REEA_T_ADESIONE.SOGGETTO_ID.isNotNull());
//    	}
//    	
//    	 Select<Record> selectStorico = null;
//         
//         boolean includeStorico =
//      	        "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
//      	        || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
//      	        || "REEA_OP_SPRESAL".equals(profiloUtente)
//      	        || "REEA_OP_CSI_PSEUDO".equals(profiloUtente);
//         
//         if (includeStorico) {
//         selectStorico = dsl
//                .selectDistinct(
//                        REEA_D_ASL.ASL_AZIENDA_DESC.as("ASL_AZIENDA_DESC"),
//                        REEA_S_SOGGETTO.FONTE_ID.as("FONTE_ID"),
//                        REEA_D_FONTE.FONTE_DESC.as("FONTE_DESC"),
//                        REEA_S_SOGGETTO.DATA_MODIFICA.as("DATA_MODIFICA"),
//                        REEA_S_SOGGETTO.DATA_CREAZIONE.as("DATA_CREAZIONE"),
//                        REEA_S_SOGGETTO.NASCITA_DATA.as("NASCITA_DATA"),
//                        REEA_S_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
//                        REEA_S_SOGGETTO.VERSIONE_NUMERO.as("VERSIONE_NUMERO"),
//                        DSL.val((LocalDateTime) null).as("ADESIONE_DATA"),
//                        DSL.val((LocalDateTime) null).as("PRESENTAZIONE_ISTANZA_DATA"),
//
//                        DSL.when(
//                                REEA_S_SOGGETTO.NOME_CIFRATO.isNotNull(),
//                                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                                        REEA_S_SOGGETTO.NOME_CIFRATO,
//                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("NOME_CHIARO"),
//
//                        DSL.when(
//                                REEA_S_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//                                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                                        REEA_S_SOGGETTO.COGNOME_CIFRATO,
//                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("COGNOME_CHIARO"),
//
//                        REEA_S_SOGGETTO.SESSO.as("SESSO"),
//                        REEA_S_SOGGETTO.CODICE_FISCALE.as("CODICE_FISCALE"),
//                        REEA_S_SOGGETTO.DOMICILIO_ASL_ID.as("DOMICILIO_ASL_ID"),
//                        REEA_S_SOGGETTO.RESIDENZA_ASL_ID.as("RESIDENZA_ASL_ID"),
//                        REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.as("ASSISTENZA_ASL_ID"),
//                        REEA_T_REGISTRO.SEZIONE.as("SEZIONE"),
//                        REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("INSERITO_IN_SORVEGLIANZA"),
//                        REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("TIPO_ELENCO_INAIL"),
//                        REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE.as("SOGGETTO_STATO_NOTE"),
//                        REEA_T_REGISTRO.REGISTRO_ID.as("REGISTRO_ID"),
//                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("SOGGETTO_STATO_DESC"),
//                        DSL.val((String) null).as("TELEFONO_AURA"),
//                        DSL.val((String) null).as("EMAIL_AURA"),
//                        DSL.val((Integer) null).as("SOGGETTO_STATO_PRECEDENTE_ID"),
//                        
//                        // MM AGGIUNTA DELLE COLONNE HASH
//        				REEA_S_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
//        				REEA_S_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
//        				REEA_S_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
//        				REEA_S_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
//        				REEA_S_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
//        				REEA_S_SOGGETTO.NOME_HASH.as("NOME_HASH")
//                )
//                .from(REEA_S_SOGGETTO)
//                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
//                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID))
//                .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                        field("reea.hmac_soggetto_id({0}, {1})",
//                                String.class,
//                                REEA_S_SOGGETTO.SOGGETTO_ID,
//                                val("16<odcc8!"))))
//                .join(REEA_D_SOGGETTO_STATO)
//                .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_S_SOGGETTO.SOGGETTO_STATO_ID))
//                .where(condGlobaleStorico
//                        .and(condFonteGlobaleS)
//                        .and(REEA_S_SOGGETTO.SOGGETTO_ID.notIn(
//                                dsl.selectDistinct(REEA_T_SOGGETTO.SOGGETTO_ID).from(REEA_T_SOGGETTO)
//                        )));
//         }
////        var unionTable = selectCorrente
////                .unionAll(selectStorico)
////                .asTable("q");
//         Table<?> unionTable;
//         if (includeStorico && selectStorico != null) {
//             unionTable = selectCorrente.unionAll(selectStorico).asTable("q");
//         } else {
//             unionTable = selectCorrente.asTable("q");
//         }
//
//      //DE METTERE QUI LA CONDIZIONE DI RICERCA PER COGNOME E NOME TRAMITE GLI HASH
//        
//        // MM disabilito le condizioni che non utilizzano le colonne HASH
//        //Condition condOuterCognome = noCondition();
//        //if (cognome != null && !cognome.isEmpty()) {
//        //    condOuterCognome = DSL.field(DSL.name("q", "COGNOME_CHIARO"), String.class)
//        //            .likeIgnoreCase( cognome + "%");
//        //}
//
//        //Condition condOuterNome = noCondition();
//        //if (nome != null && !nome.isEmpty()) {
//        //    condOuterNome = DSL.field(DSL.name("q", "NOME_CHIARO"), String.class)
//        //            .likeIgnoreCase( nome + "%");
//        //}
//
//        //Condition condCognomeLettDa = noCondition();
//        //if (cognomeLettDa != null && !cognomeLettDa.isEmpty()) {
//        //    condCognomeLettDa = DSL.upper(DSL.left(
//        //            DSL.field(DSL.name("q", "COGNOME_CHIARO"), String.class), 1))
//        //            .greaterOrEqual(cognomeLettDa.trim().toUpperCase());
//        //}
//
//        //Condition condCognomeLettA = noCondition();
//        //if (cognomeLettA != null && !cognomeLettA.isEmpty()) {
//        //    condCognomeLettA = DSL.upper(DSL.left(
//        //            DSL.field(DSL.name("q", "COGNOME_CHIARO"), String.class), 1))
//        //            .lessOrEqual(cognomeLettA.trim().toUpperCase());
//        //}
//        
//        // MM inserite nuove condizioni che utilizzando le colonne HASH
//        // -----------------------------
//        // Filtro COGNOME (HMAC)
//        // -----------------------------
//        Condition condOuterCognome = noCondition();
//        if (cognome != null && !cognome.isEmpty()) {
//
//        	int len = cognome.length();
//        	String value = cognome.trim().toUpperCase();
//
//        	Field<byte[]> hmacValue = DSL.function(
//        			"hmac",
//        			SQLDataType.BLOB,
//        			DSL.val(value, SQLDataType.VARCHAR),
//        			DSL.inline("Porcatrota2000!"),
//        			DSL.inline("sha256")
//        			);
//
//        	if (len == 1) {
//        		condOuterCognome = DSL.field(DSL.name("q", "COGNOME_HASH_1CHAR"), byte[].class)
//        				.eq(hmacValue);
//        	} else if (len == 2) {
//        		condOuterCognome = DSL.field(DSL.name("q", "COGNOME_HASH_2CHAR"), byte[].class)
//        				.eq(hmacValue);
//        	} else {
//        		condOuterCognome = DSL.field(DSL.name("q", "COGNOME_HASH"), byte[].class)
//        				.eq(hmacValue);
//        	}
//        }
//
//        // -----------------------------
//        // Filtro NOME (HMAC)
//        // -----------------------------
//        Condition condOuterNome = noCondition();
//        if (nome != null && !nome.isEmpty()) {
//
//        	int len = nome.length();
//        	String value = nome.trim().toUpperCase();
//
//        	Field<byte[]> hmacValue = DSL.function(
//        			"hmac",
//        			SQLDataType.BLOB,
//        			DSL.val(value, SQLDataType.VARCHAR),
//        			DSL.inline("Porcatrota2000!"),
//        			DSL.inline("sha256")
//        			);
//
//        	if (len == 1) {
//        		condOuterNome = DSL.field(DSL.name("q", "NOME_HASH_1CHAR"), byte[].class)
//        				.eq(hmacValue);
//        	} else if (len == 2) {
//        		condOuterNome = DSL.field(DSL.name("q", "NOME_HASH_2CHAR"), byte[].class)
//        				.eq(hmacValue);
//        	} else {
//        		condOuterNome = DSL.field(DSL.name("q", "NOME_HASH"), byte[].class)
//        				.eq(hmacValue);
//        	}
//        }
//
//	    // -----------------------------
//	    // Filtro alfabetico COGNOME DA - A (range di lettere)
//	    // -----------------------------
//	    Condition condCognomeRange = noCondition();
//	
//	    if (cognomeLettDa != null && !cognomeLettDa.isEmpty()
//	            && cognomeLettA != null && !cognomeLettA.isEmpty()) {
//	
//	        // I due caratteri sono sempre singoli e maiuscoli
//	        char da = cognomeLettDa.trim().charAt(0);
//	        char a  = cognomeLettA.trim().charAt(0);
//	
//	        // Costruisco la lista delle lettere comprese nel range
//	        List<String> lettere = new ArrayList<>();
//	        for (char c = da; c <= a; c++) {
//	            lettere.add(String.valueOf(c));
//	        }
//	
//	        // Costruisco la lista degli HMAC corrispondenti
//	        List<Field<byte[]>> hmacList = new ArrayList<>();
//	        for (String letter : lettere) {
//	            Field<byte[]> hmacLetter = DSL.function(
//	                    "hmac",
//	                    SQLDataType.BLOB,
//	                    DSL.val(letter, SQLDataType.VARCHAR),
//	                    DSL.inline("Porcatrota2000!"),
//	                    DSL.inline("sha256")
//	            );
//	            hmacList.add(hmacLetter);
//	        }
//	
//	        // Costruisco la condizione IN (...)
//	        condCognomeRange = DSL.field(DSL.name("q", "COGNOME_HASH_1CHAR"), byte[].class)
//	                .in(hmacList);
//	    }
//        // MM fine
//        
//        return dsl.selectFrom(unionTable)
//        		// MM nella where inserisco la nuova condizione condCognomeRange
//                //.where(condOuterCognome.and(condOuterNome).and(condCognomeLettDa).and(condCognomeLettA))
//        		.where(
//        	            condOuterCognome
//        	            .and(condOuterNome)
//        	            .and(condCognomeRange)
//        	        )
//                .orderBy(
//                        field(DSL.name("q", "COGNOME_CHIARO")).asc(),
//                        field(DSL.name("q", "NOME_CHIARO")).asc()
//                )
//                .limit(1000)
//                .fetch()
//                .stream()
//                .map(r -> {
//                    AnagraficaDTO row = new AnagraficaDTO();
//
//                    Integer soggettoIdInt = r.get("SOGGETTO_ID", Integer.class);
//                    Long soggettoId = soggettoIdInt != null ? soggettoIdInt.longValue() : null;
//                    row.setSoggettoId(soggettoId);
//
//                    row.setFonteId(r.get("FONTE_ID", Integer.class));
//
//                    String fonteDesc = r.get("FONTE_DESC", String.class);
//                    row.setDescrizioneFonte(List.of(fonteDesc != null ? fonteDesc : "PROVA"));
//
//                    RegistroDTO registro = RegistroUtils.ensureRegistro(row);
//                    registro.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get("DATA_MODIFICA", LocalDateTime.class)));
//                    registro.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get("DATA_CREAZIONE", LocalDateTime.class)));
//                    registro.setVersioneNumero(r.get("VERSIONE_NUMERO", Integer.class));
//                    registro.setRegistroId(r.get("REGISTRO_ID", Integer.class));
//                    registro.setSezione(r.get("SEZIONE", Integer.class));
//                    registro.setInseritoInSorveglianza(r.get("INSERITO_IN_SORVEGLIANZA", Boolean.class));
//                    registro.setTipoElencoInail(r.get("TIPO_ELENCO_INAIL", String.class));
//
//                    row.setNascitaData(r.get("NASCITA_DATA", LocalDate.class));
//
//                    String nomeChiaro = r.get("NOME_CHIARO", String.class);
//                    row.setNome(nomeChiaro != null ? nomeChiaro.toUpperCase() : "");
//
//                    String cognomeChiaro = r.get("COGNOME_CHIARO", String.class);
//                    row.setCognome(cognomeChiaro != null ? cognomeChiaro.toUpperCase() : "");
//
//                    row.setSesso(r.get("SESSO", String.class));
//                    row.setCodiceFiscale(r.get("CODICE_FISCALE", String.class));
//                    row.setDomicilioAslId(r.get("DOMICILIO_ASL_ID", Integer.class));
//                    row.setResidenzaAslId(r.get("RESIDENZA_ASL_ID", Integer.class));
//
//                    Integer assistenzaId = r.get("ASSISTENZA_ASL_ID", Integer.class);
//                    row.setAssistenzaAslId(assistenzaId != null ? String.format("%06d", assistenzaId) : null);
//
//                    row.setDescrizioneAslCompetenza(r.get("ASL_AZIENDA_DESC", String.class));
//                    row.setTelefonoAura(r.get("TELEFONO_AURA", String.class));
//                    row.setEmailAura(r.get("EMAIL_AURA", String.class));
//                    row.setDescrizioneStato(r.get("SOGGETTO_STATO_DESC", String.class));
//                    row.setSoggettoStatoNote(r.get("SOGGETTO_STATO_NOTE", String.class));
//                    row.setSoggettoStatoPrecedenteId(r.get("SOGGETTO_STATO_PRECEDENTE_ID", Integer.class));
//
//                    if (soggettoId != null) {
//                        row.setListaFonteByIdSoggetto(
//                                fonteRepository != null
//                                        ? fonteRepository.findListaFonteDescByIdSoggetto(soggettoId)
//                                        : Collections.emptyList()
//                        );
//
//                        row.setListaEsenzione(
//                                esenzioneRepository != null
//                                        ? esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId)
//                                        : Collections.emptyList()
//                        );
//                    } else {
//                        row.setListaFonteByIdSoggetto(Collections.emptyList());
//                        row.setListaEsenzione(Collections.emptyList());
//                    }
//
//                    LocalDateTime adesioneData = r.get("ADESIONE_DATA", LocalDateTime.class);
//                    row.setAdesioneData(adesioneData != null ? adesioneData.toString() : null);
//
//                    LocalDateTime presentazione = r.get("PRESENTAZIONE_ISTANZA_DATA", LocalDateTime.class);
//                    row.setPresentazioneIstanzaData(
//                            presentazione != null ? presentazione.toLocalDate().toString() : null
//                    );
//
//                    return row;
//                })
////                .collect(Collectors.toList());
//                .filter(dto -> dto.getSoggettoId() != null && isValidoPerExport(dto.getSoggettoId().intValue(), profiloUtente))
//                .toList();
//    }
    
    
    private List<AnagraficaDTO> findListaAnagraficaFast(
            List<Integer> filtroFonteId,
            List<String> descrizioniStato,
            List<String> sezione,
            Boolean insInSorveglianza,
            String tipoElencoInail,
            List<Integer> assistenzaAslId,
            String codiceFiscale,
            String cognomeLettDa,
            String cognomeLettA,
            LocalDate nascitaData,
            Boolean azzeraContatoreProssimoStep,
            String profiloUtente,
            AuditLogRequest auditLogRequest) {

        /*
         * Normalizzazione del filtro fonte.
         */
        if (filtroFonteId == null || filtroFonteId.isEmpty()) {
            filtroFonteId = List.of(1, 2, 3, 4, 5);
        } else {
            filtroFonteId = new ArrayList<>(filtroFonteId);
        }

        /*
         * Regola applicativa esistente.
         */
//        if (filtroFonteId.contains(2)) {
//            azzeraContatoreProssimoStep =
//                    Boolean.TRUE;
//        }

        boolean stepAttivo =
                azzeraContatoreProssimoStep != null;

        Set<String> profiliCorrente = Set.of(
                "REEA_OP_SPRESAL",
                "REEA_OP_CRPT",
                "REEA_OP_CSI",
                "REEA_OP_CRPT_PSEUDO",
                "REEA_OP_SPRESAL_PSEUDO",
                "REEA_OP_EPI_PSEUDO",
                "REEA_OP_CSI_PSEUDO"
        );

        Set<String> profiliStorico = Set.of(
                "REEA_OP_SPRESAL_PSEUDO",
                "REEA_OP_EPI_PSEUDO",
                "REEA_OP_SPRESAL",
                "REEA_OP_CSI_PSEUDO"
        );

        boolean includeCorrente =
                profiliCorrente.contains(profiloUtente);

        boolean includeStorico =
                profiliStorico.contains(profiloUtente);

        if (!includeCorrente) {
            return Collections.emptyList();
        }

        /*
         * Condizioni dinamiche.
         */
        Condition condDescrizioneStato =
                noCondition();

        if (descrizioniStato != null
                && !descrizioniStato.isEmpty()) {
            condDescrizioneStato =
                    REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
                            .in(descrizioniStato);
        }

        Condition condSezione =
                noCondition();

        if (sezione != null
                && !sezione.isEmpty()) {
            condSezione =
                    REEA_T_REGISTRO.SEZIONE.in(sezione);
        }

        Condition condInseritoInSorveglianza =
                noCondition();

        if (insInSorveglianza != null) {
            condInseritoInSorveglianza =
                    REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA
                            .eq(insInSorveglianza);
        }

        Condition condTipoElencoInail =
                noCondition();

        if (tipoElencoInail != null
                && !tipoElencoInail.isBlank()) {
            condTipoElencoInail =
                    REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(
                            tipoElencoInail
                                    .trim()
                                    .toUpperCase(Locale.ROOT)
                    );
        }

        Condition condAssistenzaAsl =
                noCondition();

        Condition condAssistenzaAslStorico =
                noCondition();

        if (assistenzaAslId != null
                && !assistenzaAslId.isEmpty()) {

            condAssistenzaAsl =
                    REEA_T_SOGGETTO.ASSISTENZA_ASL_ID
                            .in(assistenzaAslId);

            condAssistenzaAslStorico =
                    REEA_S_SOGGETTO.ASSISTENZA_ASL_ID
                            .in(assistenzaAslId);
        }

        Condition condCodiceFiscaleT =
                noCondition();

        Condition condCodiceFiscaleS =
                noCondition();

        if (codiceFiscale != null
                && !codiceFiscale.isBlank()) {

            String codiceFiscaleNormalizzato =
                    codiceFiscale.trim();

            condCodiceFiscaleT =
                    REEA_T_SOGGETTO.CODICE_FISCALE
                            .equalIgnoreCase(
                                    codiceFiscaleNormalizzato
                            );

            condCodiceFiscaleS =
                    REEA_S_SOGGETTO.CODICE_FISCALE
                            .equalIgnoreCase(
                                    codiceFiscaleNormalizzato
                            );
        }

        Condition condNascitaDataT =
                noCondition();

        Condition condNascitaDataS =
                noCondition();

        if (nascitaData != null) {
            condNascitaDataT =
                    REEA_T_SOGGETTO.NASCITA_DATA
                            .eq(nascitaData);

            condNascitaDataS =
                    REEA_S_SOGGETTO.NASCITA_DATA
                            .eq(nascitaData);
        }

        /*
         * Paging progressivo.
         *
         * true  -> ignora il valore precedente;
         * false -> riparte dall'ultimo REGISTRO_ID salvato;
         * null  -> nessun paging progressivo.
         */
        Condition condRegistroIdPaging =
                noCondition();

        if (Boolean.FALSE.equals(
                azzeraContatoreProssimoStep
        )) {
            Integer lastRegistroId =
                    getUltimoRegistroIdProcessato(
                            auditLogRequest
                    );

            if (lastRegistroId != null) {
                condRegistroIdPaging =
                        REEA_T_REGISTRO.REGISTRO_ID
                                .gt(lastRegistroId);
            }
        }

        Condition condGlobale =
                condDescrizioneStato
                        .and(condSezione)
                        .and(condInseritoInSorveglianza)
                        .and(condTipoElencoInail)
                        .and(condAssistenzaAsl)
                        .and(condCodiceFiscaleT)
                        .and(condNascitaDataT)
                        .and(condRegistroIdPaging);

        Condition condGlobaleStorico =
                condDescrizioneStato
                        .and(condSezione)
                        .and(condInseritoInSorveglianza)
                        .and(condTipoElencoInail)
                        .and(condAssistenzaAslStorico)
                        .and(condCodiceFiscaleS)
                        .and(condNascitaDataS);

        /*
         * Filtro fonte corrente.
         */
        Condition condFonteGlobaleT =
                REEA_T_SOGGETTO.FONTE_ID
                        .in(filtroFonteId)
                        .orExists(
                                dsl.selectOne()
                                        .from(REEA_S_SOGGETTO)
                                        .where(
                                                REEA_S_SOGGETTO
                                                        .SOGGETTO_ID
                                                        .eq(
                                                                REEA_T_SOGGETTO
                                                                        .SOGGETTO_ID
                                                        )
                                        )
                                        .and(
                                                REEA_S_SOGGETTO
                                                        .FONTE_ID
                                                        .in(
                                                                filtroFonteId
                                                        )
                                        )
                        );

        Condition condFonteGlobaleS =
                REEA_S_SOGGETTO.FONTE_ID
                        .in(filtroFonteId);

        /*
         * Limiti configurabili.
         */
        Integer limiteRecordStepCaricamentoAdesioni =
                Optional.ofNullable(
                        dsl.select(
                                REEA_C_PARAMETRO
                                        .PARAMETRO_VALORE
                        )
                        .from(REEA_C_PARAMETRO)
                        .where(
                                REEA_C_PARAMETRO.PARAMETRO_COD
                                        .eq(
                                                "NumeroRecordRicercaAdesioni"
                                        )
                        )
                        .fetchOneInto(Integer.class)
                )
                .orElse(1000);

        Integer limiteRecord =
                Optional.ofNullable(
                        dsl.select(
                                REEA_C_PARAMETRO
                                        .PARAMETRO_VALORE
                        )
                        .from(REEA_C_PARAMETRO)
                        .where(
                                REEA_C_PARAMETRO.PARAMETRO_COD
                                        .eq(
                                                "NumeroRecordRicercaGenerica"
                                        )
                        )
                        .fetchOneInto(Integer.class)
                )
                .orElse(1000);

        /*
         * Query preliminare per il paging.
         */
        var basePageQuery =
                dsl.select(
                        REEA_T_SOGGETTO.SOGGETTO_ID,
                        REEA_T_REGISTRO.REGISTRO_ID
                )
                .from(REEA_T_SOGGETTO)
                .join(REEA_T_REGISTRO)
                    .on(
                            REEA_T_REGISTRO
                                    .SOGGETTO_ID_HMAC
                                    .eq(
                                            field(
                                                    "reea.hmac_soggetto_id({0}, {1})",
                                                    String.class,
                                                    REEA_T_SOGGETTO
                                                            .SOGGETTO_ID,
                                                    val("16<odcc8!")
                                            )
                                    )
                    )
                .join(REEA_D_SOGGETTO_STATO)
                    .on(
                            REEA_D_SOGGETTO_STATO
                                    .SOGGETTO_STATO_ID
                                    .eq(
                                            REEA_T_SOGGETTO
                                                    .SOGGETTO_STATO_ID
                                    )
                    )
                .join(REEA_D_FONTE)
                    .on(
                            REEA_D_FONTE.FONTE_ID
                                    .eq(
                                            REEA_T_SOGGETTO
                                                    .FONTE_ID
                                    )
                    )
                .leftJoin(REEA_D_ASL)
                    .on(
                            REEA_D_ASL.ASL_ID
                                    .eq(
                                            REEA_T_SOGGETTO
                                                    .ASSISTENZA_ASL_ID
                                    )
                    )
                .where(
                        condGlobale
                                .and(condFonteGlobaleT)
                )
                .orderBy(
                        REEA_T_REGISTRO.REGISTRO_ID.asc()
                );

        /*
         * Recupero della pagina.
         */
        List<Integer> idsPagina =
                Collections.emptyList();

        if (stepAttivo) {
            int limiteStep =
                    Boolean.TRUE.equals(
                            azzeraContatoreProssimoStep
                    )
                            ? limiteRecordStepCaricamentoAdesioni
                            : limiteRecord;

            Result<?> page =
                    basePageQuery
                            .limit(limiteStep)
                            .fetch();

            idsPagina =
                    page.getValues(
                            REEA_T_SOGGETTO.SOGGETTO_ID
                    );

            Integer nuovoLastRegistroId =
                    page.isEmpty()
                            ? null
                            : page.get(
                                    page.size() - 1
                            ).get(
                                    REEA_T_REGISTRO.REGISTRO_ID
                            );

            if (nuovoLastRegistroId != null) {
                salvaUltimoRegistroIdProcessato(
                        nuovoLastRegistroId,
                        auditLogRequest
                );
            }

            if (idsPagina.isEmpty()) {
                return Collections.emptyList();
            }
        }

        Condition condPaginaCorrente =
                stepAttivo
                        ? REEA_T_SOGGETTO.SOGGETTO_ID
                                .in(idsPagina)
                        : noCondition();

        Condition condPaginaStorico =
                stepAttivo
                        ? REEA_S_SOGGETTO.SOGGETTO_ID
                                .in(idsPagina)
                        : noCondition();

        /*
         * Query corrente.
         */
        Select<Record> selectCorrente =
                dsl.selectDistinct(
                        REEA_D_ASL.ASL_AZIENDA_DESC
                                .as("ASL_AZIENDA_DESC"),
                        REEA_T_SOGGETTO.FONTE_ID
                                .as("FONTE_ID"),
                        REEA_D_FONTE.FONTE_DESC
                                .as("FONTE_DESC"),
                        REEA_T_SOGGETTO.DATA_MODIFICA
                                .as("DATA_MODIFICA"),
                        REEA_T_SOGGETTO.DATA_CREAZIONE
                                .as("DATA_CREAZIONE"),
                        REEA_T_SOGGETTO.NASCITA_DATA
                                .as("NASCITA_DATA"),
                        REEA_T_SOGGETTO.SOGGETTO_ID
                                .as("SOGGETTO_ID"),
                        REEA_T_SOGGETTO.VERSIONE_NUMERO
                                .as("VERSIONE_NUMERO"),
                        REEA_T_ADESIONE.ADESIONE_DATA
                                .as("ADESIONE_DATA"),
                        REEA_T_SOGGETTO
                                .PRESENTAZIONE_ISTANZA_DATA
                                .as(
                                        "PRESENTAZIONE_ISTANZA_DATA"
                                ),
                        field(
                                "pgp_sym_decrypt({0}, {1})",
                                String.class,
                                REEA_T_SOGGETTO
                                        .NOME_CIFRATO,
                                val("16<odcc8!")
                        ).as("NOME_CHIARO"),
                        field(
                                "pgp_sym_decrypt({0}, {1})",
                                String.class,
                                REEA_T_SOGGETTO
                                        .COGNOME_CIFRATO,
                                val("16<odcc8!")
                        ).as("COGNOME_CHIARO"),
                        REEA_T_SOGGETTO.SESSO
                                .as("SESSO"),
                        REEA_T_SOGGETTO.CODICE_FISCALE
                                .as("CODICE_FISCALE"),
                        REEA_T_SOGGETTO.DOMICILIO_ASL_ID
                                .as("DOMICILIO_ASL_ID"),
                        REEA_T_SOGGETTO.RESIDENZA_ASL_ID
                                .as("RESIDENZA_ASL_ID"),
                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID
                                .as("ASSISTENZA_ASL_ID"),
                        REEA_T_REGISTRO.SEZIONE
                                .as("SEZIONE"),
                        REEA_T_REGISTRO
                                .INSERITO_IN_SORVEGLIANZA
                                .as(
                                        "INSERITO_IN_SORVEGLIANZA"
                                ),
                        REEA_T_REGISTRO
                                .TIPO_ELENCO_INAIL
                                .as("TIPO_ELENCO_INAIL"),
                        REEA_T_SOGGETTO
                                .SOGGETTO_STATO_NOTE
                                .as("SOGGETTO_STATO_NOTE"),
                        REEA_T_REGISTRO.REGISTRO_ID
                                .as("REGISTRO_ID"),
                        REEA_D_SOGGETTO_STATO
                                .SOGGETTO_STATO_DESC
                                .as("SOGGETTO_STATO_DESC"),
                        DSL.inline((String) null)
                                .as("TELEFONO_AURA"),
                        DSL.inline((String) null)
                                .as("EMAIL_AURA"),
                        DSL.field(
                                DSL.select(
                                        REEA_S_SOGGETTO
                                                .SOGGETTO_STATO_ID
                                )
                                .from(REEA_S_SOGGETTO)
                                .where(
                                        REEA_S_SOGGETTO
                                                .SOGGETTO_ID
                                                .eq(
                                                        REEA_T_SOGGETTO
                                                                .SOGGETTO_ID
                                                )
                                )
                                .and(
                                        REEA_S_SOGGETTO
                                                .DATA_CANCELLAZIONE
                                                .isNull()
                                )
                                .orderBy(
                                        REEA_S_SOGGETTO
                                                .DATA_MODIFICA
                                                .desc()
                                )
                                .limit(1)
                        ).as(
                                "SOGGETTO_STATO_PRECEDENTE_ID"
                        )
                )
                .from(REEA_T_SOGGETTO)
                .leftJoin(REEA_D_FONTE)
                    .on(
                            REEA_D_FONTE.FONTE_ID
                                    .eq(
                                            REEA_T_SOGGETTO
                                                    .FONTE_ID
                                    )
                    )
                .leftJoin(REEA_D_ASL)
                    .on(
                            REEA_D_ASL.ASL_ID
                                    .eq(
                                            REEA_T_SOGGETTO
                                                    .ASSISTENZA_ASL_ID
                                    )
                    )
                .join(REEA_T_REGISTRO)
                    .on(
                            REEA_T_REGISTRO
                                    .SOGGETTO_ID_HMAC
                                    .eq(
                                            field(
                                                    "reea.hmac_soggetto_id({0}, {1})",
                                                    String.class,
                                                    REEA_T_SOGGETTO
                                                            .SOGGETTO_ID,
                                                    val("16<odcc8!")
                                            )
                                    )
                    )
                .leftJoin(REEA_D_SOGGETTO_STATO)
                    .on(
                            REEA_D_SOGGETTO_STATO
                                    .SOGGETTO_STATO_ID
                                    .eq(
                                            REEA_T_SOGGETTO
                                                    .SOGGETTO_STATO_ID
                                    )
                    )
                .leftJoin(REEA_T_ADESIONE)
                    .on(
                            REEA_T_ADESIONE.SOGGETTO_ID
                                    .eq(
                                            REEA_T_SOGGETTO
                                                    .SOGGETTO_ID
                                    )
                    )
                .where(
                        condGlobale
                                .and(condFonteGlobaleT)
                                .and(condPaginaCorrente)
                );

        /*
         * Query storico.
         */
        Select<Record> selectStorico =
                null;

        if (includeStorico) {
            selectStorico =
                    dsl.selectDistinct(
                            DSL.castNull(
                                    REEA_D_ASL.ASL_AZIENDA_DESC
                                            .getDataType()
                            ).as("ASL_AZIENDA_DESC"),
                            REEA_S_SOGGETTO.FONTE_ID
                                    .as("FONTE_ID"),
                            REEA_D_FONTE.FONTE_DESC
                                    .as("FONTE_DESC"),
                            REEA_S_SOGGETTO.DATA_MODIFICA
                                    .as("DATA_MODIFICA"),
                            REEA_S_SOGGETTO.DATA_CREAZIONE
                                    .as("DATA_CREAZIONE"),
                            DSL.castNull(
                                    REEA_S_SOGGETTO.NASCITA_DATA
                                            .getDataType()
                            ).as("NASCITA_DATA"),
                            REEA_S_SOGGETTO.SOGGETTO_ID
                                    .as("SOGGETTO_ID"),
                            REEA_S_SOGGETTO.VERSIONE_NUMERO
                                    .as("VERSIONE_NUMERO"),
                            DSL.val((LocalDateTime) null)
                                    .as("ADESIONE_DATA"),
                            DSL.val((LocalDateTime) null)
                                    .as(
                                            "PRESENTAZIONE_ISTANZA_DATA"
                                    ),
                            DSL.val((String) null)
                                    .as("NOME_CHIARO"),
                            DSL.val((String) null)
                                    .as("COGNOME_CHIARO"),
                            DSL.val((String) null)
                                    .as("SESSO"),
                            DSL.val((String) null)
                                    .as("CODICE_FISCALE"),
                            DSL.val((Integer) null)
                                    .as("DOMICILIO_ASL_ID"),
                            DSL.val((Integer) null)
                                    .as("RESIDENZA_ASL_ID"),
                            DSL.val((Integer) null)
                                    .as("ASSISTENZA_ASL_ID"),
                            REEA_T_REGISTRO.SEZIONE
                                    .as("SEZIONE"),
                            REEA_T_REGISTRO
                                    .INSERITO_IN_SORVEGLIANZA
                                    .as(
                                            "INSERITO_IN_SORVEGLIANZA"
                                    ),
                            REEA_T_REGISTRO
                                    .TIPO_ELENCO_INAIL
                                    .as("TIPO_ELENCO_INAIL"),
                            REEA_S_SOGGETTO
                                    .SOGGETTO_STATO_NOTE
                                    .as("SOGGETTO_STATO_NOTE"),
                            REEA_T_REGISTRO.REGISTRO_ID
                                    .as("REGISTRO_ID"),
                            REEA_D_SOGGETTO_STATO
                                    .SOGGETTO_STATO_DESC
                                    .as("SOGGETTO_STATO_DESC"),
                            DSL.val((String) null)
                                    .as("TELEFONO_AURA"),
                            DSL.val((String) null)
                                    .as("EMAIL_AURA"),
                            DSL.val((Integer) null)
                                    .as(
                                            "SOGGETTO_STATO_PRECEDENTE_ID"
                                    )
                    )
                    .from(REEA_S_SOGGETTO)
                    .join(REEA_T_REGISTRO)
                        .on(
                                REEA_T_REGISTRO
                                        .SOGGETTO_ID_HMAC
                                        .eq(
                                                field(
                                                        "reea.hmac_soggetto_id({0}, {1})",
                                                        String.class,
                                                        REEA_S_SOGGETTO
                                                                .SOGGETTO_ID,
                                                        val("16<odcc8!")
                                                )
                                        )
                        )
                    .join(REEA_D_FONTE)
                        .on(
                                REEA_D_FONTE.FONTE_ID
                                        .eq(
                                                REEA_S_SOGGETTO
                                                        .FONTE_ID
                                        )
                        )
                    .leftJoin(REEA_D_ASL)
                        .on(
                                REEA_D_ASL.ASL_ID
                                        .eq(
                                                REEA_S_SOGGETTO
                                                        .ASSISTENZA_ASL_ID
                                        )
                        )
                    .join(REEA_D_SOGGETTO_STATO)
                        .on(
                                REEA_D_SOGGETTO_STATO
                                        .SOGGETTO_STATO_ID
                                        .eq(
                                                REEA_S_SOGGETTO
                                                        .SOGGETTO_STATO_ID
                                        )
                        )
                    .where(
                            condGlobaleStorico
                                    .and(condFonteGlobaleS)
                                    .and(
                                            REEA_S_SOGGETTO
                                                    .SOGGETTO_ID
                                                    .notIn(
                                                            dsl.selectDistinct(
                                                                    REEA_T_SOGGETTO
                                                                            .SOGGETTO_ID
                                                            )
                                                            .from(
                                                                    REEA_T_SOGGETTO
                                                            )
                                                    )
                                    )
                                    .and(condPaginaStorico)
                    );
        }

        /*
         * UNION tra corrente e storico.
         */
        Table<?> unionTable;

        if (selectStorico != null) {
            unionTable =
                    selectCorrente
                            .union(selectStorico)
                            .asTable("q");
        } else {
            unionTable =
                    selectCorrente.asTable("q");
        }

        /*
         * Query finale con un'unica Condition.
         */
        Condition condFiltroFinale =
                noCondition();

        if (cognomeLettDa != null
                && !cognomeLettDa.isBlank()) {

            String letteraDa =
                    cognomeLettDa
                            .trim()
                            .substring(0, 1)
                            .toUpperCase(Locale.ROOT);

            condFiltroFinale =
                    condFiltroFinale.and(
                            DSL.upper(
                                    DSL.left(
                                            DSL.field(
                                                    DSL.name(
                                                            "q",
                                                            "COGNOME_CHIARO"
                                                    ),
                                                    String.class
                                            ),
                                            1
                                    )
                            ).greaterOrEqual(letteraDa)
                    );
        }

        if (cognomeLettA != null
                && !cognomeLettA.isBlank()) {

            String letteraA =
                    cognomeLettA
                            .trim()
                            .substring(0, 1)
                            .toUpperCase(Locale.ROOT);

            condFiltroFinale =
                    condFiltroFinale.and(
                            DSL.upper(
                                    DSL.left(
                                            DSL.field(
                                                    DSL.name(
                                                            "q",
                                                            "COGNOME_CHIARO"
                                                    ),
                                                    String.class
                                            ),
                                            1
                                    )
                            ).lessOrEqual(letteraA)
                    );
        }

        SelectConditionStep<?> finalQuery =
                dsl.selectFrom(unionTable)
                        .where(condFiltroFinale);

        Field<String> cognomeField =
                DSL.field(
                        DSL.name(
                                "q",
                                "COGNOME_CHIARO"
                        ),
                        String.class
                );

        Field<String> nomeField =
                DSL.field(
                        DSL.name(
                                "q",
                                "NOME_CHIARO"
                        ),
                        String.class
                );

        Field<Integer> registroIdField =
                DSL.field(
                        DSL.name(
                                "q",
                                "REGISTRO_ID"
                        ),
                        Integer.class
                );

        Result<?> result =
                dsl.selectFrom(unionTable)
                        .where(condFiltroFinale)
                        .orderBy(
                                cognomeField.asc(),
                                nomeField.asc(),
                                registroIdField.asc()
                        )
                        .limit(
                                stepAttivo
                                        ? limiteRecordStepCaricamentoAdesioni
                                        : limiteRecord
                        )
                        .fetch();

        /*
         * Deduplicazione per SOGGETTO_ID.
         */
        Map<Integer, org.jooq.Record> recordUnivoci =
                result.stream()
                        .filter(
                                r -> r.get(
                                        "SOGGETTO_ID",
                                        Integer.class
                                ) != null
                        )
                        .collect(
                                Collectors.toMap(
                                        r -> r.get(
                                                "SOGGETTO_ID",
                                                Integer.class
                                        ),
                                        r -> r,
                                        (record1, record2) -> record1,
                                        LinkedHashMap::new
                                )
                        );

        /*
         * Il limite viene applicato dopo la deduplicazione.
         */
        int limiteFinale =
                stepAttivo
                        ? limiteRecordStepCaricamentoAdesioni
                        : limiteRecord;

        List<org.jooq.Record> recordFinali =
                recordUnivoci.values()
                        .stream()
                        .limit(limiteFinale)
                        .toList();

        /*
         * Mapping DTO.
         */
        return recordFinali
                .stream()
                .map(r -> {
                    AnagraficaDTO row =
                            new AnagraficaDTO();

                    Integer soggettoIdInt =
                            r.get(
                                    "SOGGETTO_ID",
                                    Integer.class
                            );

                    Long soggettoId =
                            soggettoIdInt != null
                                    ? soggettoIdInt.longValue()
                                    : null;

                    row.setSoggettoId(soggettoId);

                    row.setFonteId(
                            r.get(
                                    "FONTE_ID",
                                    Integer.class
                            )
                    );

                    String fonteDesc =
                            r.get(
                                    "FONTE_DESC",
                                    String.class
                            );

                    row.setDescrizioneFonte(
                            List.of(
                                    fonteDesc != null
                                            ? fonteDesc
                                            : "PROVA"
                            )
                    );

                    RegistroDTO registro =
                            RegistroUtils.ensureRegistro(row);

                    registro.setDataModifica(
                            DateConversionUtils.toOffsetDateTime(
                                    r.get(
                                            "DATA_MODIFICA",
                                            LocalDateTime.class
                                    )
                            )
                    );

                    registro.setDataCreazione(
                            DateConversionUtils.toOffsetDateTime(
                                    r.get(
                                            "DATA_CREAZIONE",
                                            LocalDateTime.class
                                    )
                            )
                    );

                    registro.setVersioneNumero(
                            r.get(
                                    "VERSIONE_NUMERO",
                                    Integer.class
                            )
                    );

                    registro.setRegistroId(
                            r.get(
                                    "REGISTRO_ID",
                                    Integer.class
                            )
                    );

                    registro.setSezione(
                            r.get(
                                    "SEZIONE",
                                    Integer.class
                            )
                    );

                    registro.setInseritoInSorveglianza(
                            r.get(
                                    "INSERITO_IN_SORVEGLIANZA",
                                    Boolean.class
                            )
                    );

                    registro.setTipoElencoInail(
                            r.get(
                                    "TIPO_ELENCO_INAIL",
                                    String.class
                            )
                    );

                    row.setNascitaData(
                            r.get(
                                    "NASCITA_DATA",
                                    LocalDate.class
                            )
                    );

                    String nomeChiaro =
                            r.get(
                                    "NOME_CHIARO",
                                    String.class
                            );

                    row.setNome(
                            nomeChiaro != null
                                    ? nomeChiaro.toUpperCase(
                                            Locale.ROOT
                                    )
                                    : ""
                    );

                    String cognomeChiaro =
                            r.get(
                                    "COGNOME_CHIARO",
                                    String.class
                            );

                    row.setCognome(
                            cognomeChiaro != null
                                    ? cognomeChiaro.toUpperCase(
                                            Locale.ROOT
                                    )
                                    : ""
                    );

                    row.setSesso(
                            r.get(
                                    "SESSO",
                                    String.class
                            )
                    );

                    row.setCodiceFiscale(
                            r.get(
                                    "CODICE_FISCALE",
                                    String.class
                            )
                    );

                    row.setDomicilioAslId(
                            r.get(
                                    "DOMICILIO_ASL_ID",
                                    Integer.class
                            )
                    );

                    row.setResidenzaAslId(
                            r.get(
                                    "RESIDENZA_ASL_ID",
                                    Integer.class
                            )
                    );

                    Integer assistenzaId =
                            r.get(
                                    "ASSISTENZA_ASL_ID",
                                    Integer.class
                            );

                    row.setAssistenzaAslId(
                            assistenzaId != null
                                    ? String.format(
                                            "%06d",
                                            assistenzaId
                                    )
                                    : null
                    );

                    row.setDescrizioneAslCompetenza(
                            r.get(
                                    "ASL_AZIENDA_DESC",
                                    String.class
                            )
                    );

                    row.setTelefonoAura(
                            r.get(
                                    "TELEFONO_AURA",
                                    String.class
                            )
                    );

                    row.setEmailAura(
                            r.get(
                                    "EMAIL_AURA",
                                    String.class
                            )
                    );

                    row.setDescrizioneStato(
                            r.get(
                                    "SOGGETTO_STATO_DESC",
                                    String.class
                            )
                    );

                    row.setSoggettoStatoNote(
                            r.get(
                                    "SOGGETTO_STATO_NOTE",
                                    String.class
                            )
                    );

                    row.setSoggettoStatoPrecedenteId(
                            r.get(
                                    "SOGGETTO_STATO_PRECEDENTE_ID",
                                    Integer.class
                            )
                    );

                    row.setListaEsenzione(
                            Collections.emptyList()
                    );

                    if (soggettoId != null) {
                        row.setListaFonteByIdSoggetto(
                                fonteRepository != null
                                        ? fonteRepository
                                                .findListaFonteDescByIdSoggetto(
                                                        soggettoId
                                                )
                                        : Collections.emptyList()
                        );
                    } else {
                        row.setListaFonteByIdSoggetto(
                                Collections.emptyList()
                        );
                    }

                    LocalDateTime adesioneData =
                            r.get(
                                    "ADESIONE_DATA",
                                    LocalDateTime.class
                            );

                    row.setAdesioneData(
                            adesioneData != null
                                    ? adesioneData.toString()
                                    : null
                    );

                    LocalDateTime presentazione =
                            r.get(
                                    "PRESENTAZIONE_ISTANZA_DATA",
                                    LocalDateTime.class
                            );

                    row.setPresentazioneIstanzaData(
                            presentazione != null
                                    ? presentazione
                                            .toLocalDate()
                                            .toString()
                                    : null
                    );

                    return row;
                })
                .filter(
                        row -> row.getSoggettoId() != null
                )
                .toList();
    }

    
//    private List<AnagraficaDTO> findListaAnagraficaFast(
//            List<Integer> filtroFonteId,
//            List<String> descrizioniStato,
//            List<String> sezione,
//            Boolean insInSorveglianza,
//            String tipoElencoInail,
//            List<Integer> assistenzaAslId,
//            String codiceFiscale,
//            String cognomeLettDa,
//            String cognomeLettA,
//            LocalDate nascitaData,
//            Boolean azzeraContatoreProssimoStep,
//            String profiloUtente,
//            AuditLogRequest auditLogRequest) {
//
//        /*
//         * Normalizzazione del filtro fonte.
//         */
//        if (filtroFonteId == null || filtroFonteId.isEmpty()) {
//            filtroFonteId = List.of(1, 2, 3, 4, 5);
//        } else {
//            filtroFonteId = new ArrayList<>(filtroFonteId);
//        }
//
////        /*
////         * Regola applicativa esistente.
////         */
////        if (filtroFonteId.contains(2)) {
////            azzeraContatoreProssimoStep = Boolean.TRUE;
////        }
//
//        Set<String> profiliCorrente = Set.of(
//                "REEA_OP_SPRESAL",
//                "REEA_OP_CRPT",
//                "REEA_OP_CSI",
//                "REEA_OP_CRPT_PSEUDO",
//                "REEA_OP_SPRESAL_PSEUDO",
//                "REEA_OP_EPI_PSEUDO",
//                "REEA_OP_CSI_PSEUDO"
//        );
//
//        Set<String> profiliStorico = Set.of(
//                "REEA_OP_SPRESAL_PSEUDO",
//                "REEA_OP_EPI_PSEUDO",
//                "REEA_OP_SPRESAL",
//                "REEA_OP_CSI_PSEUDO"
//        );
//
//        boolean profiloCorrenteAbilitato =
//                profiliCorrente.contains(profiloUtente);
//
//        boolean includeStorico =
//                profiliStorico.contains(profiloUtente);
//
//        if (!profiloCorrenteAbilitato) {
//            return Collections.emptyList();
//        }
//
//        /*
//         * Condizioni dinamiche.
//         */
//        Condition condDescrizioneStato = noCondition();
//
//        if (descrizioniStato != null && !descrizioniStato.isEmpty()) {
//            condDescrizioneStato =
//                    REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
//                            .in(descrizioniStato);
//        }
//
//        Condition condSezione = noCondition();
//
//        if (sezione != null && !sezione.isEmpty()) {
//            condSezione =
//                    REEA_T_REGISTRO.SEZIONE.in(sezione);
//        }
//
//        Condition condInseritoInSorveglianza = noCondition();
//
//        if (insInSorveglianza != null) {
//            condInseritoInSorveglianza =
//                    REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA
//                            .eq(insInSorveglianza);
//        }
//
//        Condition condTipoElencoInail = noCondition();
//
//        if (tipoElencoInail != null && !tipoElencoInail.isBlank()) {
//            condTipoElencoInail =
//                    REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(
//                            tipoElencoInail
//                                    .trim()
//                                    .toUpperCase(Locale.ROOT)
//                    );
//        }
//
//        Condition condAssistenzaAsl = noCondition();
//        Condition condAssistenzaAslStorico = noCondition();
//
//        if (assistenzaAslId != null && !assistenzaAslId.isEmpty()) {
//            condAssistenzaAsl =
//                    REEA_T_SOGGETTO.ASSISTENZA_ASL_ID
//                            .in(assistenzaAslId);
//
//            condAssistenzaAslStorico =
//                    REEA_S_SOGGETTO.ASSISTENZA_ASL_ID
//                            .in(assistenzaAslId);
//        }
//
//        Condition condCodiceFiscaleT = noCondition();
//        Condition condCodiceFiscaleS = noCondition();
//
//        if (codiceFiscale != null && !codiceFiscale.isBlank()) {
//            String codiceFiscaleNormalizzato =
//                    codiceFiscale.trim();
//
//            condCodiceFiscaleT =
//                    REEA_T_SOGGETTO.CODICE_FISCALE
//                            .equalIgnoreCase(codiceFiscaleNormalizzato);
//
//            condCodiceFiscaleS =
//                    REEA_S_SOGGETTO.CODICE_FISCALE
//                            .equalIgnoreCase(codiceFiscaleNormalizzato);
//        }
//
//        Condition condNascitaDataT = noCondition();
//        Condition condNascitaDataS = noCondition();
//
//        if (nascitaData != null) {
//            condNascitaDataT =
//                    REEA_T_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//
//            condNascitaDataS =
//                    REEA_S_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//        }
//
//        Condition condGlobale =
//                condDescrizioneStato
//                        .and(condSezione)
//                        .and(condInseritoInSorveglianza)
//                        .and(condTipoElencoInail)
//                        .and(condAssistenzaAsl)
//                        .and(condCodiceFiscaleT)
//                        .and(condNascitaDataT);
//
//        Condition condGlobaleStorico =
//                condDescrizioneStato
//                        .and(condSezione)
//                        .and(condInseritoInSorveglianza)
//                        .and(condTipoElencoInail)
//                        .and(condAssistenzaAslStorico)
//                        .and(condCodiceFiscaleS)
//                        .and(condNascitaDataS);
//
//        /*
//         * Filtro fonte corrente.
//         *
//         * EXISTS evita la moltiplicazione delle righe.
//         */
//        Condition condFonteGlobaleT =
//                REEA_T_SOGGETTO.FONTE_ID.in(filtroFonteId)
//                        .orExists(
//                                dsl.selectOne()
//                                        .from(REEA_S_SOGGETTO)
//                                        .where(
//                                                REEA_S_SOGGETTO.SOGGETTO_ID
//                                                        .eq(
//                                                                REEA_T_SOGGETTO
//                                                                        .SOGGETTO_ID
//                                                        )
//                                        )
//                                        .and(
//                                                REEA_S_SOGGETTO.FONTE_ID
//                                                        .in(filtroFonteId)
//                                        )
//                        );
//
//        Condition condFonteGlobaleS =
//                REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId);
//
//        /*
//         * Paging sul registro.
//         */
//        Condition condRegistroIdPaging = noCondition();
//
//        if (Boolean.FALSE.equals(azzeraContatoreProssimoStep)) {
//            Integer lastRegistroId =
//                    getUltimoRegistroIdProcessato(auditLogRequest);
//
//            if (lastRegistroId != null) {
//                condRegistroIdPaging =
//                        REEA_T_REGISTRO.REGISTRO_ID
//                                .gt(lastRegistroId);
//            }
//        }
//
//        /*
//         * Query degli ID correnti.
//         */
//        Select<Record1<Integer>> selectIdsCorrente =
//                dsl.selectDistinct(
//                        REEA_T_SOGGETTO.SOGGETTO_ID
//                                .as("SOGGETTO_ID")
//                )
//                .from(REEA_T_SOGGETTO)
//                .join(REEA_D_FONTE)
//                    .on(
//                            REEA_D_FONTE.FONTE_ID.eq(
//                                    REEA_T_SOGGETTO.FONTE_ID
//                            )
//                    )
//                .leftJoin(REEA_D_ASL)
//                    .on(
//                            REEA_D_ASL.ASL_ID.eq(
//                                    REEA_T_SOGGETTO.ASSISTENZA_ASL_ID
//                            )
//                    )
//                .join(REEA_T_REGISTRO)
//                    .on(
//                            REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                                    field(
//                                            "reea.hmac_soggetto_id({0}, {1})",
//                                            String.class,
//                                            REEA_T_SOGGETTO.SOGGETTO_ID,
//                                            val("16<odcc8!")
//                                    )
//                            )
//                    )
//                .join(REEA_D_SOGGETTO_STATO)
//                    .on(
//                            REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(
//                                    REEA_T_SOGGETTO.SOGGETTO_STATO_ID
//                            )
//                    )
//                .where(
//                        condGlobale
//                                .and(condFonteGlobaleT)
//                                .and(condRegistroIdPaging)
//                );
//
//        /*
//         * Query degli ID storico.
//         */
//        Select<Record1<Integer>> selectIdsStorico = null;
//
//        if (includeStorico) {
//            selectIdsStorico =
//                    dsl.selectDistinct(
//                            REEA_S_SOGGETTO.SOGGETTO_ID
//                                    .as("SOGGETTO_ID")
//                    )
//                    .from(REEA_S_SOGGETTO)
//                    .join(REEA_D_FONTE)
//                        .on(
//                                REEA_D_FONTE.FONTE_ID.eq(
//                                        REEA_S_SOGGETTO.FONTE_ID
//                                )
//                        )
//                    .leftJoin(REEA_D_ASL)
//                        .on(
//                                REEA_D_ASL.ASL_ID.eq(
//                                        REEA_S_SOGGETTO.ASSISTENZA_ASL_ID
//                                )
//                        )
//                    .join(REEA_T_REGISTRO)
//                        .on(
//                                REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                                        field(
//                                                "reea.hmac_soggetto_id({0}, {1})",
//                                                String.class,
//                                                REEA_S_SOGGETTO.SOGGETTO_ID,
//                                                val("16<odcc8!")
//                                        )
//                                )
//                        )
//                    .join(REEA_D_SOGGETTO_STATO)
//                        .on(
//                                REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(
//                                        REEA_S_SOGGETTO.SOGGETTO_STATO_ID
//                                )
//                        )
//                    .where(
//                            condGlobaleStorico
//                                    .and(condFonteGlobaleS)
//                                    .and(
//                                            REEA_S_SOGGETTO.SOGGETTO_ID
//                                                    .notIn(
//                                                            dsl.selectDistinct(
//                                                                    REEA_T_SOGGETTO
//                                                                            .SOGGETTO_ID
//                                                            )
//                                                            .from(
//                                                                    REEA_T_SOGGETTO
//                                                            )
//                                                    )
//                                    )
//                    );
//        }
//
//        /*
//         * Unione degli ID.
//         *
//         * UNION elimina gli eventuali ID duplicati tra corrente e storico.
//         */
//        Table<?> idsTable;
//
//        if (selectIdsStorico != null) {
//            idsTable =
//                    selectIdsCorrente
//                            .union(selectIdsStorico)
//                            .asTable("ids");
//        } else {
//            idsTable =
//                    selectIdsCorrente.asTable("ids");
//        }
//
//        /*
//         * Il campo deve essere recuperato dalla tabella derivata.
//         * Non usare REEA_T_SOGGETTO.SOGGETTO_ID per leggere i Record.
//         */
//        Field<Integer> idField =
//                idsTable.field(
//                        "SOGGETTO_ID",
//                        Integer.class
//                );
//
//        if (idField == null) {
//            return Collections.emptyList();
//        }
//
//        /*
//         * Recupero degli ID univoci e validi.
//         */
//        List<Integer> ids =
//                dsl.select(idField)
//                        .from(idsTable)
//                        .where(idField.isNotNull())
//                        .fetch(idField)
//                        .stream()
//                        .filter(Objects::nonNull)
//                        .filter(
//                                id -> isValidoPerExport(
//                                        id,
//                                        profiloUtente
//                                )
//                        )
//                        .distinct()
//                        .toList();
//
//        if (ids.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        /*
//         * Limiti.
//         */
//        Integer limiteRecordStepCaricamentoAdesioni =
//                Optional.ofNullable(
//                        dsl.select(
//                                REEA_C_PARAMETRO.PARAMETRO_VALORE
//                        )
//                        .from(REEA_C_PARAMETRO)
//                        .where(
//                                REEA_C_PARAMETRO.PARAMETRO_COD.eq(
//                                        "NumeroRecordRicercaAdesioni"
//                                )
//                        )
//                        .fetchOneInto(Integer.class)
//                ).orElse(1000);
//
//        Integer limiteRecord =
//                Optional.ofNullable(
//                        dsl.select(
//                                REEA_C_PARAMETRO.PARAMETRO_VALORE
//                        )
//                        .from(REEA_C_PARAMETRO)
//                        .where(
//                                REEA_C_PARAMETRO.PARAMETRO_COD.eq(
//                                        "NumeroRecordRicercaGenerica"
//                                )
//                        )
//                        .fetchOneInto(Integer.class)
//                ).orElse(1000);
//
//        int limite =
//                azzeraContatoreProssimoStep != null
//                        ? limiteRecordStepCaricamentoAdesioni
//                        : limiteRecord;
//
//        /*
//         * Query di dettaglio corrente.
//         *
//         * Nota: il record restituito contiene gli alias della SELECT.
//         * Pertanto i valori vengono letti con:
//         *
//         * r.get("SOGGETTO_ID", Integer.class)
//         */
//        SelectConditionStep<Record> queryDettaglio =
//                dsl.select(
//                        REEA_D_ASL.ASL_AZIENDA_DESC
//                                .as("ASL_AZIENDA_DESC"),
//                        REEA_T_SOGGETTO.FONTE_ID
//                                .as("FONTE_ID"),
//                        REEA_D_FONTE.FONTE_DESC
//                                .as("FONTE_DESC"),
//                        REEA_T_SOGGETTO.DATA_MODIFICA
//                                .as("DATA_MODIFICA"),
//                        REEA_T_SOGGETTO.DATA_CREAZIONE
//                                .as("DATA_CREAZIONE"),
//                        REEA_T_SOGGETTO.NASCITA_DATA
//                                .as("NASCITA_DATA"),
//                        REEA_T_SOGGETTO.SOGGETTO_ID
//                                .as("SOGGETTO_ID"),
//                        REEA_T_SOGGETTO.VERSIONE_NUMERO
//                                .as("VERSIONE_NUMERO"),
//                        REEA_T_ADESIONE.ADESIONE_DATA
//                                .as("ADESIONE_DATA"),
//                        REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA
//                                .as("PRESENTAZIONE_ISTANZA_DATA"),
//                        field(
//                                "pgp_sym_decrypt({0}, {1})",
//                                String.class,
//                                REEA_T_SOGGETTO.NOME_CIFRATO,
//                                val("16<odcc8!")
//                        ).as("NOME_CHIARO"),
//                        field(
//                                "pgp_sym_decrypt({0}, {1})",
//                                String.class,
//                                REEA_T_SOGGETTO.COGNOME_CIFRATO,
//                                val("16<odcc8!")
//                        ).as("COGNOME_CHIARO"),
//                        REEA_T_SOGGETTO.SESSO.as("SESSO"),
//                        REEA_T_SOGGETTO.CODICE_FISCALE
//                                .as("CODICE_FISCALE"),
//                        REEA_T_SOGGETTO.DOMICILIO_ASL_ID
//                                .as("DOMICILIO_ASL_ID"),
//                        REEA_T_SOGGETTO.RESIDENZA_ASL_ID
//                                .as("RESIDENZA_ASL_ID"),
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID
//                                .as("ASSISTENZA_ASL_ID"),
//                        REEA_T_REGISTRO.SEZIONE
//                                .as("SEZIONE"),
//                        REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA
//                                .as("INSERITO_IN_SORVEGLIANZA"),
//                        REEA_T_REGISTRO.TIPO_ELENCO_INAIL
//                                .as("TIPO_ELENCO_INAIL"),
//                        REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE
//                                .as("SOGGETTO_STATO_NOTE"),
//                        REEA_T_REGISTRO.REGISTRO_ID
//                                .as("REGISTRO_ID"),
//                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
//                                .as("SOGGETTO_STATO_DESC"),
//                        DSL.inline((String) null)
//                                .as("TELEFONO_AURA"),
//                        DSL.inline((String) null)
//                                .as("EMAIL_AURA"),
//                        DSL.field(
//                                DSL.select(
//                                        REEA_S_SOGGETTO
//                                                .SOGGETTO_STATO_ID
//                                )
//                                .from(REEA_S_SOGGETTO)
//                                .where(
//                                        REEA_S_SOGGETTO.SOGGETTO_ID.eq(
//                                                REEA_T_SOGGETTO.SOGGETTO_ID
//                                        )
//                                )
//                                .and(
//                                        REEA_S_SOGGETTO.DATA_CANCELLAZIONE
//                                                .isNull()
//                                )
//                                .orderBy(
//                                        REEA_S_SOGGETTO.DATA_MODIFICA
//                                                .desc()
//                                )
//                                .limit(1)
//                        ).as("SOGGETTO_STATO_PRECEDENTE_ID")
//                )
//                .from(REEA_T_SOGGETTO)
//                .join(idsTable)
//                    .on(
//                            idField.eq(
//                                    REEA_T_SOGGETTO.SOGGETTO_ID
//                            )
//                    )
//                .leftJoin(REEA_D_FONTE)
//                    .on(
//                            REEA_D_FONTE.FONTE_ID.eq(
//                                    REEA_T_SOGGETTO.FONTE_ID
//                            )
//                    )
//                .leftJoin(REEA_D_ASL)
//                    .on(
//                            REEA_D_ASL.ASL_ID.eq(
//                                    REEA_T_SOGGETTO.ASSISTENZA_ASL_ID
//                            )
//                    )
//                .join(REEA_T_REGISTRO)
//                    .on(
//                            REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                                    field(
//                                            "reea.hmac_soggetto_id({0}, {1})",
//                                            String.class,
//                                            REEA_T_SOGGETTO.SOGGETTO_ID,
//                                            val("16<odcc8!")
//                                    )
//                            )
//                    )
//                .leftJoin(REEA_D_SOGGETTO_STATO)
//                    .on(
//                            REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(
//                                    REEA_T_SOGGETTO.SOGGETTO_STATO_ID
//                            )
//                    )
//                .leftJoin(REEA_T_ADESIONE)
//                    .on(
//                            REEA_T_ADESIONE.SOGGETTO_ID.eq(
//                                    REEA_T_SOGGETTO.SOGGETTO_ID
//                            )
//                    )
//                .where(
//                        REEA_T_SOGGETTO.SOGGETTO_ID.in(ids)
//                );
//
//        /*
//         * Recupero dei dettagli.
//         *
//         * Il limite viene applicato prima della deduplicazione:
//         * se esistono più adesioni per uno stesso soggetto, è preferibile
//         * non limitare qui. Il limite viene applicato dopo il filtro finale.
//         */
//        Result<Record> result =
//                queryDettaglio
//                        .orderBy(
//                                REEA_T_SOGGETTO.SOGGETTO_ID.asc(),
//                                REEA_T_ADESIONE.ADESIONE_DATA.desc()
//                        )
//                        .fetch();
//
//        /*
//         * Una sola riga per SOGGETTO_ID.
//         *
//         * Importante: il Record contiene i campi aliasati della SELECT.
//         * Quindi si usa sempre "SOGGETTO_ID".
//         */
//        Map<Integer, Record> recordUnivoci =
//                result.stream()
//                        .filter(
//                                r -> r.get(
//                                        "SOGGETTO_ID",
//                                        Integer.class
//                                ) != null
//                        )
//                        .collect(
//                                Collectors.toMap(
//                                        r -> r.get(
//                                                "SOGGETTO_ID",
//                                                Integer.class
//                                        ),
//                                        r -> r,
//                                        (record1, record2) -> record1,
//                                        LinkedHashMap::new
//                                )
//                        );
//
//        /*
//         * Filtro alfabetico e ordinamento.
//         */
//        List<Record> recordFiltrati =
//                recordUnivoci.values()
//                        .stream()
//                        .filter(r -> {
//                            String cognome =
//                                    r.get(
//                                            "COGNOME_CHIARO",
//                                            String.class
//                                    );
//
//                            if (cognome == null
//                                    || cognome.isBlank()) {
//                                return false;
//                            }
//
//                            String cognomeNormalizzato =
//                                    cognome.trim()
//                                            .toUpperCase(Locale.ROOT);
//
//                            String iniziale =
//                                    cognomeNormalizzato.substring(0, 1);
//
//                            boolean validoDa =
//                                    cognomeLettDa == null
//                                            || cognomeLettDa.isBlank()
//                                            || iniziale.compareToIgnoreCase(
//                                                    cognomeLettDa.trim()
//                                            ) >= 0;
//
//                            boolean validoA =
//                                    cognomeLettA == null
//                                            || cognomeLettA.isBlank()
//                                            || iniziale.compareToIgnoreCase(
//                                                    cognomeLettA.trim()
//                                            ) <= 0;
//
//                            return validoDa && validoA;
//                        })
//                        .sorted(
//                                Comparator
//                                        .<Record, String>comparing(
//                                                r -> Optional.ofNullable(
//                                                        r.get(
//                                                                "COGNOME_CHIARO",
//                                                                String.class
//                                                        )
//                                                ).orElse(""),
//                                                String.CASE_INSENSITIVE_ORDER
//                                        )
//                                        .thenComparing(
//                                                r -> Optional.ofNullable(
//                                                        r.get(
//                                                                "NOME_CHIARO",
//                                                                String.class
//                                                        )
//                                                ).orElse(""),
//                                                String.CASE_INSENSITIVE_ORDER
//                                        )
//                        )
//                        .limit(limite)
//                        .toList();
//
//        /*
//         * Conversione DTO.
//         */
//        return recordFiltrati
//                .stream()
//                .map(r -> {
//                    AnagraficaDTO row =
//                            new AnagraficaDTO();
//
//                    Integer soggettoIdInt =
//                            r.get(
//                                    "SOGGETTO_ID",
//                                    Integer.class
//                            );
//
//                    Long soggettoId =
//                            soggettoIdInt != null
//                                    ? soggettoIdInt.longValue()
//                                    : null;
//
//                    row.setSoggettoId(soggettoId);
//
//                    row.setFonteId(
//                            r.get(
//                                    "FONTE_ID",
//                                    Integer.class
//                            )
//                    );
//
//                    String fonteDesc =
//                            r.get(
//                                    "FONTE_DESC",
//                                    String.class
//                            );
//
//                    row.setDescrizioneFonte(
//                            List.of(
//                                    fonteDesc != null
//                                            ? fonteDesc
//                                            : "PROVA"
//                            )
//                    );
//
//                    RegistroDTO registro =
//                            RegistroUtils.ensureRegistro(row);
//
//                    registro.setDataModifica(
//                            DateConversionUtils.toOffsetDateTime(
//                                    r.get(
//                                            "DATA_MODIFICA",
//                                            LocalDateTime.class
//                                    )
//                            )
//                    );
//
//                    registro.setDataCreazione(
//                            DateConversionUtils.toOffsetDateTime(
//                                    r.get(
//                                            "DATA_CREAZIONE",
//                                            LocalDateTime.class
//                                    )
//                            )
//                    );
//
//                    registro.setVersioneNumero(
//                            r.get(
//                                    "VERSIONE_NUMERO",
//                                    Integer.class
//                            )
//                    );
//
//                    registro.setRegistroId(
//                            r.get(
//                                    "REGISTRO_ID",
//                                    Integer.class
//                            )
//                    );
//
//                    registro.setSezione(
//                            r.get(
//                                    "SEZIONE",
//                                    Integer.class
//                            )
//                    );
//
//                    registro.setInseritoInSorveglianza(
//                            r.get(
//                                    "INSERITO_IN_SORVEGLIANZA",
//                                    Boolean.class
//                            )
//                    );
//
//                    registro.setTipoElencoInail(
//                            r.get(
//                                    "TIPO_ELENCO_INAIL",
//                                    String.class
//                            )
//                    );
//
//                    row.setNascitaData(
//                            r.get(
//                                    "NASCITA_DATA",
//                                    LocalDate.class
//                            )
//                    );
//
//                    String nomeChiaro =
//                            r.get(
//                                    "NOME_CHIARO",
//                                    String.class
//                            );
//
//                    row.setNome(
//                            nomeChiaro != null
//                                    ? nomeChiaro.toUpperCase()
//                                    : ""
//                    );
//
//                    String cognomeChiaro =
//                            r.get(
//                                    "COGNOME_CHIARO",
//                                    String.class
//                            );
//
//                    row.setCognome(
//                            cognomeChiaro != null
//                                    ? cognomeChiaro.toUpperCase()
//                                    : ""
//                    );
//
//                    row.setSesso(
//                            r.get(
//                                    "SESSO",
//                                    String.class
//                            )
//                    );
//
//                    row.setCodiceFiscale(
//                            r.get(
//                                    "CODICE_FISCALE",
//                                    String.class
//                            )
//                    );
//
//                    row.setDomicilioAslId(
//                            r.get(
//                                    "DOMICILIO_ASL_ID",
//                                    Integer.class
//                            )
//                    );
//
//                    row.setResidenzaAslId(
//                            r.get(
//                                    "RESIDENZA_ASL_ID",
//                                    Integer.class
//                            )
//                    );
//
//                    Integer assistenzaId =
//                            r.get(
//                                    "ASSISTENZA_ASL_ID",
//                                    Integer.class
//                            );
//
//                    row.setAssistenzaAslId(
//                            assistenzaId != null
//                                    ? String.format(
//                                            "%06d",
//                                            assistenzaId
//                                    )
//                                    : null
//                    );
//
//                    row.setDescrizioneAslCompetenza(
//                            r.get(
//                                    "ASL_AZIENDA_DESC",
//                                    String.class
//                            )
//                    );
//
//                    row.setTelefonoAura(
//                            r.get(
//                                    "TELEFONO_AURA",
//                                    String.class
//                            )
//                    );
//
//                    row.setEmailAura(
//                            r.get(
//                                    "EMAIL_AURA",
//                                    String.class
//                            )
//                    );
//
//                    row.setDescrizioneStato(
//                            r.get(
//                                    "SOGGETTO_STATO_DESC",
//                                    String.class
//                            )
//                    );
//
//                    row.setSoggettoStatoNote(
//                            r.get(
//                                    "SOGGETTO_STATO_NOTE",
//                                    String.class
//                            )
//                    );
//
//                    row.setSoggettoStatoPrecedenteId(
//                            r.get(
//                                    "SOGGETTO_STATO_PRECEDENTE_ID",
//                                    Integer.class
//                            )
//                    );
//
//                    if (soggettoId != null) {
//                        row.setListaFonteByIdSoggetto(
//                                fonteRepository != null
//                                        ? fonteRepository
//                                                .findListaFonteDescByIdSoggetto(
//                                                        soggettoId
//                                                )
//                                        : Collections.emptyList()
//                        );
//                    } else {
//                        row.setListaFonteByIdSoggetto(
//                                Collections.emptyList()
//                        );
//                    }
//
//                    row.setListaEsenzione(
//                            Collections.emptyList()
//                    );
//
//                    LocalDateTime adesioneData =
//                            r.get(
//                                    "ADESIONE_DATA",
//                                    LocalDateTime.class
//                            );
//
//                    row.setAdesioneData(
//                            adesioneData != null
//                                    ? adesioneData.toString()
//                                    : null
//                    );
//
//                    LocalDateTime presentazione =
//                            r.get(
//                                    "PRESENTAZIONE_ISTANZA_DATA",
//                                    LocalDateTime.class
//                            );
//
//                    row.setPresentazioneIstanzaData(
//                            presentazione != null
//                                    ? presentazione
//                                            .toLocalDate()
//                                            .toString()
//                                    : null
//                    );
//
//                    return row;
//                })
//                .toList();
//    }
    //DE Query analoga alla precedente ma fast perch� non include nella where campi cifrati
    //ORIGINALE
//    private List<AnagraficaDTO> findListaAnagraficaFast(
//    		List<Integer> filtroFonteId,
//    		List<String> descrizioniStato,
//    		List<String> sezione,
//    		Boolean insInSorveglianza,
//    		String tipoElencoInail,
//    		List<Integer> assistenzaAslId,
//    		String codiceFiscale,
//    		String cognomeLettDa,
//    		String cognomeLettA,
//    		LocalDate nascitaData,
//    		Boolean azzeraContatoreProssimoStep,
//    		String profiloUtente,
//    		AuditLogRequest auditLogRequest) {
//
//    	Condition condDescrizioneStato = noCondition();
//    	if (descrizioniStato != null && !descrizioniStato.isEmpty()) {
//    		condDescrizioneStato = REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.in(descrizioniStato);
//    	}
//
//    	Condition condSezione = noCondition();
//    	if (sezione != null && !sezione.isEmpty()) {
//    		condSezione = REEA_T_REGISTRO.SEZIONE.in(sezione);
//    	}
//
//    	Condition condInseritoInSorveglianza = noCondition();
//    	if (insInSorveglianza != null) {
//    		condInseritoInSorveglianza = REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.eq(insInSorveglianza);
//    	}
//
//    	Condition condTipoElencoInail = noCondition();
//    	if (tipoElencoInail != null && !tipoElencoInail.isEmpty()) {
//    		condTipoElencoInail = REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(tipoElencoInail.toUpperCase());
//    	}
//
//    	Condition condAssistenzaAsl = noCondition();
//    	Condition condAssistenzaAslStorico = noCondition();
//    	if (assistenzaAslId != null && !assistenzaAslId.isEmpty()) {
//    		condAssistenzaAsl = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//    		condAssistenzaAslStorico = REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//    	}
//
//    	Condition condCodiceFiscaleT = noCondition();
//    	Condition condCodiceFiscaleS = noCondition();
//    	if (codiceFiscale != null && !codiceFiscale.isEmpty()) {
//    		condCodiceFiscaleT = REEA_T_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
//    		condCodiceFiscaleS = REEA_S_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
//    	}
//
//    	Condition condNascitaDataT = noCondition();
//    	Condition condNascitaDataS = noCondition();
//    	if (nascitaData != null) {
//    		condNascitaDataT = REEA_T_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//    		condNascitaDataS = REEA_S_SOGGETTO.NASCITA_DATA.eq(nascitaData);
//    	}
//
//    	Condition condRegistroIdPaging = noCondition();
//    	
//    	if (filtroFonteId == null)
//    		filtroFonteId = List.of(1, 2, 3, 4, 5);
//    	
//    	if(filtroFonteId.contains(2))
//    		azzeraContatoreProssimoStep = true;
//
//    	if (Boolean.FALSE.equals(azzeraContatoreProssimoStep)) {
//    		Integer lastRegistroId = getUltimoRegistroIdProcessato(auditLogRequest);
//    		if (lastRegistroId != null) {
//    			condRegistroIdPaging = REEA_T_REGISTRO.REGISTRO_ID.gt(lastRegistroId);
//    		}
//    	}
//
//    	Condition condGlobale = condDescrizioneStato
//    			.and(condSezione)
//    			.and(condInseritoInSorveglianza)
//    			.and(condTipoElencoInail)
//    			.and(condAssistenzaAsl)
//    			.and(condCodiceFiscaleT)
//    			.and(condNascitaDataT);
//
//    	Condition condGlobaleStorico = condDescrizioneStato
//    			.and(condSezione)
//    			.and(condInseritoInSorveglianza)
//    			.and(condTipoElencoInail)
//    			.and(condAssistenzaAslStorico)
//    			.and(condCodiceFiscaleS)
//    			.and(condNascitaDataS);
//
//    	Condition condFonteGlobaleT = noCondition();
//    	if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
//    		condFonteGlobaleT = REEA_T_SOGGETTO.FONTE_ID.in(filtroFonteId)
//    				.or(REEA_T_SOGGETTO.SOGGETTO_ID.in(
//    						dsl.selectDistinct(REEA_S_SOGGETTO.SOGGETTO_ID)
//    						.from(REEA_S_SOGGETTO)
//    						.where(REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId))
//    						));
//    	}
//
//    	Condition condFonteGlobaleS = noCondition();
//    	if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
//    		condFonteGlobaleS = REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId);
//    	}
//
//    	Integer limiteRecordStepCaricamentoAdesioni = Optional.ofNullable(
//    		    dsl.select(REEA_C_PARAMETRO.PARAMETRO_VALORE)
//    		       .from(REEA_C_PARAMETRO)
//    		       .where(REEA_C_PARAMETRO.PARAMETRO_COD.eq("NumeroRecordRicercaAdesioni"))
//    		       .fetchOneInto(Integer.class)
//    		).orElse(1000);
//    	
//    	Integer limiteRecord = Optional.ofNullable(
//    		    dsl.select(REEA_C_PARAMETRO.PARAMETRO_VALORE)
//    		       .from(REEA_C_PARAMETRO)
//    		       .where(REEA_C_PARAMETRO.PARAMETRO_COD.eq("NumeroRecordRicercaGenerica"))
//    		       .fetchOneInto(Integer.class)
//    		).orElse(1000);
//
//    	var basePageQuery = dsl
//    			.select(REEA_T_SOGGETTO.SOGGETTO_ID, REEA_T_REGISTRO.REGISTRO_ID)
//    			.from(REEA_T_SOGGETTO)
//    			.join(REEA_T_REGISTRO)
//    			.on(
//    					REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//    							field("reea.hmac_soggetto_id({0}, {1})",
//    									String.class,
//    									REEA_T_SOGGETTO.SOGGETTO_ID,
//    									val("16<odcc8!"))
//    							)
//    					)
//    			.leftJoin(REEA_D_SOGGETTO_STATO)
//    			.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//    			.leftJoin(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//    			.leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//    			.where(condGlobale
//    					.and(condFonteGlobaleT)
//    					.and(condRegistroIdPaging))
//    			.orderBy(REEA_T_REGISTRO.REGISTRO_ID.asc());
//
//    	// Il limit si applica solo quando lo step è attivo (MMG/Preadesioni).
//    	// Senza step (azzeraContatoreProssimoStep == null) si restituiscono tutti i record.
//    	var page = (azzeraContatoreProssimoStep != null) //&& Boolean.FALSE.equals(azzeraContatoreProssimoStep)
//    			? basePageQuery.limit(limiteRecordStepCaricamentoAdesioni).fetch()
//    			: basePageQuery.limit(limiteRecord).fetch();
//
//    	List<Integer> ids = page.getValues(REEA_T_SOGGETTO.SOGGETTO_ID);
//
//    	Integer nuovoLastRegistroId = page.isEmpty()
//    			? null
//    					: page.get(page.size() - 1).get(REEA_T_REGISTRO.REGISTRO_ID);
//
//    	if (nuovoLastRegistroId != null) {
//    		salvaUltimoRegistroIdProcessato(nuovoLastRegistroId, auditLogRequest);
//    	}
//
//    	Select<Record> selectCorrente = dsl
//    	        .select(
//    	                DSL.val((String) null).as("ASL_AZIENDA_DESC"),
//    	                DSL.val((Integer) null).as("FONTE_ID"),
//    	                DSL.val((String) null).as("FONTE_DESC"),
//    	                DSL.val((LocalDateTime) null).as("DATA_MODIFICA"),
//    	                DSL.val((LocalDateTime) null).as("DATA_CREAZIONE"),
//    	                DSL.val((LocalDate) null).as("NASCITA_DATA"),
//    	                DSL.val((Integer) null).as("SOGGETTO_ID"),
//    	                DSL.val((Integer) null).as("VERSIONE_NUMERO"),
//    	                DSL.val((LocalDateTime) null).as("ADESIONE_DATA"),
//    	                DSL.val((LocalDateTime) null).as("PRESENTAZIONE_ISTANZA_DATA"),
//    	                DSL.val((String) null).as("NOME_CHIARO"),
//    	                DSL.val((String) null).as("COGNOME_CHIARO"),
//    	                DSL.val((String) null).as("SESSO"),
//    	                DSL.val((String) null).as("CODICE_FISCALE"),
//    	                DSL.val((Integer) null).as("DOMICILIO_ASL_ID"),
//    	                DSL.val((Integer) null).as("RESIDENZA_ASL_ID"),
//    	                DSL.val((Integer) null).as("ASSISTENZA_ASL_ID"),
//    	                DSL.val((Integer) null).as("SEZIONE"),
//    	                DSL.val((Boolean) null).as("INSERITO_IN_SORVEGLIANZA"),
//    	                DSL.val((String) null).as("TIPO_ELENCO_INAIL"),
//    	                DSL.val((String) null).as("SOGGETTO_STATO_NOTE"),
//    	                DSL.val((Integer) null).as("REGISTRO_ID"),
//    	                DSL.val((String) null).as("SOGGETTO_STATO_DESC"),
//    	                DSL.val((String) null).as("TELEFONO_AURA"),
//    	                DSL.val((String) null).as("EMAIL_AURA"),
//    	                DSL.val((Integer) null).as("SOGGETTO_STATO_PRECEDENTE_ID")
//    	        )
//    	        .where(DSL.falseCondition());
//    	
//    	if ("REEA_OP_SPRESAL".equals(profiloUtente)
//    	        || "REEA_OP_CRPT".equals(profiloUtente)
//    	        || "REEA_OP_CSI".equals(profiloUtente)
//    	        || "REEA_OP_CRPT_PSEUDO".equals(profiloUtente)
//    	        || "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
//    	        || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
//    	        || "REEA_OP_CSI_PSEUDO".equals(profiloUtente)) {
//
////    	    boolean includiSenzaAdesione = "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente) || "REEA_OP_CRPT".equals(profiloUtente);
//    	    //"REEA_OP_CRPT".equals(profiloUtente) || "REEA_OP_CSI".equals(profiloUtente)
//
//    	    selectCorrente = dsl
//    	            .selectDistinct(
//    	                    REEA_D_ASL.ASL_AZIENDA_DESC.as("ASL_AZIENDA_DESC"),
//    	                    REEA_T_SOGGETTO.FONTE_ID.as("FONTE_ID"),
//    	                    REEA_D_FONTE.FONTE_DESC.as("FONTE_DESC"),
//    	                    REEA_T_SOGGETTO.DATA_MODIFICA.as("DATA_MODIFICA"),
//    	                    REEA_T_SOGGETTO.DATA_CREAZIONE.as("DATA_CREAZIONE"),
//    	                    REEA_T_SOGGETTO.NASCITA_DATA.as("NASCITA_DATA"),
//    	                    REEA_T_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
//    	                    REEA_T_SOGGETTO.VERSIONE_NUMERO.as("VERSIONE_NUMERO"),
//    	                    REEA_T_ADESIONE.ADESIONE_DATA.as("ADESIONE_DATA"),
//    	                    REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA.as("PRESENTAZIONE_ISTANZA_DATA"),
//    	                    field("pgp_sym_decrypt({0}, {1})",
//    	                            String.class,
//    	                            REEA_T_SOGGETTO.NOME_CIFRATO,
//    	                            val("16<odcc8!")).as("NOME_CHIARO"),
//    	                    field("pgp_sym_decrypt({0}, {1})",
//    	                            String.class,
//    	                            REEA_T_SOGGETTO.COGNOME_CIFRATO,
//    	                            val("16<odcc8!")).as("COGNOME_CHIARO"),
//    	                    REEA_T_SOGGETTO.SESSO.as("SESSO"),
//    	                    REEA_T_SOGGETTO.CODICE_FISCALE.as("CODICE_FISCALE"),
//    	                    REEA_T_SOGGETTO.DOMICILIO_ASL_ID.as("DOMICILIO_ASL_ID"),
//    	                    REEA_T_SOGGETTO.RESIDENZA_ASL_ID.as("RESIDENZA_ASL_ID"),
//    	                    REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("ASSISTENZA_ASL_ID"),
//    	                    REEA_T_REGISTRO.SEZIONE.as("SEZIONE"),
//    	                    REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("INSERITO_IN_SORVEGLIANZA"),
//    	                    REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("TIPO_ELENCO_INAIL"),
//    	                    REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE.as("SOGGETTO_STATO_NOTE"),
//    	                    REEA_T_REGISTRO.REGISTRO_ID.as("REGISTRO_ID"),
//    	                    REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("SOGGETTO_STATO_DESC"),
//    	                    DSL.inline((String) null).as("TELEFONO_AURA"),
//    	                    DSL.inline((String) null).as("EMAIL_AURA"),
//    	                    DSL.field(
//    	                            DSL.select(REEA_S_SOGGETTO.SOGGETTO_STATO_ID)
//    	                                    .from(REEA_S_SOGGETTO)
//    	                                    .where(REEA_S_SOGGETTO.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
//    	                                    .and(REEA_S_SOGGETTO.DATA_CANCELLAZIONE.isNull())
//    	                                    .orderBy(REEA_S_SOGGETTO.DATA_MODIFICA.desc())
//    	                                    .limit(1)
//    	                    ).as("SOGGETTO_STATO_PRECEDENTE_ID")
//    	            )
//    			.from(REEA_T_SOGGETTO)
//    			.leftJoin(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//    			.leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//    			.join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//    					field("reea.hmac_soggetto_id({0}, {1})",
//    							String.class,
//    							REEA_T_SOGGETTO.SOGGETTO_ID,
//    							val("16<odcc8!"))))
//    			.leftJoin(REEA_D_SOGGETTO_STATO)
//    			.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//    			.leftJoin(REEA_T_ADESIONE)
//    			.on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID)
//    					.and(REEA_T_ADESIONE.SOGGETTO_ID.isNotNull()))
//    			.where(condGlobale.and(condFonteGlobaleT))
////    			.and(REEA_T_SOGGETTO.SOGGETTO_ID.in(ids))
//    			.and(azzeraContatoreProssimoStep != null
//    	        ? REEA_T_SOGGETTO.SOGGETTO_ID.in(ids)
//    	        : REEA_T_SOGGETTO.SOGGETTO_ID.in(ids));
////    	        : noCondition());
////    			.and(includiSenzaAdesione ? noCondition() : REEA_T_ADESIONE.SOGGETTO_ID.isNotNull());
//       }
//       
//       Select<Record> selectStorico = null;
//       
//       boolean includeStorico =
//    	        "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
//    	        || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
//    	        || "REEA_OP_SPRESAL".equals(profiloUtente)
//    	        || "REEA_OP_CSI_PSEUDO".equals(profiloUtente);
//       
//       if (includeStorico) {
//    	selectStorico = dsl
//    			.selectDistinct(
//
//    					DSL.castNull(REEA_D_ASL.ASL_AZIENDA_DESC.getDataType()).as("ASL_AZIENDA_DESC"),
//    					REEA_S_SOGGETTO.FONTE_ID.as("FONTE_ID"),
//    					REEA_D_FONTE.FONTE_DESC.as("FONTE_DESC"),
//    					REEA_S_SOGGETTO.DATA_MODIFICA.as("DATA_MODIFICA"),
//    					REEA_S_SOGGETTO.DATA_CREAZIONE.as("DATA_CREAZIONE"),
//    					DSL.castNull(REEA_S_SOGGETTO.NASCITA_DATA.getDataType()).as("NASCITA_DATA"),
//    					REEA_S_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
//    					REEA_S_SOGGETTO.VERSIONE_NUMERO.as("VERSIONE_NUMERO"),
//    					DSL.val((LocalDateTime) null).as("ADESIONE_DATA"),
//    					DSL.val((LocalDateTime) null).as("PRESENTAZIONE_ISTANZA_DATA"),
//    					DSL.val((String) null).as("NOME_CIFRATO"),
//    					DSL.val((String) null).as("COGNOME_CIFRATO"),
//    					DSL.val((String) null).as("SESSO"),
//    					DSL.val((String) null).as("CODICE_FISCALE"),
//    					DSL.val((Integer) null).as("DOMICILIO_ASL_ID"),
//    					DSL.val((Integer) null).as("RESIDENZA_ASL_ID"),
//    					DSL.val((Integer) null).as("ASSISTENZA_ASL_ID"),
//    					REEA_T_REGISTRO.SEZIONE.as("SEZIONE"),
//    					REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("INSERITO_IN_SORVEGLIANZA"),
//    					REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("TIPO_ELENCO_INAIL"),
//    					REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE.as("SOGGETTO_STATO_NOTE"),
//    					REEA_T_REGISTRO.REGISTRO_ID.as("REGISTRO_ID"),
//    					REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("SOGGETTO_STATO_DESC"),
//    					DSL.val((String) null).as("TELEFONO_AURA"),
//    					DSL.val((String) null).as("EMAIL_AURA"),
//    					DSL.val((Integer) null).as("SOGGETTO_STATO_PRECEDENTE_ID")
//    					)
//    			.from(REEA_S_SOGGETTO)
//    			.join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
//    			.leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID))
//    			.join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//    					field("reea.hmac_soggetto_id({0}, {1})",
//    							String.class,
//    							REEA_S_SOGGETTO.SOGGETTO_ID,
//    							val("16<odcc8!"))))
//    			.join(REEA_D_SOGGETTO_STATO)
//    			.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_S_SOGGETTO.SOGGETTO_STATO_ID))
//               .where(condGlobaleStorico
//                .and(condFonteGlobaleS)
//                .and(REEA_S_SOGGETTO.SOGGETTO_ID.notIn(
//                        dsl.selectDistinct(REEA_T_SOGGETTO.SOGGETTO_ID).from(REEA_T_SOGGETTO)
//                ))
//                .and(azzeraContatoreProssimoStep != null
//                        ? REEA_S_SOGGETTO.SOGGETTO_ID.in(ids)
//                        : noCondition()));
//       }
//       
//       Table<?> unionTable;
//       if (includeStorico && selectStorico != null) {
//           unionTable = selectCorrente.unionAll(selectStorico).asTable("q");
//       } else {
//           unionTable = selectCorrente.asTable("q");
//       }
//
//    	Condition condFastLettDa = noCondition();
//    	if (cognomeLettDa != null && !cognomeLettDa.isEmpty()) {
//    		condFastLettDa = DSL.upper(DSL.left(
//    				DSL.field(DSL.name("q", "COGNOME_CHIARO"), String.class), 1))
//    				.greaterOrEqual(cognomeLettDa.trim().toUpperCase());
//    	}
//
//    	Condition condFastLettA = noCondition();
//    	if (cognomeLettA != null && !cognomeLettA.isEmpty()) {
//    		condFastLettA = DSL.upper(DSL.left(
//    				DSL.field(DSL.name("q", "COGNOME_CHIARO"), String.class), 1))
//    				.lessOrEqual(cognomeLettA.trim().toUpperCase());
//    	}
//
//    	var query = dsl.selectFrom(unionTable)
//    			.orderBy(
//    					field(DSL.name("q", "COGNOME_CHIARO")).asc(),
//    					field(DSL.name("q", "NOME_CHIARO")).asc()
//    					);
//
//    	return query
//    			.fetch()
//    			.stream()
//    			.map(r -> {
//    				AnagraficaDTO row = new AnagraficaDTO();
//
//    				Integer soggettoIdInt = r.get("SOGGETTO_ID", Integer.class);
//    				Long soggettoId = soggettoIdInt != null ? soggettoIdInt.longValue() : null;
//    				row.setSoggettoId(soggettoId);
//
//    				row.setFonteId(r.get("FONTE_ID", Integer.class));
//
//    				String fonteDesc = r.get("FONTE_DESC", String.class);
//    				row.setDescrizioneFonte(List.of(fonteDesc != null ? fonteDesc : "PROVA"));
//
//    				RegistroDTO registro = RegistroUtils.ensureRegistro(row);
//    				registro.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get("DATA_MODIFICA", LocalDateTime.class)));
//    				registro.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get("DATA_CREAZIONE", LocalDateTime.class)));
//    				registro.setVersioneNumero(r.get("VERSIONE_NUMERO", Integer.class));
//    				registro.setRegistroId(r.get("REGISTRO_ID", Integer.class));
//    				registro.setSezione(r.get("SEZIONE", Integer.class));
//    				registro.setInseritoInSorveglianza(r.get("INSERITO_IN_SORVEGLIANZA", Boolean.class));
//    				registro.setTipoElencoInail(r.get("TIPO_ELENCO_INAIL", String.class));
//
//    				row.setNascitaData(r.get("NASCITA_DATA", LocalDate.class));
//
//    				String nomeChiaro = r.get("NOME_CHIARO", String.class);
//    				row.setNome(nomeChiaro != null ? nomeChiaro.toUpperCase() : "");
//
//    				String cognomeChiaro = r.get("COGNOME_CHIARO", String.class);
//    				row.setCognome(cognomeChiaro != null ? cognomeChiaro.toUpperCase() : "");
//
//    				row.setSesso(r.get("SESSO", String.class));
//    				row.setCodiceFiscale(r.get("CODICE_FISCALE", String.class));
//    				row.setDomicilioAslId(r.get("DOMICILIO_ASL_ID", Integer.class));
//    				row.setResidenzaAslId(r.get("RESIDENZA_ASL_ID", Integer.class));
//
//    				Integer assistenzaId = r.get("ASSISTENZA_ASL_ID", Integer.class);
//    				row.setAssistenzaAslId(assistenzaId != null ? String.format("%06d", assistenzaId) : null);
//
//    				row.setDescrizioneAslCompetenza(r.get("ASL_AZIENDA_DESC", String.class));
//    				row.setTelefonoAura(r.get("TELEFONO_AURA", String.class));
//    				row.setEmailAura(r.get("EMAIL_AURA", String.class));
//    				row.setDescrizioneStato(r.get("SOGGETTO_STATO_DESC", String.class));
//    				row.setSoggettoStatoNote(r.get("SOGGETTO_STATO_NOTE", String.class));
//    				row.setSoggettoStatoPrecedenteId(r.get("SOGGETTO_STATO_PRECEDENTE_ID", Integer.class));
//
//    				if (soggettoId != null) {
//    					row.setListaFonteByIdSoggetto(
//    							fonteRepository != null
//    							? fonteRepository.findListaFonteDescByIdSoggetto(soggettoId)
//    									: Collections.emptyList()
//    							);
//
////    					row.setListaEsenzione(
////    							esenzioneRepository != null
////    							? esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId)
////    									: Collections.emptyList()
////    							);
//    					row.setListaEsenzione(Collections.emptyList());
//    				} else {
//    					row.setListaFonteByIdSoggetto(Collections.emptyList());
//    					row.setListaEsenzione(Collections.emptyList());
//    				}
//
//    				LocalDateTime adesioneData = r.get("ADESIONE_DATA", LocalDateTime.class);
//    				row.setAdesioneData(adesioneData != null ? adesioneData.toString() : null);
//
//    				LocalDateTime presentazione = r.get("PRESENTAZIONE_ISTANZA_DATA", LocalDateTime.class);
//    				row.setPresentazioneIstanzaData(
//    						presentazione != null ? presentazione.toLocalDate().toString() : null
//    						);
//
//    				return row;
//    			})
//    			.filter(dto -> dto.getSoggettoId() != null && isValidoPerExport(dto.getSoggettoId().intValue(), profiloUtente))
//    			.toList();
//    }

    
    public void salvaUltimoRegistroIdProcessato(Integer nuovoLastRegistroId, AuditLogRequest auditLogRequest) {
        String parametroCod = auditLogRequest.getUtente() + "_ContatoreProssimoStep";
        String valore = String.valueOf(nuovoLastRegistroId);

        int updated = dsl.update(REEA_C_PARAMETRO)
                .set(REEA_C_PARAMETRO.PARAMETRO_VALORE, valore)
                .where(REEA_C_PARAMETRO.PARAMETRO_COD.eq(parametroCod))
                .execute();

        if (updated == 0) {
            dsl.insertInto(REEA_C_PARAMETRO)
                    .set(REEA_C_PARAMETRO.PARAMETRO_COD, parametroCod)
                    .set(REEA_C_PARAMETRO.PARAMETRO_VALORE, valore)
                    .set(REEA_C_PARAMETRO.PARAMETRO_TIPO_ID, 8)
                    .set(REEA_C_PARAMETRO.UTENTE_CREAZIONE, auditLogRequest.getUtente())
	                .set(REEA_C_PARAMETRO.UTENTE_MODIFICA, auditLogRequest.getUtente())
                    .execute();
        }
    }


	private Integer getUltimoRegistroIdProcessato(AuditLogRequest auditLogRequest) {
        String parametroCod = auditLogRequest.getUtente() + "_ContatoreProssimoStep";

        String valore = dsl
                .select(REEA_C_PARAMETRO.PARAMETRO_VALORE)
                .from(REEA_C_PARAMETRO)
                .where(REEA_C_PARAMETRO.PARAMETRO_COD.eq(parametroCod))
                .fetchOneInto(String.class);

        if (valore == null || valore.isBlank()) {
            return 0;
        }

        return Integer.valueOf(valore);
    }
	
	
	public void azzeraUltimoRegistroIdProcessato(String utente) {
	    String parametroCod = utente + "_ContatoreProssimoStep";

	    int updated = dsl.update(REEA_C_PARAMETRO)
	            .set(REEA_C_PARAMETRO.PARAMETRO_VALORE, "0")
	            .where(REEA_C_PARAMETRO.PARAMETRO_COD.eq(parametroCod))
	            .execute();

	    if (updated == 0) {
	        dsl.insertInto(REEA_C_PARAMETRO)
	                .set(REEA_C_PARAMETRO.PARAMETRO_COD, parametroCod)
	                .set(REEA_C_PARAMETRO.PARAMETRO_VALORE, "0")
	                .set(REEA_C_PARAMETRO.PARAMETRO_TIPO_ID, 8)
	                .set(REEA_C_PARAMETRO.UTENTE_CREAZIONE, utente)
	                .set(REEA_C_PARAMETRO.UTENTE_MODIFICA, utente)
	                .execute();
	    }
	}


	@Override
    public Integer countListaAnagrafica(List<Integer> filtroFonteId,
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
                                        String profiloUtente) {


    	//DE Aggiungo questo boolean che fa da switch sulla decrypt  in line di nome e cognome cifrato
    	boolean ricercaPerNominativo = (nome != null && !nome.isBlank())  || (cognome != null && !cognome.isBlank()||cognomeLettA != null && !cognomeLettA.isBlank())  || (cognomeLettDa != null && !cognomeLettDa.isBlank());

    	 // non serve (in questo caso) eseguire la query con filtri su campi criptati quindi utilizzo quella fast
		if (!ricercaPerNominativo) {
		    return countListaAnagraficaFast(
		    		filtroFonteId,
                    descrizioniStato,
                    sezione,
                    insInSorveglianza,
                    tipoElencoInail,
                    assistenzaAslId,
                    codiceFiscale,
                    cognome,
                    nome,
                    cognomeLettDa,
                    cognomeLettA,
                    nascitaData,
                    profiloUtente);
		}
        Condition condDescrizioneStato = noCondition();
        if (descrizioniStato != null && !descrizioniStato.isEmpty()) {
            condDescrizioneStato = REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.in(descrizioniStato);
        }

        Condition condSezione = noCondition();
        if (sezione != null && !sezione.isEmpty()) {
            condSezione = REEA_T_REGISTRO.SEZIONE.in(sezione);
        }

        Condition condInseritoInSorveglianza = noCondition();
        if (insInSorveglianza != null) {
            condInseritoInSorveglianza = REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.eq(insInSorveglianza);
        }

        Condition condTipoElencoInail = noCondition();
        if (tipoElencoInail != null && !tipoElencoInail.isEmpty()) {
            condTipoElencoInail = REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(tipoElencoInail.toUpperCase());
        }

        Condition condAssistenzaAsl = noCondition();
        Condition condAssistenzaAslStorico = noCondition();
        if (assistenzaAslId != null && !assistenzaAslId.isEmpty()) {
            condAssistenzaAsl = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
            condAssistenzaAslStorico = REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
        }

        Condition condCodiceFiscaleT = noCondition();
        Condition condCodiceFiscaleS = noCondition();
        if (codiceFiscale != null && !codiceFiscale.isEmpty()) {
            condCodiceFiscaleT = REEA_T_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
            condCodiceFiscaleS = REEA_S_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
        }
        
        Condition condNascitaDataT = noCondition();
        Condition condNascitaDataS = noCondition();
        if (nascitaData != null) {
            condNascitaDataT = REEA_T_SOGGETTO.NASCITA_DATA.eq(nascitaData);
            condNascitaDataS = REEA_S_SOGGETTO.NASCITA_DATA.eq(nascitaData);
        }

        Condition condGlobale = condDescrizioneStato
                .and(condSezione)
                .and(condInseritoInSorveglianza)
                .and(condTipoElencoInail)
                .and(condAssistenzaAsl)
                .and(condCodiceFiscaleT)
                .and(condNascitaDataT);

        Condition condGlobaleStorico = condDescrizioneStato
                .and(condSezione)
                .and(condInseritoInSorveglianza)
                .and(condTipoElencoInail)
                .and(condAssistenzaAslStorico)
                .and(condCodiceFiscaleS)
                .and(condNascitaDataS);

        Condition condFonteGlobaleT = noCondition();
        if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
            condFonteGlobaleT = REEA_T_SOGGETTO.FONTE_ID.in(filtroFonteId)
                    .or(REEA_T_SOGGETTO.SOGGETTO_ID.in(
                            dsl.selectDistinct(REEA_S_SOGGETTO.SOGGETTO_ID)
                                    .from(REEA_S_SOGGETTO)
                                    .where(REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId))
                    ));
        }

        Condition condFonteGlobaleS = noCondition();
        if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
            condFonteGlobaleS = REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId);
        }

        Select<Record9<Integer,String,String,byte[],byte[],byte[],byte[],byte[],byte[]>> selectCorrente = dsl
        .selectDistinct(
                REEA_T_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),

                DSL.when(REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
                        field("pgp_sym_decrypt({0}, {1})", String.class,
                                REEA_T_SOGGETTO.NOME_CIFRATO, val("16<odcc8!"))
                ).otherwise((String) null).as("NOME_CHIARO"),

                DSL.when(REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
                        field("pgp_sym_decrypt({0}, {1})", String.class,
                                REEA_T_SOGGETTO.COGNOME_CIFRATO, val("16<odcc8!"))
                ).otherwise((String) null).as("COGNOME_CHIARO"),

                // MM aggiungo le colonne HASH
                REEA_T_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
                REEA_T_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
                REEA_T_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
                REEA_T_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
                REEA_T_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
                REEA_T_SOGGETTO.NOME_HASH.as("NOME_HASH")
        );
    	        
       if ("REEA_OP_SPRESAL".equals(profiloUtente)
               || "REEA_OP_CRPT".equals(profiloUtente)
               || "REEA_OP_CSI".equals(profiloUtente)
               || "REEA_OP_CRPT_PSEUDO".equals(profiloUtente)
               || "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
               || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
               || "REEA_OP_CSI_PSEUDO".equals(profiloUtente)) {
//    	   boolean includiSenzaAdesione = "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente) || "REEA_OP_CRPT".equals(profiloUtente);
        selectCorrente = dsl
                .selectDistinct(
                        REEA_T_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
                        DSL.when(REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
                                field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_T_SOGGETTO.NOME_CIFRATO, val("16<odcc8!"))
                        ).otherwise((String) null).as("NOME_CHIARO"),
                        DSL.when(REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
                                field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_T_SOGGETTO.COGNOME_CIFRATO, val("16<odcc8!"))
                        ).otherwise((String) null).as("COGNOME_CHIARO"),
                     
                        // MM aggiungo le colonne HASH
                        REEA_T_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
                        REEA_T_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
                        REEA_T_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
                        REEA_T_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
                        REEA_T_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
                        REEA_T_SOGGETTO.NOME_HASH.as("NOME_HASH")
                )
                .from(REEA_T_SOGGETTO)

                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
                .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                        field("reea.hmac_soggetto_id({0}, {1})",
                                String.class,
                                REEA_T_SOGGETTO.SOGGETTO_ID,
                                val("16<odcc8!"))))
                .join(REEA_D_SOGGETTO_STATO)
                .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
                .leftJoin(REEA_T_ADESIONE)
                .on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID)
                        .and(REEA_T_ADESIONE.SOGGETTO_ID.isNotNull()))
                .where(condGlobale.and(condFonteGlobaleT));
//                .and(includiSenzaAdesione ? noCondition() : REEA_T_ADESIONE.SOGGETTO_ID.isNotNull());
       }
       
       Select<Record9<Integer,String,String,byte[],byte[],byte[],byte[],byte[],byte[]>> selectStorico = dsl
    	        .selectDistinct(
    	                REEA_T_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),

    	                DSL.when(REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
    	                        field("pgp_sym_decrypt({0}, {1})", String.class,
    	                                REEA_T_SOGGETTO.NOME_CIFRATO, val("16<odcc8!"))
    	                ).otherwise((String) null).as("NOME_CHIARO"),

    	                DSL.when(REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
    	                        field("pgp_sym_decrypt({0}, {1})", String.class,
    	                                REEA_T_SOGGETTO.COGNOME_CIFRATO, val("16<odcc8!"))
    	                ).otherwise((String) null).as("COGNOME_CHIARO"),

    	                // MM aggiungo le colonne HASH
    	                REEA_T_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
    	                REEA_T_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
    	                REEA_T_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
    	                REEA_T_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
    	                REEA_T_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
    	                REEA_T_SOGGETTO.NOME_HASH.as("NOME_HASH")
    	        );
       
       if ("REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
               || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
               || "REEA_OP_SPRESAL".equals(profiloUtente)
               || "REEA_OP_CSI_PSEUDO".equals(profiloUtente)) {
        selectStorico = dsl
                .selectDistinct(
                        REEA_S_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"),
                        DSL.when(REEA_S_SOGGETTO.NOME_CIFRATO.isNotNull(),
                                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_S_SOGGETTO.NOME_CIFRATO, DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("NOME_CHIARO"),
                        DSL.when(REEA_S_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
                                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_S_SOGGETTO.COGNOME_CIFRATO, DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("COGNOME_CHIARO"),
                        
                        // MM aggiungo le colonne HASH
                        REEA_S_SOGGETTO.COGNOME_HASH_1CHAR.as("COGNOME_HASH_1CHAR"),
                        REEA_S_SOGGETTO.COGNOME_HASH_2CHAR.as("COGNOME_HASH_2CHAR"),
                        REEA_S_SOGGETTO.COGNOME_HASH.as("COGNOME_HASH"),
                        REEA_S_SOGGETTO.NOME_HASH_1CHAR.as("NOME_HASH_1CHAR"),
                        REEA_S_SOGGETTO.NOME_HASH_2CHAR.as("NOME_HASH_2CHAR"),
                        REEA_S_SOGGETTO.NOME_HASH.as("NOME_HASH")
                )
                .from(REEA_S_SOGGETTO)

                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID))
                .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                        field("reea.hmac_soggetto_id({0}, {1})",
                                String.class,
                                REEA_S_SOGGETTO.SOGGETTO_ID,
                                val("16<odcc8!"))))
                .join(REEA_D_SOGGETTO_STATO)
                .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_S_SOGGETTO.SOGGETTO_STATO_ID))
                .where(condGlobaleStorico
                        .and(condFonteGlobaleS)
                        .and(REEA_S_SOGGETTO.SOGGETTO_ID.notIn(
                                dsl.selectDistinct(REEA_T_SOGGETTO.SOGGETTO_ID).from(REEA_T_SOGGETTO)
                        )));
       }
        boolean includeStorico = "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
                || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
                || "REEA_OP_SPRESAL".equals(profiloUtente)
                || "REEA_OP_CSI_PSEUDO".equals(profiloUtente);

        Table<?> unionTable;
        if (includeStorico) {
            unionTable = selectCorrente.unionAll(selectStorico).asTable("q");
        } else {
            unionTable = selectCorrente.asTable("q");
        }

        // MM disabilito le condizioni che non utilizzano le colonne HASH
        //Condition condOuterCognome = noCondition();
        //if (cognome != null && !cognome.isEmpty()) {
        //    condOuterCognome = DSL.field(DSL.name("q", "COGNOME_CHIARO"), String.class)
        //            .likeIgnoreCase(cognome + "%");
        //}

        //Condition condOuterNome = noCondition();
        //if (nome != null && !nome.isEmpty()) {
        //    condOuterNome = DSL.field(DSL.name("q", "NOME_CHIARO"), String.class)
        //            .likeIgnoreCase(nome + "%");
        //}

        //Condition condCognomeLettDa = noCondition();
        //if (cognomeLettDa != null && !cognomeLettDa.isEmpty()) {
        //    condCognomeLettDa = DSL.upper(DSL.left(
        //            DSL.field(DSL.name("q", "COGNOME_CHIARO"), String.class), 1))
        //            .greaterOrEqual(cognomeLettDa.trim().toUpperCase());
        //}

        //Condition condCognomeLettA = noCondition();
        //if (cognomeLettA != null && !cognomeLettA.isEmpty()) {
        //    condCognomeLettA = DSL.upper(DSL.left(
        //            DSL.field(DSL.name("q", "COGNOME_CHIARO"), String.class), 1))
        //            .lessOrEqual(cognomeLettA.trim().toUpperCase());
        //}


//        return dsl.selectCount()
//                .from(unionTable)
//                .where(condOuterCognome.and(condOuterNome).and(condCognomeLettDa).and(condCognomeLettA))
//                .fetchOne(0, Integer.class);
        

        // MM inserite nuove condizioni che utilizzando le colonne HASH
        // -----------------------------
	    // Filtro COGNOME (HMAC)
	    // -----------------------------
        Condition condOuterCognome = noCondition();
	    if (cognome != null && !cognome.isEmpty()) {
	
	        String value = cognome.trim().toUpperCase();
	        int len = value.length();
	
	        // HMAC del valore cercato
	        Field<byte[]> hmacValue = DSL.function(
	                "hmac",
	                SQLDataType.BLOB,
	                DSL.val(value, SQLDataType.VARCHAR),
	                DSL.inline("Porcatrota2000!"),
	                DSL.inline("sha256")
	        );
	
	        if (len == 1) {
	            condOuterCognome = DSL.field(DSL.name("q", "COGNOME_HASH_1CHAR"), byte[].class)
	                    .eq(hmacValue);
	        } else if (len == 2) {
	            condOuterCognome = DSL.field(DSL.name("q", "COGNOME_HASH_2CHAR"), byte[].class)
	                    .eq(hmacValue);
	        } else {
	            condOuterCognome = DSL.field(DSL.name("q", "COGNOME_HASH"), byte[].class)
	                    .eq(hmacValue);
	        }
	    }
	
	    // -----------------------------
	    // Filtro NOME (HMAC)
	    // -----------------------------
	    Condition condOuterNome = noCondition();
	    if (nome != null && !nome.isEmpty()) {
	
	        String value = nome.trim().toUpperCase();
	        int len = value.length();
	
	        Field<byte[]> hmacValue = DSL.function(
	                "hmac",
	                SQLDataType.BLOB,
	                DSL.val(value, SQLDataType.VARCHAR),
	                DSL.inline("Porcatrota2000!"),
	                DSL.inline("sha256")
	        );
	
	        if (len == 1) {
	            condOuterNome = DSL.field(DSL.name("q", "NOME_HASH_1CHAR"), byte[].class)
	                    .eq(hmacValue);
	        } else if (len == 2) {
	            condOuterNome = DSL.field(DSL.name("q", "NOME_HASH_2CHAR"), byte[].class)
	                    .eq(hmacValue);
	        } else {
	            condOuterNome = DSL.field(DSL.name("q", "NOME_HASH"), byte[].class)
	                    .eq(hmacValue);
	        }
	    }
	
		// -----------------------------
		// Filtro alfabetico COGNOME DA - A (range di lettere)
		// -----------------------------
		Condition condCognomeRange = noCondition();
	
		if (cognomeLettDa != null && !cognomeLettDa.isEmpty()
		        && cognomeLettA != null && !cognomeLettA.isEmpty()) {
	
		    // I due caratteri sono sempre singoli e maiuscoli
		    char da = cognomeLettDa.trim().charAt(0);
		    char a  = cognomeLettA.trim().charAt(0);
	
		    // Costruisco la lista delle lettere comprese nel range
		    List<String> lettere = new ArrayList<>();
		    for (char c = da; c <= a; c++) {
		        lettere.add(String.valueOf(c));
		    }
	
		    // Costruisco la lista degli HMAC corrispondenti
		    List<Field<byte[]>> hmacList = new ArrayList<>();
		    for (String letter : lettere) {
		        Field<byte[]> hmacLetter = DSL.function(
		                "hmac",
		                SQLDataType.BLOB,
		                DSL.val(letter, SQLDataType.VARCHAR),
		                DSL.inline("Porcatrota2000!"),
		                DSL.inline("sha256")
		        );
		        hmacList.add(hmacLetter);
		    }
	
		    // Costruisco la condizione IN (...)
		    condCognomeRange = DSL.field(DSL.name("q", "COGNOME_HASH_1CHAR"), byte[].class)
		            .in(hmacList);
		}
	    // MM fine //
        
		// MM disabilito la return
		//return dsl.selectCount()
		//        .from(unionTable)
		//        .where(condOuterCognome.and(condOuterNome).and(condCognomeLettDa).and(condCognomeLettA))
		//        .fetchOne(0, Integer.class);

		// MM inserisco la nuova return
//		return dsl.selectCount()
//		        .from(unionTable)
//		        .where(
//		            condOuterCognome
//		            .and(condOuterNome)
//		            .and(condCognomeRange)
//		        )
//		        .fetchOne(0, Integer.class);
		
//		return Math.toIntExact(
//                dsl.selectFrom(unionTable)
//                        .where(condOuterCognome.and(condOuterNome).and(condCognomeRange))
//                        .fetch()
//                        .stream()
//                        .filter(r -> isValidoPerExport(r.get("SOGGETTO_ID", Integer.class), profiloUtente))
//                        .count()
//        );
		
		Field<Integer> soggettoIdField = DSL.field(
			    DSL.name("SOGGETTO_ID"),
			    Integer.class
			);

			return Math.toIntExact(
			    dsl.selectDistinct(soggettoIdField)
			        .from(unionTable)
			        .where(condOuterCognome.and(condOuterNome).and(condCognomeRange))
			        .fetch()
			        .stream()
			        .map(r -> r.get(soggettoIdField))
			        .filter(Objects::nonNull)
			        .filter(soggettoId -> isValidoPerExport(soggettoId, profiloUtente))
			        .count()
			);
		
    }

    // DE METODO CREATO PER ESEGUIRE UNA QERY PI� VELOCE ANCHE SULLA COUNT (OVVERO NON TIRO SU IL DEVIFRATO)
    @Override
    public Integer countListaAnagraficaFast(List<Integer> filtroFonteId,
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
                                        String profiloUtente) {
    	  //WHERE DINAMICHE
    	   Condition condDescrizioneStato = noCondition();
           if (descrizioniStato != null && !descrizioniStato.isEmpty()) {
               condDescrizioneStato = REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.in(descrizioniStato);
           }

           Condition condSezione = noCondition();
           if (sezione != null && !sezione.isEmpty()) {
               condSezione = REEA_T_REGISTRO.SEZIONE.in(sezione);
           }

           Condition condInseritoInSorveglianza = noCondition();
           if (insInSorveglianza != null) {
               condInseritoInSorveglianza = REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.eq(insInSorveglianza);
           }

           Condition condTipoElencoInail = noCondition();
           if (tipoElencoInail != null && !tipoElencoInail.isEmpty()) {
               condTipoElencoInail = REEA_T_REGISTRO.TIPO_ELENCO_INAIL.eq(tipoElencoInail.toUpperCase());
           }

           Condition condAssistenzaAsl = noCondition();
           Condition condAssistenzaAslStorico = noCondition();
           if (assistenzaAslId != null && !assistenzaAslId.isEmpty()) {
               condAssistenzaAsl = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
               condAssistenzaAslStorico = REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
           }

           Condition condCodiceFiscaleT = noCondition();
           Condition condCodiceFiscaleS = noCondition();
           if (codiceFiscale != null && !codiceFiscale.isEmpty()) {
               condCodiceFiscaleT = REEA_T_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
               condCodiceFiscaleS = REEA_S_SOGGETTO.CODICE_FISCALE.equalIgnoreCase(codiceFiscale);
           }
           
           Condition condNascitaDataT = noCondition();
           Condition condNascitaDataS = noCondition();
           if (nascitaData != null) {
               condNascitaDataT = REEA_T_SOGGETTO.NASCITA_DATA.eq(nascitaData);
               condNascitaDataS = REEA_S_SOGGETTO.NASCITA_DATA.eq(nascitaData);
           }

           Condition condGlobale = condDescrizioneStato
                   .and(condSezione)
                   .and(condInseritoInSorveglianza)
                   .and(condTipoElencoInail)
                   .and(condAssistenzaAsl)
                   .and(condCodiceFiscaleT)
                   .and(condNascitaDataT);

           Condition condGlobaleStorico = condDescrizioneStato
                   .and(condSezione)
                   .and(condInseritoInSorveglianza)
                   .and(condTipoElencoInail)
                   .and(condAssistenzaAslStorico)
                   .and(condCodiceFiscaleS)
                   .and(condNascitaDataS);

           Condition condFonteGlobaleT = noCondition();
           if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
               condFonteGlobaleT = REEA_T_SOGGETTO.FONTE_ID.in(filtroFonteId)
                       .or(REEA_T_SOGGETTO.SOGGETTO_ID.in(
                               dsl.selectDistinct(REEA_S_SOGGETTO.SOGGETTO_ID)
                                       .from(REEA_S_SOGGETTO)
                                       .where(REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId))
                       ));
           }

           Condition condFonteGlobaleS = noCondition();
           if (filtroFonteId != null && !filtroFonteId.isEmpty()) {
               condFonteGlobaleS = REEA_S_SOGGETTO.FONTE_ID.in(filtroFonteId);
           }
    	  // QUERY
//           Select<Record1<Integer>> selectCorrente = null;
           Select<Record1<Integer>> selectCorrente =
        	        dsl.select(DSL.val((Integer) null).as("SOGGETTO_ID"))
        	           .where(DSL.falseCondition());   // nessuna riga
           if ("REEA_OP_SPRESAL".equals(profiloUtente)
                   || "REEA_OP_CRPT".equals(profiloUtente)
                   || "REEA_OP_CSI".equals(profiloUtente)
                   || "REEA_OP_CRPT_PSEUDO".equals(profiloUtente)
                   || "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
                   || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
                   || "REEA_OP_CSI_PSEUDO".equals(profiloUtente)) {
//               boolean includiSenzaAdesione = "REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente) || "REEA_OP_CRPT".equals(profiloUtente);
           selectCorrente = dsl
                   .selectDistinct(
                           REEA_T_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"))            
                   .from(REEA_T_SOGGETTO)

                   .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
                   .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
                   .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                           field("reea.hmac_soggetto_id({0}, {1})",
                                   String.class,
                                   REEA_T_SOGGETTO.SOGGETTO_ID,
                                   val("16<odcc8!"))))
                   .join(REEA_D_SOGGETTO_STATO)
                   .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
                   .leftJoin(REEA_T_ADESIONE)
                   .on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID)
                           .and(REEA_T_ADESIONE.SOGGETTO_ID.isNotNull()))
                   .where(condGlobale.and(condFonteGlobaleT));
//                   .and(includiSenzaAdesione ? noCondition() : REEA_T_ADESIONE.SOGGETTO_ID.isNotNull());
           }
           Select<Record1<Integer>> selectStorico = null;
           
           if ("REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
                   || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
                   || "REEA_OP_SPRESAL".equals(profiloUtente)
                   || "REEA_OP_CSI_PSEUDO".equals(profiloUtente)) {
           selectStorico = dsl
                   .selectDistinct(
                           REEA_S_SOGGETTO.SOGGETTO_ID.as("SOGGETTO_ID"))
                  
                   .from(REEA_S_SOGGETTO)

                   .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
                   .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID))
                   .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                           field("reea.hmac_soggetto_id({0}, {1})",
                                   String.class,
                                   REEA_S_SOGGETTO.SOGGETTO_ID,
                                   val("16<odcc8!"))))
                   .join(REEA_D_SOGGETTO_STATO)
                   .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_S_SOGGETTO.SOGGETTO_STATO_ID))
                   .where(condGlobaleStorico
                           .and(condFonteGlobaleS)
                           .and(REEA_S_SOGGETTO.SOGGETTO_ID.notIn(
                                   dsl.selectDistinct(REEA_T_SOGGETTO.SOGGETTO_ID).from(REEA_T_SOGGETTO)
                           )));
           }

           var unionTable = selectCorrente
                   .unionAll(selectStorico)
                   .asTable("q");

           //ESECUZIONE
//           return dsl.selectCount()
//                   .from(unionTable)
//                   .fetchOne(0, Integer.class);	
           
           return Math.toIntExact(
        	        dsl.selectFrom(unionTable)
        	                .fetch()
        	                .stream()
        	                .filter(r -> isValidoPerExport(r.get("SOGGETTO_ID", Integer.class), profiloUtente))
        	                .count()
        	);
    }
	
	
    @Override
    public Integer inserisciSoggetto(DSLContext ctx,
                                     AnagraficaDTO dtos,
                                     Integer elaborazioneId,
                                     String utente,
                                     Integer tipoOperazione) {

        LocalDate presentazione = DateConversionUtils.stringToLocalDate(dtos.getPresentazioneIstanzaData());
        LocalDate decesso = DateConversionUtils.stringToLocalDate(dtos.getDataDecesso());
        LocalDate assistenza = DateConversionUtils.stringToLocalDate(dtos.getAssistenzaAslFine());

        String nascitaProvDesc = dtos.getNascitaProvinciaDesc();
        String nascitaComuneDesc = dtos.getNascitaComuneDesc();
        String domicilioComuneCod = dtos.getDomicilioComuneCod();
        String domicilioComuneDesc = dtos.getDomicilioComuneDesc();
        String domicilioCap = dtos.getDomicilioCap();
        String tesseraTeam = dtos.getTesseraTeam();
        String idAura = dtos.getIdAura();
        String email = dtos.getEmail();
        String telefono = dtos.getTelefono();

        boolean idAuraAssente = idAura == null || idAura.isBlank();

        Integer residenzaAslId = idAuraAssente
                ? Integer.valueOf(1174)
                : dtos.getResidenzaAslId();

        Integer domicilioAslId = idAuraAssente
                ? Integer.valueOf(1174)
                : dtos.getDomicilioAslId();

        Integer assistenzaAslId = null;
        if (dtos.getAssistenzaAslId() != null && !dtos.getAssistenzaAslId().isBlank()) {
            assistenzaAslId = Integer.valueOf(dtos.getAssistenzaAslId());
        }

        Integer soggettoId = null;

        try {
            var inserted = ctx.insertInto(REEA_T_SOGGETTO)
                    .set(REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA,
                            tesseraTeam != null && !tesseraTeam.isBlank()
                                    ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                            DSL.val(tesseraTeam, SQLDataType.VARCHAR),
                                            DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                    : null)
                    .set(REEA_T_SOGGETTO.ID_AURA_CIFRATO,
                            idAura != null && !idAura.isBlank()
                                    ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                            DSL.val(idAura, SQLDataType.VARCHAR),
                                            DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                    : null)
                    .set(REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO,
                            dtos.getResidenzaIndirizzo() != null && !dtos.getResidenzaIndirizzo().isBlank()
                                    ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                            DSL.val(dtos.getResidenzaIndirizzo(), SQLDataType.VARCHAR),
                                            DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                    : null)
                    .set(REEA_T_SOGGETTO.NASCITA_STATO_DESC, dtos.getNascitaStatoDesc())
                    .set(REEA_T_SOGGETTO.CITTADINANZA_STATO_COD, dtos.getCittadinanzaStatoCod())
                    .set(REEA_T_SOGGETTO.CITTADINANZA_STATO_DESC, dtos.getCittadinanzaStatoDesc())
                    .set(REEA_T_SOGGETTO.DOMICILIO_COMUNE_DESC, domicilioComuneDesc)
                    .set(REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_DESC, dtos.getDomicilioProvinciaDesc())
                    .set(REEA_T_SOGGETTO.DOMICILIO_STATO_DESC, dtos.getDomicilioStatoDesc())
                    .set(REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_COD, dtos.getDomicilioProvinciaCod())
                    .set(REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD, dtos.getResidenzaComuneCod())
                    .set(REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC, dtos.getResidenzaComuneDesc())
                    .set(REEA_T_SOGGETTO.RESIDENZA_STATO_COD, dtos.getResidenzaStatoCod())
                    .set(REEA_T_SOGGETTO.RESIDENZA_STATO_DESC, dtos.getResidenzaStatoDesc())
                    .set(REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD, dtos.getResidenzaProvinciaCod())
                    .set(REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC, dtos.getResidenzaProvinciaDesc())
                    .set(REEA_T_SOGGETTO.RESIDENZA_CAP, dtos.getResidenzaCap())
                    .set(REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO, dtos.getDomicilioNumeroCivico())
                    .set(REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO, dtos.getResidenzaNumeroCivico())
                    .set(REEA_T_SOGGETTO.CODICE_FISCALE, dtos.getCodiceFiscale())
                    .set(REEA_T_SOGGETTO.RESIDENZA_ASL_ID, residenzaAslId)
                    .set(REEA_T_SOGGETTO.DOMICILIO_ASL_ID, domicilioAslId)
                    .set(REEA_T_SOGGETTO.NOME_CIFRATO,
                            DSL.function("pgp_sym_encrypt",
                                    SQLDataType.BLOB,
                                    DSL.val(dtos.getNome() != null ? dtos.getNome().toUpperCase() : null, SQLDataType.VARCHAR),
                                    DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                    .set(REEA_T_SOGGETTO.COGNOME_CIFRATO,
                            DSL.function("pgp_sym_encrypt",
                                    SQLDataType.BLOB,
                                    DSL.val(dtos.getCognome() != null ? dtos.getCognome().toUpperCase() : null, SQLDataType.VARCHAR),
                                    DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                    .set(REEA_T_SOGGETTO.NASCITA_DATA, dtos.getNascitaData())
                    .set(REEA_T_SOGGETTO.SESSO, dtos.getSesso())
                    .set(REEA_T_SOGGETTO.VALIDITA_INIZIO, DSL.currentLocalDateTime())
                    .set(REEA_T_SOGGETTO.FONTE_ID, dtos.getFonteId())
                    .set(REEA_T_SOGGETTO.UTENTE_CREAZIONE, dtos.getUtenteCreazione())
                    .set(REEA_T_SOGGETTO.DATA_CREAZIONE, DSL.currentLocalDateTime())
                    .set(REEA_T_SOGGETTO.INSERIMENTO_TIPO_ID, dtos.getInserimentoTipoId())
                    .set(REEA_T_SOGGETTO.SOGGETTO_STATO_ID, dtos.getSoggettoStatoId())
                    .set(REEA_T_SOGGETTO.VERSIONE_NUMERO, 1)
                    .set(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID, assistenzaAslId)
                    .set(REEA_T_SOGGETTO.NASCITA_STATO_COD, dtos.getNascitaStatoCod())
                    .set(REEA_T_SOGGETTO.NASCITA_PROVINCIA_COD, dtos.getNascitaProvinciaCod())
                    .set(REEA_T_SOGGETTO.NASCITA_PROVINCIA_DESC, nascitaProvDesc)
                    .set(REEA_T_SOGGETTO.NASCITA_COMUNE_COD, dtos.getNascitaComuneCod())
                    .set(REEA_T_SOGGETTO.NASCITA_COMUNE_DESC, nascitaComuneDesc)
                    .set(REEA_T_SOGGETTO.DOMICILIO_COMUNE_COD, domicilioComuneCod)
                    .set(REEA_T_SOGGETTO.DOMICILIO_STATO_COD, dtos.getDomicilioStatoCod())
                    .set(REEA_T_SOGGETTO.DOMICILIO_CAP, domicilioCap)
                    .set(REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO,
                            dtos.getDomicilioIndirizzo() != null && !dtos.getDomicilioIndirizzo().isBlank()
                                    ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                            DSL.val(dtos.getDomicilioIndirizzo(), SQLDataType.VARCHAR),
                                            DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                    : null)
                    .set(REEA_T_SOGGETTO.EMAIL_CIFRATA,
                            email != null && !email.isBlank()
                                    ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                            DSL.val(email, SQLDataType.VARCHAR),
                                            DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                    : null)
                    .set(REEA_T_SOGGETTO.TELEFONO_CIFRATO,
                            telefono != null && !telefono.isBlank()
                                    ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                            DSL.val(telefono, SQLDataType.VARCHAR),
                                            DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                    : null)
                    .set(REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO,
                            dtos.getTelefonoAura() != null && !dtos.getTelefonoAura().isBlank()
                                    ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                            DSL.val(dtos.getTelefonoAura(), SQLDataType.VARCHAR),
                                            DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                    : null)
                    .set(REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA,
                            dtos.getEmailAura() != null && !dtos.getEmailAura().isBlank()
                                    ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                            DSL.val(dtos.getEmailAura(), SQLDataType.VARCHAR),
                                            DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                   : null)          
                   .set(REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA,
                            presentazione != null ? presentazione.atStartOfDay() : null)
                    .set(REEA_T_SOGGETTO.DATA_DECESSO,
                            decesso != null ? decesso.atStartOfDay() : null)
                    .set(REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE,
                            assistenza != null ? assistenza.atStartOfDay() : null)
                    
                    // HASH COMPLETI (PER ORA LI RICALCOLO POTREMMO POI MODIFICARE IL DTO)
                    .set(
                    	REEA_T_SOGGETTO.COGNOME_HASH,
                        DSL.function(
                            "hmac",
                            SQLDataType.BLOB,
                            DSL.val(
                                dtos.getCognome() != null
                                    ? dtos.getCognome().toUpperCase()
                                    : null,
                                SQLDataType.VARCHAR
                            ),
                            DSL.inline("Porcatrota2000!"),
                            DSL.inline("sha256")
                        )
                    )
                    .set(
                        REEA_T_SOGGETTO.NOME_HASH,
                        DSL.function(
                            "hmac",
                            SQLDataType.BLOB,
                            DSL.val(
                                dtos.getNome() != null
                                    ? dtos.getNome().toUpperCase()
                                    : null,
                                SQLDataType.VARCHAR
                            ),
                            DSL.inline("Porcatrota2000!"),
                            DSL.inline("sha256")
                        )
                    )

                    // HASH PRIME 2 LETTERE
                    .set(
                        REEA_T_SOGGETTO.COGNOME_HASH_2CHAR,
                        DSL.function(
                            "hmac",
                            SQLDataType.BLOB,
                            DSL.val(
                                dtos.getCognome() != null && dtos.getCognome().length() >= 2
                                    ? dtos.getCognome().toUpperCase().substring(0, 2)
                                    : null,
                                SQLDataType.VARCHAR
                            ),
                            DSL.inline("Porcatrota2000!"),
                            DSL.inline("sha256")
                        )
                    )
                    .set(
                        REEA_T_SOGGETTO.NOME_HASH_2CHAR,
                        DSL.function(
                            "hmac",
                            SQLDataType.BLOB,
                            DSL.val(
                                dtos.getNome() != null && dtos.getNome().length() >= 2
                                    ? dtos.getNome().toUpperCase().substring(0, 2)
                                    : null,
                                SQLDataType.VARCHAR
                            ),
                            DSL.inline("Porcatrota2000!"),
                            DSL.inline("sha256")
                        )
                    )

                    // HASH PRIMA LETTERA
                    .set(
                        REEA_T_SOGGETTO.COGNOME_HASH_1CHAR,
                        DSL.function(
                            "hmac",
                            SQLDataType.BLOB,
                            DSL.val(
                                dtos.getCognome() != null && !dtos.getCognome().isBlank()
                                    ? dtos.getCognome().toUpperCase().substring(0, 1)
                                    : null,
                                SQLDataType.VARCHAR
                            ),
                            DSL.inline("Porcatrota2000!"),
                            DSL.inline("sha256")
                        )
                    )
                    .set(
                        REEA_T_SOGGETTO.NOME_HASH_1CHAR,
                        DSL.function(
                            "hmac",
                            SQLDataType.BLOB,
                            DSL.val(
                                dtos.getNome() != null && !dtos.getNome().isBlank()
                                    ? dtos.getNome().toUpperCase().substring(0, 1)
                                    : null,
                                SQLDataType.VARCHAR
                            ),
                            DSL.inline("Porcatrota2000!"),
                            DSL.inline("sha256")
                        )
                    )
                    //FINE HASH 

                    .returning(REEA_T_SOGGETTO.SOGGETTO_ID)
                    .fetchOne();

            soggettoId = inserted != null
                    ? inserted.getValue(REEA_T_SOGGETTO.SOGGETTO_ID, Integer.class)
                    : null;

            if (dtos.getAdesioneId() != null || dtos.getRegInailId() != null) {
                tracciaElaborazioneRepository.inserisciFileImpatto(
                        ctx,
                        elaborazioneId,
                        "REEA_T_SOGGETTO",
                        dtos.getAdesioneId() != null ? dtos.getAdesioneId().intValue() : dtos.getRegInailId().intValue(),
                        tipoOperazione,
                        dtos.getUtenteCreazione()
                );
            }

            return soggettoId;

        } catch (Exception e) {
            String risultato = e.getMessage();

            try {
                Integer idOrigine = dtos.getAdesioneId() != null
                        ? dtos.getAdesioneId().intValue()
                        : (dtos.getAdesioneId() != null ? dtos.getAdesioneId().intValue() : null);

                tracciaElaborazioneService.inserisciErroreRiga(
                        elaborazioneId,
                        "ERR_INSERIMENTO",
                        "inserisciSoggetto ha restituito " + risultato + " per CF " + dtos.getCodiceFiscale(),
                        null,
                        dtos.getUtenteCreazione(),
                        dtos.getCognome(),
                        dtos.getNome(),
                        dtos.getNascitaData(),
                        dtos.getSesso(),
                        dtos.getCodiceFiscale(),
                        "REEA_T_SOGGETTO",
                        idOrigine
                );
            } catch (Exception exLog) {
                System.err.println("Errore nel log scarto: " + exLog.getMessage());
            }

            throw e;
        }
    }


    @Override
    public Integer inserisciRegistro(DSLContext ctx, Integer soggettoId, AnagraficaDTO dtos, Integer elaborazioneId, String utente, Integer tipoOperazione) {

    	RegistroDTO registro = RegistroUtils.ensureRegistro(dtos);

    	try {
    		Integer ris =  ctx.insertInto(REEA_T_REGISTRO)
    				.set(REEA_T_REGISTRO.SOGGETTO_ID_HMAC,
    						DSL.field("reea.hmac_soggetto_id({0}, {1})",
    								String.class,
    								DSL.val(soggettoId),
    								DSL.val("16<odcc8!")))
    				.set(REEA_T_REGISTRO.SOGGETTO_ID_CIFRATO,
    						DSL.function("pgp_sym_encrypt_bytea",
    								byte[].class,
    								DSL.val(soggettoId.toString()).cast(SQLDataType.BLOB),
    								DSL.val("16<odcc8!")))
    				.set(REEA_T_REGISTRO.SEZIONE, registro.getSezione())
    				.set(REEA_T_REGISTRO.VERSIONE_NUMERO, 1)
    				.set(REEA_T_REGISTRO.VALIDITA_INIZIO, DSL.currentLocalDateTime())
    				.set(REEA_T_REGISTRO.UTENTE_CREAZIONE, dtos.getUtenteCreazione())
    				.returning(REEA_T_REGISTRO.REGISTRO_ID)
    				.fetchOne()
    				.getValue(REEA_T_REGISTRO.REGISTRO_ID, Integer.class);

    		if(dtos.getAdesioneId() != null)
    			tracciaElaborazioneRepository.inserisciFileImpatto(
    					ctx,
    					elaborazioneId,
    					"REEA_T_REGISTRO",
    					dtos.getAdesioneId().intValue(),
    					tipoOperazione,
    					utente
    					);

    		return ris;

    	} catch (Exception e) {

    		String risultato = e.getMessage(); // o una descrizione più parlante

    		try {
    			tracciaElaborazioneService.inserisciErroreRiga(
//    					ctx,
    					elaborazioneId,
    					"ERR_INSERIMENTO",
    					"inserisciRegistro ha restituito " + risultato + " per CF " + dtos.getCodiceFiscale(),
    					null,
    					dtos.getUtenteCreazione(),
    					dtos.getCognome(),
    					dtos.getNome(),
    					dtos.getNascitaData(),
    					dtos.getSesso(),
    					dtos.getCodiceFiscale(),
    					"REEA_T_REGISTRO",
    					dtos.getAdesioneId().intValue()
    					);
    		} catch (Exception exLog) {
    			System.err.println("Errore nel log scarto: " + exLog.getMessage());
    		}

    		// Se il metodo fa parte di una transazione e vuoi rollback, rilancia
    		throw e;
    		// In alternativa, se NON vuoi far propagare l’errore:
    		// return null;
    	}
    }



    @Override
    public Integer inserisciRegistroInail(DSLContext ctx, Integer soggettoId, AnagraficaDTO dtos, Integer elaborazioneId, String utente, Integer tipoOperazione) {

    	RegistroDTO registro = RegistroUtils.ensureRegistro(dtos);

    	try {
    		Integer ris = ctx.insertInto(REEA_T_REGISTRO)
    				.set(REEA_T_REGISTRO.SOGGETTO_ID_HMAC,
    						DSL.field("reea.hmac_soggetto_id({0}, {1})",
    								String.class,
    								DSL.val(soggettoId),
    								DSL.val("16<odcc8!")))
    				.set(REEA_T_REGISTRO.SOGGETTO_ID_CIFRATO,
    						DSL.function("pgp_sym_encrypt_bytea",
    								byte[].class,
    								DSL.val(soggettoId.toString()).cast(SQLDataType.BLOB),
    								DSL.val("16<odcc8!")))
    				.set(REEA_T_REGISTRO.SEZIONE, registro.getSezione())
    				.set(REEA_T_REGISTRO.ATT_SANITARIA_STATO, registro.getAttSanitariaStato())
    				.set(REEA_T_REGISTRO.ATT_SANITARIA_INAIL, dtos.getAttSantariaInail())
    				.set(REEA_T_REGISTRO.VERSIONE_NUMERO, 1)
    				.set(REEA_T_REGISTRO.VALIDITA_INIZIO, DSL.currentLocalDateTime())
    				.set(REEA_T_REGISTRO.UTENTE_CREAZIONE, dtos.getUtenteCreazione())
    				.set(REEA_T_REGISTRO.TIPO_ELENCO_INAIL, registro.getTipoElencoInail())
    				.returning(REEA_T_REGISTRO.REGISTRO_ID)
    				.fetchOne()
    				.getValue(REEA_T_REGISTRO.REGISTRO_ID, Integer.class);

    		tracciaElaborazioneRepository.inserisciFileImpatto(
    				ctx,
    				elaborazioneId,
    				"REEA_T_REGISTRO",
    				dtos.getRegInailId(),
    				tipoOperazione,
    				utente
    				);

    		return ris;
    	} catch (Exception e) {

    		String risultato = e.getMessage(); // o una descrizione più parlante

    		try {
    			tracciaElaborazioneService.inserisciErroreRiga(
//    					ctx,
    					elaborazioneId,
    					"ERR_INSERIMENTO",
    					"inserisciRegistroInail ha restituito " + risultato + " per CF " + dtos.getCodiceFiscale(),
    					null,
    					dtos.getUtenteCreazione(),
    					dtos.getCognome(),
    					dtos.getNome(),
    					dtos.getNascitaData(),
    					dtos.getSesso(),
    					dtos.getCodiceFiscale(),
    					"REEA_T_REGISTRO",
    					dtos.getRegInailId()
    					);
    		} catch (Exception exLog) {
    			System.err.println("Errore nel log scarto: " + exLog.getMessage());
    		}

    		// Se il metodo fa parte di una transazione e vuoi rollback, rilancia
    		throw e;
    		// In alternativa, se NON vuoi far propagare l’errore:
    		// return null;
    	}
    }


    @Override
    public Integer updateRegistro(DSLContext ctx, Integer soggettoId, AnagraficaDTO dtos, Integer elaborazioneId, String utente, Integer tipoOperazione) {

    	RegistroDTO registro = RegistroUtils.ensureRegistro(dtos);
    	SpresalDTO spresal = SpresalUtils.ensureSpresal(dtos);

    	try {
    		Integer ris = ctx.update(REEA_T_REGISTRO)
    				.set(REEA_T_REGISTRO.SEZIONE, registro.getSezione())
    				.set(REEA_T_REGISTRO.ATT_SANITARIA_STATO,
    						DSL.coalesce(DSL.val(registro.getAttSanitariaStato()), REEA_T_REGISTRO.ATT_SANITARIA_STATO))
    				.set(REEA_T_REGISTRO.ATT_SANITARIA_INAIL,
    						DSL.coalesce(DSL.val(dtos.getAttSantariaInail()), REEA_T_REGISTRO.ATT_SANITARIA_INAIL))

    				.set(REEA_T_REGISTRO.VERSIONE_NUMERO, registro.getVersioneNumero())
//    				.set(REEA_T_REGISTRO.VERSIONE_NUMERO,(Integer) null)
    				.set(REEA_T_REGISTRO.VALIDITA_INIZIO, DSL.currentLocalDateTime())
    				.set(REEA_T_REGISTRO.UTENTE_CREAZIONE, dtos.getUtenteCreazione())
    				.set(REEA_T_REGISTRO.TIPO_ELENCO_INAIL,
    						DSL.coalesce(DSL.val(registro.getTipoElencoInail()), REEA_T_REGISTRO.TIPO_ELENCO_INAIL))

    				.set(REEA_T_REGISTRO.DATA_MODIFICA, DSL.currentLocalDateTime())
    				// Spresal
    				.set(REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL,
    						DSL.coalesce(DSL.val(registro.getAttSanitariaSpresal()), REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL))
    				.set(REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA,
    						DSL.coalesce(DSL.val(registro.getAttSanitariaSpresalData()), REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA))
    				.set(REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA,
    						DSL.coalesce(DSL.val(spresal.getInserimentoInSorveglianza()), REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA))

    				.set(REEA_T_REGISTRO.UTENTE_MODIFICA, utente)
    				//
    				.where(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
    						DSL.field("reea.hmac_soggetto_id({0}, {1})",
    								String.class,
    								DSL.val(soggettoId),
    								DSL.val("16<odcc8!"))))
    				.returning(REEA_T_REGISTRO.REGISTRO_ID)
    				.fetchOne()
    				.getValue(REEA_T_REGISTRO.REGISTRO_ID, Integer.class);

    		tracciaElaborazioneRepository.inserisciFileImpatto(
    				ctx,
    				elaborazioneId,
    				"REEA_T_REGISTRO",
    				dtos.getRegInailId() != null ? dtos.getRegInailId().intValue() : dtos.getSpresal().getRegSpresalAnamnesiId().intValue(),
    						tipoOperazione,
    						utente
    				);

    		return ris;
    	} catch (Exception e) {

    		String risultato = e.getMessage(); // o una descrizione più parlante

    		try {
    			tracciaElaborazioneService.inserisciErroreRiga(
//    					ctx,
    					elaborazioneId,
    					"ERR_INSERIMENTO",
    					"updateRegistro ha restituito " + risultato + " per CF " + dtos.getCodiceFiscale(),
    					null,
    					dtos.getUtenteCreazione(),
    					dtos.getCognome(),
    					dtos.getNome(),
    					dtos.getNascitaData(),
    					dtos.getSesso(),
    					dtos.getCodiceFiscale(),
    					"REEA_T_REGISTRO",
    					dtos.getRegInailId() != null ? dtos.getRegInailId().intValue() : dtos.getSpresal().getRegSpresalAnamnesiId().intValue()
    					);
    		} catch (Exception exLog) {
    			System.err.println("Errore nel log scarto: " + exLog.getMessage());
    		}

    		// Se il metodo fa parte di una transazione e vuoi rollback, rilancia
    		throw e;
    		// In alternativa, se NON vuoi far propagare l’errore:
    		// return null;
    	}
    }


    @Override
    public Integer inserisciEsposizioni(DSLContext ctx, Integer soggettoId, AnagraficaDTO dtos, Integer elaborazioneId, String utente, Integer tipoOperazione) {

    	Integer risultato = 0;

    	if (dtos.getListaEsposizione() == null) {
    		return risultato;
    	}
    	try {
    		for (EsposizioneDTO esposizione : dtos.getListaEsposizione()) {
    			System.out.println("STO ANDANDO AD INSERIRE  COME ESPOSIZIONE AZIENDA IL VALORE:"+esposizione.getEsposizioneAzienda());
    			risultato = ctx.insertInto(REEA_T_ESPOSIZIONE)
    					.set(REEA_T_ESPOSIZIONE.SOGGETTO_ID_HMAC,
    							DSL.field("reea.hmac_soggetto_id({0}, {1})",
    									String.class,
    									DSL.val(soggettoId),
    									DSL.val("16<odcc8!")))
    					.set(REEA_T_ESPOSIZIONE.SOGGETTO_ID_CIFRATO,
    							DSL.function("pgp_sym_encrypt_bytea",
    									byte[].class,
    									DSL.val(soggettoId.toString()).cast(SQLDataType.BLOB),
    									DSL.val("16<odcc8!")))
    					.set(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA, esposizione.getEsposizioneAzienda())
    					.set(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD, esposizione.getEsposizioneAziendaComuneCod())
    					.set(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC, esposizione.getEsposizioneAziendaComuneDesc())
    					.set(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP, esposizione.getEsposizioneAziendaCap())
    					.set(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA, esposizione.getEsposizioneAziendaProvincia())
    					.set(REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE,
    							esposizione.getEsposizioneMansione() != null
    							? esposizione.getEsposizioneMansione() : "N/D")
    					.set(REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO,
    							esposizione.getEsposizioneInizio() != null
    							? LocalDate.of(esposizione.getEsposizioneInizio().getYear(), 1, 1).atStartOfDay()
    									: LocalDateTime.now())

    					.set(REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE,
    							esposizione.getEsposizioneFine() != null
    							? LocalDate.of(esposizione.getEsposizioneFine().getYear(), 1, 1).atStartOfDay()
    									: null)
    					.set(REEA_T_ESPOSIZIONE.FONTE_ID, dtos.getFonteId())
    					.set(REEA_T_ESPOSIZIONE.UTENTE_CREAZIONE, dtos.getUtenteCreazione())
    					.set(REEA_T_ESPOSIZIONE.UTENTE_MODIFICA, dtos.getUtenteCreazione())
    					.returning(REEA_T_ESPOSIZIONE.ESPOSIZIONE_ID)
    					.fetchOne()
    					.getValue(REEA_T_ESPOSIZIONE.ESPOSIZIONE_ID, Integer.class);
    		}

    		if(dtos.getAdesioneId() != null)
    			tracciaElaborazioneRepository.inserisciFileImpatto(
    					ctx,
    					elaborazioneId,
    					"REEA_T_ESPOSIZIONE",
    					dtos.getAdesioneId().intValue(),
    					tipoOperazione,
    					utente
    					);

    		return risultato;

    	} catch (Exception e) {

    		try {
    			tracciaElaborazioneService.inserisciErroreRiga(
//    					ctx,
    					elaborazioneId,
    					"ERR_INSERIMENTO",
    					"inserisciEsposizioni ha restituito " + risultato + " per CF " + dtos.getCodiceFiscale(),
    					null,
    					dtos.getUtenteCreazione(),
    					dtos.getCognome(),
    					dtos.getNome(),
    					dtos.getNascitaData(),
    					dtos.getSesso(),
    					dtos.getCodiceFiscale(),
    					"REEA_T_ESPOSIZIONE",
    					dtos.getAdesioneId().intValue()
    					);
    		} catch (Exception exLog) {
    			System.err.println("Errore nel log scarto: " + exLog.getMessage());
    		}

    		// Se il metodo fa parte di una transazione e vuoi rollback, rilancia
    		throw e;
    		// In alternativa, se NON vuoi far propagare l’errore:
    		// return null;
    	}
    }
    
    
//    @Override
//    public AnagraficaDTO findByIdCampiAnonimizzati(Long soggettoId) {
//
//        var ASL_DOM = REEA_D_ASL.as("asl_dom");
//        var ASL_RES = REEA_D_ASL.as("asl_res");
//        var ASL_ASS = REEA_D_ASL.as("asl_ass");
//
//        return dsl.select(
//                        // ASL soggetto
//                        ASL_DOM.ASL_AZIENDA_DESC.as("domicilio_asl_desc_soggetto"),
//                        ASL_RES.ASL_AZIENDA_DESC.as("residenza_asl_desc_soggetto"),
//                        ASL_ASS.ASL_AZIENDA_DESC.as("assistenza_asl_desc"),
//
//                        // SOGGETTO / FONTE
//                        REEA_T_SOGGETTO.FONTE_ID,
//                        REEA_D_FONTE.FONTE_DESC,
//                        REEA_T_SOGGETTO.DATA_MODIFICA,
//                        REEA_T_SOGGETTO.DATA_CREAZIONE,
//                        REEA_T_SOGGETTO.NASCITA_DATA,
//                        REEA_T_SOGGETTO.SOGGETTO_ID,
//                        REEA_T_SOGGETTO.VERSIONE_NUMERO,
//                        REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO,
//                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD,
//                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD,
//                        REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO,
//                        REEA_T_SOGGETTO.DOMICILIO_STATO_DESC,
//                        DSL.when(REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.NOME_CIFRATO, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("nome"),
//                        DSL.when(REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.COGNOME_CIFRATO, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("cognome"),
//                        REEA_T_SOGGETTO.SESSO,
//                        REEA_T_SOGGETTO.NASCITA_COMUNE_COD.as("nascita_comune_cod"),
//                        REEA_T_SOGGETTO.NASCITA_STATO_COD.as("nascita_stato_cod"),
//                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_COD.as("nascita_provincia_cod"),
//                        REEA_T_SOGGETTO.CODICE_FISCALE,
//                        REEA_T_SOGGETTO.CITTADINANZA_STATO_COD.as("cittadinanza_stato_cod"),
//                        REEA_T_SOGGETTO.CITTADINANZA_STATO_DESC.as("cittadinanza_stato_desc"),
//                        REEA_T_SOGGETTO.DOMICILIO_ASL_ID,
//                        REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_COD.as("domicilio_provincia_cod"),
//                        REEA_T_SOGGETTO.DOMICILIO_STATO_COD.as("domicilio_stato_cod"),
//                        REEA_T_SOGGETTO.RESIDENZA_STATO_COD,
//                        REEA_T_SOGGETTO.RESIDENZA_ASL_ID,
//                        REEA_T_SOGGETTO.SOGGETTO_STATO_ID,
//                        REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE,
//                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC,
//                        REEA_T_REGISTRO.SEZIONE,
//                        REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA,
//                        REEA_T_REGISTRO.TIPO_ELENCO_INAIL,
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID,
//                        REEA_T_REGISTRO.REGISTRO_ID,
//                        REEA_T_REGISTRO.ATT_SANITARIA_STATO,
//                        REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL,
//                        REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA,
//                        REEA_T_REGISTRO.ATT_SANITARIA_INAIL,
//                        REEA_T_ADESIONE.ADESIONE_DATA,
//                        REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA,
//                        REEA_T_SOGGETTO.NASCITA_STATO_DESC,
//                        REEA_T_SOGGETTO.RESIDENZA_CAP,
//                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC,
//                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC,
//                        REEA_T_SOGGETTO.RESIDENZA_STATO_DESC,
//                        DSL.when(REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("telefono_aura"),
//                        DSL.when(REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("email_aura"),
//
//                        DSL.when(REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_ADESIONE.TELEFONO_CIFRATO, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("pre_tel_dec"),
//                        DSL.when(REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_ADESIONE.EMAIL_CIFRATA, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("pre_email_dec"),
//                        DSL.when(REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_ADESIONE.EMAIL_CIFRATA, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("preadesione_email"),
//                        DSL.when(REEA_T_SOGGETTO.TELEFONO_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.TELEFONO_CIFRATO, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("telefono_soggetto"),
//                        DSL.when(REEA_T_SOGGETTO.EMAIL_CIFRATA.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.EMAIL_CIFRATA, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("email_soggetto"),
//
//                        REEA_T_ADESIONE.ADESIONE_COD,
//
//                        // nascita descrizioni con alias
//                        REEA_T_SOGGETTO.NASCITA_COMUNE_DESC.as("nascita_comune_desc"),
//                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_DESC.as("nascita_provincia_desc"),
//
//                        // id_aura: coalesce soggetto/adesione
//                        DSL.coalesce(
//                                DSL.when(REEA_T_SOGGETTO.ID_AURA_CIFRATO.isNotNull(),
//                                        DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                                REEA_T_SOGGETTO.ID_AURA_CIFRATO, DSL.val("16<odcc8!"))
//                                ).otherwise((String) null),
//                                DSL.when(REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
//                                        DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                                REEA_T_ADESIONE.ID_AURA_CIFRATO, DSL.val("16<odcc8!"))
//                                ).otherwise((String) null)
//                        ).as("id_aura"),
//
//                        // tessera_team: coalesce soggetto/adesione
//                        DSL.coalesce(
//                                DSL.when(REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA.isNotNull(),
//                                        DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                                REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA, DSL.val("16<odcc8!"))
//                                ).otherwise((String) null),
//                                DSL.when(REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
//                                        DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                                REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA, DSL.val("16<odcc8!"))
//                                ).otherwise((String) null)
//                        ).as("tessera_team"),
//
//                        // domicilio da soggetto / adesione
//                        DSL.coalesce(REEA_T_SOGGETTO.DOMICILIO_CAP, REEA_T_ADESIONE.DOMICILIO_CAP).as("domicilio_cap"),
//                        DSL.coalesce(
//                                REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_DESC,
//                                REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC
//                        ).as("domicilio_provincia_desc"),
//                        DSL.coalesce(
//                                REEA_T_SOGGETTO.DOMICILIO_COMUNE_DESC,
//                                REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC
//                        ).as("domicilio_comune_desc"),
//                        DSL.coalesce(
//                                REEA_T_SOGGETTO.DOMICILIO_COMUNE_COD,
//                                REEA_T_ADESIONE.DOMICILIO_COMUNE_COD
//                        ).as("domicilio_comune_cod"),
//
//                        // ASL da adesione
//                        REEA_T_ADESIONE.DOMICILIO_ASL_COD,
//                        REEA_T_ADESIONE.DOMICILIO_ASL_DESC,
//                        REEA_T_ADESIONE.RESIDENZA_ASL_COD,
//                        REEA_T_ADESIONE.RESIDENZA_ASL_DESC,
//
//                        // dati anagrafici adesione
//                        DSL.when(REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_ADESIONE.COGNOME_CIFRATO, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("adesione_cognome"),
//                        DSL.when(REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_ADESIONE.NOME_CIFRATO, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("adesione_nome"),
//                        REEA_T_ADESIONE.NASCITA_DATA.as("adesione_nascita_data"),
//
//                        // indirizzi cifrati decifrati
//                        DSL.when(REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("residenza_indirizzo"),
//                        DSL.when(REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO, DSL.val("16<odcc8!"))
//                        ).otherwise((String) null).as("domicilio_indirizzo"),
//
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE,
//                        REEA_T_SOGGETTO.DATA_DECESSO
//                )
//                .from(REEA_T_SOGGETTO)
//                .join(REEA_D_FONTE)
//                    .on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//                .leftJoin(ASL_ASS)
//                    .on(ASL_ASS.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//                .leftJoin(ASL_DOM)
//                    .on(ASL_DOM.ASL_ID.eq(REEA_T_SOGGETTO.DOMICILIO_ASL_ID))
//                .leftJoin(ASL_RES)
//                    .on(ASL_RES.ASL_ID.eq(REEA_T_SOGGETTO.RESIDENZA_ASL_ID))
//                .join(REEA_T_REGISTRO)
//                    .on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//                            DSL.field("reea.hmac_soggetto_id({0}, {1})",
//                                    String.class,
//                                    REEA_T_SOGGETTO.SOGGETTO_ID,
//                                    DSL.val("16<odcc8!"))))
//                .join(REEA_D_SOGGETTO_STATO)
//                    .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//                .leftJoin(REEA_T_ADESIONE)
//                    .on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
//                .where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
//                .limit(1)
//                .fetchOne(record -> {
//                    if (record == null) {
//                        return null;
//                    }
//
//                    AnagraficaDTO result = new AnagraficaDTO();
//
//                    result.setSoggettoId(soggettoId);
//                    result.setCodiceFiscale(record.get(REEA_T_SOGGETTO.CODICE_FISCALE));
//                    result.setResidenzaAslId(record.get(REEA_T_SOGGETTO.RESIDENZA_ASL_ID));
//                    result.setDomicilioAslId(record.get(REEA_T_SOGGETTO.DOMICILIO_ASL_ID));
//
//                    result.setDescrizioneAslCompetenza(record.get("assistenza_asl_desc", String.class));
//                    result.setNascitaStatoDesc(record.get(REEA_T_SOGGETTO.NASCITA_STATO_DESC));
//                    result.setNome(record.get("nome", String.class));
//                    result.setCognome(record.get("cognome", String.class));
//                    result.setTelefonoAura(record.get("telefono_aura", String.class));
//                    result.setEmailAura(record.get("email_aura", String.class));
//
//                    result.setResidenzaNumeroCivico(record.get(REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO));
//                    result.setResidenzaComuneCod(record.get(REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD));
//                    result.setDomicilioNumeroCivico(record.get(REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO));
//                    result.setDomicilioStatoDesc(record.get(REEA_T_SOGGETTO.DOMICILIO_STATO_DESC));
//                    result.setSesso(record.get(REEA_T_SOGGETTO.SESSO));
//                    result.setNascitaData(record.get(REEA_T_SOGGETTO.NASCITA_DATA));
//                    result.setFonteId(record.get(REEA_T_SOGGETTO.FONTE_ID));
//
//                    result.setNascitaComuneCod(record.get("nascita_comune_cod", String.class));
//                    result.setNascitaComuneDesc(record.get("nascita_comune_desc", String.class));
//                    result.setNascitaProvinciaCod(record.get("nascita_provincia_cod", String.class));
//                    result.setNascitaProvinciaDesc(record.get("nascita_provincia_desc", String.class));
//                    result.setNascitaStatoCod(record.get("nascita_stato_cod", String.class));
//
//                    result.setResidenzaCap(record.get(REEA_T_SOGGETTO.RESIDENZA_CAP));
//                    result.setResidenzaComuneDesc(record.get(REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC));
//                    result.setResidenzaProvinciaCod(record.get(REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD));
//                    result.setResidenzaProvinciaDesc(record.get(REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC));
//                    result.setResidenzaStatoCod(record.get(REEA_T_SOGGETTO.RESIDENZA_STATO_COD));
//                    result.setResidenzaStatoDesc(record.get(REEA_T_SOGGETTO.RESIDENZA_STATO_DESC));
//
//                    result.setDomicilioCap(record.get("domicilio_cap", String.class));
//                    result.setDomicilioComuneDesc(record.get("domicilio_comune_desc", String.class));
//                    result.setDomicilioProvinciaCod(record.get("domicilio_provincia_cod", String.class));
//                    result.setDomicilioProvinciaDesc(record.get("domicilio_provincia_desc", String.class));
//                    result.setDomicilioComuneCod(record.get("domicilio_comune_cod", String.class));
//                    result.setDomicilioStatoCod(record.get("domicilio_stato_cod", String.class));
//
//                    result.setCittadinanzaStatoCod(record.get("cittadinanza_stato_cod", String.class));
//                    result.setCittadinanzaStatoDesc(record.get("cittadinanza_stato_desc", String.class));
//
//                    result.setResidenzaIndirizzo(record.get("residenza_indirizzo", String.class));
//                    result.setDomicilioIndirizzo(record.get("domicilio_indirizzo", String.class));
//
//                    LocalDateTime assistenzaFine = record.get(REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE);
//                    result.setAssistenzaAslFine(
//                            assistenzaFine != null ? assistenzaFine.toLocalDate().toString() : null);
//
//                    LocalDateTime dataDecesso = record.get(REEA_T_SOGGETTO.DATA_DECESSO);
//                    result.setDataDecesso(
//                            dataDecesso != null ? dataDecesso.toLocalDate().toString() : null);
//
//                    RegistroDTO registro = RegistroUtils.ensureRegistro(result);
//
//                    registro.setDataCreazione(
//                            DateConversionUtils.toOffsetDateTime(record.get(REEA_T_SOGGETTO.DATA_CREAZIONE))
//                    );
//
//                    LocalDateTime dataModifica = record.get(REEA_T_SOGGETTO.DATA_MODIFICA);
//                    if (dataModifica != null) {
//                        registro.setDataModifica(
//                                DateConversionUtils.toOffsetDateTime(dataModifica)
//                        );
//                    }
//
//                    result.setDescrizioneStato(
//                            record.get(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC)
//                    );
//                    Integer assistenzaId = record.get(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID);
//                    result.setAssistenzaAslId(
//                            assistenzaId != null ? String.format("%06d", assistenzaId) : null
//                    );
//
//                    result.setDescrizioneFonte(
//                            List.of(record.get(REEA_D_FONTE.FONTE_DESC) != null
//                                    ? record.get(REEA_D_FONTE.FONTE_DESC)
//                                    : "PROVA")
//                    );
//                    result.setTelefono(record.get("telefono_soggetto", String.class));
//                    result.setEmail(record.get("email_soggetto", String.class));
//
//                    LocalDateTime adesioneDataVal = record.get(REEA_T_ADESIONE.ADESIONE_DATA);
//                    result.setAdesioneData(adesioneDataVal != null ? adesioneDataVal.toString() : null);
//       )             result.setPreadesioneTelefono(record.get("pre_tel_dec", String.class));
//                    result.setPreadesioneEmail(record.get("pre_email_dec", String.class));
//                    result.setAdesioneCod(record.get(REEA_T_ADESIONE.ADESIONE_COD));
//                    result.setTesseraTeam(record.get("tessera_team", String.class));
//                    result.setIdAura(record.get("id_aura", String.class));
//
//                    result.setDomicilioAslCod(record.get(REEA_T_ADESIONE.DOMICILIO_ASL_COD));
//                    result.setDomicilioAslDesc(record.get("domicilio_asl_desc_soggetto", String.class));
//                    result.setResidenzaAslDesc(record.get("residenza_asl_desc_soggetto", String.class));
//
//                    LocalDateTime presentazioneIstanzaData =
//                            record.get(REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA);
//                    result.setPresentazioneIstanzaData(
//                            presentazioneIstanzaData != null ? presentazioneIstanzaData.toString() : null
//                    );
//
//                    registro.setVersioneNumero(record.get(REEA_T_SOGGETTO.VERSIONE_NUMERO));
//                    registro.setSezione(record.get(REEA_T_REGISTRO.SEZIONE));
//                    registro.setInseritoInSorveglianza(
//                            record.get(REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA)
//                    );
//                    registro.setTipoElencoInail(record.get(REEA_T_REGISTRO.TIPO_ELENCO_INAIL));
//
//                    result.setSoggettoStatoId(record.get(REEA_T_SOGGETTO.SOGGETTO_STATO_ID));
//                    result.setSoggettoStatoNote(record.get(REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE));
//
//                    result.setListaFonteByIdSoggetto(
//                            fonteRepository.findListaFonteDescByIdSoggetto(soggettoId)
//                    );
//                    registro.setAttSanitariaStato(record.get(REEA_T_REGISTRO.ATT_SANITARIA_STATO));
//                    registro.setAttSanitariaSpresal(record.get(REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL));
//                    registro.setAttSanitariaSpresalData(record.get(REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA));
//                    registro.setAttSanitariaInail(record.get(REEA_T_REGISTRO.ATT_SANITARIA_INAIL));
//
//                    Integer registroId = record.get(REEA_T_REGISTRO.REGISTRO_ID);
//                    registro.setRegistroId(registroId);
//
//                    // Esposizioni (invariato)
//                    // ...
//                    // INAIL, NPLA, Spresal, Esenzioni (come nel tuo codice originale)
//                    // ...
//
//                    // Esposizioni
//    				result.setListaEsposizione(
//    						dsl.select(
//    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA,
//    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE,
//    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO,
//    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE,
//									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD,
//									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC,
//									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP,
//									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA
//    								)
//    						.from(REEA_T_ESPOSIZIONE)
//    						.where(REEA_T_ESPOSIZIONE.SOGGETTO_ID_HMAC.eq(
//    								DSL.field("reea.hmac_soggetto_id({0}, {1})", String.class,
//    										DSL.val(soggettoId.intValue()), DSL.val("16<odcc8!"))))
//    						.fetch(r -> {
//    							EsposizioneDTO e = new EsposizioneDTO();
//    							e.setEsposizioneAzienda(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA));
//    							e.setEsposizioneMansione(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE));
//    							LocalDateTime ini = r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO);
//    							e.setEsposizioneInizio(ini != null ? ini.toLocalDate() : null);
//    							LocalDateTime fin = r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE);
//    							e.setEsposizioneFine(fin != null ? fin.toLocalDate() : null);
//								e.setEsposizioneAziendaComuneCod(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD));
//								e.setEsposizioneAziendaComuneDesc(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC));
//								e.setEsposizioneAziendaCap(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP));
//								e.setEsposizioneAziendaProvincia(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA));
//
//								return e;
//    						})
//    						);
//
//    				// INAIL e NPLA
//    				if (registroId != null) {
//    					result.setListaInail(
//    							dsl.selectFrom(REEA_T_REGISTRO_INAIL)
//    							.where(REEA_T_REGISTRO_INAIL.REGISTRO_ID.eq(registroId))
//    							.fetch(r -> {
//    								InailDTO dto = new InailDTO();
//    								dto.setRegistroId(r.get(REEA_T_REGISTRO_INAIL.REGISTRO_ID));
//    								dto.setCognome(r.get(REEA_T_REGISTRO_INAIL.COGNOME));
//    								dto.setNome(r.get(REEA_T_REGISTRO_INAIL.NOME));
//    								dto.setCodiceFiscale(r.get(REEA_T_REGISTRO_INAIL.CODICE_FISCALE));
//    								dto.setTipologiaInail(r.get(REEA_T_REGISTRO_INAIL.TIPO_ELENCO_INAIL));
//    								dto.setDomanda(r.get(REEA_T_REGISTRO_INAIL.DOMANDA));
//                                    dto.setSesso(r.get(REEA_T_REGISTRO_INAIL.SESSO));
//                                    dto.setDataNascita(r.get(REEA_T_REGISTRO_INAIL.DATA_NASCITA) != null
//                                            ? LocalDate.parse(r.get(REEA_T_REGISTRO_INAIL.DATA_NASCITA).toString()) : null);
//                                    dto.setIndirizzoResidenza(r.get(REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA));
//                                    dto.setIstatResidenza(r.get(REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA));
//                                    dto.setCapResidenza(r.get(REEA_T_REGISTRO_INAIL.CAP_RESIDENZA));
//                                    dto.setRegioneResidenza(r.get(REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA));
//                                    dto.setProvinciaResidenza(r.get(REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA));
//                                    dto.setComuneResidenza(r.get(REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA));
//
//
//
//                                    return dto;
//    							})
//    							);
//
//    					result.setListaNpla(
//    							dsl.selectFrom(REEA_T_REGISTRO_PDL_AMIANTO)
//    							.where(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.eq(registroId))
//    							.fetch(r -> {
//    								NplaDTO dto = new NplaDTO();
//    								dto.setAziendaNome(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME));
//    								dto.setAziendaPiva(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA));
//    								dto.setAslCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE));
//    								dto.setComuneCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE));
//    								dto.setAnno(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ANNO));
//    								dto.setPeriodo(r.get(REEA_T_REGISTRO_PDL_AMIANTO.PERIODO));
//    								dto.setTipologiaPiano(r.get(REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO));
//    								dto.setQuantitaDaRimuovere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE));
//    								dto.setQuantitaRimossa(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA));
//									dto.setCodiceFiscale(r.get(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE));
//									dto.setIdCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE));
//    								return dto;
//    							})
//    							);
//
//    					
//                        List<SpresalDTO> listaAnamnesiSpresal = dsl
//                                .selectFrom(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
//                                .where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.eq(registroId))
//                                .fetch(r -> {
//                                    SpresalDTO dto = new SpresalDTO();
//
//                                    // Helper solo per campi BigDecimal (sigarette/sigari/pipa anni, etÃ , die)
//                                    java.util.function.Function<Number, BigDecimal> toBD =
//                                            n -> n != null ? BigDecimal.valueOf(n.longValue()) : null;
//
//                                    // Long diretto
//                                    dto.setRegSpresalAnamnesiId(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID) != null
//                                            ? r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID).longValue() : null);
//
//
//                                    // LocalDate diretto (non String)
//                                    dto.setDataIntervista(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA));
//
//                                    dto.setNominativoIntervistatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE));
//
//                                    // === FUMATORE ===
//                                    dto.setFumatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE));
//
//                                    // === SIGARETTE ===
//                                    dto.setSigarette(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE));
//                                    dto.setSigaretteAnni(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI)));
//                                    dto.setSigaretteEtaInizio(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO)));
//                                    dto.setSigaretteFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE));
//                                    dto.setSigaretteEtaFine(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE)));
//                                    dto.setSigaretteDie(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE)));
//
//                                    // === SIGARI ===
//                                    dto.setSigari(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI));
//                                    dto.setSigariAnni(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI)));
//                                    dto.setSigariEtaInizio(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO)));
//                                    dto.setSigariFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE));
//                                    dto.setSigariEtaFine(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE)));
//                                    dto.setSigariDie(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE)));
//
//                                    // === PIPA ===
//                                    dto.setPipa(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA));
//                                    dto.setPipaAnni(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI)));
//                                    dto.setPipaEtaInizio(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO)));
//                                    dto.setPipaFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE));
//                                    dto.setPipaEtaFine(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE)));
//                                    dto.setPipaDie(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE)));
//
//                                    // === OCCUPAZIONE ===
//                                    dto.setOccupazioneNum(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM));
//                                    // Integer diretto
//                                    dto.setOccupazioneAnnoInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO));
//                                    dto.setOccupazioneAnnoFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE));
//                                    dto.setOccupazioneTipo(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO));
//                                    dto.setOccupazioneDescrizioneLavoro(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO));
//                                    dto.setOccupazioneNomeEIndirizzoDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA));
//                                    dto.setOccupazioneAttivitaDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA));
//                                    dto.setNotaAttivitaConAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO));
//
//                                    // === ESPOSIZIONE AMIANTO ===
//                                    dto.setAnamnesiEsposizioneAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO));
//                                    dto.setEsposizioneProfessionale(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE));
//                                    // Integer diretto
//                                    dto.setAnnoFineEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE));
//                                    dto.setLivelloEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE));
//                                    dto.setInserimentoInSorveglianza(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA));
//
//                                    // === CRPT ===
//                                    dto.setOccupazioneEsposizioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT));
//                                    dto.setOccupazioneSettoreDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT));
//                                    dto.setOccupazioneMansioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT));
//                                    dto.setOccupazioneRagioneSocialeDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT));
//                                    dto.setOccupazionePivaDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT));
//                                    dto.setOccupazioneCodiceFiscaleDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_CODICE_FISCALE_DITTA_CRPT));
//
//                                    return dto;
//                                });
//
//                        result.setListaAnamnesiSpresal(listaAnamnesiSpresal);
//
//                        result.setListaEsenzione(
//                                esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId)
//                        );
//                    }
//
//    				return result;
//    			});
//    }
    
    
    @Override
    public AnagraficaDTO findById(Long soggettoId) {

        var ASL_DOM = REEA_D_ASL.as("asl_dom");
        var ASL_RES = REEA_D_ASL.as("asl_res");
        var ASL_ASS = REEA_D_ASL.as("asl_ass");

        return dsl.select(
                        // ASL soggetto
                        ASL_DOM.ASL_AZIENDA_DESC.as("domicilio_asl_desc_soggetto"),
                        ASL_RES.ASL_AZIENDA_DESC.as("residenza_asl_desc_soggetto"),
                        ASL_ASS.ASL_AZIENDA_DESC.as("assistenza_asl_desc"),

                        // SOGGETTO / FONTE
                        REEA_T_SOGGETTO.FONTE_ID,
                        REEA_D_FONTE.FONTE_DESC,
                        REEA_T_SOGGETTO.DATA_MODIFICA,
                        REEA_T_SOGGETTO.DATA_CREAZIONE,
                        REEA_T_SOGGETTO.NASCITA_DATA,
                        REEA_T_SOGGETTO.SOGGETTO_ID,
                        REEA_T_SOGGETTO.VERSIONE_NUMERO,
                        REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO,
                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD,
                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD,
                        REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO,
                        REEA_T_SOGGETTO.INSERIMENTO_TIPO_ID,
                        DSL.when(REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_SOGGETTO.NOME_CIFRATO, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("nome"),
                        DSL.when(REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_SOGGETTO.COGNOME_CIFRATO, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("cognome"),
                        REEA_T_SOGGETTO.SESSO,
                        REEA_T_SOGGETTO.NASCITA_COMUNE_COD.as("nascita_comune_cod"),
                        REEA_T_SOGGETTO.NASCITA_STATO_COD.as("nascita_stato_cod"),
                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_COD.as("nascita_provincia_cod"),
                        REEA_T_SOGGETTO.CODICE_FISCALE,
                        REEA_T_SOGGETTO.CITTADINANZA_STATO_COD.as("cittadinanza_stato_cod"),
                        REEA_T_SOGGETTO.CITTADINANZA_STATO_DESC.as("cittadinanza_stato_desc"),
                        REEA_T_SOGGETTO.DOMICILIO_ASL_ID,
                        REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_COD.as("domicilio_provincia_cod"),
                        REEA_T_SOGGETTO.DOMICILIO_STATO_COD.as("domicilio_stato_cod"),
                        REEA_T_SOGGETTO.RESIDENZA_STATO_COD,
                        dsl.select(REEA_D_NAZIONE.NAZIONE_DESC_IT)
                                .from(REEA_D_NAZIONE)
                                .where(REEA_D_NAZIONE.NAZIONE_ISTAT_COD.eq(REEA_T_SOGGETTO.RESIDENZA_STATO_COD))
                                .asField("residenza_stato_desc"),
                        dsl.select(REEA_D_NAZIONE.NAZIONE_DESC_IT)
                                .from(REEA_D_NAZIONE)
                                .where(REEA_D_NAZIONE.NAZIONE_ISTAT_COD.eq(REEA_T_SOGGETTO.DOMICILIO_STATO_COD))
                                .asField("domicilio_stato_desc"),
                        REEA_T_SOGGETTO.RESIDENZA_ASL_ID,
                        REEA_T_SOGGETTO.SOGGETTO_STATO_ID,
                        REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE,
                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC,
                        REEA_T_REGISTRO.SEZIONE,
                        REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA,
                        REEA_T_REGISTRO.TIPO_ELENCO_INAIL,
                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID,
                        REEA_T_REGISTRO.REGISTRO_ID,
                        REEA_T_REGISTRO.ATT_SANITARIA_STATO,
                        REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL,
                        REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA,
                        REEA_T_REGISTRO.ATT_SANITARIA_INAIL,
                        REEA_T_ADESIONE.ADESIONE_DATA,
                        REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA,
                        REEA_T_SOGGETTO.NASCITA_STATO_DESC,
                        REEA_T_SOGGETTO.RESIDENZA_CAP,
                        DSL.coalesce(
                                REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC,
                                dsl.select(REEA_D_COMUNE.COMUNE_DESC)
                                        .from(REEA_D_COMUNE)
                                        .where(REEA_D_COMUNE.COMUNE_COD.eq(REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD))
                                        .<String>asField()
                        ).as("residenza_comune_desc"),
                        DSL.coalesce(
                                REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC,
                                dsl.select(REEA_D_PROVINCIA.PROVINCIA_DESC)
                                        .from(REEA_D_PROVINCIA)
                                        .where(REEA_D_PROVINCIA.PROVINCIA_COD.eq(REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD))
                                        .<String>asField()
                        ).as("residenza_provincia_desc"),
                        DSL.when(REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("telefono_aura"),
                        DSL.when(REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("email_aura"),

                        DSL.when(REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_ADESIONE.TELEFONO_CIFRATO, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("pre_tel_dec"),
                        DSL.when(REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_ADESIONE.EMAIL_CIFRATA, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("pre_email_dec"),
                        DSL.when(REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_ADESIONE.EMAIL_CIFRATA, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("preadesione_email"),
                        DSL.when(REEA_T_SOGGETTO.TELEFONO_CIFRATO.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_SOGGETTO.TELEFONO_CIFRATO, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("telefono_soggetto"),
                        DSL.when(REEA_T_SOGGETTO.EMAIL_CIFRATA.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_SOGGETTO.EMAIL_CIFRATA, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("email_soggetto"),

                        REEA_T_ADESIONE.ADESIONE_COD,
                        REEA_T_SOGGETTO.NASCITA_COMUNE_DESC.as("nascita_comune_desc"),
                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_DESC.as("nascita_provincia_desc"),

                        // id_aura coalescato
                        DSL.coalesce(
                                DSL.when(REEA_T_SOGGETTO.ID_AURA_CIFRATO.isNotNull(),
                                        DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                                REEA_T_SOGGETTO.ID_AURA_CIFRATO, DSL.val("16<odcc8!"))
                                ).otherwise((String) null),
                                DSL.when(REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
                                        DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                                REEA_T_ADESIONE.ID_AURA_CIFRATO, DSL.val("16<odcc8!"))
                                ).otherwise((String) null)
                        ).as("id_aura"),

                        // tessera_team coalescata
                        DSL.coalesce(
                                DSL.when(REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA.isNotNull(),
                                        DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                                REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA, DSL.val("16<odcc8!"))
                                ).otherwise((String) null),
                                DSL.when(REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
                                        DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                                REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA, DSL.val("16<odcc8!"))
                                ).otherwise((String) null)
                        ).as("tessera_team"),

                        // domicilio da soggetto / adesione
                        DSL.coalesce(
                                REEA_T_SOGGETTO.DOMICILIO_CAP,
                                REEA_T_ADESIONE.DOMICILIO_CAP
                        ).as("domicilio_cap"),
                        DSL.coalesce(
                                REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_DESC,
                                REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC
                        ).as("domicilio_provincia_desc"),
                        DSL.coalesce(
                                REEA_T_SOGGETTO.DOMICILIO_COMUNE_DESC,
                                REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC
                        ).as("domicilio_comune_desc"),
                        DSL.coalesce(
                                REEA_T_SOGGETTO.DOMICILIO_COMUNE_COD,
                                REEA_T_ADESIONE.DOMICILIO_COMUNE_COD
                        ).as("domicilio_comune_cod"),

                        // ASL da adesione
                        REEA_T_ADESIONE.DOMICILIO_ASL_COD,
                        REEA_T_ADESIONE.DOMICILIO_ASL_DESC,
                        REEA_T_ADESIONE.RESIDENZA_ASL_COD,
                        REEA_T_ADESIONE.RESIDENZA_ASL_DESC,

                        // dati anagrafici adesione
                        DSL.when(REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_ADESIONE.COGNOME_CIFRATO, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("adesione_cognome"),
                        DSL.when(REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_ADESIONE.NOME_CIFRATO, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("adesione_nome"),
                        REEA_T_ADESIONE.NASCITA_DATA.as("adesione_nascita_data"),

                        // indirizzi cifrati decifrati
                        DSL.when(REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("residenza_indirizzo"),
                        DSL.when(REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO.isNotNull(),
                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                        REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO, DSL.val("16<odcc8!"))
                        ).otherwise((String) null).as("domicilio_indirizzo"),

                        REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE,
                        REEA_T_SOGGETTO.DATA_DECESSO,
                        REEA_T_SOGGETTO.UTENTE_MODIFICA,
                        REEA_T_SOGGETTO.UTENTE_CREAZIONE
                )
                .from(REEA_T_SOGGETTO)
                .join(REEA_D_FONTE)
                    .on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
                .leftJoin(ASL_ASS)
                    .on(ASL_ASS.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
                .leftJoin(ASL_DOM)
                    .on(ASL_DOM.ASL_ID.eq(REEA_T_SOGGETTO.DOMICILIO_ASL_ID))
                .leftJoin(ASL_RES)
                    .on(ASL_RES.ASL_ID.eq(REEA_T_SOGGETTO.RESIDENZA_ASL_ID))
                .join(REEA_T_REGISTRO)
                    .on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                            DSL.field("reea.hmac_soggetto_id({0}, {1})",
                                    String.class,
                                    REEA_T_SOGGETTO.SOGGETTO_ID,
                                    DSL.val("16<odcc8!"))))
                .join(REEA_D_SOGGETTO_STATO)
                    .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
                .leftJoin(REEA_T_ADESIONE)
                    .on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
                .where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
                .limit(1)
                .fetchOne(record -> {
                    if (record == null) {
                        return null;
                    }

                    AnagraficaDTO result = new AnagraficaDTO();

                    result.setSoggettoId(soggettoId);
                    result.setCodiceFiscale(record.get(REEA_T_SOGGETTO.CODICE_FISCALE));
                    result.setResidenzaAslId(record.get(REEA_T_SOGGETTO.RESIDENZA_ASL_ID));
                    result.setDomicilioAslId(record.get(REEA_T_SOGGETTO.DOMICILIO_ASL_ID));
                    result.setDescrizioneAslCompetenza(record.get("assistenza_asl_desc", String.class));
                    result.setNascitaStatoDesc(record.get(REEA_T_SOGGETTO.NASCITA_STATO_DESC));
                    result.setNome(record.get("nome", String.class));
                    result.setCognome(record.get("cognome", String.class));
                    result.setTelefonoAura(record.get("telefono_aura", String.class));
                    result.setEmailAura(record.get("email_aura", String.class));
                    result.setResidenzaNumeroCivico(record.get(REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO));
                    result.setResidenzaComuneCod(record.get(REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD));
                    result.setDomicilioNumeroCivico(record.get(REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO));
                    result.setDomicilioStatoDesc(record.get("domicilio_stato_desc", String.class));
                    result.setSesso(record.get(REEA_T_SOGGETTO.SESSO));
                    result.setNascitaData(record.get(REEA_T_SOGGETTO.NASCITA_DATA));
                    result.setFonteId(record.get(REEA_T_SOGGETTO.FONTE_ID));
                    result.setInserimentoTipoId(record.get(REEA_T_SOGGETTO.INSERIMENTO_TIPO_ID));

                    result.setNascitaComuneCod(record.get("nascita_comune_cod", String.class));
                    result.setNascitaComuneDesc(record.get("nascita_comune_desc", String.class));
                    result.setNascitaProvinciaCod(record.get("nascita_provincia_cod", String.class));
                    result.setNascitaProvinciaDesc(record.get("nascita_provincia_desc", String.class));
                    result.setNascitaStatoCod(record.get("nascita_stato_cod", String.class));

                    result.setResidenzaCap(record.get(REEA_T_SOGGETTO.RESIDENZA_CAP));
                    result.setResidenzaComuneDesc(record.get("residenza_comune_desc", String.class));
                    result.setResidenzaProvinciaCod(record.get(REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD));
                    result.setResidenzaProvinciaDesc(record.get("residenza_provincia_desc", String.class));
                    result.setResidenzaStatoCod(record.get(REEA_T_SOGGETTO.RESIDENZA_STATO_COD));
                    result.setResidenzaStatoDesc(record.get("residenza_stato_desc", String.class));

                    result.setDomicilioCap(record.get("domicilio_cap", String.class));
                    result.setDomicilioComuneDesc(record.get("domicilio_comune_desc", String.class));
                    result.setDomicilioProvinciaCod(record.get("domicilio_provincia_cod", String.class));
                    result.setDomicilioProvinciaDesc(record.get("domicilio_provincia_desc", String.class));
                    result.setDomicilioComuneCod(record.get("domicilio_comune_cod", String.class));
                    result.setDomicilioStatoCod(record.get("domicilio_stato_cod", String.class));

                    result.setCittadinanzaStatoCod(record.get("cittadinanza_stato_cod", String.class));
                    result.setCittadinanzaStatoDesc(record.get("cittadinanza_stato_desc", String.class));

                    result.setResidenzaIndirizzo(record.get("residenza_indirizzo", String.class));
                    result.setDomicilioIndirizzo(record.get("domicilio_indirizzo", String.class));

                    LocalDateTime assistenzaFine = record.get(REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE);
                    result.setAssistenzaAslFine(
                            assistenzaFine != null ? assistenzaFine.toLocalDate().toString() : null);

                    LocalDateTime dataDecesso = record.get(REEA_T_SOGGETTO.DATA_DECESSO);
                    result.setDataDecesso(
                            dataDecesso != null ? dataDecesso.toLocalDate().toString() : null);

                    RegistroDTO registro = RegistroUtils.ensureRegistro(result);

                    registro.setDataCreazione(
                            DateConversionUtils.toOffsetDateTime(record.get(REEA_T_SOGGETTO.DATA_CREAZIONE))
                    );

                    LocalDateTime dataModifica = record.get(REEA_T_SOGGETTO.DATA_MODIFICA);
                    if (dataModifica != null) {
                        registro.setDataModifica(
                                DateConversionUtils.toOffsetDateTime(dataModifica)
                        );
                    }

                    result.setDescrizioneStato(
                            record.get(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC)
                    );
                    Integer assistenzaId = record.get(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID);
                    result.setAssistenzaAslId(
                            assistenzaId != null ? String.format("%06d", assistenzaId) : null
                    );

                    result.setDescrizioneFonte(
                            List.of(record.get(REEA_D_FONTE.FONTE_DESC) != null
                                    ? record.get(REEA_D_FONTE.FONTE_DESC)
                                    : "PROVA")
                    );
                    result.setTelefono(record.get("telefono_soggetto", String.class));
                    result.setEmail(record.get("email_soggetto", String.class));

                    LocalDateTime adesioneDataVal = record.get(REEA_T_ADESIONE.ADESIONE_DATA);
                    result.setAdesioneData(adesioneDataVal != null ? adesioneDataVal.toString() : null);
                    result.setPreadesioneTelefono(record.get("pre_tel_dec", String.class));
                    result.setPreadesioneEmail(record.get("pre_email_dec", String.class));
                    result.setAdesioneCod(record.get(REEA_T_ADESIONE.ADESIONE_COD));
                    result.setTesseraTeam(record.get("tessera_team", String.class));
                    result.setIdAura(record.get("id_aura", String.class));

                    result.setDomicilioAslCod(record.get(REEA_T_ADESIONE.DOMICILIO_ASL_COD));
                    result.setDomicilioAslDesc(record.get("domicilio_asl_desc_soggetto", String.class));
                    result.setResidenzaAslDesc(record.get("residenza_asl_desc_soggetto", String.class));
                    result.setDataCreazione(
                            DateConversionUtils.toOffsetDateTime(record.get(REEA_T_SOGGETTO.DATA_CREAZIONE))
                    );



                    LocalDateTime presentazioneIstanzaData =
                            record.get(REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA);
                    result.setPresentazioneIstanzaData(
                            presentazioneIstanzaData != null ? presentazioneIstanzaData.toString() : null
                    );

                    registro.setVersioneNumero(record.get(REEA_T_SOGGETTO.VERSIONE_NUMERO));
                    registro.setSezione(record.get(REEA_T_REGISTRO.SEZIONE));
                    registro.setInseritoInSorveglianza(
                            record.get(REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA)
                    );
                    registro.setTipoElencoInail(record.get(REEA_T_REGISTRO.TIPO_ELENCO_INAIL));

                    result.setSoggettoStatoId(record.get(REEA_T_SOGGETTO.SOGGETTO_STATO_ID));
                    result.setSoggettoStatoNote(record.get(REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE));
                    result.setUtenteModifica(record.get(REEA_T_SOGGETTO.UTENTE_MODIFICA));
                    result.setUtenteCreazione(record.get(REEA_T_SOGGETTO.UTENTE_CREAZIONE));
                    result.setListaFonteByIdSoggetto(
                            fonteRepository.findListaFonteDescByIdSoggetto(soggettoId)
                    );
                    registro.setAttSanitariaStato(record.get(REEA_T_REGISTRO.ATT_SANITARIA_STATO));
                    registro.setAttSanitariaSpresal(record.get(REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL));
                    registro.setAttSanitariaSpresalData(record.get(REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA));
                    registro.setAttSanitariaInail(record.get(REEA_T_REGISTRO.ATT_SANITARIA_INAIL));

                    Integer registroId = record.get(REEA_T_REGISTRO.REGISTRO_ID);
                    registro.setRegistroId(registroId);

                    // Esposizioni / INAIL / NPLA / Spresal / Esenzioni -> puoi lasciare esattamente
                    // il tuo codice originale, perché usa solo Field tipizzati e non nomi stringa.

                    // Esposizioni
    				result.setListaEsposizione(
    						dsl.select(
    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA,
    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE,
    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO,
    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE,
									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD,
									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC,
									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP,
									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA
    								)
    						.from(REEA_T_ESPOSIZIONE)
    						.where(REEA_T_ESPOSIZIONE.SOGGETTO_ID_HMAC.eq(
    								DSL.field("reea.hmac_soggetto_id({0}, {1})", String.class,
    										DSL.val(soggettoId.intValue()), DSL.val("16<odcc8!"))))
    						.fetch(r -> {
    							EsposizioneDTO e = new EsposizioneDTO();
    							e.setEsposizioneAzienda(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA));
    							e.setEsposizioneMansione(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE));
    							LocalDateTime ini = r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO);
    							e.setEsposizioneInizio(ini != null ? ini.toLocalDate() : null);
    							LocalDateTime fin = r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE);
    							e.setEsposizioneFine(fin != null ? fin.toLocalDate() : null);
								e.setEsposizioneAziendaComuneCod(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD));
								e.setEsposizioneAziendaComuneDesc(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC));
								e.setEsposizioneAziendaCap(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP));
								e.setEsposizioneAziendaProvincia(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA));

								return e;
    						})
    						);

    				// INAIL e NPLA
    				if (registroId != null) {
    					result.setListaInail(
    							dsl.selectFrom(REEA_T_REGISTRO_INAIL)
    							.where(REEA_T_REGISTRO_INAIL.REGISTRO_ID.eq(registroId))
    							.fetch(r -> {
    								InailDTO dto = new InailDTO();
    								dto.setRegistroId(r.get(REEA_T_REGISTRO_INAIL.REGISTRO_ID));
    								dto.setCognome(r.get(REEA_T_REGISTRO_INAIL.COGNOME));
    								dto.setNome(r.get(REEA_T_REGISTRO_INAIL.NOME));
    								dto.setCodiceFiscale(r.get(REEA_T_REGISTRO_INAIL.CODICE_FISCALE));
    								dto.setTipologiaInail(r.get(REEA_T_REGISTRO_INAIL.TIPO_ELENCO_INAIL));
    								dto.setDomanda(r.get(REEA_T_REGISTRO_INAIL.DOMANDA));
                                    dto.setSesso(r.get(REEA_T_REGISTRO_INAIL.SESSO));
                                    dto.setDataNascita(r.get(REEA_T_REGISTRO_INAIL.DATA_NASCITA) != null
                                            ? LocalDate.parse(r.get(REEA_T_REGISTRO_INAIL.DATA_NASCITA).toString()) : null);
                                    dto.setIndirizzoResidenza(r.get(REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA));
                                    dto.setIstatResidenza(r.get(REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA));
                                    dto.setCapResidenza(r.get(REEA_T_REGISTRO_INAIL.CAP_RESIDENZA));
                                    dto.setRegioneResidenza(r.get(REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA));
                                    dto.setProvinciaResidenza(r.get(REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA));
                                    dto.setComuneResidenza(r.get(REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA));



                                    return dto;
    							})
    							);

    					result.setListaNpla(
    							dsl.selectFrom(REEA_T_REGISTRO_PDL_AMIANTO)
    							.where(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.eq(registroId))
    							.fetch(r -> {
    								NplaDTO dto = new NplaDTO();
    								dto.setAziendaNome(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME));
    								dto.setAziendaPiva(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA));
    								dto.setAslCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE));
    								dto.setComuneCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE));
    								dto.setAnno(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ANNO));
    								dto.setPeriodo(r.get(REEA_T_REGISTRO_PDL_AMIANTO.PERIODO));
    								dto.setTipologiaPiano(r.get(REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO));
    								dto.setQuantitaDaRimuovere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE));
    								dto.setQuantitaRimossa(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA));
									dto.setCodiceFiscale(r.get(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE));
									dto.setIdCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE));
    								return dto;
    							})
    							);

    					
                        List<SpresalDTO> listaAnamnesiSpresal = dsl
                                .selectFrom(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                                .where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.eq(registroId))
                                .fetch(r -> {
                                    SpresalDTO dto = new SpresalDTO();

                                    // Helper solo per campi BigDecimal (sigarette/sigari/pipa anni, etÃ , die)
                                    java.util.function.Function<Number, BigDecimal> toBD =
                                            n -> n != null ? BigDecimal.valueOf(n.longValue()) : null;

                                    // Long diretto
                                    dto.setRegSpresalAnamnesiId(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID) != null
                                            ? r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID).longValue() : null);

                                    dto.setIdSpresal(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL));
                                    // LocalDate diretto (non String)
                                    dto.setDataIntervista(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA));

                                    dto.setNominativoIntervistatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE));

                                    // === FUMATORE ===
                                    dto.setFumatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE));

                                    // === SIGARETTE ===
                                    dto.setSigarette(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE));
                                    dto.setSigaretteAnni(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI)));
                                    dto.setSigaretteEtaInizio(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO)));
                                    dto.setSigaretteFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE));
                                    dto.setSigaretteEtaFine(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE)));
                                    dto.setSigaretteDie(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE)));

                                    // === SIGARI ===
                                    dto.setSigari(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI));
                                    dto.setSigariAnni(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI)));
                                    dto.setSigariEtaInizio(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO)));
                                    dto.setSigariFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE));
                                    dto.setSigariEtaFine(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE)));
                                    dto.setSigariDie(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE)));

                                    // === PIPA ===
                                    dto.setPipa(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA));
                                    dto.setPipaAnni(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI)));
                                    dto.setPipaEtaInizio(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO)));
                                    dto.setPipaFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE));
                                    dto.setPipaEtaFine(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE)));
                                    dto.setPipaDie(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE)));

                                    // === OCCUPAZIONE ===
                                    dto.setOccupazioneNum(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM));
                                    // Integer diretto
                                    dto.setOccupazioneAnnoInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO));
                                    dto.setOccupazioneAnnoFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE));

                                    dto.setOccupazioneTipo(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO));
                                    dto.setOccupazioneDescrizioneLavoro(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO));
                                    dto.setOccupazioneNomeEIndirizzoDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA));
                                    dto.setOccupazioneAttivitaDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA));
                                    dto.setNotaAttivitaConAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO));

                                    // === ESPOSIZIONE AMIANTO ===
                                    dto.setAnamnesiEsposizioneAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO));
                                    dto.setEsposizioneProfessionale(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE));
                                    // Integer diretto
                                    dto.setAnnoFineEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE));
                                    dto.setLivelloEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE));
                                    dto.setInserimentoInSorveglianza(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA));
                                    dto.setCounseling(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.COUNSELING));
                                    // === CRPT ===
                                    dto.setOccupazioneEsposizioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT));
                                    dto.setOccupazioneSettoreDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT));
                                    dto.setOccupazioneMansioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT));
                                    dto.setOccupazioneRagioneSocialeDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT));
                                    dto.setOccupazionePivaDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT));
                                    dto.setOccupazioneCodiceFiscaleDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_CODICE_FISCALE_DITTA_CRPT));

                                    return dto;
                                });

                        result.setListaAnamnesiSpresal(listaAnamnesiSpresal);

//                        Integer adesioneIdCheck = record.get(REEA_T_ADESIONE.SOGGETTO_ID);
//                        if (adesioneIdCheck != null) {
//                            result.setListaEsenzione(
//                                    esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId)
//                            );
//                        } else {
//                            result.setListaEsenzione(Collections.emptyList());
//                        }

                        result.setListaEsenzione(
                                esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId)
                        );


                    }

    				return result;
    			});
    }



//    @Override
//    public AnagraficaDTO findById(Long soggettoId) {
//
//        var ASL_DOM = REEA_D_ASL.as("asl_dom");
//        var ASL_RES = REEA_D_ASL.as("asl_res");
//        var ASL_ASS = REEA_D_ASL.as("asl_ass");
//
//        return dsl.select(
//                        ASL_DOM.ASL_AZIENDA_DESC.as("domicilio_asl_desc_soggetto"),
//                        ASL_RES.ASL_AZIENDA_DESC.as("residenza_asl_desc_soggetto"),
//                        ASL_ASS.ASL_AZIENDA_DESC.as("assistenza_asl_desc"),
//    			REEA_T_SOGGETTO.FONTE_ID,
//    			REEA_D_FONTE.FONTE_DESC,
//    			REEA_T_SOGGETTO.DATA_MODIFICA,
//    			REEA_T_SOGGETTO.DATA_CREAZIONE,
//    			REEA_T_SOGGETTO.NASCITA_DATA,
//    			REEA_T_SOGGETTO.SOGGETTO_ID,
//    			REEA_T_SOGGETTO.VERSIONE_NUMERO,
//                        REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO,
//                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD,
//                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD,
//                        REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO,
//                        REEA_T_SOGGETTO.DOMICILIO_STATO_DESC,
//                        DSL.when(REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_SOGGETTO.NOME_CIFRATO, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("nome"),
//                        DSL.when(REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_SOGGETTO.COGNOME_CIFRATO, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("cognome"),
//    			REEA_T_SOGGETTO.SESSO,
//    			REEA_T_SOGGETTO.NASCITA_COMUNE_COD,
//    			REEA_T_SOGGETTO.NASCITA_STATO_COD,
//    			REEA_T_SOGGETTO.NASCITA_PROVINCIA_COD,
//    			REEA_T_SOGGETTO.CODICE_FISCALE,
//    			REEA_T_SOGGETTO.CITTADINANZA_STATO_COD,
//    			REEA_T_SOGGETTO.CITTADINANZA_STATO_DESC,
//    			REEA_T_SOGGETTO.DOMICILIO_ASL_ID,
//    			REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_COD,
//    			REEA_T_SOGGETTO.DOMICILIO_STATO_COD,
//    			REEA_T_SOGGETTO.RESIDENZA_STATO_COD,
//    			REEA_T_SOGGETTO.RESIDENZA_ASL_ID,
//    			REEA_T_SOGGETTO.SOGGETTO_STATO_ID,
//    			REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE,
//    			REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC,
//    			REEA_T_REGISTRO.SEZIONE,
//    			REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA,
//    			REEA_T_REGISTRO.TIPO_ELENCO_INAIL,
//    			REEA_T_SOGGETTO.ASSISTENZA_ASL_ID,
//    			REEA_T_REGISTRO.REGISTRO_ID,
//				REEA_T_REGISTRO.ATT_SANITARIA_STATO,
//				REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL,
//				REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA,
//                REEA_T_REGISTRO.ATT_SANITARIA_INAIL,
//                REEA_T_ADESIONE.ADESIONE_DATA,
//                REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA,
//                        REEA_T_SOGGETTO.NASCITA_STATO_DESC,
//                        REEA_T_SOGGETTO.RESIDENZA_CAP,
//                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC,
//                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC,
//                        REEA_T_SOGGETTO.RESIDENZA_STATO_DESC,
//                        DSL.when(REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("telefono_aura"),
//                        DSL.when(REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA, DSL.val("16<odcc8!"))
//                        		).otherwise((String) null).as("email_aura"),
//
//                        DSL.when(REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_ADESIONE.TELEFONO_CIFRATO, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("pre_tel_dec"),
//                        DSL.when(REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_ADESIONE.EMAIL_CIFRATA, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("pre_email_dec"),
//                        DSL.when(REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_ADESIONE.EMAIL_CIFRATA, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("preadesione_email"),
//                        DSL.when(REEA_T_SOGGETTO.TELEFONO_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_SOGGETTO.TELEFONO_CIFRATO, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("telefono_soggetto"),
//                        DSL.when(REEA_T_SOGGETTO.EMAIL_CIFRATA.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_SOGGETTO.EMAIL_CIFRATA, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("email_soggetto"),
//
//						REEA_T_ADESIONE.ADESIONE_COD,
//                        REEA_T_SOGGETTO.NASCITA_COMUNE_DESC.as("nascita_comune_desc"),
//                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_DESC.as("nascita_provincia_desc"),
//                        DSL.coalesce(
//                                DSL.when(REEA_T_SOGGETTO.ID_AURA_CIFRATO.isNotNull(),
//                                    DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.ID_AURA_CIFRATO, DSL.val("16<odcc8!"))
//                                ).otherwise((String) null),
//                                DSL.when(REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
//                                    DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_ADESIONE.ID_AURA_CIFRATO, DSL.val("16<odcc8!"))
//                                ).otherwise((String) null)
//                            ).as("id_aura"),
//                        DSL.coalesce(
//                                DSL.when(REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA.isNotNull(),
//                                    DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA, DSL.val("16<odcc8!"))
//                                ).otherwise((String) null),
//                                DSL.when(REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
//                                    DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                        REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA, DSL.val("16<odcc8!"))
//                                ).otherwise((String) null)
//                            ).as("tessera_team"),
//
//
//                        DSL.coalesce(REEA_T_SOGGETTO.DOMICILIO_CAP, REEA_T_ADESIONE.DOMICILIO_CAP).as("domicilio_cap"),
//                        REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_DESC,REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC .as("domicilio_provincia_desc"),
//                        DSL.coalesce(REEA_T_SOGGETTO.DOMICILIO_COMUNE_DESC, REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC).as("domicilio_comune_desc"),
//                        DSL.coalesce(REEA_T_SOGGETTO.DOMICILIO_COMUNE_COD, REEA_T_ADESIONE.DOMICILIO_COMUNE_COD).as("domicilio_comune_cod"),
//						REEA_T_ADESIONE.DOMICILIO_ASL_COD,
//						REEA_T_ADESIONE.DOMICILIO_ASL_DESC,
//						REEA_T_ADESIONE.RESIDENZA_ASL_COD,
//						REEA_T_ADESIONE.RESIDENZA_ASL_DESC,
//	                    DSL.when(REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
//	                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//	                                REEA_T_ADESIONE.COGNOME_CIFRATO, DSL.val("16<odcc8!"))
//	                        ).otherwise((String) null).as("adesione_cognome"),
//	                    DSL.when(REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
//	                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//	                                REEA_T_ADESIONE.NOME_CIFRATO, DSL.val("16<odcc8!"))
//	                        ).otherwise((String) null).as("adesione_nome"),
//                        REEA_T_ADESIONE.NASCITA_DATA.as("adesione_nascita_data"),
//                        DSL.when(REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("residenza_indirizzo"),
//                        DSL.when(REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO.isNotNull(),
//                                DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
//                                    REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO, DSL.val("16<odcc8!"))
//                            ).otherwise((String) null).as("domicilio_indirizzo"),
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE,
//                        REEA_T_SOGGETTO.DATA_DECESSO
//						)
//    			.from(REEA_T_SOGGETTO)
//    			.join(REEA_D_FONTE)
//    			.on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//                .leftJoin(ASL_ASS)
//                 .on(ASL_ASS.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//
//                 .leftJoin(ASL_DOM)
//                 .on(ASL_DOM.ASL_ID.eq(REEA_T_SOGGETTO.DOMICILIO_ASL_ID))
//
//                 .leftJoin(ASL_RES)
//                  .on(ASL_RES.ASL_ID.eq(REEA_T_SOGGETTO.RESIDENZA_ASL_ID))
//
//                .join(REEA_T_REGISTRO)
//    			.on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
//    					DSL.field("reea.hmac_soggetto_id({0}, {1})",
//    							String.class,
//    							REEA_T_SOGGETTO.SOGGETTO_ID,
//    							DSL.val("16<odcc8!"))))
//    			.join(REEA_D_SOGGETTO_STATO)
//    			.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//				.leftJoin(REEA_T_ADESIONE)
//				.on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
//				.where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
//				.limit(1)
//				.fetchOne(record -> {
//					if (record == null) {
//    					return null;
//    				}
//
//    				AnagraficaDTO result = new AnagraficaDTO();
//
//    				result.setSoggettoId(soggettoId);
//    				result.setCodiceFiscale(record.get(REEA_T_SOGGETTO.CODICE_FISCALE));
//    				result.setResidenzaAslId(record.get(REEA_T_SOGGETTO.RESIDENZA_ASL_ID));
//    				result.setDomicilioAslId(record.get(REEA_T_SOGGETTO.DOMICILIO_ASL_ID));
//                    result.setDescrizioneAslCompetenza(record.get("assistenza_asl_desc", String.class));
//                    result.setNascitaStatoDesc(record.get(REEA_T_SOGGETTO.NASCITA_STATO_DESC));
//                    result.setNome(record.get("nome", String.class));
//    				result.setCognome(record.get("cognome", String.class));
//                    result.setTelefonoAura(record.get("telefono_aura", String.class));
//                    result.setEmailAura(record.get("email_aura", String.class));
//                    result.setResidenzaNumeroCivico(record.get(REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO));
//                    result.setResidenzaComuneCod(record.get(REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD));
//                    result.setDomicilioNumeroCivico(record.get(REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO));
//                    result.setDomicilioStatoDesc(record.get(REEA_T_SOGGETTO.DOMICILIO_STATO_DESC));
//                    result.setSesso(record.get(REEA_T_SOGGETTO.SESSO));
//    				result.setNascitaData(record.get(REEA_T_SOGGETTO.NASCITA_DATA));
//    				result.setFonteId(record.get(REEA_T_SOGGETTO.FONTE_ID));
////                    String cognomeAdes = record.get("adesione_cognome", String.class);
////                    result.setCognome(cognomeAdes != null ? cognomeAdes : record.get("cognome", String.class));
////                    String nomeAdes = record.get("adesione_nome", String.class);
////                    result.setNome(nomeAdes != null ? nomeAdes : record.get("nome", String.class));
////                    LocalDate nascitaAdes = record.get("adesione_nascita_data", LocalDate.class);
////                    result.setNascitaData(nascitaAdes != null ? nascitaAdes : record.get(REEA_T_SOGGETTO.NASCITA_DATA));
//    				result.setNascitaComuneCod(record.get("nascita_comune_cod", String.class));
//                    result.setNascitaComuneDesc(record.get("nascita_comune_desc", String.class));
//                    result.setNascitaProvinciaCod(record.get("nascita_provincia_cod", String.class));
//                    result.setNascitaProvinciaDesc(record.get("nascita_provincia_desc", String.class));
//                    result.setNascitaStatoCod(record.get("nascita_stato_cod", String.class));
//
//                    result.setResidenzaCap(record.get(REEA_T_SOGGETTO.RESIDENZA_CAP));
//                    result.setResidenzaComuneDesc(record.get(REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC));
//                    result.setResidenzaProvinciaCod(record.get(REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD));
//                    result.setResidenzaProvinciaDesc(record.get(REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC));
//                    result.setResidenzaStatoCod(record.get(REEA_T_SOGGETTO.RESIDENZA_STATO_COD));
//                    result.setResidenzaStatoDesc(record.get(REEA_T_SOGGETTO.RESIDENZA_STATO_DESC));
//
//                    result.setDomicilioCap(record.get("domicilio_cap", String.class));
//                    result.setDomicilioComuneDesc(record.get("domicilio_comune_desc", String.class));
//                    result.setDomicilioProvinciaCod(record.get("domicilio_provincia_cod", String.class));
//                    result.setDomicilioProvinciaDesc(record.get("domicilio_provincia_desc", String.class));
//                    result.setDomicilioComuneCod(record.get("domicilio_comune_cod", String.class));
//                    result.setDomicilioStatoCod(record.get("domicilio_stato_cod", String.class));
//
//                    result.setCittadinanzaStatoCod(record.get("cittadinanza_stato_cod", String.class));
//                    result.setCittadinanzaStatoDesc(record.get("cittadinanza_stato_desc", String.class));
//
//                    result.setResidenzaIndirizzo(record.get("residenza_indirizzo", String.class));
//                    result.setDomicilioIndirizzo(record.get("domicilio_indirizzo", String.class));
//
//                    LocalDateTime assistenzaFine = record.get(REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE);
//                    result.setAssistenzaAslFine(assistenzaFine != null ? assistenzaFine.toLocalDate().toString() : null);
//
//                    LocalDateTime dataDecesso = record.get(REEA_T_SOGGETTO.DATA_DECESSO);
//                    result.setDataDecesso(dataDecesso != null ? dataDecesso.toLocalDate().toString() : null);
//
//    				RegistroDTO registro = RegistroUtils.ensureRegistro(result);
//
//    				registro.setDataCreazione(
//    						DateConversionUtils.toOffsetDateTime(record.get(REEA_T_SOGGETTO.DATA_CREAZIONE))
//    						);
//
//    				LocalDateTime dataModifica = record.get(REEA_T_SOGGETTO.DATA_MODIFICA);
//    				if (dataModifica != null) {
//    					registro.setDataModifica(
//    							DateConversionUtils.toOffsetDateTime(dataModifica)
//    							);
//    				}
//
//    				result.setDescrizioneStato(
//    						record.get(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC)
//    						);
//    				Integer assistenzaId = record.get(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID);
//    				result.setAssistenzaAslId(
//    						assistenzaId != null ? String.format("%06d", assistenzaId) : null
//    						);
//
//    				result.setDescrizioneFonte(
//    						List.of(record.get(REEA_D_FONTE.FONTE_DESC) != null
//    						? record.get(REEA_D_FONTE.FONTE_DESC)
//    								: "PROVA")
//    						);
//                    result.setTelefono(record.get("telefono_soggetto", String.class));
//                    result.setEmail(record.get("email_soggetto", String.class));
//
//                    LocalDateTime adesioneDataVal = record.get(REEA_T_ADESIONE.ADESIONE_DATA);
//					result.setAdesioneData(adesioneDataVal != null ? adesioneDataVal.toString() : null);
//					result.setPreadesioneTelefono(record.get("pre_tel_dec", String.class));
//					result.setPreadesioneEmail(record.get("pre_email_dec", String.class));
//					result.setAdesioneCod(record.get(REEA_T_ADESIONE.ADESIONE_COD));
//					result.setTesseraTeam(record.get("tessera_team", String.class));
//					result.setIdAura(record.get("id_aura", String.class));
//					result.setDomicilioAslCod(record.get(REEA_T_ADESIONE.DOMICILIO_ASL_COD));
//                    result.setDomicilioAslDesc(record.get("domicilio_asl_desc_soggetto", String.class));
//                    result.setResidenzaAslDesc(record.get("residenza_asl_desc_soggetto", String.class));
//                    LocalDateTime presentazioneIstanzaData = record.get(REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA);
//                    result.setPresentazioneIstanzaData(
//                            presentazioneIstanzaData != null ? presentazioneIstanzaData.toString() : null
//                    );
//
//
//                    registro.setVersioneNumero(record.get(REEA_T_SOGGETTO.VERSIONE_NUMERO));
//    				registro.setSezione(record.get(REEA_T_REGISTRO.SEZIONE));
//    				registro.setInseritoInSorveglianza(
//    						record.get(REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA)
//    						);
//    				registro.setTipoElencoInail(record.get(REEA_T_REGISTRO.TIPO_ELENCO_INAIL));
//
//    				result.setSoggettoStatoId(record.get(REEA_T_SOGGETTO.SOGGETTO_STATO_ID));
//    				result.setSoggettoStatoNote(record.get(REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE));
//
//    				result.setListaFonteByIdSoggetto(
//    						fonteRepository.findListaFonteDescByIdSoggetto(soggettoId)
//    						);
//					registro.setAttSanitariaStato(record.get(REEA_T_REGISTRO.ATT_SANITARIA_STATO));
//					registro.setAttSanitariaSpresal(record.get(REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL));
//					registro.setAttSanitariaSpresalData(record.get(REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA));
//                    registro.setAttSanitariaInail(record.get(REEA_T_REGISTRO.ATT_SANITARIA_INAIL));
//
//
//
//
//                    Integer registroId = record.get(REEA_T_REGISTRO.REGISTRO_ID);
//    				registro.setRegistroId(registroId);
//
//    				// Esposizioni
//    				result.setListaEsposizione(
//    						dsl.select(
//    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA,
//    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE,
//    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO,
//    								REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE,
//									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD,
//									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC,
//									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP,
//									REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA
//    								)
//    						.from(REEA_T_ESPOSIZIONE)
//    						.where(REEA_T_ESPOSIZIONE.SOGGETTO_ID_HMAC.eq(
//    								DSL.field("reea.hmac_soggetto_id({0}, {1})", String.class,
//    										DSL.val(soggettoId.intValue()), DSL.val("16<odcc8!"))))
//    						.fetch(r -> {
//    							EsposizioneDTO e = new EsposizioneDTO();
//    							e.setEsposizioneAzienda(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA));
//    							e.setEsposizioneMansione(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE));
//    							LocalDateTime ini = r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO);
//    							e.setEsposizioneInizio(ini != null ? ini.toLocalDate() : null);
//    							LocalDateTime fin = r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE);
//    							e.setEsposizioneFine(fin != null ? fin.toLocalDate() : null);
//								e.setEsposizioneAziendaComuneCod(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD));
//								e.setEsposizioneAziendaComuneDesc(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC));
//								e.setEsposizioneAziendaCap(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP));
//								e.setEsposizioneAziendaProvincia(r.get(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA));
//
//								return e;
//    						})
//    						);
//
//    				// INAIL e NPLA
//    				if (registroId != null) {
//    					result.setListaInail(
//    							dsl.selectFrom(REEA_T_REGISTRO_INAIL)
//    							.where(REEA_T_REGISTRO_INAIL.REGISTRO_ID.eq(registroId))
//    							.fetch(r -> {
//    								InailDTO dto = new InailDTO();
//    								dto.setRegistroId(r.get(REEA_T_REGISTRO_INAIL.REGISTRO_ID));
//    								dto.setCognome(r.get(REEA_T_REGISTRO_INAIL.COGNOME));
//    								dto.setNome(r.get(REEA_T_REGISTRO_INAIL.NOME));
//    								dto.setCodiceFiscale(r.get(REEA_T_REGISTRO_INAIL.CODICE_FISCALE));
//    								dto.setTipologiaInail(r.get(REEA_T_REGISTRO_INAIL.TIPO_ELENCO_INAIL));
//    								dto.setDomanda(r.get(REEA_T_REGISTRO_INAIL.DOMANDA));
//                                    dto.setSesso(r.get(REEA_T_REGISTRO_INAIL.SESSO));
//                                    dto.setDataNascita(r.get(REEA_T_REGISTRO_INAIL.DATA_NASCITA) != null
//                                            ? LocalDate.parse(r.get(REEA_T_REGISTRO_INAIL.DATA_NASCITA).toString()) : null);
//                                    dto.setIndirizzoResidenza(r.get(REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA));
//                                    dto.setIstatResidenza(r.get(REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA));
//                                    dto.setCapResidenza(r.get(REEA_T_REGISTRO_INAIL.CAP_RESIDENZA));
//                                    dto.setRegioneResidenza(r.get(REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA));
//                                    dto.setProvinciaResidenza(r.get(REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA));
//                                    dto.setComuneResidenza(r.get(REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA));
//
//
//
//                                    return dto;
//    							})
//    							);
//
//    					result.setListaNpla(
//    							dsl.selectFrom(REEA_T_REGISTRO_PDL_AMIANTO)
//    							.where(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.eq(registroId))
//    							.fetch(r -> {
//    								NplaDTO dto = new NplaDTO();
//    								dto.setAziendaNome(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME));
//    								dto.setAziendaPiva(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA));
//    								dto.setAslCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE));
//    								dto.setComuneCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE));
//    								dto.setAnno(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ANNO));
//    								dto.setPeriodo(r.get(REEA_T_REGISTRO_PDL_AMIANTO.PERIODO));
//    								dto.setTipologiaPiano(r.get(REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO));
//    								dto.setQuantitaDaRimuovere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE));
//    								dto.setQuantitaRimossa(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA));
//									dto.setCodiceFiscale(r.get(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE));
//									dto.setIdCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE));
//    								return dto;
//    							})
//    							);
//
//    					
//                        List<SpresalDTO> listaAnamnesiSpresal = dsl
//                                .selectFrom(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
//                                .where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.eq(registroId))
//                                .fetch(r -> {
//                                    SpresalDTO dto = new SpresalDTO();
//
//                                    // Helper solo per campi BigDecimal (sigarette/sigari/pipa anni, etÃ , die)
//                                    java.util.function.Function<Number, BigDecimal> toBD =
//                                            n -> n != null ? BigDecimal.valueOf(n.longValue()) : null;
//
//                                    // Long diretto
//                                    dto.setRegSpresalAnamnesiId(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID) != null
//                                            ? r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID).longValue() : null);
//
//
//                                    // LocalDate diretto (non String)
//                                    dto.setDataIntervista(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA));
//
//                                    dto.setNominativoIntervistatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE));
//
//                                    // === FUMATORE ===
//                                    dto.setFumatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE));
//
//                                    // === SIGARETTE ===
//                                    dto.setSigarette(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE));
//                                    dto.setSigaretteAnni(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI)));
//                                    dto.setSigaretteEtaInizio(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO)));
//                                    dto.setSigaretteFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE));
//                                    dto.setSigaretteEtaFine(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE)));
//                                    dto.setSigaretteDie(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE)));
//
//                                    // === SIGARI ===
//                                    dto.setSigari(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI));
//                                    dto.setSigariAnni(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI)));
//                                    dto.setSigariEtaInizio(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO)));
//                                    dto.setSigariFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE));
//                                    dto.setSigariEtaFine(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE)));
//                                    dto.setSigariDie(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE)));
//
//                                    // === PIPA ===
//                                    dto.setPipa(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA));
//                                    dto.setPipaAnni(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI)));
//                                    dto.setPipaEtaInizio(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO)));
//                                    dto.setPipaFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE));
//                                    dto.setPipaEtaFine(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE)));
//                                    dto.setPipaDie(toBD.apply(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE)));
//
//                                    // === OCCUPAZIONE ===
//                                    dto.setOccupazioneNum(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM));
//                                    // Integer diretto
//                                    dto.setOccupazioneAnnoInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO));
//                                    dto.setOccupazioneAnnoFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE));
//                                    dto.setOccupazioneTipo(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO));
//                                    dto.setOccupazioneDescrizioneLavoro(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO));
//                                    dto.setOccupazioneNomeEIndirizzoDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA));
//                                    dto.setOccupazioneAttivitaDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA));
//                                    dto.setNotaAttivitaConAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO));
//
//                                    // === ESPOSIZIONE AMIANTO ===
//                                    dto.setAnamnesiEsposizioneAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO));
//                                    dto.setEsposizioneProfessionale(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE));
//                                    // Integer diretto
//                                    dto.setAnnoFineEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE));
//                                    dto.setLivelloEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE));
//                                    dto.setInserimentoInSorveglianza(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA));
//
//                                    // === CRPT ===
//                                    dto.setOccupazioneEsposizioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT));
//                                    dto.setOccupazioneSettoreDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT));
//                                    dto.setOccupazioneMansioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT));
//                                    dto.setOccupazioneRagioneSocialeDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT));
//                                    dto.setOccupazionePivaDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT));
//                                    dto.setOccupazioneCodiceFiscaleDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_CODICE_FISCALE_DITTA_CRPT));
//
//                                    return dto;
//                                });
//
//                        result.setListaAnamnesiSpresal(listaAnamnesiSpresal);
//
////                        Integer adesioneIdCheck = record.get(REEA_T_ADESIONE.SOGGETTO_ID);
////                        if (adesioneIdCheck != null) {
////                            result.setListaEsenzione(
////                                    esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId)
////                            );
////                        } else {
////                            result.setListaEsenzione(Collections.emptyList());
////                        }
//
//                        result.setListaEsenzione(
//                                esenzioneRepository.findListaEsenzioniByIdSoggetto(soggettoId)
//                        );
//
//
//                    }
//
//    				return result;
//    			});
//    }


    @Override
    public AnagraficaDTO findByCodiceFiscale(String codiceFiscale) {


//    	String condizioneAnonimizzataCF = CodiceFiscale.AnonimizzaCodiceFiscale(codiceFiscale);
		String condizioneAnonimizzataCF = null;
		Optional<String> flagAnonimizzaCF =
                Optional.ofNullable(
                        dsl.select(
                                REEA_C_PARAMETRO.PARAMETRO_VALORE
                        )
                        .from(REEA_C_PARAMETRO)
                        .where(
                                REEA_C_PARAMETRO.PARAMETRO_COD.eq(
                                        "FlagAnonimizzaCF"
                                )
                        )
                        .fetchOneInto(String.class)
                );
		try {
			if (flagAnonimizzaCF.isPresent()
		            && "false".equalsIgnoreCase(
		                    flagAnonimizzaCF.get().trim()
		            )
		            && codiceFiscale != null
		            && !codiceFiscale.isBlank()) 
				condizioneAnonimizzataCF = CodiceFiscale.AnonimizzaCodiceFiscale(codiceFiscale);
		} catch (IllegalArgumentException e) {
			// CF non standard o giÃ  anonimizzato â cerca solo per valore letterale
		}
    	Condition condCodiceFiscale = noCondition();
    	if (codiceFiscale != null && !codiceFiscale.isEmpty()) {
    		condCodiceFiscale = condCodiceFiscale.and(
    				REEA_T_SOGGETTO.CODICE_FISCALE.in(codiceFiscale.toUpperCase()));
    	}

    	Condition condCodiceFiscaleAnonimizzata = noCondition();
    	if (condizioneAnonimizzataCF != null && !condizioneAnonimizzataCF.isEmpty()) {
    		condCodiceFiscaleAnonimizzata = condCodiceFiscaleAnonimizzata.and(
    				REEA_T_SOGGETTO.CODICE_FISCALE.in(condizioneAnonimizzataCF.toUpperCase()));
    	}


    	return dsl.select(
    			REEA_D_ASL.ASL_AZIENDA_DESC,
    			REEA_T_SOGGETTO.FONTE_ID,
    			REEA_D_FONTE.FONTE_DESC,
    			REEA_T_SOGGETTO.DATA_MODIFICA,
    			REEA_T_SOGGETTO.DATA_CREAZIONE,
    			REEA_T_SOGGETTO.NASCITA_DATA,
    			REEA_T_SOGGETTO.SOGGETTO_ID,
    			REEA_T_SOGGETTO.VERSIONE_NUMERO,
    			REEA_T_SOGGETTO.INSERIMENTO_TIPO_ID,
    			DSL.when(
    				    REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
    				    DSL.function(
    				        "pgp_sym_decrypt",
    				        SQLDataType.VARCHAR,
    				        REEA_T_SOGGETTO.NOME_CIFRATO,
    				        DSL.val("16<odcc8!", SQLDataType.VARCHAR)
    				    )
    				).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("nome"),
    			DSL.when(
    				    REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
    				    DSL.function(
    				        "pgp_sym_decrypt",
    				        SQLDataType.VARCHAR,
    				        REEA_T_SOGGETTO.COGNOME_CIFRATO,
    				        DSL.val("16<odcc8!", SQLDataType.VARCHAR)
    				    )
    				).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("cognome"),
    			REEA_T_SOGGETTO.SESSO,
    			REEA_T_SOGGETTO.SOGGETTO_ID,
    			REEA_T_SOGGETTO.DOMICILIO_ASL_ID,
    			REEA_T_SOGGETTO.RESIDENZA_ASL_ID,
    			REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC,
    			REEA_T_REGISTRO.SEZIONE,
    			REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA,
    			REEA_T_REGISTRO.TIPO_ELENCO_INAIL,
    			REEA_T_SOGGETTO.ASSISTENZA_ASL_ID,
    			REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL,
    			REEA_T_REGISTRO.ATT_SANITARIA_INAIL,
    			REEA_T_REGISTRO.REGISTRO_ID,
    			REEA_T_SOGGETTO.SOGGETTO_STATO_ID,
    			REEA_T_SOGGETTO.UTENTE_CREAZIONE
    			)
    			.from(REEA_T_SOGGETTO)
    			.join(REEA_D_FONTE)
    			.on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
                .leftJoin(REEA_D_ASL)
    			.on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.DOMICILIO_ASL_ID))
    			.join(REEA_T_REGISTRO)
    			.on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
    					DSL.field("reea.hmac_soggetto_id({0}, {1})",
    							String.class,
    							REEA_T_SOGGETTO.SOGGETTO_ID,
    							DSL.val("16<odcc8!"))))
    			.join(REEA_D_SOGGETTO_STATO)
    			.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
    			.where(condCodiceFiscale
    					.or(condCodiceFiscaleAnonimizzata))
    			.fetchOne(record -> {
    				if (record == null) {
    					return null;
    				}

    				AnagraficaDTO result = new AnagraficaDTO();

    				result.setSoggettoId(Long.valueOf(record.get(REEA_T_SOGGETTO.SOGGETTO_ID)));
    				result.setCodiceFiscale(codiceFiscale);
    				result.setResidenzaAslId(record.get(REEA_T_SOGGETTO.RESIDENZA_ASL_ID));
    				result.setDomicilioAslId(record.get(REEA_T_SOGGETTO.DOMICILIO_ASL_ID));
    				result.setNome(record.get("nome", String.class));
    				result.setCognome(record.get("cognome", String.class));
    				result.setSesso(record.get(REEA_T_SOGGETTO.SESSO));
    				result.setNascitaData(record.get(REEA_T_SOGGETTO.NASCITA_DATA));
    				result.setFonteId(record.get(REEA_T_SOGGETTO.FONTE_ID));
    				result.setInserimentoTipoId(record.get(REEA_T_SOGGETTO.INSERIMENTO_TIPO_ID));
    				result.setUtenteCreazione(record.get(REEA_T_SOGGETTO.UTENTE_CREAZIONE));
    				// RegistroDTO sempre inizializzato
    				RegistroDTO registro = RegistroUtils.ensureRegistro(result);

    				registro.setDataCreazione(DateConversionUtils.toOffsetDateTime(record.get(REEA_T_SOGGETTO.DATA_CREAZIONE)));
    				registro.setVersioneNumero(record.get(REEA_T_SOGGETTO.VERSIONE_NUMERO));
    				registro.setSezione(record.get(REEA_T_REGISTRO.SEZIONE));
    				registro.setInseritoInSorveglianza(record.get(REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA));
    				registro.setTipoElencoInail(record.get(REEA_T_REGISTRO.TIPO_ELENCO_INAIL));
    				registro.setAttSanitariaSpresal(record.get(REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL));
    				registro.setAttSanitariaInail(record.get(REEA_T_REGISTRO.ATT_SANITARIA_INAIL));
    				registro.setRegistroId(record.get(REEA_T_REGISTRO.REGISTRO_ID));

    				result.setDescrizioneStato(record.get(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC));
    				Integer assistenzaId = record.get(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID);
    				result.setAssistenzaAslId(assistenzaId != null ? String.format("%06d", assistenzaId) : null);

    				result.setDescrizioneFonte(
    						List.of(record.get(REEA_D_FONTE.FONTE_DESC) != null
    						? record.get(REEA_D_FONTE.FONTE_DESC)
    								: "PROVA")
    						);
    				result.setSoggettoStatoId(record.get(REEA_T_SOGGETTO.SOGGETTO_STATO_ID));

    				result.setListaFonteByIdSoggetto(
    						fonteRepository.findListaFonteDescByIdSoggetto(
    								Long.valueOf(record.get(REEA_T_SOGGETTO.SOGGETTO_ID))
    								)
    						);

    				return result;
    			});
    }


    @Override
    public AnagraficaDTO updateSoggetto(DSLContext ctx, Long id, AnagraficaDTO dtos, Integer nuovaVersione, String cfOperatore) {

        Integer nuovaAslId = null;
        if (dtos.getAssistenzaAslId() != null && !dtos.getAssistenzaAslId().isBlank()) {
            try {
                nuovaAslId = Integer.parseInt(dtos.getAssistenzaAslId().trim());
            } catch (NumberFormatException ignored) {
            }
        }

        int updatedRows = ctx.update(REEA_T_SOGGETTO)
                .set(REEA_T_SOGGETTO.CODICE_FISCALE, dtos.getCodiceFiscale())
                .set(REEA_T_SOGGETTO.RESIDENZA_ASL_ID, dtos.getResidenzaAslId())
                .set(REEA_T_SOGGETTO.DOMICILIO_ASL_ID, dtos.getDomicilioAslId())

                .set(REEA_T_SOGGETTO.NOME_CIFRATO,
                        DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                DSL.val(dtos.getNome() != null ? dtos.getNome().toUpperCase() : "N/D", SQLDataType.VARCHAR),
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                .set(REEA_T_SOGGETTO.COGNOME_CIFRATO,
                        DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                DSL.val(dtos.getCognome() != null ? dtos.getCognome().toUpperCase() : "N/D", SQLDataType.VARCHAR),
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR)))

                .set(REEA_T_SOGGETTO.NASCITA_DATA, dtos.getNascitaData())
                .set(REEA_T_SOGGETTO.FONTE_ID, dtos.getFonteId())
                .set(REEA_T_SOGGETTO.SOGGETTO_STATO_ID, dtos.getSoggettoStatoId())
                .set(REEA_T_SOGGETTO.DATA_MODIFICA, DSL.currentLocalDateTime())
                .set(REEA_T_SOGGETTO.VERSIONE_NUMERO, nuovaVersione)
                .set(REEA_T_SOGGETTO.UTENTE_MODIFICA, cfOperatore)

                .set(REEA_T_SOGGETTO.TELEFONO_CIFRATO,
                        dtos.getTelefono() != null && !dtos.getTelefono().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getTelefono(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : REEA_T_SOGGETTO.TELEFONO_CIFRATO)

                .set(REEA_T_SOGGETTO.EMAIL_CIFRATA,
                        dtos.getEmail() != null && !dtos.getEmail().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getEmail(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : REEA_T_SOGGETTO.EMAIL_CIFRATA)

                .set(REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO,
                        dtos.getResidenzaIndirizzo() != null && !dtos.getResidenzaIndirizzo().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getResidenzaIndirizzo(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO)

                .set(REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO, dtos.getResidenzaNumeroCivico())
                .set(REEA_T_SOGGETTO.RESIDENZA_CAP, dtos.getResidenzaCap())
                .set(REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC, dtos.getResidenzaComuneDesc())
                .set(REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC, dtos.getResidenzaProvinciaDesc())
                .set(REEA_T_SOGGETTO.RESIDENZA_STATO_DESC, dtos.getResidenzaStatoDesc())

                .set(REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO,
                        dtos.getDomicilioIndirizzo() != null && !dtos.getDomicilioIndirizzo().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getDomicilioIndirizzo(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO)

                .set(REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO, dtos.getDomicilioNumeroCivico())
                .set(REEA_T_SOGGETTO.DOMICILIO_CAP, dtos.getDomicilioCap())
                .set(REEA_T_SOGGETTO.DOMICILIO_COMUNE_DESC, dtos.getDomicilioComuneDesc())
                .set(REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_DESC, dtos.getDomicilioProvinciaDesc())
                .set(REEA_T_SOGGETTO.DOMICILIO_STATO_DESC, dtos.getDomicilioStatoDesc())

                .set(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID, nuovaAslId)
                
                // HASH COMPLETI (PER ORA LI RICALCOLO POTREMMO POI MODIFICARE IL DTO)
                .set(
                	REEA_T_SOGGETTO.COGNOME_HASH,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getCognome() != null
                                ? dtos.getCognome().toUpperCase()
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )
                .set(
                    REEA_T_SOGGETTO.NOME_HASH,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getNome() != null
                                ? dtos.getNome().toUpperCase()
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )

                // HASH PRIME 2 LETTERE
                .set(
                    REEA_T_SOGGETTO.COGNOME_HASH_2CHAR,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getCognome() != null && dtos.getCognome().length() >= 2
                                ? dtos.getCognome().toUpperCase().substring(0, 2)
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )
                .set(
                    REEA_T_SOGGETTO.NOME_HASH_2CHAR,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getNome() != null && dtos.getNome().length() >= 2
                                ? dtos.getNome().toUpperCase().substring(0, 2)
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )

                // HASH PRIMA LETTERA
                .set(
                    REEA_T_SOGGETTO.COGNOME_HASH_1CHAR,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getCognome() != null && !dtos.getCognome().isBlank()
                                ? dtos.getCognome().toUpperCase().substring(0, 1)
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )
                .set(
                    REEA_T_SOGGETTO.NOME_HASH_1CHAR,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getNome() != null && !dtos.getNome().isBlank()
                                ? dtos.getNome().toUpperCase().substring(0, 1)
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )
                //FINE HASH 
                
                .where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(id.intValue()))
                .execute();

        if (updatedRows == 0) {
            return null;
        }

        return findById(id);
    }

    
    @Override
    public int insertStoricoSoggetto(DSLContext ctx, Long id, AnagraficaDTO dtos, Integer nuovaVersione, String utenteCreazione, String utenteModifica) {

        Integer aslId = null;

        if (dtos.getDomicilioAslId() != null) {
            aslId = ctx
                    .select(REEA_D_ASL.ASL_ID)
                    .from(REEA_D_ASL)
                    .where(REEA_D_ASL.ASL_ID.eq(dtos.getDomicilioAslId()))
                    .and(REEA_D_ASL.DATA_CANCELLAZIONE.isNull())
                    .fetchOneInto(Integer.class);

            if (aslId == null) {
                throw new IllegalStateException(
                        "ASL non trovata per codice: " + dtos.getDomicilioAslId()
                );
            }
        }

        // Parsing date
        LocalDate assistenzaFine = DateConversionUtils.stringToLocalDate(dtos.getAssistenzaAslFine());
        LocalDate dataDecesso    = DateConversionUtils.stringToLocalDate(dtos.getDataDecesso());
        LocalDateTime validitaInizio = ctx
                .select(REEA_T_SOGGETTO.DATA_MODIFICA)
                .from(REEA_T_SOGGETTO)
                .where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(id.intValue()))
                .fetchOneInto(LocalDateTime.class);
        
        

        return ctx.insertInto(REEA_S_SOGGETTO)
                // ── CHIAVI ──────────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.SOGGETTO_ID,             id.intValue())
                .set(REEA_S_SOGGETTO.CODICE_FISCALE,          dtos.getCodiceFiscale())
                .set(REEA_S_SOGGETTO.INSERIMENTO_TIPO_ID, dtos.getInserimentoTipoId())

                // ── ASL ──────────────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.DOMICILIO_ASL_ID,        dtos.getDomicilioAslId())
                .set(REEA_S_SOGGETTO.RESIDENZA_ASL_ID,        dtos.getResidenzaAslId())
                .set(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID,
                        dtos.getAssistenzaAslId() != null && !dtos.getAssistenzaAslId().isBlank()
                                ? Integer.valueOf(dtos.getAssistenzaAslId()) : null)

                // ── DATI CIFRATI ─────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.NOME_CIFRATO,
                        DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                DSL.val(dtos.getNome() != null ? dtos.getNome().toUpperCase() : null,
                                        SQLDataType.VARCHAR),
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                .set(REEA_S_SOGGETTO.COGNOME_CIFRATO,
                        DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                DSL.val(dtos.getCognome() != null ? dtos.getCognome().toUpperCase() : null,
                                        SQLDataType.VARCHAR),
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                .set(REEA_S_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO,
                        dtos.getDomicilioIndirizzo() != null && !dtos.getDomicilioIndirizzo().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getDomicilioIndirizzo(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : null)
     
                
             // HASH COMPLETI (PER ORA LI RICALCOLO POTREMMO POI MODIFICARE IL DTO)
                .set(
                    REEA_S_SOGGETTO.COGNOME_HASH,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getCognome() != null
                                ? dtos.getCognome().toUpperCase()
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )
                .set(
                    REEA_S_SOGGETTO.NOME_HASH,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getNome() != null
                                ? dtos.getNome().toUpperCase()
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )

                // HASH PRIME 2 LETTERE
                .set(
                    REEA_S_SOGGETTO.COGNOME_HASH_2CHAR,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getCognome() != null && dtos.getCognome().length() >= 2
                                ? dtos.getCognome().toUpperCase().substring(0, 2)
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )
                .set(
                    REEA_S_SOGGETTO.NOME_HASH_2CHAR,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getNome() != null && dtos.getNome().length() >= 2
                                ? dtos.getNome().toUpperCase().substring(0, 2)
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )

                // HASH PRIMA LETTERA
                .set(
                    REEA_S_SOGGETTO.COGNOME_HASH_1CHAR,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getCognome() != null && !dtos.getCognome().isBlank()
                                ? dtos.getCognome().toUpperCase().substring(0, 1)
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )
                .set(
                    REEA_S_SOGGETTO.NOME_HASH_1CHAR,
                    DSL.function(
                        "hmac",
                        SQLDataType.BLOB,
                        DSL.val(
                            dtos.getNome() != null && !dtos.getNome().isBlank()
                                ? dtos.getNome().toUpperCase().substring(0, 1)
                                : null,
                            SQLDataType.VARCHAR
                        ),
                        DSL.inline("Porcatrota2000!"),
                        DSL.inline("sha256")
                    )
                )
                //FINE HASH 
                
                // Telefono
                .set(REEA_S_SOGGETTO.TELEFONO_CIFRATO,
                        dtos.getTelefono() != null && !dtos.getTelefono().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getTelefono(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : DSL.val(null, SQLDataType.BLOB))

                // Email
                .set(REEA_S_SOGGETTO.EMAIL_CIFRATA,
                        dtos.getEmail() != null && !dtos.getEmail().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getEmail(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : DSL.val(null, SQLDataType.BLOB))

                // Tessera team
                .set(REEA_S_SOGGETTO.TESSERA_TEAM_CIFRATA,
                        dtos.getTesseraTeam() != null && !dtos.getTesseraTeam().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getTesseraTeam(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : DSL.val(null, SQLDataType.BLOB))

                // Id aura
                .set(REEA_S_SOGGETTO.ID_AURA_CIFRATO,
                        dtos.getIdAura() != null && !dtos.getIdAura().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getIdAura(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : DSL.val(null, SQLDataType.BLOB))
                
                // Email aura
                .set(REEA_S_SOGGETTO.EMAIL_AURA_CIFRATA,
                        dtos.getEmailAura() != null && !dtos.getEmailAura().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getEmailAura(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : DSL.val(null, SQLDataType.BLOB))
                
                // Telefono aura
                .set(REEA_S_SOGGETTO.TELEFONO_AURA_CIFRATO,
                        dtos.getTelefonoAura() != null && !dtos.getTelefonoAura().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                        DSL.val(dtos.getTelefonoAura(), SQLDataType.VARCHAR),
                                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : DSL.val(null, SQLDataType.BLOB))
                
                // Indirizzo aura
                .set(REEA_S_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO,
                        dtos.getResidenzaIndirizzo() != null && !dtos.getResidenzaIndirizzo().isBlank()
                                ? DSL.function("pgp_sym_encrypt", SQLDataType.BLOB,
                                DSL.val(dtos.getResidenzaIndirizzo(), SQLDataType.VARCHAR),
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                                : DSL.val(null, SQLDataType.BLOB))


                // ── ANAGRAFICA BASE ──────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.SESSO,                   dtos.getSesso())
                .set(REEA_S_SOGGETTO.NASCITA_DATA,            dtos.getNascitaData())

                // ── NASCITA ──────────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.NASCITA_COMUNE_COD,      dtos.getNascitaComuneCod())
                .set(REEA_S_SOGGETTO.NASCITA_COMUNE_DESC,     dtos.getNascitaComuneDesc())
                .set(REEA_S_SOGGETTO.NASCITA_PROVINCIA_COD,   dtos.getNascitaProvinciaCod())
                .set(REEA_S_SOGGETTO.NASCITA_PROVINCIA_DESC,  dtos.getNascitaProvinciaDesc())
                .set(REEA_S_SOGGETTO.NASCITA_STATO_COD,       dtos.getNascitaStatoCod())
                .set(REEA_S_SOGGETTO.NASCITA_STATO_DESC,      dtos.getNascitaStatoDesc())

                // ── CITTADINANZA ─────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.CITTADINANZA_STATO_COD,  dtos.getCittadinanzaStatoCod())
                .set(REEA_S_SOGGETTO.CITTADINANZA_STATO_DESC, dtos.getCittadinanzaStatoDesc())

                // ── DOMICILIO ────────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.DOMICILIO_COMUNE_COD,    dtos.getDomicilioComuneCod())
                .set(REEA_S_SOGGETTO.DOMICILIO_COMUNE_DESC,   dtos.getDomicilioComuneDesc())
                .set(REEA_S_SOGGETTO.DOMICILIO_PROVINCIA_COD, dtos.getDomicilioProvinciaCod())
                .set(REEA_S_SOGGETTO.DOMICILIO_PROVINCIA_DESC,dtos.getDomicilioProvinciaDesc())
                .set(REEA_S_SOGGETTO.DOMICILIO_STATO_COD,     dtos.getDomicilioStatoCod())
                .set(REEA_S_SOGGETTO.DOMICILIO_STATO_DESC,    dtos.getDomicilioStatoDesc())
                .set(REEA_S_SOGGETTO.DOMICILIO_CAP,           dtos.getDomicilioCap())
                .set(REEA_S_SOGGETTO.DOMICILIO_NUMERO_CIVICO, dtos.getDomicilioNumeroCivico())

                // ── RESIDENZA ────────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.RESIDENZA_COMUNE_COD,    dtos.getResidenzaComuneCod())
                .set(REEA_S_SOGGETTO.RESIDENZA_COMUNE_DESC,   dtos.getResidenzaComuneDesc())
                .set(REEA_S_SOGGETTO.RESIDENZA_PROVINCIA_COD, dtos.getResidenzaProvinciaCod())
                .set(REEA_S_SOGGETTO.RESIDENZA_PROVINCIA_DESC,dtos.getResidenzaProvinciaDesc())
                .set(REEA_S_SOGGETTO.RESIDENZA_STATO_COD,     dtos.getResidenzaStatoCod())
                .set(REEA_S_SOGGETTO.RESIDENZA_STATO_DESC,    dtos.getResidenzaStatoDesc())
                .set(REEA_S_SOGGETTO.RESIDENZA_CAP,           dtos.getResidenzaCap())
                .set(REEA_S_SOGGETTO.RESIDENZA_NUMERO_CIVICO, dtos.getResidenzaNumeroCivico())

                // ── ASSISTENZA ───────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.ASSISTENZA_ASL_FINE,
                        assistenzaFine != null ? assistenzaFine.atStartOfDay() : null)

                // ── DECESSO ──────────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.DATA_DECESSO,
                        dataDecesso != null ? dataDecesso.atStartOfDay() : null)

                // ── STATO / FONTE / VERSIONE ─────────────────────────────────────
                .set(REEA_S_SOGGETTO.FONTE_ID,                dtos.getFonteId())
                .set(REEA_S_SOGGETTO.SOGGETTO_STATO_ID,       dtos.getSoggettoStatoId())
                .set(REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE,     dtos.getSoggettoStatoNote())
                .set(REEA_S_SOGGETTO.VERSIONE_NUMERO,         nuovaVersione - 1)

                // ── AUDIT ────────────────────────────────────────────────────────
                .set(REEA_S_SOGGETTO.DATA_CREAZIONE,          DSL.currentLocalDateTime())
                .set(REEA_S_SOGGETTO.DATA_MODIFICA,           DSL.currentLocalDateTime())
                .set(REEA_S_SOGGETTO.UTENTE_CREAZIONE,        utenteCreazione)
                .set(REEA_S_SOGGETTO.UTENTE_MODIFICA,         utenteModifica)
                .set(REEA_S_SOGGETTO.VALIDITA_INIZIO, validitaInizio != null ? validitaInizio : LocalDateTime.now())

                .execute();
    }
    

//    @Override
//    public int insertStoricoSoggetto(DSLContext ctx, Long id, AnagraficaDTO dtos, Integer nuovaVersione) {
//
//        Integer aslId = null;
//
//        if (dtos.getDomicilioAslId()!= null) {
//        //if (dtos.getAssistenzaAslCod() != null) {
//            aslId = ctx
//                    .select(REEA_D_ASL.ASL_ID)
//                    .from(REEA_D_ASL)
//                    .where(REEA_D_ASL.ASL_ID.eq(dtos.getDomicilioAslId()))
//                    .and(REEA_D_ASL.DATA_CANCELLAZIONE.isNull())
//                    .fetchOneInto(Integer.class);
//
//            if (aslId == null) {
//                throw new IllegalStateException(
//                        "ASL non trovata per codice: " + dtos.getDomicilioAslId()
//                );
//            }
//        }
//
//        return  ctx.insertInto(REEA_S_SOGGETTO)
//           .set(REEA_S_SOGGETTO.SOGGETTO_ID, id.intValue())
//           .set(REEA_S_SOGGETTO.CODICE_FISCALE, dtos.getCodiceFiscale())
//           .set(REEA_S_SOGGETTO.RESIDENZA_ASL_ID, dtos.getResidenzaAslId())
//           .set(REEA_S_SOGGETTO.DOMICILIO_ASL_ID, dtos.getDomicilioAslId())
//           .set(REEA_S_SOGGETTO.NOME_CIFRATO,
//                DSL.function("pgp_sym_encrypt",
//                             SQLDataType.BLOB,
//                             DSL.val(dtos.getNome() != null
//                                         ? dtos.getNome().toUpperCase()
//                                         : null,
//                                     SQLDataType.VARCHAR),
//                             DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
//           .set(REEA_S_SOGGETTO.COGNOME_CIFRATO,
//                DSL.function("pgp_sym_encrypt",
//                             SQLDataType.BLOB,
//                             DSL.val(dtos.getCognome() != null
//                                         ? dtos.getCognome().toUpperCase()
//                                         : null,
//                                     SQLDataType.VARCHAR),
//                             DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
//           .set(REEA_S_SOGGETTO.NASCITA_DATA, dtos.getNascitaData())
//           .set(REEA_S_SOGGETTO.FONTE_ID, dtos.getFonteId())
//           .set(REEA_S_SOGGETTO.SOGGETTO_STATO_ID, dtos.getSoggettoStatoId())
//           .set(REEA_S_SOGGETTO.DATA_CREAZIONE, DSL.currentLocalDateTime())
//           .set(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID, aslId)
//           .set(REEA_S_SOGGETTO.VERSIONE_NUMERO, nuovaVersione - 1)
//           .set(REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE, dtos.getSoggettoStatoNote())
//
//                .execute();
//    }


    @Override
    public List<AnagraficaDTO> findByCForNomeOrCognomeOrDataNascita(String filtroCodiceFiscale, String filtroNome, String filtroCognome, LocalDate filtroNascitaData) {

    	// filtro CF
    	Condition condCodiceFiscale = noCondition();
    	if (filtroCodiceFiscale != null && !filtroCodiceFiscale.isEmpty()) {
    		condCodiceFiscale = condCodiceFiscale.and(
    				REEA_T_SOGGETTO.CODICE_FISCALE.in(filtroCodiceFiscale.toUpperCase()));
    	}

    	// filtro nome (decrypt + like)
    	Condition condNome = noCondition();
    	if (filtroNome != null && !filtroNome.isEmpty()) {
    		Field<String> nomeDecr = field(
    				"pgp_sym_decrypt({0}::bytea, {1})",
    				String.class,
    				REEA_T_SOGGETTO.NOME_CIFRATO,
    				val("16<odcc8!")
    				);
    		condNome = condNome.and(nomeDecr.like("%" + filtroNome.toUpperCase() + "%"));
    	}

    	// filtro cognome
    	Condition condCognome = noCondition();
    	if (filtroCognome != null && !filtroCognome.isEmpty()) {
    		Field<String> cognomeDecr = field(
    				"pgp_sym_decrypt({0}::bytea, {1})",
    				String.class,
    				REEA_T_SOGGETTO.COGNOME_CIFRATO,
    				val("16<odcc8!")
    				);
    		condCognome = condCognome.and(cognomeDecr.like("%" + filtroCognome.toUpperCase() + "%"));
    	}

    	// filtro data nascita
    	Condition condNascitaData = noCondition();
    	if (filtroNascitaData != null) {
    		condNascitaData = condNascitaData.and(
    				REEA_T_SOGGETTO.NASCITA_DATA.eq(filtroNascitaData));
    	}

    	var query = dsl.selectDistinct(
    			REEA_D_ASL.ASL_AZIENDA_DESC,
    			REEA_T_SOGGETTO.FONTE_ID,
    			REEA_D_FONTE.FONTE_DESC,
    			REEA_T_SOGGETTO.DATA_MODIFICA,
    			REEA_T_SOGGETTO.DATA_CREAZIONE,
    			REEA_T_SOGGETTO.NASCITA_DATA,
    			REEA_T_SOGGETTO.SOGGETTO_ID,
    			REEA_T_SOGGETTO.VERSIONE_NUMERO,
    			DSL.when(
    				    REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
    				    DSL.field(
    				        "pgp_sym_decrypt({0}, {1})",
    				        String.class,
    				        REEA_T_SOGGETTO.NOME_CIFRATO,
    				        DSL.val("16<odcc8!", SQLDataType.VARCHAR)
    				    )
    				).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("NOME_CHIARO"),
    			DSL.when(
    				    REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
    				    DSL.field(
    				        "pgp_sym_decrypt({0}, {1})",
    				        String.class,
    				        REEA_T_SOGGETTO.COGNOME_CIFRATO,
    				        DSL.val("16<odcc8!", SQLDataType.VARCHAR)
    				    )
    				).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("COGNOME_CHIARO"),
    			REEA_T_SOGGETTO.CODICE_FISCALE,
    			REEA_T_SOGGETTO.DOMICILIO_ASL_ID,
    			REEA_T_SOGGETTO.RESIDENZA_ASL_ID,
    			REEA_T_SOGGETTO.ASSISTENZA_ASL_ID,
    			REEA_T_REGISTRO.SEZIONE,
    			REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA,
    			REEA_T_REGISTRO.TIPO_ELENCO_INAIL,
    			REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
    			)
    			.from(REEA_T_SOGGETTO)
    			.join(REEA_D_FONTE)
    			.on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
    			.join(REEA_D_ASL)
    			.on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.DOMICILIO_ASL_ID))
    			.join(REEA_T_REGISTRO)
    			.on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
    					field("reea.hmac_soggetto_id({0}, {1})",
    							String.class,
    							REEA_T_SOGGETTO.SOGGETTO_ID,
    							val("16<odcc8!"))))
    			.join(REEA_D_SOGGETTO_STATO)
    			.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
    			.where(condCodiceFiscale
    					.and(condNome)
    					.and(condCognome)
    					.and(condNascitaData));

    	//        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    	return query.stream()
    			.map(r -> {
    				AnagraficaDTO row = new AnagraficaDTO();

    				Long soggettoId = Long.valueOf(r.get(REEA_T_SOGGETTO.SOGGETTO_ID));

    				row.setSoggettoId(soggettoId);
    				row.setFonteId(r.get(REEA_T_SOGGETTO.FONTE_ID));
    				row.setDescrizioneFonte(List.of(
    						r.get(REEA_D_FONTE.FONTE_DESC) != null
    						? r.get(REEA_D_FONTE.FONTE_DESC)
    								: "PROVA"));

    				RegistroDTO registro = RegistroUtils.ensureRegistro(row);

    				registro.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_SOGGETTO.DATA_MODIFICA)));
    				registro.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_SOGGETTO.DATA_CREAZIONE)));

    				row.setNascitaData(r.get(REEA_T_SOGGETTO.NASCITA_DATA));
    				row.setNome(r.get("NOME_CHIARO").toString().toUpperCase());
    				row.setCognome(r.get("COGNOME_CHIARO").toString().toUpperCase());
    				row.setCodiceFiscale(r.get(REEA_T_SOGGETTO.CODICE_FISCALE));
    				row.setResidenzaAslId(r.get(REEA_T_SOGGETTO.RESIDENZA_ASL_ID));
    				row.setDomicilioAslId(r.get(REEA_T_SOGGETTO.DOMICILIO_ASL_ID));
    				row.setDescrizioneAslCompetenza(r.get(REEA_D_ASL.ASL_AZIENDA_DESC));

    				Integer assistenzaId = r.get(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID);
    				row.setAssistenzaAslId(assistenzaId != null ? String.format("%06d", assistenzaId) : null);

    				row.setDescrizioneStato(r.get(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC));

    				registro.setVersioneNumero(r.get(REEA_T_SOGGETTO.VERSIONE_NUMERO));
    				registro.setSezione(r.get(REEA_T_REGISTRO.SEZIONE));
    				registro.setInseritoInSorveglianza(r.get(REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA));
    				registro.setTipoElencoInail(r.get(REEA_T_REGISTRO.TIPO_ELENCO_INAIL));

    				row.setListaFonteByIdSoggetto(fonteRepository.findListaFonteDescByIdSoggetto(soggettoId));

    				return row;
    			})
    			.collect(Collectors.toList());
    }


    @Override
    public Integer inserisciEsenzioni(DSLContext ctx, Integer soggettoId, AnagraficaDTO dtos, Integer elaborazioneId, String utente, Integer tipoOperazione) {

    	Integer risultato = 0;

    	if (dtos.getListaEsenzione() == null) {
    		return risultato;
    	}
    	try {
    		for (EsenzioneDTO esenzione : dtos.getListaEsenzione()) {
    			LocalDateTime validitaInizio = null;
    			if (esenzione.getValiditaInizio() != null && !esenzione.getValiditaInizio().isBlank()) {
    				LocalDate ld1 = LocalDate.parse(esenzione.getValiditaInizio()); // "yyyy-MM-dd"
    				validitaInizio = ld1.atStartOfDay(); // 00:00:00
    			}
    			LocalDateTime validitaFine = null;
    			if (esenzione.getValiditaFine() != null && !esenzione.getValiditaFine().isBlank()) {
    				LocalDate ld2 = LocalDate.parse(esenzione.getValiditaFine()); // "yyyy-MM-dd"
    				validitaFine = ld2.atStartOfDay(); // 00:00:00
    			}
    			LocalDateTime dataModifica = null;
    			if (esenzione.getDataModifica() != null && !esenzione.getDataModifica().isBlank()) {
    				LocalDate ld3 = LocalDate.parse(esenzione.getDataModifica()); // "yyyy-MM-dd"
    				dataModifica = ld3.atStartOfDay(); // 00:00:00
    			}
    			LocalDateTime dataCancellazione = null;
    			if (esenzione.getDataCancellazione() != null && !esenzione.getDataCancellazione().isBlank()) {
    				LocalDate ld4 = LocalDate.parse(esenzione.getDataCancellazione()); // "yyyy-MM-dd"
    				dataCancellazione = ld4.atStartOfDay(); // 00:00:00
    			}

    			risultato = ctx.insertInto(REEA_R_SOGGETTO_ESENZIONE)
    					.set(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC,
    							DSL.field("reea.hmac_soggetto_id({0}, {1})",
    									String.class,
    									DSL.val(soggettoId),
    									DSL.val("16<odcc8!")))
    					.set(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_CIFRATO,
    							DSL.function("pgp_sym_encrypt_bytea",
    									byte[].class,
    									DSL.val(soggettoId.toString()).cast(SQLDataType.BLOB),
    									DSL.val("16<odcc8!")))

    					.set(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_HMAC,
    							DSL.field(
    									"reea.text_to_hmac(cast({0} as text), cast({1} as text))",
    									String.class,
    									DSL.val(esenzione.getEsenzioneId(), String.class),
    									DSL.val("16<odcc8!")))
    					.set(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_CIFRATO,
    							DSL.function("pgp_sym_encrypt_bytea",
    									byte[].class,
    									DSL.val(esenzione.getEsenzioneId()).cast(SQLDataType.BLOB),
    									DSL.val("16<odcc8!")))
    					.set(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_EMISSIONE, esenzione.getEsenzioneDataEmissione())
    					.set(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_SCADENZA, esenzione.getEsenzioneDataScadenza())
    					.set(REEA_R_SOGGETTO_ESENZIONE.VALIDITA_INIZIO, validitaInizio)
    					.set(REEA_R_SOGGETTO_ESENZIONE.VALIDITA_FINE, validitaFine)
    					.set(REEA_R_SOGGETTO_ESENZIONE.DATA_MODIFICA, dataModifica)
    					.set(REEA_R_SOGGETTO_ESENZIONE.DATA_CANCELLAZIONE, dataCancellazione)
    					.set(REEA_R_SOGGETTO_ESENZIONE.UTENTE_CREAZIONE, dtos.getUtenteCreazione())
    					.set(REEA_R_SOGGETTO_ESENZIONE.UTENTE_MODIFICA, dtos.getUtenteCreazione())
    					.returning(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ESENZIONE_ID)
    					.fetchOne()
    					.getValue(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ESENZIONE_ID, Integer.class);
    		}

    		if(dtos.getAdesioneId() != null)
    			tracciaElaborazioneRepository.inserisciFileImpatto(
    					ctx,
    					elaborazioneId,
    					"REEA_R_SOGGETTO_ESENZIONE",
    					dtos.getAdesioneId().intValue(),
    					tipoOperazione,
    					utente
    					);

    		return risultato;

    	} catch (Exception e) {

    		try {
    			tracciaElaborazioneService.inserisciErroreRiga(
//    					ctx,
    					elaborazioneId,
    					"ERR_INSERIMENTO",
    					"inserisciEsenzioni ha restituito " + risultato + " per CF " + dtos.getCodiceFiscale(),
    					null,
    					dtos.getUtenteCreazione(),
    					dtos.getCognome(),
    					dtos.getNome(),
    					dtos.getNascitaData(),
    					dtos.getSesso(),
    					dtos.getCodiceFiscale(),
    					"REEA_R_SOGGETTO_ESENZIONE",
    					dtos.getAdesioneId().intValue()
    					);
    		} catch (Exception exLog) {
    			System.err.println("Errore nel log scarto: " + exLog.getMessage());
    		}

    		// Se il metodo fa parte di una transazione e vuoi rollback, rilancia
    		throw e;
    		// In alternativa, se NON vuoi far propagare l’errore:
    		// return null;
    	}
    }


    @Override
    public Integer findRegistroIdBySoggettoId(Integer soggettoId) {
        return dsl.select(REEA_T_REGISTRO.REGISTRO_ID)
                .from(REEA_T_REGISTRO)
                .where(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                        DSL.field("reea.hmac_soggetto_id({0}, {1})",
                                String.class,
                                DSL.val(soggettoId),
                                DSL.val("16<odcc8!"))))
                .fetchOneInto(Integer.class);
    }


    @Override
    public void aggiornaStato(Long soggettoId, AnagraficaDTO dto, String cfOperatore) {

    	AnagraficaDTO current = findById(soggettoId);
    	if (current == null) {
    		throw new RuntimeException("Soggetto non trovato: " + soggettoId);
    	}

    	RegistroDTO registroCurrent = RegistroUtils.ensureRegistro(current);

    	Integer nuovaVersione = (registroCurrent.getVersioneNumero() != null ? registroCurrent.getVersioneNumero() : 1) + 1;
    	insertStoricoSoggetto(dsl, soggettoId, current, nuovaVersione, cfOperatore, cfOperatore);

    	dsl.update(REEA_T_SOGGETTO)
    	.set(REEA_T_SOGGETTO.SOGGETTO_STATO_ID,   dto.getSoggettoStatoId())
    	.set(REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE,
    			(dto.getSoggettoStatoNote() != null && !dto.getSoggettoStatoNote().isBlank())
    			? dto.getSoggettoStatoNote() : null)
    	.set(REEA_T_SOGGETTO.VERSIONE_NUMERO,     nuovaVersione)
    	.set(REEA_T_SOGGETTO.DATA_MODIFICA,       DSL.currentLocalDateTime())
    	.set(REEA_T_SOGGETTO.UTENTE_MODIFICA,     cfOperatore)
    	.where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
    	.execute();
    }
    

    @Override
    public List<StoriaStatoDTO> findStoriaStati(Integer soggettoId) {

        // 1. Tutto lo storico ordinato per data
        List<StoriaStatoDTO> tuttoStorico = dsl
                .select(
                        REEA_S_SOGGETTO.SOGGETTO_STATO_ID,
                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC,
                        REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE,
                        REEA_S_SOGGETTO.DATA_CREAZIONE,
                        REEA_S_SOGGETTO.UTENTE_MODIFICA
                )
                .from(REEA_S_SOGGETTO)
                .join(REEA_D_SOGGETTO_STATO)
                .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_S_SOGGETTO.SOGGETTO_STATO_ID))
                .where(REEA_S_SOGGETTO.SOGGETTO_ID.eq(soggettoId))
                .orderBy(REEA_S_SOGGETTO.DATA_CREAZIONE.asc())
                .fetch(r -> {
                    StoriaStatoDTO dto = new StoriaStatoDTO();
                    dto.setSoggettoStatoId(
                            r.get(REEA_S_SOGGETTO.SOGGETTO_STATO_ID) != null
                                    ? String.valueOf(r.get(REEA_S_SOGGETTO.SOGGETTO_STATO_ID))
                                    : null
                    );
                    dto.setSoggettoStatoDesc(r.get(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC));
                    dto.setSoggettoStatoNote(r.get(REEA_S_SOGGETTO.SOGGETTO_STATO_NOTE));
                    LocalDateTime data = r.get(REEA_S_SOGGETTO.DATA_CREAZIONE);
                    dto.setDataModifica(data != null ? data.atOffset(java.time.ZoneOffset.UTC) : null);
                    dto.setUtenteModifica(r.get(REEA_S_SOGGETTO.UTENTE_MODIFICA));
                    return dto;
                });

        // 2. Filtra: tieni solo i record dove stato o nota sono cambiati rispetto al precedente
        List<StoriaStatoDTO> storiciCambiati = new ArrayList<>();
        StoriaStatoDTO precedente = null;
        for (StoriaStatoDTO current : tuttoStorico) {
            if (precedente == null
                    || !Objects.equals(current.getSoggettoStatoId(), precedente.getSoggettoStatoId())
                    || !Objects.equals(current.getSoggettoStatoNote(), precedente.getSoggettoStatoNote())) {
                storiciCambiati.add(current);
            }
            precedente = current;
        }

        // 3. Recupera stato corrente da T_SOGGETTO per confronto
        record StatoCorrente(String statoId, String nota) {}
        StatoCorrente corrente = dsl
                .select(REEA_T_SOGGETTO.SOGGETTO_STATO_ID, REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE)
                .from(REEA_T_SOGGETTO)
                .where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId))
                .fetchOne(r -> new StatoCorrente(
                        r.get(REEA_T_SOGGETTO.SOGGETTO_STATO_ID) != null
                                ? String.valueOf(r.get(REEA_T_SOGGETTO.SOGGETTO_STATO_ID)) : null,
                        r.get(REEA_T_SOGGETTO.SOGGETTO_STATO_NOTE)
                ));

        // 4. Rimuove l'ultimo storico se uguale al corrente
        //    (il frontend aggiunge giÃ  il corrente in cima con il pallino blu)
        if (corrente != null && !storiciCambiati.isEmpty()) {
            StoriaStatoDTO ultimo = storiciCambiati.get(storiciCambiati.size() - 1);
            if (Objects.equals(ultimo.getSoggettoStatoId(), corrente.statoId())
                    && Objects.equals(ultimo.getSoggettoStatoNote(), corrente.nota())) {
                storiciCambiati.remove(storiciCambiati.size() - 1);
            }
        }

        return storiciCambiati;
    }
    
    
    @Override
    public StoriaStatoDTO findStatoCorrente(Integer soggettoId) {
        Record2<Integer, String> record = dsl
                .select(REEA_T_SOGGETTO.SOGGETTO_STATO_ID, REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC)
                .from(REEA_T_SOGGETTO)
                .join(REEA_D_SOGGETTO_STATO)
                .on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
                .where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId))
                .fetchOne();

        if (record == null) {
            return null;
        }

        StoriaStatoDTO dto = new StoriaStatoDTO();
        dto.setSoggettoStatoId(String.valueOf(record.value1()));
        dto.setSoggettoStatoDesc(record.value2());
        return dto;
    }
    
    
  @Override
  public List<ExportDTO> recuperaTotaleRecordPerExportAsincronoStatiPrecedenti(Integer assistenzaAslId, String profiloUtente) {

      Condition condition = DSL.noCondition();

      if (assistenzaAslId != null) {
          condition = condition.and(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.eq(assistenzaAslId));
      }
//
//      if (soggettoId != null) {
//          condition = condition.and(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId));
//      }

      Field<String> preNomeChiaro = DSL.when(
              REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                      REEA_T_ADESIONE.NOME_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneNome");

      Field<String> preCognomeChiaro = DSL.when(
              REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                      REEA_T_ADESIONE.COGNOME_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneCognome");

      Field<String> preTesseraTeamChiara = DSL.when(
              REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                      REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneTesseraTeam");

      Field<String> preIdAuraChiaro = DSL.when(
              REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                      REEA_T_ADESIONE.ID_AURA_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneIdAura");

      Field<String> preEmailChiara = DSL.when(
              REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                      REEA_T_ADESIONE.EMAIL_CIFRATA,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneEmail");

      Field<String> preTelefonoChiaro = DSL.when(
              REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                      REEA_T_ADESIONE.TELEFONO_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneTelefono");

      Field<String> auraNomeChiaro = DSL.when(
    		  REEA_S_SOGGETTO.NOME_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.NOME_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraNome");

      Field<String> auraCognomeChiaro = DSL.when(
    		  REEA_S_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.COGNOME_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraCognome");

      Field<String> auraDomicilioIndirizzoChiaro = DSL.when(
    		  REEA_S_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraDomicilioIndirizzo");

      Field<String> auraResidenzaIndirizzoChiaro = DSL.when(
    		  REEA_S_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraResidenzaIndirizzo");

      Field<String> auraTesseraTeamChiara = DSL.when(
    		  REEA_S_SOGGETTO.TESSERA_TEAM_CIFRATA.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.TESSERA_TEAM_CIFRATA,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraTesseraTeam");

      Field<String> auraIdAuraChiaro = DSL.when(
    		  REEA_S_SOGGETTO.ID_AURA_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.ID_AURA_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraIdAura");

      Field<String> auraEmailChiara = DSL.when(
    		  REEA_S_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.EMAIL_AURA_CIFRATA,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraEmail");

      Field<String> auraTelefonoChiaro = DSL.when(
    		  REEA_S_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.TELEFONO_AURA_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraTelefono");

      Field<String> emailChiara = DSL.when(
    		  REEA_S_SOGGETTO.EMAIL_CIFRATA.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.EMAIL_CIFRATA,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("email");

      Field<String> telefonoChiaro = DSL.when(
    		  REEA_S_SOGGETTO.TELEFONO_CIFRATO.isNotNull(),
              DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
            		  REEA_S_SOGGETTO.TELEFONO_CIFRATO,
                      DSL.val("16<odcc8!", SQLDataType.VARCHAR))
      ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("telefono");

      Field<String> soggettoIdHmac = DSL.field(
              "reea.hmac_soggetto_id(cast({0} as int4), cast({1} as text))",
              String.class,
              REEA_S_SOGGETTO.SOGGETTO_ID,
              DSL.val("16<odcc8!")
      );

      List<ExportDTO> risultati = dsl
                      .selectDistinct(
                              //PRE (da REEA_T_ADESIONE)
                    		  REEA_S_SOGGETTO.SOGGETTO_ID.as("soggettoId"),
                              REEA_T_ADESIONE.ADESIONE_COD.as("adesioneCodiceAdesione"),
                              REEA_T_ADESIONE.ADESIONE_DATA.as("adesioneDataAdesione"),
                              REEA_T_ADESIONE.CODICE_FISCALE.as("adesioneCodiceFiscale"),
                              REEA_T_ADESIONE.ADESIONE_COD.as("codiceAdesione"),
                              REEA_T_ADESIONE.ADESIONE_DATA.as("dataAdesione"),
                              REEA_T_ADESIONE.CODICE_FISCALE.as("codiceFiscale"),
                              preCognomeChiaro,
                              preNomeChiaro,
                              REEA_T_ADESIONE.NASCITA_DATA.as("adesioneDataDiNascita"),
                              REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC.as("adesioneProvinciaDiNascita"),
                              REEA_T_ADESIONE.NASCITA_COMUNE_DESC.as("adesioneComuneDiNascita"),
                              preTesseraTeamChiara,
                              preIdAuraChiaro,
                              REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC.as("adesioneProvinciaDiDomicilio"),
                              REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC.as("adesioneComuneDiDomicilio"),
                              REEA_T_ADESIONE.DOMICILIO_COMUNE_COD.as("adesioneCodiceComuneIstatDiDomicilio"),
                              REEA_T_ADESIONE.DOMICILIO_CAP.as("adesioneCapDiDomicilio"),
                              preEmailChiara,
                              preTelefonoChiaro,
                              REEA_T_ADESIONE.DOMICILIO_ASL_COD.as("adesioneCodiceAslDiDomicilio"),
                              REEA_T_ADESIONE.DOMICILIO_ASL_DESC.as("adesioneAslDiDomicilio"),
                              REEA_T_ADESIONE.AZIENDA_COD.as("adesioneCodazi"),
                              REEA_T_ADESIONE.RESIDENZA_ASL_COD.as("adesioneCodiceAslDiResidenza"),
                              REEA_T_ADESIONE.RESIDENZA_ASL_DESC.as("adesioneAslDiResidenza"),
                              REEA_T_ADESIONE.ESPOSIZIONE_INIZIO.as("adesioneDataInizioEsposizione"),
                              REEA_T_ADESIONE.ESPOSIZIONE_FINE.as("adesioneDataFineEsposizione"),
                              REEA_T_ADESIONE.AZIENDA_DESC.as("adesioneAzienda"),
                              REEA_T_ADESIONE.AZIENDA_COMUNE_COD.as("adesioneCodiceComuneIstatAzienda"),
                              REEA_T_ADESIONE.AZIENDA_COMUNE_DESC.as("adesioneComuneAzienda"),
                              REEA_T_ADESIONE.AZIENDA_CAP.as("adesioneCapAzienda"),
                              REEA_T_ADESIONE.AZIENDA_PROVINCIA.as("adesioneProvinciaAzienda"),
                              REEA_T_ADESIONE.MANSIONE.as("adesioneMansione"),
                              

                              //AURA (da REEA_T_SOGGETTO)
                              REEA_S_SOGGETTO.CODICE_FISCALE.as("auraCodiceFiscale"),
                              REEA_S_SOGGETTO.DOMICILIO_ASL_ID.as("auraDomicilioAsl"),
                              REEA_S_SOGGETTO.RESIDENZA_ASL_ID.as("auraResidenzaAsl"),
                              REEA_S_SOGGETTO.ASSISTENZA_ASL_ID.as("auraAssistenzaAsl"),
                              auraNomeChiaro,
                              auraCognomeChiaro,
                              REEA_S_SOGGETTO.SESSO.as("auraSesso"),
                              REEA_S_SOGGETTO.NASCITA_DATA.as("auraNascitaData"),
                              REEA_S_SOGGETTO.NASCITA_COMUNE_COD.as("auraNascitaComuneCod"),
                              REEA_S_SOGGETTO.NASCITA_COMUNE_DESC.as("auraNascitaComuneDesc"),
                              REEA_S_SOGGETTO.NASCITA_PROVINCIA_COD.as("auraNascitaProvinciaCod"),
                              REEA_S_SOGGETTO.NASCITA_PROVINCIA_DESC.as("auraNascitaProvinciaDesc"),
                              REEA_S_SOGGETTO.NASCITA_STATO_COD.as("auraNascitaStatoCod"),
                              REEA_S_SOGGETTO.NASCITA_STATO_DESC.as("auraNascitaStatoDesc"),
                              REEA_S_SOGGETTO.CITTADINANZA_STATO_COD.as("auraCittadinanzaStatoCod"),
                              REEA_S_SOGGETTO.CITTADINANZA_STATO_DESC.as("auraCittadinanzaStatoDesc"),
                              REEA_S_SOGGETTO.DOMICILIO_COMUNE_COD.as("auraDomicilioComuneCod"),
                              REEA_S_SOGGETTO.DOMICILIO_COMUNE_DESC.as("auraDomicilioComuneDesc"),
                              REEA_S_SOGGETTO.DOMICILIO_PROVINCIA_COD.as("auraDomicilioProvinciaCod"),
                              REEA_S_SOGGETTO.DOMICILIO_PROVINCIA_DESC.as("auraDomicilioProvinciaDesc"),
                              REEA_S_SOGGETTO.DOMICILIO_STATO_COD.as("auraDomicilioStatoCod"),
                              REEA_S_SOGGETTO.DOMICILIO_STATO_DESC.as("auraDomicilioStatoDesc"),
                              REEA_S_SOGGETTO.DOMICILIO_CAP.as("auraDomicilioCap"),
                              auraDomicilioIndirizzoChiaro,
                              REEA_S_SOGGETTO.DOMICILIO_NUMERO_CIVICO.as("auraDomicilioNumeroCivico"),
                              REEA_S_SOGGETTO.RESIDENZA_COMUNE_COD.as("auraResidenzaComuneCod"),
                              REEA_S_SOGGETTO.RESIDENZA_COMUNE_DESC.as("auraResidenzaComuneDesc"),
                              REEA_S_SOGGETTO.RESIDENZA_PROVINCIA_COD.as("auraResidenzaProvinciaCod"),
                              REEA_S_SOGGETTO.RESIDENZA_PROVINCIA_DESC.as("auraResidenzaProvinciaDesc"),
                              REEA_S_SOGGETTO.RESIDENZA_STATO_COD.as("auraResidenzaStatoCod"),
                              REEA_S_SOGGETTO.RESIDENZA_STATO_DESC.as("auraResidenzaStatoDesc"),
                              REEA_S_SOGGETTO.RESIDENZA_CAP.as("auraResidenzaCap"),
                              auraResidenzaIndirizzoChiaro,
                              REEA_S_SOGGETTO.RESIDENZA_NUMERO_CIVICO.as("auraResidenzaNumeroCivico"),
                              auraTesseraTeamChiara,
                              auraIdAuraChiaro,
                              auraEmailChiara,
                              auraTelefonoChiaro,
                              emailChiara,
                              telefonoChiaro,
                              REEA_S_SOGGETTO.DATA_DECESSO.as("auraDataDecesso"),
                              REEA_S_SOGGETTO.ASSISTENZA_ASL_FINE.as("auraAssistenzaAslFine"),

                              // ESENZIONI
                              REEA_D_ESENZIONE.ESENZIONE_COD.as("codEsenzione"),
                              REEA_D_ESENZIONE.ESENZIONE_DESC.as("descEsenzione"),
                              REEA_D_ESENZIONE.DIAGNOSI_COD.as("codDiagnosi"),
                              REEA_D_ESENZIONE.DIAGNOSI_DESC.as("descDiagnosi"),
                              REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_EMISSIONE.as("dataEmissione"),
                              REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_SCADENZA.as("dataScadenza"),
                              
                              //ESPOSIZIONI
                              REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA.as("adesioneEsposizioneAzienda"),
                              REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD.as("adesioneEsposizioneAziendaComuneCod"),
                              REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC.as("adesioneEsposizioneAziendaComuneDesc"),
                              REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP.as("adesioneEsposizioneAziendaCap"),
                              REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO.as("adesioneEsposizioneDataInizioEsposizione"),
                              REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE.as("adesioneEsposizioneDataFineEsposizione"),
                              REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA.as("adesioneEsposizioneProvinciaAzienda"),
                              REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE.as("adesioneEsposizioneMansione"),

                              //ANAMNESI (da REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.as("anamnesiCodiceFiscale"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA.as("anamnesiIdAura"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA.as("anamnesiDataIntervista"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE.as("anamnesiNominativoIntervistatore"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE.as("anamnesiFumatore"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE.as("anamnesiSigarette"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI.as("anamnesiSigaretteAnni"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO.as("anamnesiSigaretteEtaInizio"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE.as("anamnesiSigaretteFumaAttualmente"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE.as("anamnesiSigaretteEtaFine"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE.as("anamnesiSigaretteDie"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI.as("anamnesiSigari"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI.as("anamnesiSigariAnni"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO.as("anamnesiSigariEtaInizio"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE.as("anamnesiSigariFumaAttualmente"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE.as("anamnesiSigariEtaFine"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE.as("anamnesiSigariDie"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA.as("anamnesiPipa"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI.as("anamnesiPipaAnni"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO.as("anamnesiPipaEtaInizio"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE.as("anamnesiPipaFumaAttualmente"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE.as("anamnesiPipaEtaFine"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE.as("anamnesiPipaDie"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM.as("anamnesiOccupazioneNum"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO.as("anamnesiOccupazioneAnnoInizio"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE.as("anamnesiOccupazioneAnnoFine"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO.as("anamnesiOccupazioneTipo"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO.as("anamnesiOccupazioneDescrizioneLavoro"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA.as("anamnesiOccupazioneNomeEIndirizzoDitta"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA.as("anamnesiOccupazioneAttivitaDitta"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO.as("anamnesiNotaAttivitaConAmianto"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO.as("anamnesiAnamnesiEsposizioneAmianto"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE.as("anamnesiEsposizioneProfessionale"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE.as("anamnesiAnnoFineEsposizione"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE.as("anamnesiLivelloEsposizione"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA.as("anamnesiInserimentoInSorveglianza"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL.as("anamnesiIdSpresal"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.COUNSELING.as("anamnesiCounseling"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT.as("anamnesiOccupazioneEsposizioneCrpt"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT.as("anamnesiOccupazioneSettoreDittaCrpt"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT.as("anamnesiOccupazioneMansioneCrpt"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT.as("anamnesiOccupazioneRagioneSocialeDittaCrpt"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT.as("anamnesiOccupazionePivaDittaCrpt"),
                              REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_CODICE_FISCALE_DITTA_CRPT.as("anamnesiOccupazioneCodiceFiscaleDittaCrpt"),

                              //ESITI VISITA (da REEA_T_REGISTRO_SPRESAL_ESITI)
                              REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE.as("esitiVisCodiceFiscale"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA.as("esitiVisIdAura"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.as("esitiVisDataVisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.VISITA.as("esitiVisVisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA.as("esitiVisLivelloVisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO.as("esitiVisRiceveIndennizzo"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO.as("esitiVisMalattiaIndennizzo"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX.as("esitiVisAccertamentiRx"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA.as("esitiVisAccertamentiRxData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE.as("esitiVisAccertamentiRxRefertoNormale"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA.as("esitiVisAccertamentiRxAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC.as("esitiVisAccertamentiTc"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA.as("esitiVisAccertamentiTcData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE.as("esitiVisAccertamentiTcRefertoNormale"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA.as("esitiVisAccertamentiTcAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE.as("esitiVisAccertamentiSpirometriaSemplice"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA.as("esitiVisAccertamentiSpirometriaSempliceData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaSempliceRefertoNormale"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA.as("esitiVisAccertamentiSpirometriaSempliceAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE.as("esitiVisAccertamentiSpirometriaGlobale"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA.as("esitiVisAccertamentiSpirometriaGlobaleData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaGlobaleRefertoNormale"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA.as("esitiVisAccertamentiSpirometriaGlobaleAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO.as("esitiVisAccertamentiDlco"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA.as("esitiVisAccertamentiDlcoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE.as("esitiVisAccertamentiDlcoRefertoNormale"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA.as("esitiVisAccertamentiDlcoAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET.as("esitiVisAccertamentiPet"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA.as("esitiVisAccertamentiPetData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE.as("esitiVisAccertamentiPetRefertoNormale"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA.as("esitiVisAccertamentiPetAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA.as("esitiVisAccertamentiVisitaPneumologica"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA.as("esitiVisAccertamentiVisitaPneumologicaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaPneumologicaReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaPneumologicaAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA.as("esitiVisAccertamentiVisitaRadiologica"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA.as("esitiVisAccertamentiVisitaRadiologicaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaRadiologicaReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaRadiologicaAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA.as("esitiVisAccertamentiVisitaOncologica"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA.as("esitiVisAccertamentiVisitaOncologicaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaOncologicaReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaOncologicaAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO.as("esitiVisAccertamentiAltro"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE.as("esitiVisAccertamentiAltroDescrizione"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA.as("esitiVisAccertamentiAltroData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE.as("esitiVisAccertamentiAltroRefertoNormale"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA.as("esitiVisAccertamentiAltroRefertoAcquisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO.as("esitiVisRisultatoNegativo"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI.as("esitiVisPpmPlacchepleUrdicheMonolaterali"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisPpmPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisPpmPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisPpmAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisPpmAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisPpmPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO.as("esitiVisPpmReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA.as("esitiVisPpmRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI.as("esitiVisPpbPlacchepleUricheBilaterali"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisPpbPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisPpbPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisPpbAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisPpbAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisPpbPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO.as("esitiVisPpbReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA.as("esitiVisPpbRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE.as("esitiVisApAsbestosiPolmonare"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisApPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisApPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisApAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisApAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisApPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO.as("esitiVisApReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA.as("esitiVisApRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA.as("esitiVisFpdFibrosiPleuricaDiffusa"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisFpdPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisFpdPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisFpdAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisFpdAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisFpdPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO.as("esitiVisFpdReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA.as("esitiVisFpdRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO.as("esitiVisMpMesoteliomapleurico"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisMpPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisMpPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisMpAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisMpAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisMpPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO.as("esitiVisMpReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA.as("esitiVisMpRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR.as("esitiVisMpComunicazioneAlCor"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA.as("esitiVisMpComunicazioneAlCorData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA.as("esitiVisAmAltroMesotelioma"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisAmPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisAmPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisAmAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisAmAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisAmPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO.as("esitiVisAmReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA.as("esitiVisAmRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR.as("esitiVisAmComunicazioneAlCor"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA.as("esitiVisAmComunicazioneAlCorData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE.as("esitiVisNlNeoplasiaLaringe"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisNlPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisNlPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisNlAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisNlAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisNlPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO.as("esitiVisNlReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA.as("esitiVisNlRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA.as("esitiVisNoNeoplasiaOvarica"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisNoPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisNoPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisNoAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisNoAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisNoPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO.as("esitiVisNoReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA.as("esitiVisNoRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE.as("esitiVisTpTumoreDelPolmone"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisTpPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisTpPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisTpAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisTpAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisTpPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO.as("esitiVisTpReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA.as("esitiVisTpRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR.as("esitiVisTpComunicazioneAlCor"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA.as("esitiVisTpComunicazioneAlCorData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE.as("esitiVisBpcoEnfisemaPolmonare"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisBpcoPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisBpcoPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisBpcoAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisBpcoAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisBpcoPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO.as("esitiVisBpcoReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA.as("esitiVisBpcoRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI.as("esitiVisAltraDiagnosi"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE.as("esitiVisAltraDiagnosiDescrizione"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisAltraPrimoCertificatoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisAltraPrimoCertificatoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisAltraAggravamentoEDenuncia"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisAltraAggravamentoEDenunciaData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisAltraPercentualeDiRiconoscimento"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO.as("esitiVisAltraReferto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA.as("esitiVisAltraRefertoData"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO.as("esitiVisFollowUpPrevisto"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA.as("esitiVisAnnoPresuntoProssimaVisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA.as("esitiVisAnnoUltimaVisita"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG.as("esitiVisInvioSintesiAMmg"),
                              REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL.as("esitiVisIdSpresal"),
                              
                              REEA_T_REGISTRO.ATT_SANITARIA_STATO.as("regAttSanitariaStato"),
                              REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL.as("regAttSanitariaSpresal"),
                              REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA.as("regAttSanitariaSpresalData"),
                              REEA_T_REGISTRO.ATT_SANITARIA_INAIL.as("regAttSanitariaInail"),
                              REEA_T_REGISTRO.SEZIONE.as("regSezione"),
                              REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("regInseritoInSorveglianza"),
                              REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("regTipoElencoInail"),
                              
                              REEA_T_REGISTRO_INAIL.DOMANDA.as("inailDomanda"),
                              REEA_T_REGISTRO_INAIL.COGNOME.as("inailCognome"),
                              REEA_T_REGISTRO_INAIL.NOME.as("inailNome"),
                              REEA_T_REGISTRO_INAIL.CODICE_FISCALE.as("inailCodiceFiscale"),
                              REEA_T_REGISTRO_INAIL.SESSO.as("inailSesso"),
                              REEA_T_REGISTRO_INAIL.DATA_NASCITA.as("inailDataNascita"),
                              REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA.as("inailIndirizzoResidenza"),
                              REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA.as("inailIstatResidenza"),
                              REEA_T_REGISTRO_INAIL.CAP_RESIDENZA.as("inailCapResidenza"),
                              REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA.as("inailRegioneResidenza"),
                              REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA.as("inailProvinciaResidenza"),
                              REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA.as("inailComuneResidenza"),
                              
                              REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE.as("nplaCodiceFiscale"),
                              REEA_T_REGISTRO_PDL_AMIANTO.PERIODO.as("nplaPeriodo"),
                              REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE.as("nplaIdCantiere"),
                              REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA.as("nplaAziendaPiva"),
                              REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME.as("nplaAziendaNome"),
                              REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE.as("nplaAslCantiere"),
                              REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE.as("nplaComuneCantiere"),
                              REEA_T_REGISTRO_PDL_AMIANTO.ANNO.as("nplaAnno"),
                              REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO.as("nplaTipologiaPiano"),
                              REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE.as("nplaQuantitaDaRimuovere"),
                              REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA.as("nplaQuantitaRimossa"),
                              REEA_D_ASL.ASL_AZIENDA_DESC.as("aslAziendaDesc"),
                              REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("soggettoStatoDesc")
                      )
              .from(REEA_S_SOGGETTO)
              // MM Proposta per eliminare i record doppi --- Inizio ---
              //.join(REEA_T_ADESIONE)
              //    .on(REEA_S_SOGGETTO.SOGGETTO_ID.eq(REEA_T_ADESIONE.SOGGETTO_ID))
              // MM Proposta per eliminare i record doppi ---  Fine  ---
              .leftJoin(REEA_R_SOGGETTO_ESENZIONE)
                  .on(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
              .leftJoin(REEA_D_ESENZIONE)
                  .on(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_HMAC.eq(
                          DSL.field("reea.text_to_hmac(cast({0} as text), cast({1} as text))",
                                  String.class,
                                  REEA_D_ESENZIONE.ESENZIONE_ID,
                                  DSL.val("16<odcc8!"))
                  ))
              .leftJoin(REEA_T_REGISTRO)
                  .on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
              .leftJoin(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                  .on(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
              .leftJoin(REEA_T_REGISTRO_SPRESAL_ESITI)
                  .on(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
              .leftJoin(REEA_T_REGISTRO_INAIL)
                  .on(REEA_T_REGISTRO_INAIL.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
              .leftJoin(REEA_T_REGISTRO_PDL_AMIANTO)
                  .on(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
              .leftJoin(REEA_T_ESPOSIZIONE)
                  .on(REEA_T_ESPOSIZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
              .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_S_SOGGETTO.ASSISTENZA_ASL_ID))
              .leftJoin(REEA_D_SOGGETTO_STATO).on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_S_SOGGETTO.SOGGETTO_STATO_ID))
              // MM Proposta per eliminare i record doppi --- Inizio ---
              .leftJoin(REEA_T_ADESIONE)
              .on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_S_SOGGETTO.SOGGETTO_ID))
              .and(REEA_T_ADESIONE.AZIENDA_DESC.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA))
              .and(REEA_T_ADESIONE.AZIENDA_COMUNE_COD.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD))
              .and(REEA_T_ADESIONE.AZIENDA_COMUNE_DESC.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC))
              .and(REEA_T_ADESIONE.AZIENDA_CAP.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP))
              .and(REEA_T_ADESIONE.AZIENDA_PROVINCIA.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA))
              .and(REEA_T_ADESIONE.MANSIONE.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE))
              .and(REEA_T_ADESIONE.ESPOSIZIONE_INIZIO.eq(
                      REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO.cast(LocalDate.class)
              ))
              .and(REEA_T_ADESIONE.ESPOSIZIONE_FINE.eq(
                      REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE.cast(LocalDate.class)
              ))
              // MM Proposta per eliminare i record doppi ---  Fine  ---
              .where(REEA_S_SOGGETTO.DATA_CANCELLAZIONE.isNull().and(condition))
              .fetchInto(ExportDTO.class);

      return risultati.stream()
              .filter(dto -> isValidoPerExport(dto.getSoggettoId(), profiloUtente))
              .toList();
  }
    
    
//    @Override
    public List<ExportDTO> recuperaTotaleRecordPerExportAsincronoStatiPrecedenti_ORIGINALE(Integer assistenzaAslId, String profiloUtente) {

        Condition condition = DSL.noCondition();

        if (assistenzaAslId != null) {
            condition = condition.and(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.eq(assistenzaAslId));
        }
//
//        if (soggettoId != null) {
//            condition = condition.and(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId));
//        }

        Field<String> preNomeChiaro = DSL.when(
                REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_ADESIONE.NOME_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneNome");

        Field<String> preCognomeChiaro = DSL.when(
                REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_ADESIONE.COGNOME_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneCognome");

        Field<String> preTesseraTeamChiara = DSL.when(
                REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneTesseraTeam");

        Field<String> preIdAuraChiaro = DSL.when(
                REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_ADESIONE.ID_AURA_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneIdAura");

        Field<String> preEmailChiara = DSL.when(
                REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_ADESIONE.EMAIL_CIFRATA,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneEmail");

        Field<String> preTelefonoChiaro = DSL.when(
                REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_ADESIONE.TELEFONO_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneTelefono");

        Field<String> auraNomeChiaro = DSL.when(
                REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.NOME_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraNome");

        Field<String> auraCognomeChiaro = DSL.when(
                REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.COGNOME_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraCognome");

        Field<String> auraDomicilioIndirizzoChiaro = DSL.when(
                REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraDomicilioIndirizzo");

        Field<String> auraResidenzaIndirizzoChiaro = DSL.when(
                REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraResidenzaIndirizzo");

        Field<String> auraTesseraTeamChiara = DSL.when(
                REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraTesseraTeam");

        Field<String> auraIdAuraChiaro = DSL.when(
                REEA_T_SOGGETTO.ID_AURA_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.ID_AURA_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraIdAura");

        Field<String> auraEmailChiara = DSL.when(
                REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraEmail");

        Field<String> auraTelefonoChiaro = DSL.when(
                REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraTelefono");

        Field<String> emailChiara = DSL.when(
                REEA_T_SOGGETTO.EMAIL_CIFRATA.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.EMAIL_CIFRATA,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("email");

        Field<String> telefonoChiaro = DSL.when(
                REEA_T_SOGGETTO.TELEFONO_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                        REEA_T_SOGGETTO.TELEFONO_CIFRATO,
                        DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("telefono");

        Field<String> soggettoIdHmac = DSL.field(
                "reea.hmac_soggetto_id(cast({0} as int4), cast({1} as text))",
                String.class,
                REEA_T_SOGGETTO.SOGGETTO_ID,
                DSL.val("16<odcc8!")
        );

        List<ExportDTO> risultati = dsl
                        .selectDistinct(
                                //PRE (da REEA_T_ADESIONE)
                        		REEA_T_SOGGETTO.SOGGETTO_ID.as("soggettoId"),
                                REEA_T_ADESIONE.ADESIONE_COD.as("adesioneCodiceAdesione"),
                                REEA_T_ADESIONE.ADESIONE_DATA.as("adesioneDataAdesione"),
                                REEA_T_ADESIONE.CODICE_FISCALE.as("adesioneCodiceFiscale"),
                                REEA_T_ADESIONE.ADESIONE_COD.as("codiceAdesione"),
                                REEA_T_ADESIONE.ADESIONE_DATA.as("dataAdesione"),
                                REEA_T_ADESIONE.CODICE_FISCALE.as("codiceFiscale"),
                                preCognomeChiaro,
                                preNomeChiaro,
                                REEA_T_ADESIONE.NASCITA_DATA.as("adesioneDataDiNascita"),
                                REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC.as("adesioneProvinciaDiNascita"),
                                REEA_T_ADESIONE.NASCITA_COMUNE_DESC.as("adesioneComuneDiNascita"),
                                preTesseraTeamChiara,
                                preIdAuraChiaro,
                                REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC.as("adesioneProvinciaDiDomicilio"),
                                REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC.as("adesioneComuneDiDomicilio"),
                                REEA_T_ADESIONE.DOMICILIO_COMUNE_COD.as("adesioneCodiceComuneIstatDiDomicilio"),
                                REEA_T_ADESIONE.DOMICILIO_CAP.as("adesioneCapDiDomicilio"),
                                preEmailChiara,
                                preTelefonoChiaro,
                                REEA_T_ADESIONE.DOMICILIO_ASL_COD.as("adesioneCodiceAslDiDomicilio"),
                                REEA_T_ADESIONE.DOMICILIO_ASL_DESC.as("adesioneAslDiDomicilio"),
                                REEA_T_ADESIONE.AZIENDA_COD.as("adesioneCodazi"),
                                REEA_T_ADESIONE.RESIDENZA_ASL_COD.as("adesioneCodiceAslDiResidenza"),
                                REEA_T_ADESIONE.RESIDENZA_ASL_DESC.as("adesioneAslDiResidenza"),
                                REEA_T_ADESIONE.ESPOSIZIONE_INIZIO.as("adesioneDataInizioEsposizione"),
                                REEA_T_ADESIONE.ESPOSIZIONE_FINE.as("adesioneDataFineEsposizione"),
                                REEA_T_ADESIONE.AZIENDA_DESC.as("adesioneAzienda"),
                                REEA_T_ADESIONE.AZIENDA_COMUNE_COD.as("adesioneCodiceComuneIstatAzienda"),
                                REEA_T_ADESIONE.AZIENDA_COMUNE_DESC.as("adesioneComuneAzienda"),
                                REEA_T_ADESIONE.AZIENDA_CAP.as("adesioneCapAzienda"),
                                REEA_T_ADESIONE.AZIENDA_PROVINCIA.as("adesioneProvinciaAzienda"),
                                REEA_T_ADESIONE.MANSIONE.as("adesioneMansione"),
                                

                                //AURA (da REEA_T_SOGGETTO)
                                REEA_T_SOGGETTO.CODICE_FISCALE.as("auraCodiceFiscale"),
                                REEA_T_SOGGETTO.DOMICILIO_ASL_ID.as("auraDomicilioAsl"),
                                REEA_T_SOGGETTO.RESIDENZA_ASL_ID.as("auraResidenzaAsl"),
                                REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("auraAssistenzaAsl"),
                                auraNomeChiaro,
                                auraCognomeChiaro,
                                REEA_T_SOGGETTO.SESSO.as("auraSesso"),
                                REEA_T_SOGGETTO.NASCITA_DATA.as("auraNascitaData"),
                                REEA_T_SOGGETTO.NASCITA_COMUNE_COD.as("auraNascitaComuneCod"),
                                REEA_T_SOGGETTO.NASCITA_COMUNE_DESC.as("auraNascitaComuneDesc"),
                                REEA_T_SOGGETTO.NASCITA_PROVINCIA_COD.as("auraNascitaProvinciaCod"),
                                REEA_T_SOGGETTO.NASCITA_PROVINCIA_DESC.as("auraNascitaProvinciaDesc"),
                                REEA_T_SOGGETTO.NASCITA_STATO_COD.as("auraNascitaStatoCod"),
                                REEA_T_SOGGETTO.NASCITA_STATO_DESC.as("auraNascitaStatoDesc"),
                                REEA_T_SOGGETTO.CITTADINANZA_STATO_COD.as("auraCittadinanzaStatoCod"),
                                REEA_T_SOGGETTO.CITTADINANZA_STATO_DESC.as("auraCittadinanzaStatoDesc"),
                                REEA_T_SOGGETTO.DOMICILIO_COMUNE_COD.as("auraDomicilioComuneCod"),
                                REEA_T_SOGGETTO.DOMICILIO_COMUNE_DESC.as("auraDomicilioComuneDesc"),
                                REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_COD.as("auraDomicilioProvinciaCod"),
                                REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_DESC.as("auraDomicilioProvinciaDesc"),
                                REEA_T_SOGGETTO.DOMICILIO_STATO_COD.as("auraDomicilioStatoCod"),
                                REEA_T_SOGGETTO.DOMICILIO_STATO_DESC.as("auraDomicilioStatoDesc"),
                                REEA_T_SOGGETTO.DOMICILIO_CAP.as("auraDomicilioCap"),
                                auraDomicilioIndirizzoChiaro,
                                REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO.as("auraDomicilioNumeroCivico"),
                                REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD.as("auraResidenzaComuneCod"),
                                REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC.as("auraResidenzaComuneDesc"),
                                REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD.as("auraResidenzaProvinciaCod"),
                                REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC.as("auraResidenzaProvinciaDesc"),
                                REEA_T_SOGGETTO.RESIDENZA_STATO_COD.as("auraResidenzaStatoCod"),
                                REEA_T_SOGGETTO.RESIDENZA_STATO_DESC.as("auraResidenzaStatoDesc"),
                                REEA_T_SOGGETTO.RESIDENZA_CAP.as("auraResidenzaCap"),
                                auraResidenzaIndirizzoChiaro,
                                REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO.as("auraResidenzaNumeroCivico"),
                                auraTesseraTeamChiara,
                                auraIdAuraChiaro,
                                auraEmailChiara,
                                auraTelefonoChiaro,
                                emailChiara,
                                telefonoChiaro,
                                REEA_T_SOGGETTO.DATA_DECESSO.as("auraDataDecesso"),
                                REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE.as("auraAssistenzaAslFine"),

                                // ESENZIONI
                                REEA_D_ESENZIONE.ESENZIONE_COD.as("codEsenzione"),
                                REEA_D_ESENZIONE.ESENZIONE_DESC.as("descEsenzione"),
                                REEA_D_ESENZIONE.DIAGNOSI_COD.as("codDiagnosi"),
                                REEA_D_ESENZIONE.DIAGNOSI_DESC.as("descDiagnosi"),
                                REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_EMISSIONE.as("dataEmissione"),
                                REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_SCADENZA.as("dataScadenza"),
                                
                                //ESPOSIZIONI
                                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA.as("adesioneEsposizioneAzienda"),
                                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD.as("adesioneEsposizioneAziendaComuneCod"),
                                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC.as("adesioneEsposizioneAziendaComuneDesc"),
                                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP.as("adesioneEsposizioneAziendaCap"),
                                REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO.as("adesioneEsposizioneDataInizioEsposizione"),
                                REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE.as("adesioneEsposizioneDataFineEsposizione"),
                                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA.as("adesioneEsposizioneProvinciaAzienda"),
                                REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE.as("adesioneEsposizioneMansione"),

                                //ANAMNESI (da REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.as("anamnesiCodiceFiscale"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA.as("anamnesiIdAura"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA.as("anamnesiDataIntervista"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE.as("anamnesiNominativoIntervistatore"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE.as("anamnesiFumatore"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE.as("anamnesiSigarette"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI.as("anamnesiSigaretteAnni"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO.as("anamnesiSigaretteEtaInizio"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE.as("anamnesiSigaretteFumaAttualmente"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE.as("anamnesiSigaretteEtaFine"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE.as("anamnesiSigaretteDie"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI.as("anamnesiSigari"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI.as("anamnesiSigariAnni"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO.as("anamnesiSigariEtaInizio"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE.as("anamnesiSigariFumaAttualmente"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE.as("anamnesiSigariEtaFine"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE.as("anamnesiSigariDie"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA.as("anamnesiPipa"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI.as("anamnesiPipaAnni"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO.as("anamnesiPipaEtaInizio"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE.as("anamnesiPipaFumaAttualmente"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE.as("anamnesiPipaEtaFine"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE.as("anamnesiPipaDie"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM.as("anamnesiOccupazioneNum"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO.as("anamnesiOccupazioneAnnoInizio"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE.as("anamnesiOccupazioneAnnoFine"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO.as("anamnesiOccupazioneTipo"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO.as("anamnesiOccupazioneDescrizioneLavoro"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA.as("anamnesiOccupazioneNomeEIndirizzoDitta"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA.as("anamnesiOccupazioneAttivitaDitta"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO.as("anamnesiNotaAttivitaConAmianto"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO.as("anamnesiAnamnesiEsposizioneAmianto"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE.as("anamnesiEsposizioneProfessionale"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE.as("anamnesiAnnoFineEsposizione"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE.as("anamnesiLivelloEsposizione"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA.as("anamnesiInserimentoInSorveglianza"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL.as("anamnesiIdSpresal"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.COUNSELING.as("anamnesiCounseling"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT.as("anamnesiOccupazioneEsposizioneCrpt"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT.as("anamnesiOccupazioneSettoreDittaCrpt"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT.as("anamnesiOccupazioneMansioneCrpt"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT.as("anamnesiOccupazioneRagioneSocialeDittaCrpt"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT.as("anamnesiOccupazionePivaDittaCrpt"),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_CODICE_FISCALE_DITTA_CRPT.as("anamnesiOccupazioneCodiceFiscaleDittaCrpt"),

                                //ESITI VISITA (da REEA_T_REGISTRO_SPRESAL_ESITI)
                                REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE.as("esitiVisCodiceFiscale"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA.as("esitiVisIdAura"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.as("esitiVisDataVisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.VISITA.as("esitiVisVisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA.as("esitiVisLivelloVisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO.as("esitiVisRiceveIndennizzo"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO.as("esitiVisMalattiaIndennizzo"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX.as("esitiVisAccertamentiRx"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA.as("esitiVisAccertamentiRxData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE.as("esitiVisAccertamentiRxRefertoNormale"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA.as("esitiVisAccertamentiRxAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC.as("esitiVisAccertamentiTc"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA.as("esitiVisAccertamentiTcData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE.as("esitiVisAccertamentiTcRefertoNormale"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA.as("esitiVisAccertamentiTcAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE.as("esitiVisAccertamentiSpirometriaSemplice"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA.as("esitiVisAccertamentiSpirometriaSempliceData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaSempliceRefertoNormale"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA.as("esitiVisAccertamentiSpirometriaSempliceAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE.as("esitiVisAccertamentiSpirometriaGlobale"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA.as("esitiVisAccertamentiSpirometriaGlobaleData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaGlobaleRefertoNormale"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA.as("esitiVisAccertamentiSpirometriaGlobaleAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO.as("esitiVisAccertamentiDlco"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA.as("esitiVisAccertamentiDlcoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE.as("esitiVisAccertamentiDlcoRefertoNormale"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA.as("esitiVisAccertamentiDlcoAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET.as("esitiVisAccertamentiPet"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA.as("esitiVisAccertamentiPetData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE.as("esitiVisAccertamentiPetRefertoNormale"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA.as("esitiVisAccertamentiPetAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA.as("esitiVisAccertamentiVisitaPneumologica"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA.as("esitiVisAccertamentiVisitaPneumologicaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaPneumologicaReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaPneumologicaAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA.as("esitiVisAccertamentiVisitaRadiologica"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA.as("esitiVisAccertamentiVisitaRadiologicaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaRadiologicaReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaRadiologicaAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA.as("esitiVisAccertamentiVisitaOncologica"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA.as("esitiVisAccertamentiVisitaOncologicaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaOncologicaReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaOncologicaAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO.as("esitiVisAccertamentiAltro"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE.as("esitiVisAccertamentiAltroDescrizione"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA.as("esitiVisAccertamentiAltroData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE.as("esitiVisAccertamentiAltroRefertoNormale"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA.as("esitiVisAccertamentiAltroRefertoAcquisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO.as("esitiVisRisultatoNegativo"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI.as("esitiVisPpmPlacchepleUrdicheMonolaterali"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisPpmPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisPpmPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisPpmAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisPpmAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisPpmPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO.as("esitiVisPpmReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA.as("esitiVisPpmRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI.as("esitiVisPpbPlacchepleUricheBilaterali"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisPpbPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisPpbPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisPpbAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisPpbAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisPpbPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO.as("esitiVisPpbReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA.as("esitiVisPpbRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE.as("esitiVisApAsbestosiPolmonare"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisApPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisApPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisApAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisApAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisApPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO.as("esitiVisApReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA.as("esitiVisApRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA.as("esitiVisFpdFibrosiPleuricaDiffusa"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisFpdPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisFpdPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisFpdAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisFpdAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisFpdPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO.as("esitiVisFpdReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA.as("esitiVisFpdRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO.as("esitiVisMpMesoteliomapleurico"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisMpPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisMpPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisMpAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisMpAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisMpPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO.as("esitiVisMpReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA.as("esitiVisMpRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR.as("esitiVisMpComunicazioneAlCor"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA.as("esitiVisMpComunicazioneAlCorData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA.as("esitiVisAmAltroMesotelioma"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisAmPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisAmPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisAmAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisAmAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisAmPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO.as("esitiVisAmReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA.as("esitiVisAmRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR.as("esitiVisAmComunicazioneAlCor"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA.as("esitiVisAmComunicazioneAlCorData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE.as("esitiVisNlNeoplasiaLaringe"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisNlPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisNlPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisNlAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisNlAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisNlPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO.as("esitiVisNlReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA.as("esitiVisNlRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA.as("esitiVisNoNeoplasiaOvarica"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisNoPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisNoPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisNoAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisNoAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisNoPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO.as("esitiVisNoReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA.as("esitiVisNoRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE.as("esitiVisTpTumoreDelPolmone"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisTpPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisTpPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisTpAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisTpAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisTpPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO.as("esitiVisTpReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA.as("esitiVisTpRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR.as("esitiVisTpComunicazioneAlCor"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA.as("esitiVisTpComunicazioneAlCorData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE.as("esitiVisBpcoEnfisemaPolmonare"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisBpcoPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisBpcoPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisBpcoAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisBpcoAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisBpcoPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO.as("esitiVisBpcoReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA.as("esitiVisBpcoRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI.as("esitiVisAltraDiagnosi"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE.as("esitiVisAltraDiagnosiDescrizione"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisAltraPrimoCertificatoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisAltraPrimoCertificatoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisAltraAggravamentoEDenuncia"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisAltraAggravamentoEDenunciaData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisAltraPercentualeDiRiconoscimento"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO.as("esitiVisAltraReferto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA.as("esitiVisAltraRefertoData"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO.as("esitiVisFollowUpPrevisto"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA.as("esitiVisAnnoPresuntoProssimaVisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA.as("esitiVisAnnoUltimaVisita"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG.as("esitiVisInvioSintesiAMmg"),
                                REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL.as("esitiVisIdSpresal"),
                                
                                REEA_T_REGISTRO.ATT_SANITARIA_STATO.as("regAttSanitariaStato"),
                                REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL.as("regAttSanitariaSpresal"),
                                REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA.as("regAttSanitariaSpresalData"),
                                REEA_T_REGISTRO.ATT_SANITARIA_INAIL.as("regAttSanitariaInail"),
                                REEA_T_REGISTRO.SEZIONE.as("regSezione"),
                                REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("regInseritoInSorveglianza"),
                                REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("regTipoElencoInail"),
                                
                                REEA_T_REGISTRO_INAIL.DOMANDA.as("inailDomanda"),
                                REEA_T_REGISTRO_INAIL.COGNOME.as("inailCognome"),
                                REEA_T_REGISTRO_INAIL.NOME.as("inailNome"),
                                REEA_T_REGISTRO_INAIL.CODICE_FISCALE.as("inailCodiceFiscale"),
                                REEA_T_REGISTRO_INAIL.SESSO.as("inailSesso"),
                                REEA_T_REGISTRO_INAIL.DATA_NASCITA.as("inailDataNascita"),
                                REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA.as("inailIndirizzoResidenza"),
                                REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA.as("inailIstatResidenza"),
                                REEA_T_REGISTRO_INAIL.CAP_RESIDENZA.as("inailCapResidenza"),
                                REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA.as("inailRegioneResidenza"),
                                REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA.as("inailProvinciaResidenza"),
                                REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA.as("inailComuneResidenza"),
                                
                                REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE.as("nplaCodiceFiscale"),
                                REEA_T_REGISTRO_PDL_AMIANTO.PERIODO.as("nplaPeriodo"),
                                REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE.as("nplaIdCantiere"),
                                REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA.as("nplaAziendaPiva"),
                                REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME.as("nplaAziendaNome"),
                                REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE.as("nplaAslCantiere"),
                                REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE.as("nplaComuneCantiere"),
                                REEA_T_REGISTRO_PDL_AMIANTO.ANNO.as("nplaAnno"),
                                REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO.as("nplaTipologiaPiano"),
                                REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE.as("nplaQuantitaDaRimuovere"),
                                REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA.as("nplaQuantitaRimossa"),
                                REEA_D_ASL.ASL_AZIENDA_DESC.as("aslAziendaDesc"),
                                REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("soggettoStatoDesc")
                        )
                .from(REEA_T_SOGGETTO)
                .join(REEA_T_ADESIONE)
                    .on(REEA_T_SOGGETTO.SOGGETTO_ID.eq(REEA_T_ADESIONE.SOGGETTO_ID))
                .leftJoin(REEA_R_SOGGETTO_ESENZIONE)
                    .on(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
                .leftJoin(REEA_D_ESENZIONE)
                    .on(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_HMAC.eq(
                            DSL.field("reea.text_to_hmac(cast({0} as text), cast({1} as text))",
                                    String.class,
                                    REEA_D_ESENZIONE.ESENZIONE_ID,
                                    DSL.val("16<odcc8!"))
                    ))
                .leftJoin(REEA_T_REGISTRO)
                    .on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
                .leftJoin(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                    .on(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
                .leftJoin(REEA_T_REGISTRO_SPRESAL_ESITI)
                    .on(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
                .leftJoin(REEA_T_REGISTRO_INAIL)
                    .on(REEA_T_REGISTRO_INAIL.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
                .leftJoin(REEA_T_REGISTRO_PDL_AMIANTO)
                    .on(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
                .leftJoin(REEA_T_ESPOSIZIONE)
                    .on(REEA_T_ESPOSIZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
                .leftJoin(REEA_D_SOGGETTO_STATO).on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
                .where(REEA_T_SOGGETTO.DATA_CANCELLAZIONE.isNull().and(condition))
                .fetchInto(ExportDTO.class);

        return risultati.stream()
                .filter(dto -> isValidoPerExport(dto.getSoggettoId(), profiloUtente))
                .toList();
    }
    

    private boolean isValidoPerExport(Integer soggettoId, String profiloUtente) {
    	
//    	if(soggettoId == 660831 || soggettoId == 660857) {
    	 if ("REEA_OP_SPRESAL_PSEUDO".equals(profiloUtente)
                 || "REEA_OP_EPI_PSEUDO".equals(profiloUtente)
                 || "REEA_OP_SPRESAL".equals(profiloUtente)
                 || "REEA_OP_CSI_PSEUDO".equals(profiloUtente)) {

	        List<StoriaStatoDTO> storia = findStoriaStati(soggettoId);
	        StoriaStatoDTO corrente = findStatoCorrente(soggettoId);
	        storia.add(corrente);
	        
	        if (storia == null || storia.isEmpty()) {
	            return false;
	        }
	
	//        if(soggettoId == 21744 || soggettoId == 21745) {
	//	        StoriaStatoDTO corrente = storia.get(storia.size() - 1);
		        String statoCorrente = normalizzaStato(corrente.getSoggettoStatoDesc());
		
		        if ("CARICATO".equals(statoCorrente) || "DA_VALUTARE".equals(statoCorrente)) {
		            return false;
		        }
	//        }
	
	        if ("ESCLUSO_PER_EMIGRAZIONE".equals(statoCorrente) || "ESCLUSO_PER_DECESSO".equals(statoCorrente)) {
	            if (storia.size() < 2) {
	                return false;
	            }
	
	            boolean bloccante = storia.stream()
	                    .anyMatch(s -> {
	                        String stato = normalizzaStato(s.getSoggettoStatoDesc());
	                        return "CARICATO".equals(stato) || "DA_VALUTARE".equals(stato);
	                    });
	
	            if (bloccante) {
	                return false;
	            }
	        }
    	 }
//    	}
        return true; 
    	
    }

    private String normalizzaStato(String stato) {
        if (stato == null) {
            return null;
        }

        return stato.trim()
                .toUpperCase()
                .replace(' ', '_');
    }


    @Override
    public List<ExportDTO> recuperaTotaleRecordPerExportAsincrono(Integer assistenzaAslId, Integer soggettoId, boolean includiSenzaAdesione, String profiloUtente) {

    	// Filtro assistenza_asl_id
        Condition condition = noCondition();
       
        if (assistenzaAslId != null) {
        	condition         = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
        }
        
        if (soggettoId != null) {
            condition = condition.and(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId));
        }
    	
        // ── REEA_T_ADESIONE - null safe ──────────────────────────────────────────

        Field<String> preNomeChiaro = DSL.when(
            REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_ADESIONE.NOME_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneNome");

        Field<String> preCognomeChiaro = DSL.when(
            REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_ADESIONE.COGNOME_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneCognome");

        Field<String> preTesseraTeamChiara = DSL.when(
            REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneTesseraTeam");

        Field<String> preIdAuraChiaro = DSL.when(
            REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_ADESIONE.ID_AURA_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneIdAura");

        Field<String> preEmailChiara = DSL.when(
            REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_ADESIONE.EMAIL_CIFRATA,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneEmail");

        Field<String> preTelefonoChiaro = DSL.when(
            REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_ADESIONE.TELEFONO_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneTelefono");

        // ── REEA_T_SOGGETTO - null safe ──────────────────────────────────────────

        Field<String> auraNomeChiaro = DSL.when(
            REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_SOGGETTO.NOME_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraNome");

        Field<String> auraCognomeChiaro = DSL.when(
            REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_SOGGETTO.COGNOME_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraCognome");

        Field<String> auraDomicilioIndirizzoChiaro = DSL.when(
            REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraDomicilioIndirizzo");

        Field<String> auraResidenzaIndirizzoChiaro = DSL.when(
            REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraResidenzaIndirizzo");

        Field<String> auraTesseraTeamChiara = DSL.when(
            REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraTesseraTeam");

        Field<String> auraIdAuraChiaro = DSL.when(
            REEA_T_SOGGETTO.ID_AURA_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_SOGGETTO.ID_AURA_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraIdAura");

        Field<String> auraEmailChiara = DSL.when(
            REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraEmailAura");

        Field<String> auraTelefonoChiaro = DSL.when(
            REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO,
                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraTelefonoAura");
        
        Field<String> emailChiara = DSL.when(
                REEA_T_SOGGETTO.EMAIL_CIFRATA.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                    REEA_T_SOGGETTO.EMAIL_CIFRATA,
                    DSL.val("16<odcc8!", SQLDataType.VARCHAR))
            ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("email");

            Field<String> telefonoChiaro = DSL.when(
                REEA_T_SOGGETTO.TELEFONO_CIFRATO.isNotNull(),
                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
                    REEA_T_SOGGETTO.TELEFONO_CIFRATO,
                    DSL.val("16<odcc8!", SQLDataType.VARCHAR))
            ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("telefono");

        Field<String> soggettoIdHmac = DSL.field(
            "reea.hmac_soggetto_id(cast({0} as int4), cast({1} as text))",
            String.class,
            REEA_T_SOGGETTO.SOGGETTO_ID,
            DSL.val("16<odcc8!")
        );

        List<ExportDTO> risultati = dsl
                .selectDistinct(
                        //PRE (da REEA_T_ADESIONE)
                		REEA_T_SOGGETTO.SOGGETTO_ID.as("soggettoId"),
                        REEA_T_ADESIONE.ADESIONE_COD.as("adesioneCodiceAdesione"),
                        REEA_T_ADESIONE.ADESIONE_DATA.as("adesioneDataAdesione"),
                        REEA_T_ADESIONE.CODICE_FISCALE.as("adesioneCodiceFiscale"),
                        REEA_T_ADESIONE.ADESIONE_COD.as("codiceAdesione"),
                        REEA_T_ADESIONE.ADESIONE_DATA.as("dataAdesione"),
                        REEA_T_ADESIONE.CODICE_FISCALE.as("codiceFiscale"),
                        preCognomeChiaro,
                        preNomeChiaro,
                        REEA_T_ADESIONE.NASCITA_DATA.as("adesioneDataDiNascita"),
                        REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC.as("adesioneProvinciaDiNascita"),
                        REEA_T_ADESIONE.NASCITA_COMUNE_DESC.as("adesioneComuneDiNascita"),
                        preTesseraTeamChiara,
                        preIdAuraChiaro,
                        REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC.as("adesioneProvinciaDiDomicilio"),
                        REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC.as("adesioneComuneDiDomicilio"),
                        REEA_T_ADESIONE.DOMICILIO_COMUNE_COD.as("adesioneCodiceComuneIstatDiDomicilio"),
                        REEA_T_ADESIONE.DOMICILIO_CAP.as("adesioneCapDiDomicilio"),
                        preEmailChiara,
                        preTelefonoChiaro,
                        REEA_T_ADESIONE.DOMICILIO_ASL_COD.as("adesioneCodiceAslDiDomicilio"),
                        REEA_T_ADESIONE.DOMICILIO_ASL_DESC.as("adesioneAslDiDomicilio"),
                        REEA_T_ADESIONE.AZIENDA_COD.as("adesioneCodazi"),
                        REEA_T_ADESIONE.RESIDENZA_ASL_COD.as("adesioneCodiceAslDiResidenza"),
                        REEA_T_ADESIONE.RESIDENZA_ASL_DESC.as("adesioneAslDiResidenza"),
                        REEA_T_ADESIONE.ESPOSIZIONE_INIZIO.as("adesioneDataInizioEsposizione"),
                        REEA_T_ADESIONE.ESPOSIZIONE_FINE.as("adesioneDataFineEsposizione"),
                        REEA_T_ADESIONE.AZIENDA_DESC.as("adesioneAzienda"),
                        REEA_T_ADESIONE.AZIENDA_COMUNE_COD.as("adesioneCodiceComuneIstatAzienda"),
                        REEA_T_ADESIONE.AZIENDA_COMUNE_DESC.as("adesioneComuneAzienda"),
                        REEA_T_ADESIONE.AZIENDA_CAP.as("adesioneCapAzienda"),
                        REEA_T_ADESIONE.AZIENDA_PROVINCIA.as("adesioneProvinciaAzienda"),
                        REEA_T_ADESIONE.MANSIONE.as("adesioneMansione"),
                        

                        //AURA (da REEA_T_SOGGETTO)
                        REEA_T_SOGGETTO.CODICE_FISCALE.as("auraCodiceFiscale"),
                        REEA_T_SOGGETTO.DOMICILIO_ASL_ID.as("auraDomicilioAsl"),
                        REEA_T_SOGGETTO.RESIDENZA_ASL_ID.as("auraResidenzaAsl"),
                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("auraAssistenzaAsl"),
                        auraNomeChiaro,
                        auraCognomeChiaro,
                        REEA_T_SOGGETTO.SESSO.as("auraSesso"),
                        REEA_T_SOGGETTO.NASCITA_DATA.as("auraNascitaData"),
                        REEA_T_SOGGETTO.NASCITA_COMUNE_COD.as("auraNascitaComuneCod"),
                        REEA_T_SOGGETTO.NASCITA_COMUNE_DESC.as("auraNascitaComuneDesc"),
                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_COD.as("auraNascitaProvinciaCod"),
                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_DESC.as("auraNascitaProvinciaDesc"),
                        REEA_T_SOGGETTO.NASCITA_STATO_COD.as("auraNascitaStatoCod"),
                        REEA_T_SOGGETTO.NASCITA_STATO_DESC.as("auraNascitaStatoDesc"),
                        REEA_T_SOGGETTO.CITTADINANZA_STATO_COD.as("auraCittadinanzaStatoCod"),
                        REEA_T_SOGGETTO.CITTADINANZA_STATO_DESC.as("auraCittadinanzaStatoDesc"),
                        REEA_T_SOGGETTO.DOMICILIO_COMUNE_COD.as("auraDomicilioComuneCod"),
                        REEA_T_SOGGETTO.DOMICILIO_COMUNE_DESC.as("auraDomicilioComuneDesc"),
                        REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_COD.as("auraDomicilioProvinciaCod"),
                        REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_DESC.as("auraDomicilioProvinciaDesc"),
                        REEA_T_SOGGETTO.DOMICILIO_STATO_COD.as("auraDomicilioStatoCod"),
                        REEA_T_SOGGETTO.DOMICILIO_STATO_DESC.as("auraDomicilioStatoDesc"),
                        REEA_T_SOGGETTO.DOMICILIO_CAP.as("auraDomicilioCap"),
                        auraDomicilioIndirizzoChiaro,
                        REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO.as("auraDomicilioNumeroCivico"),
                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD.as("auraResidenzaComuneCod"),
                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC.as("auraResidenzaComuneDesc"),
                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD.as("auraResidenzaProvinciaCod"),
                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC.as("auraResidenzaProvinciaDesc"),
                        REEA_T_SOGGETTO.RESIDENZA_STATO_COD.as("auraResidenzaStatoCod"),
                        REEA_T_SOGGETTO.RESIDENZA_STATO_DESC.as("auraResidenzaStatoDesc"),
                        REEA_T_SOGGETTO.RESIDENZA_CAP.as("auraResidenzaCap"),
                        auraResidenzaIndirizzoChiaro,
                        REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO.as("auraResidenzaNumeroCivico"),
                        auraTesseraTeamChiara,
                        auraIdAuraChiaro,
                        auraEmailChiara,
                        auraTelefonoChiaro,
                        emailChiara,
                        telefonoChiaro,
                        REEA_T_SOGGETTO.DATA_DECESSO.as("auraDataDecesso"),
                        REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE.as("auraAssistenzaAslFine"),

                        // ESENZIONI
                        REEA_D_ESENZIONE.ESENZIONE_COD.as("codEsenzione"),
                        REEA_D_ESENZIONE.ESENZIONE_DESC.as("descEsenzione"),
                        REEA_D_ESENZIONE.DIAGNOSI_COD.as("codDiagnosi"),
                        REEA_D_ESENZIONE.DIAGNOSI_DESC.as("descDiagnosi"),
                        REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_EMISSIONE.as("dataEmissione"),
                        REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_SCADENZA.as("dataScadenza"),
                        
                        //ESPOSIZIONI
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA.as("adesioneEsposizioneAzienda"),
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD.as("adesioneEsposizioneAziendaComuneCod"),
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC.as("adesioneEsposizioneAziendaComuneDesc"),
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP.as("adesioneEsposizioneAziendaCap"),
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO.as("adesioneEsposizioneDataInizioEsposizione"),
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE.as("adesioneEsposizioneDataFineEsposizione"),
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA.as("adesioneEsposizioneProvinciaAzienda"),
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE.as("adesioneEsposizioneMansione"),

                        //ANAMNESI (da REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.as("anamnesiCodiceFiscale"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA.as("anamnesiIdAura"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA.as("anamnesiDataIntervista"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE.as("anamnesiNominativoIntervistatore"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE.as("anamnesiFumatore"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE.as("anamnesiSigarette"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI.as("anamnesiSigaretteAnni"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO.as("anamnesiSigaretteEtaInizio"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE.as("anamnesiSigaretteFumaAttualmente"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE.as("anamnesiSigaretteEtaFine"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE.as("anamnesiSigaretteDie"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI.as("anamnesiSigari"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI.as("anamnesiSigariAnni"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO.as("anamnesiSigariEtaInizio"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE.as("anamnesiSigariFumaAttualmente"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE.as("anamnesiSigariEtaFine"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE.as("anamnesiSigariDie"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA.as("anamnesiPipa"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI.as("anamnesiPipaAnni"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO.as("anamnesiPipaEtaInizio"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE.as("anamnesiPipaFumaAttualmente"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE.as("anamnesiPipaEtaFine"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE.as("anamnesiPipaDie"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM.as("anamnesiOccupazioneNum"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO.as("anamnesiOccupazioneAnnoInizio"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE.as("anamnesiOccupazioneAnnoFine"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO.as("anamnesiOccupazioneTipo"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO.as("anamnesiOccupazioneDescrizioneLavoro"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA.as("anamnesiOccupazioneNomeEIndirizzoDitta"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA.as("anamnesiOccupazioneAttivitaDitta"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO.as("anamnesiNotaAttivitaConAmianto"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO.as("anamnesiAnamnesiEsposizioneAmianto"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE.as("anamnesiEsposizioneProfessionale"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE.as("anamnesiAnnoFineEsposizione"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE.as("anamnesiLivelloEsposizione"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA.as("anamnesiInserimentoInSorveglianza"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL.as("anamnesiIdSpresal"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.COUNSELING.as("anamnesiCounseling"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT.as("anamnesiOccupazioneEsposizioneCrpt"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT.as("anamnesiOccupazioneSettoreDittaCrpt"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT.as("anamnesiOccupazioneMansioneCrpt"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT.as("anamnesiOccupazioneRagioneSocialeDittaCrpt"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT.as("anamnesiOccupazionePivaDittaCrpt"),
                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_CODICE_FISCALE_DITTA_CRPT.as("anamnesiOccupazioneCodiceFiscaleDittaCrpt"),

                        //ESITI VISITA (da REEA_T_REGISTRO_SPRESAL_ESITI)
                        REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE.as("esitiVisCodiceFiscale"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA.as("esitiVisIdAura"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.as("esitiVisDataVisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.VISITA.as("esitiVisVisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA.as("esitiVisLivelloVisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO.as("esitiVisRiceveIndennizzo"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO.as("esitiVisMalattiaIndennizzo"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX.as("esitiVisAccertamentiRx"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA.as("esitiVisAccertamentiRxData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE.as("esitiVisAccertamentiRxRefertoNormale"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA.as("esitiVisAccertamentiRxAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC.as("esitiVisAccertamentiTc"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA.as("esitiVisAccertamentiTcData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE.as("esitiVisAccertamentiTcRefertoNormale"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA.as("esitiVisAccertamentiTcAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE.as("esitiVisAccertamentiSpirometriaSemplice"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA.as("esitiVisAccertamentiSpirometriaSempliceData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaSempliceRefertoNormale"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA.as("esitiVisAccertamentiSpirometriaSempliceAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE.as("esitiVisAccertamentiSpirometriaGlobale"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA.as("esitiVisAccertamentiSpirometriaGlobaleData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaGlobaleRefertoNormale"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA.as("esitiVisAccertamentiSpirometriaGlobaleAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO.as("esitiVisAccertamentiDlco"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA.as("esitiVisAccertamentiDlcoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE.as("esitiVisAccertamentiDlcoRefertoNormale"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA.as("esitiVisAccertamentiDlcoAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET.as("esitiVisAccertamentiPet"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA.as("esitiVisAccertamentiPetData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE.as("esitiVisAccertamentiPetRefertoNormale"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA.as("esitiVisAccertamentiPetAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA.as("esitiVisAccertamentiVisitaPneumologica"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA.as("esitiVisAccertamentiVisitaPneumologicaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaPneumologicaReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaPneumologicaAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA.as("esitiVisAccertamentiVisitaRadiologica"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA.as("esitiVisAccertamentiVisitaRadiologicaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaRadiologicaReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaRadiologicaAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA.as("esitiVisAccertamentiVisitaOncologica"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA.as("esitiVisAccertamentiVisitaOncologicaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaOncologicaReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaOncologicaAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO.as("esitiVisAccertamentiAltro"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE.as("esitiVisAccertamentiAltroDescrizione"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA.as("esitiVisAccertamentiAltroData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE.as("esitiVisAccertamentiAltroRefertoNormale"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA.as("esitiVisAccertamentiAltroRefertoAcquisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO.as("esitiVisRisultatoNegativo"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI.as("esitiVisPpmPlacchepleUrdicheMonolaterali"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisPpmPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisPpmPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisPpmAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisPpmAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisPpmPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO.as("esitiVisPpmReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA.as("esitiVisPpmRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI.as("esitiVisPpbPlacchepleUricheBilaterali"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisPpbPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisPpbPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisPpbAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisPpbAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisPpbPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO.as("esitiVisPpbReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA.as("esitiVisPpbRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE.as("esitiVisApAsbestosiPolmonare"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisApPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisApPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisApAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisApAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisApPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO.as("esitiVisApReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA.as("esitiVisApRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA.as("esitiVisFpdFibrosiPleuricaDiffusa"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisFpdPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisFpdPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisFpdAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisFpdAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisFpdPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO.as("esitiVisFpdReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA.as("esitiVisFpdRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO.as("esitiVisMpMesoteliomapleurico"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisMpPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisMpPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisMpAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisMpAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisMpPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO.as("esitiVisMpReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA.as("esitiVisMpRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR.as("esitiVisMpComunicazioneAlCor"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA.as("esitiVisMpComunicazioneAlCorData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA.as("esitiVisAmAltroMesotelioma"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisAmPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisAmPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisAmAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisAmAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisAmPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO.as("esitiVisAmReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA.as("esitiVisAmRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR.as("esitiVisAmComunicazioneAlCor"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA.as("esitiVisAmComunicazioneAlCorData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE.as("esitiVisNlNeoplasiaLaringe"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisNlPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisNlPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisNlAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisNlAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisNlPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO.as("esitiVisNlReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA.as("esitiVisNlRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA.as("esitiVisNoNeoplasiaOvarica"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisNoPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisNoPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisNoAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisNoAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisNoPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO.as("esitiVisNoReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA.as("esitiVisNoRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE.as("esitiVisTpTumoreDelPolmone"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisTpPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisTpPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisTpAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisTpAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisTpPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO.as("esitiVisTpReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA.as("esitiVisTpRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR.as("esitiVisTpComunicazioneAlCor"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA.as("esitiVisTpComunicazioneAlCorData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE.as("esitiVisBpcoEnfisemaPolmonare"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisBpcoPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisBpcoPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisBpcoAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisBpcoAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisBpcoPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO.as("esitiVisBpcoReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA.as("esitiVisBpcoRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI.as("esitiVisAltraDiagnosi"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE.as("esitiVisAltraDiagnosiDescrizione"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisAltraPrimoCertificatoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisAltraPrimoCertificatoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisAltraAggravamentoEDenuncia"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisAltraAggravamentoEDenunciaData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisAltraPercentualeDiRiconoscimento"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO.as("esitiVisAltraReferto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA.as("esitiVisAltraRefertoData"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO.as("esitiVisFollowUpPrevisto"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA.as("esitiVisAnnoPresuntoProssimaVisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA.as("esitiVisAnnoUltimaVisita"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG.as("esitiVisInvioSintesiAMmg"),
                        REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL.as("esitiVisIdSpresal"),
                        
                        REEA_T_REGISTRO.ATT_SANITARIA_STATO.as("regAttSanitariaStato"),
                        REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL.as("regAttSanitariaSpresal"),
                        REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA.as("regAttSanitariaSpresalData"),
                        REEA_T_REGISTRO.ATT_SANITARIA_INAIL.as("regAttSanitariaInail"),
                        REEA_T_REGISTRO.SEZIONE.as("regSezione"),
                        REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("regInseritoInSorveglianza"),
                        REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("regTipoElencoInail"),
                        
                        REEA_T_REGISTRO_INAIL.DOMANDA.as("inailDomanda"),
                        REEA_T_REGISTRO_INAIL.COGNOME.as("inailCognome"),
                        REEA_T_REGISTRO_INAIL.NOME.as("inailNome"),
                        REEA_T_REGISTRO_INAIL.CODICE_FISCALE.as("inailCodiceFiscale"),
                        REEA_T_REGISTRO_INAIL.SESSO.as("inailSesso"),
                        REEA_T_REGISTRO_INAIL.DATA_NASCITA.as("inailDataNascita"),
                        REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA.as("inailIndirizzoResidenza"),
                        REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA.as("inailIstatResidenza"),
                        REEA_T_REGISTRO_INAIL.CAP_RESIDENZA.as("inailCapResidenza"),
                        REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA.as("inailRegioneResidenza"),
                        REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA.as("inailProvinciaResidenza"),
                        REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA.as("inailComuneResidenza"),
                        
                        REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE.as("nplaCodiceFiscale"),
                        REEA_T_REGISTRO_PDL_AMIANTO.PERIODO.as("nplaPeriodo"),
                        REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE.as("nplaIdCantiere"),
                        REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA.as("nplaAziendaPiva"),
                        REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME.as("nplaAziendaNome"),
                        REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE.as("nplaAslCantiere"),
                        REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE.as("nplaComuneCantiere"),
                        REEA_T_REGISTRO_PDL_AMIANTO.ANNO.as("nplaAnno"),
                        REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO.as("nplaTipologiaPiano"),
                        REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE.as("nplaQuantitaDaRimuovere"),
                        REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA.as("nplaQuantitaRimossa"),
                        REEA_D_ASL.ASL_AZIENDA_DESC.as("aslAziendaDesc"),
                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("soggettoStatoDesc"))
                .from(REEA_T_SOGGETTO)
                // MM Proposta per eliminare i record doppi --- Inizio ---
                //.leftJoin(REEA_T_ADESIONE)
                //    .on(REEA_T_SOGGETTO.SOGGETTO_ID.eq(REEA_T_ADESIONE.SOGGETTO_ID))
                // MM Proposta per eliminare i record doppi ---  Fine  ---
                .leftJoin(REEA_R_SOGGETTO_ESENZIONE)
                    .on(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
                .leftJoin(REEA_D_ESENZIONE)
                    .on(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_HMAC.eq(
                            field("reea.text_to_hmac(cast({0} as text), cast({1} as text))",
                                  String.class,
                                  REEA_D_ESENZIONE.ESENZIONE_ID,
                                  val("16<odcc8!")
                            )
                    ))
                .leftJoin(REEA_T_REGISTRO)
                    .on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
                .leftJoin(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                    .on(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
                .leftJoin(REEA_T_REGISTRO_SPRESAL_ESITI)
                    .on(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
                .leftJoin(REEA_T_REGISTRO_INAIL)
                    .on(REEA_T_REGISTRO_INAIL.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
                .leftJoin(REEA_T_REGISTRO_PDL_AMIANTO)
                    .on(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
                .leftJoin(REEA_T_ESPOSIZIONE)
                    .on(REEA_T_ESPOSIZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
                .leftJoin(REEA_D_SOGGETTO_STATO).on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
                // MM Proposta per eliminare i record doppi --- Inizio ---
                .leftJoin(REEA_T_ADESIONE)
                .on(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
                .and(REEA_T_ADESIONE.AZIENDA_DESC.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA))
                .and(REEA_T_ADESIONE.AZIENDA_COMUNE_COD.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD))
                .and(REEA_T_ADESIONE.AZIENDA_COMUNE_DESC.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC))
                .and(REEA_T_ADESIONE.AZIENDA_CAP.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP))
                .and(REEA_T_ADESIONE.AZIENDA_PROVINCIA.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA))
                .and(REEA_T_ADESIONE.MANSIONE.eq(REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE))
                .and(REEA_T_ADESIONE.ESPOSIZIONE_INIZIO.eq(
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO.cast(LocalDate.class)
                ))
                .and(REEA_T_ADESIONE.ESPOSIZIONE_FINE.eq(
                        REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE.cast(LocalDate.class)
                ))
                // MM Proposta per eliminare i record doppi ---  Fine  ---
                .where(REEA_T_SOGGETTO.DATA_CANCELLAZIONE.isNull().and(condition))
//                    .and(includiSenzaAdesione ? noCondition() : REEA_T_ADESIONE.SOGGETTO_ID.isNotNull()))
                .fetchInto(ExportDTO.class);
        
        return risultati.stream()
                .filter(dto -> isValidoPerExport(dto.getSoggettoId(), profiloUtente))
                .toList();
    }
    
    
    @Override
    public Map<String, List<String>> buildCampiMascheratiMap() {
        Map<String, List<String>> map = new HashMap<>();

        dsl.selectFrom(REEA_D_CAMPO_MASCHERATO)
           .fetch()
           .forEach(r -> {
               String cod  = r.get(REEA_D_CAMPO_MASCHERATO.CAMPO_MASCHERATO_COD);
               String desc = r.get(REEA_D_CAMPO_MASCHERATO.CAMPO_MASCHERATO_DESC);

               if (cod == null || desc == null) return;

               // "INAIL1" --> "INAIL"
               String chiave = cod.replaceAll("\\d+$", "");

               map.computeIfAbsent(chiave, k -> new ArrayList<>()).add(desc);
           });

        return map;
    }
    
    
    @Override
    public Set<String> findCampiMascheratiCod() {
        return dsl.select(REEA_D_CAMPO_MASCHERATO.CAMPO_MASCHERATO_COD)
                .from(REEA_D_CAMPO_MASCHERATO)
                .where(REEA_D_CAMPO_MASCHERATO.CAMPO_MASCHERATO_COD.isNotNull())
                .fetchSet(REEA_D_CAMPO_MASCHERATO.CAMPO_MASCHERATO_COD);
    }


	@Override
	public List<String[]> findAnagraficheConAura() {

	    Field<String> decryptField = DSL.field(
	        "pgp_sym_decrypt({0}, {1})",
	        String.class,
	        REEA_T_SOGGETTO.ID_AURA_CIFRATO,
	        DSL.val("16<odcc8!")
	    );

	    return dsl
	        .select(
	            REEA_T_SOGGETTO.SOGGETTO_ID,
	            decryptField
	        )
	        .from(REEA_T_SOGGETTO)
	        .where(REEA_T_SOGGETTO.ID_AURA_CIFRATO.isNotNull())
	        .fetch(record -> new String[] {
	            record.get(REEA_T_SOGGETTO.SOGGETTO_ID).toString(),
	            record.get(decryptField)
	        });
	}


	@Override
	public Integer recuperaScarti(Integer fileId) {
	    return dsl
	            .selectCount()
	            .from(REEA_L_FILE_SCARICO_ERRORE)
	            .where(REEA_L_FILE_SCARICO_ERRORE.FILE_ID.eq(fileId))
	            .fetchOne(0, Integer.class);
	}


	@Override
	public void chiudiTuttiRecordTabAdesioneByFile(Integer fileId) {
    		dsl.update(REEA_T_ADESIONE)
    				.set(REEA_T_ADESIONE.VALIDITA_FINE, DSL.currentLocalDateTime())
    				.where(REEA_T_ADESIONE.FILE_ID.eq(fileId))
    				.execute();	
	}
    
    
}

