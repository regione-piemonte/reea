package it.csi.registry.soap.anagrafe.services;

import it.csi.registry.aura.dto.AuraAssistitoDto;
import it.csi.registry.aura.mapper.AuraMapper;
import it.csi.registry.soap.AnagrafeSanitaria.SoggettoAuraMsg;
import it.csi.registry.soap.anagrafe.clients.AnagrafeGetClient;
import org.springframework.stereotype.Service;

@Service
public class AnagrafeGetService {

    private final AnagrafeGetClient client;

    public AnagrafeGetService(AnagrafeGetClient client) {
        this.client = client;
    }

    public AuraAssistitoDto getByIdAura(String idAura) {
        SoggettoAuraMsg response = client.get(idAura);

        if (response == null || response.getBody() == null) {
            return null;
        }

        return AuraMapper.toDto(response);
    }
}
