// ============================================
// ARCHIVIO FILE DIALOG COMPONENT
// Dialog per visualizzare l'elenco dei file
// caricati nel sistema con le statistiche (CDU-027)
// ============================================

import { Component, inject, signal, computed, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';

// Angular Material imports
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

// Services
import { FileApiService, ArchivioFileCaricatiDTO, ScartoFileDTO } from '../../../../api';
import { AuthService } from '../../../../core/services/auth.service';
import { AuditApiService } from '../../../../api/audit-api.service';

// PDF
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Component({
  selector: 'app-archivio-file-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DatePipe,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatInputModule,
    MatSnackBarModule,
  ],
  templateUrl: './archivio-file-dialog.component.html',
  styleUrl: './archivio-file-dialog.component.scss'
})
export class ArchivioFileDialogComponent implements OnDestroy {

  private dialogRef = inject(MatDialogRef<ArchivioFileDialogComponent>);
  private fileApi = inject(FileApiService);
  private authService = inject(AuthService);
  private auditApi = inject(AuditApiService);
  private snackBar = inject(MatSnackBar);

  centerNotif = signal<{ text: string; tipo: 'success' | 'error' | 'info' } | null>(null);
  private notifTimer: ReturnType<typeof setTimeout> | null = null;

  showCenterNotif(text: string, tipo: 'success' | 'error' | 'info', duration = 6000): void {
    if (this.notifTimer) clearTimeout(this.notifTimer);
    this.centerNotif.set({ text, tipo });
    this.notifTimer = setTimeout(() => this.centerNotif.set(null), duration);
  }

  fonteFilter = new FormControl<string>('');
  dataDaFilter = new FormControl<Date | null>(null);
  dataAFilter = new FormControl<Date | null>(null);

  // I value devono corrispondere al nome cartella estratto dal FILE_PATH dal backend
  private readonly allFontiOptions: { value: string; label: string }[] = [
    { value: 'preadesione', label: 'Preadesioni' },
    { value: 'inail',       label: 'INAIL' },
    { value: 'npla',        label: 'NPLA' },
    { value: 'spresal',     label: 'SPRESAL' },
  ];

  get fontiOptions(): { value: string; label: string }[] {
    if (this.authService.isCrpt()) {
      return this.allFontiOptions.filter(f => f.value === 'preadesione');
    }
    if (this.authService.isSpresalNonPseudo()) {
      return this.allFontiOptions.filter(f => f.value === 'npla' || f.value === 'spresal');
    }
    if (this.authService.isInail()) {
      return this.allFontiOptions.filter(f => f.value === 'inail');
    }
    return this.allFontiOptions;
  }

  private get allowedFontiFolders(): string[] | null {
    if (this.authService.isCrpt()) return ['preadesione'];
    if (this.authService.isSpresalNonPseudo()) return ['npla', 'spresal'];
    if (this.authService.isInail()) return ['inail'];
    return null;
  }

  files = signal<ArchivioFileCaricatiDTO[]>([]);
  isLoading = signal<boolean>(false);

  displayedColumns: string[] = [
    'elaborazioneId',
    'nomeFile',
    'fonte',
    'stato',
    'recordTotali',
    'recordScartati',
    'recordElaborati',
    'recordErrati',
    'dataUpload',
    'operatore',
    'azioni'
  ];

  isDownloading = signal<number | null>(null);

  private pollingTimer: ReturnType<typeof setInterval> | null = null;
  private elaborazioniInCorso = new Set<number>();

  totalFiles = computed(() => new Set(this.files().map(f => f.fileId).filter(id => id != null)).size);

  /** Compatibile con vecchio nome record_errati e nuovo nome righe_errate */
  getErrati(file: ArchivioFileCaricatiDTO): number {
    return file.righe_errate ?? file.record_errati ?? 0;
  }

  constructor() {
    this.loadFiles();
    this.fonteFilter.valueChanges.subscribe(() => this.loadFiles());
    this.dataDaFilter.valueChanges.subscribe(() => this.loadFiles());
    this.dataAFilter.valueChanges.subscribe(() => this.loadFiles());
    this.pollingTimer = setInterval(() => this.loadFilesSilent(), 10000);
  }

  private loadFiles(): void {
    this.isLoading.set(true);
    const filtro = this.fonteFilter.value || undefined;
    const dataDa = this.dataDaFilter.value
      ? this.formatDate(this.dataDaFilter.value)
      : undefined;
    const dataA = this.dataAFilter.value
      ? this.formatDate(this.dataAFilter.value)
      : undefined;

    this.auditApi.salva('read', 'ANAGRAFICA', 'Archivio file - ricerca').subscribe();

    this.fileApi.getArchivioFileCaricati(filtro, dataDa, dataA).subscribe({
      next: (data) => {
        const sorted = this.applyFileFilters(data);
        this.files.set(sorted);
        this.isLoading.set(false);
        this.aggiornaPollingSuFiles(sorted);
      },
      error: () => {
        this.files.set([]);
        this.isLoading.set(false);
      }
    });
  }

