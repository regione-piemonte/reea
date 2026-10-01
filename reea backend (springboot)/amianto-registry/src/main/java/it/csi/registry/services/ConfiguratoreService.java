package it.csi.registry.services;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import it.csi.registry.client.configuratore.api.ConfiguratoreApiApi;
import it.csi.registry.client.configuratore.api.DefaultApi;
import it.csi.registry.client.configuratore.invoker.ApiClient;
import it.csi.registry.client.configuratore.invoker.ApiException;
import it.csi.registry.client.configuratore.model.AbilitazioneCollocazioneProfili;
import it.csi.registry.client.configuratore.model.Funzionalita;
import it.csi.registry.client.configuratore.model.ModelCollocazione;
import it.csi.registry.client.configuratore.model.ModelTokenInformazione;
import it.csi.registry.client.configuratore.model.Richiedente;
import it.csi.registry.client.configuratore.model.UtenteProfilo;
import it.csi.registry.configuratoreDTO.CollocazioneDTO;
import it.csi.registry.configuratoreDTO.ProfiloApplicativoDTO;
import it.csi.registry.configuratoreDTO.UserDTO;
import it.csi.registry.model.AdesioneDTO;
import it.csi.registry.util.ExcelFileUtils.FileSalvato;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConfiguratoreService {

    private static final String BASE_URL_PARAMETRO = "CONFIGURATORE_BASE_URL";
    private static final String USER_PARAMETRO = "CONFIGURATORE_USER";
    private static final String PASSWORD_PARAMETRO = "CONFIGURATORE_PASSWORD";
    private final ParametroService parametroService;

    public ConfiguratoreService(ParametroService parametroService) {
        this.parametroService = parametroService;
    }

	@Value("${configuratore.codice-servizio:REEA}")
	private String codiceServizio;

	public UserDTO verificaToken(String token, String cfUtente, String ipClient) {
		ApiClient apiClient = new ApiClient();
		apiClient.setBasePath(getBasePath());
		apiClient.setUsername(getValoreObbligatorio(USER_PARAMETRO));
		apiClient.setPassword(getValoreObbligatorio(PASSWORD_PARAMETRO));


// nel metodo verificaToken:
		com.fasterxml.jackson.databind.ObjectMapper mapper = apiClient.getJSON().getContext(null);
		mapper.addMixIn(Richiedente.class,             ConfiguratoreMixins.RichiedenteMixin.class);
		mapper.addMixIn(ModelCollocazione.class,       ConfiguratoreMixins.ModelCollocazioneMixin.class);
		mapper.addMixIn(Funzionalita.class,            ConfiguratoreMixins.FunzionalitaMixin.class);
		mapper.addMixIn(ModelTokenInformazione.class,  ConfiguratoreMixins.ModelTokenInformazioneMixin.class);

		DefaultApi api = new DefaultApi(apiClient);
		try {
			ModelTokenInformazione info = api.proxyTokenInformationGet1(
					cfUtente,
					UUID.randomUUID().toString(),
					ipClient,
					codiceServizio,
					token
			);
			return mapToUserDTO(info);
		} catch (ApiException e) {
			throw new RuntimeException("Token non valido o scaduto: " + e.getCode(), e);
		}
	}

    /**
     * Login diretto per gli utenti che arrivano già autenticati via Shibboleth/SPID
     * (es. reea-spid.ruparpiemonte.it), senza passare da un token PUA.
     * A differenza di verificaToken(), non richiede alcun token: interroga il Configuratore
     * solo con il codice fiscale, usando le credenziali di servizio di REEA (basicAuth).
     *
     * ATTENZIONE: i valori di codiceAzienda ("" = nessun filtro) e codiceApplicazione
     * (riusato codiceServizio) sono un'ipotesi basata sulla firma del client generato,
     * non verificata con la documentazione del Configuratore. Da confermare con loro
     * prima del rilascio in produzione.
     */
    public UserDTO verificaAccessoShibboleth(String cfUtente, String ipClient) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(getBasePath());
        apiClient.setUsername(getValoreObbligatorio(USER_PARAMETRO));
        apiClient.setPassword(getValoreObbligatorio(PASSWORD_PARAMETRO));

        ConfiguratoreApiApi api = new ConfiguratoreApiApi(apiClient);
        try {
            UtenteProfilo info = api.profiliAbilitazioniGet1(
                    cfUtente,
                    UUID.randomUUID().toString(),
                    ipClient,
                    codiceServizio,
                    cfUtente,
                    "0",
                    "50",
                    "",
                    codiceServizio
            );
            return mapUtenteProfiloToUserDTO(info);
        } catch (ApiException e) {
            throw new RuntimeException("Accesso Shibboleth non autorizzato dal Configuratore: " + e.getCode(), e);
        }
    }

    private UserDTO mapUtenteProfiloToUserDTO(UtenteProfilo info) {
        if (info == null || info.getCodiceFiscale() == null) {
            throw new RuntimeException("Risposta Configuratore senza codice fiscale");
        }

        List<AbilitazioneCollocazioneProfili> abilitazioni =
                (info.getAbilitazioni() != null && info.getAbilitazioni().getListaRis() != null)
                        ? info.getAbilitazioni().getListaRis()
                        : List.of();

        // Nota: a differenza del flusso a token, qui il Configuratore non restituisce le
        // "funzionalita" associate al profilo: restano vuote e vengono eventualmente
        // recuperate da DB locale tramite enrichFromDb() lato AuthController, come già
        // avviene oggi per il flusso a token quando i profili risultano vuoti.
        List<ProfiloApplicativoDTO> profili = abilitazioni.stream()
                .map(AbilitazioneCollocazioneProfili::getProfilo)
                .filter(Objects::nonNull)
                .map(p -> {
                    ProfiloApplicativoDTO dto = new ProfiloApplicativoDTO();
                    dto.setCodice(p.getCodice());
                    dto.setDescrizione(p.getDescrizione());
                    dto.setFunzionalita(new ArrayList<>());
                    return dto;
                })
                .collect(Collectors.toList());

        CollocazioneDTO colDTO = new CollocazioneDTO();
        abilitazioni.stream()
                .map(AbilitazioneCollocazioneProfili::getCollocazione)
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(c -> {
                    colDTO.setCodice(c.getCollocazioneCodice());
                    colDTO.setDescrizione(c.getCollocazioneDescrizione());
                    colDTO.setCodiceAzienda(c.getCollocazioneCodiceAzienda());
                    colDTO.setDescrizioneAzienda(c.getCollocazioneDescrizioneAzienda());
                });

        UserDTO user = new UserDTO();
        user.setCodiceFiscale(info.getCodiceFiscale());
        user.setNome(info.getNome());
        user.setCognome(info.getCognome());
        user.setCollocazione(colDTO);
        user.setProfili(profili);
        user.setRuolo(profili.isEmpty() ? null : profili.get(0).getCodice());
        return user;
    }

    private String getValoreObbligatorio(String cod) {
        final String value;
        try {
            value = parametroService.getValoreByCod(cod);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configurazione Configuratore non disponibile", ex);
        }
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Parametro DB " + cod + " non valorizzato");
        }
        return value;
    }

    private String getBasePath() {
        try {
            URI uri = URI.create(getValoreObbligatorio(BASE_URL_PARAMETRO).trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("URL base HTTP(S) assoluto richiesto");
            }
            // Il client aggiunge il percorso dell'operazione all'URL base.
            return uri.toASCIIString().replaceAll("/+$", "");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Parametro DB CONFIGURATORE_BASE_URL non valido", ex);
        }
    }

	private UserDTO mapToUserDTO(ModelTokenInformazione info) {
		var r = info.getRichiedente();
		if (r == null) throw new RuntimeException("Risposta Configuratore senza richiedente");

		// ── COLLOCAZIONE ──────────────────────────────────────────────────
		CollocazioneDTO colDTO = new CollocazioneDTO();
		if (r.getCollocazione() != null) {
			var col = r.getCollocazione();
			colDTO.setCodice(col.getCodiceCollocazione());
			colDTO.setDescrizione(col.getDescrizioneCollocazione()); // → ASL header
			colDTO.setCodiceAzienda(col.getCodiceAzienda());
			colDTO.setDescrizioneAzienda(col.getDescrizioneAzienda());
		} else {
			System.out.println(">>> [WARN] getCollocazione() è NULL su Richiedente");
		}

		// ── PROFILI (da descrizioneFunzionalitaPadre) ─────────────────────
		List<ProfiloApplicativoDTO> profili = List.of();
		var funzList = info.getFunzionalita();
		if (funzList != null) {
			Map<String, List<String>> gruppi = new LinkedHashMap<>();
			Map<String, String> descPadre = new LinkedHashMap<>();
			for (var f : funzList) {
				String padre = f.getCodiceFunzionalitaPadre();
				if (padre == null || padre.isBlank()) continue;
				gruppi.computeIfAbsent(padre, k -> new ArrayList<>()).add(f.getCodice());
				descPadre.putIfAbsent(padre, f.getDescrizioneFunzionalitaPadre()); // → Profilo header
			}
			profili = gruppi.entrySet().stream().map(e -> {
				ProfiloApplicativoDTO p = new ProfiloApplicativoDTO();
				p.setCodice(e.getKey());                        // REEA_OP_CRPT
				p.setDescrizione(descPadre.get(e.getKey()));    // Operatore CRPT
				p.setFunzionalita(e.getValue());
				return p;
			}).collect(Collectors.toList());
		} else {
			System.out.println(">>> [WARN] getFunzionalita() è NULL su ModelTokenInformazione");
		}

		System.out.println(">>> collocazione.descrizione: " + colDTO.getDescrizione());
		System.out.println(">>> profili count: " + profili.size());

		UserDTO user = new UserDTO();
		user.setCodiceFiscale(r.getCodiceFiscale());
		user.setNome(r.getNome());
		user.setCognome(r.getCognome());
		user.setRuolo(profili.isEmpty() ? r.getRuolo() : profili.get(0).getCodice());
		user.setCollocazione(colDTO);
		user.setProfili(profili);
		return user;
	}



}

