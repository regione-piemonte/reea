package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaTRegistroInail.REEA_T_REGISTRO_INAIL;
import static it.csi.registry.jooq.tables.ReeaTRegistroPdlAmianto.REEA_T_REGISTRO_PDL_AMIANTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import it.csi.registry.model.NplaDTO;
import it.csi.registry.services.TracciaElaborazioneService;
import it.csi.registry.util.ErroreImportUtility;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jooq.impl.DSL.currentLocalDateTime;

@Repository
public class NplaRepositoryImpl implements NplaRepository {

    private final DSLContext dsl;
    private final TracciaElaborazioneService tracciaElaborazioneService;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NplaRepositoryImpl.class);

    public NplaRepositoryImpl(DSLContext dsl, TracciaElaborazioneService tracciaElaborazioneService) {
        this.dsl = dsl;
        this.tracciaElaborazioneService = tracciaElaborazioneService;
    }

    @Override
    public void deleteRecordNonProcessati() {
        dsl.deleteFrom(REEA_T_REGISTRO_PDL_AMIANTO)
                .where(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.isNull())
                .execute();
    }
    
    
    @Override
    public void scaricaNpla(List<NplaDTO> rows, Integer fileId, String utenteCreazione) {
        for (NplaDTO dto : rows) {
            try {
                // 1) Verifica duplicato su (CODICE_FISCALE, ID_CANTIERE)
                Boolean esiste = dsl
                    .selectOne()
                    .from(REEA_T_REGISTRO_PDL_AMIANTO)
                    .where(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE.eq(dto.getCodiceFiscale()))
                    .and(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE.eq(dto.getIdCantiere()))
                    .fetchOne() != null;

                if (esiste) {
                    // Record duplicato: lo tracci come scarto e salti l'inserimento
                    String descrizioneErrore = "Record duplicato su CODICE_FISCALE e ID_CANTIERE";

                    System.out.println("Scarto per duplicato CF=" + dto.getCodiceFiscale() +
                                       ", ID_CANTIERE=" + dto.getIdCantiere());

                    tracciaElaborazioneService.inserisciScarto(
                            fileId,
                            "ERR_DUPLICATO",
                            descrizioneErrore,
                            null,
                            utenteCreazione,
                            dto.getCognome(),
                            dto.getNome(),
                            dto.getDataNascita(),
                            dto.getSesso(),
                            dto.getCodiceFiscale(),
                            "REEA_T_REGISTRO_PDL_AMIANTO",
                            dto.getFileId() != null ? dto.getFileId() : 0
                    );

                    continue; // salto all'iterazione successiva
                }

                // 2) Inserimento vero e proprio (solo se non duplicato)
                dsl.insertInto(REEA_T_REGISTRO_PDL_AMIANTO)
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE, dto.getCodiceFiscale())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.PERIODO, dto.getPeriodo())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE,    dto.getIdCantiere())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA,   dto.getAziendaPiva())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME,   dto.getAziendaNome())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE,   dto.getAslCantiere())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE,dto.getComuneCantiere())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.ANNO,           dto.getAnno())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO,dto.getTipologiaPiano())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE, dto.getQuantitaDaRimuovere())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA,      dto.getQuantitaRimossa())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.FILE_ID, fileId)
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_INIZIO,
                            dto.getValiditaInizio() != null
                                    ? dto.getValiditaInizio().toLocalDateTime()
                                    : java.time.LocalDateTime.now())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE,
                            dto.getValiditaFine() != null
                                    ? dto.getValiditaFine().toLocalDateTime()
                                    : null)
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.DATA_CREAZIONE,  DSL.currentLocalDateTime())
                    .set(REEA_T_REGISTRO_PDL_AMIANTO.UTENTE_CREAZIONE, utenteCreazione)
                    // hai due set su VALIDITA_INIZIO; tieni solo quello corretto, ad esempio:
                    // .set(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_INIZIO, DSL.currentLocalDateTime())
                    // oppure usa quello del DTO come sopra
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
                            "REEA_T_REGISTRO_PDL_AMIANTO",
                            dto.getFileId() != null ? dto.getFileId() : 0
                    );
                } catch (Exception ex) {
                    LOGGER.error("Errore inserendo CF: {}", dto.getCodiceFiscale(), ex);
                }
            }
        }
    }
