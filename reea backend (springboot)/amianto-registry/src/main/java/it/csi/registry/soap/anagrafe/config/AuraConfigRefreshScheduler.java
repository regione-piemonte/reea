package it.csi.registry.soap.anagrafe.config;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import it.csi.registry.repositories.ParametroRepository;
import it.csi.registry.soap.anagrafe.clients.AnagrafeFindClient;
import it.csi.registry.soap.anagrafe.clients.AnagrafeGetClient;

/**
 * Rilegge periodicamente da reea_c_parametro i parametri di connessione AURA
 * (AURA_ENDPOINT_FIND, AURA_ENDPOINT_GET, AURA_USER, AURA_PASSWORD) e, se sono
 * cambiati rispetto all'ultima lettura, riconfigura a runtime i client SOAP
 * Find/Get — così una modifica sul DB ha effetto senza dover riavviare il pod.
 */
@Component
public class AuraConfigRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuraConfigRefreshScheduler.class);

    private final ParametroRepository parametroRepository;
    private final AnagrafeFindClient anagrafeFindClient;
    private final AnagrafeGetClient anagrafeGetClient;

    private volatile boolean inizializzato = false;
    private String ultimoEndpointFind;
    private String ultimoEndpointGet;
    private String ultimoUser;
    private String ultimoPassword;

    public AuraConfigRefreshScheduler(ParametroRepository parametroRepository,
                                       AnagrafeFindClient anagrafeFindClient,
                                       AnagrafeGetClient anagrafeGetClient) {
        this.parametroRepository = parametroRepository;
        this.anagrafeFindClient = anagrafeFindClient;
        this.anagrafeGetClient = anagrafeGetClient;
    }

    @Scheduled(
            initialDelayString = "${aura.config.refresh.interval-ms:60000}",
            fixedDelayString = "${aura.config.refresh.interval-ms:60000}")
    public void verificaESincronizzaConfigurazione() {
        String endpointFind = parametroRepository.getValoreByCod("AURA_ENDPOINT_FIND");
        String endpointGet = parametroRepository.getValoreByCod("AURA_ENDPOINT_GET");
        String user = parametroRepository.getValoreByCod("AURA_USER");
        String password = parametroRepository.getValoreByCod("AURA_PASSWORD");

        if (!inizializzato) {
            // Primo giro dopo l'avvio: i client sono già stati configurati dal costruttore
            // con questi stessi valori, ci limitiamo a memorizzarli come stato di riferimento.
            inizializzato = true;
        } else {
            boolean credenzialiCambiate = !Objects.equals(user, ultimoUser)
                    || !Objects.equals(password, ultimoPassword);

            if (credenzialiCambiate || !Objects.equals(endpointFind, ultimoEndpointFind)) {
                log.info("Rilevata modifica alla configurazione AURA Find in reea_c_parametro: riconfiguro il client a runtime");
                anagrafeFindClient.reinizializza(endpointFind, user, password);
            }

            if (credenzialiCambiate || !Objects.equals(endpointGet, ultimoEndpointGet)) {
                log.info("Rilevata modifica alla configurazione AURA Get in reea_c_parametro: riconfiguro il client a runtime");
                anagrafeGetClient.reinizializza(endpointGet, user, password);
            }
        }

        ultimoEndpointFind = endpointFind;
        ultimoEndpointGet = endpointGet;
        ultimoUser = user;
        ultimoPassword = password;
    }
}
