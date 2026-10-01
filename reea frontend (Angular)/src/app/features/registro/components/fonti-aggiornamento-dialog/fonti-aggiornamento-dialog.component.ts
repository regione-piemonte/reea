import { Component, computed, inject, signal, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Assistito, FonteStorico } from '@core/models';
import { FonteApiService } from '../../../../api';
import { AuthService } from '@core/services';

export interface FontiAggiornamentoDialogData {
  assistito: Assistito;
}

@Component({
  selector: 'app-fonti-aggiornamento-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './fonti-aggiornamento-dialog.component.html',
  styleUrl: './fonti-aggiornamento-dialog.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class FontiAggiornamentoDialogComponent {
  private dialogRef = inject(MatDialogRef<FontiAggiornamentoDialogComponent>);
  private data = inject<FontiAggiornamentoDialogData>(MAT_DIALOG_DATA);
  private fonteApi = inject(FonteApiService);
  private authService = inject(AuthService);

  isPseudo = this.authService.isPseudo;
  assistito = this.data.assistito;

  fonteAttuale = signal<FonteStorico | null>(null);
  storicoFonti = signal<FonteStorico[]>([]);
  loading = signal<boolean>(true);

  tutteLeFonti = computed<FonteStorico[]>(() => {
    const attuale = this.fonteAttuale();
    const storico = this.storicoFonti();
    return attuale ? [attuale, ...storico] : storico;
  });

  getDateLabel(fonteName: string): string {
    const name = (fonteName ?? '').toLowerCase();
    if (name.includes('preadesion')) return 'Data preadesione';
    if (name.includes('segnalazione') || name.includes('mmg')) return 'Data adesione';
    return 'Data caricamento file';
  }

  constructor() {
    this.loadStoricoFonti();
  }

  private loadStoricoFonti(): void {
    const soggettoId = this.assistito.id;
    if (!soggettoId) {
      this.loading.set(false);
      return;
    }

    this.fonteApi.getStoricoFonti(Number(soggettoId)).subscribe({
      next: (fonti) => {
        // Mappa il DTO dal backend al modello frontend
        // data_fonte = data specifica per tipo fonte (BE); fallback a data_creazione
        const mappedFonti: FonteStorico[] = fonti
          .map(f => {
            const isPreadesione = (f.fonte_desc ?? '').toLowerCase().includes('preadesion');
            const dataAssociazione = f.data_fonte
              ?? (isPreadesione && this.assistito.dataAdesione ? this.assistito.dataAdesione : f.data_creazione);
            return { descrizione: f.fonte_desc, dataAssociazione };
          });

        // Ordina per data discendente: la più recente è sempre la fonte attuale (blu)
        const sorted = [...mappedFonti].sort((a, b) => {
          const da = a.dataAssociazione ? new Date(a.dataAssociazione).getTime() : 0;
          const db = b.dataAssociazione ? new Date(b.dataAssociazione).getTime() : 0;
          return db - da;
        });
        if (sorted.length > 0) {
          this.fonteAttuale.set(sorted[0]);
          this.storicoFonti.set(sorted.slice(1));
        }

        this.loading.set(false);
      },
      error: () => {
        // In caso di errore, usa i dati già presenti nell'assistito
        const fallback = this.assistito.storicoFonti ?? [];
        this.fonteAttuale.set(fallback[0] ?? null);
        this.storicoFonti.set(fallback.slice(1));
        this.loading.set(false);
      }
    });
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    // Se contiene 'T' (ISO datetime), prendi solo la parte data
    if (dateStr.includes('T')) {
      dateStr = dateStr.split('T')[0];
    }
    // Se è in formato yyyy-MM-dd, converti a dd/MM/yyyy
    if (/^\d{4}-\d{2}-\d{2}/.test(dateStr)) {
      const [year, month, day] = dateStr.split('-');
      return `${day}/${month}/${year}`;
    }
    return dateStr;
  }

  onChiudi(): void {
    this.dialogRef.close();
  }
}
