import { Component, OnInit, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ParametriApiService } from '../../../../api/parametri-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-reea-pua',
  standalone: true,
  imports: [],
  template: `
    <div class="pua-container">
      @if (errore()) {
        <div class="pua-error">
          <i class="fas fa-lock"></i>
          <h2>Accesso non autorizzato</h2>
          <p>{{ errore() }}</p>
          <p class="pua-hint">
            Accedere all'applicazione tramite il Portale Unico di Accesso (PUA).
          </p>
        </div>
      } @else {
        <div class="pua-loading">
          <div class="pua-spinner"></div>
          <p>{{ messaggio() }}</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .pua-container {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      background: #f5f7fa;
    }
    .pua-loading {
      text-align: center;
      color: #555;
      p { margin-top: 16px; font-size: 15px; }
    }
    .pua-spinner {
      width: 48px;
      height: 48px;
      border: 4px solid #e0e0e0;
      border-top-color: #1e50a0;
      border-radius: 50%;
      margin: 0 auto;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .pua-error {
      text-align: center;
      color: #555;
      i { font-size: 48px; color: #c0392b; margin-bottom: 16px; display: block; }
      h2 { color: #c0392b; margin-bottom: 8px; }
      p { margin: 4px 0; font-size: 14px; }
      .pua-hint { margin-top: 16px; color: #888; font-size: 13px; }
    }
    .pua-direct-login {
      margin-top: 16px;
      text-align: center;
      a { color: #1e50a0; font-size: 13px; text-decoration: underline; cursor: pointer; }
    }
  `]
})
export class ReeaPuaComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  private readonly parametri = inject(ParametriApiService);
  private readonly document = inject(DOCUMENT);

  errore = signal<string | null>(null);
  messaggio = signal('Autenticazione in corso...');

  ngOnInit(): void {
    this.auth.getTipoAuth().subscribe({
      next: ({ accesso_pua }) => {
        if (accesso_pua !== 'ON') {
          this.errore.set('Accesso non autorizzato attraverso il PUA.');
          return;
        }
        this.processaToken();
      },
      error: () => {
        this.errore.set('Impossibile verificare la configurazione di accesso.');
      }
    });
  }

  private processaToken(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    const cf = this.route.snapshot.queryParamMap.get('cf') ?? '';

    if (!token && !environment.useMockData) {
      // Il ripristino della sessione deve terminare prima di lasciare REEA.
      this.auth.checkAuth().pipe(catchError(() => of(null))).subscribe(user => {
        if (user) {
          this.completaAccesso();
          return;
        }
        // Se Shibboleth ha già autenticato l'utente su questo ingresso (es. reea-spid.ruparpiemonte.it
        // che riporta qui direttamente, senza passare da authGuard), proviamo il login diretto
        // prima di rimandare a PUA.
        this.auth.loginShibboleth().pipe(catchError(() => of(null))).subscribe(shibToken => {
          if (shibToken) {
            this.completaAccesso();
          } else {
            this.apriPortalePua();
          }
        });
      });
      return;
    }

    const tokenDaUsare = token ?? 'mock';

    this.auth.loginWithToken(tokenDaUsare, cf).subscribe({
      next: () => this.completaAccesso(),
      error: () => {
        this.errore.set('Errore durante la verifica del token. Riprovare.');
      }
    });
  }

  private completaAccesso(): void {
    if (this.auth.needsProfiloSelection()) {
      this.router.navigate(['/registro-amianto/scelta-profilo']);
    } else {
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/registro-amianto';
      this.router.navigateByUrl(returnUrl);
    }
  }

  private apriPortalePua(): void {
    this.messaggio.set('Reindirizzamento al Portale Unico di Accesso...');
    this.parametri.getValore('PUA_URL').subscribe({
      next: value => {
        try {
          const url = new URL(value.trim());
          const appUrl = new URL(this.document.baseURI);
          if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password
              || (url.origin === appUrl.origin && url.pathname.startsWith(appUrl.pathname))) {
            throw new Error('URL del portale non valido');
          }
          // Conserva anche il fragment del portale (es. /pua/#/).
          // Non invia al portale token, codice fiscale o returnUrl di REEA.
          this.document.location.replace(url.href);
        } catch {
          this.errore.set('Indirizzo del portale PUA non configurato correttamente. Contattare l\'assistenza.');
        }
      },
      error: () => {
        this.errore.set('Impossibile recuperare l\'indirizzo del portale PUA. Riprovare tra qualche istante.');
      }
    });
  }
}
