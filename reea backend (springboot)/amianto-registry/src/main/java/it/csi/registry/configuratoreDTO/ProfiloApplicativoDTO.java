package it.csi.registry.configuratoreDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfiloApplicativoDTO {
    private String codice;
    private String descrizione;
    private List<String> funzionalita = new ArrayList<>();
    private List<CollocazioneDTO> collocazioni = new ArrayList<>();

    public String getCodice() { return codice; }
    public void setCodice(String codice) { this.codice = codice; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public List<String> getFunzionalita() { return funzionalita; }
    public void setFunzionalita(List<String> funzionalita) { this.funzionalita = funzionalita; }

    public List<CollocazioneDTO> getCollocazioni() { return collocazioni; }
    public void setCollocazioni(List<CollocazioneDTO> collocazioni) { this.collocazioni = collocazioni; }
}

