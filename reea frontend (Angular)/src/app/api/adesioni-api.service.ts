import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpParams, HttpProgressEvent, HttpResponse } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { API_ENDPOINTS } from './api.config';
import { FileCaricato } from '../core/models';
import { AuthService } from '../core/services/auth.service';

/**
 * Risposta dell'import Excel
 * Il BE restituisce { file_id: X } nel body per permettere il filtraggio in anteprima
 */
export interface ImportResult {
  success: boolean;
  fileId?: number;
  recordElaborati?: number;
  recordScartati?: number;
  errori?: string[];
}

/**
 * DTO per le adesioni da file (staging table REEA_T_ADESIONE)
 * Usato per: Preadesione, INAIL A, INAIL B, SPRESAL
 * I campi "*_cifrato" contengono testo in chiaro nell'import da file
 */
export interface AdesioneDTO {
  adesione_id?: number;
  adesione_cod?: string;
  codice_fiscale?: string;
  cognome_cifrato?: string;
  nome_cifrato?: string;
  cognome?: string;
  nome?: string;
  nascita_data?: string;
  nascita_provincia_desc?: string;
  nascita_comune_desc?: string;
  tessera_team?: string;
  id_aura?: string;
  domicilio_provincia_desc?: string;
  domicilio_comune_desc?: string;
  domicilio_comune_cod?: string;
  domicilio_cap?: string;
  domicilio_asl_desc?: string;
  domicilio_asl_cod?: string;
  residenza_asl_desc?: string;
  residenza_asl_cod?: string;
  email_cifrata?: string;
  email?: string;
  telefono_cifrato?: string;
  telefono?: string;
  adesione_data?: string;
  azienda_cod?: string;
}

/**
 * DTO per i record NPLA (Notifica Piano Lavori Amianto)
 * Staging table: REEA_T_REGISTRO_PDL_AMIANTO
 */
export interface NplaDTO {
  reg_pdl_amianto_id?: number;
  codice_fiscale?: string;
  periodo?: number;
  id_cantiere?: number;
  azienda_piva?: string;
  azienda_nome?: string;
  asl_cantiere?: string;
  comune_cantiere?: string;
  anno?: number;
  tipologia_piano?: string;
  quantita_da_rimuovere?: number;
  quantita_rimossa?: number;
  validita_inizio?: string;
  validita_fine?: string;
}

/**
 * Risposta dell'inserimento massivo NPLA
 * Contiene i CF non trovati in AURA (import parziale)
 */
export interface NplaImportResult {
  cfNonTrovatiInAura: string[];
}

/**
 * DTO per i record INAIL A (staging table REEA_T_REGISTRO_INAIL)
 */
export interface InailDTO {
  reg_inail_id?: number;
  registro_id?: number;
  domanda?: number;
  cognome?: string;
  nome?: string;
  codice_fiscale?: string;
  sesso?: string;
  data_nascita?: string;
  indirizzo_residenza?: string;
  cap_residenza?: number;
  istat_residenza?: number;
  comune_residenza?: string;
  provincia_residenza?: string;
  regione_residenza?: string;
  validita_inizio?: string;
  validita_fine?: string;
}

/**
 * DTO per i record SPRESAL (staging table REEA_T_REGISTRO_SPRESAL_ANAMNESI)
 */
export interface SpresalDTO {
  reg_spresal_id?: number;
  registro_id?: number;
  codice_fiscale?: string;
  data_intervista?: string;
  nominativo_intervistatore?: string;
  esposizione_professionale?: string;
  livello_esposizione?: string;
  inserimento_in_sorveglianza?: boolean;
  counseling?: boolean | null;
}

/**
 * DTO per i record SPRESAL Esiti nella staging table (anteprima prima dell'inserimento massivo)
 */
