// ============================================
// NOTE REGISTRO DIALOG COMPONENT
// ============================================

import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { MAT_DIALOG_DATA, MatDialog, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { Assistito } from '@core/models';
import { NotaApiService, NotaDTO } from '../../../../api/nota-api.service';
import { ConfermaEliminazioneDialogComponent } from '../conferma-eliminazione-dialog/conferma-eliminazione-dialog.component';

export interface NoteRegistroDialogData {
  assistito: Assistito;
}

@Component({
  selector: 'app-note-registro-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './note-registro-dialog.component.html',
  styleUrl: './note-registro-dialog.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class NoteRegistroDialogComponent {

  private dialogRef  = inject(MatDialogRef<NoteRegistroDialogComponent>);
  private data       = inject<NoteRegistroDialogData>(MAT_DIALOG_DATA);
  private notaApi    = inject(NotaApiService);
  private dialog     = inject(MatDialog);
  private snackBar   = inject(MatSnackBar);

  assistito = this.data.assistito;
  note = signal<NotaDTO[]>([]);
  loading = signal<boolean>(false);

  notaInModifica = signal<NotaDTO | null>(null);
  nuovaNotaControl = new FormControl('');
  editNotaControl  = new FormControl('');

  displayedColumns: string[] = ['descrizione', 'dataModifica', 'operatoreModifica'];

  constructor() {
    this.loadNote();
  }

  private loadNote(): void {
    if (!this.assistito.id) return;
    this.loading.set(true);
    this.notaApi.getNote(Number(this.assistito.id)).subscribe({
      next: (note) => { this.note.set(note); this.loading.set(false); },
      error: ()    => this.loading.set(false)
    });
  }

  salvaNota(): void {
    const testo = this.nuovaNotaControl.value?.trim();
    if (!testo) return;
    this.notaApi.inserisci(Number(this.assistito.id), testo).subscribe({
      next: (nota) => {
        this.note.set([nota, ...this.note()]);
        this.nuovaNotaControl.reset();
        this.showSuccess('Nota salvata');
      },
      error: () => this.showError('Errore durante il salvataggio della nota')
    });
  }

  abilitaModifica(nota: NotaDTO): void {
    this.notaInModifica.set(nota);
    this.editNotaControl.setValue(nota.descrizione);
  }

  salvaModifica(): void {
    const nota = this.notaInModifica();
    if (!nota) return;
    const testo = this.editNotaControl.value?.trim();
    if (!testo) return;
    this.notaApi.modifica(nota.nota_id, testo).subscribe({
      next: (updated) => {
        this.note.set(this.note().map(n => n.nota_id === nota.nota_id ? updated : n));
        this.notaInModifica.set(null);
        this.editNotaControl.reset();
        this.showSuccess('Nota aggiornata');
      },
      error: () => this.showError('Errore durante la modifica della nota')
    });
  }

  annullaModifica(): void {
    this.notaInModifica.set(null);
    this.editNotaControl.reset();
  }

  eliminaNota(nota: NotaDTO): void {
    const ref = this.dialog.open(ConfermaEliminazioneDialogComponent, { width: '360px' });
    ref.afterClosed().subscribe((confermato: boolean) => {
      if (!confermato) return;
      this.notaApi.elimina(nota.nota_id).subscribe({
        next: () => {
          this.note.set(this.note().filter(n => n.nota_id !== nota.nota_id));
          this.showSuccess('Nota eliminata');
        },
        error: () => this.showError('Errore durante l\'eliminazione della nota')
      });
    });
  }

  onChiudi(): void {
    this.dialogRef.close();
  }

  private showSuccess(msg: string): void {
    this.snackBar.open(msg, 'Chiudi', { duration: 3000, panelClass: ['snackbar-success'] });
  }

  private showError(msg: string): void {
    this.snackBar.open(msg, 'Chiudi', { duration: 5000, panelClass: ['snackbar-error'] });
  }
}
