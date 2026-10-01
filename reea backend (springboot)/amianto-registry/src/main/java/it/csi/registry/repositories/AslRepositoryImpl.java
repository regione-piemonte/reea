package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaDAsl.REEA_D_ASL;

import java.text.Normalizer;
import java.util.List;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.AslDTO;

@Repository
public class AslRepositoryImpl implements AslRepository {

    private final DSLContext dsl;

    public AslRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<AslDTO> findAllAslCompetenza() {
    	
        return dsl
            .select(REEA_D_ASL.ASL_AZIENDA_DESC,
                    REEA_D_ASL.ASL_ID,
                    REEA_D_ASL.ASL_COD
                    )
            .from(REEA_D_ASL)
            .where(REEA_D_ASL.ASL_REGIONE_COD.eq("010"))
            .fetch(record -> {
                AslDTO result = new AslDTO();
                result.setAslAziendaDesc(record.get(REEA_D_ASL.ASL_AZIENDA_DESC));
                result.setAslId(record.get(REEA_D_ASL.ASL_ID));
                result.setAslCod(record.get(REEA_D_ASL.ASL_COD));
                return result;
            });
    }

    @Override
    public Integer getAslIdByAslCod(String parametroAsl) {

    	return dsl
            .select(REEA_D_ASL.ASL_ID)
            .from(REEA_D_ASL)
            .where(REEA_D_ASL.ASL_COD.eq(parametroAsl))
            .fetchOne(REEA_D_ASL.ASL_ID);   // Integer o null

    }



    @Override
    public AslDTO findByCod(String aslCod) {
        return dsl
                .select(REEA_D_ASL.ASL_ID, REEA_D_ASL.ASL_AZIENDA_DESC, REEA_D_ASL.ASL_COD)
                .from(REEA_D_ASL)
                .where(REEA_D_ASL.ASL_COD.eq(aslCod))
                .fetchOne(record -> {
                    AslDTO r = new AslDTO();
                    r.setAslId(record.get(REEA_D_ASL.ASL_ID));
                    r.setAslAziendaDesc(record.get(REEA_D_ASL.ASL_AZIENDA_DESC));
                    r.setAslCod(record.get(REEA_D_ASL.ASL_COD));
                    return r;
                });
    }

    @Override
    public Integer getAslIdByAslDesc(String aslDesc) {
        if (aslDesc == null || aslDesc.isBlank()) return null;
        String needle = normalizeAsl(aslDesc);

        return dsl.select(REEA_D_ASL.ASL_ID, REEA_D_ASL.ASL_AZIENDA_DESC)
                .from(REEA_D_ASL)
                .where(REEA_D_ASL.VALIDITA_FINE.isNull()) // solo ASL attive
                .fetch()
                .stream()
                .filter(r -> {
                    String dbDesc = r.get(REEA_D_ASL.ASL_AZIENDA_DESC);
                    return dbDesc != null && normalizeAsl(dbDesc).contains(needle);
                })
                .map(r -> r.get(REEA_D_ASL.ASL_ID))
                .findFirst()
                .orElse(null);
    }

    private String normalizeAsl(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // rimuove accenti (à→a, è→e...)
                .replace("'", "").replace("\u2019", "").replace("`", "") // rimuove apostrofi
                .toUpperCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Override
    public List<AslDTO> findListaFiltroAssistiti() {
        return dsl
                .select(REEA_D_ASL.ASL_AZIENDA_DESC, REEA_D_ASL.ASL_ID, REEA_D_ASL.ASL_COD)
                .from(REEA_D_ASL)
                .where(REEA_D_ASL.ASL_REGIONE_COD.in("010", "999"))
                .orderBy(REEA_D_ASL.ASL_AZIENDA_DESC)
                .fetch(record -> {
                    AslDTO result = new AslDTO();
                    result.setAslAziendaDesc(record.get(REEA_D_ASL.ASL_AZIENDA_DESC));
                    result.setAslId(record.get(REEA_D_ASL.ASL_ID));
                    result.setAslCod(record.get(REEA_D_ASL.ASL_COD));
                    return result;
                });
    }

    @Override
    public AslDTO findById(Integer aslId) {
        return dsl.selectFrom(REEA_D_ASL)
                .where(REEA_D_ASL.ASL_ID.eq(aslId))
                .fetchOneInto(AslDTO.class);
    }


}

