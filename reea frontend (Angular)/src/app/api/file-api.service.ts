import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { API_ENDPOINTS } from './api.config';
import { FileCaricato, FileCaricatoBackendDTO, mapFileCaricato } from '../core/models';

export interface ScartoFileDTO {
  numero_riga?: number;
  target_tabella_nome?: string;
  target_record_id?: number;
  codice_errore?: string;
  descrizione_errore?: string;
  contenuto_riga_raw?: string;
  cognome?: string;
  nome?: string;
  data_nascita?: string;
  sesso?: string;
  codice_fiscale?: string;
  domanda?: string;
  campo_extra?: string;
}

export interface ExportFileListDTO {
  file_id?: number | null;
  elaborazione_id?: number | null;
  nome?: string | null;
  tipo?: 'chiaro' | 'pseudo' | null;
  stato?: string | null;
  data_richiesta?: string | null;
  data_generazione?: string | null;
  operatore?: string | null;
}

export interface ExportAllegato4DTO {
  elaborazione_id?: number | null;
  anno?: number | null;
  file_name?: string | null;
  stato?: string | null;
  data_richiesta?: string | null;
  data_generazione?: string | null;
  operatore?: string | null;
}

export interface ArchivioFileCaricatiDTO {
  elaborazione_id?: number;
  fileId?: number;
  nome_file?: string;
  fonte_provenienza?: string;
  stato?: string;
  record_totali?: number;
  record_elaborati?: number;
  record_scartati?: number;
  righe_errate?: number;
  record_errati?: number;
  scarico_errore_count?: number;
  data_caricamento?: string;
  data_inizio?: string;
  operatore?: string;
}

export interface StoriaProfFileDTO {
  nomeFile: string;
  percorsoAssoluto: string;
  dimensione?: number;
  dataModifica?: string;
}

export interface UploadPdfResponse {
  successo: boolean;
  percorso: string;
  nomeFile: string;
  dimensione: number;
  tipo: string;
}

@Injectable({
  providedIn: 'root'
})
export class FileApiService {

  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  getListaFile(): Observable<FileCaricato[]> {
    return this.http.get<FileCaricatoBackendDTO[]>(API_ENDPOINTS.file.getLista).pipe(
      map(dtos => dtos.map(mapFileCaricato)),
      catchError(err => {
        console.error('Errore caricamento lista file:', err);
        return of([]);
      })
    );
  }

