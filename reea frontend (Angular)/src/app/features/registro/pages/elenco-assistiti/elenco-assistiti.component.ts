import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSortModule } from '@angular/material/sort';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SelectionModel } from '@angular/cdk/collections';

import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { AssistitiService, SearchService, AssistitiStateService } from '../../../../core/services';
import { AuthService } from '../../../../core/services/auth.service';
import { RuoloUtente } from '../../../../core/models';
import { API_ENDPOINTS } from '../../../../api/api.config';
import { AdesioniApiService, ParametriApiService, AslApiService, AslDTO } from '../../../../api';
import { FileApiService, ExportAllegato4DTO, ArchivioFileCaricatiDTO } from '../../../../api/file-api.service';
import { AuditApiService } from '../../../../api/audit-api.service';
import { ParametroDTO } from '../../../../core/models';
import { Assistito, AssistitoFiltri, PaginatedResponse, StatoAssistito } from '../../../../core/models';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { AppHeaderComponent } from '../../../../shared/components/app-header/app-header.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { ImportaFileDialogComponent } from '../../components/importa-file-dialog/importa-file-dialog.component';
import { ArchivioFileDialogComponent } from '../../components/archivio-file-dialog/archivio-file-dialog.component';
import { StoriaStatiDialogComponent } from '../../components/storia-stati-dialog/storia-stati-dialog.component';
import { FontiAggiornamentoDialogComponent } from '../../components/fonti-aggiornamento-dialog/fonti-aggiornamento-dialog.component';
import { NoteRegistroDialogComponent } from '../../components/note-registro-dialog/note-registro-dialog.component';
import { ValutaAssistitoDialogComponent, ValutaAssistitoDialogResult } from '../../components/valuta-assistito-dialog/valuta-assistito-dialog.component';
import { SearchOption } from '../../../../core/services/search.service';
import { DateMaskDirective } from '../../../../shared/directives/date-mask.directive';

/**
 * Formato data personalizzato per Angular Material Datepicker (dd/MM/yyyy)
 */
export const IT_DATE_FORMATS = {
  parse: {
    dateInput: 'DD/MM/YYYY',
  },
  display: {
    dateInput: 'DD/MM/YYYY',
    monthYearLabel: 'MMM YYYY',
    dateA11yLabel: 'DD/MM/YYYY',
    monthYearA11yLabel: 'MMMM YYYY',
  },
};

/**
 * Componente principale per l'elenco degli assistiti ex esposti amianto
 */
