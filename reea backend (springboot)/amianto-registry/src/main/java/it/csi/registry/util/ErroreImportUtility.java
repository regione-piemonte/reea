package it.csi.registry.util;

import java.nio.charset.StandardCharsets;

public final class ErroreImportUtility {

    private ErroreImportUtility() {
        throw new IllegalStateException("Utility class");
    }

    public static String getRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String message = root.getMessage() != null ? root.getMessage() : root.toString();
        return fixEncodingIfNeeded(message);
    }

    public static String fixEncodingIfNeeded(String text) {
        if (text == null) {
            return null;
        }

        if (text.contains("ï¿½") || text.contains("Ã") || text.contains("Â")) {
            try {
                return new String(text.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            } catch (Exception e) {
                return text;
            }
        }

        return text;
    }

    public static String extractPrimaryPostgresError(String message) {
        if (message == null || message.isBlank()) {
            return "Errore database non disponibile";
        }

        String result = message;

        int errorIdx = result.indexOf("ERROR:");
        if (errorIdx >= 0) {
            result = result.substring(errorIdx).trim();
        }

        int detailIdx = result.indexOf("Dettaglio:");
        if (detailIdx >= 0) {
            result = result.substring(0, detailIdx).trim();
        }

        result = result.replaceAll("\\s+", " ").trim();
        return result;
    }

    public static String extractNotNullColumn(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String marker = "null value in column \"";
        int start = message.indexOf(marker);
        if (start < 0) {
            return null;
        }

        start += marker.length();
        int end = message.indexOf("\"", start);
        if (end < 0) {
            return null;
        }

        return message.substring(start, end).trim();
    }

    public static String buildErroreDescrizione(String erroreSqlPulito, String colonnaErrore) {
        if (colonnaErrore != null && !colonnaErrore.isBlank()) {
            return "Campo obbligatorio mancante: " + colonnaErrore;
        }

        return erroreSqlPulito != null ? erroreSqlPulito : "Errore generico in inserimento";
    }
    
    public static String buildErroreDescrizione(Throwable throwable) {
        String rawMessage = getRootCauseMessage(throwable);
        String erroreSqlPulito = extractPrimaryPostgresError(rawMessage);
        String colonnaErrore = extractNotNullColumn(rawMessage);
        return buildErroreDescrizione(erroreSqlPulito, colonnaErrore);
    }
}