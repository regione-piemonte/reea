import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AuditApiService } from '../../../../api/audit-api.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  private readonly auditApi = inject(AuditApiService);

  loading = signal(false);
  errore = signal<string | null>(null);
  showPassword = signal(false);
  loadingDev = signal(false);
  puaObbligatorio = signal(false);
  readonly isLocale = environment.customVar === 'locale';
  cfDev = '';

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  ngOnInit(): void {
    this.auth.getTipoAuth().subscribe({
      next: ({ accesso_pua }) => {
        if (accesso_pua === 'ON') {
          this.puaObbligatorio.set(true);
        }
      }
    });
  }

  accedi(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errore.set(null);

    const username = this.form.value.username ?? '';
    const password = this.form.value.password ?? '';

    this.auth.loginConCredenziali(username, password).subscribe({
      next: () => {
        const cf = this.auth.currentUser()?.codiceFiscale ?? username;
        const ruolo = this.auth.currentUser()?.ruolo ?? '';
        this.auditApi.salva('LOGIN', 'ACCESSO', `${cf} - ${ruolo}`).subscribe();
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/registro-amianto';
        this.router.navigateByUrl(returnUrl);
      },
      error: () => {
        this.loading.set(false);
        this.errore.set('Username o password non validi.');
      }
    });
  }

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  loginDev(): void {
    if (!this.cfDev.trim()) return;
    this.loadingDev.set(true);
    this.auth.loginDev(this.cfDev.trim()).subscribe({
      next: () => {
        const cf = this.auth.currentUser()?.codiceFiscale ?? this.cfDev.trim();
        const ruolo = this.auth.currentUser()?.ruolo ?? '';
        this.auditApi.salva('LOGIN', 'ACCESSO', `${cf} - ${ruolo}`).subscribe();
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/registro-amianto';
        this.router.navigateByUrl(returnUrl);
      },
      error: () => this.loadingDev.set(false)
    });
  }
}
