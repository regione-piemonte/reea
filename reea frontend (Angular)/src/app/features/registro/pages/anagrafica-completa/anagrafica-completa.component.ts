// ============================================
// ANAGRAFICA COMPLETA COMPONENT
// ============================================

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AppHeaderComponent } from '@shared/components/app-header/app-header.component';
import { StatusBadgeComponent } from '@shared/components/status-badge/status-badge.component';

import { HttpClient } from '@angular/common/http';
import { AnagraficaApiService } from '../../../../api/anagrafica-api.service';
import { AslApiService, AslDTO, API_ENDPOINTS, NazioneApiService, NazioneDTO, ProvinciaApiService, ProvinciaDTO, ComuneApiService, ComuneDTO } from '../../../../api';
import { EsposizioneBackendDTO, EsenzioneBackendDTO, RigaListaAnagraficaDTO, StatoAssistito } from '@core/models';
import { AuraService, AuthService } from '@core/services';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-anagrafica-completa',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    AppHeaderComponent,
    StatusBadgeComponent
  ],
  templateUrl: './anagrafica-completa.component.html',
  styleUrl: './anagrafica-completa.component.scss'
})
export class AnagraficaCompletaComponent implements OnInit {

  private route        = inject(ActivatedRoute);
  private router       = inject(Router);
  private anagraficaApi = inject(AnagraficaApiService);
  private aslApi       = inject(AslApiService);
  private nazioneApi   = inject(NazioneApiService);
  private provinciaApi = inject(ProvinciaApiService);
  private comuneApi    = inject(ComuneApiService);
  private http         = inject(HttpClient);
  private auraService  = inject(AuraService);
  private authService  = inject(AuthService);
  private fb           = inject(FormBuilder);
  private snackBar     = inject(MatSnackBar);

  private readonly FIELD_MAP: Record<string, string> = {
    codice_fiscale:             'aura_codice_fiscale',
    id_aura:                    'aura_id_aura',
    cognome:                    'aura_cognome',
    nome:                       'aura_nome',
    sesso:                      'aura_sesso',
    nascita_data:               'aura_nascita_data',
    nascita_comune_desc:        'aura_nascita_comune_desc',
    nascita_provincia_desc:     'aura_nascita_provincia_desc',
    nascita_stato_desc:         'aura_nascita_stato_desc',
    tessera_team:               'aura_tessera_team',
    descrizione_asl_competenza: 'adesione_asl_di_domicilio',
    data_decesso:               'aura_data_decesso',
    assistenza_asl_fine:        'aura_assistenza_asl_fine',
    residenza_stato_desc:       'aura_residenza_stato_desc',
    residenza_provincia_desc:   'aura_residenza_provincia_desc',
    residenza_comune_desc:      'aura_residenza_comune_desc',
    residenza_comune_cod:       'aura_residenza_comune_cod',
    residenza_indirizzo:        'aura_residenza_indirizzo',
    residenza_numero_civico:    'aura_residenza_numero_civico',
    residenza_cap:              'aura_residenza_cap',
    residenza_asl_desc:         'adesione_asl_di_residenza',
    domicilio_stato_desc:       'aura_domicilio_stato_desc',
    domicilio_provincia_desc:   'aura_domicilio_provincia_desc',
    domicilio_comune_desc:      'aura_domicilio_comune_desc',
    domicilio_comune_cod:       'aura_domicilio_comune_cod',
    domicilio_indirizzo:        'aura_domicilio_indirizzo',
    domicilio_numero_civico:    'aura_domicilio_numero_civico',
    domicilio_cap:              'aura_domicilio_cap',
    domicilio_asl_desc:         'adesione_asl_di_domicilio',
    telefono:                   'soggetto_telefono',
    telefono_aura:              'aura_telefono_aura',
    email:                      'soggetto_email',
    email_aura:                 'aura_email_aura',
  };

