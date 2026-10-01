// ============================================
// DETTAGLIO ASSISTITO COMPONENT
// ============================================

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';

// Angular Material
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

// Dialogs
import { ConfermaEliminazioneDialogComponent } from '../../components/conferma-eliminazione-dialog/conferma-eliminazione-dialog.component';
import { ValutaAssistitoDialogComponent, ValutaAssistitoDialogResult, ValutaAssistitoMode } from '../../components/valuta-assistito-dialog/valuta-assistito-dialog.component';
import { CrptDialogComponent, CrptDialogResult } from '../../components/crpt-dialog/crpt-dialog.component';
import { EsitoVisitaDialogComponent } from '../../components/esito-visita-dialog/esito-visita-dialog.component';

// Components
import { AppHeaderComponent } from '@shared/components/app-header/app-header.component';
import { StatusBadgeComponent } from '@shared/components/status-badge/status-badge.component';

import { catchError, forkJoin, of } from 'rxjs';

// Services & API
import { AssistitiService, AuthService } from '@core/services';
import { AnagraficaApiService } from '../../../../api/anagrafica-api.service';
import { NotaApiService } from '../../../../api/nota-api.service';
import { SpresalApiService } from '../../../../api/spresal-api.service';
import { AdesioniApiService } from '../../../../api/adesioni-api.service';
import { FileApiService, StoriaProfFileDTO } from '../../../../api/file-api.service';
import { AuditApiService } from '../../../../api/audit-api.service';

// Models
import {
  Assistito,
  StatoAssistito,
  FonteProvenienza,
  PreAdesioneData,
  EsposizioneLavorativa,
  StoriaStato,
  InailRecord,
  NplaRecord,
  SpresalEsitiDTO,
  SpresalAnamnesiRecord,
  RuoloUtente
} from '@core/models';

export interface Nota {
  nota_id: number;
  registro_id: number;
  descrizione: string;
  data_modifica: string;
  utente_modifica: string;
}

@Component({
  selector: 'app-dettaglio-assistito',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatTableModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MatDialogModule,
    AppHeaderComponent,
    StatusBadgeComponent,
    CrptDialogComponent
  ],
  templateUrl: './dettaglio-assistito.component.html',
  styleUrl: './dettaglio-assistito.component.scss'
})
export class DettaglioAssistitoComponent implements OnInit {

