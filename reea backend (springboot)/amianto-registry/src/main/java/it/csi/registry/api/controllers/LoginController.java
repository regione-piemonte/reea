package it.csi.registry.api.controllers;

import it.csi.registry.configuratoreDTO.CollocazioneDTO;
import it.csi.registry.configuratoreDTO.ProfiloApplicativoDTO;
import it.csi.registry.configuratoreDTO.UserDTO;
import it.csi.registry.model.LoginRequestDTO;
import it.csi.registry.model.LoginResponseDTO;
import it.csi.registry.repositories.AnagraficaRepositoryImpl;
import it.csi.registry.services.LogoutService;
import it.csi.registry.services.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    private final DSLContext dsl;
    private final LogoutService logoutService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final AnagraficaRepositoryImpl anagraficaRepositoryImpl;


    public LoginController(DSLContext dsl, AuditService auditService,  JdbcTemplate jdbcTemplate, AnagraficaRepositoryImpl anagraficaRepositoryImpl, LogoutService logoutService) {
        this.dsl = dsl;
        this.logoutService = logoutService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
        this.anagraficaRepositoryImpl = anagraficaRepositoryImpl;
    }
    


    @PostMapping("/accedi")
    public ResponseEntity<?> accedi(@RequestBody LoginRequestDTO request,
                                    HttpServletRequest httpServletRequest,
                                    HttpSession session) {
        if (request.getUsername() == null || request.getPassword() == null
                || request.getUsername().isBlank() || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Username e password obbligatori");
        }

        String ipClient = getClientIp(httpServletRequest);

        Record record = dsl.fetchOne(
                "SELECT utente_user FROM reea.reea_t_utente " +
                        "WHERE utente_user = {0} AND utente_pwd = crypt({1}, utente_pwd)",
                request.getUsername().trim(), request.getPassword()
        );

        if (record == null) {
            auditService.logLoginFailed(request.getUsername().trim(), ipClient);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Username e/o password non validi");
        }

        auditService.logLogin(request.getUsername().trim(), ipClient);

        UserDTO userDTO = buildUserFromUsername(request.getUsername().trim());

        session.setAttribute("utente_login", request.getUsername().trim());
        session.setAttribute("currentUser", userDTO);
        
        anagraficaRepositoryImpl.azzeraUltimoRegistroIdProcessato(userDTO.getCodiceFiscale());

        LoginResponseDTO resp = new LoginResponseDTO();
        resp.setUsername(request.getUsername().trim());
        resp.setIpAddress(ipClient);
        return ResponseEntity.ok(resp);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr(); // fallback
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private UserDTO buildUserFromUsername(String username) {

        // ── 1. GetRuoliUtente (CDU par. 7.2) ──────────────────────────────────
        String sqlRuoli = """
        SELECT u.utente_id, u.codice_fiscale, u.nome, u.cognome,
               r.ruolo_cod, r.ruolo_desc
        FROM reea.reea_t_utente u
        JOIN reea.reea_r_utente_ruolo ur ON ur.utente_id = u.utente_id
            AND ur.data_cancellazione IS NULL
        JOIN reea.reea_d_ruolo r ON r.ruolo_id = ur.ruolo_id
            AND r.data_cancellazione IS NULL
        WHERE u.utente_user = ? AND u.data_cancellazione IS NULL
    """;
        List<Map<String, Object>> ruoliRows = jdbcTemplate.queryForList(sqlRuoli, username);

        UserDTO user = new UserDTO();
        if (ruoliRows.isEmpty()) return user;

        Map<String, Object> first = ruoliRows.get(0);
        Object uid = first.get("utente_id");
        if (uid != null) user.setUtenteId(((Number) uid).longValue());
        user.setCodiceFiscale((String) first.get("codice_fiscale"));
        user.setNome((String) first.get("nome"));
        user.setCognome((String) first.get("cognome"));

        List<ProfiloApplicativoDTO> profili = new ArrayList<>();
        for (Map<String, Object> row : ruoliRows) {
            String ruoloCod = (String) row.get("ruolo_cod");
            if (ruoloCod == null) continue;
            boolean exists = profili.stream().anyMatch(p -> ruoloCod.equals(p.getCodice()));
            if (!exists) {
                ProfiloApplicativoDTO p = new ProfiloApplicativoDTO();
                p.setCodice(ruoloCod);
                p.setDescrizione((String) row.get("ruolo_desc"));
                p.setFunzionalita(new ArrayList<>());
                p.setCollocazioni(new ArrayList<>());
                profili.add(p);
            }
        }
        user.setProfili(profili);

        // ── 2. GetCollocazioniUtente (CDU par. 7.3: join su utente_id) ─────────
        if (uid != null) {
            String sqlColl = """
            SELECT c.collocazione_cod, c.collocazione_desc,
                   a.asl_cod, a.asl_azienda_desc
            FROM reea.reea_r_utente_collocazione uc
            JOIN reea.reea_t_collocazione c ON c.collocazione_id = uc.collocazione_id
                AND c.data_cancellazione IS NULL
            LEFT JOIN reea.reea_d_asl a ON a.asl_id = c.azienda_id
            WHERE uc.utente_id = ? AND uc.data_cancellazione IS NULL
        """;
            List<Map<String, Object>> collRows = jdbcTemplate.queryForList(sqlColl, user.getUtenteId());

            List<CollocazioneDTO> collocazioni = new ArrayList<>();
            for (Map<String, Object> row : collRows) {
                CollocazioneDTO col = new CollocazioneDTO();
                col.setCodice((String) row.get("collocazione_cod"));
                col.setDescrizione((String) row.get("collocazione_desc"));
                col.setCodiceAzienda((String) row.get("asl_cod"));
                col.setDescrizioneAzienda((String) row.get("asl_azienda_desc"));
                collocazioni.add(col);
            }
            user.setCollocazioni(collocazioni);
        }

        // ruolo e collocazione attiva restano null finché l'utente non sceglie in scelta-profilo
        return user;
    }



    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        logoutService.invalidateSession(request);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(Map.of("logoutUrl", logoutService.getLogoutUrl()));
    }

    // Notifica tramite browser: nessun redirect al provider, per evitare loop.
    @GetMapping("/logout")
    public ResponseEntity<Void> logoutLocale(HttpServletRequest request) {
        logoutService.invalidateSession(request);
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        String username = (String) session.getAttribute("utente_login");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDTO user = (UserDTO) session.getAttribute("currentUser");
        if (user == null) {
            // fallback: ricostruisci da DB
            user = buildUserFromUsername(username);
            session.setAttribute("currentUser", user);
        }
        return ResponseEntity.ok(user);
    }


    @GetMapping("/tipo-auth")
    public ResponseEntity<Map<String, String>> getTipoAuth() {
        Record record = dsl.fetchOne(
                "SELECT parametro_valore FROM reea.reea_c_parametro WHERE parametro_tipo_id = 6"
        );
        String valore = record != null ? record.get("parametro_valore", String.class) : "OFF";
        return ResponseEntity.ok(Map.of("accesso_pua", valore));
    }

}
