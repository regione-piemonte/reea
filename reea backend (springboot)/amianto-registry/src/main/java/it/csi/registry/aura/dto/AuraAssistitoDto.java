package it.csi.registry.aura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import it.csi.registry.model.EsenzioneDTO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Schema(
        name = "AuraAssistitoDto",
        description = "Rappresenta i dati anagrafici dell'assistito provenienti da AURA"
)
public class AuraAssistitoDto {

    private String idAura;
    private String codiceFiscale;
    private String cognome;
    private String nome;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate dataNascita;

    private String sesso;

    // Nascita
    private String statoNascita;
    private String provinciaNascitaCod;
    private String provinciaNascitaDesc;
    private String comuneNascitaCod;
    private String comuneNascitaDesc;

    // Residenza
    private String statoResidenza;
    private String provinciaResidenzaCod;
    private String provinciaResidenzaDesc;
    private String comuneResidenzaCod;
    private String comuneResidenzaDesc;
    private String indirizzoResidenza;
    private String civicoResidenza;
    private String capResidenza;

    // Domicilio
    private String statoDomicilio;
    private String provinciaDomicilioCod;
    private String provinciaDomicilioDesc;
    private String comuneDomicilioCod;
    private String comuneDomicilioDesc;
    private String indirizzoDomicilio;
    private String civicoDomicilio;
    private String capDomicilio;

    // Contatti
    private List<String> telefoni;
    private List<String> email;

    // ASL
    private String aslAssistenzaCod;
    private String aslAssistenzaDesc;

    private List<EsenzioneDTO> esenzioni;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate dataDecesso;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate dataFineAsl;

    // ================= GETTER / SETTER =================

    @Schema(name = "id_aura", requiredMode = RequiredMode.NOT_REQUIRED)
    @JsonProperty("id_aura")
    public String getIdAura() {
        return idAura;
    }

    public void setIdAura(String idAura) {
        this.idAura = idAura;
    }

    @Schema(name = "codice_fiscale")
    @JsonProperty("codice_fiscale")
    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    @Schema(name = "cognome")
    @JsonProperty("cognome")
    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    @Schema(name = "nome")
    @JsonProperty("nome")
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @Schema(name = "data_nascita")
    @JsonProperty("data_nascita")
    public @Valid LocalDate getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(LocalDate dataNascita) {
        this.dataNascita = dataNascita;
    }

    @Schema(name = "sesso")
    @JsonProperty("sesso")
    public String getSesso() {
        return sesso;
    }

    public void setSesso(String sesso) {
        this.sesso = sesso;
    }

    @Schema(name = "stato_nascita")
    @JsonProperty("stato_nascita")
    public String getStatoNascita() {
        return statoNascita;
    }

    public void setStatoNascita(String statoNascita) {
        this.statoNascita = statoNascita;
    }

    @Schema(name = "provincia_nascita_cod")
    @JsonProperty("provincia_nascita_cod")
    public String getProvinciaNascitaCod() {
        return provinciaNascitaCod;
    }

    public void setProvinciaNascitaCod(String provinciaNascitaCod) {
        this.provinciaNascitaCod = provinciaNascitaCod;
    }

    @Schema(name = "provincia_nascita_desc")
    @JsonProperty("provincia_nascita_desc")
    public String getProvinciaNascitaDesc() {
        return provinciaNascitaDesc;
    }

    public void setProvinciaNascitaDesc(String provinciaNascitaDesc) {
        this.provinciaNascitaDesc = provinciaNascitaDesc;
    }

    @Schema(name = "comune_nascita_cod")
    @JsonProperty("comune_nascita_cod")
    public String getComuneNascitaCod() {
        return comuneNascitaCod;
    }

    public void setComuneNascitaCod(String comuneNascitaCod) {
        this.comuneNascitaCod = comuneNascitaCod;
    }

    @Schema(name = "comune_nascita_desc")
    @JsonProperty("comune_nascita_desc")
    public String getComuneNascitaDesc() {
        return comuneNascitaDesc;
    }

    public void setComuneNascitaDesc(String comuneNascitaDesc) {
        this.comuneNascitaDesc = comuneNascitaDesc;
    }

    // Residenza

    @Schema(name = "provincia_residenza_cod")
    @JsonProperty("provincia_residenza_cod")
    public String getProvinciaResidenzaCod() {
        return provinciaResidenzaCod;
    }

    public void setProvinciaResidenzaCod(String provinciaResidenzaCod) {
        this.provinciaResidenzaCod = provinciaResidenzaCod;
    }

    @Schema(name = "provincia_residenza_desc")
    @JsonProperty("provincia_residenza_desc")
    public String getProvinciaResidenzaDesc() {
        return provinciaResidenzaDesc;
    }

    public void setProvinciaResidenzaDesc(String provinciaResidenzaDesc) {
        this.provinciaResidenzaDesc = provinciaResidenzaDesc;
    }

    @Schema(name = "comune_residenza_cod")
    @JsonProperty("comune_residenza_cod")
    public String getComuneResidenzaCod() {
        return comuneResidenzaCod;
    }

    public void setComuneResidenzaCod(String comuneResidenzaCod) {
        this.comuneResidenzaCod = comuneResidenzaCod;
    }

    @Schema(name = "comune_residenza_desc")
    @JsonProperty("comune_residenza_desc")
    public String getComuneResidenzaDesc() {
        return comuneResidenzaDesc;
    }

    public void setComuneResidenzaDesc(String comuneResidenzaDesc) {
        this.comuneResidenzaDesc = comuneResidenzaDesc;
    }

