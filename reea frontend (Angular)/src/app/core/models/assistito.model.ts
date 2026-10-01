/**
 * Storico fonte per un assistito (frontend)
 */
export interface FonteStorico {
  descrizione: string;
  dataAssociazione: string;
}

/**
 * Modello dati per Assistito Ex Esposto Amianto
 */
export interface Assistito {
  id?: string;
  registroId?: number;
  numeroRegistro: string;
  cognome: string;
  nome: string;
  codiceFiscale: string;
  dataNascita: Date | string;
  aslCompetenza: string;
  dataInserimento: Date | string;
  dataUltimoAggiornamento: Date | string;
  stato: StatoAssistito;
  fonteProvenienza: FonteProvenienza;
  storicoFonti?: FonteStorico[];
  note?: string;
  telefono?: string;
  email?: string;
  preadesioneTelefono?: string;
  preAdesioneEmail?: string;
  preadesione?: PreAdesioneData;
  // Campi preadesione aggiuntivi (reea_t_adesione)
  codiceAdesione?: string;
  provinciaNascita?: string;
  comuneNascita?: string;
  tesseraTeam?: string;
  idAura?: string;
  provinciaDomicilio?: string;
  comuneDomicilio?: string;
  codiceComuneIstatDomicilio?: string;
  capDomicilio?: string;
  codiceAslDomicilio?: string;
  aslDomicilio?: string;
  codiceAslResidenza?: string;
  aslResidenza?: string;
  // Campi Dati del Registro
  attestazioneSanitaria?: string;
  provenienzaAttestazione?: string;
  dataAttestazione?: Date | string;
  sezione?: string | null;
  insertoInSorveglianza?: boolean | null;
  tipoElencoInail?: string | null;
  dataAdesione?: string | null;
  dataPresentazioneIstanza?: string | null;
  notaStatoAttuale?: string | null;
  esposizioni?: EsposizioneLavorativa[];
  inailRecords?: InailRecord[];
  nplaRecords?: NplaRecord[];
  anamnesiSpresalRecords?: SpresalAnamnesiRecord[];
  telefonoAura?: string;
  emailAura?: string;
  listaPdf?: { nomeFile: string; percorsoAssoluto: string; dimensione?: number; dataModifica?: string }[];
  utenteModifica?: string;
  utenteCreazione?: string;
}

/**
 * Dati Preadesione dell'assistito
 */
export interface PreAdesioneData {
  // Dati anagrafici (reea_t_adesione)
  codiceAdesione?: string;
  dataPreAdesione?: string;
  codiceFiscale?: string;
  cognome?: string;
  nome?: string;
  dataNascita?: string;
  provinciaNascita?: string;
  comuneNascita?: string;
  tesseraTeam?: string;
  idAura?: string;
  provinciaDomicilio?: string;
  comuneDomicilio?: string;
  codiceComuneIstatDomicilio?: string;
  capDomicilio?: string;
  email?: string;
  telefono?: string;
  codiceAslDomicilio?: string;
  aslDomicilio?: string;
  codiceAslResidenza?: string;
  aslResidenza?: string;
}

/**
 * Esposizione lavorativa dell'assistito
 */
export interface EsposizioneLavorativa {
  id?: string;
  azienda: string;
  codiceComuneIstatAzienda?: string;
  comuneAzienda?: string;
  capAzienda?: string;
  provinciaAzienda?: string;
  mansione: string;
  dataInizioEsposizione: string;
  dataFineEsposizione: string;
}

export interface InailRecord {
  registroId: number;
  cognome: string;
  nome: string;
  codiceFiscale: string;
  tipologiaInail: string;
  domanda: number;
  sesso?: string;
  dataNascita?: string;
  indirizzoResidenza?: string;
  istatResidenza?: string;
  capResidenza?: string;
  regioneResidenza?: string;
  provinciaResidenza?: string;
  comuneResidenza?: string;
}

export interface NplaRecord {
  codiceFiscale?: string;
  periodo?: string;
  idCantiere?: string;
  aziendaPiva?: string;
  aziendaNome: string;
  aslCantiere: string;
  comuneCantiere: string;
  anno: number;
  tipologiaPiano: string;
  quantitaDaRimuovere: number;
  quantitaRimossa: number;
}