  getScartiFile(elaborazioneId: number): Observable<ScartoFileDTO[]> {
    const formData = new FormData();
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('read', 'ANAGRAFICA', `getElaborazioneErroreFile - ${elaborazioneId}`))], { type: 'application/json' }));
    return this.http.post<ScartoFileDTO[]>(API_ENDPOINTS.archivioFileCaricati.getErrori(elaborazioneId), formData).pipe(
      catchError(err => {
        console.error('Errore caricamento errori file:', err);
        return of([]);
      })
    );
  }

  getScartatiFile(fileId: number): Observable<ScartoFileDTO[]> {
    const formData = new FormData();
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('read', 'ANAGRAFICA', `getScaricoErroreFile - ${fileId}`))], { type: 'application/json' }));
    return this.http.post<ScartoFileDTO[]>(API_ENDPOINTS.archivioFileCaricati.getScartati(fileId), formData).pipe(
      catchError(err => {
        console.error('Errore caricamento scarti file:', err);
        return of([]);
      })
    );
  }

  getArchivioFileCaricati(filtroFonte?: string, dataDa?: string, dataA?: string): Observable<ArchivioFileCaricatiDTO[]> {
    const formData = new FormData();
    if (filtroFonte) formData.append('filtro_fonte', filtroFonte);
    if (dataDa) formData.append('data_da', dataDa);
    if (dataA) formData.append('data_a', dataA);
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('read', 'ANAGRAFICA', 'Archivio file - ricerca'))], { type: 'application/json' }));
    return this.http.post<ArchivioFileCaricatiDTO[]>(API_ENDPOINTS.archivioFileCaricati.getLista, formData).pipe(
      catchError(err => {
        console.error('Errore caricamento archivio file:', err);
        return of([]);
      })
    );
  }

  getParametroValore(cod: string): Observable<string> {
    return this.http.get(API_ENDPOINTS.parametri.getValore(cod), { responseType: 'text' });
  }

  checkNomeFile(nomeFile: string): Observable<boolean> {
    return this.http.get<boolean>(API_ENDPOINTS.archivioFileCaricati.checkNomeFile(nomeFile));
  }

  checkConcorrenza(tipo: string): Observable<{ bloccato: boolean; messaggio: string | null }> {
    return this.http.get<{ bloccato: boolean; messaggio: string | null }>(
      API_ENDPOINTS.archivioFileCaricati.checkConcorrenza(tipo)
    );
  }

  getExportList(aslId?: string): Observable<ExportFileListDTO[]> {
    return this.http.get<ExportFileListDTO[]>(API_ENDPOINTS.file.getExportList(aslId)).pipe(
      catchError(err => {
        console.error('Errore caricamento export list:', err);
        return of([]);
      })
    );
  }

  downloadFile(_fileId: number, nomeFile: string): Observable<void> {
    const formData = new FormData();
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('read', 'ANAGRAFICA', `download export - ${nomeFile}`))], { type: 'application/json' }));
    return this.http.post(API_ENDPOINTS.file.downloadByName(nomeFile), formData, { responseType: 'blob' }).pipe(
      map(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nomeFile;
        a.click();
        URL.revokeObjectURL(url);
      }),
      catchError(err => {
        console.error('Errore download file:', err);
        return of(undefined);
      })
    );
  }

  downloadImport(fileId: number, nomeFile: string): Observable<void> {
    const formData = new FormData();
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('read', 'ANAGRAFICA', `downloadImport - ${nomeFile}`))], { type: 'application/json' }));
    return this.http.post(API_ENDPOINTS.file.downloadImport(fileId), formData, { responseType: 'blob' }).pipe(
      map(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nomeFile;
        a.click();
        URL.revokeObjectURL(url);
      }),
      catchError(err => {
        console.error('Errore download import:', err);
        return of(undefined);
      })
    );
  }

  uploadPdf(file: File, cartella?: string): Observable<UploadPdfResponse> {
    const formData = new FormData();
    formData.append('file', file);
    if (cartella) formData.append('cartella', cartella);
    return this.http.post<UploadPdfResponse>(API_ENDPOINTS.file.uploadPdf, formData).pipe(
      catchError(err => {
        console.error('Errore upload PDF:', err);
        throw err;
      })
    );
  }

  getStoriaProfFiles(soggettoId: number): Observable<StoriaProfFileDTO[]> {
    return this.http.get<{ prefisso: number; totaleTrovati: number; pdfList: StoriaProfFileDTO[] }>(
      API_ENDPOINTS.file.listaPdf(soggettoId)
    ).pipe(
      map(resp => resp.pdfList ?? []),
      catchError(err => {
        console.error('Errore caricamento storia professionale:', err);
        return of([]);
      })
    );
  }

  downloadPdfByName(nomeFile: string): Observable<void> {
    return this.http.get(API_ENDPOINTS.file.downloadPdfByName(nomeFile), { responseType: 'blob' }).pipe(
      map(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nomeFile;
        a.click();
        URL.revokeObjectURL(url);
      }),
      catchError(err => {
        console.error('Errore download PDF:', err);
        return of(undefined);
      })
    );
  }

  downloadPdf(percorso: string, nomeFile: string): Observable<void> {
    return this.http.get(API_ENDPOINTS.file.downloadPdf, { params: { percorso }, responseType: 'blob' }).pipe(
      map(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nomeFile;
        a.click();
        URL.revokeObjectURL(url);
      }),
      catchError(err => {
        console.error('Errore download PDF:', err);
        return of(undefined);
      })
    );
  }

  eliminaPdf(percorso: string): Observable<{ successo: boolean }> {
    return this.http.delete<{ successo: boolean }>(API_ENDPOINTS.file.eliminaPdf, { params: { percorso } }).pipe(
      catchError(err => {
        console.error('Errore eliminazione PDF:', err);
        throw err;
      })
    );
  }

  avviaExportAllegato4(anno: number): Observable<void> {
    const formData = new FormData();
    formData.append('auditLogRequest', new Blob([JSON.stringify(this.authService.buildAuditLog('write', 'ANAGRAFICA', `avviaExportAllegato4 - ${anno}`))], { type: 'application/json' }));
    return this.http.post<void>(API_ENDPOINTS.file.avviaExportAllegato4(anno), formData).pipe(
      catchError(err => {
        console.error('Errore avvio export Allegato 4:', err);
        throw err;
      })
    );
  }

  getExportListAllegato4(): Observable<ExportAllegato4DTO[]> {
    return this.http.get<ExportAllegato4DTO[]>(API_ENDPOINTS.file.getExportListAllegato4).pipe(
      catchError(err => {
        console.error('Errore caricamento lista Allegato 4:', err);
        return of([]);
      })
    );
  }

  downloadAllegato4(elaborazioneId: number, nome: string): Observable<void> {
    const url = API_ENDPOINTS.file.downloadAllegato4(elaborazioneId);
    return this.http.get(url, { responseType: 'blob' }).pipe(
      map(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = nome;
        a.click();
        URL.revokeObjectURL(a.href);
      }),
      catchError(err => {
        console.error('Errore download Allegato 4:', err);
        return of(undefined);
      })
    );
  }
}
