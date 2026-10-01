import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { API_ENDPOINTS } from '../../api/api.config';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Assistito,
  AssistitoFiltri,
  PaginatedResponse,
  PaginationParams,
  StatoAssistito,
  FonteProvenienza,
  FonteStorico,
  RigaListaAnagraficaDTO,
  AnagraficaCreateDTO,
  EsposizioneDTO,
  EsposizioneLavorativa,
  InailRecord,
  NplaRecord,
  SpresalAnamnesiRecord,
  AuraEsenzione
} from '../models';
import { environment } from '../../../environments/environment';
import { AnagraficaApiService, AnagraficaFiltriApi } from '../../api';
import { AuthService } from './auth.service';
import { RuoloUtente } from '../models';

/**
 * Interfaccia per i dati completi del nuovo assistito dal form wizard
 */
export interface NuovoAssistitoData {
  // Dati identificativi
  codiceFiscale?: string;
  cognome?: string;
  nome?: string;
  dataNascita?: string | Date;
  // Dati nascita
  sesso?: string;  // M / F
  statoNascita?: string;
  provinciaNascita?: string | number | null;
  provinciaNascitaSigla?: string | null;   // sigla provincia nascita (es. "TO")
  provinciaNascitaDesc?: string | null;    // descrizione provincia nascita
  comuneNascita?: string;
  comuneNascitaDesc?: string | null;       // descrizione comune nascita
  // Residenza
  residenza?: {
    stato?: string;
    statoDesc?: string | null;             // descrizione stato (es. "Italia")
    provincia?: string | number | null;
    provinciaSigla?: string | null;        // sigla provincia (es. "TO")
    provinciaDesc?: string | null;         // descrizione provincia
    comune?: string;
    comuneDesc?: string | null;            // descrizione comune
    indirizzo?: string;
    civico?: string;
    cap?: string;
  };
  // Domicilio
  domicilio?: {
    stato?: string;
    provincia?: string | number | null;
    provinciaSigla?: string | null;
    provinciaDesc?: string | null;
    comune?: string;
    comuneDesc?: string | null;
    indirizzo?: string;
    civico?: string;
    cap?: string;
  } | null;
  domicilioCoincide?: boolean;
  // Contatti
  telefono?: string;
  email?: string;
  telefonoAura?: string | null;  // telefoni da AURA (uniti con ";") → telefono_aura
  emailAura?: string | null;     // email da AURA (unite con ";") → email_aura
  // Esposizioni
  luoghiEsposizione?: Array<{
    azienda: string;
    comune: string;
    comuneCod?: string;
    provinciaAziendaSigla?: string | null;
    cap?: string;
    mansione?: string;
    annoInizio?: number | null;
    annoFine?: number | null;
  }>;
  // ASL - ID numerico (PK reea_d_asl) + codice stringa (richiesto da AnagraficaServiceImpl.getAslIdByAslCod)
  aslId?: number | null;
  aslCod?: string | null;
  // ID AURA: se presente, evita double-call in AnagraficaServiceImpl
  idAura?: string | null;
  // Data presentazione istanza (Segnalazione MMG)
  dataPresentazioneIstanza?: string | Date | null;
  // Fine assistenza ASL (da AURA data_fine_asl, sentinel "9999-12-31" → null)
  assistenzaAslFine?: string | null;
  // Esenzioni da AURA (visibili nel riepilogo del wizard)
  esenzioni?: AuraEsenzione[];
  // Altri
  stato?: StatoAssistito;
  fonteProvenienza?: FonteProvenienza;
}

/**
 * Service per la gestione degli assistiti ex esposti amianto 
 */
@Injectable({
  providedIn: 'root'
})
export class AssistitiService {
  private readonly http = inject(HttpClient);
  private readonly anagraficaApi = inject(AnagraficaApiService);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = `${environment.apiUrl}/assistiti`;