  private applyFileFilters(data: ArchivioFileCaricatiDTO[]): ArchivioFileCaricatiDTO[] {
    const toMinute = (d: string | undefined) =>
      d ? Math.floor(new Date(d).getTime() / 60000) : 0;
    let sorted = [...data].sort((a, b) => {
      const ma = toMinute(a.data_caricamento);
      const mb = toMinute(b.data_caricamento);
      if (mb !== ma) return mb - ma;
      return (b.elaborazione_id ?? 0) - (a.elaborazione_id ?? 0);
    });
    sorted = sorted.filter(f => f.fonte_provenienza != null && f.fonte_provenienza.trim() !== '');
    sorted = sorted.filter(f => !(f.nome_file ?? '').toLowerCase().startsWith('export_'));
    const allowed = this.allowedFontiFolders;
    if (allowed) {
      sorted = sorted.filter(f => {
        const fonte = (f.fonte_provenienza ?? '').toLowerCase();
        return allowed.some(a => fonte.includes(a));
      });
    }
    return sorted;
  }

  private loadFilesSilent(): void {
    const filtro = this.fonteFilter.value || undefined;
    const dataDa = this.dataDaFilter.value ? this.formatDate(this.dataDaFilter.value) : undefined;
    const dataA = this.dataAFilter.value ? this.formatDate(this.dataAFilter.value) : undefined;
    this.fileApi.getArchivioFileCaricati(filtro, dataDa, dataA).subscribe({
      next: (data) => {
        const sorted = this.applyFileFilters(data);
        this.files.set(sorted);
        this.aggiornaPollingSuFiles(sorted);
      }
    });
  }

  private aggiornaPollingSuFiles(files: ArchivioFileCaricatiDTO[]): void {
    const nowInCorso = new Set<number>();
    files.forEach(f => {
      if (f.elaborazione_id && (f.stato ?? '').toUpperCase().includes('ELABORAR')) {
        nowInCorso.add(f.elaborazione_id);
      }
    });

    this.elaborazioniInCorso.forEach(id => {
      if (!nowInCorso.has(id)) {
        const file = files.find(f => f.elaborazione_id === id);
        if (!file) return;
        const stato = (file.stato ?? '').toUpperCase();
        if (!stato.includes('TERMINAT') && !stato.includes('FALLITA')) return;
        const fallita = stato.includes('FALLITA');
        this.showCenterNotif(
          fallita
            ? `Elaborazione ${file.nome_file ?? ''} terminata con errori.`
            : `Elaborazione ${file.nome_file ?? ''} completata con successo!`,
          fallita ? 'error' : 'success'
        );
      }
    });

    this.elaborazioniInCorso = nowInCorso;
  }

