import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, delay, map, catchError, switchMap } from 'rxjs/operators';
import { User, RuoloUtente, AuthToken, AuditLogRequest } from '../models';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../../api/api.config';

const INTERNAL_TOKEN = 'internal';
const PROFILO_CONFERMATO_KEY = 'reea_profilo_confermato';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  currentUser = signal<User | null>(null);
  isAuthenticated = signal<boolean>(false);

  // Persiste nel sessionStorage: si azzera al logout / nuovo login PUA
  private _profiloConfermato = signal(
    sessionStorage.getItem(PROFILO_CONFERMATO_KEY) === '1'
  );

  isPseudo = computed(() => {
    const u = this.currentUser();
    return (u?.ruolo ?? '').includes('PSEUDO') || u?.tipoProfiloId === 2;
  });
  isCrpt = computed(() => this.currentUser()?.ruolo === RuoloUtente.OPERATORE_CRPT);
  isSpresalNonPseudo = computed(() => this.currentUser()?.ruolo === RuoloUtente.OPERATORE_SPRESAL);
  isInail = computed(() => this.currentUser()?.ruolo === RuoloUtente.OPERATORE_INAIL);

  // true = mostra scelta profilo; false = accesso diretto
  needsProfiloSelection = computed(() =>
    !this._profiloConfermato() && (this.currentUser()?.profili?.length ?? 0) >= 1
  );

  private confermaProfiloInSessione(): void {
    this._profiloConfermato.set(true);
    sessionStorage.setItem(PROFILO_CONFERMATO_KEY, '1');
  }

  private resetProfiloInSessione(): void {
    this._profiloConfermato.set(false);
    sessionStorage.removeItem(PROFILO_CONFERMATO_KEY);
  }

  /** Login interno con username/password su reea_t_utente — poi mostra scelta profilo */
  loginConCredenziali(username: string, password: string): Observable<void> {
    this.resetProfiloInSessione();
    return this.http.post<{ username: string; ipAddress?: string }>(
      API_ENDPOINTS.login.accedi,
      { username, password },
      { withCredentials: true }
    ).pipe(
      switchMap(() => this.http.get<User>(`${this.apiUrl}/me`, { withCredentials: true })),
      tap(user => {
        this.setCurrentUser(user);
        localStorage.setItem('auth_token', INTERNAL_TOKEN);
      }),
      map(() => void 0)
    );
  }

  /**
   * Login diretto per chi arriva già autenticato via Shibboleth/SPID (es. reea-spid.ruparpiemonte.it),
   * senza passare dal token PUA. Se l'header Shib non è presente sul backend (accesso non tramite
   * quell'ingresso), risponde 401: qui lo trasformiamo in null così l'authGuard può ripiegare su PUA/login
   * senza propagare un errore.
   */
  loginShibboleth(): Observable<AuthToken | null> {
    this.resetProfiloInSessione();
    return this.http.post<AuthToken>(`${this.apiUrl}/login-shib`, {}, { withCredentials: true }).pipe(
      tap(authToken => {
        this.setCurrentUser(authToken.user);
        localStorage.setItem('auth_token', authToken.token);
      }),
      catchError(() => of(null))
    );
  }

  /** Login tramite token del Configuratore (mock o reale) — mostra sempre scelta profilo */
  loginWithToken(token: string, codiceFiscale: string = ''): Observable<AuthToken> {
    this.resetProfiloInSessione();
    if (environment.useMockData) {
      return this.getMockAuthToken().pipe(
        tap(authToken => {
          this.setCurrentUser(authToken.user);
          localStorage.setItem('auth_token', authToken.token);
        })
      );
    }
    return this.http.post<AuthToken>(`${this.apiUrl}/login`, { token, codiceFiscale }).pipe(
      tap(authToken => {
        this.setCurrentUser(authToken.user);
        localStorage.setItem('auth_token', authToken.token);
      })
    );
  }

  /** Login dev: bypassa Configuratore, usa /auth/login-dev sul backend */
  loginDev(codiceFiscale: string): Observable<AuthToken> {
    this.resetProfiloInSessione();
    return this.http.post<AuthToken>(
      `${this.apiUrl}/login-dev`,
      { codiceFiscale },
      { withCredentials: true }
    ).pipe(
      tap(authToken => {
        this.setCurrentUser(authToken.user);
        localStorage.setItem('auth_token', authToken.token);
      })
    );
  }

  loginConToken(token: string, codiceFiscale: string): Observable<AuthToken> {
    this.resetProfiloInSessione();
    return this.http.post<AuthToken>(
      `${this.apiUrl}/login`,
      { token, codiceFiscale },
      { withCredentials: true }
    ).pipe(
      tap(authToken => {
        this.setCurrentUser(authToken.user);
        localStorage.setItem('auth_token', authToken.token);
      })
    );
  }

  selezionaRuolo(ruoloCod: string, collocazioneCod?: string): Observable<User> {
    return this.http.post<User>(
      `${this.apiUrl}/seleziona-ruolo`,
      { ruoloCod, collocazioneCod },
      { withCredentials: true }
    ).pipe(tap(user => this.setCurrentUser(user)));
  }

  selezionaProfilo(profiloCod: string): Observable<User> {
    return this.http.post<User>(
      `${this.apiUrl}/seleziona-profilo`,
      { profiloCod },
      { withCredentials: true }
    ).pipe(
      tap(user => {
        this.setCurrentUser(user);
        this.confermaProfiloInSessione();
      })
    );
  }

  getTipoAuth(): Observable<{ accesso_pua: string }> {
    return this.http.get<{ accesso_pua: string }>(API_ENDPOINTS.login.tipoAuth);
  }

  logout(): Observable<{ logoutUrl: string }> {
    const request$ = environment.useMockData
      ? of({ logoutUrl: '' })
      : this.http.post<{ logoutUrl: string }>(
          API_ENDPOINTS.login.logout, {}, { withCredentials: true }
        );
    return request$.pipe(
      tap(() => {
        this.currentUser.set(null);
        this.isAuthenticated.set(false);
        this.resetProfiloInSessione();
        localStorage.removeItem('auth_token');
      })
    );
  }

  checkAuth(): Observable<User | null> {
    const token = localStorage.getItem('auth_token');
    if (!token) return of(null);

    // Login interno: recupera utente completo dalla sessione backend
    if (token === INTERNAL_TOKEN) {
      return this.http.get<User>(`${this.apiUrl}/me`, { withCredentials: true }).pipe(
        tap(user => this.setCurrentUser(user)),
        catchError(() => {
          localStorage.removeItem('auth_token');
          return of(null);
        })
      );
    }

    if (environment.useMockData) {
      return this.getMockUser().pipe(tap(user => this.setCurrentUser(user)));
    }

    return this.http.get<User>(`${this.apiUrl}/me`).pipe(
      tap(user => this.setCurrentUser(user))
    );
  }

  private setCurrentUser(user: User | null): void {
    this.currentUser.set(user);
    this.isAuthenticated.set(!!user);
  }

  private buildInternalUser(username: string, ipAddress?: string): User {
    return {
      codiceFiscale: username,
      nome: username,
      cognome: '',
      ruolo: RuoloUtente.OPERATORE_CSI, // TEMP: cambia in OPERATORE_CRPT dopo il test

      ipAddress: ipAddress ?? '',
      collocazione: { codice: '', descrizione: '' },
      profili: [{
        codice: 'OPERATORE_CRPT',
        descrizione: 'Operatore CRPT',
        funzionalita: [
          'OP-RIC_ANAG', 'OP-INS_ANAG', 'OP-UPDATE_ANAG',
          'OP-RIC_REG', 'OP-DETT_REG', 'OP-UPDATE_REG',
          'OP-GEST_NOTE', 'OP-RIC_NOTE', 'OP-STORIA_REG',
          'OP-UPLOAD_FILE', 'OP-RICERCA_FILE',
          'OP_EXP_SELEZIONATI', 'OP_EXP_TOTALE', 'OP_EXP_ALLEGATO4'
        ]
      }]
    };
  }

  /**
   * Costruisce l'oggetto auditLogRequest da includere in ogni chiamata API.
   * @param operazione  "read" | "write" | "delete"
   * @param oggOper     Es. "ANAGRAFICA", "REGISTRO"
   * @param keyOper     Descrizione sintetica dei parametri usati
   */
  buildAuditLog(operazione: string, oggOper: string, keyOper: string = ''): AuditLogRequest {
    const user = this.currentUser();
    return {
      idApp: 'REEAFE',
      ipAddress: user?.ipAddress ?? '',
      utente: user?.codiceFiscale ?? '',
      operazione,
      oggOper,
      keyOper,
      uuid: crypto.randomUUID(),
      esitoChiamata: 200,
      loginLogout: false,
      skipPayload: false
    };
  }

  hasRole(ruolo: RuoloUtente): boolean {
    return this.currentUser()?.ruolo === ruolo;
  }

  hasFunzionalita(codice: string): boolean {
    const user = this.currentUser();
    if (!user) return false;
    return user.profili.some(p => p.funzionalita.includes(codice));
  }

  private getMockAuthToken(): Observable<AuthToken> {
    const mockToken: AuthToken = {
      token: 'mock_jwt_token_12345',
      expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000),
      user: {
        codiceFiscale: 'TRNGNNA85T50L219Y',
        nome: 'Gianna',
        cognome: 'Trentasette',
        ruolo: RuoloUtente.OPERATORE_CRPT,
        collocazione: {
          codice: '010301',
          descrizione: 'AZIENDA OSP. CITTA DELLA SALUTE E DELLA SCIENZA DI TORINO',
          codiceAzienda: '0103',
          descrizioneAzienda: 'ASL TO1'
        },
        profili: [{
          codice: 'OPERATORE_CRPT',
          descrizione: 'Operatore CRPT',
          funzionalita: [
            'OP-RIC_ANAG', 'OP-INS_ANAG', 'OP-UPDATE_ANAG',
            'OP-RIC_REG', 'OP-DETT_REG', 'OP-UPDATE_REG',
            'OP-GEST_NOTE', 'OP-RIC_NOTE', 'OP-STORIA_REG',
            'OP-UPLOAD_FILE', 'OP-RICERCA_FILE',
            'OP_EXP_SELEZIONATI', 'OP_EXP_TOTALE', 'OP_EXP_ALLEGATO4'
          ]
        }]
      }
    };
    return of(mockToken).pipe(delay(300));
  }

  private getMockUser(): Observable<User> {
    const mockUser: User = {
      codiceFiscale: 'TRNGNNA85T50L219Y',
      nome: 'Gianna',
      cognome: 'Trentasette',
      ruolo: RuoloUtente.OPERATORE_CRPT,
      collocazione: {
        codice: '010301',
        descrizione: 'AZIENDA OSP. CITTA DELLA SALUTE E DELLA SCIENZA DI TORINO'
      },
      profili: [{
        codice: 'OPERATORE_CRPT',
        descrizione: 'Operatore CRPT',
        funzionalita: [
          'OP-RIC_ANAG', 'OP-INS_ANAG', 'OP-UPDATE_ANAG',
          'OP-RIC_REG', 'OP-DETT_REG', 'OP-UPDATE_REG',
          'OP-GEST_NOTE', 'OP-RIC_NOTE', 'OP-STORIA_REG',
          'OP-UPLOAD_FILE', 'OP-RICERCA_FILE',
          'OP_EXP_SELEZIONATI', 'OP_EXP_TOTALE', 'OP_EXP_ALLEGATO4'
        ]
      }]
    };
    return of(mockUser).pipe(delay(200));
  }
}
