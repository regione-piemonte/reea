package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaDAsl.REEA_D_ASL;
import static it.csi.registry.jooq.tables.ReeaDSoggettoStato.REEA_D_SOGGETTO_STATO;
import static it.csi.registry.jooq.tables.ReeaTRegistro.REEA_T_REGISTRO;
import static it.csi.registry.jooq.tables.ReeaTRegistroPdlAmianto.REEA_T_REGISTRO_PDL_AMIANTO;
import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalAnamnesi.REEA_T_REGISTRO_SPRESAL_ANAMNESI;
import static it.csi.registry.jooq.tables.ReeaTRegistroSpresalEsiti.REEA_T_REGISTRO_SPRESAL_ESITI;
import static it.csi.registry.jooq.tables.ReeaTSoggetto.REEA_T_SOGGETTO;
import static it.csi.registry.jooq.tables.ReeaDRegistroSpresalAnamnesiRagionesociale.REEA_D_REGISTRO_SPRESAL_ANAMNESI_RAGIONESOCIALE;
import static it.csi.registry.jooq.tables.ReeaTAdesione.REEA_T_ADESIONE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.csi.registry.model.*;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;
import it.csi.registry.services.TracciaElaborazioneService;
import it.csi.registry.util.DateConversionUtils;
import it.csi.registry.util.ErroreImportUtility;
import it.csi.registry.util.RegistroUtils;
import it.csi.registry.util.SpresalUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jooq.impl.DSL.currentLocalDateTime;

@Repository
public class SpresalRepositoryImpl implements SpresalRepository{

	private final DSLContext dsl;
	private final TracciaElaborazioneRepository tracciaElaborazioneRepository;
	private final TracciaElaborazioneService tracciaElaborazioneService;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SpresalRepositoryImpl.class);


	public SpresalRepositoryImpl(DSLContext dsl, TracciaElaborazioneRepository tracciaElaborazioneRepository, TracciaElaborazioneService tracciaElaborazioneService) {
		this.dsl = dsl;
		this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
		this.tracciaElaborazioneService = tracciaElaborazioneService;
	}

	//	    @Override
	//	    public void deleteRecordNonProcessati() {
	//	        dsl.deleteFrom(REEA_T_ADESIONE)
	//	                .where(REEA_T_ADESIONE.SOGGETTO_ID.isNull())
	//	                .execute();
	//	    }

	@Override
	public void deleteRecordNonProcessati() {
		dsl.deleteFrom(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
		.where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.isNull())
		.execute();
	}

	@Override
	public Set<String> getListaRecordApertiSpresalAnamnesi() {
	    return dsl
	            .select(
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM
	            )
	            .from(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
	            .where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE.isNull())
	            .stream()
	            .map(r -> r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE)
	                    + "_" + r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM))
	            .collect(Collectors.toCollection(HashSet::new));
	}
	
	
	@Override
	public Set<String> getListaRecordRegistroIdIsNotNullSpresalAnamnesi() {
	    return dsl
	            .select(
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM
	            )
	            .from(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
	            .where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.isNotNull())            
	            .stream()
	            .map(r -> r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE)
	                    + "_" + r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM))
	            .collect(Collectors.toCollection(HashSet::new));
	}
	
	
	@Override
	public Set<String> getListaRecordApertiSpresalEsiti() {
	    return dsl
	            .select(
	                    REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA
	            )
	            .from(REEA_T_REGISTRO_SPRESAL_ESITI)
	            .where(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE.isNull())
	            .stream()
	            .map(r -> r.get(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE)
	                    + "_" + r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA))
	            .collect(Collectors.toCollection(HashSet::new));
	}
	
	
	@Override
	public Set<String> getListaRecordRegistroIdIsNotNullSpresalEsiti() {
	    return dsl
	            .select(
	                    REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA
	            )
	            .from(REEA_T_REGISTRO_SPRESAL_ESITI)
	            .where(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE.isNotNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.isNotNull())
	            .stream()
	            .map(r -> r.get(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE)
	                    + "_" + r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA))
	            .collect(Collectors.toCollection(HashSet::new));
	}

	
	@Override
	public List<SpresalDTO> listaRecordSpresalAnamnesi(Integer fileId) {

	    var query = dsl
	            .select(
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_INIZIO,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CREAZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_MODIFICA,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CANCELLAZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CREAZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CANCELLAZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO,
	                    REEA_T_REGISTRO_SPRESAL_ANAMNESI.FILE_ID,
	                    DSL.when(
	                            REEA_T_SOGGETTO.COGNOME_CIFRATO.isNotNull(),
	                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
	                                    REEA_T_SOGGETTO.COGNOME_CIFRATO,
	                                    DSL.val("16<odcc8!", SQLDataType.VARCHAR))
	                    ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("cognome_soggetto"),
	                    DSL.when(
	                            REEA_T_SOGGETTO.NOME_CIFRATO.isNotNull(),
	                            DSL.function("pgp_sym_decrypt", SQLDataType.VARCHAR,
	                                    REEA_T_SOGGETTO.NOME_CIFRATO,
	                                    DSL.val("16<odcc8!", SQLDataType.VARCHAR))
	                    ).otherwise(DSL.val((String) null, SQLDataType.VARCHAR)).as("nome_soggetto"),
	                    REEA_T_SOGGETTO.SESSO,
	                    REEA_T_SOGGETTO.NASCITA_DATA
	            )
	            .from(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
	            .leftJoin(REEA_T_SOGGETTO)
	            .on(REEA_T_SOGGETTO.CODICE_FISCALE.eq(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE))
	            .where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.isNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE.isNull()) // MM
	            //.and(fileId != null ? REEA_T_REGISTRO_SPRESAL_ANAMNESI.FILE_ID.eq(fileId) : DSL.noCondition())
	            .orderBy(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CREAZIONE.asc());

	    List<SpresalDTO> result = new ArrayList<>();

	    for (var r : query.fetch()) {
	        try {
	            SpresalDTO row = new SpresalDTO();

	            Integer id = r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID);
	            row.setRegSpresalAnamnesiId(id != null ? id.longValue() : null);

	            row.setCodiceFiscale(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE));
	            row.setRegistroId(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID));
	            row.setIdAura(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA));
	            row.setDataIntervista(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA));
	            row.setNominativoIntervistatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE));
	            row.setFumatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE));
	            row.setSigarette(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE));
	            row.setSigaretteAnni(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI));
	            row.setSigaretteEtaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO));
	            row.setSigaretteFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE));
	            row.setSigaretteEtaFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE));
	            row.setSigaretteDie(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE));
	            row.setSigari(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI));
	            row.setSigariAnni(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI));
	            row.setSigariEtaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO));
	            row.setSigariFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE));
	            row.setSigariEtaFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE));
	            row.setSigariDie(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE));
	            row.setPipa(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA));
	            row.setPipaAnni(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI));
	            row.setPipaEtaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO));
	            row.setPipaFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE));
	            row.setPipaEtaFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE));
	            row.setPipaDie(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE));
	            row.setOccupazioneNum(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM));
	            row.setOccupazioneAnnoInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO));
	            row.setOccupazioneAnnoFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE));
	            row.setOccupazioneTipo(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO));
	            row.setOccupazioneDescrizioneLavoro(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO));
	            row.setOccupazioneNomeEIndirizzoDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA));
	            row.setOccupazioneAttivitaDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA));
	            row.setNotaAttivitaConAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO));
	            row.setAnamnesiEsposizioneAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO));
	            row.setEsposizioneProfessionale(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE));
	            row.setAnnoFineEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE));
	            row.setLivelloEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE));
	            row.setInserimentoInSorveglianza(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA));
	            row.setIdSpresal(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL));

	            LocalDateTime vi = r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_INIZIO);
	            row.setValiditaInizio(vi != null ? DateConversionUtils.toOffsetDateTime(vi) : null);

	            LocalDateTime vf = r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE);
	            row.setValiditaFine(vf != null ? DateConversionUtils.toOffsetDateTime(vf) : null);

	            LocalDateTime dc = r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CREAZIONE);
	            row.setDataCreazione(dc != null ? DateConversionUtils.toOffsetDateTime(dc) : null);

	            LocalDateTime dm = r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_MODIFICA);
	            row.setDataModifica(dm != null ? DateConversionUtils.toOffsetDateTime(dm) : null);

	            LocalDateTime dca = r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CANCELLAZIONE);
	            row.setDataCancellazione(dca != null ? DateConversionUtils.toOffsetDateTime(dca) : null);

	            row.setUtenteCreazione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CREAZIONE));
	            row.setUtenteModifica(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA));
	            row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CANCELLAZIONE));

	            row.setOccupazioneEsposizioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT));
	            row.setOccupazioneSettoreDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT));
	            row.setOccupazioneMansioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT));
	            row.setOccupazioneRagioneSocialeDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT));
	            row.setOccupazionePivaDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT));
	            row.setFileId(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FILE_ID));
	            row.setCognome(r.get("cognome_soggetto", String.class));
	            row.setNome(r.get("nome_soggetto", String.class));
	            row.setSesso(r.get(REEA_T_SOGGETTO.SESSO));
	            row.setDataNascita(r.get(REEA_T_SOGGETTO.NASCITA_DATA));

	            result.add(row);

	        } catch (Exception e) {
	            System.out.println("Errore mapping SPRESAL_ANAMNESI. REG_SPRESAL_ANAMNESI_ID="
	                    + r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID)
	                    + ", CODICE_FISCALE="
	                    + r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE)
	                    + ", errore="
	                    + e.getMessage());
//	            e.printStackTrace();
	            LOGGER.error("Errore mapping SPRESAL_ANAMNESI. REG_SPRESAL_ANAMNESI_ID= ", r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID), e);
	        }
	    }

	    return result;
	}
	
	
	private Integer parseIntegerSafe(String value) {
	    if (value == null || value.isBlank()) {
	        return null;
	    }
	    try {
	        return Integer.valueOf(value);
	    } catch (NumberFormatException e) {
	        return null;
	    }
	}
	
	
