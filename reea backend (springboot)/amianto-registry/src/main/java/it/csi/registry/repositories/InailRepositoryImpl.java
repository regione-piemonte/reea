package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaTAdesione.REEA_T_ADESIONE;
import static it.csi.registry.jooq.tables.ReeaTRegistroInail.REEA_T_REGISTRO_INAIL;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.InailDTO;
import it.csi.registry.model.RegistroDTO;
import it.csi.registry.services.TracciaElaborazioneService;
import it.csi.registry.util.DateConversionUtils;
import it.csi.registry.util.ErroreImportUtility;
import it.csi.registry.util.RegistroUtils;

import static org.jooq.impl.DSL.currentLocalDateTime;

@Repository
public class InailRepositoryImpl implements InailRepository {

    private final DSLContext dsl;
    private final TracciaElaborazioneRepository tracciaElaborazioneRepository;
    private final TracciaElaborazioneService tracciaElaborazioneService;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InailRepositoryImpl.class);
 	

    public InailRepositoryImpl(DSLContext dsl, TracciaElaborazioneRepository tracciaElaborazioneRepository, TracciaElaborazioneService tracciaElaborazioneService) {
        this.dsl = dsl;
        this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
    }

//    @Override
//    public void deleteRecordNonProcessati() {
//        dsl.deleteFrom(REEA_T_ADESIONE)
//                .where(REEA_T_ADESIONE.SOGGETTO_ID.isNull())
//                .execute();
//    }

    @Override
    public void deleteRecordNonProcessati(String tipologiaInail) {
        dsl.deleteFrom(REEA_T_REGISTRO_INAIL)
                .where(REEA_T_REGISTRO_INAIL.REGISTRO_ID.isNull())
                .and(REEA_T_REGISTRO_INAIL.TIPO_ELENCO_INAIL.eq(tipologiaInail))
                .execute();
    }

    @Override
    public Set<String> getProcessedCodiciFiscali(String tipologiaInail) {
        return new HashSet<>(
                dsl.select(REEA_T_REGISTRO_INAIL.CODICE_FISCALE)
                        .from(REEA_T_REGISTRO_INAIL)
                        .where(REEA_T_REGISTRO_INAIL.REGISTRO_ID.isNotNull())
//                        .where(REEA_T_REGISTRO_INAIL.TIPO_ELENCO_INAIL.eq(tipologiaInail))
//                        .and(REEA_T_REGISTRO_INAIL.REGISTRO_ID.isNotNull())
                        .fetch(REEA_T_REGISTRO_INAIL.CODICE_FISCALE)
        );
    }

