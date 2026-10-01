package it.csi.registry.repositories;

import java.util.List;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.ComuneDTO;
import org.jooq.Condition;
import static org.jooq.impl.DSL.noCondition;
import static it.csi.registry.jooq.tables.ReeaDComune.REEA_D_COMUNE;

@Repository
public class ComuneRepositoryImpl implements ComuneRepository {

    private final DSLContext dsl;

    public ComuneRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<ComuneDTO> findComuneNascitaResidenzaAzienda(Integer provinciaId) {

        Condition condProvinciaId = noCondition();
        if (provinciaId != null) {
            condProvinciaId = condProvinciaId.and(REEA_D_COMUNE.PROVINCIA_ID.eq(provinciaId));
        }

        return dsl
            .select(REEA_D_COMUNE.COMUNE_DESC,
                    REEA_D_COMUNE.COMUNE_COD)
            .from(REEA_D_COMUNE)
            .where(condProvinciaId)
            .fetch(record -> {
                ComuneDTO result = new ComuneDTO();
                result.setComuneDesc(record.get(REEA_D_COMUNE.COMUNE_DESC));
                result.setComuneCod(record.get(REEA_D_COMUNE.COMUNE_COD));
                return result;
            });
    }

	@Override
	public String findComuneDescByComuneCod(String comuneCod) {
		
		return dsl
	            .select(REEA_D_COMUNE.COMUNE_DESC)
	            .from(REEA_D_COMUNE)
	            .where(REEA_D_COMUNE.COMUNE_COD.eq(comuneCod))
	            .fetchOne(REEA_D_COMUNE.COMUNE_DESC);
	}
	
	@Override
	public String findComuneCodByComuneDesc(String comuneDesc) {
		
		return dsl
	            .select(REEA_D_COMUNE.COMUNE_COD)
	            .from(REEA_D_COMUNE)
	            .where(REEA_D_COMUNE.COMUNE_DESC.eq(comuneDesc))
	            .fetchOne(REEA_D_COMUNE.COMUNE_COD);
	}
}

