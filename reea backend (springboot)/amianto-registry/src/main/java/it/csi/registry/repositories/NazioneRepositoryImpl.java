package it.csi.registry.repositories;

import java.util.List;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.NazioneDTO;

import static it.csi.registry.jooq.tables.ReeaDNazione.REEA_D_NAZIONE;

@Repository
public class NazioneRepositoryImpl implements NazioneRepository {

    private final DSLContext dsl;

    public NazioneRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<NazioneDTO> findAllStatoNascitaResidenza() {

        return dsl
            .select(REEA_D_NAZIONE.NAZIONE_DESC_IT,
                    REEA_D_NAZIONE.NAZIONE_ISTAT_COD)
            .from(REEA_D_NAZIONE)
            .fetch(record -> {
                NazioneDTO result = new NazioneDTO();
                result.setNazioneDescIt(record.get(REEA_D_NAZIONE.NAZIONE_DESC_IT));
                result.setNazioneIstatCod(record.get(REEA_D_NAZIONE.NAZIONE_ISTAT_COD));
                return result;
            });
    }

	@Override
	public String findDescByIstatCod(String istatCod) {
		return dsl
		        .select(REEA_D_NAZIONE.NAZIONE_DESC_IT)
		        .from(REEA_D_NAZIONE)
		        .where(REEA_D_NAZIONE.NAZIONE_ISTAT_COD.eq(istatCod))
		        .fetchOneInto(String.class);
	}
}
