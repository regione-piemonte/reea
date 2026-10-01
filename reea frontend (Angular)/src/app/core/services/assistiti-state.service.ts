import { Injectable } from '@angular/core';
import { Assistito, AssistitoFiltri } from '../models';

interface NavigationSnapshot {
  allAssistiti: Assistito[];
  filters: Record<string, unknown>;
  page: number;
  pageSize: number;
  stepOffset: number;
  totalRecordsDB: number;
  stepEsaurito: boolean;
  esportatoStep: boolean;
  sortColumn: string;
  sortDirection: string;
  savedAt: number;
}

@Injectable({ providedIn: 'root' })
export class AssistitiStateService {
  private readonly NAV_TTL_MS    = 10 * 60 * 1000; // 10 min
  private readonly MASTER_TTL_MS = 30 * 60 * 1000; // 30 min

  // ── Navigation snapshot (ripristino back-button) ──────────────────────
  private _snapshot: NavigationSnapshot | null = null;

  save(state: Omit<NavigationSnapshot, 'savedAt'>): void {
    this._snapshot = { ...state, savedAt: Date.now() };
  }

  restore(): NavigationSnapshot | null {
    if (!this._snapshot) return null;
    if (Date.now() - this._snapshot.savedAt > this.NAV_TTL_MS) {
      this._snapshot = null;
      return null;
    }
    return this._snapshot;
  }

  clear(): void { this._snapshot = null; }

  // ── Master list (client-side filtering) ──────────────────────────────
  private _masterList: Assistito[] | null = null;
  private _masterFilterState: AssistitoFiltri | null = null;
  private _masterLoadedAt: number | null = null;
  /** true se il BE ha restituito tutti i record (content.length >= totalRecordsDB). */
  private _masterIsComplete = false;

  /**
   * Salva la lista caricata dal BE come base per il filtering client-side.
   * @param filterState  filtri ESATTI con cui è stata fatta la chiamata BE
   * @param isComplete   true se content.length >= totalRecordsDB
   */
  saveMasterList(list: Assistito[], filterState: AssistitoFiltri, isComplete = false): void {
    this._masterList = list;
    this._masterFilterState = filterState;
    this._masterLoadedAt = Date.now();
    this._masterIsComplete = isComplete;
  }

  getMasterList(): Assistito[] | null {
    if (!this._masterList || !this._masterLoadedAt) return null;
    if (Date.now() - this._masterLoadedAt > this.MASTER_TTL_MS) {
      this.clearMasterList();
      return null;
    }
    return this._masterList;
  }

  /** Filtri con cui la master list è stata caricata; null se non disponibile. */
  getMasterFilterState(): AssistitoFiltri | null {
    return this.getMasterList() ? this._masterFilterState : null;
  }

  /** True se il BE ha restituito tutti i record per i filtri della master list. */
  get isMasterComplete(): boolean {
    return !!this.getMasterList() && this._masterIsComplete;
  }

  get hasMasterList(): boolean {
    return this.getMasterList() !== null;
  }

  clearMasterList(): void {
    this._masterList = null;
    this._masterFilterState = null;
    this._masterLoadedAt = null;
    this._masterIsComplete = false;
  }

  clearAll(): void {
    this.clear();
    this.clearMasterList();
  }
}
