package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaDFileStato.REEA_D_FILE_STATO;
import static it.csi.registry.jooq.tables.ReeaLFileElaborazione.REEA_L_FILE_ELABORAZIONE;
import static it.csi.registry.jooq.tables.ReeaLFileElaborazioneErrore.REEA_L_FILE_ELABORAZIONE_ERRORE;
import static it.csi.registry.jooq.tables.ReeaLFileScaricoErrore.REEA_L_FILE_SCARICO_ERRORE;
import static it.csi.registry.jooq.tables.ReeaTAdesione.REEA_T_ADESIONE;
import static it.csi.registry.jooq.tables.ReeaTFile.REEA_T_FILE;
import static it.csi.registry.jooq.tables.ReeaTRegistroInail.REEA_T_REGISTRO_INAIL;
import static it.csi.registry.jooq.tables.ReeaTRegistroPdlAmianto.REEA_T_REGISTRO_PDL_AMIANTO;
import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalAnamnesi.REEA_T_REGISTRO_SPRESAL_ANAMNESI;
import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalEsiti.REEA_T_REGISTRO_SPRESAL_ESITI;
import static org.jooq.impl.DSL.noCondition;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.ArchivioFileCaricatiDTO;
import it.csi.registry.model.ScartoFileDTO;
import it.csi.registry.util.DateConversionUtils;

@Repository
public class ArchivioFileCaricatiRepositoryImpl implements ArchivioFileCaricatiRepository {

    private final DSLContext dsl;

    public ArchivioFileCaricatiRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }


    @Override
    public List<ArchivioFileCaricatiDTO> listaArchivioFileCaricati(
            String filtroFonte, LocalDate dataDa, LocalDate dataA) {

        Condition cond = noCondition();

        cond = cond.and(REEA_T_FILE.FILE_NAME.notLike("export_totale_%"));


        if (filtroFonte != null && !filtroFonte.isBlank()) {
            Field<String> normalizedPath = DSL.replace(REEA_T_FILE.FILE_PATH, "\\", "/");
            Field<String> fonteField = DSL.field("split_part({0}, '/reea/fonti/', 2)", String.class, normalizedPath);
            Field<String> fonteSegmento = DSL.field("split_part({0}, '/', 1)", String.class, fonteField);
            cond = cond.and(fonteSegmento.eq(filtroFonte));
        }
        if (dataDa != null) {
            cond = cond.and(REEA_T_FILE.DATA_CREAZIONE.greaterOrEqual(
                    dataDa.atStartOfDay().atOffset(ZoneOffset.UTC).toLocalDateTime()));
        }
        if (dataA != null) {
            cond = cond.and(REEA_T_FILE.DATA_CREAZIONE.lessOrEqual(
                    dataA.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).toLocalDateTime()));
        }


        return dsl
                .select(
                        REEA_T_FILE.FILE_ID,
                        REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID,
                        REEA_T_FILE.FILE_NAME,
                        REEA_T_FILE.FILE_PATH,
                        REEA_L_FILE_ELABORAZIONE.RIGHE_TOTALI,
                        REEA_L_FILE_ELABORAZIONE.RIGHE_ELABORATE,
                        REEA_L_FILE_ELABORAZIONE.RIGHE_SCARTATE,
                        REEA_L_FILE_ELABORAZIONE.RIGHE_ERRATE,
                        REEA_T_FILE.DATA_CREAZIONE,
                        REEA_T_FILE.UTENTE_CREAZIONE,
                        REEA_D_FILE_STATO.FILE_STATO_DESC
                )
                .from(REEA_T_FILE)
                .leftJoin(REEA_L_FILE_ELABORAZIONE).on(REEA_L_FILE_ELABORAZIONE.FILE_ID.eq(REEA_T_FILE.FILE_ID))
                .leftJoin(REEA_D_FILE_STATO).on(
                    REEA_D_FILE_STATO.FILE_STATO_ID.eq(
                        DSL.coalesce(REEA_L_FILE_ELABORAZIONE.FILE_STATO_ID, REEA_T_FILE.FILE_STATO_ID)))
                .where(cond)
                .orderBy(REEA_T_FILE.DATA_CREAZIONE.desc())
                .stream().map(r -> {
                    ArchivioFileCaricatiDTO row = new ArchivioFileCaricatiDTO();
                    row.setFileId(r.get(REEA_T_FILE.FILE_ID) != null ? r.get(REEA_T_FILE.FILE_ID).intValue() : null);
                    row.setElaborazioneId(r.get(REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID) != null
                            ? r.get(REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID).intValue() : null);  // â†� aggiunto
                    row.setNomeFile(r.get(REEA_T_FILE.FILE_NAME));
                    row.setFonteProvenienza(r.get(REEA_T_FILE.FILE_PATH));
                    row.setStato(r.get(REEA_D_FILE_STATO.FILE_STATO_DESC));
                    row.setRecordTotali(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_TOTALI));
                    row.setRecordElaborati(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_ELABORATE));
                    row.setRecordScartati(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_SCARTATE));
                    row.setRecordErrati(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_ERRATE));
                    row.setDataCaricamento(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_FILE.DATA_CREAZIONE)));
                    row.setOperatore(r.get(REEA_T_FILE.UTENTE_CREAZIONE));
                    return row;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean existsFileByName(String nomeFile) {
        return dsl.fetchExists(
            dsl.selectOne()
               .from(REEA_T_FILE)
               .where(REEA_T_FILE.FILE_NAME.eq(nomeFile))
        );
    }

    @Override
    public boolean hasElaborazioneTerminata(String tipoFonte) {

        var query = dsl.select(
                        REEA_T_FILE.FILE_ID,
                        REEA_T_FILE.FILE_PATH,
                        REEA_L_FILE_ELABORAZIONE.FILE_STATO_ID
                    )
                    .from(REEA_T_FILE)
                    .leftJoin(REEA_L_FILE_ELABORAZIONE)
                        .on(REEA_L_FILE_ELABORAZIONE.FILE_ID.eq(REEA_T_FILE.FILE_ID))
                    .where(REEA_L_FILE_ELABORAZIONE.FILE_STATO_ID.eq(2))
                    .and(REEA_T_FILE.FILE_PATH.containsIgnoreCase(tipoFonte)
                    // DE AGGIUNTA VALIFITA FINE CHE VIENE VALORIZZATA SOLO QUANDO HA TUTTE LE RIGHE CARICATE E SOLO ALLORA POSSO ELABORARLA
                    .and(REEA_T_FILE.VALIDITA_FINE.isNotNull())
                    		);
/*
        System.out.println("========================================");
        System.out.println("TIPO FONTE: " + tipoFonte);

        System.out.println("SQL GENERATA:");
        System.out.println(query.getSQL());

        System.out.println("SQL CON PARAMETRI:");
        System.out.println(dsl.renderInlined(query));
*/
        var records = query.fetch();

        System.out.println("NUMERO RECORD TROVATI: " + records.size());
/*
        records.forEach(record -> {
            System.out.println(
                "FILE_ID=" + record.get(REEA_T_FILE.FILE_ID)
                + " | FILE_PATH=" + record.get(REEA_T_FILE.FILE_PATH)
                + " | FILE_STATO_ID=" + record.get(REEA_L_FILE_ELABORAZIONE.FILE_STATO_ID)
            );

        });
            */
        boolean result = !records.isEmpty();
        
        System.out.println("========================================");
        System.out.println("RISULTATO fetchExists = " + result);
        System.out.println("========================================");

        return result;
    }
   
   
    
    
    @Override
    public boolean hasPendingElaborazioni(String fonteFolder) {
        Field<String> normalizedPath = DSL.replace(REEA_T_FILE.FILE_PATH, "\\", "/");
        Field<String> fonteField = DSL.field("split_part({0}, '/reea/fonti/', 2)", String.class, normalizedPath);
        Field<String> fonteSegmento = DSL.field("split_part({0}, '/', 1)", String.class, fonteField);
        return dsl.fetchExists(
            dsl.selectOne()
               .from(REEA_T_FILE)
               .leftJoin(REEA_L_FILE_ELABORAZIONE).on(REEA_L_FILE_ELABORAZIONE.FILE_ID.eq(REEA_T_FILE.FILE_ID))
               .where(
                   DSL.coalesce(REEA_L_FILE_ELABORAZIONE.FILE_STATO_ID, REEA_T_FILE.FILE_STATO_ID).in(1, 2)
                   .and(fonteSegmento.eq(fonteFolder))
                   .and(REEA_T_FILE.FILE_NAME.notLike("export_totale_%"))
               )
        );
    }

    @Override
    public List<ScartoFileDTO> getElaborazioneErroreFile(Integer elaborazioneId) {
        return dsl
                .select(
//                        REEA_L_FILE_ELABORAZIONE_ERRORE.NUMERO_RIGA,
                		REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_TABELLA_NOME,
                		REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_RECORD_ID,
                        REEA_L_FILE_ELABORAZIONE_ERRORE.CODICE_ERRORE,
                        REEA_L_FILE_ELABORAZIONE_ERRORE.DESCRIZIONE_ERRORE,
                        REEA_L_FILE_ELABORAZIONE_ERRORE.CONTENUTO_RIGA_RAW,
                        REEA_L_FILE_ELABORAZIONE_ERRORE.CODICE_FISCALE,
                        REEA_L_FILE_ELABORAZIONE_ERRORE.COGNOME,
                        REEA_L_FILE_ELABORAZIONE_ERRORE.NOME,
                        REEA_L_FILE_ELABORAZIONE_ERRORE.DATA_NASCITA,
                        REEA_L_FILE_ELABORAZIONE_ERRORE.SESSO,
                        DSL.coalesce(
                                REEA_T_ADESIONE.AZIENDA_COD.cast(String.class),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM.cast(String.class),
                                REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.cast(String.class),
                                REEA_T_REGISTRO_INAIL.DOMANDA.cast(String.class),
                                REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE.cast(String.class)
                        ).as("campo_extra")
                )
                .from(REEA_L_FILE_ELABORAZIONE_ERRORE)
                .leftJoin(REEA_T_ADESIONE)
                .on(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_ADESIONE")
                        .and(REEA_T_ADESIONE.ADESIONE_ID            // â†� verifica PK
                                .eq(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_RECORD_ID)))
                .leftJoin(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                .on(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_REGISTRO_SPRESAL_ANAMNESI")
                        .and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID   // â†� verifica PK
                                .eq(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_RECORD_ID)))
                .leftJoin(REEA_T_REGISTRO_SPRESAL_ESITI)
                .on(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_REGISTRO_SPRESAL_ESITI")
                        .and(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID       // â†� verifica PK
                                .eq(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_RECORD_ID)))
                .leftJoin(REEA_T_REGISTRO_INAIL)
                .on(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_REGISTRO_INAIL")
                        .and(REEA_T_REGISTRO_INAIL.REG_INAIL_ID               // â†� verifica PK
                                .eq(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_RECORD_ID)))
                .leftJoin(REEA_T_REGISTRO_PDL_AMIANTO)
                .on(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_REGISTRO_PDL_AMIANTO")
                        .and(REEA_T_REGISTRO_PDL_AMIANTO.REG_PDL_AMIANTO_ID    // â†� verifica PK
                                .eq(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_RECORD_ID)))
                .where(REEA_L_FILE_ELABORAZIONE_ERRORE.ELABORAZIONE_ID.eq(elaborazioneId))
                .stream().map(r -> {
                    ScartoFileDTO dto = new ScartoFileDTO();
//                    dto.setNumeroRiga(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.NUMERO_RIGA));
                    dto.setTargetTabellaNome(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_TABELLA_NOME));
                    dto.setTargetRecordId(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_RECORD_ID));
                    dto.setCodiceErrore(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.CODICE_ERRORE));
                    dto.setDescrizioneErrore(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.DESCRIZIONE_ERRORE));
                    dto.setContenutoRigaRaw(Optional.ofNullable(r.get((Field<byte[]>) REEA_L_FILE_ELABORAZIONE_ERRORE.CONTENUTO_RIGA_RAW))
                            .map(b -> new String(b, StandardCharsets.UTF_8)).orElse(null));
                    dto.setCodiceFiscale(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.CODICE_FISCALE));
                    dto.setCognome(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.COGNOME));
                    dto.setNome(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.NOME));
                    LocalDate nascita = r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.DATA_NASCITA);
                    dto.setDataNascita(nascita != null ? nascita.toString() : null);
                    dto.setSesso(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.SESSO));
                    dto.setCampoExtra(r.get("campo_extra", String.class));
                    return dto;
                }).collect(Collectors.toList());
    }
    
    
    @Override
    public List<ScartoFileDTO> getScaricoErroreFile(Integer fileId) {
        return dsl
                .select(
                		REEA_L_FILE_SCARICO_ERRORE.TARGET_TABELLA_NOME,
                		REEA_L_FILE_SCARICO_ERRORE.TARGET_RECORD_ID,
                		REEA_L_FILE_SCARICO_ERRORE.CODICE_ERRORE,
                		REEA_L_FILE_SCARICO_ERRORE.DESCRIZIONE_ERRORE,
                		REEA_L_FILE_SCARICO_ERRORE.CONTENUTO_RIGA_RAW,
                		REEA_L_FILE_SCARICO_ERRORE.CODICE_FISCALE,
                		REEA_L_FILE_SCARICO_ERRORE.COGNOME,
                		REEA_L_FILE_SCARICO_ERRORE.NOME,
                		REEA_L_FILE_SCARICO_ERRORE.DATA_NASCITA,
                		REEA_L_FILE_SCARICO_ERRORE.SESSO,
                        DSL.coalesce(
                                REEA_T_ADESIONE.AZIENDA_COD.cast(String.class),
                                REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM.cast(String.class),
                                REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.cast(String.class),
                                REEA_T_REGISTRO_INAIL.DOMANDA.cast(String.class),
                                REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE.cast(String.class)
                        ).as("campo_extra")
                )
                .from(REEA_L_FILE_SCARICO_ERRORE)
                .leftJoin(REEA_T_ADESIONE)
                .on(REEA_L_FILE_SCARICO_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_ADESIONE")
                        .and(REEA_T_ADESIONE.ADESIONE_ID            // â†� verifica PK
                                .eq(REEA_L_FILE_SCARICO_ERRORE.TARGET_RECORD_ID)))
                .leftJoin(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                .on(REEA_L_FILE_SCARICO_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_REGISTRO_SPRESAL_ANAMNESI")
                        .and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID   // â†� verifica PK
                                .eq(REEA_L_FILE_SCARICO_ERRORE.TARGET_RECORD_ID)))
                .leftJoin(REEA_T_REGISTRO_SPRESAL_ESITI)
                .on(REEA_L_FILE_SCARICO_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_REGISTRO_SPRESAL_ESITI")
                        .and(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID       // â†� verifica PK
                                .eq(REEA_L_FILE_SCARICO_ERRORE.TARGET_RECORD_ID)))
                .leftJoin(REEA_T_REGISTRO_INAIL)
                .on(REEA_L_FILE_SCARICO_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_REGISTRO_INAIL")
                        .and(REEA_T_REGISTRO_INAIL.REG_INAIL_ID               // â†� verifica PK
                                .eq(REEA_L_FILE_SCARICO_ERRORE.TARGET_RECORD_ID)))
                .leftJoin(REEA_T_REGISTRO_PDL_AMIANTO)
                .on(REEA_L_FILE_SCARICO_ERRORE.TARGET_TABELLA_NOME.eq("REEA_T_REGISTRO_PDL_AMIANTO")
                        .and(REEA_T_REGISTRO_PDL_AMIANTO.REG_PDL_AMIANTO_ID    // â†� verifica PK
                                .eq(REEA_L_FILE_SCARICO_ERRORE.TARGET_RECORD_ID)))
                .where(REEA_L_FILE_SCARICO_ERRORE.FILE_ID.eq(fileId))
                .stream().map(r -> {
                    ScartoFileDTO dto = new ScartoFileDTO();
//                    dto.setNumeroRiga(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.NUMERO_RIGA));
                    dto.setTargetTabellaNome(r.get(REEA_L_FILE_SCARICO_ERRORE.TARGET_TABELLA_NOME));
                    dto.setTargetRecordId(r.get(REEA_L_FILE_SCARICO_ERRORE.TARGET_RECORD_ID));
                    dto.setCodiceErrore(r.get(REEA_L_FILE_SCARICO_ERRORE.CODICE_ERRORE));
                    dto.setDescrizioneErrore(r.get(REEA_L_FILE_SCARICO_ERRORE.DESCRIZIONE_ERRORE));
                    dto.setContenutoRigaRaw(Optional.ofNullable(r.get((Field<byte[]>) REEA_L_FILE_SCARICO_ERRORE.CONTENUTO_RIGA_RAW))
                            .map(b -> new String(b, StandardCharsets.UTF_8)).orElse(null));
                    dto.setCodiceFiscale(r.get(REEA_L_FILE_SCARICO_ERRORE.CODICE_FISCALE));
                    dto.setCognome(r.get(REEA_L_FILE_SCARICO_ERRORE.COGNOME));
                    dto.setNome(r.get(REEA_L_FILE_SCARICO_ERRORE.NOME));
                    LocalDate nascita = r.get(REEA_L_FILE_SCARICO_ERRORE.DATA_NASCITA);
                    dto.setDataNascita(nascita != null ? nascita.toString() : null);
                    dto.setSesso(r.get(REEA_L_FILE_SCARICO_ERRORE.SESSO));
                    dto.setCampoExtra(r.get("campo_extra", String.class));
                    return dto;
                }).collect(Collectors.toList());
    }




