import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { API_ENDPOINTS } from './api.config';
import { SpresalEsitiDTO } from '@core/models';
import { AuthService } from '../core/services/auth.service';

export interface CrptDizionarioItem {
  cod: string;
  desc: string;
  ditta_codice_fiscale?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SpresalApiService {

  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  getEsiti(registroId: number): Observable<SpresalEsitiDTO[]> {
    return this.http.get<SpresalEsitiDTO[]>(API_ENDPOINTS.spresal.getEsitiByRegistroId(registroId)).pipe(
      catchError(() => of([]))
    );
  }

  getDizionarioSettore(): Observable<CrptDizionarioItem[]> {
    return this.http.get<CrptDizionarioItem[]>(API_ENDPOINTS.spresal.dizionarioSettore).pipe(catchError(() => of([])));
  }

  getDizionarioMansione(): Observable<CrptDizionarioItem[]> {
    return this.http.get<CrptDizionarioItem[]>(API_ENDPOINTS.spresal.dizionarioMansione).pipe(catchError(() => of([])));
  }

  getDizionarioRagioneSociale(): Observable<CrptDizionarioItem[]> {
    return this.http.get<CrptDizionarioItem[]>(API_ENDPOINTS.spresal.dizionarioRagioneSociale).pipe(catchError(() => of([])));
  }

  // La PIVA è ricavata dalla stessa lista ragionesociale (campo desc), nessun endpoint separato
  getDizionarioPiva(): Observable<CrptDizionarioItem[]> {
    return of([]);
  }

  saveRagioneSocialeDecodifica(ragionesociale: string, piva: string): Observable<void> {
    return this.http.post<void>(API_ENDPOINTS.spresal.saveRagioneSocialeDecodifica, { ragionesociale, piva }).pipe(
      catchError(() => of(undefined as any))
    );
  }

  aggiornaPrestazioneAcquisita(esitoId: number, field: string, value: boolean | null): Observable<void> {
    const formData = new FormData();
    formData.append('body', new Blob([JSON.stringify({ field, value })], { type: 'application/json' }));
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('write', 'ANAGRAFICA', `aggiornaPrestazioneAcquisita - ${esitoId}`))], { type: 'application/json' }));
    return this.http.put<void>(API_ENDPOINTS.spresal.aggiornaPrestazioneAcquisita(esitoId), formData);
  }

  aggiornaCounseling(regSpresalAnamnesiId: number, counseling: string | null): Observable<void> {
    const formData = new FormData();
    formData.append('body', new Blob([JSON.stringify({ counseling })], { type: 'application/json' }));
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('write', 'ANAGRAFICA', `aggiornaCounseling - ${regSpresalAnamnesiId}`))], { type: 'application/json' }));
    return this.http.put<void>(API_ENDPOINTS.spresal.aggiornaCounseling(regSpresalAnamnesiId), formData);
  }

  aggiornaCrpt(regSpresalAnamnesiId: number, data: {
    occupazione_esposizione_crpt: boolean | null;
    occupazione_settore_ditta_crpt: string | null;
    occupazione_mansione_crpt: string | null;
    occupazione_ragione_sociale_ditta_crpt: string | null;
    occupazione_piva_ditta_crpt: string | null;
    occupazione_codice_fiscale_ditta_crpt: string | null;
  }): Observable<void> {
    const formData = new FormData();
    formData.append('body', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('write', 'ANAGRAFICA', `aggiornaCrpt - ${regSpresalAnamnesiId}`))], { type: 'application/json' }));
    return this.http.put<void>(API_ENDPOINTS.spresal.aggiornaCrpt(regSpresalAnamnesiId), formData);
  }
}
