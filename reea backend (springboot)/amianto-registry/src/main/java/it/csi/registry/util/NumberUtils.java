package it.csi.registry.util;

import java.math.BigDecimal;

public final class NumberUtils {
    private NumberUtils() {}

    public static Integer toInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null; // o logga l'errore
        }
    }
    
    public static String formatBigDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        // Se la scala è 0 o la parte frazionaria è 0, restituisci l'intero
        if (value.scale() <= 0 || value.stripTrailingZeros().scale() <= 0) {
            return String.valueOf(value.longValue());
        }
        // Altrimenti restituisci il decimale (es. 2.5, 3.75)
        return value.toPlainString();
    }
}
