import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ProfiloApplicativo, Collocazione } from '../../../../core/models';
import { AppHeaderComponent } from '../../../../shared/components/app-header/app-header.component';
import { AuditApiService } from '../../../../api/audit-api.service';

@Component({
  selector: 'app-scelta-profilo',
  standalone: true,
  imports: [AppHeaderComponent],
  templateUrl: './scelta-profilo.component.html',
  styleUrl: './scelta-profilo.component.scss'
})
export class SceltaProfiloComponent implements OnInit {

  private auth = inject(AuthService);
  private router = inject(Router);
  private auditApi = inject(AuditApiService);

  // Step 1: scelta ruolo + collocazione 
  // Step 2: scelta profilo
  step = signal<'ruolo' | 'profilo'>('ruolo');

  ruoli = signal<ProfiloApplicativo[]>([]);
  collocazioni = signal<Collocazione[]>([]);
  ruoloScelto = signal<string | null>(null);
  collocazioneScelta = signal<string | null>(null);

  profili = signal<ProfiloApplicativo[]>([]);
  profiloScelto = signal<string | null>(null);

  loading = signal(false);
  errore = signal<string | null>(null);

  canProceedStep1 = computed(() =>
    !!this.ruoloScelto() &&
    (this.collocazioni().length === 0 || !!this.collocazioneScelta()) &&
    !this.loading()
  );

  canProceedStep2 = computed(() =>
    !!this.profiloScelto() && !this.loading()
  );

  ngOnInit(): void {
    const user = this.auth.currentUser();
    if (!user) { this.router.navigate(['/login']); return; }
    if (!this.auth.needsProfiloSelection()) {
      this.router.navigate(['/registro-amianto']); return;
    }
    this.ruoli.set(user.profili ?? []);

    if (user.profili?.length === 1) {
      this.selezionaRuolo(user.profili[0].codice);
    }
  }

  selezionaRuolo(codice: string): void {
    this.ruoloScelto.set(codice);
    this.collocazioneScelta.set(null);

    const user = this.auth.currentUser()!;
    // Login interno (user+pwd): collocazioni a livello utente (CDU par. 7.3)
    // Login PUA: collocazioni nested nel profilo dal Configuratore
    const userCols = user.collocazioni ?? [];
    const ruoloCols = this.ruoli().find(r => r.codice === codice)?.collocazioni ?? [];
    const raw = userCols.length > 0 ? userCols : ruoloCols;
    // deduplicazione per codice (sicurezza contro righe duplicate in DB)
    const cols = raw.filter((c, i, arr) => arr.findIndex(x => x.codice === c.codice) === i);

    this.collocazioni.set(cols);
    if (cols.length === 1) this.collocazioneScelta.set(cols[0].codice);
  }

  selezionaCollocazione(codice: string): void { this.collocazioneScelta.set(codice); }
  selezionaProfilo(codice: string): void { this.profiloScelto.set(codice); }

  proseguiStep1(): void {
    const ruoloCod = this.ruoloScelto();
    if (!ruoloCod) return;
    this.loading.set(true);
    this.errore.set(null);

    this.auth.selezionaRuolo(ruoloCod, this.collocazioneScelta() ?? undefined).subscribe({
      next: (user) => {
        this.loading.set(false);
        const profili = user.profili ?? [];
        if (profili.length === 0) {
          this.errore.set('Nessun profilo disponibile per il ruolo selezionato.');
          return;
        }
        if (profili.length === 1) {
          // profilo unico: seleziona direttamente
          this.profiloScelto.set(profili[0].codice);
          this.profili.set(profili);
          this.proseguiStep2();
        } else {
          this.profili.set(profili);
          this.profiloScelto.set(null);
          this.step.set('profilo');
        }
      },
      error: () => {
        this.loading.set(false);
        this.errore.set('Errore durante la selezione del ruolo. Riprovare.');
      }
    });
  }

  proseguiStep2(): void {
    const profiloCod = this.profiloScelto();
    if (!profiloCod) return;
    this.loading.set(true);
    this.errore.set(null);

    const profiloDesc = this.profili().find(p => p.codice === profiloCod)?.descrizione ?? profiloCod;

    this.auth.selezionaProfilo(profiloCod).subscribe({
      next: () => {
        this.auditApi.salva('login', 'PROFILO', profiloDesc).subscribe();
        this.router.navigate(['/registro-amianto']);
      },
      error: () => {
        this.loading.set(false);
        this.errore.set('Errore durante la selezione del profilo. Riprovare.');
      }
    });
  }

  tornaAStep1(): void {
    this.step.set('ruolo');
    this.profiloScelto.set(null);
    this.errore.set(null);
  }
}
