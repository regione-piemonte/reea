import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from './api.config';

/**
 * DTO per le province - campi snake_case come restituiti da jOOQ
 * Endpoint: GET /api/province/getListaProvinciaNascitaResidenza
 */
export interface ProvinciaDTO {
  provincia_id: number;
  provincia_sigla: string;
  provincia_desc: string;
  provincia_cod?: string; // ISTAT code (e.g. "001" for TO) – added after BE openapi.yaml fix
}

/**
 * Servizio API per le province
 * Controller: ProvinciaController - /api/province
 */
@Injectable({
  providedIn: 'root'
})
export class ProvinciaApiService {
  private readonly http = inject(HttpClient);

  /**
   * Ottiene la lista delle province per nascita/residenza/domicilio
   * GET /api/province/getListaProvinciaNascitaResidenza
   */
  getLista(): Observable<ProvinciaDTO[]> {
    return this.http.get<ProvinciaDTO[]>(API_ENDPOINTS.province.getLista);
  }
}