  soggettoId  = signal<number>(0);
  dati        = signal<RigaListaAnagraficaDTO | null>(null);
  campiAnon   = signal<Record<string, string | null> | null>(null);
  isPseudo    = this.authService.isPseudo;
  esenzioni   = signal<EsenzioneBackendDTO[]>([]);
  loading    = signal<boolean>(true);
  saving     = signal<boolean>(false);
  editMode   = signal<boolean>(false);
  aslOptions          = signal<AslDTO[]>([]);
  nazioniOptions      = signal<NazioneDTO[]>([]);
  provinceOptions     = signal<ProvinciaDTO[]>([]);
  comuniResidenzaOptions = signal<ComuneDTO[]>([]);
  comuniDomicilioOptions = signal<ComuneDTO[]>([]);
  residenzaEstera    = signal<boolean>(false);
  domicilioEstero    = signal<boolean>(false);

  /** True se l'assistito è ricondotto ad AURA (id_aura valorizzato) */
  isFromAura = computed(() => !!this.dati()?.id_aura);

  editForm: FormGroup = this.fb.group({
    telefono:                  [''],
    email:                     [''],
    residenza_indirizzo:       [''],
    residenza_numero_civico:   [''],
    residenza_cap:             [''],
    residenza_nazione_cod:     [''],
    residenza_provincia_id:    [null as number | null],
    residenza_comune_cod_sel:  [''],
    domicilio_indirizzo:       [''],
    domicilio_numero_civico:   [''],
    domicilio_cap:             [''],
    domicilio_nazione_cod:     [''],
    domicilio_provincia_id:    [null as number | null],
    domicilio_comune_cod_sel:  [''],
    assistenza_asl_id:         [null as number | null],
  });

  esposizioniColumns = ['azienda', 'mansione', 'dataInizio', 'dataFine'];
  inailColumns       = ['tipologia', 'domanda', 'cognome', 'nome', 'cf'];
  nplaColumns        = ['azienda', 'anno', 'periodo', 'tipologia', 'comune', 'daRimuovere', 'rimossa'];
  fontiColumns       = ['fonte', 'data'];

