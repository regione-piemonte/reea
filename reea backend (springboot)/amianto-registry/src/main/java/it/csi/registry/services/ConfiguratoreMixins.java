package it.csi.registry.services;

import com.fasterxml.jackson.annotation.JsonAlias;
import it.csi.registry.client.configuratore.model.Funzionalita;
import java.util.List;

public class ConfiguratoreMixins {

    abstract static class RichiedenteMixin {
        @JsonAlias("codice_fiscale")
        abstract String getCodiceFiscale();
    }

    abstract static class ModelCollocazioneMixin {
        @JsonAlias("codice_collocazione")
        abstract String getCodiceCollocazione();
        @JsonAlias("descrizione_collocazione")
        abstract String getDescrizioneCollocazione();
        @JsonAlias("codice_azienda")
        abstract String getCodiceAzienda();
        @JsonAlias("descrizione_azienda")
        abstract String getDescrizioneAzienda();
    }

    abstract static class FunzionalitaMixin {
        @JsonAlias("codice_funzionalita_padre")
        abstract String getCodiceFunzionalitaPadre();
        @JsonAlias("descrizione_funzionalita_padre")
        abstract String getDescrizioneFunzionalitaPadre();
    }

    abstract static class ModelTokenInformazioneMixin {
        @JsonAlias("funzionalita_abilitate")
        abstract List<Funzionalita> getFunzionalita();
    }
}
