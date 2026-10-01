package it.csi.registry.services;

import it.csi.registry.configuratoreDTO.UserDTO;
import it.csi.registry.repositories.AnagraficaRepositoryImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LogoutServiceImpl implements LogoutService {
    private static final Logger log = LoggerFactory.getLogger(LogoutServiceImpl.class);
    private final ParametroService parametri;
    private final AnagraficaRepositoryImpl anagrafica;
    private final AuditService audit;

    public LogoutServiceImpl(ParametroService parametri, AnagraficaRepositoryImpl anagrafica,
                         AuditService audit) {
        this.parametri = parametri;
        this.anagrafica = anagrafica;
        this.audit = audit;
    }

    @Override
    public void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return;

        String username;
        String codiceFiscale;
        try {
            username = (String) session.getAttribute("utente_login");
            UserDTO user = (UserDTO) session.getAttribute("currentUser");
            codiceFiscale = user != null ? user.getCodiceFiscale() : null;
            // Invalidare prima di qualsiasi accesso DB: un errore di audit non deve
            // lasciare aperta la sessione. Vale anche per login PUA e login-dev.
            session.invalidate();
        } catch (IllegalStateException ex) {
            return; // Sessione gia' invalidata da una richiesta concorrente.
        }

        String identity = username != null && !username.isBlank() ? username : codiceFiscale;
        if (codiceFiscale != null && !codiceFiscale.isBlank()) {
            try {
                anagrafica.azzeraUltimoRegistroIdProcessato(codiceFiscale);
            } catch (RuntimeException ex) {
                log.error("Sessione invalidata, reset contatore non riuscito", ex);
            }
        }
        if (identity != null && !identity.isBlank()) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
            if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
            try {
                audit.logLogout(identity, ip);
            } catch (RuntimeException ex) {
                log.error("Sessione invalidata, registrazione audit logout non riuscita", ex);
            }
        }
    }

    @Override
    public String getLogoutUrl() {
        final String value;
        try {
            value = parametri.getValoreByCod("LOGOUT_URL");
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Sessione invalidata, configurazione logout non disponibile", ex);
        }
        // Un valore assente o vuoto indica il solo logout applicativo.
        if (value == null || value.isBlank()) return "";
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("URL HTTP(S) assoluto richiesto");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Sessione invalidata, LOGOUT_URL non valido", ex);
        }
    }
}