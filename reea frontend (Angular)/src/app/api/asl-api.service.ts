import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from './api.config';

/**
 * DTO per le ASL - campi snake_case come restituiti da jOOQ
 * Endpoint: GET /api/asl/getListaASLCompetenza
 * NOTE: asl_cod deve essere restituito dal BE - necessario per AnagraficaDTO.aslDomicilioCod/residenzaAslCod
 */
export interface AslDTO {
  asl_id: number;
  asl_azienda_desc: string;
  asl_cod?: string | null;
}

/**
 * Servizio API per le ASL
 * Controller: AslController - /api/asl
 */
@Injectable({
  providedIn: 'root'
})
export class AslApiService {
  private readonly http = inject(HttpClient);

  /**
   * Ottiene la lista delle ASL di competenza (solo Piemonte, ASL_REGIONE_COD = '010')
   * GET /api/asl/getListaASLCompetenza
   */
  getListaCompetenza(): Observable<AslDTO[]> {
    return this.http.get<AslDTO[]>(API_ENDPOINTS.asl.getListaCompetenza);
  }

  /**
   * Ottiene la lista delle ASL per il filtro Elenco Assistiti
   * Include Piemonte (ASL_REGIONE_COD='010') + ASL '999' (soggetti senza ASL da AURA)
   * GET /api/asl/getListaFiltroAssistiti
   */
  getListaFiltroAssistiti(): Observable<AslDTO[]> {
    return this.http.get<AslDTO[]>(API_ENDPOINTS.asl.getListaFiltroAssistiti);
  }
}
