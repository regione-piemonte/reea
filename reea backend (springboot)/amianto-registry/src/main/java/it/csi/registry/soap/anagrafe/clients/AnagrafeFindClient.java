package it.csi.registry.soap.anagrafe.clients;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

import it.csi.registry.soap.AnagrafeFind.AnagrafeFind;
import it.csi.registry.soap.AnagrafeFind.AnagrafeFindSoap;
import it.csi.registry.soap.AnagrafeFind.DatiAnagrafici;
import it.csi.registry.soap.AnagrafeFind.DatiAnagraficiMsg;
import it.csi.registry.soap.AnagrafeFind.FindProfiliAnagraficiRequest;
import it.csi.registry.util.PasswordCallbackHandler;
import jakarta.xml.ws.BindingProvider;


public class AnagrafeFindClient {

    private static final Logger log = LoggerFactory.getLogger(AnagrafeFindClient.class);

    private final AnagrafeFindSoap client;
    private final Client cxfClient;

    private volatile String endpointInUso;
    private volatile boolean endpointConfigurato;
    private Interceptor<? extends Message> wsSecurityInterceptor;

    public AnagrafeFindClient(String endpoint, String user, String password) {
        URL wsdlUrl = null;
        try {
            wsdlUrl = getClass().getClassLoader().getResource("wsdl/AURA.WS.AnagrafeFind.wsdl");
            if (wsdlUrl == null) {
                throw new IOException("WSDL non trovato nel classpath: wsdl/AURA.WS.AnagrafeFind.wsdl");
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore caricamento WSDL dal classpath", e);
        }

        AnagrafeFind service = new AnagrafeFind(wsdlUrl);
        this.client = service.getAnagrafeFindSoap();
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
        log.info("AnagrafeFindClient riconfigurato a runtime. endpoint={}, credenzialiPresenti={}",
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
            log.info("AnagrafeFindClient inizializzato con endpoint: {}", endpoint);
        } else {
            log.error("AURA_ENDPOINT_FIND non configurato in reea_c_parametro: il servizio Find AURA sarà disabilitato (nessun fallback sul WSDL)");
        }

        this.endpointConfigurato = endpoint != null;
        this.endpointInUso = (String) ((BindingProvider) client)
                .getRequestContext()
                .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
    }

    private void verificaEndpointConfigurato() {
        if (!endpointConfigurato) {
            throw new IllegalStateException(
                    "AURA_ENDPOINT_FIND non configurato in reea_c_parametro: impossibile eseguire la ricerca AURA");
        }
    }

    public DatiAnagraficiMsg find(String flagDecesso, String codiceFiscale, String nome, String cognome) {
        verificaEndpointConfigurato();
        log.info("Chiamata AURA Find (find) verso endpoint: {}", endpointInUso);
        FindProfiliAnagraficiRequest req = new FindProfiliAnagraficiRequest();
        req.setFlagDecesso(flagDecesso);
        req.setCodiceFiscale(codiceFiscale);
        return client.findProfiliAnagrafici(req);
    }

    public DatiAnagraficiMsg findByAnagrafica(String cognome, String nome, String dataNascita) {
        verificaEndpointConfigurato();
        log.info("Chiamata AURA Find (findByAnagrafica) verso endpoint: {}", endpointInUso);
        FindProfiliAnagraficiRequest req = new FindProfiliAnagraficiRequest();
        req.setFlagDecesso("0");
        req.setCognome(cognome);
        req.setNome(nome);

        if (dataNascita != null && !dataNascita.isBlank()) {
            try {
                LocalDate d = LocalDate.parse(dataNascita);
                req.setDataNascita(d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            } catch (Exception e) {
                req.setDataNascita(dataNascita);
            }
        }

        return client.findProfiliAnagrafici(req);
    }
}