  /**
   * Mappa RigaListaAnagraficaDTO (BE) -> Assistito (FE)
   */
  private mapToAssistito(riga: RigaListaAnagraficaDTO): Assistito {
    // Mappa lo storico fonti dal backend
    const storicoFonti: FonteStorico[] = riga.lista_fonte_by_id_soggetto?.map(f => ({
      descrizione: f.fonte_desc,
      dataAssociazione: this.parseDate(f.data_creazione)
    })) ?? [];

    // Il backend può restituire i campi di RegistroDTO annidati sotto "registro"
    // oppure appiattiti al livello radice (se @JsonUnwrapped). Leggiamo entrambi.
    const reg = riga.registro;

    return {
      id: riga.soggetto_id?.toString(),
      registroId: riga.registro_id
        ?? reg?.registro_id
        ?? riga.lista_anamnesi_spresal?.[0]?.registro_id
        ?? riga.lista_inail?.[0]?.registro_id,
      numeroRegistro: riga.soggetto_id?.toString() ?? '',
      cognome: riga.cognome ?? '',
      nome: riga.nome ?? '',
      codiceFiscale: riga.codice_fiscale ?? '',
      dataNascita: this.parseDate(riga.nascita_data),
      aslCompetenza: riga.descrizione_asl_competenza ?? '',
      dataInserimento: this.parseDate(riga.data_creazione),
      dataUltimoAggiornamento: this.parseDate(riga.data_modifica),
      stato: this.mapStatoById(riga.soggetto_stato_id) ?? this.mapStato(riga.descrizione_stato),
      fonteProvenienza: this.mapFonteById(riga.fonte_id) ?? this.mapFonte(riga.descrizione_fonte?.[0]),
      storicoFonti,
      sezione: (riga.sezione ?? reg?.sezione) != null
        ? String(riga.sezione ?? reg?.sezione)
        : null,
      insertoInSorveglianza: riga.inserito_in_sorveglianza ?? reg?.inserito_in_sorveglianza ?? null,
      tipoElencoInail: riga.tipo_elenco_inail ?? reg?.tipo_elenco_inail ?? null,
      // telefono/email in REEA_T_SOGGETTO: inseriti dall'utente (wizard o matita nella card Dati Anagrafici)
      // Mostrati per tutti gli assistiti indipendentemente dalla fonte di provenienza
      telefono: riga.telefono || undefined,
      email: riga.email || undefined,
      telefonoAura: riga.telefono_aura || undefined,
      emailAura: riga.email_aura || undefined,
      preadesioneTelefono: riga.preadesione_telefono || riga.adesione?.telefono || undefined,
      preAdesioneEmail: riga.preadesione_email || riga.adesione?.email || undefined,
      dataAdesione: this.parseDate(riga.adesione_data) || null,
      codiceAdesione: riga.adesione_cod || riga.adesione?.adesione_cod || undefined,
      preadesione: riga.adesione ? {
        codiceAdesione:           riga.adesione.adesione_cod,
        dataPreAdesione:          riga.adesione.adesione_data,
        codiceFiscale:            riga.adesione.codice_fiscale,
        cognome:                  riga.adesione.cognome,
        nome:                     riga.adesione.nome,
        dataNascita:              riga.adesione.nascita_data,
        provinciaNascita:         riga.adesione.nascita_provincia_desc,
        comuneNascita:            riga.adesione.nascita_comune_desc,
        tesseraTeam:              riga.adesione.tessera_team,
        idAura:                   riga.adesione.id_aura,
        provinciaDomicilio:       riga.adesione.domicilio_provincia_desc,
        comuneDomicilio:          riga.adesione.domicilio_comune_desc,
        codiceComuneIstatDomicilio: riga.adesione.domicilio_comune_cod,
        capDomicilio:             riga.adesione.domicilio_cap,
        email:                    riga.adesione.email,
        telefono:                 riga.adesione.telefono,
        codiceAslDomicilio:       riga.adesione.domicilio_asl_cod,
        aslDomicilio:             riga.adesione.domicilio_asl_desc,
        codiceAslResidenza:       riga.adesione.residenza_asl_cod,
        aslResidenza:             riga.adesione.residenza_asl_desc,
      } : undefined,
      provinciaNascita: riga.nascita_provincia_desc || undefined,
      comuneNascita: riga.nascita_comune_desc || undefined,
      tesseraTeam: riga.tessera_team || undefined,
      idAura: riga.id_aura || undefined,
      provinciaDomicilio: riga.domicilio_provincia_desc || undefined,
      comuneDomicilio: riga.domicilio_comune_desc || undefined,
      codiceComuneIstatDomicilio: riga.domicilio_comune_cod || undefined,
      capDomicilio: riga.domicilio_cap || undefined,
      codiceAslDomicilio: riga.domicilio_asl_cod || undefined,
      aslDomicilio: riga.domicilio_asl_desc || undefined,
      codiceAslResidenza: riga.residenza_asl_cod || undefined,
      aslResidenza: riga.residenza_asl_desc || undefined,
      dataPresentazioneIstanza: this.parseDate(riga.preadesione_data || riga.presentazione_istanza_data) || null,
      notaStatoAttuale: riga.soggetto_stato_note ?? null,
      utenteModifica: (riga.utente_modifica && !['ADMIN','SISTEMA','SCONOSCIUTO'].includes(riga.utente_modifica.toUpperCase())) ? riga.utente_modifica : '',
      utenteCreazione: riga.utente_creazione ?? '',
      attestazioneSanitaria: (riga.att_sanitaria_stato ?? reg?.att_sanitaria_stato) === true ? 'SI'
        : (riga.att_sanitaria_stato ?? reg?.att_sanitaria_stato) === false ? 'NO'
        : undefined,
      provenienzaAttestazione: (() => {
        const spresal = riga.att_sanitaria_spresal ?? reg?.att_sanitaria_spresal;
        const inail   = riga.att_sanitaria_inail ?? reg?.att_sanitaria_inail
                     ?? riga.att_santaria_inail  ?? reg?.att_santaria_inail;
        const rawData = riga.att_sanitaria_spresal_data ?? reg?.att_sanitaria_spresal_data;
        const data = rawData
          ? rawData.includes('-')
            ? rawData.split('-').reverse().join('/')   // "2026-03-25" → "25/03/2026"
            : rawData                                  // già formattata "dd/MM/yyyy"
          : null;
        const parts: string[] = [];
        if (spresal) parts.push(data ? `SPRESAL (${data})` : 'SPRESAL');
        if (inail)   parts.push('INAIL');
        return parts.length > 0 ? parts.join(', ') : undefined;
      })(),
      esposizioni: riga.lista_esposizione?.map(e => ({
        azienda: e.esposizione_azienda,
        codiceComuneIstatAzienda: e.esposizione_azienda_comune_cod || undefined,
        comuneAzienda: e.esposizione_azienda_comune_desc || undefined,
        capAzienda: e.esposizione_azienda_cap || undefined,
        provinciaAzienda: e.esposizione_azienda_provincia || undefined,
        mansione: e.esposizione_mansione,
        dataInizioEsposizione: e.esposizione_inizio || '-',
        dataFineEsposizione: e.esposizione_fine || '-'
      } as EsposizioneLavorativa)) ?? [],
      inailRecords: riga.lista_inail?.map(i => ({
        registroId: i.registro_id,
        cognome: i.cognome,
        nome: i.nome,
        codiceFiscale: i.codice_fiscale,
        tipologiaInail: i.tipologia_inail,
        domanda: i.domanda,
        sesso: i.sesso,
        dataNascita: i.data_nascita,
        indirizzoResidenza: i.indirizzo_residenza,
        istatResidenza: i.istat_residenza != null ? String(i.istat_residenza).padStart(6, '0') : undefined,
        capResidenza: i.cap_residenza != null ? String(i.cap_residenza).padStart(5, '0') : undefined,
        regioneResidenza: i.regione_residenza,
        provinciaResidenza: i.provincia_residenza,
        comuneResidenza: i.comune_residenza
      } as InailRecord)) ?? [],
      nplaRecords: riga.lista_npla?.map(n => ({
        codiceFiscale: n.codice_fiscale || undefined,
        periodo: n.periodo || undefined,
        idCantiere: n.id_cantiere || undefined,
        aziendaPiva: n.azienda_piva || undefined,
        aziendaNome: n.azienda_nome,
        aslCantiere: n.asl_cantiere,
        comuneCantiere: n.comune_cantiere,
        anno: n.anno,
        tipologiaPiano: n.tipologia_piano,
        quantitaDaRimuovere: n.quantita_da_rimuovere,
        quantitaRimossa: n.quantita_rimossa
      } as NplaRecord)) ?? [],
      anamnesiSpresalRecords: riga.lista_anamnesi_spresal?.map(a => ({
        regSpresalAnamnesiId: a.reg_spresal_anamnesi_id,
        dataIntervista: this.parseDate(a.data_intervista),
        nominativoIntervistatore: a.nominativo_intervistatore,
        fumatore: a.fumatore,
        sigarette: a.sigarette,
        sigaretteAnni: a.sigarette_anni,
        sigaretteEtaInizio: a.sigarette_eta_inizio,
        sigaretteFumaAttualmente: a.sigarette_fuma_attualmente,
        sigaretteEtaFine: a.sigarette_eta_fine,
        sigaretteDie: a.sigarette_die,
        sigari: a.sigari,
        sigariAnni: a.sigari_anni,
        sigariEtaInizio: a.sigari_eta_inizio,
        sigariFumaAttualmente: a.sigari_fuma_attualmente,
        sigariEtaFine: a.sigari_eta_fine,
        sigariDie: a.sigari_die,
        pipa: a.pipa,
        pipaAnni: a.pipa_anni,
        pipaEtaInizio: a.pipa_eta_inizio,
        pipaFumaAttualmente: a.pipa_fuma_attualmente,
        pipaEtaFine: a.pipa_eta_fine,
        pipaDie: a.pipa_die,
        occupazioneNum: a.occupazione_num,
        occupazioneAnnoInizio: a.occupazione_anno_inizio,
        occupazioneAnnoFine: a.occupazione_anno_fine,
        occupazioneTipo: a.occupazione_tipo,
        occupazioneDescrizioneLavoro: a.occupazione_descrizione_lavoro,
        occupazioneNomeIndirizzoDitta: a.occupazione_nome_e_indirizzo_ditta,
        occupazioneAttivitaDitta: a.occupazione_attivita_ditta,
        notaAttivitaConAmianto: a.nota_attivita_con_amianto,
        anamnesiEsposizioneAmianto: a.anamnesi_esposizione_amianto,
        esposizioneProfessionale: a.esposizione_professionale,
        annoFineEsposizione: a.anno_fine_esposizione,
        livelloEsposizione: a.livello_esposizione,
        inserimentoInSorveglianza: a.inserimento_in_sorveglianza,
        occupazioneEsposizioneCrpt: a.occupazione_esposizione_crpt,
        occupazioneSettoreDittaCrpt: a.occupazione_settore_ditta_crpt,
        occupazioneMansioneCrpt: a.occupazione_mansione_crpt,
        occupazioneRagioneSocialeDittaCrpt: a.occupazione_ragione_sociale_ditta_crpt,
        occupazionePivaDittaCrpt: a.occupazione_piva_ditta_crpt,
        occupazioneCodiceFiscaleDittaCrpt: a.occupazione_codice_fiscale_ditta_crpt,
        counseling: a.counseling ?? null
      } as SpresalAnamnesiRecord)) ?? [],
      listaPdf: riga.listaPdf?.map(p => ({
        nomeFile: p.nomeFile ?? '',
        percorsoAssoluto: p.percorsoAssoluto ?? '',
        dimensione: p.dimensione,
        dataModifica: p.dataModifica
      })) ?? []
    };
  }

