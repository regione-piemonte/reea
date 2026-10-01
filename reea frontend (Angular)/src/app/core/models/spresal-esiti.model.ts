/**
 * DTO per gli Esiti di Sorveglianza SPRESAL
 * Campi allineati esattamente ai @JsonProperty del backend SpresalEsitiDTO
 */
export interface SpresalEsitiDTO {
  reg_spresal_esiti_id: number;
  registro_id: number;
  codice_fiscale?: string | null;
  id_aura?: number | null;

  // ===== DATI VISITA =====
  data_visita: string | null;
  visita: string | null;
  livello_visita: string | null;
  riceve_indennizzo: boolean | null;
  malattia_indennizzo?: string | null;

  // ===== ACCERTAMENTI DIAGNOSTICI =====
  accertamenti_rx: boolean | null;
  accertamenti_rx_data?: string | null;
  accertamenti_rx_referto_normale?: string | null;
  accertamenti_rx_acquisita?: boolean | null;

  accertamenti_tc: boolean | null;
  accertamenti_tc_data?: string | null;
  accertamenti_tc_referto_normale?: string | null;
  accertamenti_tc_acquisita?: boolean | null;

  accertamenti_spirometria_semplice: boolean | null;
  accertamenti_spirometria_semplice_data?: string | null;
  accertamenti_spirometria_semplice_referto_norm?: string | null;
  accertamenti_spirometria_semplice_acquisita?: boolean | null;

  accertamenti_spirometria_globale: boolean | null;
  accertamenti_spirometria_globale_data?: string | null;
  accertamenti_spirometria_globale_referto_norm?: string | null;
  accertamenti_spirometria_globale_acquisita?: boolean | null;

  accertamenti_dlco: boolean | null;
  accertamenti_dlco_data?: string | null;
  accertamenti_dlco_referto_normale?: string | null;
  accertamenti_dlco_acquisita?: boolean | null;

  accertamenti_pet: boolean | null;
  accertamenti_pet_data?: string | null;
  accertamenti_pet_referto_normale?: string | null;
  accertamenti_pet_acquisita?: boolean | null;

  accertamenti_visita_pneumologica: boolean | null;
  accertamenti_visita_pneumologica_data?: string | null;
  accertamenti_visita_pneumologica_referto?: string | null;
  accertamenti_visita_pneumologica_acquisita?: boolean | null;

  accertamenti_visita_radiologica: boolean | null;
  accertamenti_visita_radiologica_data?: string | null;
  accertamenti_visita_radiologica_referto?: string | null;
  accertamenti_visita_radiologica_acquisita?: boolean | null;

  accertamenti_visita_oncologica: boolean | null;
  accertamenti_visita_oncologica_data?: string | null;
  accertamenti_visita_oncologica_referto?: string | null;
  accertamenti_visita_oncologica_acquisita?: boolean | null;

  accertamenti_altro: boolean | null;
  accertamenti_altro_descrizione?: string | null;
  accertamenti_altro_data?: string | null;
  accertamenti_altro_referto_normale?: string | null;
  accertamenti_altro_referto_acquisita?: boolean | null;

  // ===== DIAGNOSI =====
  risultato_negativo?: boolean | null;

  ppm_placche_pleuriche_monolaterali: boolean | null;
  ppm_primo_certificato_e_denuncia?: boolean | null;
  ppm_primo_certificato_e_denuncia_data?: string | null;
  ppm_aggravamento_e_denuncia?: boolean | null;
  ppm_aggravamento_e_denuncia_data?: string | null;
  ppm_percentuale_di_riconoscimento?: number | null;
  ppm_referto?: boolean | null;
  ppm_referto_data?: string | null;

  ppb_placche_pleuriche_bilaterali: boolean | null;
  ppb_primo_certificato_e_denuncia?: boolean | null;
  ppb_primo_certificato_e_denuncia_data?: string | null;
  ppb_aggravamento_e_denuncia?: boolean | null;
  ppb_aggravamento_e_denuncia_data?: string | null;
  ppb_percentuale_di_riconoscimento?: number | null;
  ppb_referto?: boolean | null;
  ppb_referto_data?: string | null;

  ap_asbestosi_polmonare: boolean | null;
  ap_primo_certificato_e_denuncia?: boolean | null;
  ap_primo_certificato_e_denuncia_data?: string | null;
  ap_aggravamento_e_denuncia?: boolean | null;
  ap_aggravamento_e_denuncia_data?: string | null;
  ap_percentuale_di_riconoscimento?: number | null;
  ap_referto?: boolean | null;
  ap_referto_data?: string | null;

