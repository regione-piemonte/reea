import { Component, Input, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-header',
  imports: [CommonModule, MatButtonModule, MatMenuModule, MatSnackBarModule],
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.scss'
})
export class AppHeaderComponent {
  @Input() title: string = 'Registro Ex Esposti Amianto';

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  readonly logoutInProgress = signal(false);

  readonly nomeCompleto = computed(() => {
    const u = this.auth.currentUser();
    if (!u) return '';
    return `${u.nome} ${u.cognome}`.trim();
  });

  readonly descrizioneAzienda = computed(() =>
    this.auth.currentUser()?.collocazione?.descrizioneAzienda
    ?? this.auth.currentUser()?.collocazione?.descrizione
    ?? ''
  );

  readonly descrizioneProfilo = computed(() => {
    if (this.auth.needsProfiloSelection()) return '';
    return this.auth.currentUser()?.profili?.[0]?.descrizione ?? '';
  });

  esci(): void {
    if (this.logoutInProgress()) return;
    this.logoutInProgress.set(true);
    this.auth.logout().pipe(
      finalize(() => this.logoutInProgress.set(false))
    ).subscribe({
      next: ({ logoutUrl }) => {
        if (logoutUrl) {
          window.location.assign(logoutUrl);
        } else {
          void this.router.navigate(['/login']);
        }
      },
      error: () => {
        this.snackBar.open(
          'Impossibile completare il logout. Riprova tra qualche istante.',
          'Chiudi',
          { duration: 7000 }
        );
      }
    });
  }
}
