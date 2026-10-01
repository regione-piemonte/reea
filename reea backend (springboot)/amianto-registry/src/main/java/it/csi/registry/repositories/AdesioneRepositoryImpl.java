package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaDAsl.REEA_D_ASL;
import static it.csi.registry.jooq.tables.ReeaDEsenzione.REEA_D_ESENZIONE;
import static it.csi.registry.jooq.tables.ReeaDSoggettoStato.REEA_D_SOGGETTO_STATO;
import static it.csi.registry.jooq.tables.ReeaRSoggettoEsenzione.REEA_R_SOGGETTO_ESENZIONE;
import static it.csi.registry.jooq.tables.ReeaTAdesione.REEA_T_ADESIONE;
import static it.csi.registry.jooq.tables.ReeaTEsposizione.REEA_T_ESPOSIZIONE;
import static it.csi.registry.jooq.tables.ReeaTRegistro.REEA_T_REGISTRO;
import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalAnamnesi.REEA_T_REGISTRO_SPRESAL_ANAMNESI;
import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalEsiti.REEA_T_REGISTRO_SPRESAL_ESITI;
import static it.csi.registry.jooq.tables.ReeaTSoggetto.REEA_T_SOGGETTO;
import static it.csi.registry.jooq.tables.ReeaTRegistroPdlAmianto.REEA_T_REGISTRO_PDL_AMIANTO;
import static it.csi.registry.jooq.tables.ReeaTRegistroInail.REEA_T_REGISTRO_INAIL;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.val;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectConditionStep;
import org.jooq.SelectWhereStep;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import it.csi.registry.api.controllers.SpresalController;
import it.csi.registry.model.AdesioneDTO;
import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.ExportDTO;
import it.csi.registry.services.TracciaElaborazioneService;
import it.csi.registry.util.ErroreImportUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class AdesioneRepositoryImpl implements AdesioneRepository {

    private final DSLContext dsl;
    private final TracciaElaborazioneRepository tracciaElaborazioneRepository;
    private final TracciaElaborazioneService tracciaElaborazioneService;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdesioneRepositoryImpl.class);

    public AdesioneRepositoryImpl(DSLContext dsl, TracciaElaborazioneRepository tracciaElaborazioneRepository, TracciaElaborazioneService tracciaElaborazioneService) {
        this.dsl = dsl;
        this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
    }

    @Override
    public void deleteRecordNonProcessati() {
        dsl.deleteFrom(REEA_T_ADESIONE)
                .where(REEA_T_ADESIONE.SOGGETTO_ID.isNull())
                .execute();
    }
    
    
