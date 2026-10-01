import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, of } from 'rxjs';
import { API_ENDPOINTS } from './api.config';
import { AuditLogRequest } from '../core/models';
import { AuthService } from '../core/services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuditApiService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  salva(operazione: string, oggOper: string, keyOper: string): Observable<number | null> {
    const auditLog: AuditLogRequest = this.authService.buildAuditLog(operazione, oggOper, keyOper);
    const formData = new FormData();
    formData.append(
      'auditLogRequest',
      new Blob([JSON.stringify(auditLog)], { type: 'application/json' })
    );
    return this.http.post<number>(API_ENDPOINTS.audit.salva, formData, { withCredentials: true }).pipe(
      catchError(() => of(null))
    );
  }
}
