import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from './api.config';

/**
 * DTO per le nazioni - campi snake_case come restituiti da jOOQ
 * Endpoint: GET /api/nazioni/getListaStatoNascitaResidenza
 */
export interface NazioneDTO {
  nazione_desc_it: string;
  nazione_istat_cod: string;
}

/**
 * Servizio API per le nazioni
 * Controller: NazioneController - /api/nazioni
 */
@Injectable({
  providedIn: 'root'
})
export class NazioneApiService {
  private readonly http = inject(HttpClient);

  /**
   * Ottiene la lista degli stati per nascita/residenza/domicilio
   * GET /api/nazioni/getListaStatoNascitaResidenza
   */
  getLista(): Observable<NazioneDTO[]> {
    return this.http.get<NazioneDTO[]>(API_ENDPOINTS.nazioni.getLista);
  }
}
