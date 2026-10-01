// ============================================
// VALUTA / PRENDI IN CARICO DIALOG COMPONENT
// Gestisce CDU-023 (Valuta assistito) e CDU-024 (Prendi in carico)
// ============================================

import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

import { Assistito, StatoAssistito } from '@core/models';

/**
 * Modalità del dialog:
 * - 'valuta'               → CDU-023 §2.2/2.3: DA VALUTARE → ELEGGIBILE o NON ELEGGIBILE
 * - 'cambia-valutazione'   → CDU-023 §2.5:     NON ELEGGIBILE → ELEGGIBILE (senza scelta)
 * - 'prendi-in-carico'     → CDU-024 §2.2/2.3: ELEGGIBILE → PRESO IN CARICO o NON PRESO IN CARICO
 * - 'cambia-presa-in-carico' → CDU-024 §2.5/2.6: toggle PRESO ↔ NON PRESO IN CARICO (senza scelta)
 */
export type ValutaAssistitoMode =
  | 'valuta'
  | 'cambia-valutazione'
  | 'prendi-in-carico'
  | 'cambia-presa-in-carico'
  | 'concludi-sorveglianza';

export interface ValutaAssistitoDialogData {
  assistito: Assistito;
  mode?: ValutaAssistitoMode; // default: 'valuta'
}

export interface ValutaAssistitoDialogResult {
  statoId: number;
  nota: string;
}

@Component({
  selector: 'app-valuta-assistito-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule],
  templateUrl: './valuta-assistito-dialog.component.html',
  styleUrl: './valuta-assistito-dialog.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class ValutaAssistitoDialogComponent {

  private dialogRef = inject(MatDialogRef<ValutaAssistitoDialogComponent>);
  private data = inject<ValutaAssistitoDialogData>(MAT_DIALOG_DATA);

  assistito = this.data.assistito;
  mode: ValutaAssistitoMode = this.data.mode ?? 'valuta';

  /**
   * Per le modalità con scelta ('valuta', 'prendi-in-carico'):
   * 'a' = prima opzione (Eleggibile / SI)
   * 'b' = seconda opzione (Non eleggibile / NO)
   *
   * Per le modalità senza scelta ('cambia-valutazione', 'cambia-presa-in-carico'):
   * il valore è pre-impostato e non modificabile
   */
  sceltaSelezionata = signal<'a' | 'b' | null>(
    (this.data.mode === 'cambia-valutazione' || this.data.mode === 'cambia-presa-in-carico' || this.data.mode === 'concludi-sorveglianza')
      ? 'a'
      : null
  );

  nota = '';

  // ============================================
  // CONFIGURAZIONE PER MODALITÀ
  // ============================================

  get titolo(): string {
    switch (this.mode) {
      case 'valuta':               return 'Valutazione Assistito';
      case 'cambia-valutazione':   return 'Cambia Valutazione';
      case 'prendi-in-carico':     return 'Prendi in carico Assistito';
      case 'cambia-presa-in-carico': return 'Cambia Presa in Carico';
      case 'concludi-sorveglianza': return 'Concludi Sorveglianza';
      default:                     return '';
    }
  }

  get labelBtnA(): string {
    return this.mode === 'prendi-in-carico' ? 'SI' : 'Eleggibile';
  }

  get labelBtnB(): string {
    return this.mode === 'prendi-in-carico' ? 'NO' : 'Non eleggibile';
  }

  /** Mostra i pulsanti di scelta solo nelle modalità con scelta esplicita */
  get mostraScelta(): boolean {
    return this.mode === 'valuta' || this.mode === 'prendi-in-carico';
  }

  get isConcludiSorveglianza(): boolean {
    return this.mode === 'concludi-sorveglianza';
  }

  /** Mostra la nota appena una scelta è stata fatta (o subito per le modalità senza scelta) */
  get mostraNota(): boolean {
    return this.sceltaSelezionata() !== null;
  }

  /** La nota è obbligatoria per stati negativi: Non eleggibile e Non preso in carico */
  get isNotaObbligatoria(): boolean {
    const scelta = this.sceltaSelezionata();
    if (scelta === null) return false;
    const statoId = this.calcolaStatoId(scelta);
    return statoId === 3 || statoId === 11; // NON_ELEGGIBILE | NON_PRESO_IN_CARICO
  }

  get canSave(): boolean {
    if (this.sceltaSelezionata() === null) return false;
    if (this.isNotaObbligatoria && !this.nota.trim()) return false;
    return true;
  }

  /** Label che descrive la transizione di stato (solo per le modalità senza scelta) */
  get labelTransizione(): string | null {
    switch (this.mode) {
      case 'cambia-valutazione':
        return ' Non eleggibile → Eleggibile';
      case 'cambia-presa-in-carico':
        return this.assistito.stato === StatoAssistito.PRESO_IN_CARICO
          ? ' Preso in carico → Non preso in carico'
          : ' Non preso in carico → Preso in carico';
      case 'concludi-sorveglianza':
        return ' Avviato alla sorveglianza → Sorveglianza conclusa';
      default:
        return null;
    }
  }

  // ============================================
  // AZIONI
  // ============================================

  seleziona(scelta: 'a' | 'b'): void {
    this.sceltaSelezionata.set(scelta);
  }

  onSalva(): void {
    const scelta = this.sceltaSelezionata();
    if (!scelta) return;

    const statoId = this.calcolaStatoId(scelta);
    this.dialogRef.close({ statoId, nota: this.nota.trim() } as ValutaAssistitoDialogResult);
  }

  onChiudi(): void {
    this.dialogRef.close(null);
  }

  // ============================================
  // CALCOLO STATO ID
  // ============================================

  calcolaStatoId(scelta: 'a' | 'b'): number {
    switch (this.mode) {
      case 'valuta':
        return scelta === 'a' ? 2 : 3; // ELEGGIBILE / NON ELEGGIBILE

      case 'cambia-valutazione':
        return 2; // sempre → ELEGGIBILE

      case 'prendi-in-carico':
        return scelta === 'a' ? 4 : 11; // PRESO IN CARICO / NON PRESO IN CARICO

      case 'cambia-presa-in-carico':
        return this.assistito.stato === StatoAssistito.PRESO_IN_CARICO ? 11 : 4;

      case 'concludi-sorveglianza':
        return 7; // SORVEGLIANZA_CONCLUSA
    }
  }
}