//	@Override
//	public List<SpresalEsitiDTO> listaRecordSpresalEsiti(Integer fileId) {
//
//	    var query = dsl
//	        .select(
//	            REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.VISITA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE,
//					REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA,
//
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL,
//
//	            REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE,
//	            REEA_T_REGISTRO_SPRESAL_ESITI.FILE_ID
//	        )
//	        .from(REEA_T_REGISTRO_SPRESAL_ESITI)
//	        .where(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.isNull())
//				.and(fileId != null ? REEA_T_REGISTRO_SPRESAL_ESITI.FILE_ID.eq(fileId) : DSL.noCondition())
//	        .orderBy(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE.asc())
////	        .where(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE.in("RGZXXX99A17Z129D","PNSXXX99S04F351O","FGLXXX99M10H150O","GRDXXX99A31I470N","LLSXXX99L25I470O","BRNXXX99A08L219W","ZLNXXX99A02L219D","FRRXXX99P23I470U","CLNXXX99P02G308I"));
//	        ;
//
//	    List<SpresalEsitiDTO> result = query.stream().map(r -> {
//	        SpresalEsitiDTO row = new SpresalEsitiDTO();
//
//	        row.setRegSpresalEsitiId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID).longValue());
//	        row.setRegistroId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID));
//	        row.setCodiceFiscale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE));
//	        row.setIdAura(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA));
//	        row.setDataVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA));
//	        row.setVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VISITA));
//	        row.setLivelloVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA));
//	        row.setRiceveIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO));
//	        row.setMalattiaIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO));
//
//	        row.setAccertamentiRx(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX));
//	        row.setAccertamentiRxData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA));
//	        row.setAccertamentiRxRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE));
//
//	        row.setAccertamentiTc(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC));
//	        row.setAccertamentiTcData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA));
//	        row.setAccertamentiTcRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE));
//
//	        row.setAccertamentiSpirometriaSemplice(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE));
//	        row.setAccertamentiSpirometriaSempliceData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA));
//	        row.setAccertamentiSpirometriaSempliceRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM));
//
//	        row.setAccertamentiSpirometriaGlobale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE));
//	        row.setAccertamentiSpirometriaGlobaleData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA));
//	        row.setAccertamentiSpirometriaGlobaleRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM));
//
//	        row.setAccertamentiDlco(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO));
//	        row.setAccertamentiDlcoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA));
//	        row.setAccertamentiDlcoRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE));
//
//	        row.setAccertamentiPet(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET));
//	        row.setAccertamentiPetData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA));
//	        row.setAccertamentiPetRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE));
//
//	        row.setAccertamentiVisitaPneumologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA));
//	        row.setAccertamentiVisitaPneumologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA));
//	        row.setAccertamentiVisitaPneumologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO));
//
//	        row.setAccertamentiVisitaRadiologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA));
//	        row.setAccertamentiVisitaRadiologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA));
//	        row.setAccertamentiVisitaRadiologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO));
//
//	        row.setAccertamentiVisitaOncologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA));
//	        row.setAccertamentiVisitaOncologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA));
//	        row.setAccertamentiVisitaOncologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO));
//
//	        row.setAccertamentiAltro(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO));
//	        row.setAccertamentiAltroDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE));
//	        row.setAccertamentiAltroData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA));
//	        row.setAccertamentiAltroRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE));
//
//	        row.setRisultatoNegativo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO));
//
//	        row.setPpmPlacchePleuricheMonolaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI));
//	        row.setPpmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setPpmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setPpmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setPpmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setPpmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setPpmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO));
//	        row.setPpmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA));
//
//	        row.setPpbPlacchePleuricheBilaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI));
//	        row.setPpbPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setPpbPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setPpbAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setPpbAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setPpbPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setPpbReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO));
//	        row.setPpbRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA));
//
//	        row.setApAsbestosiPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE));
//	        row.setApPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setApPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setApAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setApAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setApPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setApReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO));
//	        row.setApRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA));
//
//	        row.setFpdFibrosiPleuricaDiffusa(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA));
//	        row.setFpdPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setFpdPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setFpdAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setFpdAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setFpdPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setFpdReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO));
//	        row.setFpdRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA));
//
//	        row.setMpMesoteliomaPleurico(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO));
//	        row.setMpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setMpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setMpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setMpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setMpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setMpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO));
//	        row.setMpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA));
//	        row.setMpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR));
//	        row.setMpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA));
//
//	        row.setAmAltroMesotelioma(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA));
//	        row.setAmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setAmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setAmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setAmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setAmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setAmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO));
//	        row.setAmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA));
//	        row.setAmComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR));
//	        row.setAmComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA));
//
//	        row.setNlNeoplasiaLaringe(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE));
//	        row.setNlPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setNlPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setNlAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setNlAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setNlPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setNlReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO));
//	        row.setNlRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA));
//
//	        row.setNoNeoplasiaOvarica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA));
//	        row.setNoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setNoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setNoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setNoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setNoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setNoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO));
//	        row.setNoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA));
//
//	        row.setTpTumoreDelPolmone(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE));
//	        row.setTpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setTpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setTpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setTpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setTpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setTpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO));
//	        row.setTpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA));
//	        row.setTpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR));
//	        row.setTpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA));
//
//	        row.setBpcoEnfisemaPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE));
//	        row.setBpcoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setBpcoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setBpcoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setBpcoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setBpcoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setBpcoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO));
//	        row.setBpcoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA));
//
//
//			row.setAccertamentiRxAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA));
//			row.setAccertamentiTcAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA));
//			row.setAccertamentiSpirometriaSempliceAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA));
//			row.setAccertamentiSpirometriaGlobaleAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA));
//			row.setAccertamentiDlcoAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA));
//			row.setAccertamentiPetAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA));
//			row.setAccertamentiVisitaPneumologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA));
//			row.setAccertamentiVisitaRadiologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA));
//			row.setAccertamentiVisitaOncologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA));
//			row.setAccertamentiAltroRefertoAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA));
//
//
//			row.setAltraDiagnosi(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI));
//	        row.setAltraDiagnosiDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE));
//	        row.setAltraPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA));
//	        row.setAltraPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
//	        row.setAltraAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA));
//	        row.setAltraAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA));
//	        row.setAltraPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO));
//	        row.setAltraReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO));
//	        row.setAltraRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA));
//
//	        row.setFollowUpPrevisto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO));
//	        row.setAnnoPresuntoProssimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA));
//	        row.setAnnoUltimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA));
//	        row.setInvioSintesiAMmg(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG));
//	        row.setIdSpresal(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL));
//
//	        row.setValiditaInizio(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO)));
//	        row.setValiditaFine(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE)));
//	        row.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE)));
//	        row.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA)));
//	        row.setDataCancellazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE)));
//	        row.setUtenteCreazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE));
//	        row.setUtenteModifica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA));
//	        row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE));
//	        row.setFileId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FILE_ID));
//
//
//	        return row;
//	    }).collect(Collectors.toList());
//
//	    return result;
//	}
	
	@Override
	public List<SpresalEsitiDTO> listaRecordSpresalEsiti(Integer fileId) {

	    var query = dsl
	            .select(
	                    REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.VISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE,
	                    REEA_T_REGISTRO_SPRESAL_ESITI.FILE_ID
	            )
	            .from(REEA_T_REGISTRO_SPRESAL_ESITI)
	            .where(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.isNull())
	            .and(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE.isNull()) // MM
	            //.and(fileId != null ? REEA_T_REGISTRO_SPRESAL_ESITI.FILE_ID.eq(fileId) : DSL.noCondition())
	            .orderBy(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE.asc());

	    List<SpresalEsitiDTO> result = new ArrayList<>();

	    for (var r : query.fetch()) {
	        try {
	            SpresalEsitiDTO row = new SpresalEsitiDTO();

	            Integer id = r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID);
	            row.setRegSpresalEsitiId(id != null ? id.longValue() : null);

	            row.setRegistroId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID));
	            row.setCodiceFiscale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE));
	            row.setIdAura(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA));
	            row.setDataVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA));
	            row.setVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VISITA));
	            row.setLivelloVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA));
	            row.setRiceveIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO));
	            row.setMalattiaIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO));

	            row.setAccertamentiRx(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX));
	            row.setAccertamentiRxData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA));
	            row.setAccertamentiRxRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE));
	            row.setAccertamentiRxAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA));

	            row.setAccertamentiTc(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC));
	            row.setAccertamentiTcData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA));
	            row.setAccertamentiTcRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE));
	            row.setAccertamentiTcAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA));

	            row.setAccertamentiSpirometriaSemplice(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE));
	            row.setAccertamentiSpirometriaSempliceData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA));
	            row.setAccertamentiSpirometriaSempliceRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM));
	            row.setAccertamentiSpirometriaSempliceAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA));

	            row.setAccertamentiSpirometriaGlobale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE));
	            row.setAccertamentiSpirometriaGlobaleData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA));
	            row.setAccertamentiSpirometriaGlobaleRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM));
	            row.setAccertamentiSpirometriaGlobaleAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA));

	            row.setAccertamentiDlco(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO));
	            row.setAccertamentiDlcoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA));
	            row.setAccertamentiDlcoRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE));
	            row.setAccertamentiDlcoAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA));

	            row.setAccertamentiPet(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET));
	            row.setAccertamentiPetData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA));
	            row.setAccertamentiPetRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE));
	            row.setAccertamentiPetAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA));

	            row.setAccertamentiVisitaPneumologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA));
	            row.setAccertamentiVisitaPneumologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA));
	            row.setAccertamentiVisitaPneumologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO));
	            row.setAccertamentiVisitaPneumologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA));

	            row.setAccertamentiVisitaRadiologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA));
	            row.setAccertamentiVisitaRadiologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA));
	            row.setAccertamentiVisitaRadiologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO));
	            row.setAccertamentiVisitaRadiologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA));

	            row.setAccertamentiVisitaOncologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA));
	            row.setAccertamentiVisitaOncologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA));
	            row.setAccertamentiVisitaOncologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO));
	            row.setAccertamentiVisitaOncologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA));

	            row.setAccertamentiAltro(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO));
	            row.setAccertamentiAltroDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE));
	            row.setAccertamentiAltroData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA));
	            row.setAccertamentiAltroRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE));
	            row.setAccertamentiAltroRefertoAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA));

	            row.setRisultatoNegativo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO));

	            row.setPpmPlacchePleuricheMonolaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI));
	            row.setPpmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setPpmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setPpmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA));
	            row.setPpmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setPpmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setPpmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO));
	            row.setPpmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA));

	            row.setPpbPlacchePleuricheBilaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI));
	            row.setPpbPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setPpbPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setPpbAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA));
	            row.setPpbAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setPpbPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setPpbReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO));
	            row.setPpbRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA));

	            row.setApAsbestosiPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE));
	            row.setApPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setApPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setApAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA));
	            row.setApAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setApPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setApReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO));
	            row.setApRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA));

	            row.setFpdFibrosiPleuricaDiffusa(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA));
	            row.setFpdPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setFpdPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setFpdAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA));
	            row.setFpdAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setFpdPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setFpdReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO));
	            row.setFpdRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA));

	            row.setMpMesoteliomaPleurico(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO));
	            row.setMpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setMpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setMpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA));
	            row.setMpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setMpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setMpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO));
	            row.setMpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA));
	            row.setMpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR));
	            row.setMpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA));

	            row.setAmAltroMesotelioma(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA));
	            row.setAmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setAmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setAmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA));
	            row.setAmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setAmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setAmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO));
	            row.setAmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA));
	            row.setAmComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR));
	            row.setAmComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA));

	            row.setNlNeoplasiaLaringe(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE));
	            row.setNlPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setNlPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setNlAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA));
	            row.setNlAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setNlPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setNlReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO));
	            row.setNlRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA));

	            row.setNoNeoplasiaOvarica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA));
	            row.setNoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setNoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setNoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA));
	            row.setNoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setNoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setNoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO));
	            row.setNoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA));

	            row.setTpTumoreDelPolmone(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE));
	            row.setTpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setTpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setTpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA));
	            row.setTpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setTpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setTpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO));
	            row.setTpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA));
	            row.setTpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR));
	            row.setTpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA));

	            row.setBpcoEnfisemaPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE));
	            row.setBpcoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setBpcoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setBpcoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA));
	            row.setBpcoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setBpcoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setBpcoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO));
	            row.setBpcoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA));

	            row.setAltraDiagnosi(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI));
	            row.setAltraDiagnosiDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE));
	            row.setAltraPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA));
	            row.setAltraPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	            row.setAltraAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA));
	            row.setAltraAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA));
	            row.setAltraPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO));
	            row.setAltraReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO));
	            row.setAltraRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA));

	            row.setFollowUpPrevisto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO));
	            row.setAnnoPresuntoProssimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA));
	            row.setAnnoUltimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA));
	            row.setInvioSintesiAMmg(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG));
	            row.setIdSpresal(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL));

	            row.setValiditaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO) != null
	                    ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO))
	                    : null);
	            row.setValiditaFine(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE) != null
	                    ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE))
	                    : null);
	            row.setDataCreazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE) != null
	                    ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE))
	                    : null);
	            row.setDataModifica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA) != null
	                    ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA))
	                    : null);
	            row.setDataCancellazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE) != null
	                    ? DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE))
	                    : null);

	            row.setUtenteCreazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE));
	            row.setUtenteModifica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA));
	            row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE));
	            row.setFileId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FILE_ID));

	            result.add(row);
	        } catch (Exception e) {
	            System.out.println("Errore nel mapping del record SPRESAL_ESITI con REG_SPRESAL_ESITI_ID={}" +
	                    r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID) + e);
	        }
	    }

	    return result;
	}


	@Override
	public void scaricoDatiExcelInTRegistroSpresalAnamnesi(List<SpresalDTO> rows, Integer fileId, String utenteCreazione) {
	    for (SpresalDTO dto : rows) {
	        try {
//	            LocalDateTime validitaInizio      = DateConversionUtils.toStartOfDay(dto.getValiditaInizio());
	            LocalDateTime validitaFine        = DateConversionUtils.toStartOfDay(dto.getValiditaFine());
//	            LocalDateTime dataCreazione       = DateConversionUtils.toStartOfDay(dto.getDataCreazione());
	            LocalDateTime dataModifica        = DateConversionUtils.toStartOfDay(dto.getDataModifica());
	            LocalDateTime dataCancellazione   = DateConversionUtils.toStartOfDay(dto.getDataCancellazione());

	            dsl.insertInto(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID, dto.getRegistroId())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA, dto.getIdAura())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA, dto.getDataIntervista())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE, dto.getNominativoIntervistatore())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE, dto.getCodiceFiscale())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE, dto.getFumatore())

	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE, dto.getSigarette())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI, dto.getSigaretteAnni())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO, dto.getSigaretteEtaInizio())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE, dto.getSigaretteFumaAttualmente())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE, dto.getSigaretteEtaFine())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE, dto.getSigaretteDie())

	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI, dto.getSigari())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI, dto.getSigariAnni())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO, dto.getSigariEtaInizio())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE, dto.getSigariFumaAttualmente())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE, dto.getSigariEtaFine())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE, dto.getSigariDie())

	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA, dto.getPipa())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI, dto.getPipaAnni())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO, dto.getPipaEtaInizio())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE, dto.getPipaFumaAttualmente())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE, dto.getPipaEtaFine())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE, dto.getPipaDie())

	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM, dto.getOccupazioneNum())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO, dto.getOccupazioneAnnoInizio())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE, dto.getOccupazioneAnnoFine())

	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO, dto.getOccupazioneTipo())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO, dto.getOccupazioneDescrizioneLavoro())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA, dto.getOccupazioneNomeEIndirizzoDitta())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA, dto.getOccupazioneAttivitaDitta())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO, dto.getNotaAttivitaConAmianto())

	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO, dto.getAnamnesiEsposizioneAmianto())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE, dto.getEsposizioneProfessionale())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE, dto.getAnnoFineEsposizione())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE, dto.getLivelloEsposizione())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA, dto.getInserimentoInSorveglianza())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL, dto.getIdSpresal())

	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_INIZIO, DSL.currentLocalDateTime())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE, validitaFine)
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CREAZIONE, DSL.currentLocalDateTime())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_MODIFICA, dataModifica)
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CANCELLAZIONE, dataCancellazione)
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CREAZIONE, utenteCreazione)
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA, dto.getUtenteModifica())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CANCELLAZIONE, dto.getUtenteCancellazione())

	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT, dto.getOccupazioneEsposizioneCrpt())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT, dto.getOccupazioneSettoreDittaCrpt())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT, dto.getOccupazioneMansioneCrpt())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT, dto.getOccupazioneRagioneSocialeDittaCrpt())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT, dto.getOccupazionePivaDittaCrpt())
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FILE_ID, fileId)
	               .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.COUNSELING, dto.getCounseling())