    @Schema(name = "indirizzo_residenza")
    @JsonProperty("indirizzo_residenza")
    public String getIndirizzoResidenza() {
        return indirizzoResidenza;
    }

    public void setIndirizzoResidenza(String indirizzoResidenza) {
        this.indirizzoResidenza = indirizzoResidenza;
    }

    @Schema(name = "civico_residenza")
    @JsonProperty("civico_residenza")
    public String getCivicoResidenza() {
        return civicoResidenza;
    }

    public void setCivicoResidenza(String civicoResidenza) {
        this.civicoResidenza = civicoResidenza;
    }

    @Schema(name = "cap_residenza")
    @JsonProperty("cap_residenza")
    public String getCapResidenza() {
        return capResidenza;
    }

    public void setCapResidenza(String capResidenza) {
        this.capResidenza = capResidenza;
    }

    @Schema(name = "asl_assistenza_cod")
    @JsonProperty("asl_assistenza_cod")
    public String getAslAssistenzaCod() {
        return aslAssistenzaCod;
    }

    public void setAslAssistenzaCod(String aslAssistenzaCod) {
        this.aslAssistenzaCod = aslAssistenzaCod;
    }

    @Schema(name = "asl_assistenza_desc")
    @JsonProperty("asl_assistenza_desc")
    public String getAslAssistenzaDesc() {
        return aslAssistenzaDesc;
    }

    public void setAslAssistenzaDesc(String aslAssistenzaDesc) {
        this.aslAssistenzaDesc = aslAssistenzaDesc;
    }

    @Schema(name = "data_decesso")
    @JsonProperty("data_decesso")
    public LocalDate getDataDecesso() {
        return dataDecesso;
    }

    public void setDataDecesso(LocalDate dataDecesso) {
        this.dataDecesso = dataDecesso;
    }

    @Schema(name = "data_fine_asl")
    @JsonProperty("data_fine_asl")
    public LocalDate getDataFineAsl() {
        return dataFineAsl;
    }

    public void setDataFineAsl(LocalDate dataFineAsl) {
        this.dataFineAsl = dataFineAsl;
    }

    @Schema(name = "stato_domicilio") @JsonProperty("stato_domicilio")
    public String getStatoDomicilio() { return statoDomicilio; }
    public void setStatoDomicilio(String statoDomicilio) { this.statoDomicilio = statoDomicilio; }

    @Schema(name = "provincia_domicilio_cod") @JsonProperty("provincia_domicilio_cod")
    public String getProvinciaDomicilioCod() { return provinciaDomicilioCod; }
    public void setProvinciaDomicilioCod(String v) { this.provinciaDomicilioCod = v; }

    @Schema(name = "provincia_domicilio_desc") @JsonProperty("provincia_domicilio_desc")
    public String getProvinciaDomicilioDesc() { return provinciaDomicilioDesc; }
    public void setProvinciaDomicilioDesc(String v) { this.provinciaDomicilioDesc = v; }

    @Schema(name = "comune_domicilio_cod") @JsonProperty("comune_domicilio_cod")
    public String getComuneDomicilioCod() { return comuneDomicilioCod; }
    public void setComuneDomicilioCod(String v) { this.comuneDomicilioCod = v; }

    @Schema(name = "comune_domicilio_desc") @JsonProperty("comune_domicilio_desc")
    public String getComuneDomicilioDesc() { return comuneDomicilioDesc; }
    public void setComuneDomicilioDesc(String v) { this.comuneDomicilioDesc = v; }

    @Schema(name = "indirizzo_domicilio") @JsonProperty("indirizzo_domicilio")
    public String getIndirizzoDomicilio() { return indirizzoDomicilio; }
    public void setIndirizzoDomicilio(String v) { this.indirizzoDomicilio = v; }

    @Schema(name = "civico_domicilio") @JsonProperty("civico_domicilio")
    public String getCivicoDomicilio() { return civicoDomicilio; }
    public void setCivicoDomicilio(String v) { this.civicoDomicilio = v; }

    @Schema(name = "cap_domicilio") @JsonProperty("cap_domicilio")
    public String getCapDomicilio() { return capDomicilio; }
    public void setCapDomicilio(String v) { this.capDomicilio = v; }

    @Schema(name = "telefoni") @JsonProperty("telefoni")
    public List<String> getTelefoni() { return telefoni; }
    public void setTelefoni(List<String> telefoni) { this.telefoni = telefoni; }

    @Schema(name = "email") @JsonProperty("email")
    public List<String> getEmail() { return email; }
    public void setEmail(List<String> email) { this.email = email; }
    // equals, hashCode, toString

    @Schema(name = "esenzioni")
    @JsonProperty("esenzioni")
    public List<EsenzioneDTO> getEsenzioni() { return esenzioni; }
    public void setEsenzioni(List<EsenzioneDTO> esenzioni) { this.esenzioni = esenzioni; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuraAssistitoDto that)) return false;
        return Objects.equals(idAura, that.idAura);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAura);
    }

    @Override
    public String toString() {
        return "AuraAssistitoDto{" +
                "idAura='" + idAura + '\'' +
                ", codiceFiscale='" + codiceFiscale + '\'' +
                '}';
    }

    @Schema(name = "stato_residenza")
    @JsonProperty("stato_residenza")
    public String getStatoResidenza() {
        return statoResidenza;
    }

    public void setStatoResidenza(String statoResidenza) {
        this.statoResidenza = statoResidenza;
    }
}