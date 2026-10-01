/**
 * Storico fonte dal backend (snake_case)
 */
export interface FonteStoricoDTO {
  fonte_desc: string;
  data_creazione: string;
}

/**
 * DTO per i dati preadesione (T_ADESIONE) inclusi nel dettaglio assistito
 */
export interface AdesioneBackendDTO {
  adesione_cod?: string;
  adesione_data?: string;
  codice_fiscale?: string;
  cognome?: string;
  nome?: string;
  nascita_data?: string;
  nascita_provincia_desc?: string;
  nascita_comune_desc?: string;
  tessera_team?: string;
  id_aura?: string;
  domicilio_provincia_desc?: string;
  domicilio_comune_desc?: string;
  domicilio_comune_cod?: string;
  domicilio_cap?: string;
  email?: string;
  telefono?: string;
  domicilio_asl_cod?: string;
  domicilio_asl_desc?: string;
  residenza_asl_cod?: string;
  residenza_asl_desc?: string;
}

/**
 * DTO per la lista anagrafiche dal backend
 * Endpoint: GET /api/getListaAnagrafiche
 * NOTA: Il backend restituisce snake_case
 */
export interface RegistroBackendDTO {
  registro_id?: number;
  sezione?: number | string;
  inserito_in_sorveglianza?: boolean;
  tipo_elenco_inail?: string;
  att_sanitaria_stato?: boolean;
  att_sanitaria_spresal?: boolean;
  att_sanitaria_spresal_data?: string;
  att_sanitaria_inail?: boolean;
  att_santaria_inail?: boolean;
}

export interface RigaListaAnagraficaDTO {
  soggetto_id: number;
  registro_id?: number;
  registro?: RegistroBackendDTO;
  codice_fiscale: string;
  domicilio_asl_id: number ;
  residenza_asl_id: number ;
  assistenza_asl_id: number ;
  nome: string;
  cognome: string;
  nascita_data: string ;
  data_creazione: string ;
  data_modifica: string ;
  fonte_id: number ;
  soggetto_stato_id: number ;
  descrizione_fonte: string[];
  descrizione_asl_competenza: string ;
  descrizione_stato: string ;
  sezione: number ;
  inserito_in_sorveglianza: boolean ;
  tipo_elenco_inail: string ;
  versione_numero: number ;
  lista_fonte_by_id_soggetto: FonteStoricoDTO[] ;
  telefono?: string ;
  email?: string ;
  telefono_aura?: string ;
  email_aura?: string ;
  adesione_cod?: string;
  adesione_data?: string ;
  adesione?: AdesioneBackendDTO;
  presentazione_istanza_data?: string ;
  preadesione_telefono?: string ;
  preadesione_email?: string ;
  preadesione_data?: string ;
  nascita_provincia_desc?: string;
  nascita_comune_desc?: string;
  nascita_stato_desc?: string;
  tessera_team?: string;
  id_aura?: string;
  domicilio_provincia_desc?: string;
  domicilio_comune_desc?: string;
  domicilio_comune_cod?: string;
  domicilio_cap?: string;
  domicilio_asl_cod?: string;
  domicilio_asl_desc?: string;
  residenza_asl_cod?: string;
  residenza_asl_desc?: string;
  sesso?: string;
  domicilio_indirizzo?: string;
  domicilio_numero_civico?: string;
  domicilio_stato_desc?: string;
  residenza_indirizzo?: string;
  residenza_numero_civico?: string;
  residenza_comune_cod?: string;
  residenza_cap?: string;
  residenza_comune_desc?: string;
  residenza_provincia_desc?: string;
  residenza_stato_desc?: string;
  assistenza_asl_fine?: string;
  data_decesso?: string;
  soggetto_stato_note?: string ;
  soggetto_stato_precedente_id?: number ;
  lista_esposizione?: EsposizioneBackendDTO[] ;
  lista_esenzione?: EsenzioneBackendDTO[] ;
  lista_inail?: InailBackendDTO[] ;
  lista_npla?: NplaBackendDTO[] ;
  lista_anamnesi_spresal?: SpresalAnamnesiBackendDTO[] ;
  listaPdf?: PdfInfoBackendDTO[];
  att_sanitaria_stato?: boolean ;
  att_sanitaria_spresal?: boolean ;
  att_sanitaria_spresal_data?: string ;
  att_sanitaria_inail?: boolean ;
  att_santaria_inail?: boolean ;  // typo nel backend
  utente_modifica?: string;
  utente_creazione?: string;
}

export interface PdfInfoBackendDTO {
  nomeFile?: string;
  percorsoAssoluto?: string;
  dimensione?: number;
  dataModifica?: string;
}

