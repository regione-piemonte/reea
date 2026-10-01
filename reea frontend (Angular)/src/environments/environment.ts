// Questo file è committato come fallback per build Docker/npm dirette.
// In Jenkins/Maven: viene sovrascritto automaticamente da environment.template.ts
// In sviluppo locale: mantieni aggiornato a mano con i valori del tuo ambiente
export const environment = {
  production: false,
  appName: 'reeafe',
  apiUrl: '/reeabe',
  useMockData: false,
  refreshTimeMs: 3600000,
  customVar: 'locale'
};
