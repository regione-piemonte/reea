package it.csi.registry.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import it.csi.registry.model.NplaDTO;
import it.csi.registry.repositories.TracciaElaborazioneRepository;

@Component
public class ExcelNplaParser {
	
	private final TracciaElaborazioneRepository tracciaElaborazioneRepository;

    private ExcelNplaParser(TracciaElaborazioneRepository tracciaElaborazioneRepository) {
    	 this.tracciaElaborazioneRepository = tracciaElaborazioneRepository;
    }

    public List<NplaDTO> parseNpla(InputStream is, DSLContext ctx, Integer fileId, String utenteCreazione) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<NplaDTO> rows = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) { 
                	tracciaElaborazioneRepository.inserisciScarto(
                			ctx,
                            fileId,
                            "ERR_ELABORAZIONE",
                            "ERRORE",
                            null,
                            utenteCreazione,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "REEA_T_REGISTRO_PDL_AMIANTO",
                            0
                    );
                	continue;
                }

                String cf = getString(r, 0);
                if (cf == null) {
                	tracciaElaborazioneRepository.inserisciScarto(
                			ctx,
                            fileId,
                            "ERR_ELABORAZIONE",
                            "Codice Fiscale non presente",
                            null,
                            utenteCreazione,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "REEA_T_REGISTRO_PDL_AMIANTO",
                            0
                    );
                	continue;
                }

                NplaDTO dto = new NplaDTO();
                dto.setCodiceFiscale(cf);                                      // A = CF
                dto.setPeriodo(getInteger(r, 1));                              // B = Periodo  ← MANCA
                dto.setIdCantiere(getInteger(r, 2));                           // C = Id cantiere
                dto.setAziendaPiva(getString(r, 3));                           // D = Azienda PIVA
                dto.setAziendaNome(getString(r, 4));                           // E = Azienda nome
                dto.setAslCantiere(getString(r, 5));                           // F = ASL cantiere
                dto.setComuneCantiere(getString(r, 6));                       // G = Comune cantiere
                dto.setAnno(getInteger(r, 7));                                 // H = Anno  ← anche questo mancava
                dto.setTipologiaPiano(getString(r, 8));
                dto.setQuantitaDaRimuovere(getBigDecimal(r, 9));
                dto.setQuantitaRimossa(getBigDecimal(r, 10));

                rows.add(dto);
            }
            return rows;
        }
    }

    private static String getString(Row r, int idx) {
        Cell c = r.getCell(idx);
        if (c == null || c.getCellType() == CellType.BLANK) return null;
        String v;
        switch (c.getCellType()) {
            case STRING:  v = c.getStringCellValue(); break;
            case NUMERIC: v = String.valueOf((long) c.getNumericCellValue()); break;
            case BOOLEAN: v = String.valueOf(c.getBooleanCellValue()); break;
            case FORMULA:
                try { v = c.getStringCellValue(); }
                catch (Exception e) { v = String.valueOf((long) c.getNumericCellValue()); }
                break;
            default: return null;
        }
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private static Integer getInteger(Row r, int idx) {
        String s = getString(r, idx);
        if (s == null) return null;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }

    private static BigDecimal getBigDecimal(Row r, int idx) {
        Cell c = r.getCell(idx);
        if (c == null || c.getCellType() == CellType.BLANK) return null;
        switch (c.getCellType()) {
            case NUMERIC: return BigDecimal.valueOf(c.getNumericCellValue());
            case STRING:
                try { return new BigDecimal(c.getStringCellValue().trim().replace(",", ".")); }
                catch (NumberFormatException e) { return null; }
            case FORMULA:
                try { return BigDecimal.valueOf(c.getNumericCellValue()); }
                catch (Exception e) { return null; }
            default: return null;
        }
    }


}