/**
 * DTO per creazione/modifica anagrafica
 * Endpoint: POST /api/getListaAnagrafiche
 * NOTA: Il backend usa RigaListaAnagraficaDTO che richiede snake_case
 * Basato su CDU-004 par. 6.2 e tabelle REEA_T_SOGGETTO, REEA_T_REGISTRO
 */
export interface AnagraficaCreateDTO {
  // Dati identificativi obbligatori (snake_case per il backend)
  codice_fiscale: string;
  cognome: string;
  nome: string;
  nascita_data: string; // formato yyyy-MM-dd

  // Sesso (obbligatorio per il backend)
  sesso?: string;

  // ID AURA: se presente, BE usa anagrafeGetClient.get(idAura) evitando la find per CF
  id_aura?: string ;

  // ASL - ID numerici (reea_d_asl.asl_id)
  residenza_asl_id?: number ;
  domicilio_asl_id?: number ;
  assistenza_asl_id?: number ;

  // ASL - Codici stringa per AnagraficaServiceImpl.getAslIdByAslCod()
  // NOTA BE: /api/asl/getListaASLCompetenza deve restituire anche asl_cod
  domicilio_asl_cod?: string ;
  residenza_asl_cod?: string ;

  // Fonte di provenienza (1=SEGNALAZIONE_MMG, 2=PREADESIONI, 3=INAIL, 4=NPLA, 5=SPRESAL)
  fonte_id?: number;

  // Stato soggetto - NOTA: BE sovrascrive a 2 (ELEGGIBILE) per flagCaricamentoDaFile=false
  soggetto_stato_id?: number;

  // Data presentazione istanza (reea_t_soggetto.presentazione_istanza_data)
  presentazione_istanza_data?: string ;

  // Dati geografici nascita (nomi da @JsonProperty di AnagraficaDTO.java)
  nascita_stato_cod?: string ;         // @JsonProperty("nascita_stato_cod") - codice ISTAT stato
  nascita_provincia_cod?: string ;     // @JsonProperty("nascita_provincia_cod") - sigla provincia
  nascita_provincia_desc?: string ;    // @JsonProperty("nascita_provincia_desc") - desc provincia
  nascita_comune_cod?: string ;        // @JsonProperty("nascita_comune_cod") - codice ISTAT comune
  nascita_comune_desc?: string ;       // @JsonProperty("nascita_comune_desc") - desc comune

  // Dati geografici domicilio (nomi da @JsonProperty di AnagraficaDTO.java)
  domicilio_comune_cod?: string ;        // @JsonProperty("domicilio_comune_cod")
  domicilio_provincia_cod?: string ;     // @JsonProperty("domicilio_provincia_cod") - sigla provincia
  domicilio_stato_cod?: string ;         // @JsonProperty("domicilio_stato_cod") - codice stato
  domicilio_indirizzo?: string ;         // @JsonProperty("domicilio_indirizzo") → domicilio_indirizzo_cifrato
  domicilio_numero_civico?: string ;     // @JsonProperty("domicilio_numero_civico") → reea_t_soggetto.domicilio_numero_civico
  domicilio_cap?: string ;               // @JsonProperty("domicilio_cap")
  domicilio_comune_desc?: string ;       // @JsonProperty("domicilio_comune_desc") - desc comune
  domicilio_provincia_desc?: string ;    // @JsonProperty("domicilio_provincia_desc") - desc provincia

  // Dati geografici residenza (nomi da @JsonProperty di AnagraficaDTO.java)
  residenza_comune_cod?: string ;        // @JsonProperty("residenza_comune_cod")
  residenza_comune_desc?: string ;       // @JsonProperty("residenza_comune_desc")
  residenza_provincia_cod?: string ;     // @JsonProperty("residenza_provincia_cod") - sigla provincia
  residenza_provincia_desc?: string ;    // @JsonProperty("residenza_provincia_desc") - desc provincia
  residenza_stato_cod?: string ;         // @JsonProperty("residenza_stato_cod")
  residenza_stato_desc?: string ;        // @JsonProperty("residenza_stato_desc")
  residenza_cap?: string ;               // @JsonProperty("residenza_cap")
  residenza_indirizzo?: string ;         // @JsonProperty("residenza_indirizzo") → residenza_indirizzo_cifrato
  residenza_numero_civico?: string ;     // @JsonProperty("residenza_numero_civico")

  // Contatti (nomi da @JsonProperty di AnagraficaDTO.java)
  email?: string ;              // @JsonProperty("email") → email_cifrata (pgp_sym_encrypt)
  telefono?: string ;           // @JsonProperty("telefono") → reea_t_soggetto.telefono
  telefono_aura?: string ; // @JsonProperty("telefono_aura") → da AURA
  email_aura?: string ;    // @JsonProperty("email_aura") → da AURA

