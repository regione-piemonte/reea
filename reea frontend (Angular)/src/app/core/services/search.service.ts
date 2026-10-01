import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, catchError, shareReplay } from 'rxjs/operators';
import { ParametroDTO, ParametroTipo } from '../models';
import { ParametriApiService, AslApiService } from '../../api';

/**
 * Interfaccia per le opzioni delle dropdown di ricerca
 */
export interface SearchOption {
  value: string;
  label: string;
}

/**
 * Service per le dropdown di ricerca (stati, fonti, ASL, ecc.)
 * e per i pulsanti della homepage
 */
@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private readonly parametriApi = inject(ParametriApiService);
  private readonly aslApi = inject(AslApiService);

  // Cache dei parametri per evitare chiamate multiple
  private parametriCache$: Observable<ParametroDTO[]> | null = null;

  /**
   * Ottiene tutti i parametri dal backend (con cache)
   */
  private getParametri(): Observable<ParametroDTO[]> {
    if (!this.parametriCache$) {
      this.parametriCache$ = this.parametriApi.getListaParametri().pipe(
        shareReplay(1),
        catchError(err => {
          console.error('Errore nel caricamento parametri:', err);
          this.parametriCache$ = null;
          return of([]);
        })
      );
    }
    return this.parametriCache$;
  }

  /**
   * Ottiene i pulsanti della homepage dal backend
   * Restituisce direttamente i ParametroDTO filtrati per tipo PULSANTI_HOMEPAGE
   */
  getPulsantiHomepage(): Observable<ParametroDTO[]> {
    return this.getParametri().pipe(
      map(parametri => parametri.filter(p => p.parametro_tipo_id === ParametroTipo.PULSANTI_HOMEPAGE)),
      catchError(() => of([]))
    );
  }

  /**
   * Ottiene il testo del disclaimer trattamento dati (parametro_id=10)
   * Campo parametro_valore dalla tabella reea_c_parametro
   */
  getTestoDatiPersonali(): Observable<string> {
    return this.getParametri().pipe(
      map(parametri => {
        const p = parametri.find(x => x.parametro_id === 10);
        return p?.parametro_valore ?? 'Autorizzo al trattamento dati';
      }),
      catchError(() => of('Autorizzo al trattamento dati'))
    );
  }

  /**
   * Ottiene gli stati disponibili (fallback hardcoded)
   */
  getStati(): Observable<SearchOption[]> {
    return of(this.getMockStati());
  }

  /**
   * Ottiene le fonti di provenienza (fallback hardcoded)
   */
  getFontiProvenienza(): Observable<SearchOption[]> {
    return of(this.getFonti());
  }

  /**
   * Ottiene le ASL per il filtro Elenco Assistiti
   * Regioni '010' (Piemonte) + '999' (soggetti senza ASL da AURA)
   */
  getAsl(): Observable<SearchOption[]> {
    return this.aslApi.getListaFiltroAssistiti().pipe(
      map(lista => lista.map(a => ({
        value: String(a.asl_id),
        label: a.asl_azienda_desc
      }))),
      catchError(() => of([]))
    );
  }


  /**
   * Invalida la cache dei parametri (utile dopo modifiche)
   */
  invalidateCache(): void {
    this.parametriCache$ = null;
  }

  // ============================================
  // FALLBACK DATA - Per dropdown filtri
  // ============================================

  /**
   * Stati assistito secondo tabella reea_d_soggetto_stato
   * Usa soggetto_stato_id come value
   */
  private getMockStati(): SearchOption[] {
    return [
      { value: '1', label: 'Da valutare' },
      { value: '2', label: 'Eleggibile' },
      { value: '3', label: 'Non eleggibile' },
      { value: '4', label: 'Preso in carico' },
      { value: '5', label: 'Avviato alla sorveglianza' },
      { value: '6', label: 'Escluso dalla sorveglianza' },
      { value: '7', label: 'Sorveglianza conclusa' },
      { value: '8', label: 'Escluso per decesso' },
      { value: '9', label: 'Escluso per emigrazione' },
      { value: '10', label: 'Caricato' },
      { value: '11', label: 'Non preso in carico' }
    ];
  }

  /**
   * Fonti di provenienza secondo tabella reea_d_fonte
   * Usa fonte_id come value
   */
  private getFonti(): SearchOption[] {
    return [
      { value: '1', label: 'Segnalazione MMG' },
      { value: '2', label: 'Preadesioni' },
      { value: '3', label: 'INAIL' },
      { value: '4', label: 'NPLA' },
      { value: '5', label: 'SPRESAL' }
    ];
  }

}
