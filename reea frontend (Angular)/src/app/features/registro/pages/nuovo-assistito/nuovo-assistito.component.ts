// ============================================
// NUOVO ASSISTITO COMPONENT - Wizard Multi-Step
// Pagina per l'inserimento di un nuovo assistito
// nel Registro Ex Esposti Amianto
// ============================================

import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';

// Angular Material imports
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

// Components
import { AppHeaderComponent } from '@shared/components/app-header/app-header.component';
// Directives
import { DateMaskDirective } from '@shared/directives/date-mask.directive';

// Services
import { AssistitiService, SearchService, AuraService } from '@core/services';
import { StatoAssistito, FonteProvenienza, AuraAssistitoDto, AuraEsenzione } from '@core/models';
import {
  AnagraficaApiService,
  AnagraficaSearchParams,
  NazioneApiService,
  NazioneDTO,
  ProvinciaApiService,
  ProvinciaDTO,
  ComuneApiService,
  ComuneDTO,
  AslApiService,
  AslDTO
} from '../../../../api';

/**
 * Interfaccia per i luoghi di esposizione
 */
interface LuogoEsposizione {
  azienda: string;
  provinciaAziendaId: number | null;
  provinciaAziendaSigla: string;
  comuneAziendaCod: string;
  comuneAziendaDesc: string;
  capAzienda: string;
  mansione: string;
  annoInizio: number | null;
  annoFine: number | null;
}

@Component({
  selector: 'app-nuovo-assistito',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    AppHeaderComponent,
    DateMaskDirective
  ],
  templateUrl: './nuovo-assistito.component.html',
  styleUrl: './nuovo-assistito.component.scss'
})
export class NuovoAssistitoComponent {

  // ============================================
  // DEPENDENCY INJECTION
  // ============================================

  private router = inject(Router);
  private fb = inject(FormBuilder);
  private assistitiService = inject(AssistitiService);
  private anagraficaApi = inject(AnagraficaApiService);
  private nazioneApi = inject(NazioneApiService);
  private provinciaApi = inject(ProvinciaApiService);
  private comuneApi = inject(ComuneApiService);
  private aslApi = inject(AslApiService);
  private searchService = inject(SearchService);
  private auraService = inject(AuraService);
  private snackBar = inject(MatSnackBar);

  // ============================================
  // STEPPER STATE
  // ============================================

  currentStep = signal<number>(0);
  isLinear = true;

  // ============================================
  // SEARCH RESULT STATE (tra Step 1 e Step 2)
  // ============================================

  searchResultType = signal<'aura' | 'aura_multiple' | 'reea' | 'not_found' | 'aura_error' | null>(null);
  showSearchResult = signal<boolean>(false);
  /** Modalità di ricerca selezionata nel Step 1: 'cf' | 'anagrafica' | null (selezione iniziale) */
  searchMode = signal<'cf' | 'anagrafica' | null>(null);

  selectSearchMode(mode: 'cf' | 'anagrafica' | null): void {
    this.searchMode.set(mode);
    this.searchForm.reset();
  }

  existingAssistitoId = signal<string | null>(null);
  /** Risultati multipli AURA dalla ricerca per anagrafica */
  auraMultiResults = signal<AuraAssistitoDto[]>([]);
  auraData = signal<AuraAssistitoDto | null>(null);
  isSearching = signal<boolean>(false);
  /** Indica se i dati vengono da AURA (campi anagrafica non editabili) */
  isFromAura = computed(() => this.searchResultType() === 'aura');
  /** Stato calcolato da AURA (o ELEGGIBILE per inserimento manuale) */
  auraStato = signal<StatoAssistito>(StatoAssistito.ELEGGIBILE);
  /** True se AURA riporta un decesso */
  auraHasDecesso = computed(() => !!this.auraData()?.data_decesso);
  /** True se AURA riporta una fine ASL reale (esclude sentinel 9999-12-31) e già trascorsa:
   *  una data futura non giustifica ancora l'esclusione per emigrazione. */
  auraHasFineAsl = computed(() => this.isFineAslGiaTrascorsa(this.auraData()?.data_fine_asl));

  /** True se la data_fine_asl AURA è valorizzata (non sentinel 9999-12-31) ed è <= oggi. */
  private isFineAslGiaTrascorsa(dataFineAsl: string | null | undefined): boolean {
    if (!dataFineAsl || dataFineAsl === '9999-12-31') return false;
    const fineAsl = new Date(dataFineAsl);
    if (isNaN(fineAsl.getTime())) return false;
    const oggi = new Date();
    oggi.setHours(0, 0, 0, 0);
    fineAsl.setHours(0, 0, 0, 0);
    return fineAsl <= oggi;
  }
  /** Esenzioni restituite da AURA */
  auraEsenzioni = computed<AuraEsenzione[]>(() => this.auraData()?.esenzioni ?? []);
  /** Telefoni restituiti da AURA (read-only nel riepilogo) */
  auraTelefoni = computed<string[]>(() => this.auraData()?.telefoni?.filter(Boolean) as string[] ?? []);
  /** Email restituiti da AURA (read-only nel riepilogo) */
  auraEmails = computed<string[]>(() => this.auraData()?.email?.filter(Boolean) as string[] ?? []);
  /** ASL di assistenza ricevuta da AURA (descrizione testuale, read-only) */
  aslAuraDesc = signal<string>('');
  /** asl_id risolto dal codice AURA via match con aslOptions (richiede asl_cod dal BE) */
  aslAuraId = signal<number | null>(null);
  /** ASL selezionata manualmente nel riepilogo (solo per inserimento manuale) */
  aslManualeId = signal<number | null>(null);
  /** Mostra errore se l'utente tenta di salvare senza selezionare l'ASL (solo manuale) */
  aslManualeError = signal<boolean>(false);
  /** Testo del disclaimer trattamento dati (da reea_c_parametro parametro_id=10) */
  testoDatiPersonali = signal<string>('Autorizzo al trattamento dati');
  /** Stato della checkbox trattamento dati */
  trattamentoDatiAccettato = signal<boolean>(false);

  // ============================================
  // STEP 1: DATI IDENTIFICATIVI
  // ============================================

  searchForm: FormGroup = this.fb.group({
    codiceFiscale: ['', [Validators.pattern(/^[A-Z]{6}\d{2}[A-Z]\d{2}[A-Z]\d{3}[A-Z]$/i)]],
    cognome: [''],
    nome: [''],
    dataNascita: ['']
  });

  // ============================================
  // STEP 2: ANAGRAFICA
  // ============================================

