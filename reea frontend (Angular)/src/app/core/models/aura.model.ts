export interface AuraAssistitoDto {
  id_aura: string | null;
  codice_fiscale: string;
  cognome: string;
  nome: string;
  data_nascita: string | null;   // ISO date string (LocalDate → "YYYY-MM-DD")
  sesso: string;

  // Nascita
  stato_nascita: string | null;
  provincia_nascita_cod: string | null;
  provincia_nascita_desc: string | null;
  comune_nascita_cod: string | null;
  comune_nascita_desc: string | null;

  // Residenza
  stato_residenza: string | null;
  statoResidenza?: string | null; // BE serialization workaround: missing @JsonProperty("stato_residenza")
  provincia_residenza_cod: string | null;
  provincia_residenza_desc: string | null;
  comune_residenza_cod: string | null;
  comune_residenza_desc: string | null;
  indirizzo_residenza: string | null;
  civico_residenza: string | null;
  cap_residenza: string | null;

  // Domicilio (separato dalla residenza se diverso)
  stato_domicilio?: string | null;
  provincia_domicilio_cod?: string | null;
  provincia_domicilio_desc?: string | null;
  comune_domicilio_cod?: string | null;
  comune_domicilio_desc?: string | null;
  indirizzo_domicilio?: string | null;
  civico_domicilio?: string | null;
  cap_domicilio?: string | null;

  // ASL
  asl_assistenza_cod: string | null;
  asl_assistenza_desc: string | null;

  // Decesso / fine ASL
  data_decesso: string | null;
  data_fine_asl: string | null;

  // Contatti (possono essere multipli → uniti con ";" nel form )))
  telefoni?: string[] | null;
  email?: string[] | null;

  // Esenzioni
  esenzioni?: AuraEsenzione[] | null;
}

// Campi snake_case: corrispondono ai @JsonProperty di EsenzioneDTO (backend)
export interface AuraEsenzione {
  esenzione_cod: string;
  esenzione_desc: string;
  diagnosi_cod: string | null;
  diagnosi_desc?: string | null;
  esenzione_data_emissione?: string | null;  // "YYYY-MM-DD" (LocalDate)
  esenzione_data_scadenza?: string | null;   // "YYYY-MM-DD" (LocalDate)
  validita_inizio?: string | null;
  validita_fine?: string | null;
}