//    @Override
//    public List<InailDTO> listaRecordInail(String tipologiaInail, Integer fileId) {
//
//        var query = dsl
//            .select(
//            	REEA_T_REGISTRO_INAIL.REG_INAIL_ID,
//            	REEA_T_REGISTRO_INAIL.REGISTRO_ID,
//            	REEA_T_REGISTRO_INAIL.DOMANDA,
//            	REEA_T_REGISTRO_INAIL.COGNOME,
//            	REEA_T_REGISTRO_INAIL.NOME,
//            	REEA_T_REGISTRO_INAIL.CODICE_FISCALE,
//            	REEA_T_REGISTRO_INAIL.SESSO,
//            	
//            	REEA_T_REGISTRO_INAIL.DATA_NASCITA,
//            	REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA,
//            	REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA,
//            	REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA,
//
//            	REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA,
//            	REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA,
//            	REEA_T_REGISTRO_INAIL.VALIDITA_INIZIO,
//            	REEA_T_REGISTRO_INAIL.VALIDITA_FINE,
//            	REEA_T_REGISTRO_INAIL.DATA_CREAZIONE,
//            	REEA_T_REGISTRO_INAIL.DATA_MODIFICA,
//            	REEA_T_REGISTRO_INAIL.DATA_CANCELLAZIONE,
//            	REEA_T_REGISTRO_INAIL.UTENTE_CREAZIONE,
//            	REEA_T_REGISTRO_INAIL.UTENTE_MODIFICA,
//            	REEA_T_REGISTRO_INAIL.UTENTE_CANCELLAZIONE,
//            	REEA_T_REGISTRO_INAIL.FILE_ID
//            )
//            .from(REEA_T_REGISTRO_INAIL)
////            .where(REEA_T_ADESIONE.CODICE_FISCALE.in("LMBGPP55R15D314W"));
//        .where(REEA_T_REGISTRO_INAIL.REGISTRO_ID.isNull().and(REEA_T_REGISTRO_INAIL.TIPO_ELENCO_INAIL.eq(tipologiaInail))
//         .and(fileId != null ? REEA_T_REGISTRO_INAIL.FILE_ID.eq(fileId) : DSL.noCondition()));
//        // fetch finale
//		List<InailDTO> result = query.stream().map(r -> {
//    	
//		InailDTO row = new InailDTO();
//		row.setRegInailId(r.get(REEA_T_REGISTRO_INAIL.REG_INAIL_ID).longValue());
//        row.setRegistroId(r.get(REEA_T_REGISTRO_INAIL.REGISTRO_ID));
//        row.setDomanda(r.get(REEA_T_REGISTRO_INAIL.DOMANDA));
//        row.setNome(r.get(REEA_T_REGISTRO_INAIL.NOME));
//        row.setCognome(r.get(REEA_T_REGISTRO_INAIL.COGNOME));
//        row.setCodiceFiscale(r.get(REEA_T_REGISTRO_INAIL.CODICE_FISCALE));
//        row.setSesso(r.get(REEA_T_REGISTRO_INAIL.SESSO));
//        row.setDataNascita(r.get(REEA_T_REGISTRO_INAIL.DATA_NASCITA));
//        row.setIndirizzoResidenza(r.get(REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA));
////        row.setCapResidenza(r.get(REEA_T_REGISTRO_INAIL.CAP_RESIDENZA));
//        row.setIstatResidenza(r.get(REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA));
//        row.setComuneResidenza(r.get(REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA));
//        row.setProvinciaResidenza(r.get(REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA));
//        row.setRegioneResidenza(r.get(REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA));
//        row.setValiditaInizio(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.VALIDITA_INIZIO)));
//        row.setValiditaFine(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.VALIDITA_FINE)));
//        row.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.DATA_CREAZIONE)));
//        row.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.DATA_MODIFICA)));
//        row.setDataCancellazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.DATA_CANCELLAZIONE)));
//        row.setUtenteCreazione(r.get(REEA_T_REGISTRO_INAIL.UTENTE_CREAZIONE));
//        row.setUtenteModifica(r.get(REEA_T_REGISTRO_INAIL.UTENTE_MODIFICA));
//        row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_INAIL.UTENTE_CANCELLAZIONE));
//        row.setTipologiaInail(tipologiaInail);
//        row.setFileId(r.get(REEA_T_REGISTRO_INAIL.FILE_ID));
//        
//        return row;
//	})
//    .collect(Collectors.toList());
//		return result;
//    }
    
    @Override
    public List<InailDTO> listaRecordInail(String tipologiaInail, Integer fileId) {


        var query = dsl
                .select(
                        REEA_T_REGISTRO_INAIL.REG_INAIL_ID,
                        REEA_T_REGISTRO_INAIL.REGISTRO_ID,
                        REEA_T_REGISTRO_INAIL.DOMANDA,
                        REEA_T_REGISTRO_INAIL.COGNOME,
                        REEA_T_REGISTRO_INAIL.NOME,
                        REEA_T_REGISTRO_INAIL.CODICE_FISCALE,
                        REEA_T_REGISTRO_INAIL.SESSO,
                        REEA_T_REGISTRO_INAIL.DATA_NASCITA,
                        REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA,
                        REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA,
                        REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA,
                        REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA,
                        REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA,
                        REEA_T_REGISTRO_INAIL.VALIDITA_INIZIO,
                        REEA_T_REGISTRO_INAIL.VALIDITA_FINE,
                        REEA_T_REGISTRO_INAIL.DATA_CREAZIONE,
                        REEA_T_REGISTRO_INAIL.DATA_MODIFICA,
                        REEA_T_REGISTRO_INAIL.DATA_CANCELLAZIONE,
                        REEA_T_REGISTRO_INAIL.UTENTE_CREAZIONE,
                        REEA_T_REGISTRO_INAIL.UTENTE_MODIFICA,
                        REEA_T_REGISTRO_INAIL.UTENTE_CANCELLAZIONE,
                        REEA_T_REGISTRO_INAIL.FILE_ID
                )
                .from(REEA_T_REGISTRO_INAIL)
                .where(
                        REEA_T_REGISTRO_INAIL.REGISTRO_ID.isNull()
                                .and(REEA_T_REGISTRO_INAIL.VALIDITA_FINE.isNull())
                                .and(REEA_T_REGISTRO_INAIL.TIPO_ELENCO_INAIL.eq(tipologiaInail))
                               
                                /* il file id da batch non c'� e non ha senso
                               .and(fileId != null
                                        ? REEA_T_REGISTRO_INAIL.FILE_ID.eq(fileId)
                                        : DSL.noCondition())
                                        */
                );
    	System.out.println("-----> TIPOLOGIA ELENCO INAIL ="+tipologiaInail);
        System.out.println(query.getSQL());
        
        return query.fetch().stream().map(r -> {
            InailDTO row = new InailDTO();

            Integer regInailId = r.get(REEA_T_REGISTRO_INAIL.REG_INAIL_ID);
            row.setRegInailId(regInailId != null ? regInailId.longValue() : null);

            row.setRegistroId(r.get(REEA_T_REGISTRO_INAIL.REGISTRO_ID));
            row.setDomanda(r.get(REEA_T_REGISTRO_INAIL.DOMANDA));
            row.setNome(r.get(REEA_T_REGISTRO_INAIL.NOME));
            row.setCognome(r.get(REEA_T_REGISTRO_INAIL.COGNOME));
            row.setCodiceFiscale(r.get(REEA_T_REGISTRO_INAIL.CODICE_FISCALE));
            row.setSesso(r.get(REEA_T_REGISTRO_INAIL.SESSO));
            row.setDataNascita(r.get(REEA_T_REGISTRO_INAIL.DATA_NASCITA));
            row.setIndirizzoResidenza(r.get(REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA));
            row.setIstatResidenza(r.get(REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA));
            row.setComuneResidenza(r.get(REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA));
            row.setProvinciaResidenza(r.get(REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA));
            row.setRegioneResidenza(r.get(REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA));

            row.setValiditaInizio(
                    r.get(REEA_T_REGISTRO_INAIL.VALIDITA_INIZIO) != null
                            ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.VALIDITA_INIZIO))
                            : null
            );

            row.setValiditaFine(
                    r.get(REEA_T_REGISTRO_INAIL.VALIDITA_FINE) != null
                            ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.VALIDITA_FINE))
                            : null
            );

            row.setDataCreazione(
                    r.get(REEA_T_REGISTRO_INAIL.DATA_CREAZIONE) != null
                            ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.DATA_CREAZIONE))
                            : null
            );

            row.setDataModifica(
                    r.get(REEA_T_REGISTRO_INAIL.DATA_MODIFICA) != null
                            ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.DATA_MODIFICA))
                            : null
            );

            row.setDataCancellazione(
                    r.get(REEA_T_REGISTRO_INAIL.DATA_CANCELLAZIONE) != null
                            ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_INAIL.DATA_CANCELLAZIONE))
                            : null
            );

            row.setUtenteCreazione(r.get(REEA_T_REGISTRO_INAIL.UTENTE_CREAZIONE));
            row.setUtenteModifica(r.get(REEA_T_REGISTRO_INAIL.UTENTE_MODIFICA));
            row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_INAIL.UTENTE_CANCELLAZIONE));
            row.setTipologiaInail(tipologiaInail);
            row.setFileId(r.get(REEA_T_REGISTRO_INAIL.FILE_ID));

            return row;
        }).collect(Collectors.toList());
    }
    

