import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  template: `
    <div class="callback-container">
      @if (errore()) {
        <div class="callback-error">
          <i class="fas fa-lock"></i>
          <h2>Accesso non autorizzato</h2>
          <p>{{ errore() }}</p>
          <p class="callback-hint">
            Accedere all'applicazione tramite il Portale Unico di Accesso (PUA).
          </p>
        </div>
      } @else {
        <div class="callback-loading">
          <div class="callback-spinner"></div>
          <p>Autenticazione in corso...</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .callback-container {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      background: #f5f7fa;
    }
    .callback-loading {
      text-align: center;
      color: #555;
      p { margin-top: 16px; font-size: 15px; }
    }
    .callback-spinner {
      width: 48px;
      height: 48px;
      border: 4px solid #e0e0e0;
      border-top-color: #1e50a0;
      border-radius: 50%;
      margin: 0 auto;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .callback-error {
      text-align: center;
      color: #555;
      i { font-size: 48px; color: #c0392b; margin-bottom: 16px; }
      h2 { color: #c0392b; margin-bottom: 8px; }
      p { margin: 4px 0; font-size: 14px; }
      .callback-hint { margin-top: 16px; color: #888; font-size: 13px; }
    }
  `]
})
export class AuthCallbackComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);

  errore = signal<string | null>(null);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    const cf = this.route.snapshot.queryParamMap.get('cf') ?? '';

    if (!token && !environment.useMockData) {
      this.errore.set('Nessun token di autenticazione ricevuto.');
      return;
    }

    // In dev (useMockData) usa un token mock; in prod usa quello ricevuto dal PUA
    const tokenDaUsare = token ?? 'mock';

    this.auth.loginWithToken(tokenDaUsare, cf).subscribe({
      next: () => {
        // Torna alla destinazione originale o alla home
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/registro-amianto';
        this.router.navigateByUrl(returnUrl);
      },
      error: () => {
        this.errore.set('Errore durante la verifica del token. Riprovare.');
      }
    });
  }
}