export interface SpresalAnamnesiRecord {
  regSpresalAnamnesiId?: number;
  dataIntervista?: string;
  nominativoIntervistatore?: string;
  fumatore?: boolean;
  sigarette?: boolean;
  sigaretteAnni?: number;
  sigaretteEtaInizio?: number;
  sigaretteFumaAttualmente?: boolean;
  sigaretteEtaFine?: number;
  sigaretteDie?: number;
  sigari?: boolean;
  sigariAnni?: number;
  sigariEtaInizio?: number;
  sigariFumaAttualmente?: boolean;
  sigariEtaFine?: number;
  sigariDie?: number;
  pipa?: boolean;
  pipaAnni?: number;
  pipaEtaInizio?: number;
  pipaFumaAttualmente?: boolean;
  pipaEtaFine?: number;
  pipaDie?: number;
  occupazioneNum?: string;
  occupazioneAnnoInizio?: string;
  occupazioneAnnoFine?: string;
  occupazioneTipo?: string;
  occupazioneDescrizioneLavoro?: string;
  occupazioneNomeIndirizzoDitta?: string;
  occupazioneAttivitaDitta?: string;
  notaAttivitaConAmianto?: string;
  anamnesiEsposizioneAmianto?: boolean;
  esposizioneProfessionale?: string;
  annoFineEsposizione?: number;
  livelloEsposizione?: string;
  inserimentoInSorveglianza?: boolean;
  occupazioneEsposizioneCrpt?: boolean;
  occupazioneSettoreDittaCrpt?: string;
  occupazioneMansioneCrpt?: string;
  occupazioneRagioneSocialeDittaCrpt?: string;
  occupazionePivaDittaCrpt?: string;
  occupazioneCodiceFiscaleDittaCrpt?: string;
  counseling?: string | null;
}

/**
 * Stati possibili dell'assistito nel registro
 * Basato sulla tabella reea_d_soggetto_stato
 */
export enum StatoAssistito {
  DA_VALUTARE = 'Da valutare',           // ID 1
  ELEGGIBILE = 'Eleggibile',             // ID 2
  NON_ELEGGIBILE = 'Non eleggibile',     // ID 3
  PRESO_IN_CARICO = 'Preso in carico',   // ID 4
  AVVIATO_SORV = 'Avviato alla sorveglianza',        // ID 5
  ESCLUSO_SORV = 'Escluso dalla sorveglianza',       // ID 6
  CONCLU_SORV = 'Sorveglianza conclusa',             // ID 7
  ESCLUSO_DECESSO = 'Escluso per decesso',           // ID 8
  ESCLUSO_EMIGRAZIONE = 'Escluso per emigrazione',  // ID 9
  CARICATO = 'Caricato',                            // ID 10
  NON_PRESO_IN_CARICO = 'Non preso in carico'       // ID 11
}

/**
 * Fonte di provenienza del dato
 * Basato sulla tabella reea_d_fonte
 */
export enum FonteProvenienza {
  SEGNALAZIONE_MMG = 'Segnalazione MMG',  // ID 1
  PREADESIONI = 'Preadesioni',             // ID 2
  INAIL = 'INAIL',                         // ID 3
  NPLA = 'NPLA',                           // ID 4
  SPRESAL = 'SPRESAL'                      // ID 5
}

/**
 * DTO per filtri di ricerca
 * Nota: stato e fonteProvenienza sono array per supportare selezione multipla
 */
export interface AssistitoFiltri {
  // Filtri ricerca anagrafica (colonna destra)
  codiceFiscale?: string;
  cognome?: string;
  nome?: string;
  dataNascita?: Date | string;
  // Filtri categorici (colonna sinistra)
  fonteProvenienza?: string[]; // Array per selezione multipla
  sezione?: string[] | string; // Array per selezione multipla (Sezione 1, 2, 3)
  stato?: string[];            // Array per selezione multipla
  inseritoInSorveglianza?: string[]; // Array per selezione multipla
  elencoInail?: string[];            // Array per selezione multipla
  assistenzaAslId?: string;          // Singola selezione: '' = nessun filtro, 'all' = tutte le ASL, '123' = ASL specifica
  cognomeLettDa?: string;            // Prima lettera cognome (range da, es. "A")
  cognomeLettA?: string;             // Prima lettera cognome (range a, es. "F")
}

/**
 * Response paginata dal backend
 */
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalRecordsDB: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/**
 * Parametri per richieste paginate
 */
export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

/**
 * Modello per la storia degli stati di un assistito
 */
export interface StoriaStato {
  soggetto_stato_id: string;
  soggetto_stato_desc: string;
  data_modifica: Date | string | null;
  data_creazione: Date | string | null;
  utente_modifica: string;
  soggetto_stato_note: string;
}
