package it.csi.registry.api.controllers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.csi.registry.configuratoreDTO.AuthTokenDTO;
import it.csi.registry.configuratoreDTO.CollocazioneDTO;
import it.csi.registry.configuratoreDTO.ProfiloApplicativoDTO;
import it.csi.registry.configuratoreDTO.UserDTO;
import it.csi.registry.services.ConfiguratoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

	private final ConfiguratoreService configuratoreService;
	private final JdbcTemplate jdbcTemplate;

	public AuthController(ConfiguratoreService configuratoreService, JdbcTemplate jdbcTemplate) {
		this.configuratoreService = configuratoreService;
		this.jdbcTemplate = jdbcTemplate;
	}
	@PostMapping("/login")
	public ResponseEntity<AuthTokenDTO> login(
			@RequestBody Map<String, String> body,
			HttpServletRequest request,
			HttpSession session) {

		String token = body.get("token");
		if (token == null || token.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		// In prod: CF viene da Shibboleth header
		// In dev: lo prendiamo dal token stesso (il Configuratore lo conosce)
		String cfUtente = request.getHeader("Shib-Identita-CodiceFiscale");
		if (cfUtente == null || cfUtente.isBlank()) {
			cfUtente = body.getOrDefault("codiceFiscale", "");
		}

		String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
				.orElse(request.getRemoteAddr());

		UserDTO user = configuratoreService.verificaToken(token, cfUtente, ip);
		if ((user.getProfili() == null || user.getProfili().isEmpty())
				&& user.getCodiceFiscale() != null) {
			enrichFromDb(user);
		}

		String profiloCod = user.getRuolo();
		if (profiloCod != null && !profiloCod.isBlank()) {
			user.setTipoProfiloId(fetchTipoProfiloId(profiloCod));
		}
		// Salva in sessione per /auth/me
		session.setAttribute("currentUser", user);

		AuthTokenDTO authToken = new AuthTokenDTO();
		authToken.setToken(token);                                     // restituiamo lo stesso token PUA
		authToken.setExpiresAt(Instant.now().plus(8, ChronoUnit.HOURS));
		authToken.setUser(user);

		return ResponseEntity.ok(authToken);
	}

	/**
	 * Login diretto per chi arriva già autenticato via Shibboleth/SPID (es. reea-spid.ruparpiemonte.it),
	 * senza passare dal token PUA. Richiede solo l'header Shib-Identita-CodiceFiscale, impostato
	 * dall'Apache/Shibboleth SP davanti a questo endpoint: se manca, non siamo su quell'ingresso
	 * e rispondiamo 401 così il frontend può ripiegare sul flusso PUA.
	 */
	@PostMapping("/login-shib")
	public ResponseEntity<AuthTokenDTO> loginShib(
			HttpServletRequest request,
			HttpSession session) {

		String cfUtente = request.getHeader("Shib-Identita-CodiceFiscale");
		if (cfUtente == null || cfUtente.isBlank()) {
			LOGGER.info("login-shib: header Shib-Identita-CodiceFiscale assente, nessun accesso Shibboleth su questa richiesta");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
				.orElse(request.getRemoteAddr());

		LOGGER.info("login-shib: tentativo accesso Shibboleth per CF={}", cfUtente);

		UserDTO user;
		try {
			user = configuratoreService.verificaAccessoShibboleth(cfUtente, ip);
		} catch (RuntimeException e) {
			LOGGER.error("login-shib: errore verifica accesso Shibboleth per CF={}", cfUtente, e);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if ((user.getProfili() == null || user.getProfili().isEmpty())
				&& user.getCodiceFiscale() != null) {
			enrichFromDb(user);
		}

		String profiloCod = user.getRuolo();
		if (profiloCod != null && !profiloCod.isBlank()) {
			user.setTipoProfiloId(fetchTipoProfiloId(profiloCod));
		}
		session.setAttribute("currentUser", user);

		LOGGER.info("login-shib: accesso riuscito per CF={}, profili={}", cfUtente,
				user.getProfili() != null ? user.getProfili().size() : 0);

		AuthTokenDTO authToken = new AuthTokenDTO();
		authToken.setToken("shib-" + cfUtente);
		authToken.setExpiresAt(Instant.now().plus(8, ChronoUnit.HOURS));
		authToken.setUser(user);

		return ResponseEntity.ok(authToken);
	}

	@GetMapping("/me")
	public ResponseEntity<UserDTO> me(HttpSession session) {
		UserDTO user = (UserDTO) session.getAttribute("currentUser");
		if (user == null) {
			return ResponseEntity.status(401).build();
		}
		return ResponseEntity.ok(user);
	}


	@PostMapping("/login-dev")
	public ResponseEntity<AuthTokenDTO> loginDev(
			@RequestBody Map<String, String> body,
			HttpSession session) {

		String codiceFiscale = body.getOrDefault("codiceFiscale", "AAAAAA00B77B000F");
		String profiloCod = body.get("profiloCod");

	    UserDTO user;
	    try {
	        user = buildDevUserFromDb(codiceFiscale, profiloCod);
	    } catch (Exception e) {
	        return ResponseEntity.badRequest().build();
	    }

	    session.setAttribute("currentUser", user);

	    AuthTokenDTO authToken = new AuthTokenDTO();
	    authToken.setToken("dev-token-" + codiceFiscale);
	    authToken.setExpiresAt(Instant.now().plus(8, ChronoUnit.HOURS));
	    authToken.setUser(user);

	    return ResponseEntity.ok(authToken);
	}
	// ── ENDPOINT SOLO PER SVILUPPO LOCALE ────────────────────────────────────


	private void enrichFromDb(UserDTO user) {
		String cf = user.getCodiceFiscale();
		if (cf == null || cf.isBlank()) return;

		try {
			String sql = """
    SELECT u.nome, u.cognome,
           p.profilo_cod, p.profilo_desc, p.profilo_tipo_id,
           c.collocazione_cod, c.collocazione_desc,
           a.asl_cod, a.asl_azienda_desc
    FROM reea.reea_t_utente u
    JOIN reea.reea_r_utente_profilo up ON up.utente_id = u.utente_id AND up.data_cancellazione IS NULL
    JOIN reea.reea_d_profilo p ON p.profilo_id = up.profilo_id AND p.data_cancellazione IS NULL
    LEFT JOIN reea.reea_r_utente_collocazione uc ON uc.utente_id = u.utente_id AND uc.data_cancellazione IS NULL
    LEFT JOIN reea.reea_t_collocazione c ON c.collocazione_id = uc.collocazione_id AND c.data_cancellazione IS NULL
    LEFT JOIN reea.reea_d_asl a ON a.asl_id = c.azienda_id
    WHERE u.codice_fiscale = ? AND u.data_cancellazione IS NULL
    LIMIT 1
""";

			List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, cf);
			if (rows.isEmpty()) return;

			Map<String, Object> row = rows.get(0);

			// nome/cognome — solo se il Configuratore non li ha già forniti
			if (user.getNome() == null || user.getNome().isBlank()) {
				user.setNome((String) row.get("nome"));
			}
			if (user.getCognome() == null || user.getCognome().isBlank()) {
				user.setCognome((String) row.get("cognome"));
			}

			CollocazioneDTO col = new CollocazioneDTO();
			col.setCodice((String) row.get("collocazione_cod"));
			col.setDescrizione((String) row.get("collocazione_desc"));
			col.setCodiceAzienda((String) row.get("asl_cod"));
			col.setDescrizioneAzienda((String) row.get("asl_azienda_desc"));
			user.setCollocazione(col);

			ProfiloApplicativoDTO profilo = new ProfiloApplicativoDTO();
			profilo.setCodice((String) row.get("profilo_cod"));
			profilo.setDescrizione((String) row.get("profilo_desc"));
			profilo.setFunzionalita(List.of());
			user.setProfili(List.of(profilo));
			user.setRuolo((String) row.get("profilo_cod"));

			// tipoProfiloId
			Object tipoId = row.get("profilo_tipo_id");
			if (tipoId != null) {
				user.setTipoProfiloId(((Number) tipoId).intValue());
			}

		} catch (Exception e) {
			// fallback silenzioso
		}
	}

	private UserDTO buildDevUserFromDb(String cf, String profiloCod) {
		String sqlUtente = """
        SELECT u.utente_id, u.nome, u.cognome, u.codice_fiscale,
               p.profilo_cod, p.profilo_desc, p.profilo_tipo_id,
               c.collocazione_cod, c.collocazione_desc,
               a.asl_cod, a.asl_azienda_desc
        FROM reea.reea_t_utente u
        JOIN reea.reea_r_utente_profilo up ON up.utente_id = u.utente_id AND up.data_cancellazione IS NULL
        JOIN reea.reea_d_profilo p ON p.profilo_id = up.profilo_id AND p.data_cancellazione IS NULL
        LEFT JOIN reea.reea_r_utente_collocazione uc ON uc.utente_id = u.utente_id AND uc.data_cancellazione IS NULL
        LEFT JOIN reea.reea_t_collocazione c ON c.collocazione_id = uc.collocazione_id AND c.data_cancellazione IS NULL
        LEFT JOIN reea.reea_d_asl a ON a.asl_id = c.azienda_id
        WHERE u.codice_fiscale = ? AND u.data_cancellazione IS NULL
        """ + (profiloCod != null ? "AND p.profilo_cod = ?" : "LIMIT 1");

		List<Map<String, Object>> utenteRows = profiloCod != null
				? jdbcTemplate.queryForList(sqlUtente, cf, profiloCod)
				: jdbcTemplate.queryForList(sqlUtente, cf);

		if (utenteRows.isEmpty()) throw new RuntimeException("Utente non trovato: " + cf);

		Map<String, Object> first = utenteRows.get(0);

		// funzionalità direttamente da profilo→funzionalita (CDU par. 7.5)
		String profCod = profiloCod != null ? profiloCod : (String) first.get("profilo_cod");
		String sqlFunz = """
        SELECT DISTINCT f.funzionalita_cod
        FROM reea.reea_d_profilo p
        JOIN reea.reea_r_profilo_funzionalita pf ON pf.profilo_id = p.profilo_id AND pf.data_cancellazione IS NULL
        JOIN reea.reea_t_funzionalita f ON f.funzionalita_id = pf.funzionalita_id AND f.data_cancellazione IS NULL
        WHERE p.profilo_cod = ? AND p.data_cancellazione IS NULL
        """;
		List<String> funzionalita = jdbcTemplate.queryForList(sqlFunz, String.class, profCod);

		CollocazioneDTO col = new CollocazioneDTO();
		col.setCodice((String) first.get("collocazione_cod"));
		col.setDescrizione((String) first.get("collocazione_desc"));
		col.setCodiceAzienda((String) first.get("asl_cod"));           // FIX: era "azienda_cod"
		col.setDescrizioneAzienda((String) first.get("asl_azienda_desc")); // FIX: era "azienda_desc"

		ProfiloApplicativoDTO profilo = new ProfiloApplicativoDTO();
		profilo.setCodice((String) first.get("profilo_cod"));
		profilo.setDescrizione((String) first.get("profilo_desc"));
		profilo.setFunzionalita(funzionalita);

		UserDTO user = new UserDTO();
		user.setCodiceFiscale((String) first.get("codice_fiscale"));
		user.setNome((String) first.get("nome"));
		user.setCognome((String) first.get("cognome"));
		user.setRuolo((String) first.get("profilo_cod"));
		user.setCollocazione(col);
		user.setProfili(List.of(profilo));

		// FIX: senza utenteId, selezionaRuolo prende il branch PUA
		Object uid = first.get("utente_id");
		if (uid != null) user.setUtenteId(((Number) uid).longValue());

		Object tipoId = first.get("profilo_tipo_id");
		if (tipoId != null) user.setTipoProfiloId(((Number) tipoId).intValue());

		return user;
	}


//	private UserDTO buildDevUserFromDb(String cf, String profiloCod) {
//		String sqlUtente = """
//    SELECT u.utente_id, u.nome, u.cognome, u.codice_fiscale,
//           p.profilo_cod, p.profilo_desc, p.profilo_tipo_id,
//           c.collocazione_cod, c.collocazione_desc,
//           a.asl_cod, a.asl_azienda_desc
//    FROM reea.reea_t_utente u
//    JOIN reea.reea_r_utente_profilo up ON up.utente_id = u.utente_id AND up.data_cancellazione IS NULL
//    JOIN reea.reea_d_profilo p ON p.profilo_id = up.profilo_id AND p.data_cancellazione IS NULL
//    LEFT JOIN reea.reea_r_utente_collocazione uc ON uc.utente_id = u.utente_id AND uc.data_cancellazione IS NULL
//    LEFT JOIN reea.reea_t_collocazione c ON c.collocazione_id = uc.collocazione_id AND c.data_cancellazione IS NULL
//    LEFT JOIN reea.reea_d_asl a ON a.asl_id = c.azienda_id
//    WHERE u.codice_fiscale = ? AND u.data_cancellazione IS NULL
//    """ + (profiloCod != null ? "AND p.profilo_cod = ?" : "LIMIT 1");
//
//
//		List<Map<String, Object>> utenteRows = profiloCod != null
//				? jdbcTemplate.queryForList(sqlUtente, cf, profiloCod)
//				: jdbcTemplate.queryForList(sqlUtente, cf);
//
//		if (utenteRows.isEmpty()) throw new RuntimeException("Utente non trovato: " + cf);
//
//		// Poi prendi le funzionalità (può essere vuota)
//		String sqlFunz = """
//				SELECT DISTINCT f.funzionalita_cod
//		 FROM reea.reea_d_ruolo r
//		JOIN reea.reea_r_ruolo_profilo rp ON rp.ruolo_id = r.ruolo_id AND rp.data_cancellazione IS NULL
//		JOIN reea.reea_d_profilo p ON p.profilo_id = rp.profilo_id AND p.data_cancellazione IS NULL
//		JOIN reea.reea_r_profilo_funzionalita pf ON pf.profilo_id = p.profilo_id AND pf.data_cancellazione IS NULL
//		JOIN reea.reea_t_funzionalita f ON f.funzionalita_id = pf.funzionalita_id AND f.data_cancellazione IS NULL
//		 WHERE r.ruolo_cod = ? AND r.data_cancellazione IS NULL
//    """ + (profiloCod != null ? "AND p.profilo_cod = ?" : "");
//
//		List<String> funzionalita = profiloCod != null
//				? jdbcTemplate.queryForList(sqlFunz, String.class, cf, profiloCod)
//				: jdbcTemplate.queryForList(sqlFunz, String.class, cf);
//
//		Map<String, Object> first = utenteRows.get(0);
//
//		CollocazioneDTO col = new CollocazioneDTO();
//		col.setCodice((String) first.get("collocazione_cod"));
//		col.setDescrizione((String) first.get("collocazione_desc"));
//		col.setCodiceAzienda((String) first.get("asl_cod"));
//		col.setDescrizioneAzienda((String) first.get("asl_azienda_desc"));
//
//		ProfiloApplicativoDTO profilo = new ProfiloApplicativoDTO();
//		profilo.setCodice((String) first.get("profilo_cod"));
//		profilo.setDescrizione((String) first.get("profilo_desc"));
//		profilo.setFunzionalita(funzionalita);
//
//		UserDTO user = new UserDTO();
//		user.setCodiceFiscale((String) first.get("codice_fiscale"));
//		user.setNome((String) first.get("nome"));
//		user.setCognome((String) first.get("cognome"));
//		user.setRuolo((String) first.get("profilo_cod"));
//		user.setCollocazione(col);
//		user.setProfili(List.of(profilo));
//
//		Object tipoId = first.get("profilo_tipo_id");
//		if (tipoId != null) {
//			user.setTipoProfiloId(((Number) tipoId).intValue());
//		}
//
//		return user;
//	}

	private Integer fetchTipoProfiloId(String profiloCod) {
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(
					"SELECT profilo_tipo_id FROM reea.reea_d_profilo WHERE profilo_cod = ? AND data_cancellazione IS NULL LIMIT 1",
					profiloCod
			);
			if (!rows.isEmpty() && rows.get(0).get("profilo_tipo_id") != null) {
				return ((Number) rows.get(0).get("profilo_tipo_id")).intValue();
			}
		} catch (Exception e) { /* fallback silenzioso */ }
		return null;
	}


	@PostMapping("/seleziona-profilo")
	public ResponseEntity<UserDTO> selezionaProfilo(
			@RequestBody Map<String, String> body,
			HttpSession session) {

		UserDTO user = (UserDTO) session.getAttribute("currentUser");
		if (user == null) return ResponseEntity.status(401).build();

		String profiloCod = body.get("profiloCod");
		if (profiloCod == null || profiloCod.isBlank()) return ResponseEntity.badRequest().build();

		// Verifica che il profilo scelto sia tra quelli caricati in seleziona-ruolo
		Optional<ProfiloApplicativoDTO> selected = user.getProfili().stream()
				.filter(p -> profiloCod.equals(p.getCodice()))
				.findFirst();
		if (selected.isEmpty()) return ResponseEntity.badRequest().build();

		// GetAbilitazioni (CDU par. 7.5): funzionalità associate al profilo scelto
		String sqlFunz = """
        SELECT DISTINCT f.funzionalita_cod
        FROM reea.reea_d_profilo p
        JOIN reea.reea_r_profilo_funzionalita pf
            ON pf.profilo_id = p.profilo_id AND pf.data_cancellazione IS NULL
        JOIN reea.reea_t_funzionalita f
            ON f.funzionalita_id = pf.funzionalita_id AND f.data_cancellazione IS NULL
        WHERE p.profilo_cod = ? AND p.data_cancellazione IS NULL
    """;
		List<String> funzionalita = jdbcTemplate.queryForList(sqlFunz, String.class, profiloCod);

		// Caso PUA: il DB non ha funzionalità per questo profilo → preserva quelle del Configuratore
		if (funzionalita.isEmpty()) {
			List<String> existing = selected.get().getFunzionalita();
			if (existing != null && !existing.isEmpty()) {
				funzionalita = existing;
			}
		}

		// GetAbilitazioni (CDU par. 7.5): tipo profilo (1=dati in chiaro, 2=pseudonimizzati)
		Integer tipoProfiloId = null;
		try {
			List<Map<String, Object>> tipoRows = jdbcTemplate.queryForList(
					"SELECT profilo_tipo_id FROM reea.reea_d_profilo WHERE profilo_cod = ? AND data_cancellazione IS NULL LIMIT 1",
					profiloCod
			);
			if (!tipoRows.isEmpty() && tipoRows.get(0).get("profilo_tipo_id") != null) {
				tipoProfiloId = ((Number) tipoRows.get(0).get("profilo_tipo_id")).intValue();
			}
		} catch (Exception ignored) {}

		ProfiloApplicativoDTO profilo = selected.get();
		profilo.setFunzionalita(funzionalita);

		// ruolo = ruolo scelto nello step 1 (salvato in sessione da seleziona-ruolo)
		user.setRuolo(profiloCod);
		user.setTipoProfiloId(tipoProfiloId);
		user.setProfili(List.of(profilo));

		session.setAttribute("currentUser", user);
		return ResponseEntity.ok(user);
	}




	@PostMapping("/seleziona-ruolo")
	public ResponseEntity<UserDTO> selezionaRuolo(
			@RequestBody Map<String, String> body,
			HttpSession session) {

		UserDTO user = (UserDTO) session.getAttribute("currentUser");
		if (user == null) return ResponseEntity.status(401).build();

		String ruoloCod        = body.get("ruoloCod");
		String collocazioneCod = body.get("collocazioneCod");
		if (ruoloCod == null || ruoloCod.isBlank()) return ResponseEntity.badRequest().build();

		// Imposta collocazione attiva cercando nelle collocazioni del ruolo scelto
		// Sostituisci il blocco "Imposta collocazione attiva" con questo:
		if (collocazioneCod != null) {
			if (user.getUtenteId() != null) {
				// Login interno: collocazioni a livello utente (CDU par. 7.3)
				if (user.getCollocazioni() != null) {
					user.getCollocazioni().stream()
							.filter(c -> collocazioneCod.equals(c.getCodice()))
							.findFirst()
							.ifPresent(user::setCollocazione);
				}
			} else {
				// PUA: collocazioni nested nel profilo dal Configuratore
				user.getProfili().stream()
						.filter(p -> ruoloCod.equals(p.getCodice()))
						.findFirst()
						.flatMap(p -> p.getCollocazioni() == null ? java.util.Optional.empty() :
								p.getCollocazioni().stream()
										.filter(c -> collocazioneCod.equals(c.getCodice()))
										.findFirst())
						.ifPresent(user::setCollocazione);
			}
		}

		List<ProfiloApplicativoDTO> profili = new ArrayList<>();

		if (user.getUtenteId() != null) {
			// Login user+password: carica profili assegnati direttamente all'utente (CDU par. 7.4)
			String sqlProfili = """
            SELECT DISTINCT p.profilo_cod, p.profilo_desc
            FROM reea.reea_r_utente_profilo up
            JOIN reea.reea_d_profilo p ON p.profilo_id = up.profilo_id
                AND p.data_cancellazione IS NULL
            WHERE up.utente_id = ?
              AND up.data_cancellazione IS NULL
        """;
			List<Map<String, Object>> profiloRows = jdbcTemplate.queryForList(sqlProfili, user.getUtenteId());

			for (Map<String, Object> row : profiloRows) {
				ProfiloApplicativoDTO p = new ProfiloApplicativoDTO();
				p.setCodice((String) row.get("profilo_cod"));
				p.setDescrizione((String) row.get("profilo_desc"));
				p.setFunzionalita(new ArrayList<>());
				profili.add(p);
			}
		} else {
			// Login PUA: profili già caricati dal Configuratore, usa quello corrispondente al ruolo scelto
			user.getProfili().stream()
					.filter(p -> ruoloCod.equals(p.getCodice()))
					.findFirst()
					.ifPresent(profili::add);
		}

		user.setProfili(profili);
		session.setAttribute("ruoloScelto", ruoloCod);
		session.setAttribute("currentUser", user);
		return ResponseEntity.ok(user);
	}





}
