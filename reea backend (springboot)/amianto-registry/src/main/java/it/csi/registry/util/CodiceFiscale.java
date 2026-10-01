package it.csi.registry.util;

public class CodiceFiscale {

    private CodiceFiscale() {}


    public static  String AnonimizzaCodiceFiscale(String cf) {
    	if (cf == null || cf.length() != 16) {
    		throw new IllegalArgumentException("Codice fiscale non valido");
    	}

    	StringBuilder sb = new StringBuilder(16);

    	// primi 3 caratteri invariati
    	sb.append(cf, 0, 3);   // CRT

    	// 4-6 -> XXX
    	sb.append("XXX");

    	// 7-8 -> 99
    	sb.append("99");

    	// dal 9° carattere in poi invariati
    	sb.append(cf.substring(8)); // da 'M' di M15L418M in poi

    	return sb.toString();
    }

}

