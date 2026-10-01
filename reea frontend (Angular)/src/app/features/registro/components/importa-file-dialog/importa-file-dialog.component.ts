import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl, Validators } from '@angular/forms';

import { MatDialogRef, MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { SearchService, AuthService } from '@core/services';
import { AdesioniApiService } from '../../../../api';
import { FileApiService } from '../../../../api/file-api.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-importa-file-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressBarModule
  ],
  templateUrl: './importa-file-dialog.component.html',
  styleUrl: './importa-file-dialog.component.scss'
})
export class ImportaFileDialogComponent {

  private dialogRef = inject(MatDialogRef<ImportaFileDialogComponent>);
  private dialog = inject(MatDialog);
  private searchService = inject(SearchService);
  private authService = inject(AuthService);
  private adesioniApi = inject(AdesioniApiService);
  private fileApi = inject(FileApiService);

  fonteProvenienzaControl = new FormControl<string>('', Validators.required);
  selectedFile = signal<File | null>(null);
  isUploading = signal<boolean>(false);
  isLocked = signal<boolean>(false);
  isDragOver = signal<boolean>(false);
  fontiOptions = signal<{ value: string; label: string }[]>([]);
  uploadError = signal<string | null>(null);

  readonly acceptedFormats = '.csv,.xlsx,.xls';

  private readonly filePatterns: Array<{ keywords: string[]; patterns: RegExp[] }> = [
    {
      keywords: ['preadesion'],
      patterns: [/^Elenco_Ex_Esposti_Amianto_\d{8}/i]
    },
    {
      keywords: ['inail'],
      patterns: [/^INAIL_[AB]_\d{8}/i]
    },
    {
      keywords: ['npla', 'piano', 'lavoro amianto'],
      patterns: [/^NPLA_\d{8}/i]
    },
    {
      keywords: ['spresal'],
      patterns: [
        /^SPRESAL_.+_(esiti|anamnesi)_\d{8}/i,
        /^SPRESAL_(esiti|anamnesi)_.+_\d{8}/i
      ]
    }
  ];

  constructor() {
    this.loadFontiOptions();
  }

  private loadFontiOptions(): void {
    this.searchService.getFontiProvenienza().subscribe({
      next: (fonti) => {
        let filtered = fonti.filter(f => f.value !== '1');

        if (this.authService.isCrpt()) {
          filtered = filtered.filter(f => f.value === '2');
        } else if (this.authService.isSpresalNonPseudo()) {
          filtered = filtered.filter(f => f.value !== '2' && f.value !== '3');
        } else if (this.authService.isInail()) {
          filtered = filtered.filter(f => f.value === '3');
        }

        this.fontiOptions.set(filtered);
        if (filtered.length === 1) {
          this.fonteProvenienzaControl.setValue(filtered[0].value);
        }
      },
      error: (err) => {
        console.error('Errore caricamento fonti:', err);
      }
    });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(false);

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  triggerFileInput(fileInput: HTMLInputElement): void {
    fileInput.value = '';
    fileInput.click();
  }

  private handleFile(file: File): void {
    this.uploadError.set(null);
    const validExtensions = ['.csv', '.xlsx', '.xls'];
    const fileExtension = '.' + file.name.split('.').pop()?.toLowerCase();

    if (!validExtensions.includes(fileExtension)) {
      alert('Formato file non supportato. Utilizzare CSV, XLSX o XLS.');
      return;
    }

    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
      alert('Il file supera la dimensione massima consentita (10MB).');
      return;
    }

    this.fileApi.checkNomeFile(file.name).subscribe({
      next: (esiste) => {
        if (esiste) {
          this.uploadError.set(
            'Import non consentito: esiste già un file da elaborare/elaborato con lo stesso nome.\nSuggerimento: cambiare il nome del file.'
          );
          return;
        }
        this.selectedFile.set(file);
      },
      error: () => {
        this.selectedFile.set(file);
      }
    });
  }

