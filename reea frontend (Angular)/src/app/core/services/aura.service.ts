import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';
import { API_ENDPOINTS } from '../../api/api.config';
import { AuraAssistitoDto } from '../models/aura.model';

@Injectable({ providedIn: 'root' })
export class AuraService {
  private http = inject(HttpClient);

  /**
   * Ricerca per codice fiscale:
   * 1. chiama /api/aura/find?cf=... → ottiene id_aura + dati base
   * 2. con id_aura chiama /api/aura/{idAura} → ottiene dati completi (residenza, ASL, ecc.)
   * Emette errore HTTP se non trovato (404).
   */
  findByCf(cf: string): Observable<AuraAssistitoDto> {
    const params = new HttpParams().set('cf', cf);
    return this.http
      .get<AuraAssistitoDto>(API_ENDPOINTS.aura.find, { params })
      .pipe(
        switchMap((basic) => this.getById(basic.id_aura!))
      );
  }

  /**
   * Ricerca per anagrafica (cognome + nome + data_nascita):
   * Chiama /api/aura/findByAnagrafica?cognome=...&nome=...&data_nascita=...
   * Restituisce una lista (può contenere 0, 1 o più soggetti).
   */
  findByAnagrafica(cognome: string, nome: string, dataNascita: string): Observable<AuraAssistitoDto[]> {
    const params = new HttpParams()
      .set('cognome', cognome)
      .set('nome', nome)
      .set('data_nascita', dataNascita);
    return this.http.get<AuraAssistitoDto[]>(API_ENDPOINTS.aura.findByAnagrafica, { params });
  }

  /**
   * Dettaglio completo per ID AURA.
   */
  getById(idAura: string): Observable<AuraAssistitoDto> {
    return this.http.get<AuraAssistitoDto>(API_ENDPOINTS.aura.getById(idAura));
  }
}
