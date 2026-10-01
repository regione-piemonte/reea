package it.csi.registry.util;

public class StringUtil {
	
	public static String normalizzaVariabileSpresalEsiti(String value) {
	    if (value == null) return null;

	    String s = value.trim();

	    if (s.isEmpty()) return null;

	    // case-insensitive su italiano
	    String lower = s.toLowerCase();

	    if (lower.equals("non conclusivo")) {
	        return "Non conclusivo";   // forma esatta che il DB accetta
	    }

	    // eventualmente altri mapping:
	    // if (lower.equals("si")) return "SI";
	    // if (lower.equals("no")) return "NO";

	    return s; // di default restituisci la stringa ripulita
	}

}
