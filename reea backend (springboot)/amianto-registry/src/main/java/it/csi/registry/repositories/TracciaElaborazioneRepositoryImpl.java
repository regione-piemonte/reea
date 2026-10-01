package it.csi.registry.repositories;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.TracciaFileDTO;

import static it.csi.registry.jooq.tables.ReeaLFileElaborazione.REEA_L_FILE_ELABORAZIONE;
import static it.csi.registry.jooq.tables.ReeaTFile.REEA_T_FILE;
import static it.csi.registry.jooq.tables.ReeaTFileImpatto.REEA_T_FILE_IMPATTO;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static it.csi.registry.jooq.tables.ReeaLFileElaborazioneErrore.REEA_L_FILE_ELABORAZIONE_ERRORE;
import static it.csi.registry.jooq.tables.ReeaLFileScaricoErrore.REEA_L_FILE_SCARICO_ERRORE;

@Repository
public class TracciaElaborazioneRepositoryImpl implements TracciaElaborazioneRepository{
	
	private static final String PGP_KEY = "16<odcc8!";
	
	@Override
	public Integer inserisciFile(DSLContext ctx,
			String fileName,
			String filePath,
			String fileHashChecksum,
			String fileMimeType,
			long fileDimensioneBytes,
			Integer fileStatoId,
			String utente) {

		return ctx.insertInto(REEA_T_FILE)
				.set(REEA_T_FILE.FILE_NAME, fileName)
				.set(REEA_T_FILE.FILE_PATH, filePath)
				.set(REEA_T_FILE.FILE_HASH_CHECKSUM, fileHashChecksum)
				.set(REEA_T_FILE.FILE_MIME_TYPE, fileMimeType)
				.set(REEA_T_FILE.FILE_DIMENSIONE_BYTES, fileDimensioneBytes)
				.set(REEA_T_FILE.FILE_STATO_ID, fileStatoId)
				.set(REEA_T_FILE.VALIDITA_INIZIO, DSL.currentLocalDateTime())
				.set(REEA_T_FILE.DATA_CREAZIONE, DSL.currentLocalDateTime())
				.set(REEA_T_FILE.DATA_MODIFICA, DSL.currentLocalDateTime())
				.set(REEA_T_FILE.UTENTE_CREAZIONE, utente)
				.set(REEA_T_FILE.UTENTE_MODIFICA, utente)
				.returning(REEA_T_FILE.FILE_ID)
				.fetchOne()
				.getValue(REEA_T_FILE.FILE_ID, Integer.class);
	}

