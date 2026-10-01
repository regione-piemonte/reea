package it.csi.registry.soap.anagrafe.clients;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.csi.registry.soap.AnagrafeSanitaria.*;
import it.csi.registry.util.PasswordCallbackHandler;
import jakarta.xml.ws.BindingProvider;
import java.net.URL;
import java.io.IOException;


public class AnagrafeGetClient {

    private static final Logger log = LoggerFactory.getLogger(AnagrafeGetClient.class);

    private final AnagrafeSanitariaSoap client;
    private final Client cxfClient;

    private volatile String endpointInUso;
    private volatile boolean endpointConfigurato;
    private Interceptor<? extends Message> wsSecurityInterceptor;

    public AnagrafeGetClient(String endpoint, String user, String password) {
        URL wsdlUrl = null;
        try {
            wsdlUrl = getClass().getClassLoader().getResource("wsdl/AURA.WS.AnagrafeSanitaria.wsdl");
            if (wsdlUrl == null) {
                throw new IOException("WSDL non trovato nel classpath: wsdl/AURA.WS.AnagrafeSanitaria.wsdl");
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore caricamento WSDL dal classpath", e);
        }

        AnagrafeSanitaria service = new AnagrafeSanitaria(wsdlUrl);
        this.client = service.getAnagrafeSanitariaSoap();
        this.cxfClient = ClientProxy.getClient(client);

        // Logga la busta SOAP grezza inviata/ricevuta, per diagnosticare i fault AURA
        cxfClient.getInInterceptors().add(new LoggingInInterceptor());
        cxfClient.getOutInterceptors().add(new LoggingOutInterceptor());

        applicaConfigurazione(endpoint, user, password);
    }

    /**
     * Riapplica endpoint e credenziali senza ricreare il client SOAP: usato sia dal
     * costruttore sia da /api/aura/admin/refresh, per aggiornare la configurazione
     * a runtime senza dover riavviare il pod dopo una modifica a reea_c_parametro.
     */
    public synchronized void reinizializza(String endpoint, String user, String password) {
        applicaConfigurazione(endpoint, user, password);
        log.info("AnagrafeGetClient riconfigurato a runtime. endpoint={}, credenzialiPresenti={}",
                endpointInUso, user != null && password != null);
    }

    private void applicaConfigurazione(String endpoint, String user, String password) {
        // Rimuove l'interceptor WS-Security precedente, se presente, prima di applicarne uno nuovo
        if (wsSecurityInterceptor != null) {
            cxfClient.getOutInterceptors().remove(wsSecurityInterceptor);
            wsSecurityInterceptor = null;
        }

        if (user != null && password != null) {
            Map<String, Object> props = new HashMap<>();
            props.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
            props.put(WSHandlerConstants.USER, user);
            props.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
            props.put(WSHandlerConstants.PW_CALLBACK_REF, new PasswordCallbackHandler(user, password));
            wsSecurityInterceptor = new WSS4JOutInterceptor(props);
            cxfClient.getOutInterceptors().add(wsSecurityInterceptor);
        } else {
            log.warn("AURA_USER / AURA_PASSWORD non configurati in reea_c_parametro: WS-Security disabilitato");
        }

        if (endpoint != null) {
            ((BindingProvider) client)
                .getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
            log.info("AnagrafeGetClient inizializzato con endpoint: {}", endpoint);
        } else {
            log.error("AURA_ENDPOINT_GET non configurato in reea_c_parametro: il servizio Get AURA sarà disabilitato (nessun fallback sul WSDL)");
        }

        this.endpointConfigurato = endpoint != null;
        this.endpointInUso = (String) ((BindingProvider) client)
                .getRequestContext()
                .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
    }

    public SoggettoAuraMsg get(String idAura) {
        if (!endpointConfigurato) {
            throw new IllegalStateException(
                    "AURA_ENDPOINT_GET non configurato in reea_c_parametro: impossibile eseguire la Get AURA");
        }
        log.info("Chiamata AURA Get verso endpoint: {}", endpointInUso);
        return client.getProfiloSanitario(idAura);
    }
}