@Component({
  selector: 'app-elenco-assistiti',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatMenuModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule,
    MatSortModule,
    MatTabsModule,
    MatSnackBarModule,
    StatusBadgeComponent,
    AppHeaderComponent,
    DateMaskDirective
  ],
  templateUrl: './elenco-assistiti.component.html',
  styleUrl: './elenco-assistiti.component.scss',
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'it-IT' },
    { provide: MAT_DATE_FORMATS, useValue: IT_DATE_FORMATS }
  ]
})
export class ElencoAssistitiComponent implements OnInit, OnDestroy {
  private readonly FILTER_STORAGE_KEY = 'elenco-assistiti-filters';
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly assistitiService = inject(AssistitiService);
  private readonly searchService = inject(SearchService);
  private readonly adesioniApi = inject(AdesioniApiService);
  private readonly parametriApi = inject(ParametriApiService);
  private readonly aslApi = inject(AslApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly authService = inject(AuthService);
  private readonly fileApi = inject(FileApiService);
  private readonly auditApi = inject(AuditApiService);
  private readonly assistitiState = inject(AssistitiStateService);

  centerNotif = signal<{ text: string; tipo: 'success' | 'error' | 'info' } | null>(null);
  private notifTimer: ReturnType<typeof setTimeout> | null = null;
  private msgAvvioElaborazione = 'Elaborazione avviata. I record saranno processati in background: ricontrolla l\'archivio più tardi.';
  private msgExportMassivoAttesa = '';

  showCenterNotif(text: string, tipo: 'success' | 'error' | 'info', duration = 6000, persist = false): void {
    if (this.notifTimer) clearTimeout(this.notifTimer);
    this.centerNotif.set({ text, tipo });
    if (!persist) {
      this.notifTimer = setTimeout(() => this.centerNotif.set(null), duration);
    }
  }

  assistiti = signal<Assistito[]>([]);
  loading = signal<boolean>(false);
  totalElements = signal<number>(0);
  totalRecordsDB = signal<number>(0);
  currentPage = signal<number>(0);
  pageSize = signal<number>(10);

  esportatoStep = signal<boolean>(false);
  stepOffset = signal<number>(0);
  stepEsaurito = signal<boolean>(false);
  private _lastExportedCount = 0;

  stepDisplayStart = computed(() =>
    this.showEsportaExcel
      ? this.stepOffset() + this.currentPage() * this.pageSize() + 1
      : this.currentPage() * this.pageSize() + 1
  );
  stepDisplayEnd = computed(() => {
    const rawEnd = (this.currentPage() + 1) * this.pageSize();
    if (this.showEsportaExcel) {
      return Math.min(this.stepOffset() + rawEnd, this.stepOffset() + this.totalElements());
    }
    return Math.min(rawEnd, this.totalElements());
  });
  stepDisplayTotal = computed(() =>
    this.showEsportaExcel
      ? this.stepOffset() + this.totalElements()
      : this.totalElements()
  );
  stepSize = computed(() => {
    const val = this.parametri().find(p => p.parametro_cod === 'NumeroRecordRicercaAdesioni')?.parametro_valore;
    return val ? parseInt(val, 10) : 0;
  });

  parametri = signal<ParametroDTO[]>([]);
  exportLoading = signal<boolean>(false);
  exportMessage = signal<string>('');
  exportError = signal<string>('');
  exportSuccess = signal<string>('');

  // Import PDF FileSystem (solo CSI)
  pdfFsUploading = signal<boolean>(false);
  pdfFsCartella = signal<string>('storia_professionale');
  pdfFsFiles = signal<{ nomeFile: string; percorso: string }[]>([]);

  exportArchivioPage = signal<number>(0);
  readonly exportPageSize = 20;

  exportArchivioSlice = computed(() => {
    const all = this.exportArchivio();
    const start = this.exportArchivioPage() * this.exportPageSize;
    return all.slice(start, start + this.exportPageSize);
  });

  getExportTotalPages(): number {
    return Math.max(1, Math.ceil(this.exportArchivio().length / this.exportPageSize));
  }

  goToExportFirstPage(): void    { this.exportArchivioPage.set(0); }
  goToExportPreviousPage(): void { this.exportArchivioPage.update(p => Math.max(0, p - 1)); }
  goToExportNextPage(): void     { this.exportArchivioPage.update(p => Math.min(this.getExportTotalPages() - 1, p + 1)); }
  goToExportLastPage(): void     { this.exportArchivioPage.set(this.getExportTotalPages() - 1); }

  exportChiaroLoading = signal<boolean>(false);
  exportPseudoLoading = signal<boolean>(false);
  downloadingRowIds = signal<Set<number>>(new Set<number>());

  private startRowDownload(id: number): void {
    this.downloadingRowIds.update(s => new Set(s).add(id));
  }
  private endRowDownload(id: number): void {
    this.downloadingRowIds.update(s => { const ns = new Set(s); ns.delete(id); return ns; });
  }
  exportTotaliError = signal<string>('');
  exportAslFiltro = signal<string>('');
  exportArchivio = signal<{
    idElaborazione: number | null;
    nome: string;
    fileId: number | null;
    blobUrl: string | null;
    tipo: 'chiaro' | 'pseudo' | 'allegato4';
    anno?: string;
    stato: string;
    dataRichiesta: string;
    dataGenerazione: string;
    operatore: string;
  }[]>([]);

  // CDU-030: Export Allegato 4 — ANNO ATTUALE, ANNO ATTUALE-1, ANNO ATTUALE-2
  readonly allegato4AnniOptions: number[] = (() => {
    const now = new Date().getFullYear();
    return [now, now - 1, now - 2];
  })();
  allegato4Anno = signal<number>(new Date().getFullYear());
  allegato4Loading = signal<boolean>(false);
  allegato4Error = signal<string>('');

  statiOptions = signal<SearchOption[]>([]);
  fontiOptions = signal<SearchOption[]>([]);
  aslOptions = signal<SearchOption[]>([]);
  aslRaw = signal<AslDTO[]>([]);
  private _appliedFonteProvenienza = signal<string[]>([]);


  // REEA-CDU-003-V01-HomePage - VARIAZIONI - 2: rimossa opzione "Elenchi A e B", selezione multipla
  elencoInailOptions: SearchOption[] = [
    { value: 'A', label: 'Elenco A' },
    { value: 'B', label: 'Elenco B' },
    { value: 'null', label: 'Non appartenente a elenchi INAIL' }
  ];

  sezioneOptions: SearchOption[] = [
    { value: '1', label: 'Prima' },
    { value: '2', label: 'Seconda' },
    { value: '3', label: 'Terza' }
  ];

  // REEA-CDU-003-V01-HomePage - VARIAZIONI - 2: rimossa opzione "SI e NO", selezione multipla
  inseritoSorveglianzaOptions: SearchOption[] = [
    { value: '1', label: 'SI' },
    { value: '0', label: 'NO' },
    { value: 'null', label: 'Non ancora preso in carico' }
  ];

  sortColumn = signal<string>('');
  sortDirection = signal<'asc' | 'desc' | ''>('');

  valutazioneElencoOn = computed(() =>
    this.parametri().find(p => p.parametro_cod === 'VALUTAZIONE_ELENCO')?.parametro_valore === 'ON'
  );
  presaInCaricoElencoOn = computed(() =>
    this.parametri().find(p => p.parametro_cod === 'PRESA_IN_CARICO_ELENCO')?.parametro_valore === 'ON'
  );
  concludiSorveglianzaElencoOn = computed(() =>
    this.parametri().find(p => p.parametro_cod === 'CONCLU_SORVEGLIANAZA_ELENCO')?.parametro_valore === 'ON'
  );

  isSpresal = computed(() =>
    this.authService.currentUser()?.ruolo === RuoloUtente.OPERATORE_SPRESAL
  );
  isSpresalPseudo = computed(() =>
    this.authService.currentUser()?.ruolo === RuoloUtente.OPERATORE_SPRESAL_PSEUDO
  );
  isEpiPseudo = computed(() =>
    this.authService.currentUser()?.ruolo === RuoloUtente.OPERATORE_EPI_PSEUDO
  );

  isPseudo           = this.authService.isPseudo;
  isCrpt             = this.authService.isCrpt;
  isInail            = this.authService.isInail;
  isSpresalNonPseudo = this.authService.isSpresalNonPseudo;
  isCrptPseudo          = computed(() => this.authService.currentUser()?.ruolo === RuoloUtente.OPERATORE_CRPT_PSEUDO);
  isCSI                 = computed(() =>
    this.authService.currentUser()?.ruolo === RuoloUtente.OPERATORE_CSI
  );
  exportAslLabel        = computed(() => {
    const found = this.aslOptions().find(a => a.value === this.exportAslFiltro());
    return found?.label ?? this.exportAslFiltro();
  });

  selection = new SelectionModel<Assistito>(true, []);
  selectedCount = computed(() => this.selection.selected.length);
  selectedDaValutareCount = computed(() =>
    this.selection.selected.filter(a => a.stato === StatoAssistito.DA_VALUTARE).length
  );
  selectedEleggibileCount = computed(() =>
    this.selection.selected.filter(a => a.stato === StatoAssistito.ELEGGIBILE).length
  );

  filterForm!: FormGroup;

  displayedColumns: string[] = [
    'numeroRegistro',
    'cognomeNome',
    'dataAdesione',
    'dataNascita',
    'aslCompetenza',
    'sezione',
    'sorveglianza',
    'elencoInail',
    'fonte',
    'stato',
    'actions'
  ];

  ngOnInit(): void {
    this.initFilterForm();
    if (this.isInail()) {
      this.filterForm.patchValue({ fonteProvenienza: ['3'] });
    }
    this.loadLookupData();
    this.parametriApi.getListaParametri().subscribe(p => this.parametri.set(p));
    this.fileApi.getParametroValore('AVVIO_ELABOLAZIONE_BATCH').subscribe(v => { if (v) this.msgAvvioElaborazione = v; });
    this.fileApi.getParametroValore('EXPORT_MASSIVO_ATTESA').subscribe(v => { if (v) this.msgExportMassivoAttesa = v; });
    const hasQueryParams = this.route.snapshot.queryParamMap.keys.length > 0;
    if (hasQueryParams) {
      this.applyQueryParamFilters();
      sessionStorage.removeItem(this.FILTER_STORAGE_KEY);
      this.assistitiState.clear();
      this.auditApi.salva('read', 'ANAGRAFICA', 'Home page - ricerca assistiti').subscribe();
    } else {
      this.restoreFiltersFromStorage();
    }

    const snapshot = this.assistitiState.restore();
    if (snapshot && !hasQueryParams) {
      this.filterForm.patchValue(snapshot.filters);
      this._allAssistiti = snapshot.allAssistiti;
      this.currentPage.set(snapshot.page);
      this.pageSize.set(snapshot.pageSize);
      this.stepOffset.set(snapshot.stepOffset);
      this.totalRecordsDB.set(snapshot.totalRecordsDB);
      this.stepEsaurito.set(snapshot.stepEsaurito);
      this.esportatoStep.set(snapshot.esportatoStep);
      this.sortColumn.set(snapshot.sortColumn);
      this.sortDirection.set(snapshot.sortDirection as 'asc' | 'desc' | '');
      this._applyCurrentPage();
      this._appliedFonteProvenienza.set(this.filterForm.get('fonteProvenienza')?.value || []);
    } else if (!this.isSpresal()) {
      const fonteVal: string[] = this.filterForm.get('fonteProvenienza')?.value || [];
      const nonAll = fonteVal.filter((v: string) => v !== 'all');
      const stepEligible = fonteVal.includes('all') || nonAll.length === 0 || nonAll.every((v: string) => ['1', '2'].includes(v));
      if (hasQueryParams) this._mostraNotifCaricamento = true;
      this.loadAssistiti(stepEligible ? true : null);
    }
    this.loadExportArchivio();
  }

  private pollingTimer: ReturnType<typeof setInterval> | null = null;
  private exportPollingTimer: ReturnType<typeof setInterval> | null = null;
  private elaborazioniTracked = new Set<number>();

  /** Cache locale: lista completa filtrata, aggiornata ad ogni fetch */
  private _allAssistiti: Assistito[] = [];
  private _mostraNotifCaricamento = false;
  /** Snapshot JSON dei filtri dell'ultima applicazione andata a buon fine (BE o client-side). */
  private _lastAppliedFiltriJson: string | null = null;
  /** Valore corrente del mat-select "Iniziale cognome" — gestito separatamente perché non ha formControlName. */
  _cognomeLettRange = signal<{ da: string; a: string } | null>(null);

  private _applyCurrentPage(): void {
    const start = this.currentPage() * this.pageSize();
    const end = start + this.pageSize();
    this.assistiti.set(this._allAssistiti.slice(start, end));
    this.totalElements.set(this._allAssistiti.length);
  }

  /**
   * Verifica se il filtro corrente è compatibile con la master list salvata.
   * Regola: tutti i filtri categorici (fonte, ASL, stato, sezione, sorveglianza, elenco,
   * data, range lettere) devono essere IDENTICI al master. Solo i campi di testo
   * (CF, cognome, nome) possono cambiare liberamente senza chiamata al BE.
   */
  private _isMasterCompatible(newFiltri: AssistitoFiltri): boolean {
    if (!this.assistitiState.getMasterList()) return false;
    const mf = this.assistitiState.getMasterFilterState();
    if (!mf) return false;

    const toArr = (v: unknown) => ((v ?? []) as string[]).filter(x => x !== 'all').sort().join('|');

    // Tutti i filtri categorici devono essere identici al master
    if (toArr(newFiltri.fonteProvenienza) !== toArr(mf.fonteProvenienza)) return false;
    if (String(newFiltri.assistenzaAslId ?? '') !== String(mf.assistenzaAslId ?? '')) return false;
    if (toArr(newFiltri.sezione) !== toArr(mf.sezione)) return false;
    if (toArr(newFiltri.stato) !== toArr(mf.stato)) return false;
    if (toArr(newFiltri.inseritoInSorveglianza) !== toArr(mf.inseritoInSorveglianza)) return false;
    if (toArr(newFiltri.elencoInail) !== toArr(mf.elencoInail)) return false;
    // Filtri destra: se attivi (nuovi o nel master), sempre BE — mai client-side
    if ((newFiltri.codiceFiscale ?? '').trim() || (mf.codiceFiscale ?? '').trim()) return false;
    if ((newFiltri.cognome ?? '').trim() || (mf.cognome ?? '').trim()) return false;
    if ((newFiltri.nome ?? '').trim() || (mf.nome ?? '').trim()) return false;
    if (newFiltri.dataNascita || mf.dataNascita) return false;
    if ((newFiltri.cognomeLettDa ?? '').trim() || (mf.cognomeLettDa ?? '').trim()) return false;

    return true;
  }

  private _filterClientSide(filtri: AssistitoFiltri, master: Assistito[]): Assistito[] {
    return master.filter(a => {
      // Codice fiscale (contains)
      if (filtri.codiceFiscale?.trim()) {
        if (!(a.codiceFiscale ?? '').toUpperCase().includes(filtri.codiceFiscale.trim().toUpperCase())) return false;
      }
      // Cognome (startsWith)
      if (filtri.cognome?.trim()) {
        if (!(a.cognome ?? '').toUpperCase().startsWith(filtri.cognome.trim().toUpperCase())) return false;
      }
      // Nome (startsWith)
      if (filtri.nome?.trim()) {
        if (!(a.nome ?? '').toUpperCase().startsWith(filtri.nome.trim().toUpperCase())) return false;
      }
      // Data nascita
      if (filtri.dataNascita) {
        const toIso = (d: Date | string | null | undefined) =>
          d ? (d instanceof Date ? d : new Date(d as string)).toISOString().split('T')[0] : '';
        if (toIso(filtri.dataNascita as Date) !== toIso(a.dataNascita)) return false;
      }
      // Range alfabetico cognome
      if (filtri.cognomeLettDa?.trim() && filtri.cognomeLettA?.trim()) {
        const first = (a.cognome?.[0] ?? '').toUpperCase();
        if (first < filtri.cognomeLettDa.toUpperCase() || first > filtri.cognomeLettA.toUpperCase()) return false;
      }
      // Fonte provenienza
      const fonteIds = ((filtri.fonteProvenienza ?? []) as string[]).filter(v => v !== 'all');
      if (fonteIds.length > 0) {
        const labels = fonteIds.map(id => this.fontiOptions().find(o => o.value === id)?.label ?? '');
        if (!labels.some(l => a.fonteProvenienza === l)) return false;
      }
      // Stato
      const statoIds = ((filtri.stato ?? []) as string[]).filter(v => v !== 'all');
      if (statoIds.length > 0) {
        const labels = statoIds.map(id => this.statiOptions().find(o => o.value === id)?.label ?? '');
        if (!labels.includes(a.stato as string)) return false;
      }
      // Sezione
      const sezioni = ((filtri.sezione ?? []) as string[]).filter(v => v !== 'all');
      if (sezioni.length > 0) {
        if (!sezioni.includes(String(a.sezione ?? ''))) return false;
      }
      // Inserito in sorveglianza
      const sorv = ((filtri.inseritoInSorveglianza ?? []) as string[]).filter(v => v !== 'all');
      if (sorv.length > 0) {
        const matched = sorv.some(v => {
          if (v === '1') return a.insertoInSorveglianza === true;
          if (v === '0') return a.insertoInSorveglianza === false;
          return a.insertoInSorveglianza == null;
        });
        if (!matched) return false;
      }
      // Elenco INAIL
      const elenchi = ((filtri.elencoInail ?? []) as string[]).filter(v => v !== 'all');
      if (elenchi.length > 0) {
        const val = a.tipoElencoInail ?? 'null';
        if (!elenchi.includes(val)) return false;
      }
      // ASL competenza
      if (filtri.assistenzaAslId && filtri.assistenzaAslId !== '') {
        const aslLabel = this.aslOptions().find(o => o.value === String(filtri.assistenzaAslId))?.label ?? '';
        if (a.aslCompetenza !== aslLabel) return false;
      }
      return true;
    });
  }

  ngOnDestroy(): void {
    this.saveFiltersToStorage();
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
    if (this.exportPollingTimer) {
      clearInterval(this.exportPollingTimer);
      this.exportPollingTimer = null;
    }
  }

  private startExportPolling(): void {
    if (this.exportPollingTimer) return;
    this.exportPollingTimer = setInterval(() => {
      const hasPending = this.exportArchivio().some(r => r.stato !== 'ELABORAZIONE TERMINATA');
      if (!hasPending) {
        clearInterval(this.exportPollingTimer!);
        this.exportPollingTimer = null;
        return;
      }
      this.loadExportArchivio(false);
    }, 5000);
  }

  private startPollingForCompletion(): void {
    if (this.pollingTimer) return;

    this.fileApi.getArchivioFileCaricati().subscribe({
      next: (files: ArchivioFileCaricatiDTO[]) => {
        files
          // Traccia tutto ciò che non è ancora in uno stato finale (non solo "DA ELABORARE":
          // con file grandi lo stato può essere già "ELABORAZIONE IN CORSO" quando partiamo).
          .filter(f => {
            const stato = (f.stato ?? '').toUpperCase();
            return stato.length > 0 && !stato.includes('TERMINAT') && !stato.includes('FALLITA');
          })
          .forEach(f => { if (f.elaborazione_id) this.elaborazioniTracked.add(f.elaborazione_id); });

        if (this.elaborazioniTracked.size === 0) return;

        this.pollingTimer = setInterval(() => {
          this.fileApi.getArchivioFileCaricati().subscribe({
            next: (current: ArchivioFileCaricatiDTO[]) => {
              current.forEach(f => {
                if (!f.elaborazione_id || !this.elaborazioniTracked.has(f.elaborazione_id)) return;
                const stato = (f.stato ?? '').toUpperCase();
                if (stato.includes('TERMINAT') || stato.includes('FALLITA')) {
                  this.elaborazioniTracked.delete(f.elaborazione_id);
                  const fallita = stato.includes('FALLITA');
                  this.showCenterNotif(
                    fallita
                      ? `Elaborazione ${f.nome_file ?? ''} terminata con errori.`
                      : `Elaborazione ${f.nome_file ?? ''} completata con successo!`,
                    fallita ? 'error' : 'success',
                    0,
                    true
                  );
                  if (!fallita) this.loadAssistiti();
                }
              });
              if (this.elaborazioniTracked.size === 0) {
                clearInterval(this.pollingTimer!);
                this.pollingTimer = null;
              }
            }
          });
        }, 15000);
      }
    });
  }

  private saveFiltersToStorage(): void {
    const state = {
      filters: this.filterForm.value,
      page: this.currentPage(),
      pageSize: this.pageSize(),
      sortColumn: this.sortColumn(),
      sortDirection: this.sortDirection()
    };
    sessionStorage.setItem(this.FILTER_STORAGE_KEY, JSON.stringify(state));
  }

  private restoreFiltersFromStorage(): void {
    const stored = sessionStorage.getItem(this.FILTER_STORAGE_KEY);
    if (!stored) return;
    try {
      const state = JSON.parse(stored);
      if (state.filters) {
        if (state.filters.dataNascita) {
          state.filters.dataNascita = new Date(state.filters.dataNascita);
        }
        this.filterForm.patchValue(state.filters);
      }
      if (state.page !== undefined) this.currentPage.set(state.page);
      if (state.pageSize !== undefined) this.pageSize.set(state.pageSize);
      if (state.sortColumn !== undefined) this.sortColumn.set(state.sortColumn);
      if (state.sortDirection !== undefined) this.sortDirection.set(state.sortDirection);
    } catch {
      // ignore corrupted data
    }
  }

  private loadExportArchivio(includiAllegato4 = true): void {
    this.fileApi.getExportList(undefined).subscribe(list => {
      const cfCorrente = this.authService.currentUser()?.codiceFiscale ?? '';
      const filteredList = cfCorrente ? list.filter(item => (item.operatore ?? '') === cfCorrente) : list;
      const rows = filteredList.map(item => ({
        idElaborazione: item.elaborazione_id ?? null,
        nome: item.nome ?? '',
        fileId: item.file_id ?? null,
        blobUrl: null as string | null,
        tipo: (item.tipo ?? 'chiaro') as 'chiaro' | 'pseudo' | 'allegato4',
        anno: undefined as string | undefined,
        stato: item.stato ?? '',
        dataRichiesta: this.parseAndFmtDate(item.data_richiesta),
        dataGenerazione: this.parseAndFmtDate(item.data_generazione),
        operatore: item.operatore ?? ''
      }));
      if (this.isCrpt() || this.isCSI()) {
        if (includiAllegato4) {
          this.fileApi.getExportListAllegato4().subscribe((a4list: ExportAllegato4DTO[]) => {
            const filteredA4 = cfCorrente ? a4list.filter(item => (item.operatore ?? '') === cfCorrente) : a4list;
            const a4rows = filteredA4.map(item => ({
              idElaborazione: item.elaborazione_id ?? null,
              nome: item.file_name ?? '',
              fileId: null as number | null,
              blobUrl: null as string | null,
              tipo: 'allegato4' as 'chiaro' | 'pseudo' | 'allegato4',
              anno: item.anno ? String(item.anno) : this.estraiAnnoRiferimento(item.file_name ?? ''),
              stato: item.stato ?? '',
              dataRichiesta: this.parseAndFmtDate(item.data_richiesta),
              dataGenerazione: this.parseAndFmtDate(item.data_generazione),
              operatore: item.operatore ?? ''
            }));
            const merged = [...rows, ...a4rows].sort((a, b) => (b.idElaborazione ?? 0) - (a.idElaborazione ?? 0));
            this.exportArchivio.set(merged);
            this.exportArchivioPage.set(0);
            if (merged.some(r => r.stato !== 'ELABORAZIONE TERMINATA')) this.startExportPolling();
          });
        } else {
          const existingA4 = this.exportArchivio().filter(r => r.tipo === 'allegato4');
          const merged = [...rows, ...existingA4].sort((a, b) => (b.idElaborazione ?? 0) - (a.idElaborazione ?? 0));
          this.exportArchivio.set(merged);
          this.exportArchivioPage.set(0);
          if (merged.some(r => r.stato !== 'ELABORAZIONE TERMINATA')) this.startExportPolling();
        }
      } else {
        this.exportArchivio.set(rows);
        if (rows.some(r => r.stato !== 'ELABORAZIONE TERMINATA')) this.startExportPolling();
      }
    });
  }

  private estraiAnnoRiferimento(nome: string): string {
    // Formato atteso: export_PIEMONTE_all4_<<anno>>_<<aaaammddhhmm>>
    const match = nome.match(/all4_(\d{4})_/);
    return match ? match[1] : '';
  }

  downloadAllegato4(idElaborazione: number | null, nome: string): void {
    if (!idElaborazione) return;
    this.startRowDownload(idElaborazione);
    this.fileApi.downloadAllegato4(idElaborazione, nome).subscribe({
      complete: () => this.endRowDownload(idElaborazione),
      error: () => this.endRowDownload(idElaborazione)
    });
  }

  avviaExportAllegato4(): void {
    if (this.allegato4Loading()) return;
    this.allegato4Loading.set(true);
    this.allegato4Error.set('');
    this.fileApi.avviaExportAllegato4(this.allegato4Anno()).subscribe({
      next: () => {
        this.allegato4Loading.set(false);
        this.loadExportArchivio();
        this.startExportPolling();
        if (this.msgExportMassivoAttesa) this.showCenterNotif(this.msgExportMassivoAttesa, 'info', 10000);
      },
      error: (err) => {
        this.allegato4Loading.set(false);
        const msg: string = err?.error?.message ?? err?.message ?? '';
        if (msg.toLowerCase().includes('nessun dato') || err?.status === 500) {
          this.showCenterNotif(`Nessun dato per il ${this.allegato4Anno()}`, 'info', 6000);
        }
      }
    });
  }

  /**
   * Applica i filtri passati come query params (dalla homepage)
   * Secondo documento CDU-003, ogni pulsante della homepage
   * apre questa pagina con filtri preimpostati
   */
  private applyQueryParamFilters(): void {
    const queryParams = this.route.snapshot.queryParamMap;

    // Filtro Fonte di Provenienza (pulsanti BLU: PULSANTE1-4)
    const fonte = queryParams.get('fonte');
    if (fonte) {
      this.filterForm.patchValue({
        fonteProvenienza: [fonte]
      });
    }

    // Filtro Sezione (pulsanti ARANCIONE: PULSANTE5-7)
    const sezione = queryParams.get('sezione');
    if (sezione) {
      this.filterForm.patchValue({
        sezione: [sezione]
      });
    }

    // Filtro Stato (pulsanti GRIGIO: PULSANTE8-9)
    // Gli stati possono essere multipli (separati da virgola)
    const stato = queryParams.get('stato');
    if (stato) {
      const stati = stato.split(',');
      this.filterForm.patchValue({
        stato: stati
      });
    }
  }

  private initFilterForm(): void {
    this.filterForm = this.fb.group({
      codiceFiscale: [''],
      cognome: [''],
      nome: [''],
      dataNascita: [null],
      fonteProvenienza: [[] as string[]],
      sezione: [[] as string[]],
      stato: [[] as string[]],
      inseritoInSorveglianza: [[] as string[]],
      elencoInail: [[] as string[]],
      assistenzaAslId: [''],
      cognomeLettDa: [''],
      cognomeLettA: ['']
    });
  }

  private loadLookupData(): void {
    this.searchService.getStati().subscribe(stati => {
      const nascondiCaricatoDaValutare = this.isPseudo() || this.isSpresalNonPseudo();
      const filtered = nascondiCaricatoDaValutare
        ? stati.filter(s => s.value !== '1' && s.value !== '10')
        : stati;
      this.statiOptions.set(filtered);
    });

    this.searchService.getFontiProvenienza().subscribe(fonti => {
      this.fontiOptions.set(fonti);
    });

    this.aslApi.getListaFiltroAssistiti().subscribe(lista => {
      this.aslRaw.set(lista);
      this.aslOptions.set(lista.map(a => ({ value: String(a.asl_id), label: a.asl_azienda_desc })));
      if (this.isSpresal()) {
        const codAzienda = this.authService.currentUser()?.collocazione?.codiceAzienda;
        const match = codAzienda ? lista.find(a => a.asl_cod === codAzienda) : null;
        if (match) {
          this.exportAslFiltro.set(String(match.asl_id));
          this.filterForm.patchValue({ assistenzaAslId: String(match.asl_id) });
          this.filterForm.get('assistenzaAslId')?.disable();
        }
        const fonteVal: string[] = this.filterForm.get('fonteProvenienza')?.value || [];
        const nonAll = fonteVal.filter((v: string) => v !== 'all');
        const stepAttivo = fonteVal.includes('all') || nonAll.length === 0 || nonAll.every((v: string) => ['1', '2'].includes(v));
        this.loadAssistiti(stepAttivo);
      }
    });
  }

  loadAssistiti(azzeraContatore: boolean | null = null): void {
    this.loading.set(true);
    const prevAssistiti = [...this._allAssistiti];
    const prevStepOffset = this.stepOffset();
    this._allAssistiti = [];
    this.totalRecordsDB.set(0);
    this._applyCurrentPage();
    this._appliedFonteProvenienza.set(this.filterForm.get('fonteProvenienza')?.value || []);

    const filtri: AssistitoFiltri = this.filterForm.getRawValue();

    this.assistitiService.getAssistiti(filtri, { page: 0, size: 99999 }, azzeraContatore).subscribe({
      next: (response: PaginatedResponse<Assistito>) => {
        if (azzeraContatore === false && response.content.length === 0) {
          this._allAssistiti = prevAssistiti;
          this.stepOffset.set(prevStepOffset);
          this._applyCurrentPage();
          this.loading.set(false);
          this.stepEsaurito.set(true);
          // const fv = this.filterForm.getRawValue();
          // const isRicercaNominale = !!(fv.nome?.trim() || fv.cognome?.trim() || fv.codiceFiscale?.trim());
          // this.showCenterNotif(
          //   isRicercaNominale ? 'Assistito non trovato.' : 'Tutti gli assistiti sono stati caricati.',
          //   'info', 0, true
          // );
          return;
        }
        this.stepEsaurito.set(false);
        if (azzeraContatore === false) {
          this.stepOffset.set(prevStepOffset + prevAssistiti.length);
        }
        this._allAssistiti = response.content;
        const totDB = response.totalRecordsDB ?? response.content.length;
        this.totalRecordsDB.set(totDB);
        this._applyCurrentPage();
        this.loading.set(false);
        this.selection.clear();
        if (response.content.length > 0) {
          const masterIsComplete = response.content.length >= totDB;
          this.assistitiState.saveMasterList(response.content, filtri, masterIsComplete);
        } else {
          this.assistitiState.clearMasterList();
        }
        if (this._mostraNotifCaricamento && response.content.length > 0) {
          const n = response.content.length;
          const msg = n < totDB
            ? `Sono stati caricati i primi ${n} su un totale di ${totDB} assistiti. Per una ricerca più precisa, aggiungere ulteriori filtri.`
            : `Sono stati caricati ${n} assistiti.`;
          this.showCenterNotif(msg, 'info', 0, true);
        }
        this._mostraNotifCaricamento = false;
        this._lastAppliedFiltriJson = JSON.stringify(filtri);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Errore nel caricamento degli assistiti:', error);
        this._allAssistiti = [];
        this._applyCurrentPage();
        this.loading.set(false);
        const isTimeout = error.status === 502;
        if (isTimeout) {
          const msg = this.getParametroValore('ERRORE_TIMEOUT') || 'Ricerca non andata a buon fine: aggiungere ulteriori filtri';
          this.showCenterNotif(msg, 'error', 10000);
        } else {
          const msg = this.getParametroValore('ERRORE_GENERICO') || 'Ricerca non andata a buon fine: riprovare più tardi';
          this.showCenterNotif(msg, 'error', 10000);
        }
      }
    });
  }

  private statoIdToLabel(statoId: number): string {
    const map: Record<number, string> = {
      1: 'Da valutare',
      2: 'Eleggibile',
      3: 'Non eleggibile',
      4: 'Preso in carico',
      5: 'Avviato alla sorveglianza',
      6: 'Escluso dalla sorveglianza',
      7: 'Sorveglianza conclusa',
      8: 'Escluso per decesso',
      9: 'Escluso per emigrazione',
      10: 'Caricato',
      11: 'Non preso in carico'
    };
    return map[statoId] ?? String(statoId);
  }

  /**
   * Aggiornamento ottimistico: imposta subito il nuovo stato nella lista locale,
   * poi ricarica dal backend dopo 500ms per lasciar committare il DB.
   * Evita la race condition dove il reload arriva prima della commit.
   */
  private aggiornaStatoOttimistico(assistitoId: string, nuovoStatoId: number): void {
    const statoIdMap: Record<number, StatoAssistito> = {
      1: StatoAssistito.DA_VALUTARE,
      2: StatoAssistito.ELEGGIBILE,
      3: StatoAssistito.NON_ELEGGIBILE,
      4: StatoAssistito.PRESO_IN_CARICO,
      5: StatoAssistito.AVVIATO_SORV,
      6: StatoAssistito.ESCLUSO_SORV,
      7: StatoAssistito.CONCLU_SORV,
      8: StatoAssistito.ESCLUSO_DECESSO,
      9: StatoAssistito.ESCLUSO_EMIGRAZIONE,
      10: StatoAssistito.CARICATO,
      11: StatoAssistito.NON_PRESO_IN_CARICO
    };
    const nuovoStato = statoIdMap[nuovoStatoId] ?? StatoAssistito.DA_VALUTARE;
    this._allAssistiti = this._allAssistiti.map(a =>
      a.id === assistitoId ? { ...a, stato: nuovoStato } : a
    );
    this._applyCurrentPage();
  }

  applicaFiltri(): void {
    const currentFiltriJson = JSON.stringify(this.filterForm.getRawValue());
    if (this._lastAppliedFiltriJson === currentFiltriJson && this._allAssistiti.length > 0) {
      return;
    }

    this.assistitiState.clear();
    this.currentPage.set(0);
    this.stepOffset.set(0);
    this.esportatoStep.set(false);
    this.stepEsaurito.set(false);
    this.saveFiltersToStorage();
    this.auditApi.salva('read', 'ANAGRAFICA', 'Ricerca assistiti - ricerca con filtri').subscribe();

    const masterList = this.assistitiState.getMasterList();
    if (masterList) {
      const filtri: AssistitoFiltri = this.filterForm.getRawValue();
      if (this._isMasterCompatible(filtri)) {
        this._allAssistiti = this._filterClientSide(filtri, masterList);
        this._appliedFonteProvenienza.set(this.filterForm.get('fonteProvenienza')?.value || []);
        this._applyCurrentPage();
        this.selection.clear();
        if (this._allAssistiti.length === 0) {
          const isRicercaNominale = !!(filtri.nome?.trim() || filtri.cognome?.trim() || filtri.codiceFiscale?.trim());
          this.showCenterNotif(
            isRicercaNominale ? 'Assistito non trovato.' : 'Nessun risultato trovato.',
            'info', 0, true
          );
        } else {
          const n = this._allAssistiti.length;
          const totDB = this.totalRecordsDB();
          const msg = n < totDB
            ? `Sono stati caricati i primi ${n} su un totale di ${totDB} assistiti. Per una ricerca più precisa, aggiungere ulteriori filtri.`
            : `Sono stati caricati ${n} assistiti.`;
          this.showCenterNotif(msg, 'info', 0, true);
        }
        this._lastAppliedFiltriJson = currentFiltriJson;
        return;
      }
      this.assistitiState.clearMasterList();
    }

    const fonteVal: string[] = this.filterForm.get('fonteProvenienza')?.value || [];
    const nonAll = fonteVal.filter((v: string) => v !== 'all');
    const consentite = ['1', '2'];
    const fv = this.filterForm.getRawValue();
    const hasTextFilter = !!(fv.cognome?.trim() || fv.nome?.trim() || fv.codiceFiscale?.trim());
    const stepAttivo = !hasTextFilter && (fonteVal.includes('all') || nonAll.length === 0 || nonAll.every((v: string) => consentite.includes(v)));
    this._mostraNotifCaricamento = true;
    this.loadAssistiti(stepAttivo ? true : null);
  }

  annullaFiltri(): void {
    this.assistitiState.clearAll();
    this.filterForm.reset();
    this.filterForm.patchValue({ assistenzaAslId: '' });
    if (this.isInail()) {
      this.filterForm.patchValue({ fonteProvenienza: ['3'] });
    }
    if (this.isSpresal()) {
      const codAzienda = this.authService.currentUser()?.collocazione?.codiceAzienda;
      const match = codAzienda ? this.aslRaw().find(a => a.asl_cod === codAzienda) : null;
      if (match) this.filterForm.patchValue({ assistenzaAslId: String(match.asl_id) });
    }
    this.currentPage.set(0);
    this.stepOffset.set(0);
    this.esportatoStep.set(false);
    this.stepEsaurito.set(false);
    this.sortColumn.set('');
    this.sortDirection.set('');
    this._allFontiSelected = false;
    this._allStatiSelected = false;
    this._allSezioniSelected = false;
    this.centerNotif.set(null);
    sessionStorage.removeItem(this.FILTER_STORAGE_KEY);
    this._lastAppliedFiltriJson = null;
    this._cognomeLettRange.set(null);
    // Nessuna chiamata BE — l'utente usa "Applica filtri" per ricaricare
  }

  clearMultiSelect(controlName: string): void {
    this.filterForm.get(controlName)?.setValue([]);
  }

  /**
   * Gestisce la selezione nel filtro Fonte
   * Se 'all' viene selezionato/deselezionato, aggiorna tutte le opzioni
   */
  onFonteSelectionChange(event: MatSelectChange): void {
    const currentValue: string[] = event.value || [];
    const allFontiValues = this.fontiOptions().map(f => f.value);
    const control = this.filterForm.get('fonteProvenienza');

    if (currentValue.includes('all') && !this._allFontiSelected) {
      // 'all' appena selezionato → seleziona tutto
      this._allFontiSelected = true;
      control?.setValue(['all', ...allFontiValues], { emitEvent: false });
    } else if (!currentValue.includes('all') && this._allFontiSelected) {
      // 'all' deselezionato (1 click) → svuota tutto
      this._allFontiSelected = false;
      control?.setValue([], { emitEvent: false });
    } else if (currentValue.includes('all')) {
      // Voce individuale deselezionata mentre 'all' era attivo → rimuovi 'all'
      this._allFontiSelected = false;
      control?.setValue(currentValue.filter(v => v !== 'all'), { emitEvent: false });
    }
  }
  private _allFontiSelected = false;

  getFontiDisplayLabel(): string {
    const selected: string[] = this.filterForm.get('fonteProvenienza')?.value || [];
    const individual = selected.filter(v => v !== 'all');
    if (individual.length === 0) return '';
    return this.fontiOptions().filter(f => individual.includes(f.value)).map(f => f.label).join(', ');
  }

  setLetterRange(value: { da: string; a: string } | null): void {
    this._cognomeLettRange.set(value ?? null);
    this.filterForm.patchValue({ cognomeLettDa: value?.da ?? '', cognomeLettA: value?.a ?? '' });
  }

  compareLettRange(a: { da: string; a: string } | null, b: { da: string; a: string } | null): boolean {
    if (!a && !b) return true;
    if (!a || !b) return false;
    return a.da === b.da && a.a === b.a;
  }

  get showEsportaExcel(): boolean {
    const selected = this._appliedFonteProvenienza();
    const consentite = ['1', '2']; // '1' = Segnalazione MMG, '2' = Preadesioni
    const nonAll = selected.filter((v: string) => v !== 'all');
    return nonAll.length > 0 && nonAll.every((v: string) => consentite.includes(v));
  }

  /**
   * Gestisce la selezione nel filtro Stato
   */
  onStatoSelectionChange(event: MatSelectChange): void {
    const currentValue: string[] = event.value || [];
    const allStatiValues = this.statiOptions().map(s => s.value);
    const control = this.filterForm.get('stato');

    if (currentValue.includes('all') && !this._allStatiSelected) {
      this._allStatiSelected = true;
      control?.setValue(['all', ...allStatiValues], { emitEvent: false });
    } else if (!currentValue.includes('all') && this._allStatiSelected) {
      this._allStatiSelected = false;
      control?.setValue([], { emitEvent: false });
    } else if (currentValue.includes('all')) {
      this._allStatiSelected = false;
      control?.setValue(currentValue.filter(v => v !== 'all'), { emitEvent: false });
    }
  }
  private _allStatiSelected = false;

  getStatiDisplayLabel(): string {
    const selected: string[] = this.filterForm.get('stato')?.value || [];
    const individual = selected.filter(v => v !== 'all');
    if (individual.length === 0) return '';
    return this.statiOptions().filter(s => individual.includes(s.value)).map(s => s.label).join(', ');
  }

  /**
   * Gestisce la selezione nel filtro Sezione
   */
  onSezioneSelectionChange(event: MatSelectChange): void {
    const currentValue: string[] = event.value || [];
    const allSezioniValues = this.sezioneOptions.filter(s => s.value !== '').map(s => s.value);
    const control = this.filterForm.get('sezione');

    if (currentValue.includes('all') && !this._allSezioniSelected) {
      this._allSezioniSelected = true;
      control?.setValue(['all', ...allSezioniValues], { emitEvent: false });
    } else if (!currentValue.includes('all') && this._allSezioniSelected) {
      this._allSezioniSelected = false;
      control?.setValue([], { emitEvent: false });
    } else if (currentValue.includes('all')) {
      this._allSezioniSelected = false;
      control?.setValue(currentValue.filter(v => v !== 'all'), { emitEvent: false });
    }
  }
  private _allSezioniSelected = false;

  getSezioniDisplayLabel(): string {
    const selected: string[] = this.filterForm.get('sezione')?.value || [];
    const individual = selected.filter(v => v !== 'all');
    if (individual.length === 0) return '';
    return this.sezioneOptions.filter(s => individual.includes(s.value)).map(s => s.label).join(', ');
  }

  toggleSort(column: string, direction: 'asc' | 'desc'): void {
    if (this.sortColumn() === column && this.sortDirection() === direction) {
      this.sortColumn.set('');
      this.sortDirection.set('');
      const master = this.assistitiState.getMasterList();
      if (master) {
        this._allAssistiti = this._filterClientSide(this.filterForm.getRawValue(), master);
        this.currentPage.set(0);
        this._applyCurrentPage();
      } else {
        this.loadAssistiti();
      }
    } else {
      this.sortColumn.set(column);
      this.sortDirection.set(direction);
      this.sortData();
    }
  }

  private sortData(): void {
    const data = [...this._allAssistiti];
    const column = this.sortColumn();
    const direction = this.sortDirection();

    if (!column || !direction) {
      return;
    }

    data.sort((a, b) => {
      let valueA: any;
      let valueB: any;

      switch (column) {
        case 'numeroRegistro':
          valueA = parseInt(a.numeroRegistro) || 0;
          valueB = parseInt(b.numeroRegistro) || 0;
          break;
        case 'cognomeNome':
          valueA = `${a.cognome} ${a.nome}`.toLowerCase();
          valueB = `${b.cognome} ${b.nome}`.toLowerCase();
          break;
        case 'dataAdesione':
  valueA = a.dataAdesione ? new Date(a.dataAdesione).getTime() : 0;
  valueB = b.dataAdesione ? new Date(b.dataAdesione).getTime() : 0;
  break;
        case 'dataNascita':
          valueA = a.dataNascita ? new Date(a.dataNascita as string).getTime() : 0;
          valueB = b.dataNascita ? new Date(b.dataNascita as string).getTime() : 0;
          break;
        case 'sezione':
          valueA = a.sezione ?? '';
          valueB = b.sezione ?? '';
          break;
        case 'sorveglianza':
          valueA = a.insertoInSorveglianza === true ? 1 : a.insertoInSorveglianza === false ? 0 : -1;
          valueB = b.insertoInSorveglianza === true ? 1 : b.insertoInSorveglianza === false ? 0 : -1;
          break;
        case 'elencoInail':
          valueA = a.tipoElencoInail ?? '';
          valueB = b.tipoElencoInail ?? '';
          break;
        case 'aslCompetenza':
          valueA = a.aslCompetenza?.toLowerCase() || '';
          valueB = b.aslCompetenza?.toLowerCase() || '';
          break;
        case 'fonte':
          valueA = a.fonteProvenienza?.toLowerCase() || '';
          valueB = b.fonteProvenienza?.toLowerCase() || '';
          break;
        case 'stato':
          valueA = a.stato?.toLowerCase() || '';
          valueB = b.stato?.toLowerCase() || '';
          break;
        default:
          valueA = '';
          valueB = '';
      }

      const comparison = valueA < valueB ? -1 : valueA > valueB ? 1 : 0;
      return direction === 'asc' ? comparison : -comparison;
    });

    this._allAssistiti = data;
    this.currentPage.set(0);
    this._applyCurrentPage();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this._applyCurrentPage();
  }

  getTotalPages(): number {
    return Math.ceil(this.totalElements() / this.pageSize());
  }

  goToFirstPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.set(0);
      this._applyCurrentPage();
    }
  }

  goToPreviousPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update(page => page - 1);
      this._applyCurrentPage();
    }
  }

  goToNextPage(): void {
    if (this.currentPage() < this.getTotalPages() - 1) {
      this.currentPage.update(page => page + 1);
      this._applyCurrentPage();
    }
  }

  goToLastPage(): void {
    const lastPage = this.getTotalPages() - 1;
    if (this.currentPage() < lastPage) {
      this.currentPage.set(lastPage);
      this._applyCurrentPage();
    }
  }

  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.assistiti().length;
    return numSelected === numRows;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.assistiti().forEach(row => this.selection.select(row));
    }
  }

  valutaAssistiti(): void {
    const ids = this.selection.selected
      .filter(a => a.stato === StatoAssistito.DA_VALUTARE)
      .map(a => a.id!);

    if (ids.length === 0) {
      alert('Seleziona almeno un assistito in stato "Da valutare"');
      return;
    }

    if (confirm(`Confermi di voler valutare ${ids.length} assistiti?`)) {
      this.loading.set(true);
      this.assistitiService.validaAssistiti(ids).subscribe({
        next: (result) => {
          alert(`${result.success} assistiti valutati con successo`);
          this.loadAssistiti();
        },
        error: (error) => {
          console.error('Errore nella valutazione:', error);
          alert('Errore durante la valutazione degli assistiti');
          this.loading.set(false);
        }
      });
    }
  }

  inviaAllaSorveglianza(): void {
    const ids = this.selection.selected
      .filter(a => a.stato === StatoAssistito.ELEGGIBILE)
      .map(a => a.id!);

    if (ids.length === 0) {
      alert('Seleziona almeno un assistito in stato "Eleggibile"');
      return;
    }

    if (confirm(`Confermi di voler inviare ${ids.length} assistiti al monitoraggio?`)) {
      this.loading.set(true);
      this.assistitiService.inviaAlMonitoraggio(ids).subscribe({
        next: (result) => {
          alert(`${result.success} assistiti inviati al monitoraggio con successo`);
          this.loadAssistiti();
        },
        error: (error) => {
          console.error('Errore nell\'invio al monitoraggio:', error);
          alert('Errore durante l\'invio al monitoraggio');
          this.loading.set(false);
        }
      });
    }
  }

  valutaSingoloAssistito(assistito: Assistito): void {
    const dialogRef = this.dialog.open(ValutaAssistitoDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      disableClose: false,
      data: { assistito }
    });

    dialogRef.afterClosed().subscribe((result: ValutaAssistitoDialogResult | null) => {
      if (!result) return;

      this.assistitiService.valutaAssistito(assistito.id!, result.statoId, result.nota, assistito.codiceFiscale ?? '', this.statoIdToLabel(result.statoId)).subscribe({
        next: () => {
          this.aggiornaStatoOttimistico(assistito.id!, result.statoId);
        },
        error: (error) => {
          console.error('Errore nella valutazione:', error);
        }
      });
    });
  }

  /**
   * CDU-023 §2.5 - Cambia valutazione: NON ELEGGIBILE → ELEGGIBILE
   */
  cambiaValutazioneSingolo(assistito: Assistito): void {
    const dialogRef = this.dialog.open(ValutaAssistitoDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      disableClose: false,
      data: { assistito, mode: 'cambia-valutazione' }
    });

    dialogRef.afterClosed().subscribe((result: ValutaAssistitoDialogResult | null) => {
      if (!result) return;

      this.assistitiService.valutaAssistito(assistito.id!, result.statoId, result.nota, assistito.codiceFiscale ?? '', this.statoIdToLabel(result.statoId)).subscribe({
        next: () => {
          this.aggiornaStatoOttimistico(assistito.id!, result.statoId);
        },
        error: (error) => {
          console.error('Errore nel cambio valutazione:', error);
        }
      });
    });
  }

  /**
   * CDU-024 §2.2/2.3 - Prendi in carico: ELEGGIBILE → PRESO IN CARICO o NON PRESO IN CARICO
   */
  prendiInCaricoSingolo(assistito: Assistito): void {
    const dialogRef = this.dialog.open(ValutaAssistitoDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      disableClose: false,
      data: { assistito, mode: 'prendi-in-carico' }
    });

    dialogRef.afterClosed().subscribe((result: ValutaAssistitoDialogResult | null) => {
      if (!result) return;

      this.assistitiService.valutaAssistito(assistito.id!, result.statoId, result.nota, assistito.codiceFiscale ?? '', this.statoIdToLabel(result.statoId)).subscribe({
        next: () => {
          this.aggiornaStatoOttimistico(assistito.id!, result.statoId);
        },
        error: (error) => {
          console.error('Errore nella presa in carico:', error);
        }
      });
    });
  }

  /**
   * CDU-024 §2.5/2.6 - Cambia presa in carico: toggle PRESO ↔ NON PRESO IN CARICO
   */
  cambiaPresaInCaricoSingolo(assistito: Assistito): void {
    const dialogRef = this.dialog.open(ValutaAssistitoDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      disableClose: false,
      data: { assistito, mode: 'cambia-presa-in-carico' }
    });

    dialogRef.afterClosed().subscribe((result: ValutaAssistitoDialogResult | null) => {
      if (!result) return;

      this.assistitiService.valutaAssistito(assistito.id!, result.statoId, result.nota, assistito.codiceFiscale ?? '', this.statoIdToLabel(result.statoId)).subscribe({
        next: () => {
          this.aggiornaStatoOttimistico(assistito.id!, result.statoId);
        },
        error: (error) => {
          console.error('Errore nel cambio presa in carico:', error);
        }
      });
    });
  }

  concludiSorveglianzaSingolo(assistito: Assistito): void {
    const dialogRef = this.dialog.open(ValutaAssistitoDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      disableClose: false,
      data: { assistito, mode: 'concludi-sorveglianza' }
    });

    dialogRef.afterClosed().subscribe((result: ValutaAssistitoDialogResult | null) => {
      if (!result) return;

      this.assistitiService.valutaAssistito(assistito.id!, result.statoId, result.nota, assistito.codiceFiscale ?? '', this.statoIdToLabel(result.statoId)).subscribe({
        next: () => {
          this.aggiornaStatoOttimistico(assistito.id!, result.statoId);
        },
        error: (error) => {
          console.error('Errore nel concludere la sorveglianza:', error);
        }
      });
    });
  }

  getSezioneLabel(sezione: string | null | undefined): string {
    const map: Record<string, string> = { '1': 'Prima', '2': 'Seconda', '3': 'Terza' };
    return sezione ? (map[sezione] ?? sezione) : '-';
  }

  getSorveglianzaLabel(valore: boolean | null | undefined): string {
    if (valore === true) return 'SI';
    if (valore === false) return 'NO';
    return '-';
  }

  /** Restituisce la descrizione dell'ultima fonte (più recente) dell'assistito */
  getLastFonte(assistito: Assistito): string {
    return assistito.fonteProvenienza ?? '-';
  }

  /** True se l'assistito ha più di una fonte nel suo storico */
  hasMultipleFonti(assistito: Assistito): boolean {
    return (assistito.storicoFonti?.length ?? 0) > 1;
  }

  // REEA-CDU-003-V01-HomePage - VARIAZIONI - 2: mostrare solo "A" o "B" nei risultati
  getElencoInailLabel(valore: string | null | undefined): string {
    if (!valore) return '-';
    return valore; // 'A' o 'B' già nel formato corretto
  }

  chiudiExportError(): void { this.exportError.set(''); }
  chiudiExportSuccess(): void { this.exportSuccess.set(''); }

  esportaChiaro(): void {
    if (this.exportChiaroLoading()) return;
    this.exportChiaroLoading.set(true);
    this.exportTotaliError.set('');
    this.runExport(false);
  }

  esportaPseudo(): void {
    if (this.exportPseudoLoading()) return;
    this.exportPseudoLoading.set(true);
    this.exportTotaliError.set('');
    this.runExport(true);
  }

  private runExport(pseudo: boolean): void {
    const now = new Date();
    const mm  = String(now.getMonth()+1).padStart(2,'0');
    const dd  = String(now.getDate()).padStart(2,'0');
    const hh  = String(now.getHours()).padStart(2,'0');
    const min = String(now.getMinutes()).padStart(2,'0');
    const tipo: 'chiaro' | 'pseudo' = pseudo ? 'pseudo' : 'chiaro';
    const asl = this.exportAslFiltro() || '';
    const profiloUtente = this.authService.currentUser()?.ruolo ?? '';
    const nomeFile = asl
      ? `export_totale_${tipo}_${asl}_${now.getFullYear()}${mm}${dd}${hh}${min}.xlsx`
      : `export_totale_${tipo}_${now.getFullYear()}${mm}${dd}${hh}${min}.xlsx`;

    this.assistitiService.scaricaDatiExcelTotali(pseudo, asl, profiloUtente).subscribe({
      next: (response: HttpResponse<{fileId: number; elaborazioneId: number}>) => {
        pseudo ? this.exportPseudoLoading.set(false) : this.exportChiaroLoading.set(false);

        const fileId = response.body?.fileId ?? null;
        const blobUrl = null;

        this.exportArchivio.update(rows => [{
          idElaborazione: rows.length + 1,
          nome: nomeFile,
          fileId,
          blobUrl,
          tipo,
          stato: 'ELABORAZIONE IN CORSO',
          dataRichiesta: this.fmtDate(now),
          dataGenerazione: '',
          operatore: this.authService.currentUser()?.nome ?? '-'
        }, ...rows]);
        this.loadExportArchivio(false);
        this.startExportPolling();
        if (this.msgExportMassivoAttesa) this.showCenterNotif(this.msgExportMassivoAttesa, 'info', 10000);
      },
      error: () => {
        pseudo ? this.exportPseudoLoading.set(false) : this.exportChiaroLoading.set(false);
      }
    });
  }

  downloadExportTotali(fileId: number | null, blobUrl: string | null, nome: string, idElaborazione: number | null): void {
    if (!fileId && !blobUrl && !nome) return;
    const rowId = idElaborazione ?? fileId ?? 0;
    if (blobUrl) {
      const link = document.createElement('a');
      link.style.display = 'none';
      link.href = blobUrl;
      link.download = nome;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } else if (fileId) {
      this.startRowDownload(rowId);
      this.fileApi.downloadFile(fileId, nome).subscribe({
        complete: () => this.endRowDownload(rowId),
        error: () => this.endRowDownload(rowId)
      });
    }
  }

  private fmtDate(d: Date): string {
    const dd = d.getDate().toString().padStart(2, '0');
    const mm = (d.getMonth() + 1).toString().padStart(2, '0');
    const hh = d.getHours().toString().padStart(2, '0');
    const min = d.getMinutes().toString().padStart(2, '0');
    return `${dd}/${mm}/${d.getFullYear()} ${hh}:${min}`;
  }

  private parseAndFmtDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '';
    if (/^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/.test(dateStr)) return dateStr;
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? dateStr : this.fmtDate(d);
  }

  private getParametroValore(cod: string): string {
    return this.parametri().find(p => p.parametro_cod === cod)?.parametro_valore ?? '';
  }

  esportaExcel(): void {
    if (this.showEsportaExcel) {
      const dialogRef = this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(
        ConfirmDialogComponent,
        {
          data: {
            message: 'Saranno esportati solo gli assistiti che hanno una data di adesione valorizzata, ovvero che hanno fatto la preadesione o hanno fatto una segnalazione al Medico di Medicina Generale. Vuoi procedere con l\'export?'
          },
          width: '480px',
          disableClose: true
        }
      );
      dialogRef.afterClosed().subscribe(confermato => {
        if (!confermato) return;
        this._esportaExcelStep();
      });
      return;
    }

    const totale = this.totalElements();

    if (totale === 0) {
      this.snackBar.open('Nessun assistito trovato.', 'Chiudi', { duration: 5000, panelClass: ['snack-warning'] });
      return;
    }

    const messaggio = this.getParametroValore('EXPORT_SINCRONO');

    const dialogRef = this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(
      ConfirmDialogComponent,
      {
        data: { message: messaggio || 'Vuoi procedere con l\'esportazione?' },
        width: '480px',
        disableClose: true
      }
    );

    dialogRef.afterClosed().subscribe(confermato => {
      if (!confermato) return;

      this.exportMessage.set('Recupero risultati in corso...');
      this.exportLoading.set(true);

      const filtri = this.filterForm.getRawValue();
      this.assistitiService.getAssistiti(filtri, { page: 0, size: totale }).subscribe({
        next: (response) => {
          const ids = response.content
            .filter(a => a.id != null)
            .map(a => Number(a.id));

          this.exportMessage.set('Esportazione in corso...');
          this.adesioniApi.scaricaDatiExcelPreadesioniFiltrate(ids, this.exportAslFiltro()).subscribe({
            next: (blob: Blob) => {
              this.exportLoading.set(false);
              this.exportMessage.set('');
              this.exportSuccess.set('Esportazione completata. Il file è stato scaricato.');
              const url = window.URL.createObjectURL(blob);
              const link = document.createElement('a');
              link.href = url;
              link.download = `preadesioni_filtrate_${new Date().getTime()}.xlsx`;
              link.click();
              window.URL.revokeObjectURL(url);
            },
            error: (error) => {
              this.exportLoading.set(false);
              this.exportMessage.set('');
              console.error('Errore nell\'export:', error);
              this.exportError.set('Si è verificato un errore durante l\'esportazione. Riprovare.');
            }
          });
        },
        error: () => {
          this.exportLoading.set(false);
          this.exportMessage.set('');
          this.exportError.set('Errore nel recupero dei dati da esportare. Riprovare.');
        }
      });
    });
  }

  private _esportaExcelStep(): void {
    const ids = this._allAssistiti
      .filter(a => a.id != null)
      .map(a => Number(a.id));

    if (ids.length === 0) {
      this.snackBar.open('Nessun assistito da esportare.', 'Chiudi', { duration: 5000, panelClass: ['snack-warning'] });
      return;
    }

    this.exportMessage.set('Esportazione in corso...');
    this.exportLoading.set(true);

    this.adesioniApi.scaricaDatiExcelPreadesioniFiltrate(ids, this.exportAslFiltro()).subscribe({
      next: (blob: Blob) => {
        this.exportLoading.set(false);
        this.exportMessage.set('');
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `preadesioni_step_${Date.now()}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(url);
        this._lastExportedCount = ids.length;
        this.esportatoStep.set(true);
      },
      error: () => {
        this.exportLoading.set(false);
        this.exportMessage.set('');
        this.exportError.set('Errore nell\'esportazione. Riprovare.');
      }
    });
  }

  caricaProssimoStep(): void {
    this._lastExportedCount = 0;
    this.esportatoStep.set(false);
    this.currentPage.set(0);
    this.loadAssistiti(false);
  }

  apriArchivioFile(): void {
    this.dialog.open(ArchivioFileDialogComponent, {
      width: '95vw',
      maxWidth: '95vw',
      maxHeight: '92vh',
      panelClass: 'archivio-file-dialog'
    });
  }

  apriStoriaStati(assistito: Assistito): void {
    this.auditApi.salva('read', 'ANAGRAFICA', `Ricerca assistiti - selezione assistito - visualizza storia stati - ${assistito.codiceFiscale}`).subscribe();
    this.dialog.open(StoriaStatiDialogComponent, {
      width: '800px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'storia-stati-dialog',
      data: { assistito }
    });
  }

  apriFontiAggiornamento(assistito: Assistito): void {
    this.auditApi.salva('read', 'ANAGRAFICA', `Ricerca assistiti - selezione assistito - visualizza fonti associate - ${assistito.codiceFiscale}`).subscribe();
    this.dialog.open(FontiAggiornamentoDialogComponent, {
      width: '480px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'fonti-aggiornamento-dialog',
      data: { assistito }
    });
  }

  apriNote(assistito: Assistito): void {
    this.auditApi.salva('read', 'ANAGRAFICA', `Ricerca assistiti - selezione assistito - visualizza note - ${assistito.codiceFiscale}`).subscribe();
    this.dialog.open(NoteRegistroDialogComponent, {
      width: '850px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'note-registro-dialog',
      data: { assistito }
    });
  }

  importaDaFile(): void {
    const dialogRef = this.dialog.open(ImportaFileDialogComponent, {
      width: '580px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: true,
      panelClass: 'importa-file-dialog'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.success) {
        this.assistitiState.clearMasterList();
        if (result.backgroundProcessing) {
          this.showCenterNotif(this.msgAvvioElaborazione, 'info', 0, true);
          this.startPollingForCompletion();
        } else {
          this.loadAssistiti();
        }
      } else if (result && result.success === false) {
        // result === null/undefined → utente ha premuto Annulla, nessun messaggio.
        // result.success === false → la richiesta di import è fallita lato frontend (es. timeout
        // del gateway su file grandi), ma il file può comunque essere già stato salvato lato
        // backend e in elaborazione: avviamo lo stesso polling del percorso di successo, così
        // l'esito reale (terminata/fallita) arriva come notifica automatica senza dover aprire
        // manualmente l'archivio file.
        const msg = result.errori?.length
          ? result.errori.join(' ')
          : 'Importazione non riuscita. Verifica dell\'esito in corso: riceverai una notifica appena disponibile.';
        this.showCenterNotif(msg, 'error', 0, true);
        this.startPollingForCompletion();
      }
    });
  }

  aggiungiAssistito(): void {
    this.router.navigate(['/registro-amianto/nuovo-assistito']);
  }

  private _saveState(): void {
    this.assistitiState.save({
      allAssistiti: this._allAssistiti,
      filters: this.filterForm.getRawValue(),
      page: this.currentPage(),
      pageSize: this.pageSize(),
      stepOffset: this.stepOffset(),
      totalRecordsDB: this.totalRecordsDB(),
      stepEsaurito: this.stepEsaurito(),
      esportatoStep: this.esportatoStep(),
      sortColumn: this.sortColumn(),
      sortDirection: this.sortDirection()
    });
  }

  visualizzaDettaglio(assistito: Assistito): void {
    this._saveState();
    this.router.navigate(['/registro-amianto/assistiti', assistito.id]);
  }

  modificaAssistito(assistito: Assistito): void {
    this._saveState();
    this.router.navigate(['/registro-amianto/assistiti', assistito.id], { queryParams: { edit: true } });
  }

  eliminaAssistito(assistito: Assistito): void {
    if (confirm(`Confermi di voler eliminare l'assistito ${assistito.cognome} ${assistito.nome}?`)) {
      this.assistitiService.deleteAssistito(assistito.id!).subscribe({
        next: () => {
          alert('Assistito eliminato con successo');
          this.loadAssistiti();
        },
        error: (error) => {
          console.error('Errore nell\'eliminazione:', error);
          alert('Errore durante l\'eliminazione dell\'assistito');
        }
      });
    }
  }

  tornaAllaSelezione(): void {
    this.router.navigate(['/registro-amianto']);
  }

  // ============================================
  // IMPORT PDF FILESYSTEM (solo CSI)
  // ============================================

  uploadPdfFilesystem(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    input.value = '';
    this.pdfFsUploading.set(true);
    this.fileApi.uploadPdf(file).subscribe({
      next: (resp) => {
        if (resp.successo) {
          this.pdfFsFiles.update(list => [
            { nomeFile: resp.nomeFile, percorso: resp.percorso },
            ...list
          ]);
          this.snackBar.open(`File "${resp.nomeFile}" caricato con successo`, '', { duration: 3000, panelClass: ['snackbar-success'] });
        }
        this.pdfFsUploading.set(false);
      },
      error: () => {
        this.snackBar.open('Errore durante il caricamento del file', 'Chiudi', { duration: 4000, panelClass: ['snackbar-error'] });
        this.pdfFsUploading.set(false);
      }
    });
  }

  downloadPdfFilesystem(percorso: string, nomeFile: string): void {
    this.fileApi.downloadPdf(percorso, nomeFile).subscribe();
  }

  eliminaPdfFilesystem(percorso: string): void {
    this.fileApi.eliminaPdf(percorso).subscribe({
      next: (resp) => {
        if (resp.successo) {
          this.pdfFsFiles.update(list => list.filter(f => f.percorso !== percorso));
          this.snackBar.open('File eliminato', '', { duration: 2000 });
        }
      },
      error: () => this.snackBar.open('Errore durante l\'eliminazione', 'Chiudi', { duration: 4000, panelClass: ['snackbar-error'] })
    });
  }
}