  removeFile(): void {
    this.selectedFile.set(null);
    this.uploadError.set(null);
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    if (fileInput) fileInput.value = '';
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  isFileNameValid(): boolean {
    const file = this.selectedFile();
    const selectedValue = this.fonteProvenienzaControl.value;
    if (!file || !selectedValue) return true;

    const selectedFonte = this.fontiOptions().find(f => f.value === selectedValue);
    if (!selectedFonte) return true;

    const labelLower = selectedFonte.label.toLowerCase();
    for (const group of this.filePatterns) {
      if (group.keywords.some(kw => labelLower.includes(kw))) {
        const fileNameNoExt = file.name.replace(/\.(csv|xlsx|xls)$/i, '');
        return group.patterns.some(p => p.test(fileNameNoExt));
      }
    }
    return true;
  }

  isFormValid(): boolean {
    return this.fonteProvenienzaControl.valid &&
           this.selectedFile() !== null &&
           this.isFileNameValid();
  }

  private isNpla(): boolean {
    const selectedFonte = this.fontiOptions().find(f => f.value === this.fonteProvenienzaControl.value);
    if (!selectedFonte) return false;
    return selectedFonte.label.toLowerCase().includes('npla') ||
           selectedFonte.label.toLowerCase().includes('piano') ||
           selectedFonte.value === '4';
  }

  private isInailA(): boolean {
    const selectedFonte = this.fontiOptions().find(f => f.value === this.fonteProvenienzaControl.value);
    if (!selectedFonte) return false;
    if (!selectedFonte.label.toLowerCase().includes('inail')) return false;
    const file = this.selectedFile();
    if (!file) return false;
    return /^INAIL_A_/i.test(file.name);
  }

  private isInailB(): boolean {
    const selectedFonte = this.fontiOptions().find(f => f.value === this.fonteProvenienzaControl.value);
    if (!selectedFonte) return false;
    if (!selectedFonte.label.toLowerCase().includes('inail')) return false;
    const file = this.selectedFile();
    if (!file) return false;
    return /^INAIL_B_/i.test(file.name);
  }

  private isSpresal(): boolean {
    const selectedFonte = this.fontiOptions().find(f => f.value === this.fonteProvenienzaControl.value);
    if (!selectedFonte) return false;
    return selectedFonte.label.toLowerCase().includes('spresal');
  }

  private isSpresalEsiti(): boolean {
    const file = this.selectedFile();
    if (!file) return false;
    return /esiti/i.test(file.name);
  }

  onImporta(): void {
    if (!this.isFormValid()) return;

    const file = this.selectedFile()!;
    const npla = this.isNpla();
    const inailA = this.isInailA();
    const inailB = this.isInailB();
    const spresal = this.isSpresal();
    const spresalEsiti = spresal && this.isSpresalEsiti();
    const tipo: 'adesioni' | 'npla' | 'inail-a' | 'inail-b' | 'spresal' | 'spresal-esiti' =
      npla ? 'npla' : inailA ? 'inail-a' : inailB ? 'inail-b' : spresalEsiti ? 'spresal-esiti' : spresal ? 'spresal' : 'adesioni';

    this.isLocked.set(true);
    this.isUploading.set(true);
    this.uploadError.set(null);

    this.fileApi.checkConcorrenza(tipo).subscribe({
      next: (res) => {
        this.isUploading.set(false);
        if (res.bloccato) {
          this.isLocked.set(false);
          this.uploadError.set(res.messaggio ?? 'Elaborazione già in corso per questa fonte. Riprovare più tardi.');
          return;
        }
        this.apriConfirm(tipo, npla, inailA, inailB, spresal, spresalEsiti, file);
      },
      error: () => {
        this.isUploading.set(false);
        this.apriConfirm(tipo, npla, inailA, inailB, spresal, spresalEsiti, file);
      }
    });
  }

  private apriConfirm(
    tipo: 'adesioni' | 'npla' | 'inail-a' | 'inail-b' | 'spresal' | 'spresal-esiti',
    npla: boolean, inailA: boolean, inailB: boolean, spresal: boolean, spresalEsiti: boolean,
    file: File
  ): void {
    const ref = this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(
      ConfirmDialogComponent,
      {
        data: {
          message: 'Sicuro di voler continuare l\'operazione?',
          labelSi: 'Sì',
          labelNo: 'No'
        },
        width: '420px',
        disableClose: true
      }
    );
    ref.afterClosed().subscribe(confermato => {
      if (!confermato) {
        this.isLocked.set(false);
        return;
      }
      this.eseguiImport(tipo, npla, inailA, inailB, spresal, spresalEsiti, file);
    });
  }

  private eseguiImport(
    tipo: 'adesioni' | 'npla' | 'inail-a' | 'inail-b' | 'spresal' | 'spresal-esiti',
    npla: boolean, inailA: boolean, inailB: boolean, spresal: boolean, spresalEsiti: boolean,
    file: File
  ): void {
    this.isUploading.set(true);

    const import$ = npla
      ? this.adesioniApi.importNpla(file)
      : inailA
        ? this.adesioniApi.importInailA(file)
        : inailB
          ? this.adesioniApi.importInailB(file)
          : spresalEsiti
            ? this.adesioniApi.importSpresalEsiti(file)
            : spresal
              ? this.adesioniApi.importSpresal(file)
              : this.adesioniApi.importExcel(file);

    import$.subscribe({
      next: (result) => {
        this.isUploading.set(false);
        if (!result.success) {
          this.dialogRef.close({ success: false, errori: result.errori });
          return;
        }
        this.dialogRef.close({
          success: true,
          file: this.selectedFile(),
          fonte: this.fonteProvenienzaControl.value,
          tipo,
          backgroundProcessing: true
        });
      },
      error: (error) => {
        console.error('Errore durante l\'importazione:', error);
        this.isUploading.set(false);
        this.dialogRef.close({
          success: false,
          file: file,
          fonte: this.fonteProvenienzaControl.value,
          errori: [error.message || 'Errore durante l\'importazione del file']
        });
      }
    });
  }

  onReset(): void {
    this.fonteProvenienzaControl.reset();
    this.selectedFile.set(null);
    this.uploadError.set(null);
  }

  onAnnulla(): void {
    if (this.isLocked()) return;
    this.dialogRef.close(null);
  }
}
