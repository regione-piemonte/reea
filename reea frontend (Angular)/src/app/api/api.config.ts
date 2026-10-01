/**
 * Configurazione centralizzata delle API
 * API_BASE_URL viene letto da environment.ts (generato da Maven dal template)
 */
import { environment } from '../../environments/environment';

// Base URL del backend - valorizzato dal profilo Maven via environment.ts
export const API_BASE_URL = environment.apiUrl;

// Endpoints organizzati per controller
export const API_ENDPOINTS = {

  // AnagraficaController - /api/anagrafica
  anagrafica: {
    getLista: `${API_BASE_URL}/api/anagrafica/getListaAnagrafiche`,
    scaricaDatiExcelTotali: `${API_BASE_URL}/api/anagrafica/scaricaDatiExcelTotaliAsincroni`,
    crea: `${API_BASE_URL}/api/anagrafica/creazioneAnagrafica`,
    getById: (soggettoId: number) => `${API_BASE_URL}/api/anagrafica/getById/${soggettoId}`,
    getByIdCampiAnonimizzati: (soggettoId: number, flagCampiAnonimi: boolean) => `${API_BASE_URL}/api/anagrafica/getByIdCampiAnonimizzati/${soggettoId}?flagCampiAnonimi=${flagCampiAnonimi}`,
    modifica: (soggettoId: number) => `${API_BASE_URL}/api/anagrafica/modifica/${soggettoId}`,
    search: `${API_BASE_URL}/api/anagrafica/getByCForNomeorCognomeorDataNascita`,
    aggiornaStato: (soggettoId: number) => `${API_BASE_URL}/api/anagrafica/aggiornaStato/${soggettoId}`,
    storiaStati: (soggettoId: number) => `${API_BASE_URL}/api/anagrafica/storiaStati/${soggettoId}`,
  },

  // FonteController - /api/fonte
  fonte: {
    getStoricoFonti: (soggettoId: number) => `${API_BASE_URL}/api/fonte/getListaFonteDescByIdSoggetto/${soggettoId}`,
  },

  // AdesioneImportController - /api/adesioni
  adesioni: {
    import: `${API_BASE_URL}/api/adesioni/import`,
    getRecordTabAdesioni: `${API_BASE_URL}/api/adesioni/getRecordTabAdesioni`,
    inserimentiMassiviFileAdesioni: `${API_BASE_URL}/api/adesioni/inserimentiMassiviFileAdesioni`,
    getBySoggettoId: (soggettoId: number) => `${API_BASE_URL}/api/adesioni/getBySoggettoId/${soggettoId}`,
    scaricaDatiExcelPreadesioniFiltrate: `${API_BASE_URL}/api/adesioni/scaricaDatiExcelPreadesioniFiltrate`,
  },

  // NplaController - /api/npla
  npla: {
    import: `${API_BASE_URL}/api/npla/import`,
    getRecordTabNpla: `${API_BASE_URL}/api/npla/getRecordTabNpla`,
    inserimentiMassiviNpla: `${API_BASE_URL}/api/npla/inserimentiMassiviNpla`,
  },

  // InailController - /api/inail
  inail: {
    importInailA: `${API_BASE_URL}/api/inail/import/inail/A`,
    getRecordTabInail: `${API_BASE_URL}/api/inail/getRecordTabInail`,
    inserimentiMassiviFileInailA: `${API_BASE_URL}/api/inail/inserimentiMassiviFileInail/A`,
    importInailB: `${API_BASE_URL}/api/inail/import/inail/B`,
    getRecordTabInailB: `${API_BASE_URL}/api/inail/getRecordTabInail`,
    inserimentiMassiviFileInailB: `${API_BASE_URL}/api/inail/inserimentiMassiviFileInail/B`,
  },

  // SpresalController - /api/spresal
  spresal: {
    import: `${API_BASE_URL}/api/spresal/import`,
    importEsiti: `${API_BASE_URL}/api/spresal/import/esiti`,
    getRecordTabSpresal: `${API_BASE_URL}/api/spresal/getRecordTabSpresal`,
    inserimentiMassiviFileSpresal: `${API_BASE_URL}/api/spresal/inserimentiMassiviFileSpresalAnamnesi`,
    getRecordTabSpresalEsiti: `${API_BASE_URL}/api/spresal/getRecordTabSpresalEsiti`,
    inserimentiMassiviFileSpresalEsiti: `${API_BASE_URL}/api/spresal/inserimentiMassiviFileSpresalEsiti`,
    getEsitiByRegistroId: (registroId: number) => `${API_BASE_URL}/api/spresal/getEsitiByRegistroId/${registroId}`,
    aggiornaCrpt: (regSpresalAnamnesiId: number) => `${API_BASE_URL}/api/spresal/aggiornaCrpt/${regSpresalAnamnesiId}`,
    aggiornaCounseling: (regSpresalAnamnesiId: number) => `${API_BASE_URL}/api/spresal/aggiornaCounseling/${regSpresalAnamnesiId}`,
    aggiornaPrestazioneAcquisita: (esitoId: number) => `${API_BASE_URL}/api/spresal/aggiornaPrestazioneAcquisita/${esitoId}`,
    dizionarioSettore: `${API_BASE_URL}/api/spresal/dizionari/settore`,
    dizionarioMansione: `${API_BASE_URL}/api/spresal/dizionari/mansione`,
    dizionarioRagioneSociale: `${API_BASE_URL}/api/spresal/dizionari/ragionesociale`,
    dizionarioPiva: `${API_BASE_URL}/api/spresal/dizionari/piva`,
    saveRagioneSocialeDecodifica: `${API_BASE_URL}/api/spresal/dizionari/ragionesociale`,
  },

  // AuditController - /api/audit
  audit: {
    salva: `${API_BASE_URL}/api/audit/salva`,
  },

  // ParametroController -  /api/parametri
  parametri: {
    getLista: `${API_BASE_URL}/api/parametri/getListaParametri`,
    getValore: (cod: string) => `${API_BASE_URL}/api/parametri/getValore?cod=${cod}`,
  },

  // AslController - /api/asl
  asl: {
    getListaCompetenza: `${API_BASE_URL}/api/asl/getListaASLCompetenza`,
    getListaFiltroAssistiti: `${API_BASE_URL}/api/asl/getListaFiltroAssistiti`,
  },

  // NazioneController - /api/nazioni
  nazioni: {
    getLista: `${API_BASE_URL}/api/nazioni/getListaStatoNascitaResidenza`,
  },

  // ProvinciaController - /api/province
  province: {
    getLista: `${API_BASE_URL}/api/province/getListaProvinciaNascitaResidenza`,
  },

  // ComuneController - /api/comuni
  comuni: {
    // provincia_id opzionale: se assente restituisce tutti i comuni
    getLista: `${API_BASE_URL}/api/comuni/getListaComuneNascitaResidenzaAzienda`,
  },

  // FileController - /api/file (CDU-026)
  file: {
    getLista: `${API_BASE_URL}/api/file/getListaFile`,
    download: (fileId: number) => `${API_BASE_URL}/api/file/download/${fileId}`,
    downloadByName: (nome: string) => `${API_BASE_URL}/api/file/download/${nome}`,
    downloadImport: (fileId: number) => `${API_BASE_URL}/api/file/downloadImport/${fileId}`,
    getExportList: (aslId?: string) =>
      `${API_BASE_URL}/api/file/getExportList${aslId ? `?assistenza_asl_id=${aslId}` : ''}`,
    uploadPdf: `${API_BASE_URL}/api/file/upload-pdf`,
    downloadPdf: `${API_BASE_URL}/api/file/download-pdf`,
    eliminaPdf: `${API_BASE_URL}/api/file/elimina-pdf`,
    listaPdf: (soggettoId: number) => `${API_BASE_URL}/api/pdf/lista/${soggettoId}`,
    downloadPdfByName: (nomeFile: string) => `${API_BASE_URL}/api/pdf/download/${nomeFile}`,
    avviaExportAllegato4: (anno: number) => `${API_BASE_URL}/api/spresal/export/allegato4?anno=${anno}`,
    getExportListAllegato4: `${API_BASE_URL}/api/spresal/export/allegato4`,
    downloadAllegato4: (elaborazioneId: number) => `${API_BASE_URL}/api/spresal/export/allegato4/download/${elaborazioneId}`,
  },

  // ArchivioFileCaricatiController - /api/archivioFileCaricatiport file massivi 
  archivioFileCaricati: {
    getLista: `${API_BASE_URL}/api/archivioFileCaricati/getArchivioFileCaricati`,
    getErrori: (elaborazioneId: number) => `${API_BASE_URL}/api/archivioFileCaricati/getElaborazioneErroreFile/${elaborazioneId}`,
    getScartati: (fileId: number) => `${API_BASE_URL}/api/archivioFileCaricati/getScaricoErroreFile/${fileId}`,
    checkNomeFile: (nomeFile: string) => `${API_BASE_URL}/api/archivioFileCaricati/checkNomeFile?nomeFile=${encodeURIComponent(nomeFile)}`,
    checkConcorrenza: (tipo: string) => `${API_BASE_URL}/api/archivioFileCaricati/checkConcorrenza?tipo=${tipo}`,
  },

  // NoteController - /api/note
  note: {
    getByRegistroId: (soggettoId: number) => `${API_BASE_URL}/api/note/getByRegistroId/${soggettoId}`,
    inserisci: `${API_BASE_URL}/api/note/inserisci`,
    modifica: (notaId: number) => `${API_BASE_URL}/api/note/modifica/${notaId}`,
    elimina: (notaId: number) => `${API_BASE_URL}/api/note/elimina/${notaId}`,
  },

  // EsenzioneController - /api/esenzioni
  esenzioni: {
    getByIdSoggetto: (soggettoId: number) => `${API_BASE_URL}/api/esenzioni/getRecordTabEsenzione/${soggettoId}`,
  },

  // LoginController - /api/login
  login: {
    accedi:    `${API_BASE_URL}/api/login/accedi`,
    logout:    `${API_BASE_URL}/api/login/logout`,
    me:        `${API_BASE_URL}/api/login/me`,
    tipoAuth:  `${API_BASE_URL}/api/login/tipo-auth`,
  },

  // AuraController - /api/aura
  aura: {
    find: `${API_BASE_URL}/api/aura/find`,
    findByAnagrafica: `${API_BASE_URL}/api/aura/findByAnagrafica`,
    getById: (idAura: string) => `${API_BASE_URL}/api/aura/${idAura}`,
  },
};