  fpd_fibrosi_pleurica_diffusa: boolean | null;
  fpd_primo_certificato_e_denuncia?: boolean | null;
  fpd_primo_certificato_e_denuncia_data?: string | null;
  fpd_aggravamento_e_denuncia?: boolean | null;
  fpd_aggravamento_e_denuncia_data?: string | null;
  fpd_percentuale_di_riconoscimento?: number | null;
  fpd_referto?: boolean | null;
  fpd_referto_data?: string | null;

  mp_mesotelioma_pleurico: boolean | null;
  mp_primo_certificato_e_denuncia?: boolean | null;
  mp_primo_certificato_e_denuncia_data?: string | null;
  mp_aggravamento_e_denuncia?: boolean | null;
  mp_aggravamento_e_denuncia_data?: string | null;
  mp_percentuale_di_riconoscimento?: number | null;
  mp_referto?: boolean | null;
  mp_referto_data?: string | null;
  mp_comunicazione_al_cor?: boolean | null;
  mp_comunicazione_al_cor_data?: string | null;

  am_altro_mesotelioma: boolean | null;
  am_primo_certificato_e_denuncia?: boolean | null;
  am_primo_certificato_e_denuncia_data?: string | null;
  am_aggravamento_e_denuncia?: boolean | null;
  am_aggravamento_e_denuncia_data?: string | null;
  am_percentuale_di_riconoscimento?: number | null;
  am_referto?: boolean | null;
  am_referto_data?: string | null;
  am_comunicazione_al_cor?: boolean | null;
  am_comunicazione_al_cor_data?: string | null;

  nl_neoplasia_laringe: boolean | null;
  nl_primo_certificato_e_denuncia?: boolean | null;
  nl_primo_certificato_e_denuncia_data?: string | null;
  nl_aggravamento_e_denuncia?: boolean | null;
  nl_aggravamento_e_denuncia_data?: string | null;
  nl_percentuale_di_riconoscimento?: number | null;
  nl_referto?: boolean | null;
  nl_referto_data?: string | null;

  no_neoplasia_ovarica: boolean | null;
  no_primo_certificato_e_denuncia?: boolean | null;
  no_primo_certificato_e_denuncia_data?: string | null;
  no_aggravamento_e_denuncia?: boolean | null;
  no_aggravamento_e_denuncia_data?: string | null;
  no_percentuale_di_riconoscimento?: number | null;
  no_referto?: boolean | null;
  no_referto_data?: string | null;

  tp_tumore_del_polmone: boolean | null;
  tp_primo_certificato_e_denuncia?: boolean | null;
  tp_primo_certificato_e_denuncia_data?: string | null;
  tp_aggravamento_e_denuncia?: boolean | null;
  tp_aggravamento_e_denuncia_data?: string | null;
  tp_percentuale_di_riconoscimento?: number | null;
  tp_referto?: boolean | null;
  tp_referto_data?: string | null;
  tp_comunicazione_al_cor?: boolean | null;
  tp_comunicazione_al_cor_data?: string | null;

  bpco_enfisema_polmonare: boolean | null;
  bpco_primo_certificato_e_denuncia?: boolean | null;
  bpco_primo_certificato_e_denuncia_data?: string | null;
  bpco_aggravamento_e_denuncia?: boolean | null;
  bpco_aggravamento_e_denuncia_data?: string | null;
  bpco_percentuale_di_riconoscimento?: number | null;
  bpco_referto?: boolean | null;
  bpco_referto_data?: string | null;

  altra_diagnosi: boolean | null;
  altra_diagnosi_descrizione?: string | null;
  altra_prima_certificato_e_denuncia?: boolean | null;
  altra_prima_certificato_e_denuncia_data?: string | null;
  altra_aggravamento_e_denuncia?: boolean | null;
  altra_aggravamento_e_denuncia_data?: string | null;
  altra_percentuale_di_riconoscimento?: number | null;
  altra_referto?: boolean | null;
  altra_referto_data?: string | null;

  // ===== FOLLOW-UP =====
  follow_up_previsto: boolean | null;
  anno_presunto_prossima_visita: number | null;
  anno_ultima_visita: number | null;
  invio_sintesi_a_mmg: boolean | null;
}
