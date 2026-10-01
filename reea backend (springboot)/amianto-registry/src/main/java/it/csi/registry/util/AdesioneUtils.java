//package it.csi.registry.util;
//
//import it.csi.registry.model.AnagraficaDTO;
//import it.csi.registry.model.AdesioneDTO;
//
//public final class AdesioneUtils {
//
//    private AdesioneUtils() {
//        // utility class
//    }
//
//    /**
//     * Garantisce che l'AnagraficaDTO abbia un AdesioneDTO non nullo.
//     * Ritorna sempre la stessa istanza (non nulla) di AdesioneDTO.
//     */
//    public static AdesioneDTO ensureAdesione(AnagraficaDTO dto) {
//        if (dto == null) {
//            throw new IllegalArgumentException("AnagraficaDTO nullo");
//        }
//        AdesioneDTO adesione = dto.getAdesione();
//        if (adesione == null) {
//            adesione = new AdesioneDTO();
//            dto.setAdesione(adesione);
//        }
//        return adesione;
//    }
//}