//				   .onConflictDoNothing()
//						.onConflict(
//								REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE,
//								REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM
//						)
//						.doNothing()
						.execute();

//	        } catch (Exception e) {
//	        	System.out.println("Errore inserendo CF=" + dto.getCodiceFiscale() + " " + e.getMessage());
//	        }
//	    }
//	}
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
	                        null,
	                        null,
	                        null,
	                        null,
	                        dto.getCodiceFiscale(),
	                        "REEA_T_REGISTRO_SPRESAL_ANAMNESI",
	                        dto.getFileId() != null ? dto.getFileId() : 0
	                );
	            } catch (Exception ex) {
//	                ex.printStackTrace();
	            	LOGGER.error("Errore inserendo CF=", dto.getCodiceFiscale(), ex);
	            }
	        }
	    }
	}


	@Override
	public void scaricoDatiExcelInTRegistroSpresalEsiti(List<SpresalEsitiDTO> rows, Integer fileId, String utenteCreazione) {
	    for (SpresalEsitiDTO dto : rows) {
	        try {
	            LocalDateTime validitaFine      = DateConversionUtils.toStartOfDay(dto.getValiditaFine());
	            LocalDateTime dataModifica      = DateConversionUtils.toStartOfDay(dto.getDataModifica());
	            LocalDateTime dataCancellazione = DateConversionUtils.toStartOfDay(dto.getDataCancellazione());

	            dsl.insertInto(REEA_T_REGISTRO_SPRESAL_ESITI)
	               // chiavi e dati base
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID, dto.getRegistroId())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE, dto.getCodiceFiscale())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA, dto.getIdAura())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA, dto.getDataVisita())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.VISITA, dto.getVisita())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA, dto.getLivelloVisita())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO, dto.getRiceveIndennizzo())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO, dto.getMalattiaIndennizzo())

	               // accertamenti RX / TC / spirometria / DLCO / PET / visite
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX, dto.getAccertamentiRx())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA, dto.getAccertamentiRxData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE, dto.getAccertamentiRxRefertoNormale())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA, dto.getAccertamentiRxAcquisita())

	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC, dto.getAccertamentiTc())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA, dto.getAccertamentiTcData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE, dto.getAccertamentiTcRefertoNormale())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA, dto.getAccertamentiTcAcquisita())

	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE, dto.getAccertamentiSpirometriaSemplice())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA, dto.getAccertamentiSpirometriaSempliceData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM, dto.getAccertamentiSpirometriaSempliceRefertoNorm())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA, dto.getAccertamentiSpirometriaSempliceAcquisita())

	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE, dto.getAccertamentiSpirometriaGlobale())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA, dto.getAccertamentiSpirometriaGlobaleData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM, dto.getAccertamentiSpirometriaGlobaleRefertoNorm())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA, dto.getAccertamentiSpirometriaGlobaleAcquisita())
	               
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO, dto.getAccertamentiDlco())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA, dto.getAccertamentiDlcoData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE, dto.getAccertamentiDlcoRefertoNormale())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA, dto.getAccertamentiDlcoAcquisita())

	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET, dto.getAccertamentiPet())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA, dto.getAccertamentiPetData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE, dto.getAccertamentiPetRefertoNormale())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA, dto.getAccertamentiPetAcquisita())

	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA, dto.getAccertamentiVisitaPneumologica())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA, dto.getAccertamentiVisitaPneumologicaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO, dto.getAccertamentiVisitaPneumologicaReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA, dto.getAccertamentiVisitaPneumologicaAcquisita())

	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA, dto.getAccertamentiVisitaRadiologica())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA, dto.getAccertamentiVisitaRadiologicaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO, dto.getAccertamentiVisitaRadiologicaReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA, dto.getAccertamentiVisitaRadiologicaAcquisita())

	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA, dto.getAccertamentiVisitaOncologica())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA, dto.getAccertamentiVisitaOncologicaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO, dto.getAccertamentiVisitaOncologicaReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA, dto.getAccertamentiVisitaOncologicaAcquisita())

	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO, dto.getAccertamentiAltro())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE, dto.getAccertamentiAltroDescrizione())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA, dto.getAccertamentiAltroData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE, dto.getAccertamentiAltroRefertoNormale())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA, dto.getAccertamentiAltroRefertoAcquisita())

	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO, dto.getRisultatoNegativo())

	               // blocco PPM
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI, dto.getPpmPlacchePleuricheMonolaterali())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getPpmPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getPpmPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA, dto.getPpmAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getPpmAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO, dto.getPpmPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO, dto.getPpmReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA, dto.getPpmRefertoData())

	               // blocco PPB
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI, dto.getPpbPlacchePleuricheBilaterali())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getPpbPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getPpbPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA, dto.getPpbAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getPpbAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO, dto.getPpbPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO, dto.getPpbReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA, dto.getPpbRefertoData())

	               // blocco AP
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE, dto.getApAsbestosiPolmonare())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getApPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getApPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA, dto.getApAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getApAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO, dto.getApPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO, dto.getApReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA, dto.getApRefertoData())

	               // blocco FPD
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA, dto.getFpdFibrosiPleuricaDiffusa())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getFpdPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getFpdPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA, dto.getFpdAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getFpdAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO, dto.getFpdPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO, dto.getFpdReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA, dto.getFpdRefertoData())

	               // blocco MP
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO, dto.getMpMesoteliomaPleurico())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getMpPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getMpPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA, dto.getMpAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getMpAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO, dto.getMpPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO, dto.getMpReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA, dto.getMpRefertoData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR, dto.getMpComunicazioneAlCor())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA, dto.getMpComunicazioneAlCorData())

	               // blocco AM
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA, dto.getAmAltroMesotelioma())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getAmPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getAmPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA, dto.getAmAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getAmAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO, dto.getAmPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO, dto.getAmReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA, dto.getAmRefertoData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR, dto.getAmComunicazioneAlCor())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA, dto.getAmComunicazioneAlCorData())

	               // blocco NL
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE, dto.getNlNeoplasiaLaringe())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getNlPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getNlPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA, dto.getNlAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getNlAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO, dto.getNlPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO, dto.getNlReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA, dto.getNlRefertoData())

	               // blocco NO
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA, dto.getNoNeoplasiaOvarica())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getNoPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getNoPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA, dto.getNoAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getNoAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO, dto.getNoPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO, dto.getNoReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA, dto.getNoRefertoData())

	               // blocco TP
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE, dto.getTpTumoreDelPolmone())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getTpPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getTpPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA, dto.getTpAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getTpAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO, dto.getTpPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO, dto.getTpReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA, dto.getTpRefertoData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR, dto.getTpComunicazioneAlCor())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA, dto.getTpComunicazioneAlCorData())

	               // blocco BPCO
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE, dto.getBpcoEnfisemaPolmonare())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getBpcoPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getBpcoPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA, dto.getBpcoAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getBpcoAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO, dto.getBpcoPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO, dto.getBpcoReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA, dto.getBpcoRefertoData())

	               // blocco ALTRA diagnosi
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI, dto.getAltraDiagnosi())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE, dto.getAltraDiagnosiDescrizione())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA, dto.getAltraPrimoCertificatoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA, dto.getAltraPrimoCertificatoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA, dto.getAltraAggravamentoEDenuncia())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA, dto.getAltraAggravamentoEDenunciaData())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO, dto.getAltraPercentualeDiRiconoscimento())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO, dto.getAltraReferto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA, dto.getAltraRefertoData())

	               // follow-up
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO, dto.getFollowUpPrevisto())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA, dto.getAnnoPresuntoProssimaVisita())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA, dto.getAnnoUltimaVisita())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG, dto.getInvioSintesiAMmg())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL, dto.getIdSpresal())

	               // audit
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO, DSL.currentLocalDateTime())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE, validitaFine)
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE, DSL.currentLocalDateTime())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA, dataModifica)
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE, dataCancellazione)
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE, utenteCreazione)
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA, dto.getUtenteModifica())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE, dto.getUtenteCancellazione())
	               .set(REEA_T_REGISTRO_SPRESAL_ESITI.FILE_ID, fileId)