//    @Override
//    public List<ScartoFileDTO> getScartiFile(Integer fileId) {
//        return dsl
//                .select(
//                        REEA_L_FILE_ELABORAZIONE_ERRORE.NUMERO_RIGA,
//                        REEA_L_FILE_ELABORAZIONE_ERRORE.CODICE_ERRORE,
//                        REEA_L_FILE_ELABORAZIONE_ERRORE.DESCRIZIONE_ERRORE,
//                        REEA_L_FILE_ELABORAZIONE_ERRORE.CONTENUTO_RIGA_RAW,
//                        DSL.field("cognome", String.class),
//                        DSL.field("nome", String.class),
//                        DSL.field("data_nascita", LocalDate.class),
//                        DSL.field("sesso", String.class),
//                        DSL.field("codice_fiscale", String.class)
//                )
//                .from(REEA_L_FILE_ELABORAZIONE_ERRORE)
//                .join(REEA_L_FILE_ELABORAZIONE)
//                .on(REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID
//                        .eq(REEA_L_FILE_ELABORAZIONE_ERRORE.ELABORAZIONE_ID))
//                .where(REEA_L_FILE_ELABORAZIONE.FILE_ID.eq((int) fileId.longValue()))
//                .stream().map(r -> {
//                    ScartoFileDTO dto = new ScartoFileDTO();
//                    dto.setNumeroRiga(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.NUMERO_RIGA));
//                    dto.setCodiceErrore(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.CODICE_ERRORE));
//                    dto.setDescrizioneErrore(r.get(REEA_L_FILE_ELABORAZIONE_ERRORE.DESCRIZIONE_ERRORE));
//                    dto.setContenutoRigaRaw(Optional.ofNullable(r.get((Field<byte[]>) REEA_L_FILE_ELABORAZIONE_ERRORE.CONTENUTO_RIGA_RAW))
//                            .map(b -> new String(b, StandardCharsets.UTF_8)).orElse(null));
//                    dto.setCognome(r.get(DSL.field("cognome", String.class)));
//                    dto.setNome(r.get(DSL.field("nome", String.class)));
//                    dto.setDataNascita(r.get(DSL.field("data_nascita", LocalDate.class)) != null
//                            ? r.get(DSL.field("data_nascita", LocalDate.class)).toString() : null);
//                    dto.setSesso(r.get(DSL.field("sesso", String.class)));
//                    dto.setCodiceFiscale(r.get(DSL.field("codice_fiscale", String.class)));
//                    return dto;
//                }).collect(Collectors.toList());
//    }




