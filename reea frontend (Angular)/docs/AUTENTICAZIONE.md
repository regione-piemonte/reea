# Autenticazione e Integrazione con il Configuratore CSI (SOLCONFIG)

## Panoramica

Questo documento descrive il lavoro svolto per implementare il sistema di autenticazione dell'applicazione REEAfe / REEAbe, basato sul Configuratore dei Servizi Sanitari Digitali della Regione Piemonte (SOLCONFIG), accessibile tramite il Portale Unico di Accesso (PUA).

---

## Flusso di autenticazione in produzione

```
Operatore → PUA (autenticazione Shibboleth/RUPAR/SPID)
         → sceglie Ruolo, Collocazione, Applicazione (REEA)
         → PUA genera token monouso
         → redirect a: https://app/reeafe/auth/callback?token=TOKEN
         → Frontend invia token al backend
         → Backend chiama Configuratore (Server-to-Server, BasicAuth)
         → Configuratore risponde con profilazione utente
         → Utente accede all'applicazione con il suo ruolo e funzionalità
```

---

## Cosa è stato implementato

### Frontend (Angular 19)

#### Autenticazione

| File | Descrizione |
|------|-------------|
| `src/app/core/services/auth.service.ts` | Servizio centrale di autenticazione. Gestisce login, logout, sessione utente e modalità mock per sviluppo. |
| `src/app/core/guards/auth.guard.ts` | Guard che protegge tutte le rotte: se l'utente non è autenticato, redirige alla pagina di login. |
| `src/app/features/auth/pages/login/` | Pagina di login con form (Codice Fiscale + password). In ambiente di sviluppo esegue un login simulato senza chiamare servizi reali. |
| `src/app/features/auth/pages/callback/` | Pagina di callback PUA. Legge il token dall'URL (`?token=...`) e avvia il processo di autenticazione reale. |

#### Routing

Tutte le rotte dell'applicazione (`/registro-amianto/**`) sono protette dal guard. Le rotte pubbliche sono `/login` e `/auth/callback`.

#### Modalità sviluppo (mock)

Il file `src/environments/environment.test.ts` contiene il flag:
```typescript
useMockData: true
```

Con questo flag attivo:
- Non viene effettuata nessuna chiamata al backend
- L'utente viene autenticato automaticamente con un profilo di test predefinito (Gianna Trentasette, Operatore CRPT)
- Nessuna dipendenza da PUA, Shibboleth o Configuratore

Quando il backend sarà pronto e l'integrazione completata, sarà sufficiente impostare `useMockData: false`.

---

### Backend (Spring Boot)

#### Configurazione

Le credenziali di accesso al Configuratore sono configurate nei file YAML per profilo:

**`application-local.yaml`** (sviluppo locale):
```yaml
configuratore:
  base-path: http://tst-be-srv-solconfig.csi.it/configuratoreapi/api/v1
  username: apisolconfigpreprod
  password: [credenziali CSI]
  codice-servizio: REEA
```

**`application-tst.yaml`** (ambiente di test):
```yaml
configuratore:
  base-path: http://tst-be-srv-solconfig.csi.it/configuratoreapi/api/v1
  username: apisolconfigpreprod
  password: [credenziali CSI]
  codice-servizio: REEA
```

#### Struttura creata

| File | Descrizione |
|------|-------------|
| `auth/dto/UserDTO.java` | Dati utente restituiti al frontend (CF, nome, cognome, ruolo, collocazione, profili). |
| `auth/dto/CollocazioneDTO.java` | Collocazione/azienda dell'utente (codice, descrizione, codice azienda). |
| `auth/dto/ProfiloDTO.java` | Profilo applicativo con lista di funzionalità abilitate. |
| `auth/dto/AuthTokenDTO.java` | Risposta del login: token, scadenza, dati utente. |
| `auth/service/ConfiguratoreService.java` | Chiama l'API del Configuratore CSI (`/login/token-information2`) con autenticazione BasicAuth Server-to-Server. Mappa la risposta nel modello interno dell'applicazione. |
| `auth/controller/AuthController.java` | Espone due endpoint REST: `POST /auth/login` e `GET /auth/me`. |

#### Endpoint esposti dal backend

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| `POST` | `/reeabe/auth/login` | Riceve il token dal frontend, lo verifica sul Configuratore, salva la sessione e restituisce i dati utente. |
| `GET` | `/reeabe/auth/me` | Restituisce i dati dell'utente dalla sessione corrente (usato per ripristinare la sessione al refresh della pagina). |

#### Logica di mapping profilazione

Il Configuratore restituisce una lista piatta di funzionalità con un campo `codiceFunzionalitaPadre`. Il backend raggruppa queste funzionalità per profilo padre, producendo la struttura gerarchica attesa dal frontend:

```
Configuratore (lista piatta):              →  App (struttura gerarchica):
  funzionalita[0]:                              profili[0]:
    codice: "VISUALIZZA_ASSISTITI"                codice: "OPERATORE_CRPT"
    codiceFunzionalitaPadre: "OPERATORE_CRPT"     funzionalita: [
  funzionalita[1]:                                  "VISUALIZZA_ASSISTITI",
    codice: "CREA_ASSISTITO"                        "CREA_ASSISTITO"
    codiceFunzionalitaPadre: "OPERATORE_CRPT"     ]
```

---

## Cosa manca per andare in produzione

Il codice è completo. I passi rimanenti sono **infrastrutturali e burocratici**, da coordinare con CSI Piemonte:

### 1. Registrazione su Shibboleth
L'applicazione deve essere registrata come Service Provider nella community Shibboleth (WRUP per credenziali RUPAR, o GASPRP\_SALUTE per SPID). Shibboleth è il sistema che autentica l'operatore prima ancora che arrivi all'applicazione, aggiungendo il Codice Fiscale come header HTTP.

**Azione**: inviare richiesta a CSI Piemonte con l'URL del server applicativo.

### 2. Registrazione sul Configuratore SOLCONFIG
Il Configuratore deve sapere che l'applicazione REEA esiste, quali ruoli ammette e quali funzionalità ha ogni profilo. Questo si configura lato CSI.

**Azione**: compilare il modulo `Template-Richiesta-Integrazione-Configuratore` e inviarlo a `supporto.fse@csi.it` con:
- URL dell'applicazione
- Codice servizio: `REEA`
- Elenco enti abilitati
- Elenco profili, ruoli e funzionalità

### 3. Utenti di test sul Configuratore
Per effettuare i test di integrazione, CSI fornirà degli utenti di prova già configurati sul Configuratore.

### 4. Deploy su infrastruttura raggiungibile dal PUA
Il backend deve essere raggiungibile dall'ambiente PUA/Shibboleth (tipicamente sulla rete RUPAR o su server CSI).

---

## Attivazione in produzione

Quando l'infrastruttura sarà pronta, l'unica modifica al codice necessaria sarà:

```typescript
// src/environments/environment.ts e environment.test.ts
useMockData: false
```

Dopodiché il flusso reale sarà attivo automaticamente.

---

## Riferimenti

| Documento | Descrizione |
|-----------|-------------|
| `SOLCONFIG--SDI-01-V07-Specifiche di integrazione.pdf` | Specifiche tecniche complete del Configuratore CSI |
| `configuratore.yaml` (in `src/main/resources/yaml-servizi-esterni/`) | Swagger spec per la generazione del client Java del Configuratore |
| `Basic_auth.txt` | Credenziali BasicAuth per ambienti di test e produzione |