  ngOnDestroy(): void {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  onResetFiltri(): void {
    this.fonteFilter.setValue('', { emitEvent: false });
    this.dataDaFilter.setValue(null, { emitEvent: false });
    this.dataAFilter.setValue(null, { emitEvent: false });
    this.loadFiles();
  }

  statoClass(stato?: string): string {
    if (!stato) return 'stato-errore';
    const s = stato.toUpperCase();
    if (s.includes('TERMINAT')) return 'stato-ok';
    if (s.includes('FALLITA'))  return 'stato-errore';
    if (s.includes('CORSO'))    return 'stato-progress';
    if (s.includes('ELABORAR')) return 'stato-attesa';
    return 'stato-errore';
  }

  /**
   * Estrae il nome cartella fonte dal path completo.
   * Es: "...reea\fonti\inail\INAIL_A.xlsx" → "inail"
   */
  getFonteLabel(path?: string): string {
    if (!path) return '—';
    const normalized = path.replace(/\\/g, '/');
    const match = normalized.match(/\/reea\/fonti\/([^/]+)/i);
    if (match) {
      const folder = match[1].toLowerCase();
      const labels: Record<string, string> = {
        inail:        'INAIL',
        spresal:      'SPRESAL',
        preadesione:  'Preadesioni',
        npla:         'NPLA',
      };
      return labels[folder] ?? folder;
    }
    return '—';
  }

  onVisualizzaScarti(file: ArchivioFileCaricatiDTO): void {
    if (!file.elaborazione_id) return;
    this.fileApi.getScartiFile(file.elaborazione_id).subscribe({
      next: (scarti) => this.generaPdfScarti(scarti, file)
    });
  }

  onVisualizzaScartati(file: ArchivioFileCaricatiDTO): void {
    if (!file.fileId) return;
    this.fileApi.getScartatiFile(file.fileId).subscribe({
      next: (scarti) => {
        if (!scarti || scarti.length === 0) {
          this.snackBar.open('Nessun record scartato per questo file.', 'OK', { duration: 4000 });
          return;
        }
        this.generaPdfScartati(scarti, file);
      }
    });
  }

  private getAltroCampoLabel(file: ArchivioFileCaricatiDTO): string {
    const fonte = (file.fonte_provenienza ?? '').toLowerCase();
    if (fonte.includes('preadesione')) return 'Codazi';
    if (fonte.includes('inail'))       return 'Domanda';
    if (fonte.includes('npla'))        return 'Id cantiere';
    if (fonte.includes('spresal')) {
      const nome = (file.nome_file ?? '').toLowerCase();
      return nome.includes('esit') ? 'Data visita' : 'Num. Occupazione';
    }
    return 'Altro';
  }

  private generaPdfScarti(scarti: ScartoFileDTO[], file: ArchivioFileCaricatiDTO): void {
    const altroCampoLabel = this.getAltroCampoLabel(file);
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    const oggi = new Date().toLocaleDateString('it-IT');
    const nomeFile = file.nome_file ?? 'File';

    // Header
    doc.setFontSize(10);
    doc.setTextColor(100);
    doc.text(`Id Elaborazione: ${file.elaborazione_id ?? '—'}`, 14, 14);
    doc.text(`Data caricamento: ${oggi}`, 14, 20);
    doc.text(`Nome file: ${nomeFile}`, 14, 26);

    // Titolo
    doc.setFontSize(14);
    doc.setTextColor(180, 0, 0);
    doc.text(`RECORD ELABORATI CON ERRORE: ${scarti.length}`, 14, 38);

    // Tabella
    autoTable(doc, {
      startY: 44,
      head: [['Cognome e Nome', 'Data Nascita', 'Sesso', 'Codice Fiscale', altroCampoLabel, 'Errore']],
      body: scarti.map(s => [
        `${s.cognome ?? ''} ${s.nome ?? ''}`.trim() || '—',
        s.data_nascita ?? '—',
        s.sesso ?? '—',
        s.codice_fiscale ?? '—',
        s.campo_extra ?? '—',
        s.descrizione_errore ?? s.codice_errore ?? '—'
      ]),
      headStyles: { fillColor: [176, 0, 0], textColor: 255, fontStyle: 'bold', fontSize: 9 },
      bodyStyles: { fontSize: 8, textColor: 50 },
      alternateRowStyles: { fillColor: [255, 245, 245] },
      columnStyles: {
        0: { cellWidth: 50 },
        1: { cellWidth: 26 },
        2: { cellWidth: 13 },
        3: { cellWidth: 36 },
        4: { cellWidth: 30 },
        5: { cellWidth: 'auto' }
      },
      margin: { left: 14, right: 14 }
    });

    const safeName = nomeFile.replace(/\.[^.]+$/, '');
    doc.save(`Errori_${safeName}.pdf`);
  }

  private generaPdfScartati(scarti: ScartoFileDTO[], file: ArchivioFileCaricatiDTO): void {
    const altroCampoLabel = this.getAltroCampoLabel(file);
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    const oggi = new Date().toLocaleDateString('it-IT');
    const nomeFile = file.nome_file ?? 'File';

    doc.setFontSize(10);
    doc.setTextColor(100);
    doc.text(`Id Elaborazione: ${file.elaborazione_id ?? '—'}`, 14, 14);
    doc.text(`Data caricamento: ${oggi}`, 14, 20);
    doc.text(`Nome file: ${nomeFile}`, 14, 26);

    doc.setFontSize(14);
    doc.setTextColor(21, 101, 192);
    doc.text(`RECORD SCARTATI: ${scarti.length}`, 14, 38);

    autoTable(doc, {
      startY: 44,
      head: [['Cognome e Nome', 'Data Nascita', 'Sesso', 'Codice Fiscale', altroCampoLabel, 'Motivo scarto']],
      body: scarti.map(s => [
        `${s.cognome ?? ''} ${s.nome ?? ''}`.trim() || '—',
        s.data_nascita ?? '—',
        s.sesso ?? '—',
        s.codice_fiscale ?? '—',
        s.domanda ?? s.campo_extra ?? '—',
        s.descrizione_errore ?? s.codice_errore ?? '—'
      ]),
      headStyles: { fillColor: [21, 101, 192], textColor: 255, fontStyle: 'bold', fontSize: 9 },
      bodyStyles: { fontSize: 8, textColor: 50 },
      alternateRowStyles: { fillColor: [235, 245, 255] },
      columnStyles: {
        0: { cellWidth: 50 },
        1: { cellWidth: 26 },
        2: { cellWidth: 13 },
        3: { cellWidth: 36 },
        4: { cellWidth: 30 },
        5: { cellWidth: 'auto' }
      },
      margin: { left: 14, right: 14 }
    });

    const safeName = nomeFile.replace(/\.[^.]+$/, '');
    doc.save(`Scarti_${safeName}.pdf`);
  }

  onDownload(file: ArchivioFileCaricatiDTO): void {
    if (!file.fileId || !file.nome_file) return;
    this.isDownloading.set(file.fileId);
    this.fileApi.downloadImport(file.fileId, file.nome_file).subscribe({
      next: () => this.isDownloading.set(null),
      error: () => this.isDownloading.set(null),
    });
  }

  onChiudi(): void {
    this.dialogRef.close();
  }
}
