package it.csi.registry.repositories;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
//import static it.csi.registry.jooq.tables.ReeaTAdesione.REEA_T_ADESIONE;
import it.csi.registry.model.FonteConDataDTO;
import it.csi.registry.util.DateConversionUtils;

import static it.csi.registry.jooq.tables.ReeaDFonte.REEA_D_FONTE;
import static it.csi.registry.jooq.tables.ReeaSSoggetto.REEA_S_SOGGETTO;
import static it.csi.registry.jooq.tables.ReeaTSoggetto.REEA_T_SOGGETTO;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.val;
import static it.csi.registry.jooq.tables.ReeaTRegistroInail.REEA_T_REGISTRO_INAIL;
import static it.csi.registry.jooq.tables.ReeaTRegistroPdlAmianto.REEA_T_REGISTRO_PDL_AMIANTO;
import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalAnamnesi.REEA_T_REGISTRO_SPRESAL_ANAMNESI;
import static it.csi.registry.jooq.tables.ReeaTRegistro.REEA_T_REGISTRO;
import static it.csi.registry.jooq.tables.ReeaTAdesione.REEA_T_ADESIONE;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class FonteRepositoryImpl implements FonteRepository {


    private final DSLContext dsl;

    public FonteRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

//    @Override
//    public List<FonteConDataDTO> findListaFonteDescByIdSoggetto(Long soggettoId) {
//
//        Field<LocalDateTime> adesioneDataField = dsl
//                .select(DSL.max(REEA_T_ADESIONE.ADESIONE_DATA))
//                .from(REEA_T_ADESIONE)
//                .where(REEA_T_ADESIONE.SOGGETTO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_ID))
//                .asField();
//    	
//
//
//        List<Record3<Integer, String, LocalDateTime>> recordsUniti = dsl
//                .select(REEA_S_SOGGETTO.FONTE_ID,
//                        REEA_D_FONTE.FONTE_DESC,
//                        REEA_S_SOGGETTO.DATA_CREAZIONE)
//                .from(REEA_S_SOGGETTO)
//                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
//                .where(REEA_S_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
//                .unionAll(
//                        dsl.select(
//                                        REEA_T_SOGGETTO.FONTE_ID,
//                                        REEA_D_FONTE.FONTE_DESC,
//                                        DSL.when(REEA_T_SOGGETTO.FONTE_ID.eq(2), adesioneDataField)   //adesioneDataField       // Preadesione → adesione_data
//                                                .when(REEA_T_SOGGETTO.FONTE_ID.eq(1), REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA) // MMG → presentazione_istanza_data
//                                                .otherwise(REEA_T_SOGGETTO.DATA_CREAZIONE))                       // altri → data_creazione
//                                .from(REEA_T_SOGGETTO)
//                                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
//                                .where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
//                )
//                .orderBy(DSL.field("3").desc())
//                .fetch();
//
//        return recordsUniti.stream()
//                .map(record -> {
//                    FonteConDataDTO dto = new FonteConDataDTO();
//                    dto.setFonteDesc(record.get(REEA_D_FONTE.FONTE_DESC));
//                    LocalDateTime data = record.get(2, LocalDateTime.class);
//                    dto.setDataCreazione(data != null ? data.toString() : null);
//                    dto.setDataAdesione(data != null ? data.toString() : null);
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }
    
    
    @Override
    public List<FonteConDataDTO> findListaFonteDescByIdSoggetto(Long soggettoId) {

        // Data specifica per Preadesione
        Field<LocalDateTime> adesioneDataField = dsl
                .select(DSL.max(REEA_T_ADESIONE.ADESIONE_DATA))
                .from(REEA_T_ADESIONE)
                .where(REEA_T_ADESIONE.SOGGETTO_ID.eq(soggettoId.intValue()))
                .asField();

        // Data effettiva di importazione INAIL (fonte_id = 3)
        Field<LocalDateTime> inailDataField = dsl
                .select(DSL.max(REEA_T_REGISTRO_INAIL.DATA_CREAZIONE))
                .from(REEA_T_REGISTRO_INAIL)
                .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.REGISTRO_ID.eq(REEA_T_REGISTRO_INAIL.REGISTRO_ID))
                .where(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                        field("reea.hmac_soggetto_id({0}, {1})",
                                String.class,
                                soggettoId.intValue(),
                                val("16<odcc8!"))))
                .asField();

        // Data effettiva di importazione NPLA/Piani di Lavoro (fonte_id = 4)
        Field<LocalDateTime> nplaDataField = dsl
                .select(DSL.max(REEA_T_REGISTRO_PDL_AMIANTO.DATA_CREAZIONE))
                .from(REEA_T_REGISTRO_PDL_AMIANTO)
                .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.REGISTRO_ID.eq(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID))
                .where(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                        field("reea.hmac_soggetto_id({0}, {1})",
                                String.class,
                                soggettoId.intValue(),
                                val("16<odcc8!"))))
                .asField();

        // Data effettiva di importazione SPRESAL (fonte_id = 5)
        Field<LocalDateTime> spresalDataField = dsl
                .select(DSL.max(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CREAZIONE))
                .from(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
                .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.REGISTRO_ID.eq(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID))
                .where(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                        field("reea.hmac_soggetto_id({0}, {1})",
                                String.class,
                                soggettoId.intValue(),
                                val("16<odcc8!"))))
                .asField();

        List<Record5<Integer, String, LocalDateTime, LocalDateTime, Integer>> recordsUniti = dsl
                .select(
                        REEA_S_SOGGETTO.FONTE_ID,
                        REEA_D_FONTE.FONTE_DESC,
                        // Data import reale per fonte: INAIL→reea_t_registro_inail, NPLA→reea_t_registro_pdl_amianto, altri→data_creazione storico
                        DSL.when(REEA_S_SOGGETTO.FONTE_ID.eq(1), REEA_S_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA)
                                .when(REEA_S_SOGGETTO.FONTE_ID.eq(2), adesioneDataField)
                                .when(REEA_S_SOGGETTO.FONTE_ID.eq(3), inailDataField)
                                .when(REEA_S_SOGGETTO.FONTE_ID.eq(4), nplaDataField)
                                .when(REEA_S_SOGGETTO.FONTE_ID.eq(5), spresalDataField)
                                .otherwise(REEA_S_SOGGETTO.DATA_CREAZIONE)
                                .as("data_creazione"),
                        DSL.when(REEA_S_SOGGETTO.FONTE_ID.eq(2), adesioneDataField)
                                .otherwise(DSL.inline(null, LocalDateTime.class))
                                .as("data_adesione"),
                        REEA_S_SOGGETTO.VERSIONE_NUMERO
                )
                .from(REEA_S_SOGGETTO)
                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
                .where(REEA_S_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
                .unionAll(
                        dsl.select(
                                        REEA_T_SOGGETTO.FONTE_ID,
                                        REEA_D_FONTE.FONTE_DESC,
                                        DSL.when(REEA_T_SOGGETTO.FONTE_ID.eq(1), REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA)
                                                .when(REEA_T_SOGGETTO.FONTE_ID.eq(2), adesioneDataField)
                                                .when(REEA_T_SOGGETTO.FONTE_ID.eq(3), inailDataField)
                                                .when(REEA_T_SOGGETTO.FONTE_ID.eq(4), nplaDataField)
                                                .when(REEA_T_SOGGETTO.FONTE_ID.eq(5), spresalDataField)
                                                .otherwise(REEA_T_SOGGETTO.DATA_CREAZIONE)
                                                .as("data_creazione"),
                                        DSL.when(REEA_T_SOGGETTO.FONTE_ID.eq(2), adesioneDataField)
                                                .otherwise(DSL.inline(null, LocalDateTime.class))
                                                .as("data_adesione"),
                                        REEA_T_SOGGETTO.VERSIONE_NUMERO
                                )
                                .from(REEA_T_SOGGETTO)
                                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
                                .where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
                )
                .orderBy(DSL.field("5").desc(), DSL.field("3").desc())
                .fetch();

        // LinkedHashMap per preservare l'ordine SQL (versione più recente prima)
        return recordsUniti.stream()
                .map(record -> {
                    FonteConDataDTO dto = new FonteConDataDTO();
                    dto.setFonteDesc(record.get(REEA_D_FONTE.FONTE_DESC));
                    dto.setDataCreazione(DateConversionUtils.localDateTimeToString(record.get(2, LocalDateTime.class)));
                    dto.setDataAdesione(DateConversionUtils.localDateTimeToString(record.get(3, LocalDateTime.class)));
                    return dto;
                })
                .collect(Collectors.toMap(
                        FonteConDataDTO::getFonteDesc,
                        dto -> dto,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new   // preserva ordine SQL: più recente prima
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    

    @Override
    public List<FonteConDataDTO> findListaFonteDescByIdSoggettoInail(Long soggettoId) {

        Field<LocalDateTime> inailDataField = dsl
                .select(DSL.max(REEA_T_REGISTRO_INAIL.DATA_CREAZIONE))
                .from(REEA_T_REGISTRO_INAIL)
                .join(REEA_T_REGISTRO).on(REEA_T_REGISTRO.REGISTRO_ID.eq(REEA_T_REGISTRO_INAIL.REGISTRO_ID))
                .where(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                        field("reea.hmac_soggetto_id({0}, {1})",
                                String.class,
                                soggettoId.intValue(),
                                val("16<odcc8!"))))
                .asField();


        List<Record3<Integer, String, LocalDateTime>> recordsUniti = dsl
                .select(REEA_S_SOGGETTO.FONTE_ID,
                        REEA_D_FONTE.FONTE_DESC,
                        REEA_S_SOGGETTO.DATA_CREAZIONE)
                .from(REEA_S_SOGGETTO)
                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_S_SOGGETTO.FONTE_ID))
                .where(REEA_S_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
                .unionAll(
                        dsl.select(
                                        REEA_T_SOGGETTO.FONTE_ID,
                                        REEA_D_FONTE.FONTE_DESC,
                                        DSL.when(REEA_T_SOGGETTO.FONTE_ID.eq(3), inailDataField)          // Preadesione → adesione_data
                                                .when(REEA_T_SOGGETTO.FONTE_ID.eq(1), REEA_T_SOGGETTO.PRESENTAZIONE_ISTANZA_DATA) // MMG → presentazione_istanza_data
                                                .otherwise(REEA_T_SOGGETTO.DATA_CREAZIONE))                       // altri → data_creazione
                                .from(REEA_T_SOGGETTO)
                                .join(REEA_D_FONTE).on(REEA_D_FONTE.FONTE_ID.eq(REEA_T_SOGGETTO.FONTE_ID))
                                .where(REEA_T_SOGGETTO.SOGGETTO_ID.eq(soggettoId.intValue()))
                )
                .orderBy(DSL.field("3").desc())
                .fetch();

        return recordsUniti.stream()
                .map(record -> {
                    FonteConDataDTO dto = new FonteConDataDTO();
                    dto.setFonteDesc(record.get(REEA_D_FONTE.FONTE_DESC));
                    LocalDateTime data = record.get(2, LocalDateTime.class);
                    dto.setDataCreazione(data != null ? data.toString() : null);
                    return dto;
                })
                .collect(Collectors.toMap(
                        FonteConDataDTO::getFonteDesc,
                        dto -> dto,
                        (existing, replacement) -> existing  // mantieni il primo (versione più recente)
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

}