  anagraficaForm: FormGroup = this.fb.group({
    // Dati identificativi (obbligatori)
    codiceFiscale: ['', [Validators.required, Validators.pattern(/^[A-Z]{6}\d{2}[A-Z]\d{2}[A-Z]\d{3}[A-Z]$/i)]],
    cognome: ['', Validators.required],
    nome: ['', Validators.required],
    dataNascita: ['', [Validators.required, this.minAge18Validator]],

    // Dati nascita
    sesso: ['', Validators.required],
    statoNascita: ['', Validators.required],
    provinciaNascita: [null],
    comuneNascita: ['', Validators.required],

    // Residenza (obbligatoria)
    statoResidenza: ['', Validators.required],
    provinciaResidenza: [null, Validators.required],
    comuneResidenza: ['', Validators.required],
    indirizzoResidenza: ['', Validators.required],
    civicoResidenza: [''],
    capResidenza: ['', [Validators.required, Validators.pattern(/^\d+$/)]],

    // Domicilio
    domicilioCoincide: [true],
    statoDomicilio: [''],
    provinciaDomicilio: [null],
    comuneDomicilio: [''],
    indirizzoDomicilio: [''],
    civicoDomicilio: [''],
    capDomicilio: [''],

    // Contatti (facoltativi: l'utente può inserire i propri, quelli AURA sono nel riepilogo)
    telefono: ['', Validators.required],
    email: ['', Validators.email]
  });

  // ============================================
  // STEP 3: LUOGHI ESPOSIZIONE
  // ============================================

  luoghiEsposizione = signal<LuogoEsposizione[]>([
    { azienda: '', provinciaAziendaId: null, provinciaAziendaSigla: '', comuneAziendaCod: '', comuneAziendaDesc: '', capAzienda: '', mansione: '', annoInizio: null, annoFine: null }
  ]);

  // ============================================
  // STEP 4: RIEPILOGO
  // ============================================

  // Data massima per datePicker (= oggi, non si accettano date future)
  readonly today = new Date();

  // riepilogoForm: contiene i campi inseriti manualmente nel riepilogo
  riepilogoForm: FormGroup = this.fb.group({
    dataPresentazioneIstanza: ['', Validators.required]
  });

  // ============================================
  // OPTIONS - Caricate dal backend
  // ============================================

  sessoOptions = [
    { value: 'M', label: 'Maschio' },
    { value: 'F', label: 'Femmina' }
  ];

  // Anni per dropdown esposizione (dal 1900 all'anno corrente, ordinati decrescenti)
  anniOptions: number[] = (() => {
    const currentYear = new Date().getFullYear();
    return Array.from({ length: currentYear - 1900 + 1 }, (_, i) => currentYear - i);
  })();

  readonly ITALIA_COD = '100';

  get isResidenzaEstera(): boolean {
    const v = this.anagraficaForm.get('statoResidenza')?.value;
    return !!v && v !== this.ITALIA_COD;
  }

  get isNascitaEstera(): boolean {
    const v = this.anagraficaForm.get('statoNascita')?.value;
    return !!v && v !== this.ITALIA_COD;
  }

  get isDomicilioEstero(): boolean {
    const v = this.anagraficaForm.get('statoDomicilio')?.value;
    return !!v && v !== this.ITALIA_COD;
  }

  onStatoResidenzaChange(_cod: string): void {
    this.anagraficaForm.patchValue({ provinciaResidenza: null, comuneResidenza: '', capResidenza: '' });
    this.comuniResidenzaOptions.set([]);
    const estero = this.isResidenzaEstera;
    for (const name of ['provinciaResidenza', 'comuneResidenza', 'capResidenza']) {
      const ctrl = this.anagraficaForm.get(name);
      if (estero) { ctrl?.clearValidators(); } else { ctrl?.setValidators(Validators.required); }
      ctrl?.updateValueAndValidity();
    }
  }

  nazioniOptions = signal<NazioneDTO[]>([]);
  provinceOptions = signal<ProvinciaDTO[]>([]);
  comuniNascitaOptions = signal<ComuneDTO[]>([]);
  comuniResidenzaOptions = signal<ComuneDTO[]>([]);
  comuniDomicilioOptions = signal<ComuneDTO[]>([]);
  comuniEsposizioneOptions = signal<ComuneDTO[][]>([[]]); // un array per ciascun luogo

  aslOptions = signal<AslDTO[]>([]);

  // ============================================
  // STATE
  // ============================================

  isSaving = signal<boolean>(false);
  cfInAuraChecking = signal<boolean>(false);

  // ============================================
  // INITIALIZATION
  // ============================================

  constructor() {
    this.loadNazioni();
    this.loadProvince();
    this.loadASLOptions();
    this.searchService.getTestoDatiPersonali().subscribe(testo => this.testoDatiPersonali.set(testo));
  }

  private loadNazioni(): void {
    this.nazioneApi.getLista().subscribe(nazioni => this.nazioniOptions.set(nazioni));
  }

  private loadProvince(): void {
    this.provinciaApi.getLista().subscribe(province => this.provinceOptions.set(province));
  }

  private loadASLOptions(): void {
    this.aslApi.getListaCompetenza().subscribe(asl => this.aslOptions.set(asl));
  }

  // ============================================
  // NAVIGATION
  // ============================================

  tornaAllElenco(): void {
    this.router.navigate(['/registro-amianto/assistiti']);
  }

  // ============================================
  // STEP 1: DATI IDENTIFICATIVI
  // ============================================

  canProceedStep1(): boolean {
    const form = this.searchForm.value;
    const mode = this.searchMode();
    const cfPattern = /^[A-Z]{6}\d{2}[A-Z]\d{2}[A-Z]\d{3}[A-Z]$/i;

    if (mode === 'cf') {
      const cf = form.codiceFiscale?.trim();
      return !!(cf && cfPattern.test(cf));
    }

    if (mode === 'anagrafica') {
      const cognome = form.cognome?.trim();
      const nome = form.nome?.trim();
      const dataNascita = form.dataNascita;
      return !!(cognome && nome && dataNascita);
    }

    return false;
  }

  getStep1ErrorMessage(): string {
    const mode = this.searchMode();
    const form = this.searchForm.value;

    if (mode === 'cf') {
      const cf = form.codiceFiscale?.trim();
      if (cf && cf.length > 0) {
        return 'Il Codice Fiscale deve essere di 16 caratteri';
      }
      return 'Inserire un Codice Fiscale valido';
    }

    if (mode === 'anagrafica') {
      const cognome = form.cognome?.trim();
      const nome = form.nome?.trim();
      const dataNascita = form.dataNascita;
      if (!cognome) return 'Inserire il Cognome';
      if (!nome) return 'Inserire il Nome';
      if (!dataNascita) return 'Inserire la Data di Nascita';
      return 'Inserire Nome, Cognome e Data di Nascita';
    }

    return 'Selezionare una modalità di ricerca';
  }

