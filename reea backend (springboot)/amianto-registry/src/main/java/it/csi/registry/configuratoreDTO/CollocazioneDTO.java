package it.csi.registry.configuratoreDTO;

public class CollocazioneDTO {
    private String codice;
    private String descrizione;
    private String codiceAzienda;
    private String descrizioneAzienda;

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }

    public String getDescrizioneAzienda() {
        return descrizioneAzienda;
    }

    public void setDescrizioneAzienda(String descrizioneAzienda) {
        this.descrizioneAzienda = descrizioneAzienda;
    }
}
