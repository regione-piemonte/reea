package it.csi.registry.repositories;

import static it.csi.registry.jooq.tables.ReeaTRegistro.REEA_T_REGISTRO;
import static it.csi.registry.jooq.tables.ReeaTNota.REEA_T_NOTA;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.val;
import static org.jooq.impl.DSL.coalesce;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.AuditLogRequest;
import it.csi.registry.model.NotaDTO;

@Repository
public class NoteRepositoryImpl implements NoteRepository {

    private final DSLContext dsl;

    public NoteRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Ricava il registro_id dal soggetto_id tramite hmac
    private Integer getRegistroId(Long soggettoId) {
        return dsl.select(REEA_T_REGISTRO.REGISTRO_ID)
                .from(REEA_T_REGISTRO)
                .where(REEA_T_REGISTRO.SOGGETTO_ID_HMAC.eq(
                        field("reea.hmac_soggetto_id({0}, {1})", String.class,
                                val(soggettoId.intValue()), val("16<odcc8!"))))
                .fetchOneInto(Integer.class);
    }

    @Override
    public List<NotaDTO> findBySoggettoId(Long soggettoId) {
        Integer registroId = getRegistroId(soggettoId);
        if (registroId == null) return List.of();

        return dsl.select(
                        REEA_T_NOTA.NOTA_ID,
                        REEA_T_NOTA.REGISTRO_ID,
                        REEA_T_NOTA.DESCRIZIONE,
                        coalesce(REEA_T_NOTA.DATA_MODIFICA, REEA_T_NOTA.DATA_CREAZIONE).as("data_modifica"),
                        coalesce(REEA_T_NOTA.UTENTE_MODIFICA, REEA_T_NOTA.UTENTE_CREAZIONE).as("utente_modifica")
                )
                .from(REEA_T_NOTA)
                .where(REEA_T_NOTA.REGISTRO_ID.eq(registroId))
                .and(REEA_T_NOTA.DATA_CANCELLAZIONE.isNull())
                .orderBy(REEA_T_NOTA.DATA_CREAZIONE.desc())
                .fetch(r -> {
                    NotaDTO dto = new NotaDTO();
                    dto.setNotaId(r.get(REEA_T_NOTA.NOTA_ID).longValue());
                    dto.setRegistroId(r.get(REEA_T_NOTA.REGISTRO_ID));
                    dto.setDescrizione(r.get(REEA_T_NOTA.DESCRIZIONE));
                    LocalDateTime data = r.get("data_modifica", LocalDateTime.class);
                    dto.setDataModifica(data != null ? data.format(FMT) : null);
                    dto.setUtenteModifica(r.get("utente_modifica", String.class));
                    return dto;
                });
    }

    @Override
    public NotaDTO inserisci(Long soggettoId, String descrizione, AuditLogRequest auditLogRequest) {
        Integer registroId = getRegistroId(soggettoId);
        if (registroId == null) throw new IllegalStateException("Registro non trovato per soggetto_id: " + soggettoId);

        return dsl.insertInto(REEA_T_NOTA)
                .set(REEA_T_NOTA.REGISTRO_ID, registroId)
                .set(REEA_T_NOTA.DESCRIZIONE, descrizione)
                .set(REEA_T_NOTA.DATA_CREAZIONE, DSL.currentLocalDateTime())
                .set(REEA_T_NOTA.VALIDITA_INIZIO, DSL.currentLocalDateTime())
                .set(REEA_T_NOTA.UTENTE_CREAZIONE, auditLogRequest.getUtente())
                .returning(
                        REEA_T_NOTA.NOTA_ID,
                        REEA_T_NOTA.REGISTRO_ID,
                        REEA_T_NOTA.DESCRIZIONE,
                        REEA_T_NOTA.DATA_CREAZIONE,
                        REEA_T_NOTA.UTENTE_CREAZIONE
                )
                .fetchOne(r -> {
                    NotaDTO dto = new NotaDTO();
                    dto.setNotaId(r.get(REEA_T_NOTA.NOTA_ID).longValue());
                    dto.setRegistroId(r.get(REEA_T_NOTA.REGISTRO_ID));
                    dto.setDescrizione(r.get(REEA_T_NOTA.DESCRIZIONE));
                    LocalDateTime data = r.get(REEA_T_NOTA.DATA_CREAZIONE);
                    dto.setDataModifica(data != null ? data.format(FMT) : null);
                    dto.setUtenteModifica(r.get(REEA_T_NOTA.UTENTE_CREAZIONE));
                    return dto;
                });
    }

    @Override
    public NotaDTO modifica(Long notaId, String descrizione, AuditLogRequest auditLogRequest) {
        return dsl.update(REEA_T_NOTA)
                .set(REEA_T_NOTA.DESCRIZIONE, descrizione)
                .set(REEA_T_NOTA.DATA_MODIFICA, DSL.currentLocalDateTime())
                .set(REEA_T_NOTA.UTENTE_MODIFICA, auditLogRequest.getUtente())
                .where(REEA_T_NOTA.NOTA_ID.eq(notaId.intValue()))
                .returning(
                        REEA_T_NOTA.NOTA_ID,
                        REEA_T_NOTA.REGISTRO_ID,
                        REEA_T_NOTA.DESCRIZIONE,
                        REEA_T_NOTA.DATA_MODIFICA,
                        REEA_T_NOTA.UTENTE_MODIFICA
                )
                .fetchOne(r -> {
                    NotaDTO dto = new NotaDTO();
                    dto.setNotaId(r.get(REEA_T_NOTA.NOTA_ID).longValue());
                    dto.setRegistroId(r.get(REEA_T_NOTA.REGISTRO_ID));
                    dto.setDescrizione(r.get(REEA_T_NOTA.DESCRIZIONE));
                    LocalDateTime data = r.get(REEA_T_NOTA.DATA_MODIFICA);
                    dto.setDataModifica(data != null ? data.format(FMT) : null);
                    dto.setUtenteModifica(r.get(REEA_T_NOTA.UTENTE_MODIFICA));
                    return dto;
                });
    }

    @Override
    public void elimina(Long notaId) {
        dsl.deleteFrom(REEA_T_NOTA)
                .where(REEA_T_NOTA.NOTA_ID.eq(notaId.intValue()))
                .execute();
    }
}
