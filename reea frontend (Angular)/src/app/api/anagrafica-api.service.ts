import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { RigaListaAnagraficaDTO, AnagraficaCreateDTO, StoriaStato } from '../core/models';
import { API_ENDPOINTS } from './api.config';
import { AuthService } from '../core/services/auth.service';

/**
 * Parametri filtro per la lista anagrafiche (formato atteso dal backend POST)
 */
export interface AnagraficaFiltriApi {
  filtroFonteId?: number[] | null;
  descrizioniStato?: string[] | null;
  sezione?: number[] | null;
  insInSorveglianza?: boolean | null;
  tipoElencoInail?: string | null;
  assistenzaAslId?: number[] | null;
  codiceFiscale?: string | null;
  cognome?: string | null;
  nome?: string | null;
  nascitaData?: string | null;
  cognomeLettDa?: string | null;
  cognomeLettA?: string | null;
}

/**
 * Parametri per la ricerca anagrafica
 * Endpoint: GET /api/anagrafica/getByCForNomeorCognomeorDataNascita
 */
export interface AnagraficaSearchParams {
  codice_fiscale?: string;
  nome?: string;
  cognome?: string;
  nascita_data?: string; // formato yyyy-MM-dd
}

/**
 * Servizio API per le anagrafiche
 * Controller: AnagraficaController - /api/anagrafica
 */
