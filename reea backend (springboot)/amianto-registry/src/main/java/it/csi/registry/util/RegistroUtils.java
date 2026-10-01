package it.csi.registry.util;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.RegistroDTO;

public final class RegistroUtils {

    private RegistroUtils() {
        // utility class
    }

    /**
     * Garantisce che l'AnagraficaDTO abbia un RegistroDTO non nullo.
     * Ritorna sempre la stessa istanza (non nulla) di RegistroDTO.
     */
    public static RegistroDTO ensureRegistro(AnagraficaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AnagraficaDTO nullo");
        }
        RegistroDTO registro = dto.getRegistro();
        if (registro == null) {
            registro = new RegistroDTO();
            dto.setRegistro(registro);
        }
        return registro;
    }
}