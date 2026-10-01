package it.csi.registry.util;

import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.SpresalDTO;

public final class SpresalUtils {

    private SpresalUtils() {
    }

    public static SpresalDTO ensureSpresal(AnagraficaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AnagraficaDTO nullo");
        }
        SpresalDTO spresal = dto.getSpresal();
        if (spresal == null) {
            spresal = new SpresalDTO();
            dto.setSpresal(spresal);
        }
        return spresal;
    }
}
