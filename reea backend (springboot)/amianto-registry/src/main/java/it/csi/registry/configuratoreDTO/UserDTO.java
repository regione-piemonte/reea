package it.csi.registry.configuratoreDTO;

import java.util.List;

public class UserDTO {
    private String codiceFiscale;
    private String nome;
    private String cognome;
    private String ruolo;
    private CollocazioneDTO collocazione;
    private List<CollocazioneDTO> collocazioni;
    private List<ProfiloApplicativoDTO> profili;
    private Integer tipoProfiloId;
    private Long utenteId;

    public Long getUtenteId() { return utenteId; }
    public void setUtenteId(Long utenteId) { this.utenteId = utenteId; }
    public Integer getTipoProfiloId() {
        return tipoProfiloId;
    }

    public void setTipoProfiloId(Integer tipoProfiloId) {
        this.tipoProfiloId = tipoProfiloId;
    }

    public List<CollocazioneDTO> getCollocazioni() {
        return collocazioni;
    }
    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

    public CollocazioneDTO getCollocazione() {
        return collocazione;
    }

    public void setCollocazione(CollocazioneDTO collocazione) {
        this.collocazione = collocazione;
    }

    public List<ProfiloApplicativoDTO> getProfili() {
        return profili;
    }

    public void setProfili(List<ProfiloApplicativoDTO> profili) {
        this.profili = profili;
    }

    public void setCollocazioni(List<CollocazioneDTO> collocazioni) {
        this.collocazioni = collocazioni;
    }

}
