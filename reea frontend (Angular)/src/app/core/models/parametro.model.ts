/**
 * DTO per i parametri di configurazione dal backend
 * Endpoint: GET /api/getListaAnagrafiche/getListaParametri
 * NOTA: Il backend restituisce snake_case
 */
export interface ParametroDTO {
  parametro_id: number;
  parametro_cod: string;
  parametro_desc: string;
  parametro_valore: string;
  parametro_tipo_id: number;
  validita_inizio: string | null;
  validita_fine: string | null;
  data_creazione: string | null;
  data_modifica: string | null;
  data_cancellazione: string | null;
  utente_creazione: string | null;
  utente_modifica: string | null;
  utente_cancellazione: string | null;
}

/**
 * Tipi di parametro (basati su parametro_tipo_id)
 */
export enum ParametroTipo {
  PULSANTI_HOMEPAGE = 1,  // Pulsanti della homepage selezione assistiti
  TRATTAMENTO_DATI = 5,   // Testo disclaimer trattamento dati (parametro_id=10)
}
