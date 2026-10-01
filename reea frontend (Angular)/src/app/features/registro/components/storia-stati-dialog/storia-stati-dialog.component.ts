// ============================================
// STORIA STATI DIALOG COMPONENT
// Dialog per visualizzare la storia degli stati
// di un assistito nel registro
// ============================================

import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';

// Angular Material imports
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

// Models & API
import { Assistito, StoriaStato, StatoAssistito } from '@core/models';
import { AnagraficaApiService } from '../../../../api/anagrafica-api.service';
import { AssistitiService, AuthService } from '@core/services';
import { StatusBadgeComponent } from '@shared/components/status-badge/status-badge.component';
import { forkJoin } from 'rxjs';

/**
 * Dati passati al dialog
 */
export interface StoriaStatiDialogData {
  assistito: Assistito;
}

@Component({
  selector: 'app-storia-stati-dialog',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatDialogModule,
    MatButtonModule,
    MatTableModule,
    MatProgressSpinnerModule,
    StatusBadgeComponent
  ],
  templateUrl: './storia-stati-dialog.component.html',
  styleUrl: './storia-stati-dialog.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class StoriaStatiDialogComponent {

  // ============================================
  // DEPENDENCY INJECTION
  // ============================================

  private dialogRef = inject(MatDialogRef<StoriaStatiDialogComponent>);
  private data = inject<StoriaStatiDialogData>(MAT_DIALOG_DATA);
  private anagraficaApi = inject(AnagraficaApiService);
  private assistitiService = inject(AssistitiService);
  private authService = inject(AuthService);

  isPseudo = this.authService.isPseudo;

  // ============================================
  // DATI
  // ============================================

  /** Assistito corrente (aggiornato via getById) */
  assistito = signal<Assistito>(this.data.assistito);

  /** Storia degli stati */
  storiaStati = signal<StoriaStato[]>([]);
  loading = signal<boolean>(false);

  /** Colonne visualizzate nella tabella */
  displayedColumns: string[] = [
    'stato',
    'dataModifica',
    'operatoreModifica',
    'notaPassaggioStato'
  ];

  /** Dati tabella: stato attuale + storico */
  get tableData(): (StoriaStato & { isCurrent?: boolean })[] {
    const a = this.assistito();
    const storia = this.storiaStati();
    const filterCf = (cf?: string | null): string => {
      if (!cf) return '';
      return ['ADMIN', 'SISTEMA', 'SCONOSCIUTO'].includes(cf.toUpperCase()) ? '' : cf;
    };
    // Rimuove il suffisso timezone dalle date della storia (arrivano con offset errato dal backend)
    // in modo coerente con parseDate usato per dataInserimento/dataUltimoAggiornamento
    const normDate = (val: Date | string | null | undefined): string | null => {
      if (!val) return null;
      const str = typeof val === 'string' ? val : (val as Date).toISOString();
      if (/^\d{4}-\d{2}-\d{2}T/.test(str)) return str.substring(0, 19);
      return str || null;
    };
    const isDaValutareIniziale = a.stato === StatoAssistito.DA_VALUTARE && storia.length === 0;
    const dataCorrente = isDaValutareIniziale
      ? (a.dataInserimento || null)
      : ((a.dataUltimoAggiornamento || null)
          || (storia.length > 0
            ? normDate(storia[storia.length - 1].data_modifica || storia[storia.length - 1].data_creazione)
            : null)
          || (a.dataInserimento || null));
    const operatoreAttuale = isDaValutareIniziale
      ? filterCf(a.utenteCreazione)
      : (filterCf(a.utenteModifica) || filterCf(storia.length > 0 ? storia[storia.length - 1].utente_modifica : ''));
    const rigaAttuale: StoriaStato & { isCurrent: boolean } = {
      soggetto_stato_id: 'current',
      soggetto_stato_desc: a.stato,
      data_modifica: dataCorrente,
      utente_modifica: operatoreAttuale,
      soggetto_stato_note: a.notaStatoAttuale ?? '',
      data_creazione: null,
      isCurrent: true
    };
    const storicoOrdinato = [...storia].reverse().map((s, idx) => {
      const isOldest = idx === storia.length - 1;
      const rigaPrecedente = isOldest ? null : storia[storia.length - 2 - idx];
      const cfOperatore = isOldest
        ? filterCf(a.utenteCreazione)
        : filterCf(rigaPrecedente?.utente_modifica);
      const dataIngresso = isOldest
        ? (a.dataInserimento || null)
        : normDate(rigaPrecedente?.data_modifica || rigaPrecedente?.data_creazione);
      return {
        ...s,
        utente_modifica: cfOperatore,
        data_modifica: dataIngresso
      };
    });
    return [rigaAttuale, ...storicoOrdinato];
  }

  // ============================================
  // LIFECYCLE
  // ============================================

  constructor() {
    this.loadStoriaStati();
  }

  // ============================================
  // DATA LOADING
  // ============================================

  private loadStoriaStati(): void {
    const id = this.assistito().id;
    if (!id) return;
    this.loading.set(true);
    forkJoin({
      storia: this.anagraficaApi.getStoriaStati(Number(id)),
      assistito: this.assistitiService.getAssistitoById(id)
    }).subscribe({
      next: ({ storia, assistito }) => {
        this.storiaStati.set(storia);
        // Preserva dataInserimento dall'originale se findById non restituisce data_creazione
        this.assistito.set({
          ...assistito,
          dataInserimento: assistito.dataInserimento || this.data.assistito.dataInserimento,
          utenteCreazione: assistito.utenteCreazione || this.data.assistito.utenteCreazione,
        });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  // ============================================
  // AZIONI
  // ============================================

  /**
   * Mappa la descrizione stato (stringa DB) all'enum StatoAssistito
   */
  mapToStatoEnum(desc: string): StatoAssistito {
    const map: Record<string, StatoAssistito> = {
      'DA VALUTARE':                    StatoAssistito.DA_VALUTARE,
      'ELEGGIBILE':                     StatoAssistito.ELEGGIBILE,
      'NON ELEGGIBILE':                 StatoAssistito.NON_ELEGGIBILE,
      'PRESO IN CARICO':                StatoAssistito.PRESO_IN_CARICO,
      'NON PRESO IN CARICO':            StatoAssistito.NON_PRESO_IN_CARICO,
      'AVVIATO ALLA SORVEGLIANZA':      StatoAssistito.AVVIATO_SORV,
      'AVVIATO SORVEGLIANZA':           StatoAssistito.AVVIATO_SORV,
      'ESCLUSO DALLA SORVEGLIANZA':     StatoAssistito.ESCLUSO_SORV,
      'ESCLUSO SORVEGLIANZA':           StatoAssistito.ESCLUSO_SORV,
      'SORVEGLIANZA CONCLUSA':          StatoAssistito.CONCLU_SORV,
      'CONCLUSO SORVEGLIANZA':          StatoAssistito.CONCLU_SORV,
      'ESCLUSO PER DECESSO':            StatoAssistito.ESCLUSO_DECESSO,
      'ESCLUSO DECESSO':                StatoAssistito.ESCLUSO_DECESSO,
      'ESCLUSO PER EMIGRAZIONE':        StatoAssistito.ESCLUSO_EMIGRAZIONE,
      'ESCLUSO EMIGRAZIONE':            StatoAssistito.ESCLUSO_EMIGRAZIONE,
      'CARICATO':                       StatoAssistito.CARICATO,
    };
    return map[desc?.toUpperCase()] ?? StatoAssistito.DA_VALUTARE;
  }

  /**
   * Chiude il dialog
   */
  onChiudi(): void {
    this.dialogRef.close();
  }
}