  // ============================================
  // DEPENDENCY INJECTION
  // ============================================

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private assistitiService = inject(AssistitiService);
  private anagraficaApi = inject(AnagraficaApiService);
  private notaApi = inject(NotaApiService);
  private spresalApi = inject(SpresalApiService);
  private adesioniApi = inject(AdesioniApiService);
  private fileApi = inject(FileApiService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private authService = inject(AuthService);
  private auditApi = inject(AuditApiService);

  // ============================================
  // STATE
  // ============================================

  private readonly FIELD_MAP: Record<string, string> = {
    // Card anagrafica (assistito)
    'a.codiceFiscale':              'aura_codice_fiscale',
    'a.nome':                       'aura_nome',
    'a.cognome':                    'aura_cognome',
    'a.telefono':                   'soggetto_telefono',
    'a.email':                      'soggetto_email',
    // Tab Preadesione
    'p.codiceAdesione':             'adesione_codice_adesione',
    'p.codiceFiscale':              'adesione_codice_fiscale',
    'p.cognome':                    'adesione_cognome',
    'p.nome':                       'adesione_nome',
    'p.dataNascita':                'adesione_data_di_nascita',
    'p.comuneNascita':              'adesione_comune_di_nascita',
    'p.tesseraTeam':                'adesione_tessera_team',
    'p.idAura':                     'adesione_id_aura',
    'p.comuneDomicilio':            'adesione_comune_di_domicilio',
    'p.codiceComuneIstatDomicilio': 'adesione_codice_comune_istat_di_domicilio',
    'p.capDomicilio':               'adesione_cap_di_domicilio',
    'p.email':                      'adesione_email',
    'p.telefono':                   'adesione_telefono',
    // Tabella esposizioni — chiavi generate dal backend dal DTO (adesione_esposizione_*)
    'e.azienda':                    'adesione_esposizione_azienda',
    'e.codiceComuneIstatAzienda':   'adesione_esposizione_azienda_comune_cod',
    'e.comuneAzienda':              'adesione_esposizione_azienda_comune_desc',
    'e.capAzienda':                 'adesione_esposizione_azienda_cap',
    // Tab Anamnesi
    'an.nomeIndirizzoDitta':        'anamnesi_occupazione_nome_e_indirizzo_ditta',
    'an.ragioneSocialeDitta':       'anamnesi_occupazione_ragione_sociale_ditta_crpt',
    'an.pivaDitta':                 'anamnesi_occupazione_piva_ditta_crpt',
    'an.codiceFiscaleDitta':        'anamnesi_occupazione_codice_fiscale_ditta_crpt',
    // Tab INAIL
    'in.domanda':                   'inail_domanda',
    'in.cognome':                   'inail_cognome',
    'in.nome':                      'inail_nome',
    'in.codiceFiscale':             'inail_codice_fiscale',
    'in.dataNascita':               'inail_data_nascita',
    'in.indirizzo':                 'inail_indirizzo_residenza',
    'in.istat':                     'inail_istat_residenza',
    'in.cap':                       'inail_cap_residenza',
    'in.comune':                    'inail_comune_residenza',
    // Tab NPLA
    'np.codiceFiscale':             'npla_codice_fiscale',
    'np.aziendaPiva':               'npla_azienda_piva',
    'np.aziendaNome':               'npla_azienda_nome',
    'np.comuneCantiere':            'npla_comune_cantiere',
  };

  campiAnon = signal<Record<string, string | null> | null>(null);
  isPseudo = this.authService.isPseudo;
  isSpresalNonPseudo = this.authService.isSpresalNonPseudo;

  canEditCounseling = computed(() => {
    const ruolo = this.authService.currentUser()?.ruolo;
    return ruolo === RuoloUtente.OPERATORE_CRPT || ruolo === RuoloUtente.OPERATORE_CSI;
  });

  canConcludiSorveglianza = computed(() =>
    this.assistito()?.stato === StatoAssistito.AVVIATO_SORV &&
    (this.authService.isSpresalNonPseudo() || this.authService.currentUser()?.ruolo === RuoloUtente.OPERATORE_CSI)
  );

  isEditingCounseling  = signal<boolean>(false);
  counselingEditValue  = signal<string>('null');
  isSavingCounseling   = signal<boolean>(false);

  assistitoId = signal<string>('');
  assistito = signal<Assistito | null>(null);
  loading = signal<boolean>(true);
  isSaving = signal<boolean>(false);
  isEditingContatti = signal<boolean>(false);
  activeTab = signal<number>(0);

  // Storia stati
  storiaStati = signal<StoriaStato[]>([]);
  storiaStatiLoading = signal<boolean>(false);
  storiaStatiColumns: string[] = ['stato', 'dataModifica', 'operatoreModifica', 'notaPassaggioStato'];

  // Note
  note = signal<Nota[]>([]);
  noteColumns: string[] = ['descrizione', 'dataCreazione', 'utente', 'azioni'];
  notaInModifica = signal<Nota | null>(null);

  // ============================================
  // FORMS
  // ============================================

  contattiForm: FormGroup = this.fb.group({
    telefono: [''],
    email: ['', [Validators.email]]
  });

  get telefonoControl(): FormControl {
    return this.contattiForm.get('telefono') as FormControl;
  }

  get emailControl(): FormControl {
    return this.contattiForm.get('email') as FormControl;
  }

  nuovaNotaControl = new FormControl('');
  editNotaControl = new FormControl('');

  // Cambio stato
  showStatoPanel = signal<boolean>(false);
  statoScelto = new FormControl<number | null>(null);
  notaCambioStato = new FormControl<string>('');

  private readonly STATI_NEGATIVI = new Set([
    StatoAssistito.NON_ELEGGIBILE,
    StatoAssistito.NON_PRESO_IN_CARICO
  ]);

  private readonly transizioniPossibili: Partial<Record<StatoAssistito, Array<{ id: number; label: string; stato: StatoAssistito }>>> = {
    [StatoAssistito.DA_VALUTARE]: [
      { id: 2, label: 'Eleggibile',       stato: StatoAssistito.ELEGGIBILE },
      { id: 3, label: 'Non eleggibile',   stato: StatoAssistito.NON_ELEGGIBILE }
    ],
    [StatoAssistito.CARICATO]: [
      { id: 2, label: 'Eleggibile',       stato: StatoAssistito.ELEGGIBILE },
      { id: 3, label: 'Non eleggibile',   stato: StatoAssistito.NON_ELEGGIBILE }
    ],
    [StatoAssistito.ELEGGIBILE]: [
      { id: 4,  label: 'Preso in carico',     stato: StatoAssistito.PRESO_IN_CARICO },
      { id: 11, label: 'Non preso in carico', stato: StatoAssistito.NON_PRESO_IN_CARICO }
    ],
    [StatoAssistito.AVVIATO_SORV]: [
      { id: 7, label: 'Sorveglianza conclusa', stato: StatoAssistito.CONCLU_SORV }
    ]
  };

  get storiaStatiTableData(): (StoriaStato & { isCurrent?: boolean })[] {
    const assistito = this.assistito();
    if (!assistito) return this.storiaStati();
    const storia = this.storiaStati();
    const filterCf = (cf?: string | null): string => {
      if (!cf) return '';
      return ['ADMIN', 'SISTEMA', 'SCONOSCIUTO'].includes(cf.toUpperCase()) ? '' : cf;
    };
    const normDate = (val: Date | string | null | undefined): string | null => {
      if (!val) return null;
      const str = typeof val === 'string' ? val : (val as Date).toISOString();
      if (/^\d{4}-\d{2}-\d{2}T/.test(str)) return str.substring(0, 19);
      return str || null;
    };
    const isDaValutareIniziale = assistito.stato === StatoAssistito.DA_VALUTARE && storia.length === 0;
    const dataCorrente = isDaValutareIniziale
      ? (assistito.dataInserimento || null)
      : ((assistito.dataUltimoAggiornamento || null)
          || (storia.length > 0
            ? normDate(storia[storia.length - 1].data_modifica || storia[storia.length - 1].data_creazione)
            : null)
          || (assistito.dataInserimento || null));
    const operatoreAttuale = isDaValutareIniziale
      ? filterCf(assistito.utenteCreazione)
      : (filterCf(assistito.utenteModifica) || filterCf(storia.length > 0 ? storia[storia.length - 1].utente_modifica : ''));
    const rigaAttuale: StoriaStato & { isCurrent: boolean } = {
      soggetto_stato_id: 'current',
      soggetto_stato_desc: assistito.stato,
      data_modifica: dataCorrente,
      utente_modifica: operatoreAttuale,
      soggetto_stato_note: assistito.notaStatoAttuale ?? '',
      data_creazione: null,
      isCurrent: true
    };
    const storicoOrdinato = [...storia].reverse().map((s, idx) => {
      const isOldest = idx === storia.length - 1;
      const rigaPrecedente = isOldest ? null : storia[storia.length - 2 - idx];
      const cfOperatore = isOldest
        ? filterCf(assistito.utenteCreazione)
        : filterCf(rigaPrecedente?.utente_modifica);
      const dataIngresso = isOldest
        ? (assistito.dataInserimento || null)
        : normDate(rigaPrecedente?.data_modifica || rigaPrecedente?.data_creazione);
      return {
        ...s,
        utente_modifica: cfOperatore,
        data_modifica: dataIngresso
      };
    });
    return [rigaAttuale, ...storicoOrdinato];
  }

  get statiDisponibili() {
    const stato = this.assistito()?.stato;
    return stato ? (this.transizioniPossibili[stato] ?? []) : [];
  }

  get isNotaObbligatoria(): boolean {
    const id = this.statoScelto.value;
    if (!id) return false;
    const transizione = this.statiDisponibili.find(t => t.id === id);
    return transizione ? this.STATI_NEGATIVI.has(transizione.stato) : false;
  }

  // ============================================
  // DATI PREADESIONE (mock)
  // ============================================

  preAdesioneData = signal<PreAdesioneData>({});

  esposizioniColumns: string[] = ['azienda', 'codiceComuneIstatAzienda', 'comuneAzienda', 'capAzienda', 'provinciaAzienda', 'mansione', 'dataInizio', 'dataFine'];
  esposizioni = signal<EsposizioneLavorativa[]>([]);

  inailColumns: string[] = ['tipologiaInail', 'domanda', 'cognome', 'nome', 'codiceFiscale'];
  inailRecords = signal<InailRecord[]>([]);

  nplaColumns: string[] = ['codiceFiscale', 'periodo', 'idCantiere', 'aziendaPiva', 'aziendaNome', 'aslCantiere', 'comuneCantiere', 'anno', 'tipologiaPiano', 'quantitaDaRimuovere', 'quantitaRimossa'];
  nplaRecords = signal<NplaRecord[]>([]);

  anamnesiSpresal = signal<SpresalAnamnesiRecord[]>([]);
  expandedCrptId = signal<number | null>(null);

  occupazioneColumns: string[] = ['occupazioneNum', 'occupazioneAnnoInizio', 'occupazioneAnnoFine', 'occupazioneTipo', 'occupazioneDescrizioneLavoro', 'occupazioneNomeIndirizzoDitta', 'occupazioneAttivitaDitta', 'occupazioneEsposizione', 'azioni'];

  isDetailRow = (_index: number, _row: SpresalAnamnesiRecord) => true;

  // SPRESAL Esiti
  spresalEsiti = signal<SpresalEsitiDTO[]>([]);
  spresalEsitiLoading = signal<boolean>(false);
  editingAcquisita = signal<{ esitoId: number; field: string } | null>(null);
  editAcquistaValue = signal<string>('null');
  isSavingAcquisita = signal<boolean>(false);

  // Storia Professionale
  storiaProfFiles = signal<StoriaProfFileDTO[]>([]);
  storiaProfLoading = signal<boolean>(false);
  storiaProfUploading = signal<boolean>(false);

  // ============================================
  // LIFECYCLE
  // ==============================================

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.assistitoId.set(id);
      this.loadAssistito(id);
    } else {
      this.showError('ID assistito non trovato');
      this.tornaAllElenco();
    }
  }

  // ============================================
  // DATA LOADING
  // ============================================

  private loadAssistito(id: string): void {
    this.loading.set(true);

    const assistito$ = this.assistitiService.getAssistitoById(id);
    const campiAnon$ = this.authService.isPseudo()
      ? this.anagraficaApi.getByIdCampiAnonimizzati(Number(id), true)
          .pipe(catchError(() => of(null as Record<string, string | null> | null)))
      : of(null as Record<string, string | null> | null);

    forkJoin({ assistito: assistito$, campiAnon: campiAnon$ }).subscribe({
      next: ({ assistito, campiAnon }) => {
        this.campiAnon.set(campiAnon);
        this.assistito.set(assistito);
        this.auditApi.salva('read', 'ANAGRAFICA', `dettaglio assistito - ${assistito.codiceFiscale}`).subscribe();
        this.contattiForm.patchValue({
          telefono: assistito.telefono || '',
          email: assistito.email || ''
        });
        this.esposizioni.set(assistito.esposizioni ?? []);
        // Deduplica per domanda (possibile cartesian product da JOIN backend)
        const rawInail = assistito.inailRecords ?? [];
        const seenInail = new Set<string>();
        this.inailRecords.set(rawInail.filter(r => {
          const key = `${r.domanda ?? ''}_${r.registroId ?? ''}`;
          if (seenInail.has(key)) return false;
          seenInail.add(key);
          return true;
        }));
        this.nplaRecords.set(assistito.nplaRecords ?? []);
        // Deduplica per regSpresalAnamnesiId (possibile cartesian product da JOIN backend)
        const rawAnamnesi = assistito.anamnesiSpresalRecords ?? [];
        const seenAnamnesi = new Set<number>();
        this.anamnesiSpresal.set(rawAnamnesi.filter(r => {
          if (r.regSpresalAnamnesiId == null || seenAnamnesi.has(r.regSpresalAnamnesiId)) return false;
          seenAnamnesi.add(r.regSpresalAnamnesiId);
          return true;
        }).sort((a, b) => (Number(a.occupazioneNum) || 0) - (Number(b.occupazioneNum) || 0)));
        this.storiaProfFiles.set(assistito.listaPdf ?? []);
        this.loading.set(false);
        this.computeInitialTab();
        this.loadStoriaStati(Number(id));
        this.loadNote(Number(id));
        if (assistito.registroId) {
          this.loadEsiti(assistito.registroId);
        }
        // Carica i dati preadesione da AdesioneController (T_ADESIONE)
        this.loadAdesioneData(Number(id), assistito);
      },
      error: (err) => {
        console.error('Errore caricamento assistito:', err);
        this.showError('Errore nel caricamento dei dati');
        this.loading.set(false);
      }
    });
  }


  private loadAdesioneData(soggettoId: number, assistito: Assistito): void {
    this.adesioniApi.getAdesioneBySoggettoId(soggettoId).subscribe({
      next: (records) => {
        const ads = records?.[0];
        if (ads) {
          this.preAdesioneData.set({
            codiceAdesione: ads.adesione_cod || assistito.codiceAdesione || '',
            dataPreAdesione: ads.adesione_data || String(assistito.dataAdesione || ''),
            codiceFiscale: ads.codice_fiscale || assistito.codiceFiscale || '',
            cognome: ads.cognome || assistito.cognome || '',
            nome: ads.nome || assistito.nome || '',
            dataNascita: ads.nascita_data || (assistito.dataNascita ? String(assistito.dataNascita) : ''),
            provinciaNascita: ads.nascita_provincia_desc || assistito.provinciaNascita || '',
            comuneNascita: ads.nascita_comune_desc || assistito.comuneNascita || '',
            tesseraTeam: ads.tessera_team || assistito.tesseraTeam || '',
            idAura: ads.id_aura || assistito.idAura || '',
            provinciaDomicilio: ads.domicilio_provincia_desc || assistito.provinciaDomicilio || '',
            comuneDomicilio: ads.domicilio_comune_desc || assistito.comuneDomicilio || '',
            codiceComuneIstatDomicilio: ads.domicilio_comune_cod || assistito.codiceComuneIstatDomicilio || '',
            capDomicilio: ads.domicilio_cap || assistito.capDomicilio || '',
            email: ads.email || assistito.preAdesioneEmail || '',
            telefono: ads.telefono || assistito.preadesioneTelefono || '',
            codiceAslDomicilio: ads.domicilio_asl_cod || assistito.codiceAslDomicilio || '',
            aslDomicilio: ads.domicilio_asl_desc || assistito.aslDomicilio || '',
            codiceAslResidenza: ads.residenza_asl_cod || assistito.codiceAslResidenza || '',
            aslResidenza: ads.residenza_asl_desc || assistito.aslResidenza || ''
          });
        } else {
          // Nessun record T_ADESIONE trovato: usa i dati di T_SOGGETTO come fallback
          this.preAdesioneData.set({
            codiceAdesione: assistito.codiceAdesione || '',
            dataPreAdesione: String(assistito.dataAdesione || ''),
            codiceFiscale: assistito.codiceFiscale || '',
            cognome: assistito.cognome || '',
            nome: assistito.nome || '',
            dataNascita: assistito.dataNascita ? String(assistito.dataNascita) : '',
            provinciaNascita: assistito.provinciaNascita || '',
            comuneNascita: assistito.comuneNascita || '',
            tesseraTeam: assistito.tesseraTeam || '',
            idAura: assistito.idAura || '',
            provinciaDomicilio: assistito.provinciaDomicilio || '',
            comuneDomicilio: assistito.comuneDomicilio || '',
            codiceComuneIstatDomicilio: assistito.codiceComuneIstatDomicilio || '',
            capDomicilio: assistito.capDomicilio || '',
            email: assistito.preAdesioneEmail || '',
            telefono: assistito.preadesioneTelefono || '',
            codiceAslDomicilio: assistito.codiceAslDomicilio || '',
            aslDomicilio: assistito.aslDomicilio || '',
            codiceAslResidenza: assistito.codiceAslResidenza || '',
            aslResidenza: assistito.aslResidenza || ''
          });
        }
      },
      error: () => {
        // Fallback su T_SOGGETTO se l'endpoint non è disponibile o dà errore
        this.preAdesioneData.set({
          codiceAdesione: assistito.codiceAdesione || '',
          dataPreAdesione: String(assistito.dataAdesione || ''),
          codiceFiscale: assistito.codiceFiscale || '',
          cognome: assistito.cognome || '',
          nome: assistito.nome || '',
          dataNascita: assistito.dataNascita ? String(assistito.dataNascita) : '',
          provinciaNascita: assistito.provinciaNascita || '',
          comuneNascita: assistito.comuneNascita || '',
          tesseraTeam: assistito.tesseraTeam || '',
          idAura: assistito.idAura || '',
          provinciaDomicilio: assistito.provinciaDomicilio || '',
          comuneDomicilio: assistito.comuneDomicilio || '',
          codiceComuneIstatDomicilio: assistito.codiceComuneIstatDomicilio || '',
          capDomicilio: assistito.capDomicilio || '',
          email: assistito.preAdesioneEmail || '',
          telefono: assistito.preadesioneTelefono || '',
          codiceAslDomicilio: assistito.codiceAslDomicilio || '',
          aslDomicilio: assistito.aslDomicilio || '',
          codiceAslResidenza: assistito.codiceAslResidenza || '',
          aslResidenza: assistito.aslResidenza || ''
        });
      }
    });
  }

  private loadNote(soggettoId: number): void {
    this.notaApi.getNote(soggettoId).subscribe({
      next: (note) => {
        this.note.set(note as unknown as Nota[]);
        const cf = this.assistito()?.codiceFiscale ?? String(soggettoId);
        this.auditApi.salva('read', 'ANAGRAFICA', `Dettaglio assistito - visualizza note - ${cf}`).subscribe();
      },
      error: () => {}
    });
  }

  private loadEsiti(registroId: number): void {
    this.spresalEsitiLoading.set(true);
    this.spresalApi.getEsiti(registroId).subscribe({
      next: (esiti) => {
        // Deduplica per reg_spresal_esiti_id (possibile cartesian product da JOIN backend)
        const seenEsiti = new Set<number>();
        const unique = esiti.filter(e => {
          if (e.reg_spresal_esiti_id == null || seenEsiti.has(e.reg_spresal_esiti_id)) return false;
          seenEsiti.add(e.reg_spresal_esiti_id);
          return true;
        });
        this.spresalEsiti.set(unique);
        this.spresalEsitiLoading.set(false);
      },
      error: () => this.spresalEsitiLoading.set(false)
    });
  }

  private loadStoriaStati(soggettoId: number): void {
    this.storiaStatiLoading.set(true);
    this.anagraficaApi.getStoriaStati(soggettoId).subscribe({
      next: (storia) => {
        this.storiaStati.set(storia);
        this.storiaStatiLoading.set(false);
        const cf = this.assistito()?.codiceFiscale ?? String(soggettoId);
        this.auditApi.salva('read', 'ANAGRAFICA', `Dettaglio assistito - visualizza storia stati - ${cf}`).subscribe();
      },
      error: () => {
        this.storiaStatiLoading.set(false);
      }
    });
  }

  // =============================================
  // NAVIGATION
  // =============================================

  tornaAllElenco(): void {
    this.router.navigate(['/registro-amianto/assistiti']);
  }

  apriEsitoVisita(titolo: string, testo: string, dataVisita?: string | null): void {
    let data: string | undefined;
    if (dataVisita) {
      const d = new Date(dataVisita);
      if (!isNaN(d.getTime())) {
        data = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
      }
    }
    this.dialog.open(EsitoVisitaDialogComponent, {
      data: { titolo, testo, data },
      width: '600px',
      maxWidth: '95vw',
      autoFocus: false
    });
  }

  vaiAdAnagraficaCompleta(): void {
    const cf = this.assistito()?.codiceFiscale ?? this.assistitoId();
    this.auditApi.salva('read', 'ANAGRAFICA', `Dettaglio assistito - visualizza anagrafica completa - ${cf}`).subscribe();
    this.router.navigate(['/registro-amianto/assistiti', this.assistitoId(), 'anagrafica']);
  }

  // ============================================
  // CONTATTI ACTIONS
  // ============================================

  abilitaModificaContatti(): void {
    this.isEditingContatti.set(true);
  }

  // ============================================
  // PRESTAZIONE ACQUISITA ESITI
  // ============================================

  apriEditAcquisita(esitoId: number, field: string, currentValue: boolean | null | undefined): void {
    this.editingAcquisita.set({ esitoId, field });
    const str = currentValue == null ? 'null' : String(currentValue);
    this.editAcquistaValue.set(str);
  }

  annullaEditAcquisita(): void {
    this.editingAcquisita.set(null);
  }

  salvaAcquisita(): void {
    const editing = this.editingAcquisita();
    if (!editing) return;
    const raw = this.editAcquistaValue();
    const value: boolean | null = raw === 'null' ? null : raw === 'true';
    this.isSavingAcquisita.set(true);
    this.spresalApi.aggiornaPrestazioneAcquisita(editing.esitoId, editing.field, value).subscribe({
      next: () => {
        const idx = this.spresalEsiti().findIndex(e => e.reg_spresal_esiti_id === editing.esitoId);
        if (idx >= 0) {
          const updated = [...this.spresalEsiti()];
          (updated[idx] as any)[editing.field] = value;
          this.spresalEsiti.set(updated);
        }
        this.editingAcquisita.set(null);
        this.isSavingAcquisita.set(false);
        this.snackBar.open('Salvato', '', { duration: 2000 });
      },
      error: () => {
        this.isSavingAcquisita.set(false);
        this.snackBar.open('Errore nel salvataggio', '', { duration: 3000 });
      }
    });
  }

  annullaModificaContatti(): void {
    const assistito = this.assistito();
    if (assistito) {
      this.contattiForm.patchValue({
        telefono: assistito.telefono || '',
        email: assistito.email || ''
      });
    }
    this.isEditingContatti.set(false);
  }

  salvaContatti(): void {
    if (this.contattiForm.invalid) {
      this.showError('Verifica i dati inseriti');
      return;
    }
    this.isSaving.set(true);
    const contatti = this.contattiForm.value;
    const assistito = this.assistito();
    if (!assistito?.id) {
      this.showError('ID assistito non trovato');
      this.isSaving.set(false);
      return;
    }
    this.assistitiService.modificaAssistito(assistito.id, {
      codice_fiscale: assistito.codiceFiscale || '',
      cognome: assistito.cognome || '',
      nome: assistito.nome || '',
      nascita_data: assistito.dataNascita ? String(assistito.dataNascita).substring(0, 10) : '',
      telefono: contatti.telefono || null,
      email: contatti.email || null
    }).subscribe({
      next: () => {
        this.assistito.set({ ...assistito, telefono: contatti.telefono, email: contatti.email });
        this.isSaving.set(false);
        this.isEditingContatti.set(false);
        this.showSuccess('Contatti aggiornati con successo');
      },
      error: (err) => {
        console.error('Errore salvataggio contatti:', err);
        this.showError('Errore durante il salvataggio');
        this.isSaving.set(false);
      }
    });
  }

  // ============================================
  // NOTE ACTIONS
  // ============================================

  salvaNota(): void {
    const testo = this.nuovaNotaControl.value?.trim();
    if (!testo) return;
    this.notaApi.inserisci(Number(this.assistitoId()), testo).subscribe({
      next: (nota) => {
        this.note.set([nota as unknown as Nota, ...this.note()]);
        this.nuovaNotaControl.reset();
        this.showSuccess('Nota salvata');
      },
      error: () => this.showError('Errore durante il salvataggio della nota')
    });
  }

  abilitaModificaNota(nota: Nota): void {
    this.notaInModifica.set(nota);
    this.editNotaControl.setValue(nota.descrizione);
  }

  salvaModificaNota(): void {
    const nota = this.notaInModifica();
    if (!nota) return;
    const testo = this.editNotaControl.value?.trim();
    if (!testo) return;
    this.notaApi.modifica(nota.nota_id, testo).subscribe({
      next: (updated) => {
        this.note.set(this.note().map(n =>
          n.nota_id === nota.nota_id ? updated as unknown as Nota : n
        ));
        this.notaInModifica.set(null);
        this.editNotaControl.reset();
        this.showSuccess('Nota aggiornata');
      },
      error: () => this.showError('Errore durante la modifica della nota')
    });
  }

  eliminaNota(nota: Nota): void {
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

  annullaModificaNota(): void {
    this.notaInModifica.set(null);
    this.editNotaControl.reset();
  }

  // ============================================
  // CAMBIO STATO
  // ============================================

  /** Stati che mostrano il bottone di valutazione (stesso set del menu azioni in elenco) */
  private readonly STATI_CON_VALUTA = new Set([
    StatoAssistito.DA_VALUTARE,
    StatoAssistito.CARICATO,
    StatoAssistito.NON_ELEGGIBILE,
    StatoAssistito.ELEGGIBILE,
    StatoAssistito.PRESO_IN_CARICO,
    StatoAssistito.NON_PRESO_IN_CARICO,
    StatoAssistito.AVVIATO_SORV,
  ]);

  canCambiaStato(): boolean {
    return this.statiDisponibili.length > 0;
  }

  /** Mostra il bottone popup valutazione (stesso set del menu azioni in elenco) */
  canMostraValutaButton(): boolean {
    const stato = this.assistito()?.stato;
    if (!stato || !this.STATI_CON_VALUTA.has(stato)) return false;
    // SPRESAL non può valutare/cambia valutazione
    if (this.authService.isSpresalNonPseudo() &&
        (stato === StatoAssistito.DA_VALUTARE || stato === StatoAssistito.CARICATO || stato === StatoAssistito.NON_ELEGGIBILE)) return false;
    // CRPT non può prendi in carico/cambia presa in carico
    if (this.authService.isCrpt() &&
        (stato === StatoAssistito.ELEGGIBILE || stato === StatoAssistito.PRESO_IN_CARICO || stato === StatoAssistito.NON_PRESO_IN_CARICO)) return false;
    return true;
  }

  /** Label del bottone coerente con menu azioni in elenco */
  getValutaButtonLabel(): string {
    switch (this.assistito()?.stato) {
      case StatoAssistito.NON_ELEGGIBILE:      return 'Cambia valutazione';
      case StatoAssistito.ELEGGIBILE:          return 'Prendi in carico';
      case StatoAssistito.PRESO_IN_CARICO:
      case StatoAssistito.NON_PRESO_IN_CARICO: return 'Cambia presa in carico';
      case StatoAssistito.AVVIATO_SORV:        return 'Concludi sorveglianza';
      default:                                 return 'Valuta assistito';
    }
  }

  apriCambioStato(): void {
    this.statoScelto.reset();
    this.notaCambioStato.reset();
    this.showStatoPanel.set(true);
  }

  private getValutaMode(stato: StatoAssistito): ValutaAssistitoMode {
    switch (stato) {
      case StatoAssistito.NON_ELEGGIBILE:      return 'cambia-valutazione';
      case StatoAssistito.ELEGGIBILE:          return 'prendi-in-carico';
      case StatoAssistito.PRESO_IN_CARICO:
      case StatoAssistito.NON_PRESO_IN_CARICO: return 'cambia-presa-in-carico';
      case StatoAssistito.AVVIATO_SORV:        return 'valuta';
      default:                                 return 'valuta';
    }
  }

  private statoIdToEnum(id: number): StatoAssistito | undefined {
    const map: Record<number, StatoAssistito> = {
      1:  StatoAssistito.DA_VALUTARE,
      2:  StatoAssistito.ELEGGIBILE,
      3:  StatoAssistito.NON_ELEGGIBILE,
      4:  StatoAssistito.PRESO_IN_CARICO,
      7:  StatoAssistito.CONCLU_SORV,
      11: StatoAssistito.NON_PRESO_IN_CARICO,
    };
    return map[id];
  }

  apriValutaDialog(): void {
    const assistito = this.assistito();
    if (!assistito) return;
    // Per stati con un'unica transizione disponibile usa il pannello inline invece del dialog
    if (assistito.stato === StatoAssistito.AVVIATO_SORV) {
      this.apriCambioStato();
      return;
    }
    const mode = this.getValutaMode(assistito.stato);
    const dialogRef = this.dialog.open(ValutaAssistitoDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      disableClose: false,
      data: { assistito, mode }
    });
    dialogRef.afterClosed().subscribe((result: ValutaAssistitoDialogResult | null) => {
      if (!result) return;
      this.isSaving.set(true);
      this.assistitiService.valutaAssistito(assistito.id!, result.statoId, result.nota).subscribe({
        next: () => {
          const nuovoStato = this.statoIdToEnum(result.statoId);
          if (nuovoStato) this.assistito.set({ ...assistito, stato: nuovoStato });
          this.isSaving.set(false);
          this.loadStoriaStati(Number(assistito.id));
          this.showSuccess('Stato aggiornato con successo');
        },
        error: () => {
          this.showError('Errore durante il cambio di stato');
          this.isSaving.set(false);
        }
      });
    });
  }

  chiudiCambioStato(): void {
    this.showStatoPanel.set(false);
    this.statoScelto.reset();
    this.notaCambioStato.reset();
  }

  apriConcludiSorveglianza(): void {
    const assistito = this.assistito();
    if (!assistito) return;
    const dialogRef = this.dialog.open(ValutaAssistitoDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      disableClose: false,
      data: { assistito, mode: 'concludi-sorveglianza' }
    });
    dialogRef.afterClosed().subscribe((result: ValutaAssistitoDialogResult | null) => {
      if (!result) return;
      this.isSaving.set(true);
      this.assistitiService.valutaAssistito(assistito.id!, result.statoId, result.nota).subscribe({
        next: () => {
          this.assistito.set({ ...assistito, stato: StatoAssistito.CONCLU_SORV });
          this.isSaving.set(false);
          this.loadStoriaStati(Number(assistito.id));
          this.showSuccess('Sorveglianza conclusa con successo');
        },
        error: () => {
          this.showError('Errore durante la conclusione della sorveglianza');
          this.isSaving.set(false);
        }
      });
    });
  }

  eseguiCambioStato(): void {
    const statoId = this.statoScelto.value;
    if (!statoId) {
      this.showError('Selezionare il nuovo stato');
      return;
    }
    const nota = this.notaCambioStato.value?.trim() || '';
    if (this.isNotaObbligatoria && !nota) {
      this.showError('La nota è obbligatoria per questo stato');
      return;
    }
    const assistito = this.assistito();
    if (!assistito?.id) return;

    this.isSaving.set(true);
    this.assistitiService.valutaAssistito(assistito.id, statoId, nota).subscribe({
      next: () => {
        const nuovoStato = this.statiDisponibili.find(t => t.id === statoId)!.stato;
        this.assistito.set({ ...assistito, stato: nuovoStato });
        this.isSaving.set(false);
        this.chiudiCambioStato();
        this.loadStoriaStati(Number(assistito.id));
        this.showSuccess('Stato aggiornato con successo');
      },
      error: (err) => {
        console.error('Errore cambio stato:', err);
        this.showError('Errore durante il cambio di stato');
        this.isSaving.set(false);
      }
    });
  }

  // ============================================
  // TABS
  // ============================================

  onTabChange(index: number): void {
    this.activeTab.set(index);
  }

  private computeInitialTab(): void {
    const a = this.assistito();
    const hasSpresalFonte = a?.storicoFonti?.some(f =>
      f.descrizione?.toUpperCase().includes('SPRESAL')
    ) ?? a?.fonteProvenienza === FonteProvenienza.SPRESAL;

    // Ordine tab: 0=PREADESIONE, 1=SEGNALAZIONE MMG, 2=INAIL, 3=SPRESAL Anamnesi, 4=SPRESAL Esiti, 5=NPLA
    const checks: (() => boolean)[] = [
      () => this.hasPreadesione(),
      () => this.isSegnalazioneMMG(),
      () => this.inailRecords().length > 0,
      () => this.anamnesiSpresal().length > 0,
      () => !!hasSpresalFonte,
      () => this.nplaRecords().length > 0,
    ];
    const firstPopulated = checks.findIndex(check => check());
    this.activeTab.set(firstPopulated >= 0 ? firstPopulated : 0);
  }

  // ============================================
  // HELPERS
  // ============================================

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

  isPreadesione(): boolean {
    return this.assistito()?.fonteProvenienza === FonteProvenienza.PREADESIONI;
  }

  hasPreadesione(): boolean {
    const a = this.assistito();
    if (!a) return false;
    // Mostra il tab preadesione se è la fonte principale OPPURE se è nelle fonti storiche
    if (a.fonteProvenienza === FonteProvenienza.PREADESIONI) return true;
    const hasPreadesioneInStorico = a.storicoFonti?.some(f =>
      f.descrizione?.toLowerCase().includes('preadesion')
    ) ?? false;
    const hasPreAdesioneData = !!(
      this.preAdesioneData().telefono ||
      this.preAdesioneData().email ||
      this.preAdesioneData().dataPreAdesione ||
      this.preAdesioneData().codiceAdesione
    );
    return hasPreadesioneInStorico || hasPreAdesioneData;
  }

  isSegnalazioneMMG(): boolean {
    const a = this.assistito();
    if (!a) return false;
    if (a.fonteProvenienza === FonteProvenienza.SEGNALAZIONE_MMG) return true;
    // Controlla lo storico fonti (in caso la fonte primaria sia cambiata dopo import SPRESAL)
    const inStorico = a.storicoFonti?.some(f =>
      f.descrizione?.toLowerCase().includes('segnalazione') ||
      f.descrizione?.toLowerCase().includes('mmg')
    ) ?? false;
    if (inStorico) return true;
    // Fallback: il campo dataPresentazioneIstanza è esclusivo della Segnalazione MMG
    return !!a.dataPresentazioneIstanza;
  }

  getSezioneLabel(sezione: number | string | null | undefined): string {
    const map: Record<string, string> = { '1': 'Prima', '2': 'Seconda', '3': 'Terza' };
    return sezione != null ? (map[String(sezione)] ?? String(sezione)) : '-';
  }

  getFontiAssociate(): string {
    const a = this.assistito();
    if (!a) return '-';
    if (a.storicoFonti?.length) {
      const normalize = (d: string) => d?.toUpperCase() === 'SPRESAL' ? 'SPRESAL' : d;
      return [...new Set(a.storicoFonti.map(f => normalize(f.descrizione)))].join(', ');
    }
    return this.getSourceLabel(a.fonteProvenienza);
  }

  getSourceLabel(fonte: FonteProvenienza | string): string {
    const labelMap: Record<string, string> = {
      [FonteProvenienza.SEGNALAZIONE_MMG]: 'Segnalazione MMG',
      [FonteProvenienza.PREADESIONI]: 'Preadesioni',
      [FonteProvenienza.INAIL]: 'INAIL',
      [FonteProvenienza.NPLA]: 'NPLA',
      [FonteProvenienza.SPRESAL]: 'SPRESAL'
    };
    return labelMap[fonte] || fonte || '-';
  }

  // ============================================
  // MASKING HELPERS
  // ============================================

  v(key: string, value: unknown): string {
    const m = this.campiAnon();
    if (m) {
      const mappedKey = this.FIELD_MAP[key] ?? key;
      if (mappedKey in m) {
        const mv = m[mappedKey];
        if (mv === null || mv === undefined) return '-';
        return mv;
      }
    }
    if (value === null || value === undefined || value === '') return '-';
    return String(value);
  }

  isMasked(key: string): boolean {
    const m = this.campiAnon();
    if (!m) return false;
    const mappedKey = this.FIELD_MAP[key] ?? key;
    return m[mappedKey] === '*****';
  }

  // ============================================
  // CRPT
  // ============================================

  apriEditCrpt(a: SpresalAnamnesiRecord): void {
    const current = this.expandedCrptId();
    this.expandedCrptId.set(current === a.regSpresalAnamnesiId ? null : a.regSpresalAnamnesiId ?? null);
  }

  onCrptSaved(result: CrptDialogResult): void {
    this.anamnesiSpresal.update(list => list.map(r =>
      r.regSpresalAnamnesiId === result.regSpresalAnamnesiId ? result : r
    ));
    this.expandedCrptId.set(null);
    this.showSuccess('Dati CRPT salvati con successo');
  }

  // ============================================
  // COUNSELING
  // ============================================

  startEditCounseling(): void {
    const current = this.anamnesiSpresal()[0]?.counseling;
    this.counselingEditValue.set(current === null || current === undefined ? 'null' : String(current));
    this.isEditingCounseling.set(true);
  }

  cancelEditCounseling(): void {
    this.isEditingCounseling.set(false);
  }

  saveCounseling(): void {
    const record = this.anamnesiSpresal()[0];
    if (!record?.regSpresalAnamnesiId) return;
    const raw = this.counselingEditValue();
    const strValue: string | null = raw === 'null' ? null : raw;
    this.isSavingCounseling.set(true);
    this.spresalApi.aggiornaCounseling(record.regSpresalAnamnesiId, strValue).subscribe({
      next: () => {
        this.anamnesiSpresal.update(list =>
          list.map(r => r.regSpresalAnamnesiId === record.regSpresalAnamnesiId
            ? { ...r, counseling: strValue }
            : r
          )
        );
        this.isEditingCounseling.set(false);
        this.isSavingCounseling.set(false);
        this.showSuccess('Counseling aggiornato con successo');
      },
      error: () => {
        this.isSavingCounseling.set(false);
        this.showError('Errore durante il salvataggio del counseling');
      }
    });
  }

  // ============================================
  // STORIA PROFESSIONALE
  // ============================================

  downloadStoriaProfessionale(nomeFile: string): void {
    const cf = this.assistito()?.codiceFiscale ?? this.assistitoId();
    this.auditApi.salva('read', 'ANAGRAFICA', `Dettaglio assistito - visualizza storia professionale - ${cf} - ${nomeFile}`).subscribe();
    this.fileApi.downloadPdfByName(nomeFile).subscribe();
  }

  eliminaStoriaProfessionale(percorsoAssoluto: string): void {
    this.fileApi.eliminaPdf(percorsoAssoluto).subscribe({
      next: (resp) => {
        if (resp.successo) {
          this.storiaProfFiles.update(list => list.filter(f => f.percorsoAssoluto !== percorsoAssoluto));
          this.showSuccess('File eliminato');
        }
      },
      error: () => this.showError('Errore durante l\'eliminazione del file')
    });
  }

  // ============================================
  // NOTIFICATIONS
  // ============================================

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Chiudi', { duration: 3000, panelClass: ['snackbar-success'] });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Chiudi', { duration: 5000, panelClass: ['snackbar-error'] });
  }
}
