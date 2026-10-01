package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaDComune.REEA_D_COMUNE;
import static it.csi.registry.jooq.tables.ReeaDProvincia.REEA_D_PROVINCIA;

import java.util.List;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.ProvinciaDTO;

@Repository
public class ProvinciaRepositoryImpl implements ProvinciaRepository {

    private final DSLContext dsl;

    public ProvinciaRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<ProvinciaDTO> findAllProvinciaNascitaResidenza() {

        return dsl
            .select(REEA_D_PROVINCIA.PROVINCIA_SIGLA,
                    REEA_D_PROVINCIA.PROVINCIA_DESC,
                    REEA_D_PROVINCIA.PROVINCIA_ID,
                    REEA_D_PROVINCIA.PROVINCIA_COD)
            .from(REEA_D_PROVINCIA)
            .fetch(record -> {
                ProvinciaDTO result = new ProvinciaDTO();
                result.setProvinciaSigla(record.get(REEA_D_PROVINCIA.PROVINCIA_SIGLA));
                result.setProvinciaDesc(record.get(REEA_D_PROVINCIA.PROVINCIA_DESC));
                result.setProvinciaId(record.get(REEA_D_PROVINCIA.PROVINCIA_ID));
                result.setProvinciaCod(record.get(REEA_D_PROVINCIA.PROVINCIA_COD));
                return result;
            });
    }

    @Override
    public ProvinciaDTO findProvinciaWithComune(String comuneCod) {
        try {
            return dsl
                .select(
                    REEA_D_PROVINCIA.PROVINCIA_SIGLA,
                    REEA_D_PROVINCIA.PROVINCIA_DESC,
                    REEA_D_PROVINCIA.PROVINCIA_ID,
                    REEA_D_PROVINCIA.PROVINCIA_COD
                )
                .from(REEA_D_PROVINCIA)
                .join(REEA_D_COMUNE)
                    .on(REEA_D_COMUNE.PROVINCIA_ID.eq(REEA_D_PROVINCIA.PROVINCIA_ID))
                .where(REEA_D_COMUNE.COMUNE_COD.eq(comuneCod))
                .fetchOne(record -> {
                    ProvinciaDTO result = new ProvinciaDTO();
                    result.setProvinciaSigla(record.get(REEA_D_PROVINCIA.PROVINCIA_SIGLA));
                    result.setProvinciaDesc(record.get(REEA_D_PROVINCIA.PROVINCIA_DESC));
                    result.setProvinciaId(record.get(REEA_D_PROVINCIA.PROVINCIA_ID));
                    result.setProvinciaCod(record.get(REEA_D_PROVINCIA.PROVINCIA_COD));
                    return result;
                });
        } catch (Exception e) {
            // meglio loggare l'errore
            // log.error("Errore nel recupero provincia per comuneCod={}", comuneCod, e);
            return null;
        }
    }

    
	@Override
	public String findProvinciaCodByProvinciaDesc(String provinciaDesc) {
		
		return dsl
	            .select(REEA_D_PROVINCIA.PROVINCIA_COD)
	            .from(REEA_D_PROVINCIA)
	            .where(REEA_D_PROVINCIA.PROVINCIA_DESC.eq(provinciaDesc))
	            .fetchOne(REEA_D_PROVINCIA.PROVINCIA_COD);
	}
}