  ngOnInit(): void {
    this.aslApi.getListaFiltroAssistiti().subscribe(lista => this.aslOptions.set(lista));
    this.nazioneApi.getLista().subscribe(n => this.nazioniOptions.set(n));
    this.provinciaApi.getLista().subscribe(p => this.provinceOptions.set(p));

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const numId = Number(id);
      this.soggettoId.set(numId);
      forkJoin({
        dati:      this.anagraficaApi.getById(numId),
        campiAnon: this.authService.isPseudo()
                       ? this.anagraficaApi.getByIdCampiAnonimizzati(numId, true)
                           .pipe(catchError(() => of(null as Record<string, string | null> | null)))
                       : of(null as Record<string, string | null> | null),
        esenzioni: this.http.get<EsenzioneBackendDTO[]>(API_ENDPOINTS.esenzioni.getByIdSoggetto(numId))
                       .pipe(catchError(() => of([] as EsenzioneBackendDTO[])))
      }).subscribe({
        next: ({ dati, campiAnon, esenzioni }) => {
          this.campiAnon.set(campiAnon);
          console.log('[Esenzioni] DB response:', esenzioni);
          console.log('[Esenzioni] dati.lista_esenzione:', dati.lista_esenzione);
          console.log('[Esenzioni] dati.id_aura:', dati.id_aura);
          this.dati.set(dati);

          // 1. Esenzioni da reea_r_soggetto_esenzione (endpoint dedicato)
          if (esenzioni && esenzioni.length > 0) {
            console.log('[Esenzioni] Fonte: DB endpoint');
            this.esenzioni.set(esenzioni);
            this.loading.set(false);
          // 2. Esenzioni già incluse nella risposta getById (lista_esenzione)
          } else if (dati.lista_esenzione && dati.lista_esenzione.length > 0) {
            console.log('[Esenzioni] Fonte: lista_esenzione in getById');
            this.esenzioni.set(dati.lista_esenzione);
            this.loading.set(false);
          // 3. Fallback: carica live da AURA se persona proviene da AURA
          } else if (dati.id_aura) {
            console.log('[Esenzioni] Fonte: AURA live fallback, id_aura=', dati.id_aura);
            this.auraService.getById(dati.id_aura).subscribe({
              next: (auraData) => {
                console.log('[Esenzioni] AURA response esenzioni:', auraData.esenzioni);
                const auraEsenzioni: EsenzioneBackendDTO[] = (auraData.esenzioni ?? []).map(e => ({
                  esenzione_cod:            e.esenzione_cod,
                  esenzione_desc:           e.esenzione_desc,
                  diagnosi_cod:             e.diagnosi_cod ?? undefined,
                  diagnosi_desc:            e.diagnosi_desc ?? undefined,
                  esenzione_data_emissione: e.esenzione_data_emissione ?? undefined,
                  esenzione_data_scadenza:  e.esenzione_data_scadenza ?? undefined,
                }));
                this.esenzioni.set(auraEsenzioni);
                this.loading.set(false);
              },
              error: (err) => { console.error('[Esenzioni] AURA fallback error:', err); this.loading.set(false); }
            });
          } else {
            console.log('[Esenzioni] Nessuna fonte disponibile');
            this.esenzioni.set([]);
            this.loading.set(false);
          }
        },
        error: (err) => { console.error('[Esenzioni] forkJoin error:', err); this.loading.set(false); }
      });
    }
  }

  tornaAlDettaglio(): void {
    this.router.navigate(['/registro-amianto/assistiti', this.soggettoId()]);
  }

  /** Helper per accedere ai controlli del form con tipo corretto */
  ctrl(name: string): FormControl {
    return this.editForm.get(name) as FormControl;
  }

  attivaModifica(): void {
    const d = this.dati();
    if (!d) return;

    // Trova nazione per residenza (match case-insensitive su desc)
    const rNazione = this.nazioniOptions().find(
      n => n.nazione_desc_it?.toLowerCase() === (d.residenza_stato_desc ?? '').toLowerCase()
    );
    // Trova provincia per residenza: prima da prefisso ISTAT del comune, poi da desc
    const rProv = d.residenza_comune_cod
      ? this.provinceOptions().find(p => p.provincia_cod === d.residenza_comune_cod!.substring(0, 3))
      : this.provinceOptions().find(p => p.provincia_desc?.toLowerCase() === (d.residenza_provincia_desc ?? '').toLowerCase());

    // Trova nazione per domicilio
    const dNazione = this.nazioniOptions().find(
      n => n.nazione_desc_it?.toLowerCase() === (d.domicilio_stato_desc ?? '').toLowerCase()
    );
    // Trova provincia per domicilio
    const dProv = d.domicilio_comune_cod
      ? this.provinceOptions().find(p => p.provincia_cod === d.domicilio_comune_cod!.substring(0, 3))
      : this.provinceOptions().find(p => p.provincia_desc?.toLowerCase() === (d.domicilio_provincia_desc ?? '').toLowerCase());

    this.editForm.patchValue({
      telefono:                 d.telefono               ?? '',
      email:                    d.email                  ?? '',
      residenza_indirizzo:      d.residenza_indirizzo    ?? '',
      residenza_numero_civico:  d.residenza_numero_civico ?? '',
      residenza_cap:            d.residenza_cap          ?? '',
      residenza_nazione_cod:    rNazione?.nazione_istat_cod ?? '',
      residenza_provincia_id:   rProv?.provincia_id ?? null,
      residenza_comune_cod_sel: '',
      domicilio_indirizzo:      d.domicilio_indirizzo    ?? '',
      domicilio_numero_civico:  d.domicilio_numero_civico ?? '',
      domicilio_cap:            d.domicilio_cap          ?? '',
      domicilio_nazione_cod:    dNazione?.nazione_istat_cod ?? '',
      domicilio_provincia_id:   dProv?.provincia_id ?? null,
      domicilio_comune_cod_sel: '',
      assistenza_asl_id:        d.assistenza_asl_id ? Number(d.assistenza_asl_id) : null,
    });

    const isResidenzaEstera = !!rNazione && rNazione.nazione_desc_it?.toLowerCase() !== 'italia';
    const isDomicilioEstero = !!dNazione && dNazione.nazione_desc_it?.toLowerCase() !== 'italia';
    this.residenzaEstera.set(isResidenzaEstera);
    this.domicilioEstero.set(isDomicilioEstero);

    if (!isResidenzaEstera && rProv?.provincia_id) {
      this.comuneApi.getLista(rProv.provincia_id).subscribe(c => {
        this.comuniResidenzaOptions.set(c);
        this.editForm.patchValue({ residenza_comune_cod_sel: d.residenza_comune_cod ?? '' });
      });
    } else if (isResidenzaEstera) {
      this.editForm.patchValue({ residenza_provincia_id: null, residenza_comune_cod_sel: '' });
      this.comuniResidenzaOptions.set([]);
    }

    if (!isDomicilioEstero && dProv?.provincia_id) {
      this.comuneApi.getLista(dProv.provincia_id).subscribe(c => {
        this.comuniDomicilioOptions.set(c);
        this.editForm.patchValue({ domicilio_comune_cod_sel: d.domicilio_comune_cod ?? '' });
      });
    } else if (isDomicilioEstero) {
      this.editForm.patchValue({ domicilio_provincia_id: null, domicilio_comune_cod_sel: '' });
      this.comuniDomicilioOptions.set([]);
    }

    this.editMode.set(true);
  }

  onResidenzaStatoChange(nazioneIstatCod: string): void {
    const nazione = this.nazioniOptions().find(n => n.nazione_istat_cod === nazioneIstatCod);
    const isEstera = !!nazione && nazione.nazione_desc_it?.toLowerCase() !== 'italia';
    this.residenzaEstera.set(isEstera);
    if (isEstera || !nazioneIstatCod) {
      this.editForm.patchValue({ residenza_provincia_id: null, residenza_comune_cod_sel: '' });
      this.comuniResidenzaOptions.set([]);
    }
  }

  onDomicilioStatoChange(nazioneIstatCod: string): void {
    const nazione = this.nazioniOptions().find(n => n.nazione_istat_cod === nazioneIstatCod);
    const isEstera = !!nazione && nazione.nazione_desc_it?.toLowerCase() !== 'italia';
    this.domicilioEstero.set(isEstera);
    if (isEstera || !nazioneIstatCod) {
      this.editForm.patchValue({ domicilio_provincia_id: null, domicilio_comune_cod_sel: '' });
      this.comuniDomicilioOptions.set([]);
    }
  }

  onResidenzaProvinciaChange(provinciaId: number | null): void {
    this.editForm.patchValue({ residenza_comune_cod_sel: '' });
    this.comuniResidenzaOptions.set([]);
    if (provinciaId) {
      this.comuneApi.getLista(provinciaId).subscribe(c => this.comuniResidenzaOptions.set(c));
    }
  }

  onDomicilioProvinciaChange(provinciaId: number | null): void {
    this.editForm.patchValue({ domicilio_comune_cod_sel: '' });
    this.comuniDomicilioOptions.set([]);
    if (provinciaId) {
      this.comuneApi.getLista(provinciaId).subscribe(c => this.comuniDomicilioOptions.set(c));
    }
  }

  annullaModifica(): void {
    this.editMode.set(false);
  }

  salva(): void {
    this.saving.set(true);
    const v = this.editForm.value;

    const payload: Record<string, unknown> = {
      telefono: v.telefono || null,
      email:    v.email    || null,
    };

    if (!this.isFromAura()) {
      const rNazione  = this.nazioniOptions().find(n => n.nazione_istat_cod === v.residenza_nazione_cod);
      const rProvincia = this.provinceOptions().find(p => p.provincia_id === v.residenza_provincia_id);
      const rComune   = this.comuniResidenzaOptions().find(c => c.comune_cod === v.residenza_comune_cod_sel);
      const dNazione  = this.nazioniOptions().find(n => n.nazione_istat_cod === v.domicilio_nazione_cod);
      const dProvincia = this.provinceOptions().find(p => p.provincia_id === v.domicilio_provincia_id);
      const dComune   = this.comuniDomicilioOptions().find(c => c.comune_cod === v.domicilio_comune_cod_sel);

      Object.assign(payload, {
        residenza_indirizzo:      v.residenza_indirizzo     || null,
        residenza_numero_civico:  v.residenza_numero_civico || null,
        residenza_cap:            v.residenza_cap           || null,
        residenza_stato_desc:     rNazione?.nazione_desc_it || null,
        residenza_provincia_desc: rProvincia?.provincia_desc || null,
        residenza_comune_desc:    rComune?.comune_desc || null,
        residenza_comune_cod:     v.residenza_comune_cod_sel || null,
        domicilio_indirizzo:      v.domicilio_indirizzo     || null,
        domicilio_numero_civico:  v.domicilio_numero_civico || null,
        domicilio_cap:            v.domicilio_cap           || null,
        domicilio_stato_desc:     dNazione?.nazione_desc_it || null,
        domicilio_provincia_desc: dProvincia?.provincia_desc || null,
        domicilio_comune_desc:    dComune?.comune_desc || null,
        domicilio_comune_cod:     v.domicilio_comune_cod_sel || null,
        assistenza_asl_id:        v.assistenza_asl_id != null ? String(v.assistenza_asl_id) : (this.dati()?.assistenza_asl_id ?? null),
      });
    }

    this.anagraficaApi.modifica(this.soggettoId(), payload as any).subscribe({
      next: (updated) => {
        this.dati.set(updated);
        this.editMode.set(false);
        this.saving.set(false);
        this.snackBar.open('Dati salvati con successo', 'OK', { duration: 3000 });
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Errore durante il salvataggio', 'Chiudi', { duration: 4000 });
      }
    });
  }

  // ── Helpers di visualizzazione ──────────────────────────────

  val(v: unknown): string {
    if (v === null || v === undefined || v === '') return '-';
    return String(v);
  }

  /** Restituisce il valore mascherato dalla mappa se presente, altrimenti val() */
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
    return this.val(value);
  }

  isMasked(key: string): boolean {
    const m = this.campiAnon();
    if (!m) return false;
    const mappedKey = this.FIELD_MAP[key] ?? key;
    return m[mappedKey] === '*****';
  }

  bool(v: boolean | null | undefined): string {
    if (v === null || v === undefined) return '-';
    return v ? 'SI' : 'NO';
  }

  sezione(v: number | null | undefined): string {
    const map: Record<number, string> = { 1: 'Prima', 2: 'Seconda', 3: 'Terza' };
    return v != null ? (map[v] ?? String(v)) : '-';
  }

  statoAssistito(v: string | undefined): StatoAssistito {
    return (v ?? '') as StatoAssistito;
  }

  fontiList(): string {
    const d = this.dati();
    if (!d?.descrizione_fonte?.length) return '-';
    return d.descrizione_fonte.join(', ');
  }

  nascitaLabel(): string {
    const d = this.dati();
    if (!d) return '-';
    const parts = [d.nascita_comune_desc, d.nascita_provincia_desc].filter(Boolean);
    return parts.length > 0 ? parts.join(' - ') : '-';
  }

  espPeriodo(esp: EsposizioneBackendDTO): string {
    const inizio = esp.esposizione_inizio ? new Date(esp.esposizione_inizio).getFullYear() : '?';
    const fine   = esp.esposizione_fine   ? new Date(esp.esposizione_fine).getFullYear()   : '?';
    return `${inizio} - ${fine}`;
  }
}