  // ASL fine assistenza (nomi da @JsonProperty di AnagraficaDTO.java)
  // NOTA: AURA restituisce "9999-12-31" come sentinel per "nessuna fine" → inviare null
  assistenza_asl_fine?: string ;  // @JsonProperty("assistenza_asl_fine") → reea_t_soggetto.assistenza_asl_fine

  // Lista esposizioni
  lista_esposizione?: EsposizioneDTO[];

  // Lista esenzioni da AURA (inviate dal frontend nel wizard, prive di esenzione_id)
  lista_esenzione?: { esenzione_cod: string; diagnosi_cod?: string | null; esenzione_data_emissione?: string | null; esenzione_data_scadenza?: string | null; }[];
}

/**
 * DTO per creazione esposizione
 * Endpoint: POST insieme all'anagrafica
 * NOTA: Il backend usa snake_case
 * Basato su tabella REEA_T_ESPOSIZIONE e CDU-004 par. 6.2
 */
export interface EsposizioneDTO {
  esposizione_azienda: string;
  esposizione_azienda_comune_cod?: string; // Codice ISTAT comune
  esposizione_azienda_comune_desc?: string; // Descrizione comune
  esposizione_azienda_cap?: string;
  esposizione_azienda_provincia?: string;
  esposizione_mansione?: string; // Descrizione mansione
  esposizione_inizio?: Date; // Data inizio esposizione (01/01/anno) - CDU-004 pag. 12
  esposizione_fine?: Date; // Data fine esposizione (01/01/anno) - CDU-004 pag. 12
}

/**
 * @deprecated Use EsposizioneDTO instead
 */
export type EsposizioneCreateDTO = EsposizioneDTO;

export interface EsposizioneBackendDTO {
  esposizione_azienda: string;
  esposizione_azienda_comune_cod?: string;
  esposizione_azienda_comune_desc?: string;
  esposizione_azienda_cap?: string;
  esposizione_azienda_provincia?: string;
  esposizione_mansione: string;
  esposizione_inizio: string | null;
  esposizione_fine: string | null;
}

export interface InailBackendDTO {
  registro_id: number;
  cognome: string;
  nome: string;
  codice_fiscale: string;
  tipologia_inail: string;
  domanda: number;
  sesso?: string;
  data_nascita?: string;
  indirizzo_residenza?: string;
  istat_residenza?: string;
  cap_residenza?: string;
  regione_residenza?: string;
  provincia_residenza?: string;
  comune_residenza?: string;
}

export interface NplaBackendDTO {
  codice_fiscale?: string;
  periodo?: string;
  id_cantiere?: string;
  azienda_piva?: string;
  azienda_nome: string;
  asl_cantiere: string;
  comune_cantiere: string;
  anno: number;
  tipologia_piano: string;
  quantita_da_rimuovere: number;
  quantita_rimossa: number;
}

export interface EsenzioneBackendDTO {
  esenzione_cod: string;
  esenzione_desc: string;
  diagnosi_cod?: string;
  diagnosi_desc?: string;
  esenzione_data_emissione?: string;
  esenzione_data_scadenza?: string;
}

export interface SpresalAnamnesiBackendDTO {
  reg_spresal_anamnesi_id?: number;
  registro_id?: number;
  data_intervista?: string;           // LocalDate → "yyyy-MM-dd"
  nominativo_intervistatore?: string;
  fumatore?: boolean;
  sigarette?: boolean;
  sigarette_anni?: number;
  sigarette_eta_inizio?: number;
  sigarette_fuma_attualmente?: boolean;
  sigarette_eta_fine?: number;
  sigarette_die?: number;
  sigari?: boolean;
  sigari_anni?: number;
  sigari_eta_inizio?: number;
  sigari_fuma_attualmente?: boolean;
  sigari_eta_fine?: number;
  sigari_die?: number;
  pipa?: boolean;
  pipa_anni?: number;
  pipa_eta_inizio?: number;
  pipa_fuma_attualmente?: boolean;
  pipa_eta_fine?: number;
  pipa_die?: number;
  occupazione_num?: string;
  occupazione_anno_inizio?: string;
  occupazione_anno_fine?: string;
  occupazione_tipo?: string;
  occupazione_descrizione_lavoro?: string;
  occupazione_nome_e_indirizzo_ditta?: string;
  occupazione_attivita_ditta?: string;
  nota_attivita_con_amianto?: string;
  anamnesi_esposizione_amianto?: boolean;
  esposizione_professionale?: string;
  anno_fine_esposizione?: number;
  livello_esposizione?: string;
  inserimento_in_sorveglianza?: boolean;
  occupazione_esposizione_crpt?: boolean;
  occupazione_settore_ditta_crpt?: string;
  occupazione_mansione_crpt?: string;
  occupazione_ragione_sociale_ditta_crpt?: string;
  occupazione_piva_ditta_crpt?: string;
  occupazione_codice_fiscale_ditta_crpt?: string;
  counseling?: string | null;
}