  /**
   * Converte una data al formato "yyyy-MM-ddT00:00:00" (con componente oraria)
   * per evitare il problema di timezone: "2025-02-18" viene interpretato
   * dai browser come UTC midnight, causando shift di un giorno in UTC+1/+2.
   * "2025-02-18T00:00:00" viene invece interpretato come ora locale.
   */
  private parseDate(dateStr?: string | null): string {
    if (!dateStr) return '';

    // Se ha già la componente T (es. "2025-02-18T00:00:00"), tronca eventuali offset
    if (/^\d{4}-\d{2}-\d{2}T/.test(dateStr)) {
      return dateStr.substring(0, 19); // es. "2025-02-18T00:00:00"
    }

    // Plain yyyy-MM-dd → aggiunge T00:00:00 per timezone-safe rendering
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
      return `${dateStr}T00:00:00`;
    }

    // dd/MM/yyyy o dd/MM/yyyy HH:mm:ss → yyyy-MM-ddT...
    if (/^\d{2}\/\d{2}\/\d{4}/.test(dateStr)) {
      const [day, month, rest] = dateStr.split('/');
      const year = rest.substring(0, 4);
      const time = rest.length > 4 ? rest.substring(5) : '00:00:00';
      return `${year}-${month}-${day}T${time}`;
    }