//    @Override
//    public Set<String> getProcessedAdesioneCods() {
//        return new HashSet<>(
//                dsl.select(REEA_T_ADESIONE.ADESIONE_COD)
//                        .from(REEA_T_ADESIONE)
//                        .where(REEA_T_ADESIONE.SOGGETTO_ID.isNotNull())
//                        .fetch(REEA_T_ADESIONE.ADESIONE_COD)
//        );
//    }

    @Override
    public void scaricoDatiExcelInTRegistroInail(List<InailDTO> rows, String tipologiaInail, Integer fileId, String utenteCreazione) {
        for (InailDTO dto : rows) {
            try {
                LocalDateTime validitaFine      = DateConversionUtils.toStartOfDay(dto.getValiditaFine());
                LocalDateTime dataModifica      = DateConversionUtils.toStartOfDay(dto.getDataModifica());
                LocalDateTime dataCancellazione = DateConversionUtils.toStartOfDay(dto.getDataCancellazione());

                dsl.insertInto(REEA_T_REGISTRO_INAIL)
                   .set(REEA_T_REGISTRO_INAIL.DOMANDA, dto.getDomanda())
                   .set(REEA_T_REGISTRO_INAIL.COGNOME, dto.getCognome())
                   .set(REEA_T_REGISTRO_INAIL.NOME, dto.getNome())
                   .set(REEA_T_REGISTRO_INAIL.CODICE_FISCALE, dto.getCodiceFiscale())
                   .set(REEA_T_REGISTRO_INAIL.SESSO, dto.getSesso())
                   .set(REEA_T_REGISTRO_INAIL.DATA_NASCITA, dto.getDataNascita())
                   .set(REEA_T_REGISTRO_INAIL.INDIRIZZO_RESIDENZA, dto.getIndirizzoResidenza())
                   .set(REEA_T_REGISTRO_INAIL.CAP_RESIDENZA, dto.getCapResidenza())
                   .set(REEA_T_REGISTRO_INAIL.ISTAT_RESIDENZA, dto.getIstatResidenza())
                   .set(REEA_T_REGISTRO_INAIL.COMUNE_RESIDENZA, dto.getComuneResidenza())
                   .set(REEA_T_REGISTRO_INAIL.PROVINCIA_RESIDENZA, dto.getProvinciaResidenza())
                   .set(REEA_T_REGISTRO_INAIL.REGIONE_RESIDENZA, dto.getRegioneResidenza())
                   .set(REEA_T_REGISTRO_INAIL.VALIDITA_INIZIO, LocalDateTime.now())
                   .set(REEA_T_REGISTRO_INAIL.VALIDITA_FINE, validitaFine)
                   .set(REEA_T_REGISTRO_INAIL.DATA_MODIFICA, dataModifica)
                   .set(REEA_T_REGISTRO_INAIL.DATA_CANCELLAZIONE, dataCancellazione)
                   .set(REEA_T_REGISTRO_INAIL.UTENTE_CREAZIONE, utenteCreazione)
                   .set(REEA_T_REGISTRO_INAIL.DATA_CREAZIONE, DSL.currentLocalDateTime())
                   .set(REEA_T_REGISTRO_INAIL.UTENTE_MODIFICA, dto.getUtenteModifica())
                   .set(REEA_T_REGISTRO_INAIL.UTENTE_CANCELLAZIONE, dto.getUtenteCancellazione())
                   .set(REEA_T_REGISTRO_INAIL.TIPO_ELENCO_INAIL, tipologiaInail)
                   .set(REEA_T_REGISTRO_INAIL.FILE_ID, fileId)
                   .set(REEA_T_REGISTRO_INAIL.VALIDITA_INIZIO, DSL.currentLocalDateTime())
                   .execute();

            } catch (Exception e) {
            	String descrizioneErrore = ErroreImportUtility.buildErroreDescrizione(e);

                System.out.println("Errore inserendo CF=" + dto.getCodiceFiscale() + " " + descrizioneErrore);

                try {
                    tracciaElaborazioneService.inserisciScarto(
                    		fileId,
                            "ERR_ELABORAZIONE",
                            descrizioneErrore,
                            null,
                            utenteCreazione,
                            dto.getCognome(),
                            dto.getNome(),
                            dto.getDataNascita(),
                            dto.getSesso(),
                            dto.getCodiceFiscale(),
                            "REEA_T_REGISTRO_INAIL",
                            dto.getFileId() != null ? dto.getFileId() : 0
                    );
                } catch (Exception ex) {
//                    ex.printStackTrace();
                	LOGGER.error("Errore inserendo CF: ", dto.getCodiceFiscale(), ex);
                }
            }
        }
    }


    @Override
    public Integer aggiornaInailRegistroId(AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione) {
    	
        RegistroDTO registro = RegistroUtils.ensureRegistro(dto);
        Integer ris = 0;

        try {
           	ris = dsl.update(REEA_T_REGISTRO_INAIL)
                    .set(REEA_T_REGISTRO_INAIL.REGISTRO_ID, registro.getRegistroId())
                    // DE AGGIUNGO LA DATA FINE IN MODO DA NON ELEBORARE PIU' LA RIGA UNA SECONDA VOLTA SE SCARTATA O MENO
                    .set(REEA_T_REGISTRO_INAIL.VALIDITA_FINE, currentLocalDateTime())// timestamp corrente del DB
                    .where(REEA_T_REGISTRO_INAIL.CODICE_FISCALE.eq(dto.getCodiceFiscale()))
                    .execute();
        
        tracciaElaborazioneRepository.inserisciFileImpatto(
        		dsl,
                elaborazioneId,
                "REEA_T_REGISTRO_INAIL",
                dto.getRegInailId(),
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
    					"aggiornaInailRegistroId ha restituito " + ris + " per CF " + dto.getCodiceFiscale(),
    					null,
    					dto.getUtenteCreazione(),
    					dto.getCognome(),
    					dto.getNome(),
    					dto.getNascitaData(),
    					dto.getSesso(),
    					dto.getCodiceFiscale(),
    					"REEA_T_REGISTRO_INAIL",
    					dto.getRegInailId()
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
	public void chiudiTuttiRecordTabRegistroInailByFile(Integer fileId) {
    		dsl.update(REEA_T_REGISTRO_INAIL)
    				.set(REEA_T_REGISTRO_INAIL.VALIDITA_FINE, DSL.currentLocalDateTime())
    				.where(REEA_T_REGISTRO_INAIL.FILE_ID.eq(fileId))
    				.execute();	
	}

    
}

