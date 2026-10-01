package it.csi.registry.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.xml.datatype.XMLGregorianCalendar;

public final class DateConversionUtils {

    private DateConversionUtils() {
        // utility class
    }

    public static LocalDate toLocalDate(XMLGregorianCalendar xmlCal) {
        if (xmlCal == null) {
            return null;
        }
        // Se il campo è solo data (no ora/fuso) basta usare year/month/day
        return LocalDate.of(
                xmlCal.getYear(),
                xmlCal.getMonth(),
                xmlCal.getDay()
        );
    }
    
    public static LocalDateTime toStartOfDay(OffsetDateTime odt) {
        return odt != null ? odt.toLocalDate().atStartOfDay() : null;
    }


    public static OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        ZoneOffset offset = ZoneId.systemDefault()
                .getRules()
                .getOffset(Instant.now());
        return ldt.atOffset(offset);
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime ldt, ZoneOffset offset) {
        if (ldt == null || offset == null) {
            return null;
        }
        return ldt.atOffset(offset);
    }
    
    public static LocalDateTime stringToLocalDateTime(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.trim());
    }
    
    
//    public static LocalDate stringToLocalDate(String value) {
//        if (value == null || value.trim().isBlank()) {
//            return null;
//        }
//        return LocalDate.parse(value.trim()); // formato ISO: 2026-03-24
//    }
    
    public static LocalDate stringToLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        // Se contiene la 'T' è un formato datetime -> parse come LocalDateTime e prendi solo la data
        if (dateStr.contains("T")) {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
        }

        // Formato semplice yyyy-MM-dd
        return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    
    private static final DateTimeFormatter ISO_OFFSET_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME; // es: 2011-12-03T10:15:30+01:00

    public static String offsetDateTimeToString(OffsetDateTime value) {
        return value != null ? value.format(ISO_OFFSET_FORMATTER) : null;
    }

    public static OffsetDateTime stringToOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value, ISO_OFFSET_FORMATTER);
    }
    
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME; // es. 2026-03-19T15:20:00

    public static String localDateTimeToString(LocalDateTime value) {
        return value != null ? value.format(ISO_FORMATTER) : null;
    }
    


}
