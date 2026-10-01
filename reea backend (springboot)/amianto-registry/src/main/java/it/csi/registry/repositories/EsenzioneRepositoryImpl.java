package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaDEsenzione.REEA_D_ESENZIONE;
import static it.csi.registry.jooq.tables.ReeaRSoggettoEsenzione.REEA_R_SOGGETTO_ESENZIONE;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.val;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.EsenzioneDTO;

@Repository
public class EsenzioneRepositoryImpl implements EsenzioneRepository{
	
	private final DSLContext dsl;

    public EsenzioneRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

	@Override
	public Integer getEsenzioneIdByEsenzioneCod(String esenzioneCod, String diagnosiCod) {
		
		return dsl
	            .select(REEA_D_ESENZIONE.ESENZIONE_ID)
	            .from(REEA_D_ESENZIONE)
	            .where(REEA_D_ESENZIONE.ESENZIONE_COD.eq(esenzioneCod).and(REEA_D_ESENZIONE.DIAGNOSI_COD.eq(diagnosiCod)))
	            .fetchOne(REEA_D_ESENZIONE.ESENZIONE_ID);   // Integer o null
	}

	
	
	@Override
	public List<EsenzioneDTO> findListaEsenzioniByIdSoggetto(Long soggettoId) {
		
		var query =  dsl.select(
		        REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ESENZIONE_ID,
//		        DSL.field(
//		            "reea.hmac_soggetto_id({0}, {1})",
//		            String.class,
//		            REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC,
//		            DSL.val("16<odcc8!")
//		        ).as("soggetto_id_hmac"),
				REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_EMISSIONE,
				REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_SCADENZA,
				REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_CIFRATO,
				REEA_D_ESENZIONE.ESENZIONE_COD,
				REEA_D_ESENZIONE.DIAGNOSI_COD,
				REEA_D_ESENZIONE.DIAGNOSI_DESC,
				REEA_D_ESENZIONE.ESENZIONE_DESC)
		    .from(REEA_R_SOGGETTO_ESENZIONE)
		    .join(REEA_D_ESENZIONE)
		    .on(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_ID_HMAC.eq(
		            DSL.field("reea.text_to_hmac(cast({0} as text), cast({1} as text))",
		                String.class,
		                REEA_D_ESENZIONE.ESENZIONE_ID,     // <--- ID in chiaro
		                DSL.val("16<odcc8!")
		            )))
            .where(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ID_HMAC.eq(
            		field(
            			    "reea.hmac_soggetto_id(cast({0} as int4), cast({1} as text))",
            			    String.class,
            			    val(soggettoId),           // Long o Integer
            			    val("16<odcc8!")
            				 )));

		    
		    List<EsenzioneDTO> result = query.stream().map(record -> {
			
			       EsenzioneDTO row = new EsenzioneDTO();
			       
			       row.setSoggettoId(record.get(REEA_R_SOGGETTO_ESENZIONE.SOGGETTO_ESENZIONE_ID).toString());
			       row.setEsenzioneDataEmissione(record.get(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_EMISSIONE));
			       row.setEsenzioneDataScadenza(record.get(REEA_R_SOGGETTO_ESENZIONE.ESENZIONE_DATA_SCADENZA));
			       row.setEsenzioneCod(record.get(REEA_D_ESENZIONE.ESENZIONE_COD));
			       row.setEsenzioneDesc(record.get(REEA_D_ESENZIONE.ESENZIONE_DESC));
			       row.setDiagnosiCod(record.get(REEA_D_ESENZIONE.DIAGNOSI_COD));
			       row.setDiagnosiDesc(record.get(REEA_D_ESENZIONE.DIAGNOSI_DESC));
			       
			       return row;
			})
		    .collect(Collectors.toList());
				return result;
		    }

	@Override
	public EsenzioneDTO findDescrizioniByCods(String esenzioneCod, String diagnosiCod) {
		return dsl.select(
						REEA_D_ESENZIONE.ESENZIONE_DESC,
						REEA_D_ESENZIONE.DIAGNOSI_DESC)
				.from(REEA_D_ESENZIONE)
				.where(REEA_D_ESENZIONE.ESENZIONE_COD.eq(esenzioneCod)
						.and(REEA_D_ESENZIONE.DIAGNOSI_COD.eq(diagnosiCod)))
				.fetchOne(r -> {
					EsenzioneDTO dto = new EsenzioneDTO();
					dto.setEsenzioneDesc(r.get(REEA_D_ESENZIONE.ESENZIONE_DESC));
					dto.setDiagnosiDesc(r.get(REEA_D_ESENZIONE.DIAGNOSI_DESC));
					return dto;
				});
	}


}



		    
		    