  goToStep2(): void {
    if (!this.canProceedStep1()) {
      this.markFormGroupTouched(this.searchForm);
      this.showError(this.getStep1ErrorMessage());
      return;
    }

    this.isSearching.set(true);
    this.showSearchResult.set(false);

    const searchData = this.searchForm.value;

    this.searchInREEA(searchData).then(reeaResult => {
      if (reeaResult.found) {
        this.existingAssistitoId.set(reeaResult.id!);
        this.searchResultType.set('reea');
        this.showSearchResult.set(true);
        this.isSearching.set(false);
      } else {
        this.searchInAURA(searchData).then(auraResult => {
          if (auraResult.found && auraResult.multiple) {
            this.auraMultiResults.set(auraResult.results!);
            this.searchResultType.set('aura_multiple');
            this.showSearchResult.set(true);
          } else if (auraResult.found) {
            this.auraData.set(auraResult.data!);
            this.searchResultType.set('aura');
            this.showSearchResult.set(true);
          } else if (auraResult.error) {
            this.searchResultType.set('aura_error');
            this.showSearchResult.set(true);
          } else {
            this.searchResultType.set('not_found');
            this.showSearchResult.set(true);
          }
          this.isSearching.set(false);
        });
      }
    });
  }

  private async searchInREEA(searchData: any): Promise<{ found: boolean; id?: string }> {
    return new Promise((resolve) => {
      const params: AnagraficaSearchParams = {};

      if (searchData.codiceFiscale) {
        params.codice_fiscale = searchData.codiceFiscale.toUpperCase();
      } else {
        if (searchData.cognome) params.cognome = searchData.cognome;
        if (searchData.nome) params.nome = searchData.nome;
        if (searchData.dataNascita) params.nascita_data = this.formatDateForApi(searchData.dataNascita);
      }

      this.anagraficaApi.search(params).subscribe({
        next: (results) => {
          if (results && results.length > 0) {
            resolve({ found: true, id: results[0].soggetto_id?.toString() });
          } else {
            resolve({ found: false });
          }
        },
        error: () => resolve({ found: false })
      });
    });
  }