//    @Override
//    public List<ArchivioFileCaricatiDTO> listaArchivioFileCaricati(String filtroFonte) {
//
//        Condition condFiltroFonte = noCondition();
//
//        if (filtroFonte != null && !filtroFonte.isBlank()) {
//            condFiltroFonte = DSL.field("split_part({0}, '/', 4)", String.class, REEA_T_FILE.FILE_PATH)
//                    .eq(filtroFonte);
//        }
//
//        var query = dsl
//            .select(
//                REEA_T_FILE.FILE_NAME,
//                REEA_T_FILE.FILE_PATH,
//                REEA_T_FILE.FILE_STATO_ID,
//                REEA_L_FILE_ELABORAZIONE.RIGHE_TOTALI,
//                REEA_L_FILE_ELABORAZIONE.RIGHE_ELABORATE,
//                REEA_L_FILE_ELABORAZIONE.RIGHE_SCARTATE,
//                REEA_T_FILE.DATA_CREAZIONE,
//                REEA_T_FILE.UTENTE_CREAZIONE,
//                REEA_D_FILE_STATO.FILE_STATO_DESC
//            )
//            .from(REEA_T_FILE)
//            .join(REEA_L_FILE_ELABORAZIONE)
//                .on(REEA_L_FILE_ELABORAZIONE.FILE_ID.eq(REEA_T_FILE.FILE_ID))
//            .join(REEA_D_FILE_STATO)
//                .on(REEA_D_FILE_STATO.FILE_STATO_ID.eq(REEA_T_FILE.FILE_STATO_ID))
//            .where(condFiltroFonte);
//
//        return query.stream().map(r -> {
//            ArchivioFileCaricatiDTO row = new ArchivioFileCaricatiDTO();
//            row.setNomeFile(r.get(REEA_T_FILE.FILE_NAME));
//            row.setFonteProvenienza(r.get(REEA_T_FILE.FILE_PATH));
//            row.setStato(r.get(REEA_D_FILE_STATO.FILE_STATO_DESC));
//            row.setRecordTotali(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_TOTALI));
//            row.setRecordElaborati(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_ELABORATE));
//            row.setRecordScartati(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_SCARTATE));
//            row.setDataCaricamento(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_FILE.DATA_CREAZIONE)));
//            row.setOperatore(r.get(REEA_T_FILE.UTENTE_CREAZIONE));
//            return row;
//        }).collect(Collectors.toList());
//    }
    
    
//    @Override
//    public List<ArchivioFileCaricatiDTO> listaArchivioFileCaricati(String filtroFonte) {
//    	
//    	 // 1) condizioni dinamiche
//        Condition condFiltroFonte = noCondition();
//        if (filtroFonte != null && !filtroFonte.isEmpty()) {
//        	condFiltroFonte = REEA_T_FILE.FILE_PATH.contains(filtroFonte);
//        }
//
//        var query = dsl
//            .select(
//            		REEA_T_FILE.FILE_NAME,
//            		REEA_T_FILE.FILE_PATH,
//            		REEA_T_FILE.FILE_STATO_ID,
//            		REEA_L_FILE_ELABORAZIONE.RIGHE_TOTALI,
//            		REEA_L_FILE_ELABORAZIONE.RIGHE_ELABORATE,
//            		REEA_L_FILE_ELABORAZIONE.RIGHE_SCARTATE,
//            		REEA_T_FILE.DATA_CREAZIONE,
//            		REEA_T_FILE.UTENTE_CREAZIONE,
//            		REEA_D_FILE_STATO.FILE_STATO_DESC
//            )
//            .from(REEA_T_FILE)
//            .join(REEA_L_FILE_ELABORAZIONE).on(REEA_L_FILE_ELABORAZIONE.FILE_ID.eq(REEA_T_FILE.FILE_ID))
//            .join(REEA_D_FILE_STATO).on(REEA_D_FILE_STATO.FILE_STATO_ID.eq(REEA_T_FILE.FILE_STATO_ID))
////            .where(REEA_T_ADESIONE.CODICE_FISCALE.in("CLLXXX99R62C938A"));
//        .where(condFiltroFonte);
//
//        // fetch finale
//		List<ArchivioFileCaricatiDTO> result = query.stream().map(r -> {
//    	
//		ArchivioFileCaricatiDTO row = new ArchivioFileCaricatiDTO();
//		row.setNomeFile(r.get(REEA_T_FILE.FILE_NAME));
//		row.setFonteProvenienza(r.get(REEA_T_FILE.FILE_PATH));
//		row.setStato(r.get(REEA_D_FILE_STATO.FILE_STATO_DESC));
//		row.setRecordTotali(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_TOTALI));
//		row.setRecordElaborati(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_ELABORATE));
//		row.setRecordScartati(r.get(REEA_L_FILE_ELABORAZIONE.RIGHE_SCARTATE));
////		row.setDataCaricamento(r.get(REEA_T_FILE.DATA_CREAZIONE));
//		row.setDataCaricamento(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_FILE.DATA_CREAZIONE)));
//		row.setOperatore(r.get(REEA_T_FILE.UTENTE_CREAZIONE));
//        
//        return row;
//	})
//    .collect(Collectors.toList());
//		return result;
//    }

   
}

