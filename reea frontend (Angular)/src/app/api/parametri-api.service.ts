import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ParametroDTO } from '../core/models';
import { API_ENDPOINTS } from './api.config';

/**
 * Servizio API per i parametri di configurazione
 * Endpoint: GET /api/parametri/getListaParametri
 */
@Injectable({
  providedIn: 'root'
})
export class ParametriApiService {
  private readonly http = inject(HttpClient);

  /**
   * Ottiene la lista di tutti i parametri
   * GET /api/getListaParametri
   */
  getListaParametri(): Observable<ParametroDTO[]> {
    return this.http.get<ParametroDTO[]>(API_ENDPOINTS.parametri.getLista);
  }

  getValore(cod: string): Observable<string> {
    return this.http.get(API_ENDPOINTS.parametri.getValore(encodeURIComponent(cod)), {
      responseType: 'text',
      withCredentials: true
    });
  }
}