//	               .onConflictDoNothing()
//						.onConflict(
//								REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE,
//								REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA
//						)
//						.doNothing()
	               .execute();

//	        } catch (Exception e) {
//	            System.out.println("Errore inserendo CF=" + dto.getCodiceFiscale() + " " + e.getMessage());
//	        }
//	    }
//	}
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
	                        null,
	                        null,
	                        null,
	                        null,
	                        dto.getCodiceFiscale(),
	                        "REEA_T_REGISTRO_SPRESAL_ESITI",
	                        dto.getFileId() != null ? dto.getFileId() : 0
	                );
	            } catch (Exception ex) {
//	                ex.printStackTrace();
	            	LOGGER.error("Errore inserendo CF=", dto.getCodiceFiscale(), ex);
	            }
	        }
	    }
	}

	

	@Override
	public Integer aggiornaSpresalRegistroId(AnagraficaDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione) {

	    RegistroDTO registro = RegistroUtils.ensureRegistro(dto);
	    SpresalDTO spresal = SpresalUtils.ensureSpresal(dto);
	    Integer ris = 0;
	    
	    try {
	    	ris = dsl.update(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
	        .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID, registro.getRegistroId())
	        .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_MODIFICA, DSL.currentLocalDateTime())
	        .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA, dto.getUtenteCreazione())
            // DE AGGIUNGO LA DATA FINE IN MODO DA NON ELEBORARE PI� LA RIGA UNA SECONDA VOLTA SE SCARTATA O MENO
            .set(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE, currentLocalDateTime())// timestamp corrente del DB
	        .where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID
	               .eq(spresal.getRegSpresalAnamnesiId().intValue()))
	        .execute();
	    
	    tracciaElaborazioneRepository.inserisciFileImpatto(
	    		dsl,
                elaborazioneId,
                "REEA_T_REGISTRO_SPRESAL_ANAMNESI",
                dto.getSpresal().getRegSpresalAnamnesiId().intValue(),
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
    					"aggiornaSpresalRegistroId ha restituito " + ris + " per CF " + dto.getCodiceFiscale(),
    					null,
    					dto.getUtenteCreazione(),
    					dto.getCognome(),
    					dto.getNome(),
    					dto.getNascitaData(),
    					dto.getSesso(),
    					dto.getCodiceFiscale(),
    					"REEA_T_REGISTRO_SPRESAL_ANAMNESI",
    					dto.getSpresal().getRegSpresalAnamnesiId().intValue()
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
	public Integer aggiornaSpresalAnamnesiFlagElaborato(AnagraficaDTO dto) {
		
	    SpresalDTO spresal = SpresalUtils.ensureSpresal(dto);

	    Integer ris = dsl.update(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
	        .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ELABORATO, true)
	        .where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID
	               .eq(spresal.getRegSpresalAnamnesiId().intValue()))
	        .execute();
	    
	    return ris;
	}
	
	
	@Override
	public Integer aggiornaSpresalEsitiFlagElaborato(SpresalEsitiDTO dto) {

	    Integer ris = dsl.update(REEA_T_REGISTRO_SPRESAL_ESITI)
	        .set(REEA_T_REGISTRO_SPRESAL_ESITI.ELABORATO, true)
	        .where(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID
	               .eq(dto.getRegSpresalEsitiId().intValue()))
	        .execute();
	    
	    return ris;
	}

	
	@Override
	public Integer aggiornaSpresalEsitiRegistroId(AnagraficaDTO dto, Integer regSpresalEsitiId, Integer elaborazioneId, String utente, Integer tipoOperazione) {
		
	    RegistroDTO registro = RegistroUtils.ensureRegistro(dto);
	    Integer ris = 0;
	    try {
			ris = dsl.update(REEA_T_REGISTRO_SPRESAL_ESITI)
	        .set(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID, registro.getRegistroId())
	        .set(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA, DSL.currentLocalDateTime())
	        .set(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA, dto.getUtenteCreazione())
            // DE AGGIUNGO LA DATA FINE IN MODO DA NON ELEBORARE PI� LA RIGA UNA SECONDA VOLTA SE SCARTATA O MENO
            .set(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE, currentLocalDateTime())// timestamp corrente del DB
	        .where(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID.eq(regSpresalEsitiId))
	        .execute();
		
		if(elaborazioneId != null) {
	    	tracciaElaborazioneRepository.inserisciFileImpatto(
	    			dsl,
	                elaborazioneId,
	                "REEA_T_REGISTRO_SPRESAL_ESITI",
	                regSpresalEsitiId,
	                tipoOperazione,
	                utente
	        );
	    }
	    
	    return ris;
	    
	    } catch (Exception e) {

    		try {
    			tracciaElaborazioneService.inserisciErroreRiga(
//    					dsl,
    					elaborazioneId,
    					"ERR_INSERIMENTO",
    					"aggiornaSpresalEsitiRegistroId ha restituito " + ris + " per CF " + dto.getCodiceFiscale(),
    					null,
    					dto.getUtenteCreazione(),
    					dto.getCognome(),
    					dto.getNome(),
    					dto.getNascitaData(),
    					dto.getSesso(),
    					dto.getCodiceFiscale(),
    					"REEA_T_REGISTRO_SPRESAL_ANAMNESI",
    					regSpresalEsitiId
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
	public SpresalDTO recuperaOggettoTRegistroSpresalAnamnesiByCF(
	        String codiceFiscale,
	        boolean registroIdGiaAssegnato,
	        String occupazioneNum,
	        boolean elaborato) {

	    Condition condition = REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.eq(codiceFiscale);

	    if (registroIdGiaAssegnato) {
	        condition = condition.and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.isNotNull());
	    } else {
	        condition = condition.and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.isNull());
	    }

	    if (occupazioneNum != null) {
	        condition = condition.and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM.eq(occupazioneNum));
	    }
	    
	    condition = condition.and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ELABORATO.eq(elaborato));
	    var query = dsl
	        .select(
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE,

	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE,

	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE,

	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE,

	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO,

	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_INIZIO,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CREAZIONE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_MODIFICA,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CANCELLAZIONE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CREAZIONE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CANCELLAZIONE,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT,
	            REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO
	        )
	        .from(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
	        .where(condition);

	    SpresalDTO result = query.stream().map(r -> {
	        SpresalDTO row = new SpresalDTO();
	        row.setRegSpresalAnamnesiId(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID).longValue());
	        row.setRegistroId(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID));
	        row.setIdAura(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA));
	        row.setDataIntervista(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA));
	        row.setNominativoIntervistatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE));
	        row.setFumatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE));

	        row.setSigarette(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE));
	        row.setSigaretteAnni(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI));
	        row.setSigaretteEtaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO));
	        row.setSigaretteFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE));
	        row.setSigaretteEtaFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE));
	        row.setSigaretteDie(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE));

	        row.setSigari(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI));
	        row.setSigariAnni(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI));
	        row.setSigariEtaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO));
	        row.setSigariFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE));
	        row.setSigariEtaFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE));
	        row.setSigariDie(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE));

	        row.setPipa(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA));
	        row.setPipaAnni(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI));
	        row.setPipaEtaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO));
	        row.setPipaFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE));
	        row.setPipaEtaFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE));
	        row.setPipaDie(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE));

	        row.setOccupazioneNum(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM));
	        row.setOccupazioneAnnoInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO));
	        row.setOccupazioneAnnoFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE));

	        row.setOccupazioneTipo(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO));
	        row.setOccupazioneDescrizioneLavoro(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO));
	        row.setOccupazioneNomeEIndirizzoDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA));
	        row.setOccupazioneAttivitaDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA));
	        row.setNotaAttivitaConAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO));

	        row.setAnamnesiEsposizioneAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO));
	        row.setEsposizioneProfessionale(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE));
	        row.setAnnoFineEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE));
	        row.setLivelloEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE));
	        row.setInserimentoInSorveglianza(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA));

	        row.setIdSpresal(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL));
	        row.setValiditaInizio(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_INIZIO)));
	        row.setValiditaFine(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE)));
	        row.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CREAZIONE)));
	        row.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_MODIFICA)));
	        row.setDataCancellazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CANCELLAZIONE)));
	        row.setUtenteCreazione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CREAZIONE));
	        row.setUtenteModifica(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA));
	        row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CANCELLAZIONE));

	        row.setOccupazioneEsposizioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT));
	        row.setOccupazioneSettoreDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT));
	        row.setOccupazioneMansioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT));
	        row.setOccupazioneRagioneSocialeDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT));
	        row.setOccupazionePivaDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT));

	        return row;
	    }).findFirst().orElse(null);

	    return result;
	}
	

