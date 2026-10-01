import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { API_ENDPOINTS } from './api.config';
import { AuthService } from '../core/services/auth.service';
import { AuditApiService } from './audit-api.service';

export interface NotaDTO {
  nota_id: number;
  registro_id: number;
  descrizione: string;
  data_modifica: string;
  utente_modifica: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotaApiService {

  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly auditApi = inject(AuditApiService);

  getNote(soggettoId: number): Observable<NotaDTO[]> {
    return this.http.get<NotaDTO[]>(API_ENDPOINTS.note.getByRegistroId(soggettoId)).pipe(
      catchError(() => of([]))
    );
  }

  inserisci(soggettoId: number, descrizione: string): Observable<NotaDTO> {
    const formData = new FormData();
    formData.append('soggettoId', soggettoId.toString());
    formData.append('descrizione', descrizione);
    formData.append('auditLogRequest', new Blob([JSON.stringify(
      this.authService.buildAuditLog('create', 'NOTA', `Inserimento nota soggetto ${soggettoId}`)
    )], { type: 'application/json' }));
    return this.http.post<NotaDTO>(API_ENDPOINTS.note.inserisci, formData).pipe(
      tap(() => this.auditApi.salva('create', 'NOTA', `Inserimento nota soggetto ${soggettoId}`).subscribe()),
      catchError(() => of({} as NotaDTO))
    );
  }

  modifica(notaId: number, descrizione: string): Observable<NotaDTO> {
    const formData = new FormData();
    formData.append('descrizione', descrizione);
    formData.append('auditLogRequest', new Blob([JSON.stringify(
      this.authService.buildAuditLog('update', 'NOTA', `Modifica nota ${notaId}`)
    )], { type: 'application/json' }));
    return this.http.put<NotaDTO>(API_ENDPOINTS.note.modifica(notaId), formData).pipe(
      tap(() => this.auditApi.salva('update', 'NOTA', `Modifica nota ${notaId}`).subscribe()),
      catchError(() => of({} as NotaDTO))
    );
  }

  elimina(notaId: number): Observable<void> {
    return this.http.delete<void>(API_ENDPOINTS.note.elimina(notaId)).pipe(
      tap(() => this.auditApi.salva('delete', 'NOTA', `Eliminazione nota ${notaId}`).subscribe())
    );
  }
}
