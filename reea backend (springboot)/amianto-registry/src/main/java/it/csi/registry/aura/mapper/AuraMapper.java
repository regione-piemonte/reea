package it.csi.registry.aura.mapper;

import it.csi.registry.aura.dto.AuraAssistitoDto;
import it.csi.registry.model.EsenzioneDTO;
import it.csi.registry.soap.AnagrafeSanitaria.*;
import it.csi.registry.soap.AnagrafeFind.DatiAnagrafici;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

public class AuraMapper {

    private AuraMapper() {}

    // =====================================================
    // =================== GET BY ID AURA ==================
    // =====================================================

    public static AuraAssistitoDto toDto(SoggettoAuraMsg msg) {

        if (msg == null || msg.getBody() == null) {
            return null;
        }

        SoggettoAuraBody body = msg.getBody();
        System.out.println("=== METODI SOGGETTO AURA BODY ===");
        for (java.lang.reflect.Method m : body.getClass().getMethods()) {
            System.out.println(m.getName());
        }
        AuraAssistitoDto dto = new AuraAssistitoDto();

        // ================= ID AURA =================
        dto.setIdAura(bigDecimalToString(body.getIdAura()));

        // ================= INFO ANAGRAFICHE =================
        InfoAnagrafiche infoAnag = body.getInfoAnag();
        if (infoAnag != null) {

            // ---- DATI PRIMARI ----
            DatiPrimari primari = infoAnag.getDatiPrimari();
            if (primari != null) {

                dto.setCodiceFiscale(primari.getCodiceFiscale());
                dto.setCognome(primari.getCognome());
                dto.setNome(primari.getNome());
                dto.setSesso(primari.getSesso());

                dto.setDataNascita(toLocalDate(primari.getDataNascita()));
                dto.setDataDecesso(toLocalDate(primari.getDataDecesso()));

                dto.setStatoNascita(primari.getCodStatoNascita());
                dto.setComuneNascitaCod(primari.getCodComuneNascita());
                dto.setComuneNascitaDesc(primari.getDescComuneNascita());
                dto.setProvinciaNascitaCod(primari.getSiglaProvNascita());
                dto.setProvinciaNascitaDesc(null);
            }

            // ---- RESIDENZA ----
            DatiSecondari residenza = infoAnag.getResidenza();
            if (residenza != null) {

                dto.setStatoResidenza(residenza.getCodStato());
                dto.setComuneResidenzaCod(residenza.getCodComune());
                dto.setComuneResidenzaDesc(residenza.getDescComune());

                dto.setProvinciaResidenzaCod(null);
                dto.setProvinciaResidenzaDesc(null);

                dto.setIndirizzoResidenza(residenza.getIndirizzo());
                dto.setCivicoResidenza(residenza.getNumCivico());
                dto.setCapResidenza(residenza.getCap());
            }
            DatiSecondari domicilio = infoAnag.getDomicilio();
            if (domicilio != null) {
                dto.setStatoDomicilio(domicilio.getCodStato());
                dto.setComuneDomicilioCod(domicilio.getCodComune());
                dto.setComuneDomicilioDesc(domicilio.getDescComune());
                dto.setProvinciaDomicilioCod(null); // sigla provincia non disponibile nel WSDL
                dto.setProvinciaDomicilioDesc(null);
                dto.setIndirizzoDomicilio(domicilio.getIndirizzo());
                dto.setCivicoDomicilio(domicilio.getNumCivico());
                dto.setCapDomicilio(domicilio.getCap());
            }



            var altreInfo = body.getAltreInfo(); // verifica il tipo esatto generato dal WSDL
            if (altreInfo != null && altreInfo.getInformazioni() != null) {
                List<String> phones = altreInfo.getInformazioni().stream()
                        .filter(i -> i != null && i.getValInformazione() != null
                                && !i.getValInformazione().isBlank())
                        .filter(i -> "CELLULARE".equalsIgnoreCase(i.getDescInformazione())
                                || "TELEFONO FISSO".equalsIgnoreCase(i.getDescInformazione()))
                        .map(i -> i.getValInformazione())
                        .collect(Collectors.toList());

                List<String> emails = altreInfo.getInformazioni().stream()
                        .filter(i -> i != null && i.getValInformazione() != null
                                && !i.getValInformazione().isBlank())
                        .filter(i -> "INDIRIZZO POSTA ELETTRONICA".equalsIgnoreCase(i.getDescInformazione())
                                || "PEC".equalsIgnoreCase(i.getDescInformazione()))
                        .map(i -> i.getValInformazione())
                        .collect(Collectors.toList());


//                List<String> phones = altreInfo.getInformazioni().stream()
//                        .filter(i -> i != null && i.getValInformazione() != null
//                                && !i.getValInformazione().isBlank())
//                        .filter(i -> "CELLULARE".equalsIgnoreCase(i.getDescInformazione())
//                                || "TELEFONO FISSO".equalsIgnoreCase(i.getDescInformazione()))
//                        .map(i -> i.getValInformazione())
//                        .collect(Collectors.toList());
//
//                List<String> emails = altreInfo.getInformazioni().stream()
//                        .filter(i -> i != null && i.getValInformazione() != null
//                                && !i.getValInformazione().isBlank())
//                        .filter(i -> "EMAIL".equalsIgnoreCase(i.getDescInformazione())
//                                || "PEC".equalsIgnoreCase(i.getDescInformazione())
//                                || "INDIRIZZO POSTA ELETTRONICA".equalsIgnoreCase(i.getDescInformazione()))
//                        .map(i -> i.getValInformazione())
//                        .collect(Collectors.toList());

                if (!phones.isEmpty()) dto.setTelefoni(phones);
                if (!emails.isEmpty()) dto.setEmail(emails);
            }

        }

        // ================= INFO SANITARIE =================
        InfoSanitarie san = body.getInfoSan();
        if (san != null) {

            dto.setAslAssistenzaCod(san.getAslAssistenza());
            dto.setAslAssistenzaDesc(null);

            dto.setDataFineAsl(toLocalDate(san.getDataFineASL()));
        }

        // ================= ESENZIONI =================          ← aggiungi qui
        ArrayOfinfoesenzioneInfoEsenzione infoEsenzioni = body.getInfoEsenzioni();
        if (infoEsenzioni != null && infoEsenzioni.getInfoesenzione() != null) {
            List<EsenzioneDTO> esenzioni = infoEsenzioni.getInfoesenzione().stream()
                    .filter(Objects::nonNull)
                    .map(e -> new EsenzioneDTO()
                            .esenzioneCod(e.getCodEsenzione())
                            .esenzioneDesc(e.getDescEsenzione())
                            .diagnosiCod(e.getCodDiagnosi())
                            .esenzioneDataEmissione(toLocalDate(e.getDataEmissione()))
                            .esenzioneDataScadenza(toLocalDate(e.getDataScadenza()))
                            .validitaInizio(e.getInizioValEsenzione())
                            .validitaFine(e.getFineValEsenzione()))
                    .collect(Collectors.toList());
            dto.setEsenzioni(esenzioni);
        }


        return dto;
    }