//	@Override
//	public SpresalDTO recuperaOggettoTRegistroSpresalAnamnesiByCF(String codiceFiscale, boolean registroIdGiaAssegnato) {
//		
//		Condition condition = REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.eq(codiceFiscale);
//
//	    if (registroIdGiaAssegnato) 
//	        condition = condition.and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.isNotNull());
//	    else
//	    	condition = condition.and(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID.isNull());
//		
//		var query = dsl
//				.select(
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE,
//
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE,
//
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE,
//						
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE,
//						
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO,
//						
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_INIZIO,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CREAZIONE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_MODIFICA,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CANCELLAZIONE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CREAZIONE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CANCELLAZIONE,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT,
//						REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO
//						)
//				.from(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
//				//	            .where(REEA_T_ADESIONE.CODICE_FISCALE.in("LMBGPP55R15D314W"));
//		.where(condition);
//
//		// fetch finale
//		SpresalDTO result = query.stream().map(r -> {
//
//			SpresalDTO row = new SpresalDTO();
//			row.setRegSpresalAnamnesiId(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID).longValue());
//			row.setRegistroId(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REGISTRO_ID));
//			row.setIdAura(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_AURA));
//			row.setDataIntervista(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_INTERVISTA));
//			row.setNominativoIntervistatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOMINATIVO_INTERVISTATORE));
//			row.setFumatore(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FUMATORE));
//			row.setSigarette(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE));
//			row.setSigaretteAnni(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ANNI));
//			row.setSigaretteEtaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_INIZIO));
//			row.setSigaretteFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_FUMA_ATTUALMENTE));
//			row.setSigaretteEtaFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_ETA_FINE));
//			row.setSigaretteDie(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARETTE_DIE));
//			row.setSigari(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI));
//			row.setSigariAnni(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ANNI));
//			row.setSigariEtaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_INIZIO));
//			row.setSigariFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_FUMA_ATTUALMENTE));
//			row.setSigariEtaFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_ETA_FINE));
//			row.setSigariDie(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.SIGARI_DIE));
//			row.setPipa(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA));
//			row.setPipaAnni(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ANNI));
//			row.setPipaEtaInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_INIZIO));
//			row.setPipaFumaAttualmente(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_FUMA_ATTUALMENTE));
//			row.setPipaEtaFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_ETA_FINE));
//			row.setPipaDie(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.PIPA_DIE));
//			row.setOccupazioneNum(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NUM));
//			row.setOccupazioneAnnoInizio(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_INIZIO));
//			row.setOccupazioneAnnoFine(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ANNO_FINE));
//			row.setOccupazioneTipo(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_TIPO));
//			row.setOccupazioneDescrizioneLavoro(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_DESCRIZIONE_LAVORO));
//			row.setOccupazioneNomeEIndirizzoDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_NOME_E_INDIRIZZO_DITTA));
//			row.setOccupazioneAttivitaDitta(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ATTIVITA_DITTA));
//			row.setNotaAttivitaConAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.NOTA_ATTIVITA_CON_AMIANTO));
//			
//			row.setAnamnesiEsposizioneAmianto(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANAMNESI_ESPOSIZIONE_AMIANTO));
//			row.setEsposizioneProfessionale(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ESPOSIZIONE_PROFESSIONALE));
//			row.setAnnoFineEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ANNO_FINE_ESPOSIZIONE));
//			row.setLivelloEsposizione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.LIVELLO_ESPOSIZIONE));
//			row.setInserimentoInSorveglianza(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.INSERIMENTO_IN_SORVEGLIANZA));
//			
//			row.setIdSpresal(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.ID_SPRESAL));
//			row.setValiditaInizio(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_INIZIO)));
//			row.setValiditaFine(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE)));
//			row.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CREAZIONE)));
//			row.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_MODIFICA)));
//			row.setDataCancellazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_CANCELLAZIONE)));
//			row.setUtenteCreazione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CREAZIONE));
//			row.setUtenteModifica(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA));
//			row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_CANCELLAZIONE));
//			
//			row.setOccupazioneEsposizioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT));
//			row.setOccupazioneSettoreDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT));
//			row.setOccupazioneMansioneCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT));
//			row.setOccupazioneRagioneSocialeDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT));
//			row.setOccupazionePivaDittaCrpt(r.get(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT));
//			
//
//			return row;
//		})
//		        .findFirst()
//		        .orElse(null);
//		return result;
//	}
	
	
	@Override
	public SpresalEsitiDTO recuperaOggettoTRegistroSpresalEsitiByCF(String codiceFiscale, boolean registroIdGiaAssegnato
											, LocalDate dataVisita, boolean elaborato) {

	    Condition condition = REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE.eq(codiceFiscale);

	    if (registroIdGiaAssegnato) {
	        condition = condition.and(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.isNotNull());
	    } else {
	        condition = condition.and(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.isNull());
	    }
	    // nuovo filtro su DATA_VISITA (se valorizzato)
	    if (dataVisita != null) {
	        condition = condition.and(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.eq(dataVisita));
	    }
	    
	    condition = condition.and(REEA_T_REGISTRO_SPRESAL_ESITI.ELABORATO.eq(elaborato));

	    var query = dsl
	        .select(
	            REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID,
	            REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID,
	            REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.VISITA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE,

	            REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO,

	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR,
	            REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR,
	            REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR,
	            REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA,

	            REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG,
	            REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL,

	            REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO,
	            REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE,
	            REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA,
	            REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE
	        )
	        .from(REEA_T_REGISTRO_SPRESAL_ESITI)
	        .where(condition);

	    SpresalEsitiDTO result = query.stream().map(r -> {
	        SpresalEsitiDTO row = new SpresalEsitiDTO();

	        row.setRegSpresalEsitiId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID).longValue());
	        row.setRegistroId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID));
	        row.setCodiceFiscale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE));
	        row.setIdAura(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA));
	        row.setDataVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA));
	        row.setVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VISITA));
	        row.setLivelloVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA));
	        row.setRiceveIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO));
	        row.setMalattiaIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO));

	        row.setAccertamentiRx(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX));
	        row.setAccertamentiRxData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA));
	        row.setAccertamentiRxRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE));

	        row.setAccertamentiTc(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC));
	        row.setAccertamentiTcData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA));
	        row.setAccertamentiTcRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE));

	        row.setAccertamentiSpirometriaSemplice(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE));
	        row.setAccertamentiSpirometriaSempliceData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA));
	        row.setAccertamentiSpirometriaSempliceRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM));

	        row.setAccertamentiSpirometriaGlobale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE));
	        row.setAccertamentiSpirometriaGlobaleData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA));
	        row.setAccertamentiSpirometriaGlobaleRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM));

	        row.setAccertamentiDlco(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO));
	        row.setAccertamentiDlcoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA));
	        row.setAccertamentiDlcoRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE));

	        row.setAccertamentiPet(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET));
	        row.setAccertamentiPetData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA));
	        row.setAccertamentiPetRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE));

	        row.setAccertamentiVisitaPneumologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA));
	        row.setAccertamentiVisitaPneumologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA));
	        row.setAccertamentiVisitaPneumologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO));

	        row.setAccertamentiVisitaRadiologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA));
	        row.setAccertamentiVisitaRadiologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA));
	        row.setAccertamentiVisitaRadiologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO));

	        row.setAccertamentiVisitaOncologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA));
	        row.setAccertamentiVisitaOncologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA));
	        row.setAccertamentiVisitaOncologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO));

	        row.setAccertamentiAltro(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO));
	        row.setAccertamentiAltroDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE));
	        row.setAccertamentiAltroData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA));
	        row.setAccertamentiAltroRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE));

	        row.setRisultatoNegativo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO));

	        row.setPpmPlacchePleuricheMonolaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI));
	        row.setPpmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setPpmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setPpmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA));
	        row.setPpmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setPpmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setPpmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO));
	        row.setPpmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA));

	        row.setPpbPlacchePleuricheBilaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI));
	        row.setPpbPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setPpbPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setPpbAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA));
	        row.setPpbAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setPpbPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setPpbReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO));
	        row.setPpbRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA));

	        row.setApAsbestosiPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE));
	        row.setApPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setApPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setApAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA));
	        row.setApAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setApPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setApReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO));
	        row.setApRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA));

	        row.setFpdFibrosiPleuricaDiffusa(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA));
	        row.setFpdPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setFpdPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setFpdAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA));
	        row.setFpdAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setFpdPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setFpdReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO));
	        row.setFpdRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA));

	        row.setMpMesoteliomaPleurico(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO));
	        row.setMpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setMpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setMpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA));
	        row.setMpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setMpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setMpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO));
	        row.setMpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA));
	        row.setMpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR));
	        row.setMpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA));

	        row.setAmAltroMesotelioma(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA));
	        row.setAmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setAmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setAmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA));
	        row.setAmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setAmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setAmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO));
	        row.setAmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA));
	        row.setAmComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR));
	        row.setAmComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA));

	        row.setNlNeoplasiaLaringe(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE));
	        row.setNlPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setNlPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setNlAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA));
	        row.setNlAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setNlPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setNlReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO));
	        row.setNlRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA));

	        row.setNoNeoplasiaOvarica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA));
	        row.setNoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setNoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setNoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA));
	        row.setNoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setNoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setNoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO));
	        row.setNoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA));

	        row.setTpTumoreDelPolmone(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE));
	        row.setTpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setTpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setTpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA));
	        row.setTpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setTpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setTpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO));
	        row.setTpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA));
	        row.setTpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR));
	        row.setTpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA));

	        row.setBpcoEnfisemaPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE));
	        row.setBpcoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setBpcoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setBpcoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA));
	        row.setBpcoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setBpcoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setBpcoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO));
	        row.setBpcoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA));

	        row.setAltraDiagnosi(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI));
	        row.setAltraDiagnosiDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE));
	        row.setAltraPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA));
	        row.setAltraPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
	        row.setAltraAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA));
	        row.setAltraAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA));
	        row.setAltraPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO));
	        row.setAltraReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO));
	        row.setAltraRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA));

	        row.setFollowUpPrevisto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO));
	        row.setAnnoPresuntoProssimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA));
	        row.setAnnoUltimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA));
	        row.setInvioSintesiAMmg(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG));
	        row.setIdSpresal(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL));

	        row.setValiditaInizio(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO)));
	        row.setValiditaFine(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE)));
	        row.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE)));
	        row.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA)));
	        row.setDataCancellazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE)));
	        row.setUtenteCreazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE));
	        row.setUtenteModifica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA));
	        row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE));

	        return row;
	    }).findFirst().orElse(null);

	    return result;
	}

	@Override
	public List<SpresalEsitiDTO> getEsitiByRegistroId(Integer registroId) {

		var query = dsl
				.select(
						REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID,
						REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID,
						REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA,
						REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO,
						REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA,

						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA,

						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA,

						REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG,
						REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL,
						REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO,
						REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE,
						REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE,
						REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE,
						REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE,
						REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE
				)
				.from(REEA_T_REGISTRO_SPRESAL_ESITI)
				// â UNICA DIFFERENZA rispetto a listaRecordSpresalEsiti()
				.where(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.eq(registroId.intValue()))
				.orderBy(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.desc());

		return query.stream().map(r -> {
			SpresalEsitiDTO row = new SpresalEsitiDTO();
			// copia esatta del mapping di listaRecordSpresalEsiti()
			row.setRegSpresalEsitiId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID).longValue());
			row.setRegistroId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID));
			row.setCodiceFiscale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE));
			row.setIdAura(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA));
			row.setDataVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA));
			row.setVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VISITA));
			row.setLivelloVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA));
			row.setRiceveIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO));
			row.setMalattiaIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO));
			row.setAccertamentiRx(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX));
			row.setAccertamentiRxData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA));
			row.setAccertamentiRxRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE));
			row.setAccertamentiTc(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC));
			row.setAccertamentiTcData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA));
			row.setAccertamentiTcRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE));
			row.setAccertamentiSpirometriaSemplice(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE));
			row.setAccertamentiSpirometriaSempliceData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA));
			row.setAccertamentiSpirometriaSempliceRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM));
			row.setAccertamentiSpirometriaGlobale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE));
			row.setAccertamentiSpirometriaGlobaleData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA));
			row.setAccertamentiSpirometriaGlobaleRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM));
			row.setAccertamentiDlco(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO));
			row.setAccertamentiDlcoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA));
			row.setAccertamentiDlcoRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE));
			row.setAccertamentiPet(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET));
			row.setAccertamentiPetData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA));
			row.setAccertamentiPetRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE));
			row.setAccertamentiVisitaPneumologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA));
			row.setAccertamentiVisitaPneumologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA));
			row.setAccertamentiVisitaPneumologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO));
			row.setAccertamentiVisitaRadiologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA));
			row.setAccertamentiVisitaRadiologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA));
			row.setAccertamentiVisitaRadiologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO));
			row.setAccertamentiVisitaOncologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA));
			row.setAccertamentiVisitaOncologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA));
			row.setAccertamentiVisitaOncologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO));
			row.setAccertamentiAltro(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO));
			row.setAccertamentiAltroDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE));
			row.setAccertamentiAltroData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA));
			row.setAccertamentiAltroRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE));
			row.setRisultatoNegativo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO));
			row.setPpmPlacchePleuricheMonolaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI));
			row.setPpmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setPpmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setPpmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA));
			row.setPpmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setPpmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setPpmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO));
			row.setPpmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA));
			row.setPpbPlacchePleuricheBilaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI));
			row.setPpbPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setPpbPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setPpbAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA));
			row.setPpbAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setPpbPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setPpbReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO));
			row.setPpbRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA));
			row.setApAsbestosiPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE));
			row.setApPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setApPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setApAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA));
			row.setApAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setApPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setApReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO));
			row.setApRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA));
			row.setFpdFibrosiPleuricaDiffusa(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA));
			row.setFpdPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setFpdPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setFpdAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA));
			row.setFpdAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setFpdPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setFpdReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO));
			row.setFpdRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA));
			row.setMpMesoteliomaPleurico(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO));
			row.setMpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setMpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setMpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA));
			row.setMpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setMpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setMpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO));
			row.setMpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA));
			row.setMpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR));
			row.setMpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA));
			row.setAmAltroMesotelioma(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA));
			row.setAmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setAmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setAmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA));
			row.setAmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setAmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setAmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO));
			row.setAmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA));
			row.setAmComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR));
			row.setAmComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA));
			row.setNlNeoplasiaLaringe(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE));
			row.setNlPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setNlPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setNlAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA));
			row.setNlAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setNlPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setNlReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO));
			row.setNlRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA));
			row.setNoNeoplasiaOvarica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA));
			row.setNoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setNoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setNoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA));
			row.setNoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setNoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setNoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO));
			row.setNoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA));
			row.setTpTumoreDelPolmone(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE));
			row.setTpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setTpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setTpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA));
			row.setTpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setTpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setTpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO));
			row.setTpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA));
			row.setTpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR));
			row.setTpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA));
			row.setBpcoEnfisemaPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE));
			row.setBpcoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setBpcoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setBpcoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA));
			row.setBpcoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setBpcoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setBpcoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO));
			row.setBpcoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA));
			row.setAltraDiagnosi(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI));
			row.setAltraDiagnosiDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE));
			row.setAltraPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setAltraPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setAltraAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA));
			row.setAltraAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setAltraPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setAltraReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO));
			row.setAltraRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA));
			row.setFollowUpPrevisto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO));
			row.setAnnoPresuntoProssimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA));
			row.setAnnoUltimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA));
			row.setInvioSintesiAMmg(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG));
			row.setIdSpresal(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL));

			row.setAccertamentiRxAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA));
			row.setAccertamentiTcAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA));
			row.setAccertamentiSpirometriaSempliceAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA));
			row.setAccertamentiSpirometriaGlobaleAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA));
			row.setAccertamentiDlcoAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA));
			row.setAccertamentiPetAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA));
			row.setAccertamentiVisitaPneumologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA));
			row.setAccertamentiVisitaRadiologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA));
			row.setAccertamentiVisitaOncologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA));
			row.setAccertamentiAltroRefertoAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA));

			row.setValiditaInizio(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO)));
			row.setValiditaFine(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE)));
			row.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE)));
			row.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA)));
			row.setDataCancellazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE)));
			row.setUtenteCreazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE));
			row.setUtenteModifica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA));
			row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE));
			return row;
		}).collect(Collectors.toList());
	}


	public void eliminaSpresalEsitiSenzaRegistroId() {
		dsl.deleteFrom(REEA_T_REGISTRO_SPRESAL_ESITI)
				.where(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.isNull())
				.execute();
	}

	@Override
	public void aggiornaCrpt(Integer regSpresalAnamnesiId, Map<String, Object> body, AuditLogRequest auditLogRequest) {
		String rs   = (String) body.get("occupazione_ragione_sociale_ditta_crpt");
		String piva = (String) body.get("occupazione_piva_ditta_crpt");
		String cf   = (String) body.get("occupazione_codice_fiscale_ditta_crpt");

		boolean hasRs = rs != null && !rs.trim().isEmpty();

// Per il dizionario: null se vuoti (non "Non definito")
		String pivaDict = (piva != null && !piva.trim().isEmpty()) ? piva : null;
		String cfDict   = (cf   != null && !cf.trim().isEmpty())   ? cf   : null;

// Per la tabella anamnesi: "Non definito" se RS valorizzata e PIVA/CF vuoti
		if (hasRs) {
			if (piva == null || piva.trim().isEmpty()) piva = "Non definito";
			if (cf   == null || cf.trim().isEmpty())   cf   = "Non definito";
		}

		// 1. Aggiorna il record anamnesi
		dsl.update(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_ESPOSIZIONE_CRPT,
						(Boolean) body.get("occupazione_esposizione_crpt"))
				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_SETTORE_DITTA_CRPT,
						(String) body.get("occupazione_settore_ditta_crpt"))
				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_MANSIONE_CRPT,
						(String) body.get("occupazione_mansione_crpt"))
				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_RAGIONE_SOCIALE_DITTA_CRPT, rs)
				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_PIVA_DITTA_CRPT, piva)
				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.OCCUPAZIONE_CODICE_FISCALE_DITTA_CRPT, cf)
				.where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID.eq(regSpresalAnamnesiId))
				.execute();

		// 2. Dizionario — best effort, non blocca il salvataggio
		if (hasRs) {
			try {
				boolean exists = dsl.fetchExists(
						dsl.selectOne()
								.from(REEA_D_REGISTRO_SPRESAL_ANAMNESI_RAGIONESOCIALE)
								.where(DSL.field("ragionesociale", String.class).eq(rs))
				);
				if (exists) {
					var update = dsl.update(REEA_D_REGISTRO_SPRESAL_ANAMNESI_RAGIONESOCIALE)
							.set(DSL.field("data_modifica"), DSL.currentTimestamp())
							.set(DSL.field("utente_modifica", String.class), auditLogRequest.getUtente());

					// Aggiorna PIVA solo se valorizzata — non sovrascrivere con null
					if (pivaDict != null) {
						update = update.set(DSL.field("piva", String.class), pivaDict);
					}
					// Aggiorna CF solo se valorizzato — non sovrascrivere con null
					if (cfDict != null) {
						update = update.set(DSL.field("ditta_codice_fiscale", String.class), cfDict);
					}

					update.where(DSL.field("ragionesociale", String.class).eq(rs))
							.execute();
				} else {
					dsl.insertInto(REEA_D_REGISTRO_SPRESAL_ANAMNESI_RAGIONESOCIALE)
							.set(DSL.field("ragionesociale", String.class), rs)
							.set(DSL.field("piva", String.class), pivaDict)
							.set(DSL.field("ditta_codice_fiscale", String.class), cfDict)
							.set(DSL.field("data_creazione"), DSL.currentTimestamp())
							.set(DSL.field("data_modifica"), DSL.currentTimestamp())
							.set(DSL.field("utente_creazione", String.class), auditLogRequest.getUtente())
							.set(DSL.field("utente_modifica", String.class), auditLogRequest.getUtente())
							.execute();
				}

			} catch (Exception e) {
				System.err.println("Warning dizionario ragionesociale: " + e.getMessage());
			}
		}
	}


	@Override
	public void aggiornaCounseling(Integer regSpresalAnamnesiId, Map<String, Object> body, AuditLogRequest auditLogRequest) {
		String counseling = (String) body.get("counseling");

		dsl.update(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.COUNSELING, counseling)
				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA, auditLogRequest.getUtente())
				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.DATA_MODIFICA, DSL.currentLocalDateTime())
				.where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE.eq(
						dsl.select(REEA_T_REGISTRO_SPRESAL_ANAMNESI.CODICE_FISCALE)
								.from(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
								.where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID.eq(regSpresalAnamnesiId))
				))
				.execute();
	}

