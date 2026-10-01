package it.csi.registry.soap.anagrafe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.csi.registry.repositories.ParametroRepository;
import it.csi.registry.soap.anagrafe.clients.AnagrafeGetClient;

@Configuration
public class AnagrafeGetConfig {

    private final ParametroRepository parametroRepository;

    public AnagrafeGetConfig(ParametroRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    @Bean
    public AnagrafeGetClient anagrafeGetClient() {
        String endpoint = parametroRepository.getValoreByCod("AURA_ENDPOINT_GET");
        String user     = parametroRepository.getValoreByCod("AURA_USER");
        String password = parametroRepository.getValoreByCod("AURA_PASSWORD");
        return new AnagrafeGetClient(endpoint, user, password);
    }
}
