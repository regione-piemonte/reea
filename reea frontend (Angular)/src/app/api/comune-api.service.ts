import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from './api.config';

/**
 * DTO per i comuni - campi snake_case come restituiti dal backend
 * Endpoint: GET /api/comuni/getListaComuneNascitaResidenzaAzienda
 */
export interface ComuneDTO {
  comune_cod: string;
  comune_desc: string;
  comune_cap?: string;
}

/**
 * Servizio API per i comuni
 * Controller: ComuneController - /api/comuni
 */
@Injectable({
  providedIn: 'root'
})
export class ComuneApiService {
  private readonly http = inject(HttpClient);

  /**
   * Ottiene la lista dei comuni per nascita, residenza/domicilio e azienda
   * GET /api/comuni/getListaComuneNascitaResidenzaAzienda
   * @param provinciaId - opzionale: filtra i comuni per provincia
   */
  getLista(provinciaId?: number): Observable<ComuneDTO[]> {
    let params = new HttpParams();

    if (provinciaId !== undefined && provinciaId !== null) {
      params = params.set('provincia_id', provinciaId.toString());
    }

    return this.http.get<ComuneDTO[]>(API_ENDPOINTS.comuni.getLista, { params });
  }
}