    return dateStr;
  }

  /**
   * Normalizza una data (Date object o stringa) in formato yyyy-MM-dd
   */
  private normalizeDate(date: Date | string | null | undefined): string {
    if (!date) return '';

    if (typeof date === 'string') {
      if (/^\d{4}-\d{2}-\d{2}/.test(date)) {
        return date.substring(0, 10);
      }
      if (/^\d{2}\/\d{2}\/\d{4}/.test(date)) {
        const [day, month, year] = date.split('/');
        return `${year}-${month}-${day}`;
      }
      return date;
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Mappa soggetto_stato_id (numero) all'enum StatoAssistito
   * Basato sulla tabella reea_d_soggetto_stato
   */
  private mapStatoById(statoId?: number | null): StatoAssistito | null {
    if (!statoId) return null;

    const statoIdMap: Record<number, StatoAssistito> = {
      1: StatoAssistito.DA_VALUTARE,
      2: StatoAssistito.ELEGGIBILE,
      3: StatoAssistito.NON_ELEGGIBILE,
      4: StatoAssistito.PRESO_IN_CARICO,
      5: StatoAssistito.AVVIATO_SORV,
      6: StatoAssistito.ESCLUSO_SORV,
      7: StatoAssistito.CONCLU_SORV,
      8: StatoAssistito.ESCLUSO_DECESSO,
      9: StatoAssistito.ESCLUSO_EMIGRAZIONE,
      10: StatoAssistito.CARICATO,
      11: StatoAssistito.NON_PRESO_IN_CARICO
    };

    return statoIdMap[statoId] ?? null;
  }

  /**
   * Mappa la descrizione stato del BE all'enum StatoAssistito
   */
  private mapStato(descrizione?: string | null): StatoAssistito {
    if (!descrizione) return StatoAssistito.DA_VALUTARE;

    const statoMap: Record<string, StatoAssistito> = {
      'DA VALUTARE': StatoAssistito.DA_VALUTARE,
      'Da valutare': StatoAssistito.DA_VALUTARE,
      'ELEGGIBILE': StatoAssistito.ELEGGIBILE,
      'Eleggibile': StatoAssistito.ELEGGIBILE,
      'ELIGIBILE': StatoAssistito.ELEGGIBILE,        
      'Eligibile': StatoAssistito.ELEGGIBILE,        
      'NON ELEGGIBILE': StatoAssistito.NON_ELEGGIBILE,
      'Non eleggibile': StatoAssistito.NON_ELEGGIBILE,
      'NON ELIGIBILE': StatoAssistito.NON_ELEGGIBILE, 
      'Non eligibile': StatoAssistito.NON_ELEGGIBILE, 
      'PRESO IN CARICO': StatoAssistito.PRESO_IN_CARICO,
      'Preso in carico': StatoAssistito.PRESO_IN_CARICO,
      'AVVIATO ALLA SORVEGLIANZA': StatoAssistito.AVVIATO_SORV,
      'Avviato alla sorveglianza': StatoAssistito.AVVIATO_SORV,
      'ESCLUSO DALLA SORVEGLIANZA': StatoAssistito.ESCLUSO_SORV,
      'Escluso dalla sorveglianza': StatoAssistito.ESCLUSO_SORV,
      'SORVEGLIANZA_CONCLUSA': StatoAssistito.CONCLU_SORV,
      'SORVEGLIANZA CONCLUSA': StatoAssistito.CONCLU_SORV,
      'Sorveglianza conclusa': StatoAssistito.CONCLU_SORV,
      'ESCLUSO PER DECESSO': StatoAssistito.ESCLUSO_DECESSO,
      'Escluso per decesso': StatoAssistito.ESCLUSO_DECESSO,
      'ESCLUSO PER EMIGRAZIONE': StatoAssistito.ESCLUSO_EMIGRAZIONE,
      'Escluso per emigrazione': StatoAssistito.ESCLUSO_EMIGRAZIONE,
      'CARICATO': StatoAssistito.CARICATO,
      'Caricato': StatoAssistito.CARICATO,
      'NON PRESO IN CARICO': StatoAssistito.NON_PRESO_IN_CARICO,
      'Non preso in carico': StatoAssistito.NON_PRESO_IN_CARICO
    };

    return statoMap[descrizione] ?? StatoAssistito.DA_VALUTARE;
  }

  /**
   * Mappa fonte_id (numero) all'enum FonteProvenienza
   * Basato sulla tabella reea_d_fonte
   */
  private mapFonteById(fonteId?: number | null): FonteProvenienza | null {
    if (!fonteId) return null;

    // Mapping basato su reea_d_fonte
    const fonteIdMap: Record<number, FonteProvenienza> = {
      1: FonteProvenienza.SEGNALAZIONE_MMG,  // SEGNALAZIONE_MMG
      2: FonteProvenienza.PREADESIONI,       // PREADESIONE
      3: FonteProvenienza.INAIL,             // INAIL
      4: FonteProvenienza.NPLA,              // NPLA (Piani di Lavoro Amianto)
      5: FonteProvenienza.SPRESAL,           // SPRESAL
      // Per INAIL A e B, se servono ID separati, aggiungerli qui
    };

    return fonteIdMap[fonteId] ?? null;
  }

  /**
   * Mappa la descrizione fonte del BE all'enum FonteProvenienza
   */
  private mapFonte(descrizione?: string): FonteProvenienza {
    if (!descrizione) return FonteProvenienza.INAIL;

    const fonteMap: Record<string, FonteProvenienza> = {
      'Preadesioni': FonteProvenienza.PREADESIONI,
      'PREADESIONE': FonteProvenienza.PREADESIONI,
      'Preadesione': FonteProvenienza.PREADESIONI,
      'Segnalazione MMG': FonteProvenienza.SEGNALAZIONE_MMG,
      'SEGNALAZIONE_MMG': FonteProvenienza.SEGNALAZIONE_MMG,
      'INAIL': FonteProvenienza.INAIL,
      'NPLA': FonteProvenienza.NPLA,
      'Piani di Lavoto Amianto': FonteProvenienza.NPLA,
      'SPRESAL': FonteProvenienza.SPRESAL,
      'SPreSAL': FonteProvenienza.SPRESAL
    };

    return fonteMap[descrizione] ?? FonteProvenienza.INAIL;
  }

  /**
   * Mapping da stato_id (stringa) a descrizione per il backend
   * Il BE accetta descrizione_stato come stringa (es. 'DA VALUTARE', 'ELEGGIBILE')
   */
  // Valori allineati all'enum StatoAssistito (= descrizioni in reea_d_soggetto_stato)
  // Valori ESATTI di soggetto_stato_desc nella tabella reea_d_soggetto_stato (uppercase)
  private readonly statoIdToDescMap: Record<string, string> = {
    '1': 'DA VALUTARE',
    '2': 'ELEGGIBILE',
    '3': 'NON ELEGGIBILE',
    '4': 'PRESO IN CARICO',
    '5': 'AVVIATO ALLA SORVEGLIANZA',
    '6': 'ESCLUSO DALLA SORVEGLIANZA',
    '7': 'SORVEGLIANZA CONCLUSA',
    '8': 'ESCLUSO PER DECESSO',
    '9': 'ESCLUSO PER EMIGRAZIONE',
    '10': 'CARICATO',
    '11': 'NON PRESO IN CARICO'
  };

  /**
   * Mappa FonteProvenienza (enum FE) a fonte_id (numero BE)
   * Basato sulla tabella reea_d_fonte
   */
  private mapFonteToId(fonte?: FonteProvenienza): number {
    const map: Partial<Record<FonteProvenienza, number>> = {
      [FonteProvenienza.SEGNALAZIONE_MMG]: 1,
      [FonteProvenienza.PREADESIONI]: 2,
      [FonteProvenienza.INAIL]: 3,
      [FonteProvenienza.NPLA]: 4,
      [FonteProvenienza.SPRESAL]: 5
    };
    return (fonte && map[fonte]) ?? 1;
  }

  /**
   * Mappa StatoAssistito (enum FE) a soggetto_stato_id (numero BE)
   */
  private mapStatoToId(stato?: StatoAssistito): number {
    const map: Partial<Record<StatoAssistito, number>> = {
      [StatoAssistito.DA_VALUTARE]: 1,
      [StatoAssistito.ELEGGIBILE]: 2,
      [StatoAssistito.NON_ELEGGIBILE]: 3,
      [StatoAssistito.PRESO_IN_CARICO]: 4,
      [StatoAssistito.AVVIATO_SORV]: 5,
      [StatoAssistito.ESCLUSO_SORV]: 6,
      [StatoAssistito.CONCLU_SORV]: 7,
      [StatoAssistito.ESCLUSO_DECESSO]: 8,
      [StatoAssistito.ESCLUSO_EMIGRAZIONE]: 9,
      [StatoAssistito.CARICATO]: 10,
      [StatoAssistito.NON_PRESO_IN_CARICO]: 11
    };
    return (stato && map[stato]) ?? 1;
  }

  /**
   * Ottiene lista paginata di assistiti con filtri server-side
   * GET /api/getListaAnagrafiche con filtri: fonte_id, descrizione_stato, sezione, inserito_in_sorveglianza
   */
  getAssistiti(
    filtri: AssistitoFiltri = {},
    pagination: PaginationParams = { page: 0, size: 10 },
    azzeraContatore: boolean | null = null
  ): Observable<PaginatedResponse<Assistito>> {
    // Costruisci i filtri per l'API (formato POST - camelCase)
    const apiFiltri: AnagraficaFiltriApi = {};

    // filtroFonteId: array di numeri; 'all' = nessun filtro
    if (filtri.fonteProvenienza && filtri.fonteProvenienza.length > 0 && !filtri.fonteProvenienza.includes('all')) {
      apiFiltri.filtroFonteId = filtri.fonteProvenienza
        .map(id => parseInt(id, 10))
        .filter(id => !isNaN(id));
    }

    // descrizioniStato: array di descrizioni testuali; 'all' = nessun filtro
    if (filtri.stato && filtri.stato.length > 0 && !filtri.stato.includes('all')) {
      apiFiltri.descrizioniStato = filtri.stato
        .map(id => this.statoIdToDescMap[id])
        .filter((desc): desc is string => !!desc);
    }

    if (this.authService.isPseudo() && !apiFiltri.descrizioniStato?.length) {
      const statiVietati = new Set(['DA VALUTARE', 'CARICATO']);
      apiFiltri.descrizioniStato = Object.values(this.statoIdToDescMap)
        .filter(desc => !statiVietati.has(desc));
    }

    // sezione: array di numeri; 'all' = nessun filtro
    if (filtri.sezione && Array.isArray(filtri.sezione) && filtri.sezione.length > 0 && !filtri.sezione.includes('all')) {
      apiFiltri.sezione = filtri.sezione.map(s => parseInt(s, 10)).filter(n => !isNaN(n));
    } else if (filtri.sezione && typeof filtri.sezione === 'string' && filtri.sezione !== '' && filtri.sezione !== 'all') {
      const n = parseInt(filtri.sezione, 10);
      if (!isNaN(n)) apiFiltri.sezione = [n];
    }

    // insInSorveglianza: boolean; gestito client-side se multi-valore
    if (filtri.inseritoInSorveglianza && filtri.inseritoInSorveglianza.length > 0) {
      const nonNullVals = filtri.inseritoInSorveglianza.filter(v => v !== 'null');
      if (nonNullVals.length === 1 && !filtri.inseritoInSorveglianza.includes('null')) {
        apiFiltri.insInSorveglianza = nonNullVals[0] === '1';
      }
    }

    // tipoElencoInail: gestito client-side se multi-valore
    if (filtri.elencoInail && filtri.elencoInail.length > 0) {
      const nonNullVals = filtri.elencoInail.filter(v => v !== 'null');
      if (nonNullVals.length === 1 && !filtri.elencoInail.includes('null')) {
        apiFiltri.tipoElencoInail = nonNullVals[0];
      }
    }

    // assistenzaAslId: array di numeri; 'all' = nessun filtro
    if (filtri.assistenzaAslId && filtri.assistenzaAslId !== '') {
      if (filtri.assistenzaAslId !== 'all') {
        const aslId = parseInt(filtri.assistenzaAslId, 10);
        if (!isNaN(aslId)) {
          apiFiltri.assistenzaAslId = [aslId];
        }
      }
    }

    // codiceFiscale, cognome, nome: filtri server-side (Strada A)
    if (filtri.codiceFiscale?.trim()) {
      apiFiltri.codiceFiscale = filtri.codiceFiscale.trim();
    }
    if (filtri.cognome?.trim()) {
      apiFiltri.cognome = filtri.cognome.trim();
    }
    if (filtri.nome?.trim()) {
      apiFiltri.nome = filtri.nome.trim();
    }
    if (filtri.dataNascita) {
      const dateStr = this.normalizeDate(filtri.dataNascita);
      if (dateStr) apiFiltri.nascitaData = dateStr;
    }
    if (filtri.cognomeLettDa?.trim()) {
      apiFiltri.cognomeLettDa = filtri.cognomeLettDa.trim();
    }
    if (filtri.cognomeLettA?.trim()) {
      apiFiltri.cognomeLettA = filtri.cognomeLettA.trim();
    }

    // Il meccanismo step si applica SOLO per Preadesioni (2) e MMG (1) e solo quando
    // azzeraContatore è esplicitamente true (reset) o false (avanza).
    // null = bypass step → BE restituisce tutti i record senza limite di passo
    // (usato al restore dal dettaglio per evitare che il counter esaurito blocchi la lista).
    const fonteStepConsentite = [1, 2];
    const tutteFontiSelezionate = !!filtri.fonteProvenienza?.includes('all');
    const nessunaFonteSelezionata = !filtri.fonteProvenienza || filtri.fonteProvenienza.length === 0;
    const stepApplicabile = tutteFontiSelezionate
      || nessunaFonteSelezionata
      || (!!apiFiltri.filtroFonteId
        && apiFiltri.filtroFonteId.length > 0
        && apiFiltri.filtroFonteId.every(id => fonteStepConsentite.includes(id)));
    const azzeraContatoreEffettivo: boolean | null =
      (stepApplicabile && azzeraContatore !== null) ? azzeraContatore : null;

    return this.anagraficaApi.getLista(apiFiltri, azzeraContatoreEffettivo).pipe(
      map(({ records, totalRecords }) => this.processResults(records, filtri, pagination, totalRecords))
    );
  }

  /**
   * Processa i risultati: mapping, filtri client-side e paginazione
   */
  private processResults(
    righe: RigaListaAnagraficaDTO[],
    filtri: AssistitoFiltri,
    pagination: PaginationParams,
    totalRecordsDB = 0
  ): PaginatedResponse<Assistito> {
    // Mappa i dati dal BE al modello FE
    let assistiti = righe.map(riga => this.mapToAssistito(riga));

    // Rimuovi duplicati per soggetto_id
    const seen = new Set<string>();
    assistiti = assistiti.filter(a => {
      if (a.id && seen.has(a.id)) {
        return false;
      }
      if (a.id) seen.add(a.id);
      return true;
    });

    // Applica filtri client-side (escluso fonte che è già filtrato lato server)
    assistiti = this.applyFilters(assistiti, filtri);

    // assistiti = this.applyRuoloFilter(assistiti, righe);

    // Paginazione client-side
    const start = pagination.page * pagination.size;
    const end = start + pagination.size;
    const paginatedData = assistiti.slice(start, end);

    return {
      content: paginatedData,
      totalElements: assistiti.length,
      totalRecordsDB,
      totalPages: Math.ceil(assistiti.length / pagination.size),
      size: pagination.size,
      number: pagination.page,
      first: pagination.page === 0,
      last: end >= assistiti.length
    };
  }

  /**
   * Applica i filtri client-side (solo ricerca anagrafica)
   * I filtri categorici (fonte, stato, sezione, sorveglianza) sono gestiti server-side
   */
  private applyFilters(assistiti: Assistito[], filtri: AssistitoFiltri): Assistito[] {
    let filtered = [...assistiti];

    if (filtri.codiceFiscale) {
      filtered = filtered.filter(a =>
        a.codiceFiscale.toUpperCase() === filtri.codiceFiscale!.toUpperCase()
      );
    }

    if (filtri.cognome) {
      filtered = filtered.filter(a =>
        a.cognome.toLowerCase().includes(filtri.cognome!.toLowerCase())
      );
    }

    if (filtri.nome) {
      filtered = filtered.filter(a =>
        a.nome.toLowerCase().includes(filtri.nome!.toLowerCase())
      );
    }

    // Filtro inserito_in_sorveglianza client-side (per multi-select con null o valori multipli)
    if (filtri.inseritoInSorveglianza && filtri.inseritoInSorveglianza.length > 0) {
      const nonNullVals = filtri.inseritoInSorveglianza.filter(v => v !== 'null');
      const hasNull = filtri.inseritoInSorveglianza.includes('null');
      if (nonNullVals.length !== 1 || hasNull) {
        filtered = filtered.filter(a => {
          if (a.insertoInSorveglianza === true) return filtri.inseritoInSorveglianza!.includes('1');
          if (a.insertoInSorveglianza === false) return filtri.inseritoInSorveglianza!.includes('0');
          return hasNull;
        });
      }
    }

    // Filtro tipo_elenco_inail client-side (per multi-select con null o valori multipli)
    if (filtri.elencoInail && filtri.elencoInail.length > 0) {
      const nonNullVals = filtri.elencoInail.filter(v => v !== 'null');
      const hasNull = filtri.elencoInail.includes('null');
      if (nonNullVals.length !== 1 || hasNull) {
        filtered = filtered.filter(a => {
          if (!a.tipoElencoInail) return hasNull;
          return filtri.elencoInail!.includes(a.tipoElencoInail);
        });
      }
    }

    return filtered;
  }

  private applyRuoloFilter(assistiti: Assistito[], righe: RigaListaAnagraficaDTO[]): Assistito[] {
    const ruolo = this.authService.currentUser()?.ruolo;
    const isSpresalOrEpi =
      ruolo === RuoloUtente.OPERATORE_SPRESAL ||
      ruolo === RuoloUtente.OPERATORE_SPRESAL_PSEUDO ||
      ruolo === RuoloUtente.OPERATORE_EPI_PSEUDO;

    if (!isSpresalOrEpi) return assistiti;

    // stato_id esclusi direttamente
    const statiEsclusi = new Set([StatoAssistito.CARICATO, StatoAssistito.DA_VALUTARE]);
    // stati ESCLUSO che dipendono dallo stato precedente
    const statiCondizionati = new Set([StatoAssistito.ESCLUSO_DECESSO, StatoAssistito.ESCLUSO_EMIGRAZIONE]);
    // stato_id considerati "non ancora presi in carico"
    const statiPrecedentiEsclusi = new Set([1, 10]); // DA_VALUTARE=1, CARICATO=10

    // mappa soggetto_id → stato_precedente_id (dal backend)
    const precedenteMap = new Map<string, number | undefined>();
    righe.forEach(r => precedenteMap.set(r.soggetto_id?.toString(), r.soggetto_stato_precedente_id));

    return assistiti.filter(a => {
      if (statiEsclusi.has(a.stato as StatoAssistito)) return false;
      if (statiCondizionati.has(a.stato as StatoAssistito)) {
        const precId = precedenteMap.get(a.id ?? '');
        // Nasconde se stato precedente è assente (null/undefined) o è CARICATO/DA_VALUTARE
        if (!precId || statiPrecedentiEsclusi.has(precId)) return false;
      }
      return true;
    });
  }

  /**
   * Ottiene un singolo assistito per ID
   * GET /api/getListaAnagrafiche/getById/{soggettoId}
   */
  getAssistitoById(id: string): Observable<Assistito> {
    return this.anagraficaApi.getById(Number(id)).pipe(map(riga => this.mapToAssistito(riga)));
  }

  /**
   * Crea un nuovo assistito con tutti i dati del form wizard
   * POST /api/getListaAnagrafiche
   * @param data - Dati completi dal form wizard
   * @returns Assistito con ID assegnato dal backend
   */
  createAssistito(data: NuovoAssistitoData): Observable<Assistito> {
    const aslId = data.aslId ?? null;

    const lista_esposizione: EsposizioneDTO[] = (data.luoghiEsposizione || [])
      .filter(l => l.azienda)
      .map(l => ({
        esposizione_azienda: l.azienda,
        esposizione_azienda_comune_cod: l.comuneCod || '',
        esposizione_azienda_comune_desc: l.comune || '',
        esposizione_azienda_cap: l.cap || undefined,
        esposizione_azienda_provincia: l.provinciaAziendaSigla || undefined,
        esposizione_mansione: l.mansione || '',
        esposizione_inizio: l.annoInizio ? new Date(l.annoInizio, 0, 1) : undefined,
        esposizione_fine: l.annoFine ? new Date(l.annoFine, 0, 1) : undefined
      }));

    // Domicilio effettivo: se coincide con residenza (domicilio=null), usa residenza
    const domicilioEff = data.domicilio ?? data.residenza;

    // Costruisci il DTO per il backend (snake_case) - CDU-004 par. 6.2
    const anagrafica: AnagraficaCreateDTO = {
      codice_fiscale: data.codiceFiscale?.toUpperCase() || '',
      cognome: data.cognome?.toUpperCase() || '',
      nome: data.nome?.toUpperCase() || '',
      nascita_data: this.normalizeDate(data.dataNascita),
      sesso: data.sesso || '',

      // id_aura: evita double-call AURA in AnagraficaServiceImpl se soggetto trovato in Step 1
      id_aura: data.idAura ?? null,

      fonte_id: this.mapFonteToId(data.fonteProvenienza),

      // ASL: ID numerico + codice stringa (BE usa getAslIdByAslCod per residenza/domicilio)
      residenza_asl_id: aslId,
      domicilio_asl_id: aslId,
      assistenza_asl_id: aslId,
      domicilio_asl_cod: data.aslCod ?? null,
      residenza_asl_cod: data.aslCod ?? null,

      soggetto_stato_id: this.mapStatoToId(data.stato),

      presentazione_istanza_data: data.dataPresentazioneIstanza
        ? this.normalizeDate(data.dataPresentazioneIstanza)
        : null,

      // Dati geografici nascita (field names = @JsonProperty di AnagraficaDTO.java)
      nascita_stato_cod: data.statoNascita ?? null,
      nascita_provincia_cod: data.provinciaNascitaSigla ?? null,
      nascita_provincia_desc: data.provinciaNascitaDesc ?? null,
      nascita_comune_cod: data.comuneNascita ?? null,
      nascita_comune_desc: data.comuneNascitaDesc ?? null,

      // Dati geografici domicilio (field names = @JsonProperty di AnagraficaDTO.java)
      domicilio_comune_cod: domicilioEff?.comune ?? null,
      domicilio_provincia_cod: domicilioEff?.provinciaSigla ?? null,
      domicilio_stato_cod: domicilioEff?.stato ?? null,
      domicilio_indirizzo: domicilioEff?.indirizzo ?? null,
      domicilio_numero_civico: domicilioEff?.civico ?? null,
      domicilio_cap: domicilioEff?.cap ?? null,
      domicilio_comune_desc: domicilioEff?.comuneDesc ?? null,
      domicilio_provincia_desc: domicilioEff?.provinciaDesc ?? null,

      // Dati geografici residenza (field names = @JsonProperty di AnagraficaDTO.java)
      residenza_comune_cod: data.residenza?.comune ?? null,
      residenza_comune_desc: data.residenza?.comuneDesc ?? null,
      residenza_provincia_cod: data.residenza?.provinciaSigla ?? null,
      residenza_provincia_desc: data.residenza?.provinciaDesc ?? null,
      residenza_stato_cod: data.residenza?.stato ?? null,
      residenza_stato_desc: data.residenza?.statoDesc ?? null,
      residenza_cap: data.residenza?.cap ?? null,
      residenza_indirizzo: data.residenza?.indirizzo ?? null,
      residenza_numero_civico: data.residenza?.civico ?? null,

      // Contatti (@JsonProperty di AnagraficaDTO.java)
      email: data.email ?? null,
      telefono: data.telefono ?? null,
      telefono_aura: data.telefonoAura ?? null,
      email_aura: data.emailAura ?? null,

      // Fine assistenza ASL (AURA data_fine_asl, sentinel escluso)
      assistenza_asl_fine: data.assistenzaAslFine ?? null,

      lista_esposizione: lista_esposizione.length > 0 ? lista_esposizione : undefined,

      // Esenzioni AURA: inviate dal wizard per essere salvate a DB durante la creazione
      lista_esenzione: data.esenzioni && data.esenzioni.length > 0
        ? data.esenzioni.map(e => ({
            esenzione_cod: e.esenzione_cod,
            diagnosi_cod: e.diagnosi_cod ?? null,
            esenzione_data_emissione: e.esenzione_data_emissione ?? null,
            esenzione_data_scadenza: e.esenzione_data_scadenza ?? null
          }))
        : undefined
    };

    console.log('[ESE SERVICE] lista_esenzione nel payload:', anagrafica.lista_esenzione);

    return this.anagraficaApi.crea(anagrafica).pipe(
      map((response: number | { soggetto_id?: number } | null) => {
        // Il backend può restituire: un numero, un oggetto con soggetto_id, o null
        let soggettoId: number | null = null;

        if (typeof response === 'number') {
          soggettoId = response;
        } else if (response && typeof response === 'object' && 'soggetto_id' in response) {
          soggettoId = response.soggetto_id ?? null;
        }

        // Se non abbiamo un ID valido, genera uno temporaneo
        const idStr = soggettoId ? soggettoId.toString() : `temp-${Date.now()}`;

        return {
          id: idStr,
          numeroRegistro: idStr,
          codiceFiscale: data.codiceFiscale || '',
          cognome: data.cognome || '',
          nome: data.nome || '',
          dataNascita: this.normalizeDate(data.dataNascita),
          aslCompetenza: data.aslId?.toString() || '',
          stato: StatoAssistito.DA_VALUTARE,
          fonteProvenienza: FonteProvenienza.SEGNALAZIONE_MMG,
          dataInserimento: new Date().toISOString(),
          dataUltimoAggiornamento: new Date().toISOString()
        } as Assistito;
      })
    );
  }

  /**
   * Modifica un assistito esistente
   * POST /api/getListaAnagrafiche/modifica/{soggettoId}
   */
  modificaAssistito(id: string, data: Partial<AnagraficaCreateDTO>): Observable<Assistito> {
    return this.anagraficaApi.modifica(Number(id), data).pipe(
      map((riga: RigaListaAnagraficaDTO) => this.mapToAssistito(riga))
    );
  }

  /**
   * Aggiorna un assistito esistente (alias per modificaAssistito)
   */
  updateAssistito(id: string, assistito: Partial<Assistito>): Observable<Assistito> {
    const anagrafica: Partial<AnagraficaCreateDTO> = {
      codice_fiscale: assistito.codiceFiscale || '',
      cognome: assistito.cognome || '',
      nome: assistito.nome || '',
      nascita_data: assistito.dataNascita ? this.normalizeDate(assistito.dataNascita) : ''
    };
    return this.modificaAssistito(id, anagrafica);
  }

  /**
   * Elimina un assistito
   * DELETE /api/assistiti/{id}
   */
  deleteAssistito(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Valida assistiti in stato "Da validare"
   * POST /api/assistiti/valida
   */
  validaAssistiti(ids: string[]): Observable<{ success: number; failed: number }> {
    return this.http.post<{ success: number; failed: number }>(
      `${this.apiUrl}/valida`,
      { ids }
    );
  }

  /**
   * Valuta un assistito: aggiorna lo stato (Eleggibile / Non eleggibile) con nota opzionale.
   * Storicizza il dato su s_soggetto.
   * POST /api/anagrafica/aggiornaStato/{soggettoId}
   */
  valutaAssistito(soggettoId: string, statoId: number, nota: string, cf = '', oggOper = ''): Observable<void> {
    return this.anagraficaApi.aggiornaStato(Number(soggettoId), { soggetto_stato_id: statoId, soggetto_stato_note: nota }, cf, oggOper);
  }

  /**
   * Invia assistiti al monitoraggio SPRESAL
   * POST /api/assistiti/invia-monitoraggio
   */
  inviaAlMonitoraggio(ids: string[]): Observable<{ success: number; failed: number }> {
    return this.http.post<{ success: number; failed: number }>(
      `${this.apiUrl}/invia-monitoraggio`,
      { ids }
    );
  }

  /**
   * Importa dati da file (XLS/CSV)
   * POST /api/assistiti/import
   */
  importaDaFile(file: File): Observable<{ imported: number; errors: string[] }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ imported: number; errors: string[] }>(
      `${this.apiUrl}/import`,
      formData
    );
  }

  /**
   * Esporta assistiti in Excel
   * GET /api/assistiti/export/excel
   */
  esportaExcel(filtri: AssistitoFiltri = {}): Observable<Blob> {
    let params = new HttpParams();
    Object.entries(filtri).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, value.toString());
      }
    });

    return this.http.get(`${this.apiUrl}/export/excel`, {
      params,
      responseType: 'blob'
    });
  }

  scaricaDatiExcelTotali(flagDatiAnonimizzati: boolean = false, assistenzaAslId: string = '', profiloUtente: string = ''): Observable<HttpResponse<{fileId: number; elaborazioneId: number}>> {
    let params = new HttpParams()
      .set('flagDatiAnonimizzati', String(flagDatiAnonimizzati))
      .set('profiloUtente', profiloUtente);
    if (assistenzaAslId) params = params.set('assistenza_asl_id', assistenzaAslId);
    const formData = new FormData();
    formData.append('auditLogRequest', new Blob(
      [JSON.stringify(this.authService.buildAuditLog('read', 'ANAGRAFICA', 'scaricaDatiExcelTotali'))],
      { type: 'application/json' }
    ));
    return this.http.post<{fileId: number; elaborazioneId: number}>(
      API_ENDPOINTS.anagrafica.scaricaDatiExcelTotali,
      formData,
      { params, observe: 'response' }
    );
  }
}