  private formatDateForApi(date: any): string {
    if (!date) return '';
    if (date instanceof Date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
    if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}/.test(date)) {
      return date.substring(0, 10);
    }
    return '';
  }

  /** true se l'errore è un genuino "non trovato" (404); false se è un problema tecnico (servizio AURA irraggiungibile, credenziali errate, ecc.) */
  private isNotFoundError(err: HttpErrorResponse): boolean {
    return err?.status === 404;
  }

  private searchInAURA(searchData: any): Promise<{ found: boolean; data?: AuraAssistitoDto; multiple?: boolean; results?: AuraAssistitoDto[]; error?: boolean }> {
    return new Promise((resolve) => {
      const cf = searchData.codiceFiscale?.toUpperCase();

      if (cf) {
        // Modalità CF: ricerca univoca
        this.auraService.findByCf(cf).subscribe({
          next: (data) => resolve({ found: true, data }),
          error: (err: HttpErrorResponse) => resolve({ found: false, error: !this.isNotFoundError(err) })
        });
      } else {
        // Modalità anagrafica: possibili risultati multipli
        const cognome = searchData.cognome?.trim();
        const nome = searchData.nome?.trim();
        const dataNascita = this.formatDateForApi(searchData.dataNascita);
        if (!cognome || !nome || !dataNascita) {
          resolve({ found: false });
          return;
        }
        this.auraService.findByAnagrafica(cognome, nome, dataNascita).subscribe({
          next: (results) => {
            if (!results || results.length === 0) {
              resolve({ found: false });
            } else if (results.length === 1) {
              // Un solo risultato: carica i dettagli completi via id_aura
              const r = results[0];
              if (r.id_aura) {
                this.auraService.getById(r.id_aura).subscribe({
                  next: (full) => resolve({ found: true, data: full }),
                  error: () => resolve({ found: true, data: r })
                });
              } else {
                resolve({ found: true, data: r });
              }
            } else {
              // Più risultati: l'utente deve scegliere
              resolve({ found: true, multiple: true, results });
            }
          },
          error: (err: HttpErrorResponse) => resolve({ found: false, error: !this.isNotFoundError(err) })
        });
      }
    });
  }

  proceedToAnagrafica(): void {
    const searchData = this.searchForm.value;

    if (this.searchResultType() === 'aura' && this.auraData()) {
      const aura = this.auraData()!;

      // Determina lo stato in base ai dati AURA
      // data_fine_asl = "9999-12-31" è il sentinel AURA per "nessuna fine" → ignorato;
      // una data futura non giustifica ancora l'esclusione per emigrazione.
      const dataFineAslReale = this.isFineAslGiaTrascorsa(aura.data_fine_asl)
        ? aura.data_fine_asl : null;
      if (aura.data_decesso) {
        this.auraStato.set(StatoAssistito.ESCLUSO_DECESSO);
      } else if (dataFineAslReale) {
        this.auraStato.set(StatoAssistito.ESCLUSO_EMIGRAZIONE);
      } else {
        this.auraStato.set(StatoAssistito.ELEGGIBILE);
      }

      // ASL di assistenza: matching per cod (se asl_cod corrisponde al formato AURA),
      // altrimenti fallback su descrizione. desc=null nel mapper BE → fix BE necessario.
      this.aslAuraDesc.set(aura.asl_assistenza_desc || 'Non definita');
      const aslMatch =
        (aura.asl_assistenza_cod
          ? this.aslOptions().find(a => a.asl_cod === aura.asl_assistenza_cod)
          : null)
        ?? (aura.asl_assistenza_desc
          ? this.aslOptions().find(a => a.asl_azienda_desc === aura.asl_assistenza_desc)
          : null);
      this.aslAuraId.set(aslMatch?.asl_id ?? null);

      // Trova provincia_id (numero) dalla sigla AURA (es. "TO")
      const provNascita = this.provinceOptions().find(
        p => p.provincia_sigla === aura.provincia_nascita_cod
      ) ?? (aura.provincia_nascita_desc
        ? this.provinceOptions().find(p => p.provincia_desc?.toLowerCase() === aura.provincia_nascita_desc!.toLowerCase())
        : undefined);
      const provNascitaId = provNascita?.provincia_id ?? null;
      console.log('[AURA DEBUG nascita] sigla:', aura.provincia_nascita_cod,
        '| desc:', aura.provincia_nascita_desc,
        '| comune_cod:', aura.comune_nascita_cod,
        '| comune_desc:', aura.comune_nascita_desc,
        '| provNascitaId:', provNascitaId);

      // provincia_residenza_cod from AURA is always null (not in WSDL DatiSecondari).
      // Derive province by: 1) sigla, 2) ISTAT prefix on provincia_cod, 3) provincia_desc fallback.
      let provResidenza = aura.provincia_residenza_cod
        ? this.provinceOptions().find(p => p.provincia_sigla === aura.provincia_residenza_cod)
        : aura.comune_residenza_cod
          ? this.provinceOptions().find(p => p.provincia_cod === aura.comune_residenza_cod!.substring(0, 3))
          : undefined;
      if (!provResidenza && aura.provincia_residenza_desc) {
        provResidenza = this.provinceOptions().find(
          p => p.provincia_desc?.toLowerCase() === aura.provincia_residenza_desc!.toLowerCase()
        );
      }
      const provResidenzaId = provResidenza?.provincia_id ?? null;
      console.log('[AURA DEBUG residenza] sigla:', aura.provincia_residenza_cod,
        '| desc:', aura.provincia_residenza_desc,
        '| comune_cod:', aura.comune_residenza_cod,
        '| comune_desc:', aura.comune_residenza_desc,
        '| prefix3:', aura.comune_residenza_cod?.substring(0, 3),
        '| province sample (cod):', this.provinceOptions().slice(0, 3).map(p => p.provincia_cod),
        '| provResidenzaId:', provResidenzaId);

      // Abilita tutto, patcha i campi base, poi disabilita
      this.enableAnagrafica();
      this.anagraficaForm.patchValue({
        codiceFiscale: aura.codice_fiscale || '',
        cognome: aura.cognome || '',
        nome: aura.nome || '',
        dataNascita: aura.data_nascita ? new Date(aura.data_nascita) : null,
        sesso: aura.sesso || '',
        statoNascita: aura.stato_nascita || '',
        provinciaNascita: provNascitaId,
        comuneNascita: '',
        statoResidenza: aura.stato_residenza ?? aura.statoResidenza ?? '',
        provinciaResidenza: provResidenzaId,
        comuneResidenza: '',
        indirizzoResidenza: aura.indirizzo_residenza || '',
        civicoResidenza: aura.civico_residenza || '',
        capResidenza: aura.cap_residenza || ''
      });
      this.disableAuraFields();

      // Telefono ed email da AURA: non pre-compilano il form,
      // vengono mostrati in sola lettura nel riepilogo (step 4).
      // L'utente può inserire contatti propri nei campi input.

      // Domicilio da AURA: se diverso dalla residenza mostra i dati, altrimenti coincide
      const domicilioSameAsResidenza =
        !aura.indirizzo_domicilio ||
        (aura.indirizzo_domicilio === aura.indirizzo_residenza &&
          aura.civico_domicilio === aura.civico_residenza &&
          aura.cap_domicilio === aura.cap_residenza &&
          aura.comune_domicilio_cod === aura.comune_residenza_cod);

      if (!domicilioSameAsResidenza) {
        let provDomicilio = aura.provincia_domicilio_cod
          ? this.provinceOptions().find(p => p.provincia_sigla === aura.provincia_domicilio_cod)
          : aura.comune_domicilio_cod
            ? this.provinceOptions().find(p => p.provincia_cod === aura.comune_domicilio_cod!.substring(0, 3))
            : undefined;
        if (!provDomicilio && aura.provincia_domicilio_desc) {
          provDomicilio = this.provinceOptions().find(
            p => p.provincia_desc?.toLowerCase() === aura.provincia_domicilio_desc!.toLowerCase()
          );
        }
        const provDomicilioId = provDomicilio?.provincia_id ?? null;

        this.anagraficaForm.patchValue({
          domicilioCoincide: false,
          statoDomicilio: aura.stato_domicilio || '',
          provinciaDomicilio: provDomicilioId,
          indirizzoDomicilio: aura.indirizzo_domicilio || '',
          civicoDomicilio: aura.civico_domicilio || '',
          capDomicilio: aura.cap_domicilio || ''
        });

        if (provDomicilioId && (aura.comune_domicilio_cod || aura.comune_domicilio_desc)) {
          this.comuneApi.getLista(provDomicilioId).subscribe(comuni => {
            this.comuniDomicilioOptions.set(comuni);
            const domMatch = aura.comune_domicilio_cod
              ? (comuni.find(c => c.comune_cod === aura.comune_domicilio_cod)
                  ?? this.matchComuneByDesc(comuni, aura.comune_domicilio_desc ?? ''))
              : this.matchComuneByDesc(comuni, aura.comune_domicilio_desc ?? '');
            if (domMatch) {
              const ctrl = this.anagraficaForm.get('comuneDomicilio');
              ctrl?.enable({ emitEvent: false });
              this.anagraficaForm.patchValue({ comuneDomicilio: domMatch.comune_cod });
              ctrl?.disable({ emitEvent: false });
            }
            console.log('[AURA DEBUG domicilio comune] cod:', aura.comune_domicilio_cod,
              '| desc:', aura.comune_domicilio_desc,
              '| match:', domMatch?.comune_cod, domMatch?.comune_desc);
          });
        }

        ['domicilioCoincide', 'statoDomicilio', 'provinciaDomicilio', 'comuneDomicilio',
          'indirizzoDomicilio', 'civicoDomicilio', 'capDomicilio'].forEach(
          field => this.anagraficaForm.get(field)?.disable()
        );
      } else {
        this.anagraficaForm.patchValue({ domicilioCoincide: true });
      }

      // Carica comuni nascita: 1) match per codice, 2) fallback su descrizione (startsWith)
      if (provNascitaId && (aura.comune_nascita_cod || aura.comune_nascita_desc)) {
        this.comuneApi.getLista(provNascitaId).subscribe(comuni => {
          this.comuniNascitaOptions.set(comuni);
          const nascitaMatch = aura.comune_nascita_cod
            ? (comuni.find(c => c.comune_cod === aura.comune_nascita_cod)
                ?? this.matchComuneByDesc(comuni, aura.comune_nascita_desc ?? ''))
            : this.matchComuneByDesc(comuni, aura.comune_nascita_desc ?? '');
          if (nascitaMatch) {
            const ctrl = this.anagraficaForm.get('comuneNascita');
            ctrl?.enable({ emitEvent: false });
            this.anagraficaForm.patchValue({ comuneNascita: nascitaMatch.comune_cod });
            ctrl?.disable({ emitEvent: false });
          }
          console.log('[AURA DEBUG nascita comune] cod:', aura.comune_nascita_cod,
            '| desc:', aura.comune_nascita_desc,
            '| match:', nascitaMatch?.comune_cod, nascitaMatch?.comune_desc,
            '| list sample:', comuni.slice(0, 3).map(c => `${c.comune_cod}=${c.comune_desc}`));
        });
      } else {
        console.log('[AURA DEBUG nascita comune] skipped — provNascitaId:', provNascitaId,
          'cod:', aura.comune_nascita_cod, 'desc:', aura.comune_nascita_desc);
      }

      // Carica comuni residenza: 1) match per codice, 2) fallback su descrizione (startsWith)
      if (provResidenzaId && (aura.comune_residenza_cod || aura.comune_residenza_desc)) {
        this.comuneApi.getLista(provResidenzaId).subscribe(comuni => {
          this.comuniResidenzaOptions.set(comuni);
          const residenzaMatch = aura.comune_residenza_cod
            ? (comuni.find(c => c.comune_cod === aura.comune_residenza_cod)
                ?? this.matchComuneByDesc(comuni, aura.comune_residenza_desc ?? ''))
            : this.matchComuneByDesc(comuni, aura.comune_residenza_desc ?? '');
          if (residenzaMatch) {
            const ctrl = this.anagraficaForm.get('comuneResidenza');
            ctrl?.enable({ emitEvent: false });
            this.anagraficaForm.patchValue({ comuneResidenza: residenzaMatch.comune_cod });
            ctrl?.disable({ emitEvent: false });
          }
          console.log('[AURA DEBUG residenza comune] cod:', aura.comune_residenza_cod,
            '| desc:', aura.comune_residenza_desc,
            '| match:', residenzaMatch?.comune_cod, residenzaMatch?.comune_desc,
            '| list sample:', comuni.slice(0, 3).map(c => `${c.comune_cod}=${c.comune_desc}`));
        });
      } else {
        console.log('[AURA DEBUG residenza comune] skipped — provResidenzaId:', provResidenzaId,
          'cod:', aura.comune_residenza_cod, 'desc:', aura.comune_residenza_desc);
      }

    } else {
      // Reset COMPLETO: evita che restino dati di una precedente ricerca AURA
      this.enableAnagrafica();
      this.anagraficaForm.reset({ domicilioCoincide: true });
      this.comuniNascitaOptions.set([]);
      this.comuniResidenzaOptions.set([]);
      this.comuniDomicilioOptions.set([]);
      // Patcha solo i dati inseriti nella ricerca
      this.anagraficaForm.patchValue({
        codiceFiscale: searchData.codiceFiscale || '',
        cognome: searchData.cognome || '',
        nome: searchData.nome || '',
        dataNascita: searchData.dataNascita || ''
      });
    }

    this.showSearchResult.set(false);
    this.currentStep.set(1);
  }

  /**
   * Chiamato dalla tabella risultati multipli AURA:
   * carica i dettagli completi del soggetto selezionato e passa allo step AURA normale.
   */
  selectAuraResult(result: AuraAssistitoDto): void {
    if (result.id_aura) {
      this.auraService.getById(result.id_aura).subscribe({
        next: (full) => {
          this.auraData.set(full);
          this.auraMultiResults.set([]);
          this.searchResultType.set('aura');
        },
        error: () => {
          this.auraData.set(result);
          this.auraMultiResults.set([]);
          this.searchResultType.set('aura');
        }
      });
    } else {
      this.auraData.set(result);
      this.auraMultiResults.set([]);
      this.searchResultType.set('aura');
    }
  }

  goToExistingAssistito(): void {
    const id = this.existingAssistitoId();
    if (id) {
      this.router.navigate(['/registro-amianto/assistiti', id]);
    }
  }

  backToSearch(): void {
    this.showSearchResult.set(false);
    this.searchResultType.set(null);
    this.existingAssistitoId.set(null);
    this.auraData.set(null);
    this.auraMultiResults.set([]);
    this.aslAuraDesc.set('');
    this.aslAuraId.set(null);
    this.aslManualeId.set(null);
    this.auraStato.set(StatoAssistito.ELEGGIBILE);
    this.searchMode.set(null);
    this.enableAnagrafica();
  }

  /** Disabilita tutti i campi anagrafica provenienti da AURA (non editabili dall'utente) */
  private disableAuraFields(): void {
    const campiAura = [
      'codiceFiscale', 'cognome', 'nome', 'dataNascita',
      'sesso', 'statoNascita', 'provinciaNascita', 'comuneNascita',
      'statoResidenza', 'provinciaResidenza', 'comuneResidenza',
      'indirizzoResidenza', 'civicoResidenza', 'capResidenza'
    ];
    campiAura.forEach(field => this.anagraficaForm.get(field)?.disable());
  }

  /** Re-abilita tutti i campi anagrafica (quando si torna alla ricerca manuale) */
  private enableAnagrafica(): void {
    Object.keys(this.anagraficaForm.controls).forEach(key => {
      this.anagraficaForm.get(key)?.enable();
    });
  }

  // ============================================
  // STEP 2: ANAGRAFICA - CASCADE HANDLERS
  // ============================================

  onStatoNascitaChange(_cod: string): void {
    this.anagraficaForm.patchValue({ provinciaNascita: null, comuneNascita: '' });
    this.comuniNascitaOptions.set([]);
    const estero = this.isNascitaEstera;
    for (const name of ['provinciaNascita', 'comuneNascita']) {
      const ctrl = this.anagraficaForm.get(name);
      if (estero) { ctrl?.clearValidators(); } else { ctrl?.setValidators(Validators.required); }
      ctrl?.updateValueAndValidity();
    }
  }

  onProvinciaNascitaChange(provinciaId: number | null): void {
    this.anagraficaForm.patchValue({ comuneNascita: '' });
    this.comuniNascitaOptions.set([]);
    if (provinciaId) {
      this.comuneApi.getLista(provinciaId).subscribe(comuni => this.comuniNascitaOptions.set(comuni));
    }
  }

  onProvinciaResidenzaChange(provinciaId: number | null): void {
    this.anagraficaForm.patchValue({ comuneResidenza: '' });
    this.comuniResidenzaOptions.set([]);
    if (provinciaId) {
      this.comuneApi.getLista(provinciaId).subscribe(comuni => this.comuniResidenzaOptions.set(comuni));
    }
  }

  // onComuneResidenzaChange(comuneCod: string): void {
  //   this.anagraficaForm.patchValue({ capResidenza: comuneCod });
  // }

  onComuneResidenzaChange(_comuneCod: string): void {
  // Non tocchiamo il CAP: l'utente lo inserisce manualmente
}

  onComuneDomicilioChange(_comuneCod: string): void {
  // CAP inserito manualmente
}

  onStatoDomicilioChange(_cod: string): void {
    this.anagraficaForm.patchValue({ provinciaDomicilio: null, comuneDomicilio: '' });
    this.comuniDomicilioOptions.set([]);
    const coincide = this.anagraficaForm.get('domicilioCoincide')?.value;
    if (!coincide) {
      this.updateDomicilioValidators();
    }
  }

  private updateDomicilioValidators(): void {
    const estero = this.isDomicilioEstero;
    const provinciaComune = ['provinciaDomicilio', 'comuneDomicilio', 'capDomicilio'];
    for (const name of provinciaComune) {
      const ctrl = this.anagraficaForm.get(name);
      if (estero) { ctrl?.clearValidators(); } else { ctrl?.setValidators(Validators.required); }
      ctrl?.updateValueAndValidity();
    }
    const stato = this.anagraficaForm.get('statoDomicilio');
    stato?.setValidators(Validators.required);
    stato?.updateValueAndValidity();
    const indirizzo = this.anagraficaForm.get('indirizzoDomicilio');
    indirizzo?.setValidators(Validators.required);
    indirizzo?.updateValueAndValidity();
  }

  private clearDomicilioValidators(): void {
    for (const name of ['statoDomicilio', 'provinciaDomicilio', 'comuneDomicilio', 'indirizzoDomicilio', 'capDomicilio']) {
      const ctrl = this.anagraficaForm.get(name);
      ctrl?.clearValidators();
      ctrl?.updateValueAndValidity();
    }
  }

  onProvinciaDomicilioChange(provinciaId: number | null): void {
    this.anagraficaForm.patchValue({ comuneDomicilio: '' });
    this.comuniDomicilioOptions.set([]);
    if (provinciaId) {
      this.comuneApi.getLista(provinciaId).subscribe(comuni => this.comuniDomicilioOptions.set(comuni));
    }
  }

  onDomicilioCoincideChange(): void {
    const coincide = this.anagraficaForm.get('domicilioCoincide')?.value;
    if (coincide) {
      this.anagraficaForm.patchValue({
        statoDomicilio: this.anagraficaForm.get('statoResidenza')?.value,
        provinciaDomicilio: this.anagraficaForm.get('provinciaResidenza')?.value,
        comuneDomicilio: this.anagraficaForm.get('comuneResidenza')?.value,
        indirizzoDomicilio: this.anagraficaForm.get('indirizzoResidenza')?.value,
        civicoDomicilio: this.anagraficaForm.get('civicoResidenza')?.value,
        capDomicilio: this.anagraficaForm.get('capResidenza')?.value
      });
      this.comuniDomicilioOptions.set(this.comuniResidenzaOptions());
      this.clearDomicilioValidators();
    } else {
      this.anagraficaForm.patchValue({
        statoDomicilio: '', provinciaDomicilio: null, comuneDomicilio: '',
        indirizzoDomicilio: '', civicoDomicilio: '', capDomicilio: ''
      });
      this.comuniDomicilioOptions.set([]);
      this.updateDomicilioValidators();
    }
  }

  canProceedStep2(): boolean {
    // Un campo è OK se è valido OPPURE disabilitato (i campi AURA sono disabled ma popolati)
    const ok = (name: string) => {
      const c = this.anagraficaForm.get(name);
      return c?.valid === true || c?.disabled === true;
    };
    const coincide = this.anagraficaForm.get('domicilioCoincide')?.value;
    const domicilioOk = coincide || (
      ok('statoDomicilio') &&
      ok('indirizzoDomicilio') &&
      (this.isDomicilioEstero || (ok('provinciaDomicilio') && ok('comuneDomicilio') && ok('capDomicilio')))
    );

    return ok('codiceFiscale') &&
           ok('cognome') &&
           ok('nome') &&
           ok('dataNascita') &&
           ok('sesso') &&
           ok('statoNascita') &&
           ok('comuneNascita') &&
           ok('statoResidenza') &&
           ok('provinciaResidenza') &&
           ok('comuneResidenza') &&
           ok('indirizzoResidenza') &&
           ok('capResidenza') &&
           ok('telefono') &&
           domicilioOk;
  }

  goToStep3(): void {
    if (!this.canProceedStep2()) {
      this.markFormGroupTouched(this.anagraficaForm);
      this.showError('Compilare tutti i campi obbligatori');
      return;
    }

    if (!this.isFromAura()) {
      const cf = this.anagraficaForm.get('codiceFiscale')?.value?.trim()?.toUpperCase();
      this.cfInAuraChecking.set(true);

      // Prima verifica REEA
      this.anagraficaApi.search({ codice_fiscale: cf }).subscribe({
        next: (results) => {
          if (results && results.length > 0) {
            this.cfInAuraChecking.set(false);
            this.showError(`Il codice fiscale ${cf} è già presente nel REEA. Impossibile procedere con l'inserimento.`);
            return;
          }
          // Non in REEA → verifica AURA
          this.verificaCfInAuraDaStep3(cf);
        },
        error: () => this.verificaCfInAuraDaStep3(cf)
      });
    } else {
      this.currentStep.set(2);
    }
  }

  private verificaCfInAuraDaStep3(cf: string): void {
    this.auraService.findByCf(cf).subscribe({
      next: (_data) => {
        this.cfInAuraChecking.set(false);
        this.showError('Il Codice Fiscale è già presente nel sistema AURA. Torna al Passo 1 e utilizza la ricerca per Codice Fiscale per importare i dati anagrafici automaticamente.');
      },
      error: () => {
        this.cfInAuraChecking.set(false);
        this.currentStep.set(2);
      }
    });
  }

  goBackToStep1(): void {
    this.currentStep.set(0);
    // Resetta il form anagrafica per evitare dati stale se l'utente cambia ricerca
    this.enableAnagrafica();
    this.anagraficaForm.reset({ domicilioCoincide: true });
    this.comuniNascitaOptions.set([]);
    this.comuniResidenzaOptions.set([]);
    this.comuniDomicilioOptions.set([]);
    this.auraData.set(null);
    this.searchResultType.set(null);
    this.showSearchResult.set(false);
  }

  // ============================================
  // STEP 3: LUOGHI ESPOSIZIONE
  // ============================================

  addLuogoEsposizione(): void {
    this.luoghiEsposizione.set([
      ...this.luoghiEsposizione(),
      { azienda: '', provinciaAziendaId: null, provinciaAziendaSigla: '', comuneAziendaCod: '', comuneAziendaDesc: '', capAzienda: '', mansione: '', annoInizio: null, annoFine: null }
    ]);
    this.comuniEsposizioneOptions.set([...this.comuniEsposizioneOptions(), []]);
  }

  removeLuogoEsposizione(index: number): void {
    if (this.luoghiEsposizione().length > 1) {
      this.luoghiEsposizione.set(this.luoghiEsposizione().filter((_, i) => i !== index));
      this.comuniEsposizioneOptions.set(this.comuniEsposizioneOptions().filter((_, i) => i !== index));
    }
  }

  updateLuogoEsposizione(index: number, field: keyof LuogoEsposizione, value: string | number | null): void {
    const current = [...this.luoghiEsposizione()];
    current[index] = { ...current[index], [field]: value };
    this.luoghiEsposizione.set(current);
  }

  onProvinciaEsposizioneChange(index: number, provinciaId: number | null): void {
    this.updateLuogoEsposizione(index, 'provinciaAziendaId', provinciaId);
    const sigla = provinciaId ? (this.provinceOptions().find(p => p.provincia_id === provinciaId)?.provincia_sigla ?? '') : '';
    this.updateLuogoEsposizione(index, 'provinciaAziendaSigla', sigla);
    this.updateLuogoEsposizione(index, 'comuneAziendaCod', '');
    this.updateLuogoEsposizione(index, 'comuneAziendaDesc', '');

    const current = [...this.comuniEsposizioneOptions()];
    current[index] = [];
    this.comuniEsposizioneOptions.set(current);

    if (provinciaId) {
      this.comuneApi.getLista(provinciaId).subscribe(comuni => {
        const updated = [...this.comuniEsposizioneOptions()];
        updated[index] = comuni;
        this.comuniEsposizioneOptions.set(updated);
      });
    }
  }

  onComuneEsposizioneChange(index: number, comuneCod: string): void {
    const comuni = this.comuniEsposizioneOptions()[index] || [];
    const comune = comuni.find(c => c.comune_cod === comuneCod);
    this.updateLuogoEsposizione(index, 'comuneAziendaCod', comuneCod);
    this.updateLuogoEsposizione(index, 'comuneAziendaDesc', comune?.comune_desc || '');
    this.updateLuogoEsposizione(index, 'capAzienda', comune?.comune_cap || '');
  }

  /** Ritorna true se annoInizio > annoFine (inizio deve essere minore o uguale alla fine) */
  hasAnnoConflict(luogo: LuogoEsposizione): boolean {
    return luogo.annoInizio !== null && luogo.annoFine !== null && luogo.annoInizio > luogo.annoFine;
  }

  canProceedStep3(): boolean {
    return this.luoghiEsposizione().length > 0 &&
      this.luoghiEsposizione().every(l =>
        !!l.azienda?.trim() &&
        !!l.provinciaAziendaId &&
        !!l.comuneAziendaCod &&
        !!l.mansione?.trim() &&
        l.annoInizio !== null &&
        l.annoFine !== null &&
        l.annoInizio <= l.annoFine
      );
  }

  luogoHasErrors = signal<boolean[]>([false]);

  goToStep4(): void {
    if (!this.canProceedStep3()) {
      this.luogoHasErrors.set(this.luoghiEsposizione().map(() => true));
      this.showError('Compilare tutti i campi obbligatori dei luoghi di esposizione. Anno inizio deve essere minore o uguale all\'anno fine.');
      return;
    }
    this.luogoHasErrors.set(this.luoghiEsposizione().map(() => false));
    this.currentStep.set(3);
  }

  goBackToStep2(): void {
    this.currentStep.set(1);
  }

  // ============================================
  // STEP 4: RIEPILOGO E SALVATAGGIO
  // ============================================

  goBackToStep3(): void {
    this.currentStep.set(2);
  }

  getSessoLabel(value: string): string {
    return this.sessoOptions.find(o => o.value === value)?.label ?? value;
  }

  getNazioneLabel(istatCod: string): string {
    return this.nazioniOptions().find(n => n.nazione_istat_cod === istatCod)?.nazione_desc_it ?? istatCod;
  }

  getProvinciaLabel(provinciaId: number | null): string {
    if (!provinciaId) return '';
    const p = this.provinceOptions().find(p => p.provincia_id === provinciaId);
    return p ? `${p.provincia_desc} (${p.provincia_sigla})` : String(provinciaId);
  }

  getAslLabel(aslId: number): string {
    return this.aslOptions().find(a => a.asl_id === aslId)?.asl_azienda_desc ?? String(aslId);
  }

  onSalva(): void {
    if (!this.trattamentoDatiAccettato()) {
      this.showError('È necessario accettare il trattamento dei dati personali');
      return;
    }

    if (!this.riepilogoForm.valid) {
      this.markFormGroupTouched(this.riepilogoForm);
      this.showError('Inserire la data di presentazione istanza');
      return;
    }

    const dataPres = this.riepilogoForm.get('dataPresentazioneIstanza')?.value;
    const annoPresentazione = dataPres ? new Date(dataPres).getFullYear() : null;
    const annoEsposizioneMin = Math.min(
      ...this.luoghiEsposizione()
        .map(l => l.annoInizio)
        .filter((a): a is number => a !== null)
    );
    if (annoPresentazione !== null && isFinite(annoEsposizioneMin) && annoPresentazione < annoEsposizioneMin) {
      this.showError(`La data di presentazione istanza (${annoPresentazione}) non può essere antecedente all'inizio dell'esposizione (${annoEsposizioneMin})`);
      return;
    }

    if (!this.isFromAura() && !this.aslManualeId()) {
      this.aslManualeError.set(true);
      this.showError('Selezionare l\'ASL di assistenza dalla lista');
      return;
    }

    this.isSaving.set(true);

    // getRawValue() include anche i campi disabilitati (dati da AURA)
    const anagraficaData = this.anagraficaForm.getRawValue();
    const cfDaVerificare = anagraficaData.codiceFiscale?.toUpperCase();

    // Verifica che il CF non esista già in REEA prima di procedere
    this.anagraficaApi.search({ codice_fiscale: cfDaVerificare }).subscribe({
      next: (results) => {
        if (results && results.length > 0) {
          this.isSaving.set(false);
          this.showError(`Il codice fiscale ${cfDaVerificare} è già presente nel REEA. Impossibile procedere con l'inserimento.`);
          return;
        }
        this.verificaCfInAuraEProcedi(cfDaVerificare, anagraficaData);
      },
      error: () => {
        // Errore nella verifica CF in REEA: procedi comunque per non bloccare l'utente
        this.eseguiSalvataggio(anagraficaData);
      }
    });
  }

  private verificaCfInAuraEProcedi(cf: string, anagraficaData: any): void {
    if (this.isFromAura()) {
      this.eseguiSalvataggio(anagraficaData);
      return;
    }
    this.auraService.findByCf(cf).subscribe({
      next: (aura) => {
        this.auraData.set(aura);
        this.searchResultType.set('aura');
        this.applicaStatoAslDaAura(aura);
        this.eseguiSalvataggio(anagraficaData);
      },
      error: () => {
        // CF non in AURA: inserimento manuale puro
        this.eseguiSalvataggio(anagraficaData);
      }
    });
  }

  private applicaStatoAslDaAura(aura: AuraAssistitoDto): void {
    // data_fine_asl = "9999-12-31" è il sentinel AURA per "nessuna fine" → ignorato;
    // una data futura non giustifica ancora l'esclusione per emigrazione.
    const dataFineAslReale = this.isFineAslGiaTrascorsa(aura.data_fine_asl)
      ? aura.data_fine_asl : null;
    if (aura.data_decesso) {
      this.auraStato.set(StatoAssistito.ESCLUSO_DECESSO);
    } else if (dataFineAslReale) {
      this.auraStato.set(StatoAssistito.ESCLUSO_EMIGRAZIONE);
    } else {
      this.auraStato.set(StatoAssistito.ELEGGIBILE);
    }
    this.aslAuraDesc.set(aura.asl_assistenza_desc || 'Non definita');
    const aslMatch =
      (aura.asl_assistenza_cod
        ? this.aslOptions().find(a => a.asl_cod === aura.asl_assistenza_cod)
        : null)
      ?? (aura.asl_assistenza_desc
        ? this.aslOptions().find(a => a.asl_azienda_desc === aura.asl_assistenza_desc)
        : null);
    this.aslAuraId.set(aslMatch?.asl_id ?? null);
  }

  private eseguiSalvataggio(anagraficaData: any): void {
    const luoghiData = this.luoghiEsposizione();

    // Lookup provincia sigla/desc e comune desc dalle options caricate
    const provNascita = this.provinceOptions().find(p => p.provincia_id === anagraficaData.provinciaNascita);
    const provResidenza = this.provinceOptions().find(p => p.provincia_id === anagraficaData.provinciaResidenza);
    const provDomicilio = this.provinceOptions().find(p => p.provincia_id === anagraficaData.provinciaDomicilio);
    const comuneNascitaDesc = this.comuniNascitaOptions().find(c => c.comune_cod === anagraficaData.comuneNascita)?.comune_desc ?? null;
    const comuneResidenzaDesc = this.comuniResidenzaOptions().find(c => c.comune_cod === anagraficaData.comuneResidenza)?.comune_desc ?? null;
    const comuneDomicilioDesc = this.comuniDomicilioOptions().find(c => c.comune_cod === anagraficaData.comuneDomicilio)?.comune_desc ?? null;

    const assistitoData = {
      codiceFiscale: anagraficaData.codiceFiscale || '',
      cognome: anagraficaData.cognome || '',
      nome: anagraficaData.nome || '',
      dataNascita: anagraficaData.dataNascita || '',
      sesso: anagraficaData.sesso,
      statoNascita: anagraficaData.statoNascita,
      provinciaNascita: anagraficaData.provinciaNascita,
      provinciaNascitaSigla: provNascita?.provincia_sigla ?? null,
      provinciaNascitaDesc: provNascita?.provincia_desc ?? null,
      comuneNascita: anagraficaData.comuneNascita,
      comuneNascitaDesc: comuneNascitaDesc,
      residenza: {
        stato: anagraficaData.statoResidenza,
        statoDesc: this.getNazioneLabel(anagraficaData.statoResidenza) || null,
        provincia: anagraficaData.provinciaResidenza,
        provinciaSigla: provResidenza?.provincia_sigla ?? null,
        provinciaDesc: provResidenza?.provincia_desc ?? null,
        comune: anagraficaData.comuneResidenza,
        comuneDesc: comuneResidenzaDesc,
        indirizzo: anagraficaData.indirizzoResidenza,
        civico: anagraficaData.civicoResidenza,
        cap: anagraficaData.capResidenza
      },
      domicilio: anagraficaData.domicilioCoincide ? null : {
        stato: anagraficaData.statoDomicilio,
        provincia: anagraficaData.provinciaDomicilio,
        provinciaSigla: provDomicilio?.provincia_sigla ?? null,
        provinciaDesc: provDomicilio?.provincia_desc ?? null,
        comune: anagraficaData.comuneDomicilio,
        comuneDesc: comuneDomicilioDesc,
        indirizzo: anagraficaData.indirizzoDomicilio,
        civico: anagraficaData.civicoDomicilio,
        cap: anagraficaData.capDomicilio
      },
      domicilioCoincide: anagraficaData.domicilioCoincide,
      telefono: anagraficaData.telefono,
      email: anagraficaData.email,
      telefonoAura: this.isFromAura() ? (this.auraTelefoni().join(';') || null) : null,
      emailAura: this.isFromAura() ? (this.auraEmails().join(';') || null) : null,
      luoghiEsposizione: luoghiData.map(l => ({
        azienda: l.azienda,
        comune: l.comuneAziendaDesc,
        comuneCod: l.comuneAziendaCod,
        provinciaAziendaSigla: l.provinciaAziendaSigla || null,
        cap: l.capAzienda || undefined,
        mansione: l.mansione,
        annoInizio: l.annoInizio,
        annoFine: l.annoFine
      })),
      dataPresentazioneIstanza: this.riepilogoForm.get('dataPresentazioneIstanza')?.value || null,
      aslId: this.isFromAura() ? this.aslAuraId() : this.aslManualeId(),
      // aslCod: cod stringa richiesto da AnagraficaServiceImpl.getAslIdByAslCod()
      aslCod: this.isFromAura()
        ? (this.auraData()?.asl_assistenza_cod ?? null)
        : (this.aslOptions().find(a => a.asl_id === this.aslManualeId())?.asl_cod ?? null),
      // idAura: evita double-call AURA nel BE
      idAura: this.isFromAura() ? (this.auraData()?.id_aura ?? null) : null,
      // data_fine_asl AURA: sentinel "9999-12-31" = nessuna fine → null
      assistenzaAslFine: this.isFromAura()
        ? (this.auraData()?.data_fine_asl && this.auraData()!.data_fine_asl !== '9999-12-31'
            ? this.auraData()!.data_fine_asl
            : null)
        : null,
      esenzioni: this.isFromAura() ? this.auraEsenzioni() : [],
      stato: this.auraStato(),
      fonteProvenienza: FonteProvenienza.SEGNALAZIONE_MMG,
      dataInserimento: new Date(),
      dataUltimoAggiornamento: new Date()
    };

    this.assistitiService.createAssistito(assistitoData).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.showSuccess('Assistito salvato con successo');
        this.tornaAllElenco();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.showError('Errore durante il salvataggio');
        console.error('Errore salvataggio assistito:', err);
      }
    });
  }

  // ============================================
  // AURA HELPERS
  // ============================================

  /**
   * Matcha un comune per descrizione: prima esatto, poi startsWith in entrambe le direzioni.
   * Necessario perché AURA può dare "MIGNANO" mentre il DB ha "MIGNANO MONTE LUNGO".
   */
  private matchComuneByDesc(comuni: ComuneDTO[], desc: string): ComuneDTO | undefined {
    const d = desc.toUpperCase();
    return comuni.find(c => c.comune_desc?.toUpperCase() === d)
      ?? comuni.find(c => c.comune_desc?.toUpperCase().startsWith(d))
      ?? comuni.find(c => d.startsWith(c.comune_desc?.toUpperCase() ?? '~~'));
  }

  // ============================================
  // FORM HELPERS
  // ============================================

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      formGroup.get(key)?.markAsTouched();
    });
  }

  onCapInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const numericOnly = input.value.replace(/\D/g, '');
    input.value = numericOnly;
    this.anagraficaForm.get('capResidenza')?.setValue(numericOnly, { emitEvent: false });
  }

  minAge18Validator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    const birth = new Date(control.value);
    if (isNaN(birth.getTime())) return null;
    const today = new Date();
    const age = today.getFullYear() - birth.getFullYear() -
      (today < new Date(today.getFullYear(), birth.getMonth(), birth.getDate()) ? 1 : 0);
    return age >= 18 ? null : { minAge18: true };
  }

  hasError(form: FormGroup, fieldName: string): boolean {
    const field = form.get(fieldName);
    return field ? field.invalid && field.touched : false;
  }

  getErrorMessage(form: FormGroup, fieldName: string): string {
    const field = form.get(fieldName);
    if (!field) return '';
    if (field.hasError('required')) return 'Campo obbligatorio';
    if (field.hasError('email')) return 'Email non valida';
    if (field.hasError('minAge18')) return 'L\'assistito deve avere almeno 18 anni';
    if (field.hasError('pattern')) return 'Sono ammessi solo numeri';
    return 'Valore non valido';
  }

  // ============================================
  // NOTIFICATIONS
  // ============================================

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Chiudi', {
      duration: 3000,
      panelClass: ['snackbar-success']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Chiudi', {
      duration: 5000,
      panelClass: ['snackbar-error']
    });
  }
}