//    @Override
//    public void aggiornaPrestazioneAcquisita(Integer esitoId, Map<String, Object> body) {
//        String field = (String) body.get("field");
//        Boolean value = (Boolean) body.get("value");
//
//        Map<String, String> fieldToColumn = Map.of(
//                "accertamenti_rx_acquisita",                   "ACCERTAMENTI_RX_ACQUISITA",
//                "accertamenti_tc_acquisita",                   "ACCERTAMENTI_TC_ACQUISITA",
//                "accertamenti_spirometria_semplice_acquisita", "ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA",
//                "accertamenti_spirometria_globale_acquisita",  "ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA",
//                "accertamenti_dlco_acquisita",                 "ACCERTAMENTI_DLCO_ACQUISITA",
//                "accertamenti_pet_acquisita",                  "ACCERTAMENTI_PET_ACQUISITA",
//                "accertamenti_altro_referto_acquisita",        "ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA",
//                "accertamenti_visita_pneumologica_acquisita",  "ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA",
//                "accertamenti_visita_radiologica_acquisita",   "ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA",
//                "accertamenti_visita_oncologica_acquisita",    "ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA"
//        );
//
//        String columnName = fieldToColumn.get(field);
//        if (columnName == null) throw new IllegalArgumentException("Campo non consentito: " + field);
//
//        dsl.update(REEA_T_REGISTRO_SPRESAL_ESITI)
//                .set(DSL.field(DSL.name(columnName), Boolean.class), value)
//                .where(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID.eq(esitoId))
//                .execute();
//    }


	@Override
	public void aggiornaPrestazioneAcquisita(Integer esitoId, Map<String, Object> body, AuditLogRequest auditLogRequest) {
		String field = (String) body.get("field");
		Boolean value = (Boolean) body.get("value");

		Map<String, String> fieldToColumn = Map.of(
				"accertamenti_rx_acquisita",                   "accertamenti_rx_acquisita",
				"accertamenti_tc_acquisita",                   "accertamenti_tc_acquisita",
				"accertamenti_spirometria_semplice_acquisita",  "accertamenti_spirometria_semplice_acquisita",
				"accertamenti_spirometria_globale_acquisita",   "accertamenti_spirometria_globale_acquisita",
				"accertamenti_dlco_acquisita",                  "accertamenti_dlco_acquisita",
				"accertamenti_pet_acquisita",                   "accertamenti_pet_acquisita",
				"accertamenti_altro_referto_acquisita",          "accertamenti_altro_referto_acquisita",
				"accertamenti_visita_pneumologica_acquisita",    "accertamenti_visita_pneumologica_acquisita",
				"accertamenti_visita_radiologica_acquisita",     "accertamenti_visita_radiologica_acquisita",
				"accertamenti_visita_oncologica_acquisita",      "accertamenti_visita_oncologica_acquisita"
		);

		String columnName = fieldToColumn.get(field);
		if (columnName == null) throw new IllegalArgumentException("Campo non consentito: " + field);

		String textValue = value == null ? null : (value ? "SI" : "NO");

		dsl.update(REEA_T_REGISTRO_SPRESAL_ESITI)
				.set(DSL.field(DSL.name(columnName), String.class), textValue)
				.set(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA, auditLogRequest.getUtente())
				.set(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA, DSL.currentLocalDateTime())
				.where(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID.eq(esitoId))
				.execute();
	}


    @Override
	public Integer chiudiRecordDuplicatoSpresal(SpresalDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione) {

		Integer ris = dsl.update(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
		        .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE, DSL.currentLocalDateTime())
		        .set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.UTENTE_MODIFICA, dto.getUtenteCreazione())
		        .where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.REG_SPRESAL_ANAMNESI_ID
		               .eq(dto.getRegSpresalAnamnesiId().intValue()))
		        .execute();
		if(elaborazioneId != null) {
	    	tracciaElaborazioneRepository.inserisciFileImpatto(
	    			dsl,
	                elaborazioneId,
	                "REEA_T_REGISTRO_SPRESAL_ANAMNESI",
	                dto.getRegSpresalAnamnesiId().intValue(),
	                tipoOperazione,
	                utente
	        );
	    }
		return ris;
	}
	
	@Override
	public Integer chiudiRecordDuplicatoSpresalEsiti(SpresalEsitiDTO dto, Integer elaborazioneId, String utente, Integer tipoOperazione) {
		
		    Integer ris = dsl.update(REEA_T_REGISTRO_SPRESAL_ESITI)
		        .set(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE, DSL.currentLocalDateTime())
		        .set(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA, dto.getUtenteCreazione())
		        .where(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID
		               .eq(dto.getRegSpresalEsitiId().intValue()))
		        .execute();
		    
		    if(elaborazioneId != null) {
		    	tracciaElaborazioneRepository.inserisciFileImpatto(
		    			dsl,
		                elaborazioneId,
		                "REEA_T_REGISTRO_SPRESAL_ESITI",
		                dto.getRegSpresalEsitiId().intValue(),
		                tipoOperazione,
		                utente
		        );
		    }
		    
		    return ris;
	}


	@Override
	public List<SpresalEsitiDTO> queryAllegato4Tutti(int anno) {
		var query = dsl
				.select(
						REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID,
						REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID,
						REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA,
						REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO,
						REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR,
						REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR,
						REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR,
						REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA,
						REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO,
						REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA,
						REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG,
						REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL,
						REEA_D_ASL.ASL_AZIENDA_DESC,
						REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC
				)
				.from(REEA_T_REGISTRO_SPRESAL_ESITI)
				.leftJoin(REEA_T_REGISTRO)
				.on(REEA_T_REGISTRO.REGISTRO_ID.eq(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID))
				.leftJoin(REEA_T_SOGGETTO)
				.on(REEA_T_SOGGETTO.SOGGETTO_ID.eq(
						DSL.field("convert_from(pgp_sym_decrypt_bytea({0}, '16<odcc8!'), 'UTF8')::int",
										Integer.class, REEA_T_REGISTRO.SOGGETTO_ID_CIFRATO)))
				.leftJoin(REEA_D_SOGGETTO_STATO)
				.on(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_ID.eq(REEA_T_SOGGETTO.SOGGETTO_STATO_ID))
				.leftJoin(REEA_D_ASL)
				.on(REEA_D_ASL.ASL_ID.eq(REEA_T_SOGGETTO.ASSISTENZA_ASL_ID))
				.where(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID.isNotNull())
				.and(
						DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA).eq(anno)
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA).eq(anno))
								.or(DSL.year(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA).eq(anno))
				)
				.orderBy(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE.asc(),
						REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA.asc());

		// mapping identico a listaRecordSpresalEsiti
		return query.stream().map(r -> {
			SpresalEsitiDTO row = new SpresalEsitiDTO();
			row.setRegSpresalEsitiId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REG_SPRESAL_ESITI_ID).longValue());
			row.setRegistroId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.REGISTRO_ID));
			row.setCodiceFiscale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.CODICE_FISCALE));
			row.setIdAura(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_AURA));
			row.setDataVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_VISITA));
			row.setVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VISITA));
			row.setLivelloVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.LIVELLO_VISITA));
			row.setRiceveIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RICEVE_INDENNIZZO));
			row.setMalattiaIndennizzo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MALATTIA_INDENNIZZO));
			row.setAccertamentiRx(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX));
			row.setAccertamentiRxData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_DATA));
			row.setAccertamentiRxRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_REFERTO_NORMALE));
			row.setAccertamentiRxAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_RX_ACQUISITA));
			row.setAccertamentiTc(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC));
			row.setAccertamentiTcData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_DATA));
			row.setAccertamentiTcRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_REFERTO_NORMALE));
			row.setAccertamentiTcAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_TC_ACQUISITA));
			row.setAccertamentiSpirometriaSemplice(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE));
			row.setAccertamentiSpirometriaSempliceData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_DATA));
			row.setAccertamentiSpirometriaSempliceRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_REFERTO_NORM));
			row.setAccertamentiSpirometriaSempliceAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_SEMPLICE_ACQUISITA));
			row.setAccertamentiSpirometriaGlobale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE));
			row.setAccertamentiSpirometriaGlobaleData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_DATA));
			row.setAccertamentiSpirometriaGlobaleRefertoNorm(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_REFERTO_NORM));
			row.setAccertamentiSpirometriaGlobaleAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_SPIROMETRIA_GLOBALE_ACQUISITA));
			row.setAccertamentiDlco(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO));
			row.setAccertamentiDlcoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_DATA));
			row.setAccertamentiDlcoRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_REFERTO_NORMALE));
			row.setAccertamentiDlcoAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_DLCO_ACQUISITA));
			row.setAccertamentiPet(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET));
			row.setAccertamentiPetData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_DATA));
			row.setAccertamentiPetRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_REFERTO_NORMALE));
			row.setAccertamentiPetAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_PET_ACQUISITA));
			row.setAccertamentiVisitaPneumologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA));
			row.setAccertamentiVisitaPneumologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_DATA));
			row.setAccertamentiVisitaPneumologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_REFERTO));
			row.setAccertamentiVisitaPneumologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_PNEUMOLOGICA_ACQUISITA));
			row.setAccertamentiVisitaRadiologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA));
			row.setAccertamentiVisitaRadiologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_DATA));
			row.setAccertamentiVisitaRadiologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_REFERTO));
			row.setAccertamentiVisitaRadiologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_RADIOLOGICA_ACQUISITA));
			row.setAccertamentiVisitaOncologica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA));
			row.setAccertamentiVisitaOncologicaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_DATA));
			row.setAccertamentiVisitaOncologicaReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_REFERTO));
			row.setAccertamentiVisitaOncologicaAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_VISITA_ONCOLOGICA_ACQUISITA));
			row.setAccertamentiAltro(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO));
			row.setAccertamentiAltroDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DESCRIZIONE));
			row.setAccertamentiAltroData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_DATA));
			row.setAccertamentiAltroRefertoNormale(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_NORMALE));
			row.setAccertamentiAltroRefertoAcquisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ACCERTAMENTI_ALTRO_REFERTO_ACQUISITA));
			row.setRisultatoNegativo(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.RISULTATO_NEGATIVO));
			row.setPpmPlacchePleuricheMonolaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PLACCHE_PLEURICHE_MONOLATERALI));
			row.setPpmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setPpmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setPpmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA));
			row.setPpmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setPpmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setPpmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO));
			row.setPpmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPM_REFERTO_DATA));
			row.setPpbPlacchePleuricheBilaterali(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PLACCHE_PLEURICHE_BILATERALI));
			row.setPpbPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setPpbPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setPpbAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA));
			row.setPpbAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setPpbPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setPpbReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO));
			row.setPpbRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.PPB_REFERTO_DATA));
			row.setApAsbestosiPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_ASBESTOSI_POLMONARE));
			row.setApPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setApPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setApAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA));
			row.setApAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setApPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setApReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO));
			row.setApRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AP_REFERTO_DATA));
			row.setFpdFibrosiPleuricaDiffusa(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_FIBROSI_PLEURICA_DIFFUSA));
			row.setFpdPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setFpdPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setFpdAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA));
			row.setFpdAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setFpdPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setFpdReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO));
			row.setFpdRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FPD_REFERTO_DATA));
			row.setMpMesoteliomaPleurico(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_MESOTELIOMA_PLEURICO));
			row.setMpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setMpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setMpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA));
			row.setMpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setMpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setMpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO));
			row.setMpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_REFERTO_DATA));
			row.setMpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR));
			row.setMpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.MP_COMUNICAZIONE_AL_COR_DATA));
			row.setAmAltroMesotelioma(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_ALTRO_MESOTELIOMA));
			row.setAmPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setAmPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setAmAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA));
			row.setAmAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setAmPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setAmReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO));
			row.setAmRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_REFERTO_DATA));
			row.setAmComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR));
			row.setAmComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.AM_COMUNICAZIONE_AL_COR_DATA));
			row.setNlNeoplasiaLaringe(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_NEOPLASIA_LARINGE));
			row.setNlPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setNlPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setNlAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA));
			row.setNlAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setNlPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setNlReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO));
			row.setNlRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NL_REFERTO_DATA));
			row.setNoNeoplasiaOvarica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_NEOPLASIA_OVARICA));
			row.setNoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setNoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setNoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA));
			row.setNoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setNoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setNoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO));
			row.setNoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.NO_REFERTO_DATA));
			row.setTpTumoreDelPolmone(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_TUMORE_DEL_POLMONE));
			row.setTpPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setTpPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setTpAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA));
			row.setTpAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setTpPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setTpReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO));
			row.setTpRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_REFERTO_DATA));
			row.setTpComunicazioneAlCor(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR));
			row.setTpComunicazioneAlCorData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.TP_COMUNICAZIONE_AL_COR_DATA));
			row.setBpcoEnfisemaPolmonare(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_ENFISEMA_POLMONARE));
			row.setBpcoPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setBpcoPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setBpcoAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA));
			row.setBpcoAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setBpcoPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setBpcoReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO));
			row.setBpcoRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.BPCO_REFERTO_DATA));
			row.setAltraDiagnosi(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI));
			row.setAltraDiagnosiDescrizione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_DIAGNOSI_DESCRIZIONE));
			row.setAltraPrimoCertificatoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA));
			row.setAltraPrimoCertificatoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PRIMO_CERTIFICATO_E_DENUNCIA_DATA));
			row.setAltraAggravamentoEDenuncia(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA));
			row.setAltraAggravamentoEDenunciaData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_AGGRAVAMENTO_E_DENUNCIA_DATA));
			row.setAltraPercentualeDiRiconoscimento(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_PERCENTUALE_DI_RICONOSCIMENTO));
			row.setAltraReferto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO));
			row.setAltraRefertoData(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ALTRA_REFERTO_DATA));
			row.setFollowUpPrevisto(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FOLLOW_UP_PREVISTO));
			row.setAnnoPresuntoProssimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_PRESUNTO_PROSSIMA_VISITA));
			row.setAnnoUltimaVisita(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ANNO_ULTIMA_VISITA));
			row.setInvioSintesiAMmg(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.INVIO_SINTESI_A_MMG));
			row.setIdSpresal(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.ID_SPRESAL));
