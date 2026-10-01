/**
 * Oggetto di audit da includere in ogni chiamata API (richiesto dal backend per tracciatura Shibboleth)
 */
export interface AuditLogRequest {
  idApp: string;
  ipAddress: string;
  utente: string;
  operazione: string;
  oggOper: string;
  keyOper: string;
  uuid: string;
  esitoChiamata: number;
  loginLogout: boolean;
  skipPayload: boolean;
}

/**
 * Modello per l'utente autenticato
 */
export interface User {
  codiceFiscale: string;
  nome: string;
  cognome: string;
  ruolo: RuoloUtente;
  collocazione: Collocazione;
  collocazioni?: Collocazione[];  // lista collocazioni selezionabili (reea_r_utente_collocazione)
  profili: ProfiloApplicativo[];
  tipoProfiloId?: number; // 1=dati in chiaro, 2=dati pseudonimizzati
  ipAddress?: string;     // IP client fornito da Shibboleth durante il login
}

/**
 * Ruoli disponibili nel sistema — valori allineati a profilo_cod in reea_d_profilo
 */
export enum RuoloUtente {
  OPERATORE_CRPT          = 'REEA_OP_CRPT',
  OPERATORE_CRPT_PSEUDO   = 'REEA_OP_CRPT_PSEUDO',
  OPERATORE_SPRESAL       = 'REEA_OP_SPRESAL',
  OPERATORE_SPRESAL_PSEUDO= 'REEA_OP_SPRESAL_PSEUDO',
  OPERATORE_INAIL         = 'REEA_OP_INAIL',
  OPERATORE_EPI_PSEUDO    = 'REEA_OP_EPI_PSEUDO',
  OPERATORE_CSI           = 'REEA_OP_CSI',
  OPERATORE_CSI_PSEUDO    = 'REEA_OP_CSI_PSEUDO'
}

/**
 * Collocazione dell'utente
 */
export interface Collocazione {
  codice: string;
  descrizione: string;
  codiceAzienda?: string;
  descrizioneAzienda?: string;
}

/**
 * Profili applicativi e permessiii
 */
export interface ProfiloApplicativo {
  codice: string;
  descrizione: string;
  funzionalita: string[];
  collocazioni?: Collocazione[];
}

/**
 * Token di autenticazione dal Configuratore
 */
export interface AuthToken {
  token: string;
  expiresAt: Date;
  user: User;
}
