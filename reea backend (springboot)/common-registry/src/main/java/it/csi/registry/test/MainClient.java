//package it.csi.registry.test;
//
//import it.csi.registry.client.configuratore.api.ConfiguratoreApiApi;
//import it.csi.registry.client.configuratore.invoker.ApiClient;
//import it.csi.registry.client.configuratore.model.Dettaglio;
//
//public class MainClient {
//
//    public static void main(String[] args) {
//
//        // 1️⃣ Crea ApiClient
//        ApiClient apiClient = new ApiClient();
//
//        // 2️⃣ Imposta URL base del servizio remoto (da parametrizzare in application)
//        apiClient.setBasePath("http://tst-be-srv-solconfig.csi.it/configuratoreapi/api/v1");
//
//        // 3️⃣ Imposta BASIC AUTH
//        apiClient.setUsername("apisolconfigpreprod");
//        apiClient.setPassword("mypass");
//
//        // 4️⃣ Header di default
//        apiClient.addDefaultHeader("Accept", "application/json");
//
//        // 5️⃣ Crea l'API specifica
//        ConfiguratoreApiApi api = new ConfiguratoreApiApi(apiClient);
//
//        try {
//            // 6️⃣ Chiamata all'endpoint (esempio)
//        	// qui sono da recuperare i valori corretti
//            Dettaglio response = (Dettaglio) api.getProfiliFunzionalita1(null, null, null, null, null, null, null, null);
//            // 7️⃣ Usa la risposta
//            System.out.println("Risposta: " + response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}

package it.csi.registry.test;

import it.csi.registry.client.configuratore.api.DefaultApi;
import it.csi.registry.client.configuratore.invoker.ApiClient;
import it.csi.registry.client.configuratore.model.ModelTokenInformazione;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MainClient.class);

    public static void main(String[] args) {

        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://tst-be-srv-solconfig.csi.it/configuratoreapi/api/v1");
        apiClient.setUsername("apisolconfigpreprod");
        apiClient.setPassword("mypass");

        DefaultApi api = new DefaultApi(apiClient);

        try {
            String tokenDalPua = "INSERISCI-QUI-UN-TOKEN-VALIDO";

            ModelTokenInformazione info = api.proxyTokenInformationGet1(
                    "RSSMRA80A01H501U",          // CF utente (da Shibboleth in prod)
                    UUID.randomUUID().toString(), // X-Request-Id univoco
                    "127.0.0.1",                 // X-Forwarded-For (IP client)
                    "REEA",                      // X-Codice-Servizio (codice applicazione)
                    tokenDalPua                  // Token ricevuto dal redirect PUA
            );

            System.out.println("Nome:    " + info.getRichiedente().getNome());
            System.out.println("Cognome: " + info.getRichiedente().getCognome());
            System.out.println("CF:      " + info.getRichiedente().getCodiceFiscale());
            System.out.println("Ruolo:   " + info.getRichiedente().getRuolo());
            System.out.println("Collocazione: " + info.getRichiedente().getCollocazione().getDescrizioneCollocazione());
            System.out.println("Funzionalità: " + info.getFunzionalita().size());

        } catch (Exception e) {
//            e.printStackTrace();
        	LOGGER.error(
                    "Errore durante la chiamata al servizio",
                    e
            );
        }
    }
}
