// ============================================
// FILE CARICATO MODEL
// Modello per i file importati nel sistema (CDU-026)
// Tabelle: REEA_T_FILE, REEA_L_FILE_ELABORAZIONE
// ============================================

/**
 * DTO backend per un file caricato (REEA_T_FILE + REEA_L_FILE_ELABORAZIONE)
 * Endpoint: GET /api/file/getListaFile
 */
export interface FileCaricatoBackendDTO {
  file_id: number;
  file_nome: string;
  file_mime_type?: string;
  file_dimensione_bytes?: number;
  file_stato_id: number;
  file_stato_desc: string;
  fonte_id?: number;
  fonte_desc?: string;
  righe_totali?: number;
  righe_elaborate?: number;
  righe_scartate?: number;
  data_caricamento?: string;
  utente_caricamento?: string;
  errori?: ErroreElaborazioneBackendDTO[];
}

/**
 * DTO backend per un errore di riga (da REEA_L_FILE_ELABORAZIONE)
 */
export interface ErroreElaborazioneBackendDTO {
  riga?: number;
  campo?: string;
  descrizione: string;
}

/**
 * Rappresenta un file caricato nel sistema
 * con le informazioni sull'elaborazione (vista frontend)
 */
export interface FileCaricato {
  /** ID univoco del file (REEA_T_FILE.file_id) */
  id: number;

  /** Nome del file originale */
  nomeFile: string;

  /** Fonte di provenienza dei dati */
  fonte: string;

  /** Numero totale di record nel file */
  recordTotali?: number;

  /** Numero di record elaborati con successo */
  recordElaborati?: number;

  /** Numero di record scartati per errori */
  recordScartati?: number;

  /** Data e ora del caricamento */
  dataUpload?: Date | string;

  /** Nome dell'operatore che ha caricato il file */
  operatore?: string;

  /** Stato dell'elaborazione del file */
  stato: StatoElaborazioneFile;

  /** Eventuali errori riscontrati */
  errori?: ErroreElaborazione[];
}

/**
 * Stati possibili dell'elaborazione di un file
 * Mappati da REEA_T_FILE.file_stato_id
 */
export enum StatoElaborazioneFile {
  IN_ATTESA = 'In attesa',
  IN_ELABORAZIONE = 'In elaborazione',
  COMPLETATO = 'Completato',
  COMPLETATO_CON_ERRORI = 'Completato con errori',
  ERRORE = 'Errore'
}

/**
 * Rappresenta un errore riscontrato durante l'elaborazione
 */
export interface ErroreElaborazione {
  /** Numero della riga nel file */
  riga?: number;

  /** Campo che ha causato l'errore */
  campo?: string;

  /** Descrizione dell'errore */
  descrizione: string;
}

/**
 * Converte un FileCaricatoBackendDTO nel modello frontend FileCaricato
 */
export function mapFileCaricato(dto: FileCaricatoBackendDTO): FileCaricato {
  return {
    id: dto.file_id,
    nomeFile: dto.file_nome,
    fonte: dto.fonte_desc ?? '',
    recordTotali: dto.righe_totali,
    recordElaborati: dto.righe_elaborate,
    recordScartati: dto.righe_scartate,
    dataUpload: dto.data_caricamento,
    operatore: dto.utente_caricamento,
    stato: mapStato(dto.file_stato_desc),
    errori: dto.errori?.map(e => ({
      riga: e.riga,
      campo: e.campo,
      descrizione: e.descrizione,
    })),
  };
}

function mapStato(statoDesc?: string): StatoElaborazioneFile {
  switch (statoDesc?.toUpperCase()) {
    case 'IN_ATTESA':
    case 'IN ATTESA':      return StatoElaborazioneFile.IN_ATTESA;
    case 'IN_ELABORAZIONE':
    case 'IN ELABORAZIONE': return StatoElaborazioneFile.IN_ELABORAZIONE;
    case 'COMPLETATO':      return StatoElaborazioneFile.COMPLETATO;
    case 'COMPLETATO_CON_ERRORI':
    case 'COMPLETATO CON ERRORI': return StatoElaborazioneFile.COMPLETATO_CON_ERRORI;
    default:                return StatoElaborazioneFile.ERRORE;
  }
}
