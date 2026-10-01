package it.csi.registry.soap.anagrafe.services;

import it.csi.registry.aura.dto.AuraAssistitoDto;
import it.csi.registry.aura.mapper.AuraMapper;
import it.csi.registry.soap.AnagrafeFind.DatiAnagraficiMsg;
import it.csi.registry.soap.AnagrafeFind.DatiAnagrafici;
import it.csi.registry.soap.anagrafe.clients.AnagrafeFindClient;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AnagrafeFindService {

    private final AnagrafeFindClient client;

    public AnagrafeFindService(AnagrafeFindClient client) {
        this.client = client;
    }

    public AuraAssistitoDto findByCodiceFiscale(String codiceFiscale) {
        DatiAnagraficiMsg response = client.find("1", codiceFiscale, null, null);

        if (response == null
                || response.getBody() == null
                || response.getBody().getElencoProfili() == null
                || response.getBody().getElencoProfili().getDatianagrafici().isEmpty()) {
            return null;
        }

        DatiAnagrafici dato = response.getBody()
                .getElencoProfili()
                .getDatianagrafici()
                .get(0);

        return AuraMapper.toDto(dato);
    }

    public List<AuraAssistitoDto> findByAnagrafica(String cognome, String nome, String dataNascita) {
        DatiAnagraficiMsg response = client.findByAnagrafica(cognome, nome, dataNascita);

        if (response == null
                || response.getBody() == null
                || response.getBody().getElencoProfili() == null
                || response.getBody().getElencoProfili().getDatianagrafici().isEmpty()) {
            return List.of();
        }

        return response.getBody()
                .getElencoProfili()
                .getDatianagrafici()
                .stream()
                .map(AuraMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }
}