    // =====================================================
    // =================== FIND BY CF ======================
    // =====================================================

    public static AuraAssistitoDto toDto(DatiAnagrafici dato) {

        if (dato == null) {
            return null;
        }

        AuraAssistitoDto dto = new AuraAssistitoDto();

        dto.setIdAura(
                dato.getIdProfiloAnagrafico() != null
                        ? dato.getIdProfiloAnagrafico().toString()
                        : null
        );

        dto.setCodiceFiscale(dato.getCodiceFiscale());
        dto.setCognome(dato.getCognome());
        dto.setNome(dato.getNome());
        dto.setSesso(dato.getSesso());

        // Nel FIND normalmente la data è XMLGregorianCalendar
        dto.setDataNascita(toLocalDate(dato.getDataNascita()));

        // I seguenti campi nel FIND non sempre sono presenti
        dto.setDataDecesso(null);
        dto.setStatoNascita(null);
        dto.setComuneNascitaCod(null);
        dto.setComuneNascitaDesc(null);
        dto.setProvinciaNascitaCod(null);
        dto.setProvinciaNascitaDesc(null);

        dto.setStatoResidenza(null);
        dto.setComuneResidenzaCod(null);
        dto.setComuneResidenzaDesc(null);
        dto.setProvinciaResidenzaCod(null);
        dto.setProvinciaResidenzaDesc(null);
        dto.setIndirizzoResidenza(null);
        dto.setCivicoResidenza(null);
        dto.setCapResidenza(null);

        dto.setAslAssistenzaCod(null);
        dto.setAslAssistenzaDesc(null);
        dto.setDataFineAsl(null);

        return dto;
    }

    // =====================================================
    // ====================== UTILITY ======================
    // =====================================================

    private static String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toPlainString() : null;
    }

    private static LocalDate toLocalDate(XMLGregorianCalendar xmlDate) {
        if (xmlDate == null) {
            return null;
        }
        return xmlDate.toGregorianCalendar()
                .toZonedDateTime()
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDate();
    }
}