//			row.setValiditaInizio(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_INIZIO)));
//			row.setValiditaFine(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE)));
//			row.setDataCreazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CREAZIONE)));
//			row.setDataModifica(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_MODIFICA)));
//			row.setDataCancellazione(DateConversionUtils.toOffsetDateTime(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.DATA_CANCELLAZIONE)));
//			row.setUtenteCreazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CREAZIONE));
//			row.setUtenteModifica(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_MODIFICA));
//			row.setUtenteCancellazione(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.UTENTE_CANCELLAZIONE));
//			row.setFileId(r.get(REEA_T_REGISTRO_SPRESAL_ESITI.FILE_ID));
			row.setAslAssistenza(r.get(REEA_D_ASL.ASL_AZIENDA_DESC));
			row.setSoggettoStatoDesc(r.get(REEA_D_SOGGETTO_STATO.SOGGETTO_STATO_DESC));
			return row;
		}).collect(Collectors.toList());
	}

	@Override
	public List<Allegato4ElaborazioneDTO> listaElaborazioniAllegato4() {
		return dsl
				.select(
						DSL.field("e.elaborazione_id", Integer.class).as("elab_id"),
						DSL.field("f.file_name", String.class).as("f_name"),
						DSL.field("s.file_stato_desc", String.class).as("stato_desc"),
						DSL.field("e.data_inizio", LocalDateTime.class).as("d_inizio"),
						DSL.field("e.data_fine", LocalDateTime.class).as("d_fine"),
						DSL.field("e.utente_creazione", String.class).as("utente_cr")
				)
				.from(DSL.table("reea.reea_l_file_elaborazione e"))
				.join(DSL.table("reea.reea_t_file f"))
				.on(DSL.field("e.file_id", Integer.class).eq(DSL.field("f.file_id", Integer.class)))
				.join(DSL.table("reea.reea_d_file_stato s"))
				.on(DSL.field("f.file_stato_id", Integer.class).eq(DSL.field("s.file_stato_id", Integer.class)))
				.where(DSL.field("f.file_name", String.class).like("export_PIEMONTE_all4_%"))
				.orderBy(DSL.field("e.data_inizio").desc())
				.stream()
				.map(r -> {
					Allegato4ElaborazioneDTO dto = new Allegato4ElaborazioneDTO();
					dto.setElaborazioneId(r.get("elab_id", Integer.class));
					String fileName = r.get("f_name", String.class);
					dto.setFileName(fileName);
					if (fileName != null) {
						try {
							// format: export_PIEMONTE_all4_YYYY_aaaammddhhmm.xlsx
							String[] parts = fileName.replace(".xlsx", "").split("_");
							dto.setAnno(Integer.parseInt(parts[3]));
						} catch (Exception ignored) {}
					}
					dto.setStato(r.get("stato_desc", String.class));
					dto.setOperatore(r.get("utente_cr", String.class));
					dto.setDataRichiesta(DateConversionUtils.toOffsetDateTime(r.get("d_inizio", LocalDateTime.class)));
					dto.setDataGenerazione(DateConversionUtils.toOffsetDateTime(r.get("d_fine", LocalDateTime.class)));
					return dto;
				})
				.collect(Collectors.toList());
	}

	@Override
	public String getFilePathAllegato4(Integer elaborazioneId) {
		return dsl
				.select(DSL.field("f.file_path", String.class).as("f_path"))
				.from(DSL.table("reea.reea_l_file_elaborazione e"))
				.join(DSL.table("reea.reea_t_file f"))
				.on(DSL.field("e.file_id", Integer.class).eq(DSL.field("f.file_id", Integer.class)))
				.where(DSL.field("e.elaborazione_id", Integer.class).eq(elaborazioneId))
				.fetchOne(r -> r.get("f_path", String.class));
	}
	
	
	@Override
	public void chiudiTuttiRecordTabRegistroSpresalAnamnesiByFile(Integer fileId) {
    		dsl.update(REEA_T_REGISTRO_SPRESAL_ANAMNESI)
    				.set(REEA_T_REGISTRO_SPRESAL_ANAMNESI.VALIDITA_FINE, DSL.currentLocalDateTime())
    				.where(REEA_T_REGISTRO_SPRESAL_ANAMNESI.FILE_ID.eq(fileId))
    				.execute();	
	}
	
	
	@Override
	public void chiudiTuttiRecordTabRegistroSpresalEsitiByFile(Integer fileId) {
    		dsl.update(REEA_T_REGISTRO_SPRESAL_ESITI)
    				.set(REEA_T_REGISTRO_SPRESAL_ESITI.VALIDITA_FINE, DSL.currentLocalDateTime())
    				.where(REEA_T_REGISTRO_SPRESAL_ESITI.FILE_ID.eq(fileId))
    				.execute();	
	}


}