@Injectable({
  providedIn: 'root'
})
export class AnagraficaApiService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  /**
   * Ottiene la lista di anagrafiche con filtri opzionali
   * POST /api/anagrafica/getListaAnagrafiche
   */
  getLista(filtri?: AnagraficaFiltriApi, azzeraContatore: boolean | null = null): Observable<{ records: RigaListaAnagraficaDTO[]; totalRecords: number }> {
    const keyOper = this.buildKeyOper(filtri);
    const body: Record<string, unknown> = {
      filtroFonteId: filtri?.filtroFonteId ?? null,
      descrizioniStato: filtri?.descrizioniStato ?? null,
      sezione: filtri?.sezione ?? null,
      insInSorveglianza: filtri?.insInSorveglianza ?? null,
      tipoElencoInail: filtri?.tipoElencoInail ?? null,
      assistenzaAslId: filtri?.assistenzaAslId ?? null,
      codiceFiscale: filtri?.codiceFiscale?.trim().toUpperCase() || null,
      cognome: filtri?.cognome?.trim().toUpperCase() || null,
      nome: filtri?.nome?.trim().toUpperCase() || null,
      nascitaData: filtri?.nascitaData || null,
      cognomeLettDa: filtri?.cognomeLettDa?.trim().toUpperCase() || null,
      cognomeLettA: filtri?.cognomeLettA?.trim().toUpperCase() || null,
      profiloUtente: this.authService.currentUser()?.ruolo ?? null,
      auditLogRequest: this.authService.buildAuditLog('read', 'ANAGRAFICA', keyOper)
    };
    if (azzeraContatore !== null) {
      body['azzeraContatoreProssimoStep'] = azzeraContatore;
    }
    return this.http.post<{ records: RigaListaAnagraficaDTO[]; totalRecords: number } | RigaListaAnagraficaDTO[]>(
      API_ENDPOINTS.anagrafica.getLista, body
    ).pipe(
      map(res => Array.isArray(res)
        ? { records: res, totalRecords: res.length }
        : { records: (res as any).records ?? [], totalRecords: (res as any).totalRecords ?? 0 }
      )
    );
  }

  /**
   * Ottiene un'anagrafica per ID
   * GET /api/anagrafica/getById/{soggettoId}
   */
  getById(soggettoId: number): Observable<RigaListaAnagraficaDTO> {
    return this.http.get<RigaListaAnagraficaDTO>(API_ENDPOINTS.anagrafica.getById(soggettoId));
  }

  getByIdCampiAnonimizzati(soggettoId: number, flagCampiAnonimi: boolean): Observable<Record<string, string | null>> {
    return this.http.get<Record<string, string | null>>(API_ENDPOINTS.anagrafica.getByIdCampiAnonimizzati(soggettoId, flagCampiAnonimi));
  }

  /**
   * Crea una nuova anagrafica (inserimento manuale da UI)
   * POST /api/anagrafica/creazioneAnagrafica?flagCaricamentoDaFile=false
   * @returns soggettoId del nuovo record creato
   */
  crea(anagrafica: AnagraficaCreateDTO): Observable<number> {
    const formData = new FormData();
    formData.append('anagraficaDTO', new Blob([JSON.stringify(anagrafica)], { type: 'application/json' }));
    formData.append('flagCaricamentoDaFile', new Blob(['false'], { type: 'application/json' }));
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('write', 'ANAGRAFICA', 'inserisci assistito'))], { type: 'application/json' }));
    return this.http.post<number>(API_ENDPOINTS.anagrafica.crea, formData);
  }

  /**
   * Modifica un'anagrafica esistente
   * POST /api/anagrafica/modifica/{soggettoId}
   */
  modifica(soggettoId: number, anagrafica: Partial<AnagraficaCreateDTO>): Observable<RigaListaAnagraficaDTO> {
    const formData = new FormData();
    formData.append('anagraficaDTO', new Blob([JSON.stringify(anagrafica)], { type: 'application/json' }));
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('write', 'ANAGRAFICA', `modifica - ${soggettoId}`))], { type: 'application/json' }));
    return this.http.post<RigaListaAnagraficaDTO>(API_ENDPOINTS.anagrafica.modifica(soggettoId), formData);
  }

  /**
   * Aggiorna lo stato di un soggetto con nota opzionale (valutazione).
   * POST /api/anagrafica/aggiornaStato/{soggettoId}
   */
  aggiornaStato(soggettoId: number, body: { soggetto_stato_id: number; soggetto_stato_note: string }, keyOper = '', oggOper = 'Selezione assistito'): Observable<void> {
    const formData = new FormData();
    formData.append('anagraficaDTO', new Blob([JSON.stringify(body)], { type: 'application/json' }));
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('Selezione assistito', oggOper, keyOper))], { type: 'application/json' }));
    return this.http.post<void>(API_ENDPOINTS.anagrafica.aggiornaStato(soggettoId), formData);
  }

  /**
   * Ottiene la storia degli stati di un soggetto da reea_s_soggetto
   * GET /api/anagrafica/storiaStati/{soggettoId}
   */
  getStoriaStati(soggettoId: number): Observable<StoriaStato[]> {
    return this.http.get<StoriaStato[]>(API_ENDPOINTS.anagrafica.storiaStati(soggettoId));
  }

  /**
   * Cerca anagrafiche per codice fiscale, nome, cognome o data nascita
   * GET /api/anagrafica/getByCForNomeorCognomeorDataNascita
   */
  search(params: AnagraficaSearchParams): Observable<RigaListaAnagraficaDTO[]> {
    let httpParams: Record<string, string> = {};
    if (params.codice_fiscale) httpParams['codice_fiscale'] = params.codice_fiscale.toUpperCase();
    if (params.nome)           httpParams['nome']           = params.nome.toUpperCase();
    if (params.cognome)        httpParams['cognome']        = params.cognome.toUpperCase();
    if (params.nascita_data)   httpParams['nascita_data']   = params.nascita_data;

    return this.http.get<RigaListaAnagraficaDTO[]>(API_ENDPOINTS.anagrafica.search, { params: httpParams });
  }

  private buildKeyOper(filtri?: AnagraficaFiltriApi): string {
    const parts: string[] = [];
    if (filtri?.filtroFonteId?.length)   parts.push(`fonti=${filtri.filtroFonteId.join(',')}`);
    if (filtri?.descrizioniStato?.length) parts.push(`stati=${filtri.descrizioniStato.join(',')}`);
    if (filtri?.sezione?.length)          parts.push(`sezioni=${filtri.sezione.join(',')}`);
    if (filtri?.assistenzaAslId?.length)  parts.push(`asl=${filtri.assistenzaAslId.join(',')}`);
    return parts.join(';') || 'all';
  }
}