//    @Override
//    public List<AdesioneDTO> listaRecordAdesioni(Integer fileId) {
//
//        var query = dsl
//            .select(
//                REEA_T_ADESIONE.ADESIONE_ID,
//                REEA_T_ADESIONE.ADESIONE_COD,
//                REEA_T_ADESIONE.ADESIONE_DATA,
//                REEA_T_ADESIONE.CODICE_FISCALE,
//
//                // COGNOME - decrypt solo se non null
//                DSL.when(REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
//                    field("pgp_sym_decrypt({0}, {1})", String.class,
//                        REEA_T_ADESIONE.COGNOME_CIFRATO,
//                        val("16<odcc8!"))
//                ).otherwise((String) null).as("COGNOME_CHIARO"),
//
//                // NOME - decrypt solo se non null
//                DSL.when(REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
//                    field("pgp_sym_decrypt({0}, {1})", String.class,
//                        REEA_T_ADESIONE.NOME_CIFRATO,
//                        val("16<odcc8!"))
//                ).otherwise((String) null).as("NOME_CHIARO"),
//
//                REEA_T_ADESIONE.NASCITA_DATA,
//                REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC,
//                REEA_T_ADESIONE.NASCITA_COMUNE_DESC,
//                REEA_T_ADESIONE.UTENTE_CREAZIONE,
//
//                // TESSERA_TEAM - decrypt solo se non null
//                DSL.when(REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
//                    field("pgp_sym_decrypt({0}, {1})", String.class,
//                        REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA,
//                        val("16<odcc8!"))
//                ).otherwise((String) null).as("TESSERA_TEAM_CHIARO"),
//
//                // ID_AURA - decrypt solo se non null
//                DSL.when(REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
//                    field("pgp_sym_decrypt({0}, {1})", String.class,
//                        REEA_T_ADESIONE.ID_AURA_CIFRATO,
//                        val("16<odcc8!"))
//                ).otherwise((String) null).as("ID_AURA_CHIARO"),
//
//                REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC,
//                REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC,
//                REEA_T_ADESIONE.DOMICILIO_COMUNE_COD,
//                REEA_T_ADESIONE.DOMICILIO_CAP,
//
//                // EMAIL - decrypt solo se non null
//                DSL.when(REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
//                    field("pgp_sym_decrypt({0}, {1})", String.class,
//                        REEA_T_ADESIONE.EMAIL_CIFRATA,
//                        val("16<odcc8!"))
//                ).otherwise((String) null).as("EMAIL_CHIARO"),
//
//                // TELEFONO - decrypt solo se non null
//                DSL.when(REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
//                    field("pgp_sym_decrypt({0}, {1})", String.class,
//                        REEA_T_ADESIONE.TELEFONO_CIFRATO,
//                        val("16<odcc8!"))
//                ).otherwise((String) null).as("TELEFONO_CHIARO"),
//
//                REEA_T_ADESIONE.DOMICILIO_ASL_COD,
//                REEA_T_ADESIONE.DOMICILIO_ASL_DESC,
//                REEA_T_ADESIONE.AZIENDA_COD,
//                REEA_T_ADESIONE.RESIDENZA_ASL_COD,
//                REEA_T_ADESIONE.RESIDENZA_ASL_DESC,
//                REEA_T_ADESIONE.ESPOSIZIONE_INIZIO,
//                REEA_T_ADESIONE.ESPOSIZIONE_FINE,
//                REEA_T_ADESIONE.AZIENDA_DESC,
//                REEA_T_ADESIONE.AZIENDA_COMUNE_COD,
//                REEA_T_ADESIONE.AZIENDA_COMUNE_DESC,
//                REEA_T_ADESIONE.AZIENDA_CAP,
//                REEA_T_ADESIONE.AZIENDA_PROVINCIA,
//                REEA_T_ADESIONE.MANSIONE,
//                REEA_T_ADESIONE.FILE_ID
//            )
//            .from(REEA_T_ADESIONE)
//            .where(REEA_T_ADESIONE.SOGGETTO_ID.isNull())
//                .and(fileId != null ? REEA_T_ADESIONE.FILE_ID.eq(fileId) : DSL.noCondition());
//
//
//        // fetch finale
//        List<AdesioneDTO> result = query.stream().map(r -> {
//
//            AdesioneDTO row = new AdesioneDTO();
//            row.setAdesioneId(Long.valueOf(r.get(REEA_T_ADESIONE.ADESIONE_ID)));
//            row.setAdesioneCod(r.get(REEA_T_ADESIONE.ADESIONE_COD));
//            var adesioneDataVal = r.get(REEA_T_ADESIONE.ADESIONE_DATA);
//            row.setAdesioneData(adesioneDataVal != null ? adesioneDataVal.toLocalDate() : null);
//            row.setCodiceFiscale(r.get(REEA_T_ADESIONE.CODICE_FISCALE));
//            row.setNascitaData(r.get(REEA_T_ADESIONE.NASCITA_DATA));
//            row.setNome(r.get("NOME_CHIARO") != null ? r.get("NOME_CHIARO").toString().toUpperCase() : null);
//            row.setCognome(r.get("COGNOME_CHIARO") != null ? r.get("COGNOME_CHIARO").toString().toUpperCase() : null);
//            row.setNascitaProvinciaDesc(r.get(REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC));
//            row.setNascitaComuneDesc(r.get(REEA_T_ADESIONE.NASCITA_COMUNE_DESC));
//            row.setTesseraTeam(r.get("TESSERA_TEAM_CHIARO") != null ? r.get("TESSERA_TEAM_CHIARO").toString() : null);
//            row.setIdAura(r.get("ID_AURA_CHIARO") != null ? r.get("ID_AURA_CHIARO").toString() : null);
//            row.setDomicilioProvinciaDesc(r.get(REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC));
//            row.setDomicilioComuneDesc(r.get(REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC));
//            row.setDomicilioComuneCod(r.get(REEA_T_ADESIONE.DOMICILIO_COMUNE_COD));
//            row.setDomicilioCap(r.get(REEA_T_ADESIONE.DOMICILIO_CAP));
//            row.setEmail(r.get("EMAIL_CHIARO") != null ? r.get("EMAIL_CHIARO").toString() : null);
//            row.setTelefono(r.get("TELEFONO_CHIARO") != null ? r.get("TELEFONO_CHIARO").toString() : null);
//            row.setDomicilioAslCod(r.get(REEA_T_ADESIONE.DOMICILIO_ASL_COD));
//            row.setDomicilioAslDesc(r.get(REEA_T_ADESIONE.DOMICILIO_ASL_DESC));
//            row.setAziendaCod(r.get(REEA_T_ADESIONE.AZIENDA_COD));
//            row.setResidenzaAslCod(r.get(REEA_T_ADESIONE.RESIDENZA_ASL_COD));
//            row.setResidenzaAslDesc(r.get(REEA_T_ADESIONE.RESIDENZA_ASL_DESC));
//            row.setEsposizioneInizio(r.get(REEA_T_ADESIONE.ESPOSIZIONE_INIZIO));
//            row.setEsposizioneFine(r.get(REEA_T_ADESIONE.ESPOSIZIONE_FINE));
//            row.setEsposizioneAzienda(r.get(REEA_T_ADESIONE.AZIENDA_DESC));
//            row.setEsposizioneAziendaComuneCod(r.get(REEA_T_ADESIONE.AZIENDA_COMUNE_COD));
//            row.setEsposizioneAziendaComuneDesc(r.get(REEA_T_ADESIONE.AZIENDA_COMUNE_DESC));
//            row.setEsposizioneAziendaCap(r.get(REEA_T_ADESIONE.AZIENDA_CAP));
//            row.setEsposizioneAziendaProvincia(r.get(REEA_T_ADESIONE.AZIENDA_PROVINCIA));
//            row.setEsposizioneMansione(r.get(REEA_T_ADESIONE.MANSIONE));
//            row.setFileId(r.get(REEA_T_ADESIONE.FILE_ID));
//            row.setFonteId(2);
//            row.setUtenteCreazione(r.get(REEA_T_ADESIONE.UTENTE_CREAZIONE));
//
//            return row;
//        })
//        .collect(Collectors.toList());
//
//        return result;
//    }
    
    @Override
    public List<AdesioneDTO> listaRecordAdesioni(Integer fileId) {

        var query = dsl
                .select(
                        REEA_T_ADESIONE.ADESIONE_ID,
                        REEA_T_ADESIONE.ADESIONE_COD,
                        REEA_T_ADESIONE.ADESIONE_DATA,
                        REEA_T_ADESIONE.CODICE_FISCALE,

                        DSL.when(REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
                                field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_T_ADESIONE.COGNOME_CIFRATO,
                                        val("16<odcc8!"))
                        ).otherwise((String) null).as("COGNOME_CHIARO"),

                        DSL.when(REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
                                field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_T_ADESIONE.NOME_CIFRATO,
                                        val("16<odcc8!"))
                        ).otherwise((String) null).as("NOME_CHIARO"),

                        REEA_T_ADESIONE.NASCITA_DATA,
                        REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC,
                        REEA_T_ADESIONE.NASCITA_COMUNE_DESC,
                        REEA_T_ADESIONE.UTENTE_CREAZIONE,

                        DSL.when(REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
                                field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA,
                                        val("16<odcc8!"))
                        ).otherwise((String) null).as("TESSERA_TEAM_CHIARO"),

                        DSL.when(REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
                                field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_T_ADESIONE.ID_AURA_CIFRATO,
                                        val("16<odcc8!"))
                        ).otherwise((String) null).as("ID_AURA_CHIARO"),

                        REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC,
                        REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC,
                        REEA_T_ADESIONE.DOMICILIO_COMUNE_COD,
                        REEA_T_ADESIONE.DOMICILIO_CAP,

                        DSL.when(REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
                                field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_T_ADESIONE.EMAIL_CIFRATA,
                                        val("16<odcc8!"))
                        ).otherwise((String) null).as("EMAIL_CHIARO"),

                        DSL.when(REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
                                field("pgp_sym_decrypt({0}, {1})", String.class,
                                        REEA_T_ADESIONE.TELEFONO_CIFRATO,
                                        val("16<odcc8!"))
                        ).otherwise((String) null).as("TELEFONO_CHIARO"),

                        REEA_T_ADESIONE.DOMICILIO_ASL_COD,
                        REEA_T_ADESIONE.DOMICILIO_ASL_DESC,
                        REEA_T_ADESIONE.AZIENDA_COD,
                        REEA_T_ADESIONE.RESIDENZA_ASL_COD,
                        REEA_T_ADESIONE.RESIDENZA_ASL_DESC,
                        REEA_T_ADESIONE.ESPOSIZIONE_INIZIO,
                        REEA_T_ADESIONE.ESPOSIZIONE_FINE,
                        REEA_T_ADESIONE.AZIENDA_DESC,
                        REEA_T_ADESIONE.AZIENDA_COMUNE_COD,
                        REEA_T_ADESIONE.AZIENDA_COMUNE_DESC,
                        REEA_T_ADESIONE.AZIENDA_CAP,
                        REEA_T_ADESIONE.AZIENDA_PROVINCIA,
                        REEA_T_ADESIONE.MANSIONE,
                        REEA_T_ADESIONE.FILE_ID
                )
                .from(REEA_T_ADESIONE)
                .where(REEA_T_ADESIONE.SOGGETTO_ID.isNull().and(REEA_T_ADESIONE.VALIDITA_FINE.isNull()))
                .and(fileId != null ? REEA_T_ADESIONE.FILE_ID.eq(fileId) : DSL.noCondition());

        List<AdesioneDTO> result = new ArrayList<>();

        for (var r : query.fetch()) {
            try {
                AdesioneDTO row = new AdesioneDTO();

                Integer adesioneId = r.get(REEA_T_ADESIONE.ADESIONE_ID);
                row.setAdesioneId(adesioneId != null ? Long.valueOf(adesioneId) : null);

                row.setAdesioneCod(r.get(REEA_T_ADESIONE.ADESIONE_COD));

                var adesioneDataVal = r.get(REEA_T_ADESIONE.ADESIONE_DATA);
                row.setAdesioneData(adesioneDataVal != null ? adesioneDataVal.toLocalDate() : null);

                row.setCodiceFiscale(r.get(REEA_T_ADESIONE.CODICE_FISCALE));
                row.setNascitaData(r.get(REEA_T_ADESIONE.NASCITA_DATA));

                String nomeChiaro = r.get("NOME_CHIARO", String.class);
                row.setNome(nomeChiaro != null ? nomeChiaro.toUpperCase() : null);

                String cognomeChiaro = r.get("COGNOME_CHIARO", String.class);
                row.setCognome(cognomeChiaro != null ? cognomeChiaro.toUpperCase() : null);

                row.setNascitaProvinciaDesc(r.get(REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC));
                row.setNascitaComuneDesc(r.get(REEA_T_ADESIONE.NASCITA_COMUNE_DESC));

                row.setTesseraTeam(r.get("TESSERA_TEAM_CHIARO", String.class));
                row.setIdAura(r.get("ID_AURA_CHIARO", String.class));
                row.setDomicilioProvinciaDesc(r.get(REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC));
                row.setDomicilioComuneDesc(r.get(REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC));
                row.setDomicilioComuneCod(r.get(REEA_T_ADESIONE.DOMICILIO_COMUNE_COD));
                row.setDomicilioCap(r.get(REEA_T_ADESIONE.DOMICILIO_CAP));
                row.setEmail(r.get("EMAIL_CHIARO", String.class));
                row.setTelefono(r.get("TELEFONO_CHIARO", String.class));
                row.setDomicilioAslCod(r.get(REEA_T_ADESIONE.DOMICILIO_ASL_COD));
                row.setDomicilioAslDesc(r.get(REEA_T_ADESIONE.DOMICILIO_ASL_DESC));
                row.setAziendaCod(r.get(REEA_T_ADESIONE.AZIENDA_COD));
                row.setResidenzaAslCod(r.get(REEA_T_ADESIONE.RESIDENZA_ASL_COD));
                row.setResidenzaAslDesc(r.get(REEA_T_ADESIONE.RESIDENZA_ASL_DESC));
                row.setEsposizioneInizio(r.get(REEA_T_ADESIONE.ESPOSIZIONE_INIZIO));
                row.setEsposizioneFine(r.get(REEA_T_ADESIONE.ESPOSIZIONE_FINE));
                row.setEsposizioneAzienda(r.get(REEA_T_ADESIONE.AZIENDA_DESC));
                row.setEsposizioneAziendaComuneCod(r.get(REEA_T_ADESIONE.AZIENDA_COMUNE_COD));
                row.setEsposizioneAziendaComuneDesc(r.get(REEA_T_ADESIONE.AZIENDA_COMUNE_DESC));
                row.setEsposizioneAziendaCap(r.get(REEA_T_ADESIONE.AZIENDA_CAP));
                row.setEsposizioneAziendaProvincia(r.get(REEA_T_ADESIONE.AZIENDA_PROVINCIA));
                row.setEsposizioneMansione(r.get(REEA_T_ADESIONE.MANSIONE));
                row.setFileId(r.get(REEA_T_ADESIONE.FILE_ID));
                row.setFonteId(2);
                row.setUtenteCreazione(r.get(REEA_T_ADESIONE.UTENTE_CREAZIONE));

                result.add(row);

            } catch (Exception e) {
                System.out.println("Errore nel mapping del record ADESIONE con ADESIONE_ID={}" + 
                        r.get(REEA_T_ADESIONE.ADESIONE_ID) + e);
            }
        }

        return result;
    }


    @Override
    public Set<String> getProcessedAdesioneCods() {
        return new HashSet<>(
                dsl.select(REEA_T_ADESIONE.ADESIONE_COD)
                        .from(REEA_T_ADESIONE)
                        .where(REEA_T_ADESIONE.SOGGETTO_ID.isNotNull().or(REEA_T_ADESIONE.VALIDITA_FINE.isNotNull()))
                        .fetch(REEA_T_ADESIONE.ADESIONE_COD)
        );
    }

    @Override
    public void scaricoDatiExcelInTAdesione(List<AdesioneDTO> rows, Integer fileId, String utenteCreazione) {
        for (AdesioneDTO dto : rows) {
            try {
                dsl.insertInto(REEA_T_ADESIONE)
                   .set(REEA_T_ADESIONE.ADESIONE_COD, dto.getAdesioneCod())
                   .set(REEA_T_ADESIONE.ADESIONE_DATA,
                        dto.getAdesioneData() != null ? dto.getAdesioneData().atStartOfDay() : null)
                   .set(REEA_T_ADESIONE.CODICE_FISCALE, dto.getCodiceFiscale())
                   .set(REEA_T_ADESIONE.COGNOME_CIFRATO,
                        DSL.function("pgp_sym_encrypt",
                                     SQLDataType.BLOB,
                                     DSL.val(dto.getCognome() != null
                                                 ? dto.getCognome().toUpperCase()
                                                 : null,
                                             SQLDataType.VARCHAR),
                                     DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                   .set(REEA_T_ADESIONE.NOME_CIFRATO,
                        DSL.function("pgp_sym_encrypt",
                                     SQLDataType.BLOB,
                                     DSL.val(dto.getNome() != null
                                                 ? dto.getNome().toUpperCase()
                                                 : null,
                                             SQLDataType.VARCHAR),
                                     DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                   .set(REEA_T_ADESIONE.NASCITA_DATA, dto.getNascitaData())
                   .set(REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC, dto.getNascitaProvinciaDesc())
                   .set(REEA_T_ADESIONE.NASCITA_COMUNE_DESC, dto.getNascitaComuneDesc())
                   .set(REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC, dto.getDomicilioProvinciaDesc())
                   .set(REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC, dto.getDomicilioComuneDesc())
                   .set(REEA_T_ADESIONE.DOMICILIO_COMUNE_COD, dto.getDomicilioComuneCod())
                   .set(REEA_T_ADESIONE.DOMICILIO_CAP, dto.getDomicilioCap())
                   .set(REEA_T_ADESIONE.DOMICILIO_ASL_COD, dto.getDomicilioAslCod())
                   .set(REEA_T_ADESIONE.DOMICILIO_ASL_DESC, dto.getDomicilioAslDesc())
                   .set(REEA_T_ADESIONE.AZIENDA_COD, dto.getAziendaCod())
                   .set(REEA_T_ADESIONE.RESIDENZA_ASL_COD, dto.getResidenzaAslCod())
                   .set(REEA_T_ADESIONE.RESIDENZA_ASL_DESC, dto.getResidenzaAslDesc())
                   .set(REEA_T_ADESIONE.UTENTE_CREAZIONE, utenteCreazione)
                   .set(REEA_T_ADESIONE.DATA_CREAZIONE, DSL.currentLocalDateTime())
                   .set(REEA_T_ADESIONE.VALIDITA_INIZIO, DSL.currentLocalDateTime())
                   .set(REEA_T_ADESIONE.ESPOSIZIONE_INIZIO, dto.getEsposizioneInizio())
                   .set(REEA_T_ADESIONE.ESPOSIZIONE_FINE, dto.getEsposizioneFine())
                   .set(REEA_T_ADESIONE.AZIENDA_DESC, dto.getEsposizioneAzienda())
                   .set(REEA_T_ADESIONE.AZIENDA_COMUNE_COD, dto.getEsposizioneAziendaComuneCod())
                   .set(REEA_T_ADESIONE.AZIENDA_COMUNE_DESC, dto.getEsposizioneAziendaComuneDesc())
                   .set(REEA_T_ADESIONE.AZIENDA_CAP, dto.getEsposizioneAziendaCap())
                   .set(REEA_T_ADESIONE.AZIENDA_PROVINCIA, dto.getEsposizioneAziendaProvincia())
                   .set(REEA_T_ADESIONE.MANSIONE, dto.getEsposizioneMansione())
                   .set(REEA_T_ADESIONE.FILE_ID, fileId)
                   .set(REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA,
                        DSL.function("pgp_sym_encrypt",
                                     SQLDataType.BLOB,
                                     DSL.val(dto.getTesseraTeam(),
                                             SQLDataType.VARCHAR),
                                     DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                   .set(REEA_T_ADESIONE.ID_AURA_CIFRATO,
                        DSL.function("pgp_sym_encrypt",
                                     SQLDataType.BLOB,
                                     DSL.val(dto.getIdAura(),
                                             SQLDataType.VARCHAR),
                                     DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                   .set(REEA_T_ADESIONE.EMAIL_CIFRATA,
                        DSL.function("pgp_sym_encrypt",
                                     SQLDataType.BLOB,
                                     DSL.val(dto.getEmail(),
                                             SQLDataType.VARCHAR),
                                     DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                   .set(REEA_T_ADESIONE.TELEFONO_CIFRATO,
                        DSL.function("pgp_sym_encrypt",
                                     SQLDataType.BLOB,
                                     DSL.val(dto.getTelefono(),
                                             SQLDataType.VARCHAR),
                                     DSL.val("16<odcc8!", SQLDataType.VARCHAR)))
                   .execute();
//            } catch (Exception e) {
////                throw new RuntimeException(
////                        "Errore inserendo CF=" + dto.getCodiceFiscale(), e);
//            	System.out.println("Errore inserendo CF=" + dto.getCodiceFiscale() + " " + e.getMessage());
//            }
//        }
//    }
            } catch (Exception e) {
	        	String descrizioneErrore = ErroreImportUtility.buildErroreDescrizione(e);

	            System.out.println("Errore inserendo CF=" +dto.getCodiceFiscale() != null ? dto.getCodiceFiscale() : null + " " + descrizioneErrore);

	            try {
	            	tracciaElaborazioneService.inserisciScarto(
                    		fileId,
                            "ERR_ELABORAZIONE",
                            descrizioneErrore,
                            null,
                            utenteCreazione,
                            dto.getCognome(),
                            dto.getNome(),
                            dto.getNascitaData(),
                            dto.getSesso(),
                            dto.getCodiceFiscale() != null ? dto.getCodiceFiscale() : null,
	                        "REEA_T_ADESIONI",
	                        dto.getFileId() != null ? dto.getFileId() : 0
	                );
	            } catch (Exception ex) {
//	                ex.printStackTrace();
	            	LOGGER.error("Errore inserendo CF: ", dto.getCodiceFiscale() != null ? dto.getCodiceFiscale() : null, ex);
	            }
	        }
	    }
	}


    @Override
    public Integer aggiornaAdesioniIdSoggetto(AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione) {

    	Integer ris = 0;

    	try {
    		ris = dsl.update(REEA_T_ADESIONE)
    				.set(REEA_T_ADESIONE.SOGGETTO_ID, dto.getSoggettoId().intValue())
    				.where(REEA_T_ADESIONE.ADESIONE_COD.eq(dto.getAdesioneCod()))
    				.execute();

    		if(dto.getAdesioneId() != null)
    			tracciaElaborazioneRepository.inserisciFileImpatto(
    					dsl,
    					elaborazioneId,
    					"REEA_T_ADESIONE",
    					dto.getAdesioneId().intValue(),
    					tipoOperazione,
    					utente
    					);

    		return ris;

    	} catch (Exception e) {

    		try {
    			tracciaElaborazioneService.inserisciErroreRiga(
//    					dsl,
    					elaborazioneId,
    					"ERR_INSERIMENTO",
    					"aggiornaAdesioniIdSoggetto ha restituito " + ris + " per CF " + dto.getCodiceFiscale(),
    					null,
    					dto.getUtenteCreazione(),
    					dto.getCognome(),
    					dto.getNome(),
    					dto.getNascitaData(),
    					dto.getSesso(),
    					dto.getCodiceFiscale(),
    					"REEA_T_ADESIONE",
    					dto.getAdesioneId().intValue()
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
    public List<AdesioneDTO> findBySoggettoId(Long soggettoId) {
        return dsl.select(
                        REEA_T_ADESIONE.ADESIONE_ID,
                        REEA_T_ADESIONE.ADESIONE_COD,
                        REEA_T_ADESIONE.ADESIONE_DATA,
                        REEA_T_ADESIONE.CODICE_FISCALE,

                        // COGNOME - null safe
                        DSL.when(
                            REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                REEA_T_ADESIONE.COGNOME_CIFRATO,
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("cognome"),

                        // NOME - null safe
                        DSL.when(
                            REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                REEA_T_ADESIONE.NOME_CIFRATO,
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("nome"),

                        REEA_T_ADESIONE.NASCITA_DATA,
                        REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC,
                        REEA_T_ADESIONE.NASCITA_COMUNE_DESC,

                        // TESSERA_TEAM - null safe
                        DSL.when(
                            REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA,
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("tessera_team"),

                        // ID_AURA - null safe
                        DSL.when(
                            REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                REEA_T_ADESIONE.ID_AURA_CIFRATO,
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("id_aura"),

                        REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC,
                        REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC,
                        REEA_T_ADESIONE.DOMICILIO_COMUNE_COD,
                        REEA_T_ADESIONE.DOMICILIO_CAP,

                        // EMAIL - null safe
                        DSL.when(
                            REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                REEA_T_ADESIONE.EMAIL_CIFRATA,
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("email"),

                        // TELEFONO - null safe
                        DSL.when(
                            REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
                                REEA_T_ADESIONE.TELEFONO_CIFRATO,
                                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
                        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("telefono"),

                        REEA_T_ADESIONE.DOMICILIO_ASL_COD,
                        REEA_T_ADESIONE.DOMICILIO_ASL_DESC,
                        REEA_T_ADESIONE.RESIDENZA_ASL_COD,
                        REEA_T_ADESIONE.RESIDENZA_ASL_DESC,
                        REEA_T_ADESIONE.AZIENDA_COD
                )
                .from(REEA_T_ADESIONE)
                .where(REEA_T_ADESIONE.SOGGETTO_ID.eq(soggettoId.intValue()))
                .fetch(r -> {
                    AdesioneDTO dto = new AdesioneDTO();
                    Integer id = r.get(REEA_T_ADESIONE.ADESIONE_ID);
                    dto.setAdesioneId(id != null ? id.longValue() : null);
                    dto.setAdesioneCod(r.get(REEA_T_ADESIONE.ADESIONE_COD));
                    LocalDateTime adData = r.get(REEA_T_ADESIONE.ADESIONE_DATA);
                    dto.setAdesioneData(adData != null ? adData.toLocalDate() : null);
                    dto.setCodiceFiscale(r.get(REEA_T_ADESIONE.CODICE_FISCALE));
                    dto.setCognome(r.get("cognome", String.class));
                    dto.setNome(r.get("nome", String.class));
                    dto.setNascitaData(r.get(REEA_T_ADESIONE.NASCITA_DATA));
                    dto.setNascitaProvinciaDesc(r.get(REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC));
                    dto.setNascitaComuneDesc(r.get(REEA_T_ADESIONE.NASCITA_COMUNE_DESC));
                    dto.setTesseraTeam(r.get("tessera_team", String.class));
                    dto.setIdAura(r.get("id_aura", String.class));
                    dto.setDomicilioProvinciaDesc(r.get(REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC));
                    dto.setDomicilioComuneDesc(r.get(REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC));
                    dto.setDomicilioComuneCod(r.get(REEA_T_ADESIONE.DOMICILIO_COMUNE_COD));
                    dto.setDomicilioCap(r.get(REEA_T_ADESIONE.DOMICILIO_CAP));
                    dto.setEmail(r.get("email", String.class));
                    dto.setTelefono(r.get("telefono", String.class));
                    dto.setDomicilioAslCod(r.get(REEA_T_ADESIONE.DOMICILIO_ASL_COD));
                    dto.setDomicilioAslDesc(r.get(REEA_T_ADESIONE.DOMICILIO_ASL_DESC));
                    dto.setResidenzaAslCod(r.get(REEA_T_ADESIONE.RESIDENZA_ASL_COD));
                    dto.setResidenzaAslDesc(r.get(REEA_T_ADESIONE.RESIDENZA_ASL_DESC));
                    dto.setAziendaCod(r.get(REEA_T_ADESIONE.AZIENDA_COD));
                    return dto;
                });
    }
    
    
//    @Override
//    public List<ExportDTO> recuperaPreadesioniPerExport(List<Long> soggettoIds, Integer assistenzaAslId) {
//
//    	// Filtro assistenza_asl_id
//        Condition condAssistenzaAsl = noCondition();
//       
//        if (assistenzaAslId != null) {
//            condAssistenzaAsl        = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//        }
//    	
//        // ── REEA_T_ADESIONE - null safe ──────────────────────────────────────────
//
//        Field<String> preNomeChiaro = DSL.when(
//            REEA_T_ADESIONE.NOME_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_ADESIONE.NOME_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneNome");
//
//        Field<String> preCognomeChiaro = DSL.when(
//            REEA_T_ADESIONE.COGNOME_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_ADESIONE.COGNOME_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneCognome");
//
//        Field<String> preTesseraTeamChiara = DSL.when(
//            REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneTesseraTeam");
//
//        Field<String> preIdAuraChiaro = DSL.when(
//            REEA_T_ADESIONE.ID_AURA_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_ADESIONE.ID_AURA_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneIdAura");
//
//        Field<String> preEmailChiara = DSL.when(
//            REEA_T_ADESIONE.EMAIL_CIFRATA.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_ADESIONE.EMAIL_CIFRATA,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneEmail");
//
//        Field<String> preTelefonoChiaro = DSL.when(
//            REEA_T_ADESIONE.TELEFONO_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_ADESIONE.TELEFONO_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("adesioneTelefono");
//
//        // ── REEA_T_SOGGETTO - null safe ──────────────────────────────────────────
//
//        Field<String> auraNomeChiaro = DSL.when(
//            REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_SOGGETTO.NOME_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraNome");
//
//        Field<String> auraCognomeChiaro = DSL.when(
//            REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_SOGGETTO.COGNOME_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraCognome");
//
//        Field<String> auraDomicilioIndirizzoChiaro = DSL.when(
//            REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraDomicilioIndirizzo");
//
//        Field<String> auraResidenzaIndirizzoChiaro = DSL.when(
//            REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraResidenzaIndirizzo");
//
//        Field<String> auraTesseraTeamChiara = DSL.when(
//            REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraTesseraTeam");
//
//        Field<String> auraIdAuraChiaro = DSL.when(
//            REEA_T_SOGGETTO.ID_AURA_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_SOGGETTO.ID_AURA_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraIdAura");
//
//        Field<String> auraEmailChiara = DSL.when(
//            REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraEmail");
//
//        Field<String> auraTelefonoChiaro = DSL.when(
//            REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO.isNotNull(),
//            DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO,
//                DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//        ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("auraTelefono");
//        
//        Field<String> emailChiara = DSL.when(
//                REEA_T_SOGGETTO.EMAIL_CIFRATA.isNotNull(),
//                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                    REEA_T_SOGGETTO.EMAIL_CIFRATA,
//                    DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//            ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("email");
//
//            Field<String> telefonoChiaro = DSL.when(
//                REEA_T_SOGGETTO.TELEFONO_CIFRATO.isNotNull(),
//                DSL.field("pgp_sym_decrypt({0}, {1})", String.class,
//                    REEA_T_SOGGETTO.TELEFONO_CIFRATO,
//                    DSL.val("16<odcc8!", SQLDataType.VARCHAR))
//            ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("telefono");
//
//        Field<String> soggettoIdHmac = DSL.field(
//            "reea.hmac_soggetto_id(cast({0} as int4), cast({1} as text))",
//            String.class,
//            REEA_T_SOGGETTO.SOGGETTO_ID,
//            DSL.val("16<odcc8!")
//        );
//
//        return dsl
//                .select(
//                        //PRE (da REEA_T_ADESIONE)
//                        REEA_T_ADESIONE.ADESIONE_COD.as("adesioneCodiceAdesione"),
//                        REEA_T_ADESIONE.ADESIONE_DATA.as("adesioneDataAdesione"),
//                        REEA_T_ADESIONE.CODICE_FISCALE.as("adesioneCodiceFiscale"),
//                        REEA_T_ADESIONE.ADESIONE_COD.as("codiceAdesione"),
//                        REEA_T_ADESIONE.ADESIONE_DATA.as("dataAdesione"),
//                        REEA_T_ADESIONE.CODICE_FISCALE.as("codiceFiscale"),
//                        preCognomeChiaro,
//                        preNomeChiaro,
//                        REEA_T_ADESIONE.NASCITA_DATA.as("adesioneDataDiNascita"),
//                        REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC.as("adesioneProvinciaDiNascita"),
//                        REEA_T_ADESIONE.NASCITA_COMUNE_DESC.as("adesioneComuneDiNascita"),
//                        preTesseraTeamChiara,
//                        preIdAuraChiaro,
//                        REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC.as("adesioneProvinciaDiDomicilio"),
//                        REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC.as("adesioneComuneDiDomicilio"),
//                        REEA_T_ADESIONE.DOMICILIO_COMUNE_COD.as("adesioneCodiceComuneIstatDiDomicilio"),
//                        REEA_T_ADESIONE.DOMICILIO_CAP.as("adesioneCapDiDomicilio"),
//                        preEmailChiara,
//                        preTelefonoChiaro,
//                        REEA_T_ADESIONE.DOMICILIO_ASL_COD.as("adesioneCodiceAslDiDomicilio"),
//                        REEA_T_ADESIONE.DOMICILIO_ASL_DESC.as("adesioneCslDiDomicilio"),
//                        REEA_T_ADESIONE.AZIENDA_COD.as("adesioneCodazi"),
//                        REEA_T_ADESIONE.RESIDENZA_ASL_COD.as("adesioneCodiceAslDiResidenza"),
//                        REEA_T_ADESIONE.RESIDENZA_ASL_DESC.as("adesioneAslDiResidenza"),
//                        REEA_T_ADESIONE.ESPOSIZIONE_INIZIO.as("adesioneDataInizioEsposizione"),
//                        REEA_T_ADESIONE.ESPOSIZIONE_FINE.as("adesioneDataFineEsposizione"),
//                        REEA_T_ADESIONE.AZIENDA_DESC.as("adesioneAzienda"),
//                        REEA_T_ADESIONE.AZIENDA_COMUNE_COD.as("adesioneCodiceComuneIstatAzienda"),
//                        REEA_T_ADESIONE.AZIENDA_COMUNE_DESC.as("adesioneComuneAzienda"),
//                        REEA_T_ADESIONE.AZIENDA_CAP.as("adesioneCapAzienda"),
//                        REEA_T_ADESIONE.AZIENDA_PROVINCIA.as("adesioneProvinciaAzienda"),
//                        REEA_T_ADESIONE.MANSIONE.as("adesioneMansione"),
//
//                        //AURA (da REEA_T_SOGGETTO)
//                        REEA_T_SOGGETTO.CODICE_FISCALE.as("auraCodiceFiscale"),
//                        REEA_T_SOGGETTO.DOMICILIO_ASL_ID.as("auraDomicilioAsl"),
//                        REEA_T_SOGGETTO.RESIDENZA_ASL_ID.as("auraResidenzaAsl"),
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("auraAssistenzaAsl"),
//                        auraNomeChiaro,
//                        auraCognomeChiaro,
//                        REEA_T_SOGGETTO.SESSO.as("auraSesso"),
//                        REEA_T_SOGGETTO.NASCITA_DATA.as("auraNascitaData"),
//                        REEA_T_SOGGETTO.NASCITA_COMUNE_COD.as("auraNascitaComuneCod"),
//                        REEA_T_SOGGETTO.NASCITA_COMUNE_DESC.as("auraNascitaComuneDesc"),
//                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_COD.as("auraNascitaProvinciaCod"),
//                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_DESC.as("auraNascitaProvinciaDesc"),
//                        REEA_T_SOGGETTO.NASCITA_STATO_COD.as("auraNascitaStatoCod"),
//                        REEA_T_SOGGETTO.NASCITA_STATO_DESC.as("auraNascitaStatoDesc"),
//                        REEA_T_SOGGETTO.CITTADINANZA_STATO_COD.as("auraCittadinanzaStatoCod"),
//                        REEA_T_SOGGETTO.CITTADINANZA_STATO_DESC.as("auraCittadinanzaStatoDesc"),
//                        REEA_T_SOGGETTO.DOMICILIO_COMUNE_COD.as("auraDomicilioComuneCod"),
//                        REEA_T_SOGGETTO.DOMICILIO_COMUNE_DESC.as("auraDomicilioComuneDesc"),
//                        REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_COD.as("auraDomicilioProvinciaCod"),
//                        REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_DESC.as("auraDomicilioProvinciaDesc"),
//                        REEA_T_SOGGETTO.DOMICILIO_STATO_COD.as("auraDomicilioStatoCod"),
//                        REEA_T_SOGGETTO.DOMICILIO_STATO_DESC.as("auraDomicilioStatoDesc"),
//                        REEA_T_SOGGETTO.DOMICILIO_CAP.as("auraDomicilioCap"),
//                        auraDomicilioIndirizzoChiaro,
//                        REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO.as("auraDomicilioNumeroCivico"),
//                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD.as("auraResidenzaComuneCod"),
//                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC.as("auraResidenzaComuneDesc"),
//                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD.as("auraResidenzaProvinciaCod"),
//                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC.as("auraResidenzaProvinciaDesc"),
//                        REEA_T_SOGGETTO.RESIDENZA_STATO_COD.as("auraResidenzaStatoCod"),
//                        REEA_T_SOGGETTO.RESIDENZA_STATO_DESC.as("auraResidenzaStatoDesc"),
//                        REEA_T_SOGGETTO.RESIDENZA_CAP.as("auraResidenzaCap"),
//                        auraResidenzaIndirizzoChiaro,
//                        REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO.as("auraResidenzaNumeroCivico"),
//                        auraTesseraTeamChiara,
//                        auraIdAuraChiaro,
//                        auraEmailChiara,
//                        auraTelefonoChiaro,
//                        REEA_T_SOGGETTO.DATA_DECESSO.as("auraDataDecesso"),
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE.as("auraAssistenzaAslFine"),
//                        emailChiara,
//                        telefonoChiaro,
//
//                        // ESENZIONI
//                        REEA_D_ESENZIONE.ESENZIONE_COD.as("codEsenzione"),
//                        REEA_D_ESENZIONE.ESENZIONE_DESC.as("descEsenzione"),
//                        REEA_D_ESENZIONE.DIAGNOSI_COD.as("codDiagnosi"),
//                        REEA_D_ESENZIONE.DIAGNOSI_DESC.as("descDiagnosi"),
//                        REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_EMISSIONE.as("dataEmissione"),
//                        REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_SCADENZA.as("dataScadenza"),
//
//                        //ANAMNESI (da REEA_T_REGISTRO_SPRESAL_ANAMNESI)
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.as("anamnesiCodiceFiscale"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA.as("anamnesiIdAura"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA.as("anamnesiDataIntervista"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE.as("anamnesiNominativoIntervistatore"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE.as("anamnesiFumatore"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE.as("anamnesiSigarette"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI.as("anamnesiSigaretteAnni"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO.as("anamnesiSigaretteEtaInizio"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE.as("anamnesiSigaretteFumaAttualmente"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE.as("anamnesiSigaretteEtaFine"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE.as("anamnesiSigaretteDie"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI.as("anamnesiSigari"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI.as("anamnesiSigariAnni"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO.as("anamnesiSigariEtaInizio"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE.as("anamnesiSigariFumaAttualmente"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE.as("anamnesiSigariEtaFine"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE.as("anamnesiSigariDie"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA.as("anamnesiPipa"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI.as("anamnesiPipaAnni"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO.as("anamnesiPipaEtaInizio"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE.as("anamnesiPipaFumaAttualmente"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE.as("anamnesiPipaEtaFine"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE.as("anamnesiPipaDie"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM.as("anamnesiOccupazioneNum"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO.as("anamnesiOccupazioneAnnoInizio"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE.as("anamnesiOccupazioneAnnoFine"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO.as("anamnesiOccupazioneTipo"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO.as("anamnesiOccupazioneDescrizioneLavoro"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA.as("anamnesiOccupazioneNomeEIndirizzoDitta"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA.as("anamnesiOccupazioneAttivitaDitta"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO.as("anamnesiNotaAttivitaConAmianto"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO.as("anamnesiAnamnesiEsposizioneAmianto"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE.as("anamnesiEsposizioneProfessionale"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE.as("anamnesiAnnoFineEsposizione"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE.as("anamnesiLivelloEsposizione"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA.as("anamnesiInserimentoInSorveglianza"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL.as("anamnesiIdSpresal"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT.as("anamnesiOccupazioneEsposizioneCrpt"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT.as("anamnesiOccupazioneSettoreDittaCrpt"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT.as("anamnesiOccupazioneMansioneCrpt"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT.as("anamnesiOccupazioneRagioneSocialeDittaCrpt"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT.as("anamnesiOccupazionePivaDittaCrpt"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.COUNSELING.as("anamnesiCounseling"),
//                        REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_CODICE_FISCALE_DITTA_CRPT.as("anamnesiOccupazioneCodiceFiscaleDittaCrpt"),
//
//                        //ESITI VISITA (da REEA_T_REGISTRO_SPRESAL_ESITI)
//                        REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE.as("esitiVisCodiceFiscale"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA.as("esitiVisIdAura"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.as("esitiVisDataVisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.VISITA.as("esitiVisVisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA.as("esitiVisLivelloVisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO.as("esitiVisRiceveIndennizzo"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO.as("esitiVisMalattiaIndennizzo"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX.as("esitiVisAccertamentiRx"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA.as("esitiVisAccertamentiRxData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE.as("esitiVisAccertamentiRxRefertoNormale"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC.as("esitiVisAccertamentiTc"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA.as("esitiVisAccertamentiTcData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE.as("esitiVisAccertamentiTcRefertoNormale"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE.as("esitiVisAccertamentiSpirometriaSemplice"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA.as("esitiVisAccertamentiSpirometriaSempliceData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaSempliceRefertoNormale"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE.as("esitiVisAccertamentiSpirometriaGlobale"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA.as("esitiVisAccertamentiSpirometriaGlobaleData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaGlobaleRefertoNormale"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO.as("esitiVisAccertamentiDlco"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA.as("esitiVisAccertamentiDlcoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE.as("esitiVisAccertamentiDlcoRefertoNormale"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET.as("esitiVisAccertamentiPet"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA.as("esitiVisAccertamentiPetData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE.as("esitiVisAccertamentiPetRefertoNormale"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA.as("esitiVisAccertamentiVisitaPneumologica"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA.as("esitiVisAccertamentiVisitaPneumologicaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaPneumologicaReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA.as("esitiVisAccertamentiVisitaRadiologica"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA.as("esitiVisAccertamentiVisitaRadiologicaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaRadiologicaReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA.as("esitiVisAccertamentiVisitaOncologica"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA.as("esitiVisAccertamentiVisitaOncologicaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaOncologicaReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO.as("esitiVisAccertamentiAltro"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE.as("esitiVisAccertamentiAltroDescrizione"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA.as("esitiVisAccertamentiAltroData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE.as("esitiVisAccertamentiAltroRefertoNormale"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO.as("esitiVisRisultatoNegativo"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI.as("esitiVisPpmPlacchepleUrdicheMonolaterali"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisPpmPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisPpmPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisPpmAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisPpmAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisPpmPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO.as("esitiVisPpmReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA.as("esitiVisPpmRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI.as("esitiVisPpbPlacchepleUricheBilaterali"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisPpbPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisPpbPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisPpbAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisPpbAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisPpbPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO.as("esitiVisPpbReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA.as("esitiVisPpbRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE.as("esitiVisApAsbestosiPolmonare"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisApPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisApPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisApAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisApAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisApPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO.as("esitiVisApReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA.as("esitiVisApRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA.as("esitiVisFpdFibrosiPleuricaDiffusa"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisFpdPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisFpdPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisFpdAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisFpdAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisFpdPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO.as("esitiVisFpdReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA.as("esitiVisFpdRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO.as("esitiVisMpMesoteliomapleurico"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisMpPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisMpPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisMpAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisMpAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisMpPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO.as("esitiVisMpReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA.as("esitiVisMpRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR.as("esitiVisMpComunicazioneAlCor"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA.as("esitiVisMpComunicazioneAlCorData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA.as("esitiVisAmAltroMesotelioma"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisAmPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisAmPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisAmAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisAmAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisAmPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO.as("esitiVisAmReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA.as("esitiVisAmRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR.as("esitiVisAmComunicazioneAlCor"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA.as("esitiVisAmComunicazioneAlCorData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE.as("esitiVisNlNeoplasiaLaringe"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisNlPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisNlPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisNlAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisNlAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisNlPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO.as("esitiVisNlReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA.as("esitiVisNlRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA.as("esitiVisNoNeoplasiaOvarica"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisNoPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisNoPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisNoAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisNoAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisNoPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO.as("esitiVisNoReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA.as("esitiVisNoRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE.as("esitiVisTpTumoreDelPolmone"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisTpPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisTpPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisTpAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisTpAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisTpPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO.as("esitiVisTpReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA.as("esitiVisTpRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR.as("esitiVisTpComunicazioneAlCor"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA.as("esitiVisTpComunicazioneAlCorData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE.as("esitiVisBpcoEnfisemaPolmonare"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisBpcoPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisBpcoPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisBpcoAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisBpcoAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisBpcoPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO.as("esitiVisBpcoReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA.as("esitiVisBpcoRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI.as("esitiVisAltraDiagnosi"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE.as("esitiVisAltraDiagnosiDescrizione"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA.as("esitiVisAltraPrimoCertificatoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA.as("esitiVisAltraPrimoCertificatoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA.as("esitiVisAltraAggravamentoEDenuncia"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA.as("esitiVisAltraAggravamentoEDenunciaData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO.as("esitiVisAltraPercentualeDiRiconoscimento"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO.as("esitiVisAltraReferto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA.as("esitiVisAltraRefertoData"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO.as("esitiVisFollowUpPrevisto"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA.as("esitiVisAnnoPresuntoProssimaVisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA.as("esitiVisAnnoUltimaVisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG.as("esitiVisInvioSintesiAMmg"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL.as("esitiVisIdSpresal"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA.as("esitiVisAccertamentiRxAcquisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA.as("esitiVisAccertamentiTcAcquisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA.as("esitiVisAccertamentiSpirometriaSempliceAcquisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA.as("esitiVisAccertamentiSpirometriaGlobaleAcquisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA.as("esitiVisAccertamentiDlcoAcquisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA.as("esitiVisAccertamentiPetAcquisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaPneumologicaAcquisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaRadiologicaAcquisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaOncologicaAcquisita"),
//                        REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA.as("esitiVisAccertamentiAltroRefertoAcquisita"),
//                        
//                        REEA_T_REGISTRO.ATT_SANITARIA_STATO.as("regAttSanitariaStato"),
//                        REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL.as("regAttSanitariaSpresal"),
//                        REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA.as("regAttSanitariaSpresalData"),
//                        REEA_T_REGISTRO.ATT_SANITARIA_INAIL.as("regAttSanitariaInail"),
//                        REEA_T_REGISTRO.SEZIONE.as("regSezione"),
//                        REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("regInseritoInSorveglianza"),
//                        REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("regTipoElencoInail"),
//                        REEA_D_ASL.ASL_AZIENDA_DESC.as("aslAziendaDesc"),
//                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("soggettoStatoDesc")
//
//                )
//                .from(REEA_T_SOGGETTO)
//                .join(REEA_T_ADESIONE)
//                .on(REEA_T_SOGGETTO.SOGGETTO_ID.eq(REEA_T_ADESIONE.SOGGETTO_ID))
//                .leftJoin(REEA_R_SOGGETTO_ESENZIONE)
//                    .on(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
//                .leftJoin(REEA_D_ESENZIONE)
//                    .on(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_HMAC.eq(
//                            field("reea.text_to_hmac(cast({0} as text), cast({1} as text))",
//                                  String.class,
//                                  REEA_D_ESENZIONE.ESENZIONE_ID,
//                                  val("16<odcc8!")
//                            )
//                    ))
//                .leftJoin(REEA_T_REGISTRO)
//                    .on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
//                .leftJoin(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
//                    .on(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
//                .leftJoin(REEA_T_REGISTRO_SPRESAL_ESITI)
//                    .on(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
//                .leftJoin(REEA_T_REGISTRO_PDL_AMIANTO)
//                	.on(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
//                .leftJoin(REEA_T_REGISTRO_INAIL)
//                	.on(REEA_T_REGISTRO_INAIL.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
//                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//                .leftJoin(REEA_D_SOGGETTO_STATO).on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//                .where(REEA_T_SOGGETTO.SOGGETTO_ID.in(soggettoIds))
//                .and(REEA_T_SOGGETTO.DATA_CANCELLAZIONE.isNull().and(condAssistenzaAsl))
//                .fetchInto(ExportDTO.class);
//    }
    @Override
    public List<ExportDTO> recuperaPreadesioniPerExport(List<Long> soggettoIds, Integer assistenzaAslId) {

        Condition condAssistenzaAsl = noCondition();

        if (assistenzaAslId != null) {
            condAssistenzaAsl = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.eq(assistenzaAslId);
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

        List<ExportDTO> risultatiAdesione = dsl
            .select(
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
                REEA_T_ADESIONE.DOMICILIO_ASL_DESC.as("adesioneCslDiDomicilio"),
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
                REEA_T_SOGGETTO.DATA_DECESSO.as("auraDataDecesso"),
                REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE.as("auraAssistenzaAslFine"),
                emailChiara,
                telefonoChiaro,

                REEA_D_ESENZIONE.ESENZIONE_COD.as("codEsenzione"),
                REEA_D_ESENZIONE.ESENZIONE_DESC.as("descEsenzione"),
                REEA_D_ESENZIONE.DIAGNOSI_COD.as("codDiagnosi"),
                REEA_D_ESENZIONE.DIAGNOSI_DESC.as("descDiagnosi"),
                REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_EMISSIONE.as("dataEmissione"),
                REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_SCADENZA.as("dataScadenza"),

                REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.as("anamnesiCodiceFiscale"),
                REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA.as("anamnesiIdAura"),
                REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA.as("anamnesiDataIntervista"),

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
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC.as("esitiVisAccertamentiTc"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA.as("esitiVisAccertamentiTcData"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE.as("esitiVisAccertamentiTcRefertoNormale"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE.as("esitiVisAccertamentiSpirometriaSemplice"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA.as("esitiVisAccertamentiSpirometriaSempliceData"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaSempliceRefertoNormale"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE.as("esitiVisAccertamentiSpirometriaGlobale"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA.as("esitiVisAccertamentiSpirometriaGlobaleData"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM.as("esitiVisAccertamentiSpirometriaGlobaleRefertoNormale"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO.as("esitiVisAccertamentiDlco"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA.as("esitiVisAccertamentiDlcoData"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE.as("esitiVisAccertamentiDlcoRefertoNormale"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET.as("esitiVisAccertamentiPet"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA.as("esitiVisAccertamentiPetData"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE.as("esitiVisAccertamentiPetRefertoNormale"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA.as("esitiVisAccertamentiVisitaPneumologica"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA.as("esitiVisAccertamentiVisitaPneumologicaData"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaPneumologicaReferto"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA.as("esitiVisAccertamentiVisitaRadiologica"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA.as("esitiVisAccertamentiVisitaRadiologicaData"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaRadiologicaReferto"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA.as("esitiVisAccertamentiVisitaOncologica"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA.as("esitiVisAccertamentiVisitaOncologicaData"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO.as("esitiVisAccertamentiVisitaOncologicaReferto"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO.as("esitiVisAccertamentiAltro"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE.as("esitiVisAccertamentiAltroDescrizione"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA.as("esitiVisAccertamentiAltroData"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE.as("esitiVisAccertamentiAltroRefertoNormale"),
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
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA.as("esitiVisAccertamentiRxAcquisita"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA.as("esitiVisAccertamentiTcAcquisita"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA.as("esitiVisAccertamentiSpirometriaSempliceAcquisita"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA.as("esitiVisAccertamentiSpirometriaGlobaleAcquisita"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA.as("esitiVisAccertamentiDlcoAcquisita"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA.as("esitiVisAccertamentiPetAcquisita"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaPneumologicaAcquisita"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaRadiologicaAcquisita"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA.as("esitiVisAccertamentiVisitaOncologicaAcquisita"),
              REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA.as("esitiVisAccertamentiAltroRefertoAcquisita"),
              
              REEA_T_REGISTRO.ATT_SANITARIA_STATO.as("regAttSanitariaStato"),
              REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL.as("regAttSanitariaSpresal"),
              REEA_T_REGISTRO.ATT_SANITARIA_SPRESAL_DATA.as("regAttSanitariaSpresalData"),
              REEA_T_REGISTRO.ATT_SANITARIA_INAIL.as("regAttSanitariaInail"),
              REEA_T_REGISTRO.SEZIONE.as("regSezione"),
              REEA_T_REGISTRO.INSERITO_IN_SORVEGLIANZA.as("regInseritoInSorveglianza"),
              REEA_T_REGISTRO.TIPO_ELENCO_INAIL.as("regTipoElencoInail"),
              REEA_D_ASL.ASL_AZIENDA_DESC.as("aslAziendaDesc"),
              REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("soggettoStatoDesc")
            )
            .from(REEA_T_SOGGETTO)
            .join(REEA_T_ADESIONE).on(REEA_T_SOGGETTO.SOGGETTO_ID.eq(REEA_T_ADESIONE.SOGGETTO_ID))
            .leftJoin(REEA_R_SOGGETTO_ESENZIONE).on(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
            .leftJoin(REEA_D_ESENZIONE).on(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_HMAC.eq(
                field("reea.text_to_hmac(cast({0} as text), cast({1} as text))",
                    String.class,
                    REEA_D_ESENZIONE.ESENZIONE_ID,
                    val("16<odcc8!")
                )
            ))
            .leftJoin(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
            .leftJoin(REEA_T_REGISTRO_SPRESAL_ANAMNESI).on(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
            .leftJoin(REEA_T_REGISTRO_SPRESAL_ESITI).on(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
            .leftJoin(REEA_T_REGISTRO_PDL_AMIANTO).on(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
            .leftJoin(REEA_T_REGISTRO_INAIL).on(REEA_T_REGISTRO_INAIL.REGISTRO_ID.eq(REEA_T_REGISTRO.REGISTRO_ID))
            .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
            .leftJoin(REEA_D_SOGGETTO_STATO).on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
            .where(REEA_T_SOGGETTO.SOGGETTO_ID.in(soggettoIds))
            .and(REEA_T_SOGGETTO.DATA_CANCELLAZIONE.isNull())
            .and(condAssistenzaAsl)
            .fetchInto(ExportDTO.class);

        List<ExportDTO> risultatiPresentazioneIstanza = dsl
            .select(
                REEA_T_SOGGETTO.SOGGETTO_ID.as("soggettoId"),
                REEA_T_SOGGETTO.CODICE_FISCALE.as("auraCodiceFiscale"),
                auraNomeChiaro,
                auraCognomeChiaro,
                REEA_T_SOGGETTO.SESSO.as("auraSesso"),
                REEA_T_SOGGETTO.NASCITA_DATA.as("auraNascitaData"),
                auraDomicilioIndirizzoChiaro,
                auraResidenzaIndirizzoChiaro,
                REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA.as("presentazioneIstanzaData"),
                REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("assistenzaAslId"),
                REEA_T_SOGGETTO.DOMICILIO_ASL_ID.as("auraDomicilioAsl"),
                REEA_T_SOGGETTO.RESIDENZA_ASL_ID.as("auraResidenzaAsl"),
                REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("auraAssistenzaAsl"),
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
                REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO.as("auraDomicilioNumeroCivico"),
                REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD.as("auraResidenzaComuneCod"),
                REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC.as("auraResidenzaComuneDesc"),
                REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD.as("auraResidenzaProvinciaCod"),
                REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC.as("auraResidenzaProvinciaDesc"),
                REEA_T_SOGGETTO.RESIDENZA_STATO_COD.as("auraResidenzaStatoCod"),
                REEA_T_SOGGETTO.RESIDENZA_STATO_DESC.as("auraResidenzaStatoDesc"),
                REEA_T_SOGGETTO.RESIDENZA_CAP.as("auraResidenzaCap"),
                REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO.as("auraResidenzaNumeroCivico"),
                auraTesseraTeamChiara,
                auraIdAuraChiaro,
                auraEmailChiara,
                auraTelefonoChiaro,
                REEA_T_SOGGETTO.DATA_DECESSO.as("auraDataDecesso"),
                REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE.as("auraAssistenzaAslFine"),
                emailChiara,
                telefonoChiaro,
                REEA_D_ESENZIONE.ESENZIONE_COD.as("codEsenzione"),
                REEA_D_ESENZIONE.ESENZIONE_DESC.as("descEsenzione"),
                REEA_D_ESENZIONE.DIAGNOSI_COD.as("codDiagnosi"),
                REEA_D_ESENZIONE.DIAGNOSI_DESC.as("descDiagnosi"),
                REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_EMISSIONE.as("dataEmissione"),
                REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_SCADENZA.as("dataScadenza"),
                REEA_D_ASL.ASL_AZIENDA_DESC.as("aslAziendaDesc"),
                
                //ESPOSIZIONI
                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA.as("adesioneEsposizioneAzienda"),
                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_COD.as("adesioneEsposizioneAziendaComuneCod"),
                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_COMUNE_DESC.as("adesioneEsposizioneAziendaComuneDesc"),
                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_CAP.as("adesioneEsposizioneAziendaCap"),
                REEA_T_ESPOSIZIONE.ESPOSIZIONE_INIZIO.as("adesioneEsposizioneDataInizioEsposizione"),
                REEA_T_ESPOSIZIONE.ESPOSIZIONE_FINE.as("adesioneEsposizioneDataFineEsposizione"),
                REEA_T_ESPOSIZIONE.ESPOSIZIONE_AZIENDA_PROVINCIA.as("adesioneEsposizioneProvinciaAzienda"),
                REEA_T_ESPOSIZIONE.ESPOSIZIONE_MANSIONE.as("adesioneEsposizioneMansione")
            )
            .from(REEA_T_SOGGETTO)
            .leftJoin(REEA_R_SOGGETTO_ESENZIONE).on(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
            .leftJoin(REEA_D_ESENZIONE).on(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_HMAC.eq(
                field("reea.text_to_hmac(cast({0} as text), cast({1} as text))",
                    String.class,
                    REEA_D_ESENZIONE.ESENZIONE_ID,
                    val("16<odcc8!")
                )
            ))
            .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
            .leftJoin(REEA_T_ESPOSIZIONE).on(REEA_T_ESPOSIZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
            .where(REEA_T_SOGGETTO.SOGGETTO_ID.in(soggettoIds))
            .and(REEA_T_SOGGETTO.DATA_CANCELLAZIONE.isNull())
            .and(REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA.isNotNull())
            .and(condAssistenzaAsl)
            .andNotExists(
                dsl.selectOne()
                   .from(REEA_T_ADESIONE)
                   .where(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
            )
            .fetchInto(ExportDTO.class);

        List<ExportDTO> risultatiFinali = new ArrayList<>();
        risultatiFinali.addAll(risultatiAdesione);
        risultatiFinali.addAll(risultatiPresentazioneIstanza);

        return risultatiFinali;
    }
//    @Override
//    public List<ExportDTO> recuperaPreadesioniPerExport(List<Long> soggettoIds, Integer assistenzaAslId) {
//
//        Condition condAssistenzaAsl = noCondition();
//        if (assistenzaAslId != null) {
//            condAssistenzaAsl = REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.in(assistenzaAslId);
//        }
//
//        Field<String> preNomeChiaro = safeDecrypt(REEA_T_ADESIONE.NOME_CIFRATO, "adesioneNome");
//        Field<String> preCognomeChiaro = safeDecrypt(REEA_T_ADESIONE.COGNOME_CIFRATO, "adesioneCognome");
//        Field<String> preTesseraTeamChiara = safeDecrypt(REEA_T_ADESIONE.TESSERA_TEAM_CIFRATA, "adesioneTesseraTeam");
//        Field<String> preIdAuraChiaro = safeDecrypt(REEA_T_ADESIONE.ID_AURA_CIFRATO, "adesioneIdAura");
//        Field<String> preEmailChiara = safeDecrypt(REEA_T_ADESIONE.EMAIL_CIFRATA, "adesioneEmail");
//        Field<String> preTelefonoChiaro = safeDecrypt(REEA_T_ADESIONE.TELEFONO_CIFRATO, "adesioneTelefono");
//
//        Field<String> auraNomeChiaro = safeDecrypt(REEA_T_SOGGETTO.NOME_CIFRATO, "auraNome");
//        Field<String> auraCognomeChiaro = safeDecrypt(REEA_T_SOGGETTO.COGNOME_CIFRATO, "auraCognome");
//        Field<String> auraDomicilioIndirizzoChiaro = safeDecrypt(REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO, "auraDomicilioIndirizzo");
//        Field<String> auraResidenzaIndirizzoChiaro = safeDecrypt(REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO, "auraResidenzaIndirizzo");
//        Field<String> auraTesseraTeamChiara = safeDecrypt(REEA_T_SOGGETTO.TESSERA_TEAM_CIFRATA, "auraTesseraTeam");
//        Field<String> auraIdAuraChiaro = safeDecrypt(REEA_T_SOGGETTO.ID_AURA_CIFRATO, "auraIdAura");
//        Field<String> auraEmailChiara = safeDecrypt(REEA_T_SOGGETTO.EMAIL_AURA_CIFRATA, "auraEmail");
//        Field<String> auraTelefonoChiaro = safeDecrypt(REEA_T_SOGGETTO.TELEFONO_AURA_CIFRATO, "auraTelefono");
//        Field<String> emailChiara = safeDecrypt(REEA_T_SOGGETTO.EMAIL_CIFRATA, "email");
//        Field<String> telefonoChiaro = safeDecrypt(REEA_T_SOGGETTO.TELEFONO_CIFRATO, "telefono");
//
//        Field<String> soggettoIdHmac = DSL.field(
//                "reea.hmac_soggetto_id(cast({0} as int4), cast({1} as text))",
//                String.class,
//                REEA_T_SOGGETTO.SOGGETTO_ID,
//                DSL.val("16odcc8!")
//        );
//
//        SelectConditionStep<?> baseQuery = dsl
//                .select(
//                        REEA_T_ADESIONE.ADESIONE_COD.as("adesioneCodiceAdesione"),
//                        REEA_T_ADESIONE.ADESIONE_DATA.as("adesioneDataAdesione"),
//                        REEA_T_ADESIONE.CODICE_FISCALE.as("adesioneCodiceFiscale"),
//                        REEA_T_ADESIONE.ADESIONE_COD.as("codiceAdesione"),
//                        REEA_T_ADESIONE.ADESIONE_DATA.as("dataAdesione"),
//                        REEA_T_ADESIONE.CODICE_FISCALE.as("codiceFiscale"),
//                        preCognomeChiaro,
//                        preNomeChiaro,
//                        REEA_T_ADESIONE.NASCITA_DATA.as("adesioneDataDiNascita"),
//                        REEA_T_ADESIONE.NASCITA_PROVINCIA_DESC.as("adesioneProvinciaDiNascita"),
//                        REEA_T_ADESIONE.NASCITA_COMUNE_DESC.as("adesioneComuneDiNascita"),
//                        preTesseraTeamChiara,
//                        preIdAuraChiaro,
//                        REEA_T_ADESIONE.DOMICILIO_PROVINCIA_DESC.as("adesioneProvinciaDiDomicilio"),
//                        REEA_T_ADESIONE.DOMICILIO_COMUNE_DESC.as("adesioneComuneDiDomicilio"),
//                        REEA_T_ADESIONE.DOMICILIO_COMUNE_COD.as("adesioneCodiceComuneIstatDiDomicilio"),
//                        REEA_T_ADESIONE.DOMICILIO_CAP.as("adesioneCapDiDomicilio"),
//                        preEmailChiara,
//                        preTelefonoChiaro,
//                        REEA_T_ADESIONE.DOMICILIO_ASL_COD.as("adesioneCodiceAslDiDomicilio"),
//                        REEA_T_ADESIONE.DOMICILIO_ASL_DESC.as("adesioneCslDiDomicilio"),
//                        REEA_T_ADESIONE.AZIENDA_COD.as("adesioneCodazi"),
//                        REEA_T_ADESIONE.RESIDENZA_ASL_COD.as("adesioneCodiceAslDiResidenza"),
//                        REEA_T_ADESIONE.RESIDENZA_ASL_DESC.as("adesioneAslDiResidenza"),
//                        REEA_T_ADESIONE.ESPOSIZIONE_INIZIO.as("adesioneDataInizioEsposizione"),
//                        REEA_T_ADESIONE.ESPOSIZIONE_FINE.as("adesioneDataFineEsposizione"),
//                        REEA_T_ADESIONE.AZIENDA_DESC.as("adesioneAzienda"),
//                        REEA_T_ADESIONE.AZIENDA_COMUNE_COD.as("adesioneCodiceComuneIstatAzienda"),
//                        REEA_T_ADESIONE.AZIENDA_COMUNE_DESC.as("adesioneComuneAzienda"),
//                        REEA_T_ADESIONE.AZIENDA_CAP.as("adesioneCapAzienda"),
//                        REEA_T_ADESIONE.AZIENDA_PROVINCIA.as("adesioneProvinciaAzienda"),
//                        REEA_T_ADESIONE.MANSIONE.as("adesioneMansione"),
//                        REEA_T_SOGGETTO.CODICE_FISCALE.as("auraCodiceFiscale"),
//                        REEA_T_SOGGETTO.DOMICILIO_ASL_ID.as("auraDomicilioAsl"),
//                        REEA_T_SOGGETTO.RESIDENZA_ASL_ID.as("auraResidenzaAsl"),
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("auraAssistenzaAsl"),
//                        auraNomeChiaro,
//                        auraCognomeChiaro,
//                        REEA_T_SOGGETTO.SESSO.as("auraSesso"),
//                        REEA_T_SOGGETTO.NASCITA_DATA.as("auraNascitaData"),
//                        REEA_T_SOGGETTO.NASCITA_COMUNE_COD.as("auraNascitaComuneCod"),
//                        REEA_T_SOGGETTO.NASCITA_COMUNE_DESC.as("auraNascitaComuneDesc"),
//                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_COD.as("auraNascitaProvinciaCod"),
//                        REEA_T_SOGGETTO.NASCITA_PROVINCIA_DESC.as("auraNascitaProvinciaDesc"),
//                        REEA_T_SOGGETTO.NASCITA_STATO_COD.as("auraNascitaStatoCod"),
//                        REEA_T_SOGGETTO.NASCITA_STATO_DESC.as("auraNascitaStatoDesc"),
//                        REEA_T_SOGGETTO.CITTADINANZA_STATO_COD.as("auraCittadinanzaStatoCod"),
//                        REEA_T_SOGGETTO.CITTADINANZA_STATO_DESC.as("auraCittadinanzaStatoDesc"),
//                        REEA_T_SOGGETTO.DOMICILIO_COMUNE_COD.as("auraDomicilioComuneCod"),
//                        REEA_T_SOGGETTO.DOMICILIO_COMUNE_DESC.as("auraDomicilioComuneDesc"),
//                        REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_COD.as("auraDomicilioProvinciaCod"),
//                        REEA_T_SOGGETTO.DOMICILIO_PROVINCIA_DESC.as("auraDomicilioProvinciaDesc"),
//                        REEA_T_SOGGETTO.DOMICILIO_STATO_COD.as("auraDomicilioStatoCod"),
//                        REEA_T_SOGGETTO.DOMICILIO_STATO_DESC.as("auraDomicilioStatoDesc"),
//                        REEA_T_SOGGETTO.DOMICILIO_CAP.as("auraDomicilioCap"),
//                        auraDomicilioIndirizzoChiaro,
//                        REEA_T_SOGGETTO.DOMICILIO_NUMERO_CIVICO.as("auraDomicilioNumeroCivico"),
//                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_COD.as("auraResidenzaComuneCod"),
//                        REEA_T_SOGGETTO.RESIDENZA_COMUNE_DESC.as("auraResidenzaComuneDesc"),
//                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_COD.as("auraResidenzaProvinciaCod"),
//                        REEA_T_SOGGETTO.RESIDENZA_PROVINCIA_DESC.as("auraResidenzaProvinciaDesc"),
//                        REEA_T_SOGGETTO.RESIDENZA_STATO_COD.as("auraResidenzaStatoCod"),
//                        REEA_T_SOGGETTO.RESIDENZA_STATO_DESC.as("auraResidenzaStatoDesc"),
//                        REEA_T_SOGGETTO.RESIDENZA_CAP.as("auraResidenzaCap"),
//                        auraResidenzaIndirizzoChiaro,
//                        REEA_T_SOGGETTO.RESIDENZA_NUMERO_CIVICO.as("auraResidenzaNumeroCivico"),
//                        auraTesseraTeamChiara,
//                        auraIdAuraChiaro,
//                        auraEmailChiara,
//                        auraTelefonoChiaro,
//                        REEA_T_SOGGETTO.DATA_DECESSO.as("auraDataDecesso"),
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_FINE.as("auraAssistenzaAslFine"),
//                        emailChiara,
//                        telefonoChiaro,
//                        REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC.as("assoggettoStatoDesc")
//                )
//                .from(REEA_T_SOGGETTO)
//                .join(REEA_T_ADESIONE).on(REEA_T_SOGGETTO.SOGGETTO_ID.eq(REEA_T_ADESIONE.SOGGETTO_ID))
//                .leftJoin(REEA_R_SOGGETTO_ESENZIONE).on(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
//                .leftJoin(REEA_T_REGISTRO).on(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(soggettoIdHmac))
//                .leftJoin(REEA_D_ASL).on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
//                .leftJoin(REEA_D_SOGGETTO_STATO).on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
//                .where(REEA_T_SOGGETTO.DATA_CANCELLAZIONE.isNull())
//                .and(condAssistenzaAsl);
//
//        List<Integer> soggettoIdsInt = soggettoIds.stream()
//                .map(Long::intValue)
//                .toList();
//        
//        List<ExportDTO> risultati = baseQuery
//                .and(REEA_T_SOGGETTO.SOGGETTO_ID.in(soggettoIdsInt))
//                .fetchInto(ExportDTO.class);
//
//        List<ExportDTO> risultatiPresentazioneIstanza = dsl
//                .select(
//                        REEA_T_SOGGETTO.SOGGETTO_ID.as("soggettoId"),
//                        REEA_T_SOGGETTO.CODICE_FISCALE.as("auraCodiceFiscale"),
//                        REEA_T_SOGGETTO.NOME_CIFRATO.as("auraNome"),
//                        REEA_T_SOGGETTO.COGNOME_CIFRATO.as("auraCognome"),
//                        REEA_T_SOGGETTO.SESSO.as("auraSesso"),
//                        REEA_T_SOGGETTO.NASCITA_DATA.as("auraNascitaData"),
//                        REEA_T_SOGGETTO.DOMICILIO_INDIRIZZO_CIFRATO.as("auraDomicilioIndirizzo"),
//                        REEA_T_SOGGETTO.RESIDENZA_INDIRIZZO_CIFRATO.as("auraResidenzaIndirizzo"),
//                        REEA_T_SOGGETTO.EMAIL_CIFRATA.as("email"),
//                        REEA_T_SOGGETTO.TELEFONO_CIFRATO.as("telefono"),
//                        REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA.as("presentazioneIstanzaData"),
//                        REEA_T_SOGGETTO.ASSISTENZA_ASL_ID.as("assistenzaAslId")
//                )
//                .from(REEA_T_SOGGETTO)
//                .where(REEA_T_SOGGETTO.DATA_CANCELLAZIONE.isNull())
//                .and(REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA.isNotNull())
//                .andNotExists(
//                        dsl.selectOne()
//                           .from(REEA_T_ADESIONE)
//                           .where(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
//                )
//                .fetchInto(ExportDTO.class);
//
//        return Stream.concat(risultati.stream(), risultatiPresentazioneIstanza.stream())
//                .distinct()
//                .toList();
//    }


//    private Field<String> safeDecrypt(Field<?> field, String alias) {
//        return DSL.field("reea.safe_pgp_sym_decrypt({0}, {1})",
//                String.class,
//                field,
//                DSL.val("16odcc8!"))
//            .as(alias);
//    }

}

