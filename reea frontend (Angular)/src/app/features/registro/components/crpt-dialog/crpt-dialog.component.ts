import { Component, Input, Output, EventEmitter, OnInit, signal, inject, computed, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';

import { SpresalApiService, CrptDizionarioItem } from '../../../../api/spresal-api.service';
import { AuthService } from '@core/services';
import { SpresalAnamnesiRecord } from '@core/models';

export interface CrptDialogData {
  anamnesi: SpresalAnamnesiRecord;
}

export type CrptDialogResult = SpresalAnamnesiRecord;

@Component({
  selector: 'app-crpt-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './crpt-dialog.component.html',
  styleUrl: './crpt-dialog.component.scss'
})
export class CrptDialogComponent implements OnInit {

  @Input() anamnesi!: SpresalAnamnesiRecord;
  @Input() campiAnon: Record<string, string | null> | null = null;
  @Output() saved = new EventEmitter<CrptDialogResult>();

  private spresalApi = inject(SpresalApiService);
  private elRef = inject(ElementRef);
  private authService = inject(AuthService);

  isPseudo = this.authService.isPseudo;

  editing = signal<boolean>(false);
  saving = signal<boolean>(false);
  loadingDizionari = signal<boolean>(false);
  erroreEsposizione = signal<string>('');

  // Popup state — mansione/settore
  mansionePopupOpen = signal<boolean>(false);
  settorePopupOpen  = signal<boolean>(false);
  mansioneSearch    = signal<string>('');
  settoreSearch     = signal<string>('');

  // Autocomplete state — ragione sociale / piva / codice fiscale ditta
  rsDropdownOpen   = signal<boolean>(false);
  pivaDropdownOpen = signal<boolean>(false);
  cfDropdownOpen   = signal<boolean>(false);
  rsQuery          = signal<string>('');
  pivaQuery        = signal<string>('');
  cfQuery          = signal<string>('');
  rsDisplayValue   = signal<string>('');
  pivaDisplayValue = signal<string>('');
  cfDisplayValue   = signal<string>('');

  // Traccia se il valore è stato selezionato dalla lista o inserito a mano
  rsFromList   = false;
  pivaFromList = false;
  cfFromList   = false;

  // Errori di validazione
  rsError   = signal<string>('');
  pivaError = signal<string>('');
  cfError   = signal<string>('');

  settoreList: CrptDizionarioItem[] = [];
  mansioneList: CrptDizionarioItem[] = [];
  ragioneSocialeList: CrptDizionarioItem[] = [];
  pivaList: CrptDizionarioItem[] = [];
  cfList: CrptDizionarioItem[] = [];

  anyDropdownOpen = computed(() =>
    this.settorePopupOpen() ||
    this.mansionePopupOpen() ||
    (this.rsDropdownOpen()   && this.filteredRS().length   > 0) ||
    (this.pivaDropdownOpen() && this.filteredPiva().length > 0) ||
    (this.cfDropdownOpen()   && this.filteredCf().length   > 0)
  );

  // Filtered lists — popup mansione/settore
  filteredMansione = computed(() => {
    const q = this.mansioneSearch().toLowerCase().trim();
    if (!q) return this.mansioneList;
    return this.mansioneList.filter(i =>
      i.cod.toLowerCase().includes(q) || i.desc.toLowerCase().includes(q)
    );
  });

  filteredSettore = computed(() => {
    const q = this.settoreSearch().toLowerCase().trim();
    if (!q) return this.settoreList;
    return this.settoreList.filter(i =>
      i.cod.toLowerCase().includes(q) || i.desc.toLowerCase().includes(q)
    );
  });

  // Autocomplete RS — la suggest lavora su ragionesociale (cod)
  filteredRS = computed(() => {
    const q = this.rsQuery().toLowerCase().trim();
    if (q.length < 3) return [];
    return this.ragioneSocialeList.filter(i =>
      i.cod.toLowerCase().includes(q)
    ).slice(0, 20);
  });

  // Autocomplete PIVA — la suggest lavora su piva (cod)
  filteredPiva = computed(() => {
    const q = this.pivaQuery().toLowerCase().trim();
    if (q.length < 3) return [];
    return this.pivaList.filter(i =>
      i.cod.toLowerCase().includes(q)
    ).slice(0, 20);
  });

  // Autocomplete CF ditta — la suggest lavora su codice_fiscale (cod)
  filteredCf = computed(() => {
    const q = this.cfQuery().toLowerCase().trim();
    if (q.length < 3) return [];
    return this.cfList.filter(i =>
      i.cod.toLowerCase().includes(q)
    ).slice(0, 20);
  });

  form = {
    occupazioneEsposizioneCrpt: null as boolean | null,
    occupazioneSettoreDittaCrpt: '' as string | null,
    occupazioneMansioneCrpt: '' as string | null,
    occupazioneRagioneSocialeDittaCrpt: '' as string | null,
    occupazionePivaDittaCrpt: '' as string | null,
    occupazioneCodiceFiscaleDittaCrpt: '' as string | null
  };

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elRef.nativeElement.contains(event.target)) {
      this.mansionePopupOpen.set(false);
      this.settorePopupOpen.set(false);
      this.rsDropdownOpen.set(false);
      this.pivaDropdownOpen.set(false);
      this.cfDropdownOpen.set(false);
    }
  }

  ngOnInit(): void {
    const pivaRaw = this.anamnesi.occupazionePivaDittaCrpt;
    const cfRaw   = this.anamnesi.occupazioneCodiceFiscaleDittaCrpt;

    this.form = {
      occupazioneEsposizioneCrpt: this.anamnesi.occupazioneEsposizioneCrpt ?? null,
      occupazioneSettoreDittaCrpt: this.anamnesi.occupazioneSettoreDittaCrpt ?? null,
      occupazioneMansioneCrpt: this.anamnesi.occupazioneMansioneCrpt ?? null,
      occupazioneRagioneSocialeDittaCrpt: this.anamnesi.occupazioneRagioneSocialeDittaCrpt ?? null,
      occupazionePivaDittaCrpt: pivaRaw === 'Non definito' ? null : (pivaRaw ?? null),
      occupazioneCodiceFiscaleDittaCrpt: cfRaw === 'Non definito' ? null : (cfRaw ?? null)
    };

    // RS, PIVA e CF salvano direttamente la stringa (non un codice numerico)
    this.rsDisplayValue.set(this.form.occupazioneRagioneSocialeDittaCrpt ?? '');
    this.pivaDisplayValue.set(this.form.occupazionePivaDittaCrpt ?? '');
    this.cfDisplayValue.set(this.form.occupazioneCodiceFiscaleDittaCrpt ?? '');

    this.loadingDizionari.set(true);
    forkJoin({
      settore: this.spresalApi.getDizionarioSettore(),
      mansione: this.spresalApi.getDizionarioMansione(),
      ragioneSociale: this.spresalApi.getDizionarioRagioneSociale()
    }).subscribe({
      next: (res) => {
        this.settoreList = res.settore;
        this.mansioneList = res.mansione;
        this.ragioneSocialeList = res.ragioneSociale;
        // pivaList: cod=piva, desc=ragionesociale, ditta_codice_fiscale=cf (per display)
        this.pivaList = res.ragioneSociale
          .filter(i => i.cod?.trim() && i.desc?.trim())
          .map(i => ({
            cod: i.desc!.trim(),
            desc: i.cod!.trim(),
            ditta_codice_fiscale: i.ditta_codice_fiscale?.trim() || undefined
          }));
        // cfList: cod=cf, desc=ragionesociale, ditta_codice_fiscale=piva (per display)
        this.cfList = res.ragioneSociale
          .filter(i => i.ditta_codice_fiscale?.trim())
          .map(i => ({
            cod: i.ditta_codice_fiscale!.trim(),
            desc: i.cod?.trim() ?? '',
            ditta_codice_fiscale: i.desc?.trim() || undefined
          }));

this.loadingDizionari.set(false);

        // Verifica se i valori attuali sono presenti in lista
        const rsVal = this.form.occupazioneRagioneSocialeDittaCrpt?.trim();
        this.rsFromList = rsVal
          ? this.ragioneSocialeList.some(i => i.cod?.trim() === rsVal)
          : false;

        const pivaVal = this.form.occupazionePivaDittaCrpt?.trim();
        this.pivaFromList = pivaVal
          ? this.pivaList.some(i => i.cod?.trim() === pivaVal)
          : false;
      },
      error: () => this.loadingDizionari.set(false)
    });
  }

  getLabel(list: CrptDizionarioItem[], cod: string | null | undefined): string {
    if (!cod) return '-';
    const item = list.find(i => i.cod === cod);
    return item ? `${item.cod} - ${item.desc}` : cod;
  }

  maskedVal(backendKey: string, rawValue: string | null | undefined): string {
    if (this.campiAnon) {
      const mv = this.campiAnon[backendKey];
      if (mv !== undefined) return mv ?? '-';
    }
    return rawValue || '-';
  }

  // Popup mansione
  toggleMansionePopup(event: MouseEvent): void {
    event.stopPropagation();
    const open = !this.mansionePopupOpen();
    this.settorePopupOpen.set(false);
    this.mansioneSearch.set('');
    this.mansionePopupOpen.set(open);
  }

  selectMansione(item: CrptDizionarioItem | null): void {
    this.form.occupazioneMansioneCrpt = item ? item.cod : null;
    this.mansionePopupOpen.set(false);
  }

  // Autocomplete ragione sociale
  // Lista RS: { cod: ragionesociale, desc: piva }
  onRsInput(val: string): void {
    this.rsDisplayValue.set(val);
    this.form.occupazioneRagioneSocialeDittaCrpt = val || null;
    this.rsQuery.set(val);
    this.rsDropdownOpen.set(val.length >= 3);
    this.rsFromList = false;
    this.rsError.set('');
  }

  selectRS(item: CrptDizionarioItem): void {
    // cod = ragionesociale, desc = piva, ditta_codice_fiscale = cf
    this.rsDisplayValue.set(item.cod);
    this.form.occupazioneRagioneSocialeDittaCrpt = item.cod;
    this.rsFromList = true;
    this.rsError.set('');

    // Cross-populate PIVA
    this.pivaDisplayValue.set(item.desc);
    this.form.occupazionePivaDittaCrpt = item.desc;
    this.pivaFromList = true;
    this.pivaError.set('');

    // Cross-populate CF ditta
    if (item.ditta_codice_fiscale) {
      this.cfDisplayValue.set(item.ditta_codice_fiscale);
      this.form.occupazioneCodiceFiscaleDittaCrpt = item.ditta_codice_fiscale;
      this.cfFromList = true;
      this.cfError.set('');
    }

    this.rsDropdownOpen.set(false);
    this.rsQuery.set('');
  }

  clearRS(): void {
    this.rsDisplayValue.set('');
    this.form.occupazioneRagioneSocialeDittaCrpt = null;
    this.rsFromList = false;
    this.rsDropdownOpen.set(false);
    this.rsError.set('');
  }

  // Autocomplete PIVA
  // Lista PIVA: { cod: piva, desc: ragionesociale }
  onPivaKeydown(event: KeyboardEvent): void {
    const allowed = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Enter'];
    if (!allowed.includes(event.key) && !/^\d$/.test(event.key)) {
      event.preventDefault();
    }
  }

  onPivaInput(val: string): void {
    const numericVal = val.replace(/\D/g, '');
    this.pivaDisplayValue.set(numericVal);
    this.form.occupazionePivaDittaCrpt = numericVal || null;
    this.pivaQuery.set(numericVal);
    this.pivaDropdownOpen.set(numericVal.length >= 3);
    this.pivaFromList = false;
    if (numericVal.length > 0 && numericVal.length !== 11) {
      this.pivaError.set('La Partita IVA deve essere di esattamente 11 cifre');
    } else {
      this.pivaError.set('');
    }
  }

  selectPiva(item: CrptDizionarioItem): void {
    // cod = piva, desc = ragionesociale
    this.pivaDisplayValue.set(item.cod);
    this.form.occupazionePivaDittaCrpt = item.cod;
    this.pivaFromList = true;
    this.pivaError.set('');

    // Cross-populate RS
    this.rsDisplayValue.set(item.desc);
    this.form.occupazioneRagioneSocialeDittaCrpt = item.desc;
    this.rsFromList = true;
    this.rsError.set('');

    // Cross-populate CF
    if (item.ditta_codice_fiscale) {
      this.cfDisplayValue.set(item.ditta_codice_fiscale);
      this.form.occupazioneCodiceFiscaleDittaCrpt = item.ditta_codice_fiscale;
      this.cfFromList = true;
      this.cfError.set('');
    }

    this.pivaDropdownOpen.set(false);
    this.pivaQuery.set('');
  }

  clearPiva(): void {
    this.pivaDisplayValue.set('');
    this.form.occupazionePivaDittaCrpt = null;
    this.pivaFromList = false;
    this.pivaDropdownOpen.set(false);
    this.pivaError.set('');
  }

  // Autocomplete Codice Fiscale ditta
  onCfInput(val: string): void {
    const upper = val.toUpperCase().replace(/[^A-Z0-9]/g, '');
    this.cfDisplayValue.set(upper);
    this.form.occupazioneCodiceFiscaleDittaCrpt = upper || null;
    this.cfQuery.set(upper);
    this.cfDropdownOpen.set(upper.length >= 3);
    this.cfFromList = false;
    if (upper.length > 0 && upper.length !== 16) {
      this.cfError.set('Il Codice Fiscale deve essere di esattamente 16 caratteri');
    } else {
      this.cfError.set('');
    }
  }

  selectCf(item: CrptDizionarioItem): void {
    this.cfDisplayValue.set(item.cod);
    this.form.occupazioneCodiceFiscaleDittaCrpt = item.cod;
    this.cfFromList = true;
    this.cfError.set('');

    // Cross-populate RS e PIVA: item.desc = ragionesociale
    const rs = item.desc;
    if (rs) {
      this.rsDisplayValue.set(rs);
      this.form.occupazioneRagioneSocialeDittaCrpt = rs;
      this.rsFromList = true;
      this.rsError.set('');
      const rsItem = this.ragioneSocialeList.find(i => i.cod?.trim() === rs.trim());
      if (rsItem?.desc) {
        this.pivaDisplayValue.set(rsItem.desc);
        this.form.occupazionePivaDittaCrpt = rsItem.desc;
        this.pivaFromList = true;
        this.pivaError.set('');
      }
    }

    this.cfDropdownOpen.set(false);
    this.cfQuery.set('');
  }

  clearCf(): void {
    this.cfDisplayValue.set('');
    this.form.occupazioneCodiceFiscaleDittaCrpt = null;
    this.cfFromList = false;
    this.cfDropdownOpen.set(false);
    this.cfError.set('');
  }

  // Popup settore
  toggleSettorePopup(event: MouseEvent): void {
    event.stopPropagation();
    const open = !this.settorePopupOpen();
    this.mansionePopupOpen.set(false);
    this.settoreSearch.set('');
    this.settorePopupOpen.set(open);
  }

  selectSettore(item: CrptDizionarioItem | null): void {
    this.form.occupazioneSettoreDittaCrpt = item ? item.cod : null;
    this.settorePopupOpen.set(false);
  }

  abilitaModifica(): void {
    this.editing.set(true);
  }

  annulla(): void {
    this.editing.set(false);
    this.mansionePopupOpen.set(false);
    this.settorePopupOpen.set(false);
    this.rsDropdownOpen.set(false);
    this.pivaDropdownOpen.set(false);
    this.cfDropdownOpen.set(false);
    this.rsError.set('');
    this.pivaError.set('');
    this.cfError.set('');
  }

  private validateRsPiva(): boolean {
    const pivaVal = (this.form.occupazionePivaDittaCrpt ?? '').trim();
    if (pivaVal.length > 0 && pivaVal.length !== 11) {
      this.pivaError.set('La Partita IVA deve essere di esattamente 11 cifre');
      return false;
    }
    return true;
  }

  altriCampiAbilitati(): boolean {
    return this.form.occupazioneEsposizioneCrpt === true;
  }

  salva(): void {
    if (!this.anamnesi.regSpresalAnamnesiId) return;

    this.rsError.set('');
    this.pivaError.set('');
    this.erroreEsposizione.set('');

    // Blocco SI → NO
    if (this.anamnesi.occupazioneEsposizioneCrpt === true && this.form.occupazioneEsposizioneCrpt !== true) {
      this.erroreEsposizione.set('Non è possibile modificare Esposizione da SI a NO. Per questa operazione contattare il supporto per un trattamento dati ad hoc.');
      return;
    }

    if (!this.validateRsPiva()) return;

    const doSave = () => {
      this.saving.set(true);
      this.spresalApi.aggiornaCrpt(this.anamnesi.regSpresalAnamnesiId!, {
        occupazione_esposizione_crpt: this.form.occupazioneEsposizioneCrpt,
        occupazione_settore_ditta_crpt: this.form.occupazioneSettoreDittaCrpt,
        occupazione_mansione_crpt: this.form.occupazioneMansioneCrpt,
        occupazione_ragione_sociale_ditta_crpt: this.form.occupazioneRagioneSocialeDittaCrpt,
        occupazione_piva_ditta_crpt: this.form.occupazionePivaDittaCrpt,
        occupazione_codice_fiscale_ditta_crpt: this.form.occupazioneCodiceFiscaleDittaCrpt
      }).subscribe({
        next: () => {
          const updated: SpresalAnamnesiRecord = {
            ...this.anamnesi,
            occupazioneEsposizioneCrpt: this.form.occupazioneEsposizioneCrpt ?? undefined,
            occupazioneSettoreDittaCrpt: this.form.occupazioneSettoreDittaCrpt || undefined,
            occupazioneMansioneCrpt: this.form.occupazioneMansioneCrpt || undefined,
            occupazioneRagioneSocialeDittaCrpt: this.form.occupazioneRagioneSocialeDittaCrpt || undefined,
            occupazionePivaDittaCrpt: this.form.occupazionePivaDittaCrpt || undefined,
            occupazioneCodiceFiscaleDittaCrpt: this.form.occupazioneCodiceFiscaleDittaCrpt || undefined
          };
          this.saving.set(false);
          this.editing.set(false);
          this.saved.emit(updated);
        },
        error: () => {
          this.saving.set(false);
        }
      });
    };

    doSave();
  }
}
