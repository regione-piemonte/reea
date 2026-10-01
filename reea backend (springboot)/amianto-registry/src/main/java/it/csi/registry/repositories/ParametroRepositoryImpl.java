package it.csi.registry.repositories;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import it.csi.registry.model.ParametroDTO;
import static it.csi.registry.jooq.tables.ReeaCParametro.REEA_C_PARAMETRO;

@Repository
public class ParametroRepositoryImpl implements ParametroRepository {

    private final DSLContext dsl;

    public ParametroRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public String getValoreByCod(String cod) {
        return dsl.select(REEA_C_PARAMETRO.PARAMETRO_VALORE)
                  .from(REEA_C_PARAMETRO)
                  .where(REEA_C_PARAMETRO.PARAMETRO_COD.eq(cod))
                  .fetchOne(REEA_C_PARAMETRO.PARAMETRO_VALORE);
    }

    @Override
    public List<ParametroDTO> findAll() {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return dsl.select(
                    REEA_C_PARAMETRO.PARAMETRO_ID,
                    REEA_C_PARAMETRO.PARAMETRO_COD,
                    REEA_C_PARAMETRO.PARAMETRO_DESC,
                    REEA_C_PARAMETRO.PARAMETRO_VALORE,
                    REEA_C_PARAMETRO.PARAMETRO_TIPO_ID,
                    REEA_C_PARAMETRO.VALIDITA_INIZIO,
                    REEA_C_PARAMETRO.VALIDITA_FINE,
                    REEA_C_PARAMETRO.DATA_CREAZIONE,
                    REEA_C_PARAMETRO.DATA_MODIFICA,
                    REEA_C_PARAMETRO.DATA_CANCELLAZIONE,
                    REEA_C_PARAMETRO.UTENTE_CREAZIONE,
                    REEA_C_PARAMETRO.UTENTE_MODIFICA,
                    REEA_C_PARAMETRO.UTENTE_CANCELLAZIONE
               )
               .from(REEA_C_PARAMETRO)
               .fetch()
               .stream()
               .map(r -> {
                   ParametroDTO row = new ParametroDTO();
                   row.setParametroId(Long.valueOf(r.get(REEA_C_PARAMETRO.PARAMETRO_ID)));
                   row.setParametroCod(r.get(REEA_C_PARAMETRO.PARAMETRO_COD));
                   row.setParametroDesc(r.get(REEA_C_PARAMETRO.PARAMETRO_DESC));
                   row.setParametroValore(r.get(REEA_C_PARAMETRO.PARAMETRO_VALORE));
                   row.setParametroTipoId(r.get(REEA_C_PARAMETRO.PARAMETRO_TIPO_ID));
                   row.setValiditaInizio(
                       r.get(REEA_C_PARAMETRO.VALIDITA_INIZIO) != null
                           ? r.get(REEA_C_PARAMETRO.VALIDITA_INIZIO).format(fmt)
                           : null);
                   row.setValiditaFine(
                       r.get(REEA_C_PARAMETRO.VALIDITA_FINE) != null
                           ? r.get(REEA_C_PARAMETRO.VALIDITA_FINE).format(fmt)
                           : null);
                   row.setDataCreazione(
                       r.get(REEA_C_PARAMETRO.DATA_CREAZIONE) != null
                           ? r.get(REEA_C_PARAMETRO.DATA_CREAZIONE).format(fmt)
                           : null);
                   row.setDataModifica(
                       r.get(REEA_C_PARAMETRO.DATA_MODIFICA) != null
                           ? r.get(REEA_C_PARAMETRO.DATA_MODIFICA).format(fmt)
                           : null);
                   row.setDataCancellazione(
                       r.get(REEA_C_PARAMETRO.DATA_CANCELLAZIONE) != null
                           ? r.get(REEA_C_PARAMETRO.DATA_CANCELLAZIONE).format(fmt)
                           : null);
                   row.setUtenteCreazione(r.get(REEA_C_PARAMETRO.UTENTE_CREAZIONE));
                   row.setUtenteModifica(r.get(REEA_C_PARAMETRO.UTENTE_MODIFICA));
                   row.setUtenteCancellazione(r.get(REEA_C_PARAMETRO.UTENTE_CANCELLAZIONE));
                   return row;
               })
               .collect(Collectors.toList());
    }
}

