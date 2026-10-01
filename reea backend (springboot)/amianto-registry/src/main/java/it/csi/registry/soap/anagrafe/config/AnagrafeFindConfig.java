package it.csi.registry.soap.anagrafe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.csi.registry.repositories.ParametroRepository;
import it.csi.registry.soap.anagrafe.clients.AnagrafeFindClient;

@Configuration
public class AnagrafeFindConfig {

    private final ParametroRepository parametroRepository;

    public AnagrafeFindConfig(ParametroRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    @Bean
    public AnagrafeFindClient anagrafeFindClient() {
        String endpoint = parametroRepository.getValoreByCod("AURA_ENDPOINT_FIND");
        String user     = parametroRepository.getValoreByCod("AURA_USER");
        String password = parametroRepository.getValoreByCod("AURA_PASSWORD");
        return new AnagrafeFindClient(endpoint, user, password);
    }
}