	@Override
	public Integer inserisciTracciaElaborazione(DSLContext ctx,
			Integer fileId,
			Integer fileStatoId,
			String utenteCreazione) {

		return ctx.insertInto(REEA_L_FILE_ELABORAZIONE)
				.set(REEA_L_FILE_ELABORAZIONE.FILE_ID, fileId)
				.set(REEA_L_FILE_ELABORAZIONE.NUMERO_TENTATIVO, 1)
				.set(REEA_L_FILE_ELABORAZIONE.FILE_STATO_ID, fileStatoId)
				.set(REEA_L_FILE_ELABORAZIONE.DATA_INIZIO, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_TOTALI, 0)
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_ELABORATE, 0)
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_SCARTATE, 0)
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_ERRATE, 0)
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_MODIFICANTI, 0)
				.set(REEA_L_FILE_ELABORAZIONE.VALIDITA_INIZIO, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_ELABORAZIONE.DATA_CREAZIONE, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_ELABORAZIONE.DATA_MODIFICA, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_ELABORAZIONE.UTENTE_CREAZIONE, utenteCreazione)
				.set(REEA_L_FILE_ELABORAZIONE.UTENTE_MODIFICA, utenteCreazione)
				.returning(REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID)
				.fetchOne()
				.getValue(REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID, Integer.class);
	}

	@Override
	public Integer aggiornaTracciaElaborazioneFineOk(DSLContext ctx,
			Integer elaborazioneId,
			Integer fileStatoId,
			Integer righeTotali,
			Integer righeElaborate,
			Integer righeScartate,
			Integer righeModificanti,
			Integer righeErrore,
			String messaggioSistema,
			String utenteModifica) {

		return ctx.update(REEA_L_FILE_ELABORAZIONE)
				.set(REEA_L_FILE_ELABORAZIONE.DATA_FINE, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_ELABORAZIONE.FILE_STATO_ID, fileStatoId)
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_TOTALI, righeTotali)
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_ELABORATE, righeElaborate)
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_SCARTATE, righeScartate)
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_MODIFICANTI, righeModificanti)
				.set(REEA_L_FILE_ELABORAZIONE.RIGHE_ERRATE, righeErrore)
				.set(REEA_L_FILE_ELABORAZIONE.MESSAGGIO_SISTEMA, messaggioSistema)
				.set(REEA_L_FILE_ELABORAZIONE.DATA_MODIFICA, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_ELABORAZIONE.UTENTE_MODIFICA, utenteModifica)
				.where(REEA_L_FILE_ELABORAZIONE.ELABORAZIONE_ID.eq(elaborazioneId))
				.execute();
	}

	@Override
	public Integer findFileIdByFileHashChecksum(DSLContext ctx, String fileChecksum) {
		return ctx
	            .select(REEA_T_FILE.FILE_ID)
	            .from(REEA_T_FILE)
	            .where(REEA_T_FILE.FILE_HASH_CHECKSUM.eq(fileChecksum))
	            .fetchOne(REEA_T_FILE.FILE_ID);
	}

	@Override
	public Integer aggiornaFile(DSLContext ctx, Integer fileId, Integer fileStatoId, String utenteCreazione) {
		return ctx.update(REEA_T_FILE)
				// DE RIMANDATA LA CHIUSURA AL METODO aggiornaFineValidita
				//.set(REEA_T_FILE.VALIDITA_FINE, DSL.currentLocalDateTime())
				.set(REEA_T_FILE.FILE_STATO_ID, fileStatoId)
				.set(REEA_T_FILE.DATA_MODIFICA, DSL.currentLocalDateTime())
				.set(REEA_T_FILE.UTENTE_MODIFICA, utenteCreazione)
				.where(REEA_T_FILE.FILE_ID.eq(fileId))
				.execute();
	}
	
	
	@Override
	public Integer aggiornaFileExcel(DSLContext ctx, Integer fileId, Integer fileStatoId, String checksum, String contentType, Long size, String utenteCreazione) {
		return ctx.update(REEA_T_FILE)
				// DE RIMANDATA LA CHIUSURA AL METODO aggiornaFineValidita
				//.set(REEA_T_FILE.VALIDITA_FINE, DSL.currentLocalDateTime())
				.set(REEA_T_FILE.FILE_STATO_ID, fileStatoId)
				.set(REEA_T_FILE.DATA_MODIFICA, DSL.currentLocalDateTime())
				.set(REEA_T_FILE.UTENTE_MODIFICA, utenteCreazione)
				.set(REEA_T_FILE.FILE_HASH_CHECKSUM, checksum)
				.set(REEA_T_FILE.FILE_MIME_TYPE, contentType)
				.set(REEA_T_FILE.FILE_DIMENSIONE_BYTES, size)
				.where(REEA_T_FILE.FILE_ID.eq(fileId))
				.execute();
	}

	// DE AGGIUNTO METODO PER CHIUSURA FILE SOLO QUANDO COMPLETATA TUTTA LA SCRITTURA DEL FILE
	@Override
	public Integer aggiornaFineValidita(DSLContext ctx, Integer fileId) {
		return ctx.update(REEA_T_FILE)
				.set(REEA_T_FILE.VALIDITA_FINE, DSL.currentLocalDateTime())
				.where(REEA_T_FILE.FILE_ID.eq(fileId))
				.execute();
	}



	@Override
	public Integer inserisciErroreRiga(DSLContext ctx,
									   Integer elaborazioneId,
									   String codiceErrore,
									   String descrizioneErrore,
									   String contenutoRigaRaw,
									   String utenteCreazione,
									   String cognome,
									   String nome,
									   LocalDate dataNascita,
									   String sesso,
									   String codiceFiscale,
									   String targetNomeTabella,
									   Integer targetRecordId) {

		return ctx.insertInto(REEA_L_FILE_ELABORAZIONE_ERRORE)
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.ELABORAZIONE_ID, elaborazioneId)
//				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.NUMERO_RIGA, numeroRiga)
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_TABELLA_NOME, targetNomeTabella)
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.TARGET_RECORD_ID, targetRecordId)
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.CODICE_ERRORE, codiceErrore)
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.DESCRIZIONE_ERRORE, descrizioneErrore)
				.set(
						REEA_L_FILE_ELABORAZIONE_ERRORE.CONTENUTO_RIGA_RAW,
						DSL.function(
								"pgp_sym_encrypt_bytea",
								byte[].class,
								DSL.val(contenutoRigaRaw != null ? contenutoRigaRaw.getBytes(StandardCharsets.UTF_8) : null)
										.cast(SQLDataType.BLOB),
								DSL.val(PGP_KEY)
						)
				)
				.set(DSL.field("cognome", String.class), cognome)
				.set(DSL.field("nome", String.class), nome)
				.set(DSL.field("data_nascita", LocalDate.class), dataNascita)
				.set(DSL.field("sesso", String.class), sesso)
				.set(DSL.field("codice_fiscale", String.class), codiceFiscale)
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.VALIDITA_INIZIO, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.DATA_CREAZIONE, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.DATA_MODIFICA, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.UTENTE_CREAZIONE, utenteCreazione)
				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.UTENTE_MODIFICA, utenteCreazione)
				.returning(REEA_L_FILE_ELABORAZIONE_ERRORE.ELABORAZIONE_ERRORE_ID)
				.fetchOne()
				.getValue(REEA_L_FILE_ELABORAZIONE_ERRORE.ELABORAZIONE_ERRORE_ID, Integer.class);
	}
	
	
	@Override
	public Integer inserisciScarto(DSLContext ctx,
									   Integer fileId,
									   String codiceErrore,
									   String descrizioneErrore,
									   String contenutoRigaRaw,
									   String utenteCreazione,
									   String cognome,
									   String nome,
									   LocalDate dataNascita,
									   String sesso,
									   String codiceFiscale,
									   String targetNomeTabella,
									   Integer targetRecordId) {

		return ctx.insertInto(REEA_L_FILE_SCARICO_ERRORE)
				.set(REEA_L_FILE_SCARICO_ERRORE.FILE_ID, fileId)
//				.set(REEA_L_FILE_ELABORAZIONE_ERRORE.NUMERO_RIGA, numeroRiga)
				.set(REEA_L_FILE_SCARICO_ERRORE.TARGET_TABELLA_NOME, targetNomeTabella)
				.set(REEA_L_FILE_SCARICO_ERRORE.TARGET_RECORD_ID, targetRecordId)
				.set(REEA_L_FILE_SCARICO_ERRORE.CODICE_ERRORE, codiceErrore)
				.set(REEA_L_FILE_SCARICO_ERRORE.DESCRIZIONE_ERRORE, descrizioneErrore)
//				.set(
//						REEA_L_FILE_SCARICO_ERRORE.CONTENUTO_RIGA_RAW,
//						DSL.function(
//								"pgp_sym_encrypt_bytea",
//								byte[].class,
//								DSL.val(contenutoRigaRaw != null ? contenutoRigaRaw.getBytes(StandardCharsets.UTF_8) : null)
//										.cast(SQLDataType.BLOB),
//								DSL.val(PGP_KEY)
//						)
//				)
				.set(DSL.field("cognome", String.class), cognome)
				.set(DSL.field("nome", String.class), nome)
				.set(DSL.field("data_nascita", LocalDate.class), dataNascita)
				.set(DSL.field("sesso", String.class), sesso)
				.set(DSL.field("codice_fiscale", String.class), codiceFiscale)
				.set(REEA_L_FILE_SCARICO_ERRORE.VALIDITA_INIZIO, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_SCARICO_ERRORE.DATA_CREAZIONE, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_SCARICO_ERRORE.DATA_MODIFICA, DSL.currentLocalDateTime())
				.set(REEA_L_FILE_SCARICO_ERRORE.UTENTE_CREAZIONE, utenteCreazione)
				.set(REEA_L_FILE_SCARICO_ERRORE.UTENTE_MODIFICA, utenteCreazione)
				.returning(REEA_L_FILE_SCARICO_ERRORE.FILE_ERRORE_ID)
				.fetchOne()
				.getValue(REEA_L_FILE_SCARICO_ERRORE.FILE_ERRORE_ID, Integer.class);
	}



	public Integer inserisciFileImpatto(
    		DSLContext dsl,
            Integer elaborazioneId,
            String targetTabellaNome,
            Integer targetRecordId,
            Integer operazioneTipoId,
            String utente) {

        return dsl.insertInto(REEA_T_FILE_IMPATTO,
                        REEA_T_FILE_IMPATTO.ELABORAZIONE_ID,
                        REEA_T_FILE_IMPATTO.TARGET_TABELLA_NOME,
                        REEA_T_FILE_IMPATTO.TARGET_RECORD_ID,
                        REEA_T_FILE_IMPATTO.OPERAZIONE_TIPO_ID,
                        REEA_T_FILE_IMPATTO.VALIDITA_INIZIO,
                        REEA_T_FILE_IMPATTO.DATA_CREAZIONE,
                        REEA_T_FILE_IMPATTO.DATA_MODIFICA,
                        REEA_T_FILE_IMPATTO.UTENTE_CREAZIONE,
                        REEA_T_FILE_IMPATTO.UTENTE_MODIFICA)
                .values(
                        elaborazioneId,
                        targetTabellaNome,
                        targetRecordId,
                        operazioneTipoId,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        utente,
                        utente
                )
                .returning(REEA_T_FILE_IMPATTO.FILE_IMPATTO_ID)
                .fetchOne()
                .getFileImpattoId();
    }
	
	@Override
	public TracciaFileDTO recuperaFileByName(DSLContext dsl, String fileName) {
	    if (fileName == null || fileName.isBlank()) {
	        return null;
	    }

	    // Normalizza sempre con estensione .xlsx
	    String normalizedFileName = fileName.endsWith(".xlsx")
	            ? fileName
	            : fileName + ".xlsx";

	    return dsl
	            .select(
	                    REEA_T_FILE.FILE_ID.as("id"),
	                    REEA_T_FILE.FILE_NAME.as("fileName"),
	                    REEA_T_FILE.FILE_PATH.as("baseDir"),
	                    REEA_T_FILE.FILE_HASH_CHECKSUM.as("checksum"),
	                    REEA_T_FILE.FILE_MIME_TYPE.as("contentType"),
	                    REEA_T_FILE.FILE_DIMENSIONE_BYTES.as("fileSize"),
	                    REEA_T_FILE.DATA_CREAZIONE.as("createdAt"),
	                    REEA_T_FILE.DATA_MODIFICA.as("lastUpdatedAt"),
	                    REEA_T_FILE.UTENTE_CREAZIONE.as("createdBy")
	            )
	            .from(REEA_T_FILE)
	            .where(REEA_T_FILE.FILE_NAME.eq(normalizedFileName))
	            .and(REEA_T_FILE.DATA_CANCELLAZIONE.isNull())
	            .orderBy(REEA_T_FILE.DATA_CREAZIONE.desc()) // o FILE_ID.desc()
	            .limit(1)
	            .fetchOneInto(TracciaFileDTO.class);
	}



}
