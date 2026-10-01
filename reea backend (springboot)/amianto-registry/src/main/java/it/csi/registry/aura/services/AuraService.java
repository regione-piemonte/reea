package it.csi.registry.aura.services;

import it.csi.registry.aura.dto.AuraAssistitoDto;

import java.util.List;

public interface AuraService {

    AuraAssistitoDto getByIdAura(String idAura);

    AuraAssistitoDto findByCodiceFiscale(String codiceFiscale);

    List<AuraAssistitoDto> findByAnagrafica(String cognome, String nome, String dataNascita);
}