// ORIGINALE    
//    public void scaricaNpla(List<NplaDTO> rows, Integer fileId, String utenteCreazione) {
//        for (NplaDTO dto : rows) {
//            try {
//                dsl.insertInto(REEA_T_REGISTRO_PDL_AMIANTO)
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE, dto.getCodiceFiscale())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.PERIODO,
//                                dto.getPeriodo() != null ? dto.getPeriodo() : java.time.LocalDate.now().getYear())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE,    dto.getIdCantiere())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA,   dto.getAziendaPiva())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME,   dto.getAziendaNome())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE,   dto.getAslCantiere())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE,dto.getComuneCantiere())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.ANNO,           dto.getAnno())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO,dto.getTipologiaPiano())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE, dto.getQuantitaDaRimuovere())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA,      dto.getQuantitaRimossa())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.FILE_ID, fileId)
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_INIZIO,
//                                dto.getValiditaInizio() != null
//                                        ? dto.getValiditaInizio().toLocalDateTime()
//                                        : java.time.LocalDateTime.now())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE,
//                                dto.getValiditaFine() != null
//                                        ? dto.getValiditaFine().toLocalDateTime()
//                                        : null)
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.DATA_CREAZIONE,  DSL.currentLocalDateTime())
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.UTENTE_CREAZIONE, utenteCreazione)
//                        .set(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_INIZIO, DSL.currentLocalDateTime())
//
//                        .execute();
////            } catch (Exception e) {
//////                throw new RuntimeException("Errore inserendo CF=" + dto.getCodiceFiscale(), e);
////            	System.out.println("Errore inserendo CF=" + dto.getCodiceFiscale() + " " + e.getMessage());
////            }
////        }
////    }
//            } catch (Exception e) {
//	        	String descrizioneErrore = ErroreImportUtility.buildErroreDescrizione(e);
//
//	            System.out.println("Errore inserendo CF=" + dto.getCodiceFiscale() + " " + descrizioneErrore);
//
//	            try {
//	            	tracciaElaborazioneService.inserisciScarto(
//                    		fileId,
//                            "ERR_ELABORAZIONE",
//                            descrizioneErrore,
//                            null,
//                            utenteCreazione,
//                            dto.getCognome(),
//                            dto.getNome(),
//                            dto.getDataNascita(),
//                            dto.getSesso(),
//                            dto.getCodiceFiscale(),
//	                        "REEA_T_REGISTRO_PDL_AMIANTO",
//	                        dto.getFileId() != null ? dto.getFileId() : 0
//	                );
//	            } catch (Exception ex) {
////	                ex.printStackTrace();
//	            	LOGGER.error("Errore inserendo CF: ", dto.getCodiceFiscale(), ex);
//	            }
//	        }
//	    }
//	}

    
//    @Override
//    public List<NplaDTO> listaRecordNpla(Integer fileId) {
//        return dsl
//                .select(
//                        REEA_T_REGISTRO_PDL_AMIANTO.REG_PDL_AMIANTO_ID,
//                        REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE,
//                        REEA_T_REGISTRO_PDL_AMIANTO.PERIODO,
//                        REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE,
//                        REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA,
//                        REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME,
//                        REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE,
//                        REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE,
//                        REEA_T_REGISTRO_PDL_AMIANTO.ANNO,
//                        REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO,
//                        REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE,
//                        REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA,
//                        REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_INIZIO,
//                        REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE,
//                        REEA_T_REGISTRO_PDL_AMIANTO.FILE_ID,
//                        REEA_T_REGISTRO_PDL_AMIANTO.UTENTE_CREAZIONE
//                )
//                .from(REEA_T_REGISTRO_PDL_AMIANTO)
//                .where(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.isNull())
//                .and(fileId != null ? REEA_T_REGISTRO_PDL_AMIANTO.FILE_ID.eq(fileId) : DSL.noCondition())
//                .stream()
//                .map(r -> {
//                    NplaDTO dto = new NplaDTO();
//                    dto.setRegPdlAmiantoId(r.get(REEA_T_REGISTRO_PDL_AMIANTO.REG_PDL_AMIANTO_ID));
//                    dto.setCodiceFiscale(r.get(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE));
//                    dto.setPeriodo(r.get(REEA_T_REGISTRO_PDL_AMIANTO.PERIODO));
//                    dto.setIdCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE));
//                    dto.setAziendaPiva(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA));
//                    dto.setAziendaNome(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME));
//                    dto.setAslCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE));
//                    dto.setComuneCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE));
//                    dto.setAnno(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ANNO));
//                    dto.setTipologiaPiano(r.get(REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO));
//                    dto.setQuantitaDaRimuovere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE));
//                    dto.setQuantitaRimossa(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA));
//                    dto.setFileId(r.get(REEA_T_REGISTRO_PDL_AMIANTO.FILE_ID));
//                    dto.setUtenteCreazione(r.get(REEA_T_REGISTRO_PDL_AMIANTO.UTENTE_CREAZIONE));
//                    LocalDateTime vi = r.get(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_INIZIO);
//                    dto.setValiditaInizio(vi != null ? vi.atOffset(java.time.ZoneOffset.UTC) : null);
//
//                    LocalDateTime vf = r.get(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE);
//                    dto.setValiditaFine(vf != null ? vf.atOffset(java.time.ZoneOffset.UTC) : null);
//
//
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }
    
    @Override
    public List<NplaDTO> listaRecordNpla(Integer fileId) {

        var query = dsl
                .select(
                        REEA_T_REGISTRO_PDL_AMIANTO.REG_PDL_AMIANTO_ID,
                        REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE,
                        REEA_T_REGISTRO_PDL_AMIANTO.PERIODO,
                        REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE,
                        REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA,
                        REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME,
                        REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE,
                        REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE,
                        REEA_T_REGISTRO_PDL_AMIANTO.ANNO,
                        REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO,
                        REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE,
                        REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA,
                        REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_INIZIO,
                        REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE,
                        REEA_T_REGISTRO_PDL_AMIANTO.FILE_ID,
                        REEA_T_REGISTRO_PDL_AMIANTO.UTENTE_CREAZIONE
                )
                .from(REEA_T_REGISTRO_PDL_AMIANTO)
                .where(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.isNull())
                .and(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE.isNull()); // MM
                //MM Non serve piu
                //.and(fileId != null ? REEA_T_REGISTRO_PDL_AMIANTO.FILE_ID.eq(fileId) : DSL.noCondition());

        List<NplaDTO> result = new ArrayList<>();

        for (var r : query.fetch()) {
            try {
                NplaDTO dto = new NplaDTO();

                dto.setRegPdlAmiantoId(r.get(REEA_T_REGISTRO_PDL_AMIANTO.REG_PDL_AMIANTO_ID));
                dto.setCodiceFiscale(r.get(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE));
                dto.setPeriodo(r.get(REEA_T_REGISTRO_PDL_AMIANTO.PERIODO));
                dto.setIdCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE));
                dto.setAziendaPiva(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_PIVA));
                dto.setAziendaNome(r.get(REEA_T_REGISTRO_PDL_AMIANTO.AZIENDA_NOME));
                dto.setAslCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ASL_CANTIERE));
                dto.setComuneCantiere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.COMUNE_CANTIERE));
                dto.setAnno(r.get(REEA_T_REGISTRO_PDL_AMIANTO.ANNO));
                dto.setTipologiaPiano(r.get(REEA_T_REGISTRO_PDL_AMIANTO.TIPOLOGIA_PIANO));
                dto.setQuantitaDaRimuovere(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_DA_RIMUOVERE));
                dto.setQuantitaRimossa(r.get(REEA_T_REGISTRO_PDL_AMIANTO.QUANTITA_RIMOSSA));
                dto.setFileId(r.get(REEA_T_REGISTRO_PDL_AMIANTO.FILE_ID));
                dto.setUtenteCreazione(r.get(REEA_T_REGISTRO_PDL_AMIANTO.UTENTE_CREAZIONE));

                LocalDateTime validitaInizio = r.get(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_INIZIO);
                dto.setValiditaInizio(validitaInizio != null
                        ? validitaInizio.atOffset(java.time.ZoneOffset.UTC)
                        : null);

                LocalDateTime validitaFine = r.get(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE);
                dto.setValiditaFine(validitaFine != null
                        ? validitaFine.atOffset(java.time.ZoneOffset.UTC)
                        : null);

                result.add(dto);

            } catch (Exception e) {
                System.out.println("Errore nel mapping del record NPLA con REG_PDL_AMIANTO_ID={}"+
                        r.get(REEA_T_REGISTRO_PDL_AMIANTO.REG_PDL_AMIANTO_ID) + e);
            }
        }

        return result;
    }
    

    @Override
    public Set<String> getListaRecordApertiNpla() {
        return dsl
                .select(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE,
                        REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE)
                .from(REEA_T_REGISTRO_PDL_AMIANTO)
                .where(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE.isNotNull())
                .and(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE.isNotNull())
                .and(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE.isNull())
                .and(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.isNull())               
                .stream()
                .map(r -> r.get(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE)
                        + "_" + r.get(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE))
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public void updateRegistroId(Integer regPdlAmiantoId, Integer registroId) {
        dsl.update(REEA_T_REGISTRO_PDL_AMIANTO)
                .set(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID, registroId)
                // DE AGGIUNGO LA DATA FINE IN MODO DA NON ELEBORARE PI� LA RIGA UNA SECONDA VOLTA SE SCARTATA O MENO
                .set(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE, currentLocalDateTime())// timestamp corrente del DB
                .where(REEA_T_REGISTRO_PDL_AMIANTO.REG_PDL_AMIANTO_ID.eq(regPdlAmiantoId))
                .execute();
    }

    @Override
    public void deleteById(Integer regPdlAmiantoId) {
        dsl.deleteFrom(REEA_T_REGISTRO_PDL_AMIANTO)
                .where(REEA_T_REGISTRO_PDL_AMIANTO.REG_PDL_AMIANTO_ID.eq(regPdlAmiantoId))
                .execute();
    }
    
    @Override
	public void chiudiTuttiRecordTabRegistroPdlAmiantoByFile(Integer fileId) {
    		dsl.update(REEA_T_REGISTRO_PDL_AMIANTO)
    				.set(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE, DSL.currentLocalDateTime())
    				.where(REEA_T_REGISTRO_PDL_AMIANTO.FILE_ID.eq(fileId))
    				.execute();	
	}

	@Override
	public Set<String> getListaRecordRegistroIdIsNotNullNpla() {
		return dsl
                .select(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE,
                        REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE)
                .from(REEA_T_REGISTRO_PDL_AMIANTO)
                .where(REEA_T_REGISTRO_PDL_AMIANTO.REGISTRO_ID.isNotNull())
                .and(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE.isNotNull())
                .and(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE.isNotNull())
                .and(REEA_T_REGISTRO_PDL_AMIANTO.VALIDITA_FINE.isNotNull())
                .stream()
                .map(r -> r.get(REEA_T_REGISTRO_PDL_AMIANTO.CODICE_FISCALE)
                        + "_" + r.get(REEA_T_REGISTRO_PDL_AMIANTO.ID_CANTIERE))
                .collect(Collectors.toCollection(HashSet::new));
	}

}
