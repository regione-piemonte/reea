import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from './api.config';

/**
 * DTO per lo storico fonti di un soggetto
 * Endpoint: GET /api/fonte/getListaFonteDescByIdSoggetto/{soggettoId}
 */
export interface FonteConDataDTO {
  fonte_desc: string;
  data_creazione: string;
  /**
   * Data specifica per tipo fonte:
   * - Preadesione       → reea_t_adesione.adesione_data
   * - Segnalazione MMG  → reea_t_soggetto.presentazione_istanza_data
   * - INAIL             → reea_t_registro_inail.data_creazione
   * - NPLA              → reea_t_registro_pdl_amianto.data_creazione
   * - SPRESAL           → reea_t_registro_spresal_anamnesi.data_creazione
   */
  data_fonte?: string | null;
}

/**
 * Servizio API per le fonti
 * Controller: FonteController - /api/fonte
 */
@Injectable({
  providedIn: 'root'
})
export class FonteApiService {
  private readonly http = inject(HttpClient);

  /**
   * Ottiene lo storico delle fonti per un soggetto
   * GET /api/fonte/getListaFonteDescByIdSoggetto/{soggettoId}
   */
  getStoricoFonti(soggettoId: number): Observable<FonteConDataDTO[]> {
    return this.http.get<FonteConDataDTO[]>(API_ENDPOINTS.fonte.getStoricoFonti(soggettoId));
  }
}
