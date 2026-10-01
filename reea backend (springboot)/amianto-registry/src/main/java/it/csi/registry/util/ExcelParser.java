package it.csi.registry.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import it.csi.registry.model.AdesioneDTO;
import it.csi.registry.model.InailDTO;
import it.csi.registry.model.SpresalDTO;
import it.csi.registry.model.SpresalEsitiDTO;

public class ExcelParser {

    private ExcelParser() {}

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ITALIAN),  // 03-mag-2024
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ITALIAN),   // 3-mag-2024
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );
    
    
    public static List<InailDTO> parseInail(InputStream is) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<InailDTO> rows = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;

                // Salta righe vuote
                if (getString(r, 0) == null && getString(r, 2) == null) continue;

                InailDTO dto = new InailDTO();
                dto.setDomanda(getInteger(r, 0));            // A = Domanda
                dto.setCognome(getString(r, 1));        // B = Cognome
                dto.setNome(getString(r, 2));          // C = Nome
                dto.setCodiceFiscale(getString(r, 3));         // D = Codice Fiscale
                dto.setSesso(getString(r, 4));            // E = Sesso
                dto.setDataNascita(getLocalDate(r, 5));         // F = Data di nascita
                dto.setIndirizzoResidenza(getString(r, 6));   // G = Indirizzo
                dto.setIstatResidenza(getInteger(r, 7));      // H = Istat
                dto.setCapResidenza(getInteger(r, 8));     // I = Cap
                dto.setRegioneResidenza(getString(r, 9));          // J = Regione
                dto.setProvinciaResidenza(getString(r, 10)); // K = Provincia di residenza
                dto.setComuneResidenza(getString(r, 11));   // L = Comune di residenza

                rows.add(dto);
            }
            return rows;
        }
    }
    
    
    public static List<SpresalDTO> parseSpresal(InputStream is) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<SpresalDTO> rows = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;

                // Salta righe vuote
                if (getString(r, 0) == null && getString(r, 2) == null) continue;

                SpresalDTO dto = new SpresalDTO();
                dto.setCodiceFiscale(getString(r, 0));
                dto.setIdAura(getInteger(r, 1));
                dto.setDataIntervista(getLocalDate(r, 2));
                dto.setNominativoIntervistatore(getString(r, 3));
                dto.setFumatore(getBoolean(r, 4));
                dto.setSigarette(getBoolean(r, 5));
                dto.setSigaretteAnni(getBigDecimal(r, 6));
                dto.setSigaretteEtaInizio(getBigDecimal(r, 7));
                dto.setSigaretteFumaAttualmente(getBoolean(r, 8));
                dto.setSigaretteEtaFine(getBigDecimal(r, 9)); 
                dto.setSigaretteDie(getBigDecimal(r, 10));
                dto.setSigari(getBoolean(r, 11));
                dto.setSigariAnni(getBigDecimal(r, 12));
                dto.setSigariEtaInizio(getBigDecimal(r, 13));
                dto.setSigariFumaAttualmente(getBoolean(r, 14));
                dto.setSigariEtaFine(getBigDecimal(r, 15));
                dto.setSigariDie(getBigDecimal(r, 16));
                dto.setPipa(getBoolean(r, 17));
                dto.setPipaAnni(getBigDecimal(r, 18));
                dto.setPipaEtaInizio(getBigDecimal(r, 19));
                dto.setPipaFumaAttualmente(getBoolean(r, 20));
                dto.setPipaEtaFine(getBigDecimal(r, 21));
                dto.setPipaDie(getBigDecimal(r, 22));
                dto.setOccupazioneNum(getString(r, 23));
                dto.setOccupazioneAnnoInizio(getString(r, 24));
                dto.setOccupazioneAnnoFine(getString(r, 25));
                dto.setOccupazioneTipo(getString(r, 26));
                dto.setOccupazioneDescrizioneLavoro(getString(r, 27));
                dto.setOccupazioneNomeEIndirizzoDitta(getString(r, 28));
                dto.setOccupazioneAttivitaDitta(getString(r, 29));
                dto.setNotaAttivitaConAmianto(getString(r, 30));
                dto.setAnamnesiEsposizioneAmianto(getBoolean(r, 31));
                dto.setEsposizioneProfessionale(getString(r, 32));
                dto.setAnnoFineEsposizione(getInteger(r, 33));
                dto.setLivelloEsposizione(getString(r, 34));
                dto.setInserimentoInSorveglianza(getBoolean(r, 35));
                dto.setIdSpresal(getInteger(r, 36));
                dto.setCounseling(getString(r, 37));

                rows.add(dto);
            }
            return rows;
        }
    }
    
    
    public static List<SpresalEsitiDTO> parseSpresalEsiti(InputStream is) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<SpresalEsitiDTO> rows = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;

                // Salta righe vuote (Codice_Fiscale e Id_Aura entrambi null)
                if (getString(r, 0) == null && getInteger(r, 1) == null) continue;

                SpresalEsitiDTO dto = new SpresalEsitiDTO();

                dto.setCodiceFiscale(getString(r, 0));
                dto.setIdAura(getInteger(r, 1));
                dto.setDataVisita(getLocalDate(r, 2));
                dto.setVisita(getString(r, 3));
                dto.setLivelloVisita(getString(r, 4));
                dto.setRiceveIndennizzo(getBoolean(r, 5));
                dto.setMalattiaIndennizzo(getString(r, 6));

                dto.setAccertamentiRx(getBoolean(r, 7));
                dto.setAccertamentiRxData(getLocalDate(r, 8));
                dto.setAccertamentiRxRefertoNormale(StringUtil.normalizzaVariabileSpresalEsiti(getString(r, 9)));
                dto.setAccertamentiRxAcquisita(getString(r, 10));

                dto.setAccertamentiTc(getBoolean(r, 11));
                dto.setAccertamentiTcData(getLocalDate(r, 12));
                dto.setAccertamentiTcRefertoNormale(StringUtil.normalizzaVariabileSpresalEsiti(getString(r, 13)));
                dto.setAccertamentiTcAcquisita(getString(r, 14));

                dto.setAccertamentiSpirometriaSemplice(getBoolean(r, 15));
                dto.setAccertamentiSpirometriaSempliceData(getLocalDate(r, 16));
                dto.setAccertamentiSpirometriaSempliceRefertoNorm(StringUtil.normalizzaVariabileSpresalEsiti(getString(r, 17)));
                dto.setAccertamentiSpirometriaSempliceAcquisita(getString(r, 18));

                dto.setAccertamentiSpirometriaGlobale(getBoolean(r, 19));
                dto.setAccertamentiSpirometriaGlobaleData(getLocalDate(r, 20));
                dto.setAccertamentiSpirometriaGlobaleRefertoNorm(StringUtil.normalizzaVariabileSpresalEsiti(getString(r, 21)));
                dto.setAccertamentiSpirometriaGlobaleAcquisita(getString(r, 22));

                dto.setAccertamentiDlco(getBoolean(r, 23));
                dto.setAccertamentiDlcoData(getLocalDate(r, 24));
                dto.setAccertamentiDlcoRefertoNormale(StringUtil.normalizzaVariabileSpresalEsiti(getString(r, 25)));
                dto.setAccertamentiDlcoAcquisita(getString(r, 26));

                dto.setAccertamentiPet(getBoolean(r, 27));
                dto.setAccertamentiPetData(getLocalDate(r, 28));
                dto.setAccertamentiPetRefertoNormale(StringUtil.normalizzaVariabileSpresalEsiti(getString(r, 29)));
                dto.setAccertamentiPetAcquisita(getString(r, 30));

                dto.setAccertamentiVisitaPneumologica(getBoolean(r, 31));
                dto.setAccertamentiVisitaPneumologicaData(getLocalDate(r, 32));
                dto.setAccertamentiVisitaPneumologicaReferto(getString(r, 33));
                dto.setAccertamentiVisitaPneumologicaAcquisita(getString(r, 34));

                dto.setAccertamentiVisitaRadiologica(getBoolean(r, 35));
                dto.setAccertamentiVisitaRadiologicaData(getLocalDate(r, 36));
                dto.setAccertamentiVisitaRadiologicaReferto(getString(r, 37));
                dto.setAccertamentiVisitaRadiologicaAcquisita(getString(r, 38));

                dto.setAccertamentiVisitaOncologica(getBoolean(r, 39));
                dto.setAccertamentiVisitaOncologicaData(getLocalDate(r, 40));
                dto.setAccertamentiVisitaOncologicaReferto(getString(r, 41));
                dto.setAccertamentiVisitaOncologicaAcquisita(getString(r, 42));

                dto.setAccertamentiAltro(getBoolean(r, 43));
                dto.setAccertamentiAltroDescrizione(getString(r, 44));
                dto.setAccertamentiAltroData(getLocalDate(r, 45));
                dto.setAccertamentiAltroRefertoNormale(StringUtil.normalizzaVariabileSpresalEsiti(getString(r, 46)));
                dto.setAccertamentiAltroRefertoAcquisita(getString(r, 47));

                dto.setRisultatoNegativo(getBoolean(r, 48));

                dto.setPpmPlacchePleuricheMonolaterali(getBoolean(r, 49));
                dto.setPpmPrimoCertificatoEDenuncia(getBoolean(r, 50));
                dto.setPpmPrimoCertificatoEDenunciaData(getLocalDate(r, 51));
                dto.setPpmAggravamentoEDenuncia(getBoolean(r, 52));
                dto.setPpmAggravamentoEDenunciaData(getLocalDate(r, 53));
                dto.setPpmPercentualeDiRiconoscimento(getBigDecimal(r, 54));
                dto.setPpmReferto(getBoolean(r, 55));
                dto.setPpmRefertoData(getLocalDate(r, 56));

                dto.setPpbPlacchePleuricheBilaterali(getBoolean(r, 57));
                dto.setPpbPrimoCertificatoEDenuncia(getBoolean(r, 58));
                dto.setPpbPrimoCertificatoEDenunciaData(getLocalDate(r, 59));
                dto.setPpbAggravamentoEDenuncia(getBoolean(r, 60));
                dto.setPpbAggravamentoEDenunciaData(getLocalDate(r, 61));
                dto.setPpbPercentualeDiRiconoscimento(getBigDecimal(r, 62));
                dto.setPpbReferto(getBoolean(r, 63));
                dto.setPpbRefertoData(getLocalDate(r, 64));

                dto.setApAsbestosiPolmonare(getBoolean(r, 65));
                dto.setApPrimoCertificatoEDenuncia(getBoolean(r, 66));
                dto.setApPrimoCertificatoEDenunciaData(getLocalDate(r, 67));
                dto.setApAggravamentoEDenuncia(getBoolean(r, 68));
                dto.setApAggravamentoEDenunciaData(getLocalDate(r, 69));
                dto.setApPercentualeDiRiconoscimento(getBigDecimal(r, 70));
                dto.setApReferto(getBoolean(r, 71));
                dto.setApRefertoData(getLocalDate(r, 72));

                dto.setFpdFibrosiPleuricaDiffusa(getBoolean(r, 73));
                dto.setFpdPrimoCertificatoEDenuncia(getBoolean(r, 74));
                dto.setFpdPrimoCertificatoEDenunciaData(getLocalDate(r, 75));
                dto.setFpdAggravamentoEDenuncia(getBoolean(r, 76));
                dto.setFpdAggravamentoEDenunciaData(getLocalDate(r, 77));
                dto.setFpdPercentualeDiRiconoscimento(getBigDecimal(r, 78));
                dto.setFpdReferto(getBoolean(r, 79));
                dto.setFpdRefertoData(getLocalDate(r, 80));

                dto.setMpMesoteliomaPleurico(getBoolean(r, 81));
                dto.setMpPrimoCertificatoEDenuncia(getBoolean(r, 82));
                dto.setMpPrimoCertificatoEDenunciaData(getLocalDate(r, 83));
                dto.setMpAggravamentoEDenuncia(getBoolean(r, 84));
                dto.setMpAggravamentoEDenunciaData(getLocalDate(r, 85));
                dto.setMpPercentualeDiRiconoscimento(getBigDecimal(r, 86));
                dto.setMpReferto(getBoolean(r, 87));
                dto.setMpRefertoData(getLocalDate(r, 88));
                dto.setMpComunicazioneAlCor(getBoolean(r, 89));
                dto.setMpComunicazioneAlCorData(getLocalDate(r, 90));

                dto.setAmAltroMesotelioma(getBoolean(r, 91));
                dto.setAmPrimoCertificatoEDenuncia(getBoolean(r, 92));
                dto.setAmPrimoCertificatoEDenunciaData(getLocalDate(r, 93));
                dto.setAmAggravamentoEDenuncia(getBoolean(r, 94));
                dto.setAmAggravamentoEDenunciaData(getLocalDate(r, 95));
                dto.setAmPercentualeDiRiconoscimento(getBigDecimal(r, 96));
                dto.setAmReferto(getBoolean(r, 97));
                dto.setAmRefertoData(getLocalDate(r, 98));
                dto.setAmComunicazioneAlCor(getBoolean(r, 99));
                dto.setAmComunicazioneAlCorData(getLocalDate(r, 100));

                dto.setNlNeoplasiaLaringe(getBoolean(r, 101));
                dto.setNlPrimoCertificatoEDenuncia(getBoolean(r, 102));
                dto.setNlPrimoCertificatoEDenunciaData(getLocalDate(r, 103));
                dto.setNlAggravamentoEDenuncia(getBoolean(r, 104));
                dto.setNlAggravamentoEDenunciaData(getLocalDate(r, 105));
                dto.setNlPercentualeDiRiconoscimento(getBigDecimal(r, 106));
                dto.setNlReferto(getBoolean(r, 107));
                dto.setNlRefertoData(getLocalDate(r, 108));

                dto.setNoNeoplasiaOvarica(getBoolean(r, 109));
                dto.setNoPrimoCertificatoEDenuncia(getBoolean(r, 110));
                dto.setNoPrimoCertificatoEDenunciaData(getLocalDate(r, 111));
                dto.setNoAggravamentoEDenuncia(getBoolean(r, 112));
                dto.setNoAggravamentoEDenunciaData(getLocalDate(r, 113));
                dto.setNoPercentualeDiRiconoscimento(getBigDecimal(r, 114));
                dto.setNoReferto(getBoolean(r, 115));
                dto.setNoRefertoData(getLocalDate(r, 116));

                dto.setTpTumoreDelPolmone(getBoolean(r, 117));
                dto.setTpPrimoCertificatoEDenuncia(getBoolean(r, 118));
                dto.setTpPrimoCertificatoEDenunciaData(getLocalDate(r, 119));
                dto.setTpAggravamentoEDenuncia(getBoolean(r, 120));
                dto.setTpAggravamentoEDenunciaData(getLocalDate(r, 121));
                dto.setTpPercentualeDiRiconoscimento(getBigDecimal(r, 122));
                dto.setTpReferto(getBoolean(r, 123));
                dto.setTpRefertoData(getLocalDate(r, 124));
                dto.setTpComunicazioneAlCor(getBoolean(r, 125));
                dto.setTpComunicazioneAlCorData(getLocalDate(r, 126));

                dto.setBpcoEnfisemaPolmonare(getBoolean(r, 127));
                dto.setBpcoPrimoCertificatoEDenuncia(getBoolean(r, 128));
                dto.setBpcoPrimoCertificatoEDenunciaData(getLocalDate(r, 129));
                dto.setBpcoAggravamentoEDenuncia(getBoolean(r, 130));
                dto.setBpcoAggravamentoEDenunciaData(getLocalDate(r, 131));
                dto.setBpcoPercentualeDiRiconoscimento(getBigDecimal(r, 132));
                dto.setBpcoReferto(getBoolean(r, 133));
                dto.setBpcoRefertoData(getLocalDate(r, 134));

                dto.setAltraDiagnosi(getBoolean(r, 135));
                dto.setAltraDiagnosiDescrizione(getString(r, 136));
                dto.setAltraPrimoCertificatoEDenuncia(getBoolean(r, 137));
                dto.setAltraPrimoCertificatoEDenunciaData(getLocalDate(r, 138));
                dto.setAltraAggravamentoEDenuncia(getBoolean(r, 139));
                dto.setAltraAggravamentoEDenunciaData(getLocalDate(r, 140));
                dto.setAltraPercentualeDiRiconoscimento(getBigDecimal(r, 141));
                dto.setAltraReferto(getBoolean(r, 142));
                dto.setAltraRefertoData(getLocalDate(r, 143));

                dto.setFollowUpPrevisto(getBoolean(r, 144));
                dto.setAnnoPresuntoProssimaVisita(getInteger(r, 145));
                dto.setAnnoUltimaVisita(getInteger(r, 146));
                dto.setInvioSintesiAMmg(getBoolean(r, 147));
                dto.setIdSpresal(getBigDecimal(r, 148));

                dto.setValiditaInizio(null);
                dto.setValiditaFine(null);
                dto.setDataCreazione(null);
                dto.setDataModifica(null);
                dto.setDataCancellazione(null);
                dto.setUtenteCreazione(null);
                dto.setUtenteModifica(null);
                dto.setUtenteCancellazione(null);

                rows.add(dto);
            }

            return rows;
        }
    }
    

    public static List<AdesioneDTO> parseAdesioni(InputStream is) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<AdesioneDTO> rows = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;

                // Salta righe vuote
                if (getString(r, 0) == null && getString(r, 2) == null) continue;

                AdesioneDTO dto = new AdesioneDTO();
                dto.setAdesioneCod(getString(r, 0));           // A = Codice Adesione
                dto.setAdesioneData(getLocalDate(r, 1));        // B = Data Adesione
                dto.setCodiceFiscale(getString(r, 2));          // C = Codice Fiscale
                dto.setCognome(getString(r, 3));         // D = Cognome
                dto.setNome(getString(r, 4));            // E = Nome
                dto.setNascitaData(getLocalDate(r, 5));         // F = Data di nascita
                dto.setNascitaProvinciaDesc(getString(r, 6));   // G = Provincia di nascita
                dto.setNascitaComuneDesc(getString(r, 7));      // H = Comune di nascita
                dto.setTesseraTeam(getString(r, 8));     // I = Tessera Team
                dto.setIdAura(getString(r, 9));          // J = Id Aura
                dto.setDomicilioProvinciaDesc(getString(r, 10)); // K = Provincia di domicilio
                dto.setDomicilioComuneDesc(getString(r, 11));   // L = Comune di domicilio
                dto.setDomicilioComuneCod(getString(r, 12));    // M = Codice comune ISTAT domicilio
                dto.setDomicilioCap(getString(r, 13));          // N = CAP di domicilio
                dto.setEmail(getString(r, 14));          // O = Email
                dto.setTelefono(getString(r, 15));       // P = Telefono
                dto.setDomicilioAslCod(getString(r, 16));       // Q = Codice ASL domicilio
                dto.setDomicilioAslDesc(getString(r, 17));      // R = ASL di domicilio
                dto.setAziendaCod(getString(r, 18));            // S = Codazi
                dto.setResidenzaAslCod(getString(r, 19));       // T = Codice ASL residenza
                dto.setResidenzaAslDesc(getString(r, 20));      // U = ASL di residenza
                dto.setEsposizioneInizio(getLocalDate(r, 21)); // V = Data inizio esposizione
                dto.setEsposizioneFine(getLocalDate(r, 22));   // W = Data fine esposizione
 				dto.setEsposizioneAzienda(getString(r, 23)); // X = Azienda
                dto.setEsposizioneAziendaComuneCod(getString(r, 24)); // Y = Codice comune ISTAT azienda
                dto.setEsposizioneAziendaComuneDesc(getString(r, 25)); // Z = Comune azienda
                dto.setEsposizioneAziendaCap(getString(r, 26)); // AA = CAP azienda
                dto.setEsposizioneAziendaProvincia(getString(r, 27)); // AB = Provincia azienda
                dto.setEsposizioneMansione(getString(r, 28));
                System.out.println("DE NON DEVO FARE IL SET DELLA DATA DI PRE ADESIONE DA FILE");
                /* DE DA VERIFICARE NON DEVE METTERE DATA PREADESIONE DA FILE
                if (dto.getAdesioneData() == null && dto.getAdesioneCod() != null) {
                    try {
                        long serial = Long.parseLong(dto.getAdesioneCod());
                        if (serial >= 36526 && serial <= 2958465) {
                            dto.setAdesioneData(DateUtil.getLocalDateTime((double) serial, false).toLocalDate());
                        }
                    } catch (NumberFormatException ignored) {}
                }
                */
                rows.add(dto);
            }
            return rows;
        }
    }

    private static String getString(Row r, int idx) {
        Cell c = r.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.BLANK) return null;

        String v;
        switch (c.getCellType()) {
            case STRING:
                v = c.getStringCellValue();
                break;
            case NUMERIC:
                v = String.valueOf((long) c.getNumericCellValue());
                break;
            case BOOLEAN:
                v = String.valueOf(c.getBooleanCellValue());
                break;
            case FORMULA:
                try { v = c.getStringCellValue(); }
                catch (Exception e) { v = String.valueOf((long) c.getNumericCellValue()); }
                break;
            default:
                return null;
        }
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }
    
    
    private static Boolean getBoolean(Row r, int idx) {
        Cell c = r.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.BLANK) return null;

        try {
            switch (c.getCellType()) {
                case BOOLEAN:
                    return c.getBooleanCellValue();

                case STRING:
                    String s = c.getStringCellValue();
                    if (s == null) return null;
                    s = s.trim().toLowerCase();
                    if (s.isEmpty()) return null;
                    return "sì".equals(s) || "si".equals(s) || "true".equals(s) || "1".equals(s) || "yes".equals(s);

                case NUMERIC:
                    double numericValue = c.getNumericCellValue();
                    return numericValue == 1.0 || numericValue == 0.0 ? (numericValue == 1.0) : null;

                case FORMULA:
                    try {
                        // prova boolean prima
                        return c.getBooleanCellValue();
                    } catch (Exception e) {
                        // fallback numerico
                        try {
                            double fv = c.getNumericCellValue();
                            return fv == 1.0 ? Boolean.TRUE : (fv == 0.0 ? Boolean.FALSE : null);
                        } catch (Exception e2) {
                            // fallback stringa
                            String fs = c.getStringCellValue();
                            if (fs == null) return null;
                            fs = fs.trim().toLowerCase();
                            if (fs.isEmpty()) return null;
                            return "sì".equals(fs) || "si".equals(fs) || "true".equals(fs) || "1".equals(fs) || "yes".equals(fs);
                        }
                    }

                default:
                    return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    
    
    private static Integer getInteger(Row r, int idx) {
        Cell c = r.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.BLANK) return null;

        try {
            switch (c.getCellType()) {
                case NUMERIC:
                	return (int) c.getNumericCellValue();

                case STRING:
                    String s = c.getStringCellValue();
                    if (s == null) return null;
                    s = s.trim();
                    if (s.isEmpty()) return null;
                    return Integer.valueOf(s); // lancia eccezione se non è un intero

                case BOOLEAN:
                	return c.getBooleanCellValue() ? 1 : 0;

                case FORMULA:
                    try {
                        // prova a leggere come numerico
                        return (int) c.getNumericCellValue();
                    } catch (Exception e) {
                        // fallback: prova come stringa
                        String fs = c.getStringCellValue();
                        if (fs == null) return null;
                        fs = fs.trim();
                        if (fs.isEmpty()) return null;
                        return Integer.valueOf(fs);
                    }

                default:
                    return null;
            }
        } catch (NumberFormatException ex) {
            // valore non convertibile a intero
            return null;
        }
    }
    
    
    private static BigDecimal getBigDecimal(Row r, int idx) {
        Cell c = r.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.BLANK) return null;
        
        try {
            switch (c.getCellType()) {
                case NUMERIC:
                    // BigDecimal da double con precisione completa
                    return BigDecimal.valueOf(c.getNumericCellValue());

                case STRING:
                    String s = c.getStringCellValue();
                    if (s == null) return null;
                    s = s.trim();
                    if (s.isEmpty()) return null;
                    return new BigDecimal(s);  // parse stringa con precisione esatta

                case BOOLEAN:
                    return BigDecimal.valueOf(c.getBooleanCellValue() ? 1L : 0L);

                case FORMULA:
                    try {
                    	return BigDecimal.valueOf(c.getNumericCellValue());
                    } catch (Exception e) {
                    String fs = c.getStringCellValue();
                    if (fs == null) return null;
                    fs = fs.trim();
                    if (fs.isEmpty()) return null;
                    return new BigDecimal(fs);
                }

            default:
                return null;
        }
    } catch (NumberFormatException ex) {
    	return null;
    }
}
    
    

    private static LocalDate getLocalDate(Row r, int idx) {
        Cell c = r.getCell(idx);
        if (c == null || c.getCellType() == CellType.BLANK) return null;

        switch (c.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(c)) {
                    return c.getLocalDateTimeCellValue().toLocalDate();
                }
                double numVal = c.getNumericCellValue();
                if (numVal >= 1 && numVal <= 2958465) {
                    try {
                        return DateUtil.getLocalDateTime(numVal, false).toLocalDate();
                    } catch (Exception ignored) {}
                }
                return null;


            case STRING: {
                String v = c.getStringCellValue();
                if (v == null || v.isBlank()) return null;
                v = v.trim();
                // Gestisce seriali numerici in celle stringa (es. "22912")
                try {
                    long serial = Long.parseLong(v);
                    if (serial >= 1 && serial <= 2958465) {
                        return DateUtil.getLocalDateTime((double) serial, false).toLocalDate();
                    }
                } catch (NumberFormatException ignored) {}
                for (DateTimeFormatter fmt : DATE_FORMATTERS) {
                    try { return LocalDate.parse(v, fmt); }
                    catch (Exception ignored) {}
                }
                return null;
            }

            case FORMULA: {
                try {
                    if (DateUtil.isCellDateFormatted(c)) {
                        return c.getLocalDateTimeCellValue().toLocalDate();
                    }
                } catch (Exception ignored) {}
                return null;
            }

            default:
                return null;
        }
    }
}