export interface SpresalEsitiStagingDTO {
  reg_spresal_esiti_id?: number;
  registro_id?: number;
  codice_fiscale?: string;
  data_visita?: string;
  visita?: string;
  livello_visita?: string;
  riceve_indennizzo?: boolean;
  malattia_indennizzo?: string;
  follow_up_previsto?: boolean;
  accertamenti_rx_acquisita?: boolean | null;
  accertamenti_tc_acquisita?: boolean | null;
  accertamenti_spirometria_semplice_acquisita?: boolean | null;
  accertamenti_spirometria_globale_acquisita?: boolean | null;
  accertamenti_dlco_acquisita?: boolean | null;
  accertamenti_pet_acquisita?: boolean | null;
  accertamenti_visita_pneumologica_acquisita?: boolean | null;
  accertamenti_visita_radiologica_acquisita?: boolean | null;
  accertamenti_visita_oncologica_acquisita?: boolean | null;
  accertamenti_altro_referto_acquisita?: boolean | null;
}

/**
 * Servizio API per le adesioni (import Excel)
 */
@Injectable({
  providedIn: 'root'
})
export class AdesioniApiService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  private auditPart(oggOper: string): Blob {
    return new Blob(
      [JSON.stringify(this.authService.buildAuditLog('IMPORT EXCEL', oggOper))],
      { type: 'application/json' }
    );
  }

  /**
   * Importa un file Excel di adesioni
   * POST /api/adesioni/import
   * IMPORTANTE: Non settare Content-Type, il browser lo gestisce automaticamente con FormData
   * @param file - File Excel da importare
   * @returns Observable con il risultato dell'import
   */
  importExcel(file: File): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('auditLogRequest', this.auditPart('PREADESIONE'));

    // NON passare headers con Content-Type! Il browser gestisce automaticamente
    // il Content-Type: multipart/form-data con il boundary corretto
    return this.http.post<{ file_id?: number }>(API_ENDPOINTS.adesioni.import, formData, {
      observe: 'response'
    }).pipe(
      map((response: HttpResponse<{ file_id?: number }>) => ({
        success: response.ok,
        fileId: response.body?.file_id ?? undefined,
      })),
      catchError((error) => {
        console.error('Errore import Excel:', error);
        return of({
          success: false,
          errori: [error.message || 'Errore durante l\'importazione']
        });
      })
    );
  }

  /**
   * Importa un file Excel con tracking del progresso
   * Utile per file grandi
   */
  importExcelWithProgress(file: File): Observable<{
    type: 'progress' | 'complete' | 'error';
    progress?: number;
    result?: ImportResult;
    error?: string;
  }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('auditLogRequest', this.auditPart('PREADESIONE'));

    return this.http.post<void>(API_ENDPOINTS.adesioni.import, formData, {
      reportProgress: true,
      observe: 'events'
    }).pipe(
      map((event: HttpEvent<void>) => {
        if (event.type === HttpEventType.UploadProgress) {
          const progressEvent = event as HttpProgressEvent;
          const progress = progressEvent.total
            ? Math.round(100 * progressEvent.loaded / progressEvent.total)
            : 0;
          return { type: 'progress' as const, progress };
        } else if (event.type === HttpEventType.Response) {
          return {
            type: 'complete' as const,
            result: { success: true }
          };
        }
        return { type: 'progress' as const, progress: 0 };
      }),
      catchError((error) => {
        return of({
          type: 'error' as const,
          error: error.message || 'Errore durante l\'importazione'
        });
      })
    );
  }

  /**
   * Recupera i record nella staging table dopo l'import
   * GET /api/adesioni/getRecordTabAdesioni?file_id=X
   */
  getRecordTabAdesioni(fileId?: number): Observable<AdesioneDTO[]> {
    const params = fileId != null ? { file_id: String(fileId) } : {};
    return this.http.get<AdesioneDTO[]>(API_ENDPOINTS.adesioni.getRecordTabAdesioni, { params });
  }

  /**
   * Recupera i dati di adesione associati a un soggetto (dopo inserimento massivo)
   * GET /api/adesioni/getBySoggettoId/{soggettoId}
   */
  getAdesioneBySoggettoId(soggettoId: number): Observable<AdesioneDTO[]> {
    return this.http.get<AdesioneDTO[]>(API_ENDPOINTS.adesioni.getBySoggettoId(soggettoId));
  }

  /**
   * Avvia l'inserimento massivo dei record dalla staging table in anagrafica
   * GET /api/adesioni/inserimentiMassiviFileAdesioni
   */
  inserimentiMassiviFileAdesioni(): Observable<void> {
    return this.http.get<void>(API_ENDPOINTS.adesioni.inserimentiMassiviFileAdesioni);
  }

  /**
   * Ottiene la lista dei file importati (adesioni)
   * NOTA: Endpoint non ancora implementato nel backend
   */
  getListaFileImportati(): Observable<FileCaricato[]> {
    return of([]);
  }

  /**
   * Scarica l'Excel delle preadesioni filtrate per lista di soggettoId
   * POST /api/adesioni/scaricaDatiExcelPreadesioniFiltrate
   */
  scaricaDatiExcelPreadesioniFiltrate(soggettoIds: number[], assistenzaAslId: string = ''): Observable<Blob> {
    let params = new HttpParams();
    if (assistenzaAslId) {
      params = params.set('assistenza_asl_id', assistenzaAslId);
    }
    const formData = new FormData();
    formData.append('soggettoIds', new Blob([JSON.stringify(soggettoIds)], { type: 'application/json' }));
    formData.append('auditLogRequest', this.auditPart('excel preadesioni'));
    return this.http.post(
      API_ENDPOINTS.adesioni.scaricaDatiExcelPreadesioniFiltrate,
      formData,
      { params, responseType: 'blob' }
    );
  }

  // ============================================
  // NPLA
  // ============================================

  /**
   * Importa un file Excel NPLA
   * POST /api/npla/import
   */
  importNpla(file: File): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('auditLogRequest', this.auditPart('NPLA'));
    return this.http.post<{ file_id?: number }>(API_ENDPOINTS.npla.import, formData, { observe: 'response' }).pipe(
       map((response: HttpResponse<{ file_id?: number }>) => ({ success: response.ok, fileId: response.body?.file_id ?? undefined })),
      catchError((error) => of({ success: false, errori: [error.message || 'Errore importazione NPLA'] }))
    );
  }

  /**
   * Recupera i record NPLA dalla staging table
   * GET /api/npla/getRecordTabNpla?file_id=X
   */
  getRecordTabNpla(fileId?: number): Observable<NplaDTO[]> {
    const params = fileId != null ? { file_id: String(fileId) } : {};
    return this.http.get<NplaDTO[]>(API_ENDPOINTS.npla.getRecordTabNpla, { params });
  }

  /**
   * Avvia l'inserimento massivo dei record NPLA in anagrafica
   * GET /api/npla/inserimentiMassiviNpla
   * Restituisce i CF non trovati in AURA (avvisi parziali)
   */
  inserimentiMassiviNpla(): Observable<NplaImportResult> {
    return this.http.get<NplaImportResult>(API_ENDPOINTS.npla.inserimentiMassiviNpla);
  }

  // ============================================
  // INAIL A
  // ============================================

  /** POST /api/inail/import/inailA */
  importInailA(file: File): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('tipologiaInail', 'A');
    formData.append('auditLogRequest', this.auditPart('INAIL'));
    return this.http.post<{ file_id?: number }>(API_ENDPOINTS.inail.importInailA, formData, { observe: 'response' }).pipe(
      map((response: HttpResponse<{ file_id?: number }>) => ({ success: response.ok, fileId: response.body?.file_id ?? undefined })),
      catchError((error) => of({ success: false, errori: [error.message || 'Errore importazione INAIL A'] }))
    );
  }

  /** GET /api/inail/getRecordTabInail?tipologiaInail=A&file_id=X */
  getRecordTabInail(fileId?: number): Observable<InailDTO[]> {
    const params: Record<string, string> = { tipologiaInail: 'A' };
    if (fileId != null) params['file_id'] = String(fileId);
    return this.http.get<InailDTO[]>(API_ENDPOINTS.inail.getRecordTabInail, { params });
  }

  /** GET /api/inail/inserimentiMassiviFileInailA */
  inserimentiMassiviFileInailA(): Observable<void> {
    return this.http.get<void>(API_ENDPOINTS.inail.inserimentiMassiviFileInailA);
  }

  // ============================================
  // INAIL B
  // ============================================

  /** POST /api/inail/import/inailB */
  importInailB(file: File): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('tipologiaInail', 'B');
    formData.append('auditLogRequest', this.auditPart('INAIL'));
    return this.http.post<{ file_id?: number }>(API_ENDPOINTS.inail.importInailB, formData, { observe: 'response' }).pipe(
      map((response: HttpResponse<{ file_id?: number }>) => ({ success: response.ok, fileId: response.body?.file_id ?? undefined })),
      catchError((error) => of({ success: false, errori: [error.message || 'Errore importazione INAIL B'] }))
    );
  }

  /** GET /api/inail/getRecordTabInailB?tipologiaInail=B&file_id=X */
  getRecordTabInailB(fileId?: number): Observable<InailDTO[]> {
    const params: Record<string, string> = { tipologiaInail: 'B' };
    if (fileId != null) params['file_id'] = String(fileId);
    return this.http.get<InailDTO[]>(API_ENDPOINTS.inail.getRecordTabInailB, { params });
  }

  /** GET /api/inail/inserimentiMassiviFileInailB  */
  inserimentiMassiviFileInailB(): Observable<void> {
    return this.http.get<void>(API_ENDPOINTS.inail.inserimentiMassiviFileInailB);
  }

  // ============================================
  // SPRESAL
  // ============================================

  /** POST /api/spresal/import */
  importSpresal(file: File): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('auditLogRequest', this.auditPart('SPRESAL'));
    return this.http.post<{ file_id?: number }>(API_ENDPOINTS.spresal.import, formData, { observe: 'response' }).pipe(
       map((response: HttpResponse<{ file_id?: number }>) => ({ success: response.ok, fileId: response.body?.file_id ?? undefined })),
      catchError((error) => of({ success: false, errori: [error.message || 'Errore importazione SPRESAL'] }))
    );
  }

  /** POST /api/spresal/import/esiti */
  importSpresalEsiti(file: File): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('auditLogRequest', this.auditPart('SPRESAL ESITI'));
    return this.http.post<{ file_id?: number }>(API_ENDPOINTS.spresal.importEsiti, formData, { observe: 'response' }).pipe(
      map((response: HttpResponse<{ file_id?: number }>) => ({ success: response.ok, fileId: response.body?.file_id ?? undefined })),
      catchError((error) => of({ success: false, errori: [error.message || 'Errore importazione SPRESAL Esiti'] }))
    );
  }

  /** GET /api/spresal/getRecordTabSpresal?file_id=X */
  getRecordTabSpresal(fileId?: number): Observable<SpresalDTO[]> {
    const params = fileId != null ? { file_id: String(fileId) } : {};
    return this.http.get<SpresalDTO[]>(API_ENDPOINTS.spresal.getRecordTabSpresal, { params });
  }

  /** GET /api/spresal/inserimentiMassiviFileSpresalAnamnesi */
  inserimentiMassiviFileSpresal(): Observable<void> {
    return this.http.get<void>(API_ENDPOINTS.spresal.inserimentiMassiviFileSpresal);
  }

  /** GET /api/spresal/getRecordTabSpresalEsiti?file_id=X */
  getRecordTabSpresalEsiti(fileId?: number): Observable<SpresalEsitiStagingDTO[]> {
    const params = fileId != null ? { file_id: String(fileId) } : {};
    return this.http.get<SpresalEsitiStagingDTO[]>(API_ENDPOINTS.spresal.getRecordTabSpresalEsiti, { params });
  }

  /** GET /api/spresal/inserimentiMassiviFileSpresalEsiti */
  inserimentiMassiviFileSpresalEsiti(): Observable<void> {
    return this.http.get<void>(API_ENDPOINTS.spresal.inserimentiMassiviFileSpresalEsiti);
  }

}
